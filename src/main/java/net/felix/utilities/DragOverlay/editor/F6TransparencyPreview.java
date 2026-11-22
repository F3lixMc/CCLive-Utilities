package net.felix.utilities.DragOverlay.editor;

import com.mojang.blaze3d.platform.NativeImage;
import dev.isxander.yacl3.gui.image.ImageRenderer;
import java.io.InputStream;
import java.util.function.IntSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Rechte YACL-Vorschau: Screenshot, darüber dieselbe F6-Abdunklung wie im Editor.
 */
public final class F6TransparencyPreview implements ImageRenderer {

    static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "cclive-utilities", "textures/gui/f6_transparency_preview.png");

    private static int imageWidth = 2560;
    private static int imageHeight = 1369;
    private static boolean sizeResolved;

    private final IntSupplier transparencyPercent;

    public F6TransparencyPreview(IntSupplier transparencyPercent) {
        this.transparencyPercent = transparencyPercent;
    }

    @Override
    public int render(GuiGraphicsExtractor graphics, int x, int y, int renderWidth, float tickDelta) {
        resolveSize();
        int destWidth = Math.max(1, renderWidth);
        int destHeight = Math.max(1, Math.round(destWidth * (imageHeight / (float) imageWidth)));
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x, y,
                0.0f, 0.0f,
                destWidth, destHeight,
                imageWidth, imageHeight,
                imageWidth, imageHeight);
        int alpha = F6MenuBackground.alpha(transparencyPercent.getAsInt());
        graphics.fill(x, y, x + destWidth, y + destHeight, alpha << 24);
        return destHeight;
    }

    @Override
    public void close() {
    }

    private static void resolveSize() {
        if (sizeResolved) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        try {
            var resource = client.getResourceManager().getResource(TEXTURE);
            if (resource.isEmpty()) {
                return;
            }
            try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
                imageWidth = Math.max(1, image.getWidth());
                imageHeight = Math.max(1, image.getHeight());
                sizeResolved = true;
            }
        } catch (Exception ignored) {
            sizeResolved = true;
        }
    }
}
