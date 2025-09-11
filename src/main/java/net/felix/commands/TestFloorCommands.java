package net.felix.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.felix.utilities.MobTimerUtility;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class TestFloorCommands {
    
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerCommands(dispatcher);
        });
    }
    
    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        
        // /setTestFloor <floor>
        dispatcher.register(literal("setTestFloor")
            .then(argument("floor", StringArgumentType.word())
                .executes(context -> {
                    String floor = StringArgumentType.getString(context, "floor");
                    return setTestFloor(context.getSource(), floor);
                }))
            .executes(context -> {
                context.getSource().sendError(Text.literal("Verwendung: /setTestFloor <floor>"));
                context.getSource().sendError(Text.literal("Verfügbare Floors: floor_1, floor_2, floor_3, floor_4, floor_5"));
                return 0;
            }));
        
        // /showTestFloor
        dispatcher.register(literal("showTestFloor")
            .executes(context -> {
                return showTestFloor(context.getSource());
            }));
        
        // /listAvailableFloors
        dispatcher.register(literal("listAvailableFloors")
            .executes(context -> {
                return listAvailableFloors(context.getSource());
            }));
        
        // /clearTestFloor
        dispatcher.register(literal("clearTestFloor")
            .executes(context -> {
                return clearTestFloor(context.getSource());
            }));
        
        // /testFloorHelp
        dispatcher.register(literal("testFloorHelp")
            .executes(context -> {
                return showHelp(context.getSource());
            }));
    }
    
    private static int setTestFloor(FabricClientCommandSource source, String floor) {
        try {
            MobTimerUtility.setTestFloor(floor);
            source.sendFeedback(createSuccessMessage("Floor erfolgreich auf " + floor + " gesetzt!"));
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Fehler beim Setzen des Test-Floors: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int showTestFloor(FabricClientCommandSource source) {
        try {
            MobTimerUtility.showTestFloor();
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Fehler beim Anzeigen des Test-Floors: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int listAvailableFloors(FabricClientCommandSource source) {
        try {
            MobTimerUtility.listAvailableFloors();
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Fehler beim Auflisten der Floors: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int clearTestFloor(FabricClientCommandSource source) {
        try {
            MobTimerUtility.clearTestFloor();
            source.sendFeedback(createSuccessMessage("Test-Floor erfolgreich entfernt!"));
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Fehler beim Entfernen des Test-Floors: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int showHelp(FabricClientCommandSource source) {
        MutableText helpText = Text.literal("=== TEST-FLOOR COMMANDS ===\n")
            .append(Text.literal("/setTestFloor <floor>").formatted(Formatting.GREEN))
            .append(Text.literal(" - Setzt den Test-Floor\n"))
            .append(Text.literal("/showTestFloor").formatted(Formatting.GREEN))
            .append(Text.literal(" - Zeigt den aktuellen Test-Floor\n"))
            .append(Text.literal("/listAvailableFloors").formatted(Formatting.GREEN))
            .append(Text.literal(" - Listet alle verfügbaren Floors\n"))
            .append(Text.literal("/clearTestFloor").formatted(Formatting.GREEN))
            .append(Text.literal(" - Entfernt den Test-Floor\n"))
            .append(Text.literal("/testFloorHelp").formatted(Formatting.GREEN))
            .append(Text.literal(" - Zeigt diese Hilfe\n"))
            .append(Text.literal("\nVerfügbare Floors: "))
            .append(Text.literal("floor_1, floor_2, floor_3, floor_4, floor_5").formatted(Formatting.YELLOW));
        
        source.sendFeedback(helpText);
        return 1;
    }
    
    private static MutableText createSuccessMessage(String message) {
        return Text.literal("✅ " + message).formatted(Formatting.GREEN);
    }
}

