package net.felix.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.felix.staff.StaffClientAccess;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModSanctionCompletionTest {
    @Test
    void permanentOffersConfirmLiteralAndTimedDurationDoesNot() throws Exception {
        FabricClientCommandSource source = source();
        CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();
        CCLiveCommands.registerCCLiveCommands(dispatcher);
        CommandNode<FabricClientCommandSource> mod = dispatcher.getRoot().getChild("cclive").getChild("mod");
        setRole(null);
        assertFalse(mod.canUse(source));
        setRole("moderator");
        assertTrue(mod.canUse(source));
        setRole("admin");
        assertTrue(mod.canUse(source));
        setRole("moderator");

        CommandNode<FabricClientCommandSource> muteName = mod.getChild("mute").getChild("name");
        CommandNode<FabricClientCommandSource> banName = mod.getChild("ban").getChild("name");
        assertNotNull(muteName.getChild("permanent").getChild("confirm"));
        assertNotNull(banName.getChild("permanent").getChild("confirm"));
        assertNull(muteName.getChild("duration").getChild("confirm"));
        assertNull(banName.getChild("duration").getChild("confirm"));
        assertNotNull(((ArgumentCommandNode<FabricClientCommandSource, ?>) muteName).getCustomSuggestions());
        assertNotNull(((ArgumentCommandNode<FabricClientCommandSource, ?>) mod.getChild("unmute").getChild("name")).getCustomSuggestions());
        assertNotNull(((ArgumentCommandNode<FabricClientCommandSource, ?>) mod.getChild("unban").getChild("name")).getCustomSuggestions());

        List<String> afterPermanent = suggest(dispatcher, source, "cclive mod mute Steve permanent ");
        assertTrue(afterPermanent.contains("confirm"));
        List<String> afterTimed = suggest(dispatcher, source, "cclive mod mute Steve 5m ");
        assertFalse(afterTimed.contains("confirm"));
        List<String> banConfirm = suggest(dispatcher, source, "cclive mod ban Steve permanent ");
        assertTrue(banConfirm.contains("confirm"));
        List<String> durations = suggest(dispatcher, source, "cclive mod mute Steve ");
        assertTrue(durations.contains("permanent"));
        assertTrue(durations.contains("10m"));
        assertFalse(durations.contains("confirm"));

        assertTrue(dispatcher.parse("cclive mod mute Steve permanent confirm Ausreichender Grund", source).getExceptions().isEmpty());
        assertTrue(dispatcher.parse("cclive mod ban Steve 5m Ausreichender Grund", source).getExceptions().isEmpty());
        assertFalse(dispatcher.parse("cclive mod mute Steve permanent Ausreichender Grund", source).getReader().getRemaining().isBlank());
    }

    private static List<String> suggest(
            CommandDispatcher<FabricClientCommandSource> dispatcher,
            FabricClientCommandSource source,
            String command) throws ExecutionException, InterruptedException {
        return dispatcher.getCompletionSuggestions(dispatcher.parse(command, source))
            .get()
            .getList()
            .stream()
            .map(Suggestion::getText)
            .toList();
    }

    private static void setRole(String role) throws Exception {
        Field field = StaffClientAccess.class.getDeclaredField("cachedRole");
        field.setAccessible(true);
        field.set(null, role);
    }

    private static FabricClientCommandSource source() {
        return (FabricClientCommandSource) Proxy.newProxyInstance(
            FabricClientCommandSource.class.getClassLoader(),
            new Class<?>[] { FabricClientCommandSource.class },
            (proxy, method, args) -> {
                Class<?> type = method.getReturnType();
                if (type == boolean.class) return false;
                if (type == int.class) return 0;
                if (type == long.class) return 0L;
                return null;
            });
    }
}
