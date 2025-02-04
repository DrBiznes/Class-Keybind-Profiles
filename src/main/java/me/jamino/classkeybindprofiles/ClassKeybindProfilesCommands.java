package me.jamino.classkeybindprofiles;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.wynntils.models.character.type.ClassType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class ClassKeybindProfilesCommands {

    private static final String ALL_CLASSES = "Warrior, Mage, Archer, Assassin, Shaman";

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("ckp")
                    .then(literal("help").executes(ClassKeybindProfilesCommands::executeHelp))
                    .then(literal("profiles").executes(ClassKeybindProfilesCommands::executeProfiles))
                    .then(literal("save")
                            .then(ClientCommandManager.argument("className", ClassArgumentType.classType())
                                    .executes(context -> executeSave(context, ClassArgumentType.getClassType(context, "className")))))
                    .then(literal("delete")
                            .then(ClientCommandManager.argument("className", ClassArgumentType.classType())
                                    .executes(context -> executeDelete(context, ClassArgumentType.getClassType(context, "className")))))
                    .then(literal("nuke").executes(ClassKeybindProfilesCommands::executeNuke)));
        });
    }

    private static int executeHelp(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("=== Class Keybind Profiles Commands ===").formatted(Formatting.GOLD));
        context.getSource().sendFeedback(Text.literal("/ckp help - Shows this help message").formatted(Formatting.YELLOW));
        context.getSource().sendFeedback(Text.literal("/ckp profiles - Lists all saved profiles").formatted(Formatting.YELLOW));
        context.getSource().sendFeedback(Text.literal("/ckp save <className> - Saves current keybinds for the specified class").formatted(Formatting.YELLOW));
        context.getSource().sendFeedback(Text.literal("/ckp delete <className> - Deletes the profile for the specified class").formatted(Formatting.YELLOW));
        context.getSource().sendFeedback(Text.literal("/ckp nuke - Deletes all saved profiles").formatted(Formatting.YELLOW));
        context.getSource().sendFeedback(Text.literal("Available classes: " + ALL_CLASSES).formatted(Formatting.GRAY));
        return 1;
    }

    private static int executeProfiles(CommandContext<FabricClientCommandSource> context) {
        Map<String, Map<String, String>> profiles = ClassKeybindProfiles.config.getAllProfiles();

        context.getSource().sendFeedback(Text.literal("=== Class Keybind Profiles ===").formatted(Formatting.GOLD));

        // Show status for all classes
        for (ClassType classType : ClassType.values()) {
            if (classType == ClassType.NONE) continue;

            // Convert class name to proper case (e.g., "WARRIOR" -> "Warrior")
            String properClassName = classType.name().substring(0, 1).toUpperCase()
                    + classType.name().substring(1).toLowerCase();

            boolean hasProfile = profiles.containsKey(classType.getName());
            Text statusText = Text.literal(properClassName + " - ")
                    .append(Text.literal(hasProfile ? "Profile Saved" : "No Profile Saved")
                            .formatted(hasProfile ? Formatting.GREEN : Formatting.RED));

            context.getSource().sendFeedback(statusText);
        }
        return 1;
    }

    private static int executeSave(CommandContext<FabricClientCommandSource> context, ClassType classType) {
        if (classType == ClassType.NONE) {
            context.getSource().sendFeedback(Text.literal("Invalid class name.").formatted(Formatting.RED));
            return 0;
        }

        String properClassName = classType.getName().substring(0, 1).toUpperCase()
                + classType.getName().substring(1).toLowerCase();

        ClassKeybindProfiles.saveCurrentKeybindsForClass(classType.getName());
        context.getSource().sendFeedback(Text.literal("Successfully saved keybind profile for " + properClassName)
                .formatted(Formatting.GREEN));
        return 1;
    }

    private static int executeDelete(CommandContext<FabricClientCommandSource> context, ClassType classType) {
        if (classType == ClassType.NONE) {
            context.getSource().sendFeedback(Text.literal("Invalid class name.").formatted(Formatting.RED));
            return 0;
        }

        String properClassName = classType.getName().substring(0, 1).toUpperCase()
                + classType.getName().substring(1).toLowerCase();

        ClassKeybindProfiles.clearProfileForClass(classType.getName());
        context.getSource().sendFeedback(Text.literal("Successfully deleted keybind profile for " + properClassName)
                .formatted(Formatting.GREEN));
        return 1;
    }

    private static int executeNuke(CommandContext<FabricClientCommandSource> context) {
        for (ClassType classType : ClassType.values()) {
            if (classType != ClassType.NONE) {
                ClassKeybindProfiles.clearProfileForClass(classType.getName());
            }
        }
        context.getSource().sendFeedback(Text.literal("Successfully deleted all keybind profiles").formatted(Formatting.GREEN));
        return 1;
    }
}