package me.jamino.classkeybindprofiles;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.wynntils.models.character.type.ClassType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
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
                    .then(literal("status").executes(ClassKeybindProfilesCommands::executeStatus))
                    .then(literal("profiles").executes(ClassKeybindProfilesCommands::executeProfiles))
                    .then(literal("save")
                            .executes(ClassKeybindProfilesCommands::executeSaveCurrentClass)
                            .then(ClientCommandManager.argument("className", ClassArgumentType.classType())
                                    .executes(context -> executeSave(context, ClassArgumentType.getClassType(context, "className")))))
                    .then(literal("delete")
                            .then(ClientCommandManager.argument("className", ClassArgumentType.classType())
                                    .executes(context -> executeDelete(context, ClassArgumentType.getClassType(context, "className")))))
                    .then(literal("export")
                            .then(ClientCommandManager.argument("className", ClassArgumentType.classType())
                                    .executes(context -> executeExport(context, ClassArgumentType.getClassType(context, "className")))))
                    .then(literal("import")
                            .then(ClientCommandManager.argument("code", StringArgumentType.greedyString())
                                    .executes(context -> executeImport(context, StringArgumentType.getString(context, "code")))))
                    .then(literal("nuke").executes(ClassKeybindProfilesCommands::executeNuke)));
        });
    }

    private static int executeHelp(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("=== Class Keybind Profiles Commands ===").formatted(Formatting.GOLD));
        context.getSource().sendFeedback(Text.literal("/ckp help - Shows this help message").formatted(Formatting.YELLOW));
        context.getSource().sendFeedback(Text.literal("/ckp status - Shows saved keybind information").formatted(Formatting.YELLOW));
        context.getSource().sendFeedback(Text.literal("/ckp profiles - Lists all saved profiles").formatted(Formatting.YELLOW));
        context.getSource().sendFeedback(Text.literal("/ckp save - Saves current keybinds for your current class").formatted(Formatting.YELLOW));
        context.getSource().sendFeedback(Text.literal("/ckp save <className> - Saves current keybinds for the specified class").formatted(Formatting.YELLOW));
        context.getSource().sendFeedback(Text.literal("/ckp export <className> - Exports profile as shareable code (copies to clipboard)").formatted(Formatting.YELLOW));
        context.getSource().sendFeedback(Text.literal("/ckp import <code> - Imports profile from code").formatted(Formatting.YELLOW));
        context.getSource().sendFeedback(Text.literal("/ckp delete <className> - Deletes the profile for the specified class").formatted(Formatting.YELLOW));
        context.getSource().sendFeedback(Text.literal("/ckp nuke - Deletes all saved profiles").formatted(Formatting.YELLOW));
        context.getSource().sendFeedback(Text.literal("Available classes: " + ALL_CLASSES).formatted(Formatting.GRAY));
        return 1;
    }

    private static int executeStatus(CommandContext<FabricClientCommandSource> context) {
        Map<String, Map<String, String>> profiles = ClassKeybindProfiles.config.getAllProfiles();

        // Check if any profiles exist
        boolean hasAnyProfiles = profiles.values().stream().anyMatch(profile -> !profile.isEmpty());

        if (!hasAnyProfiles) {
            context.getSource().sendFeedback(Text.literal("No profiles saved yet. Use '/ckp save' to create your first profile.").formatted(Formatting.RED));
            return 0;
        }

        context.getSource().sendFeedback(Text.literal("=== Class Keybind Profiles Status ===").formatted(Formatting.GOLD));
        context.getSource().sendFeedback(Text.literal("Detected keybinds in saved profiles:").formatted(Formatting.GRAY));

        // Analyze all profiles to see which mod types have keybinds
        boolean hasWynntils = false;
        boolean hasWynncraft = false;
        boolean hasBetterWynn = false;
        int wynntilsCount = 0;
        int wynncraftCount = 0;
        int betterWynnCount = 0;

        for (Map<String, String> profile : profiles.values()) {
            for (String key : profile.keySet()) {
                if (key.startsWith("Cast ")) {
                    hasWynntils = true;
                    wynntilsCount++;
                } else if (key.startsWith("key.wynncraft-spell-caster")) {
                    hasWynncraft = true;
                    wynncraftCount++;
                } else if (key.startsWith("key.ktnwynnmacros")) {
                    hasBetterWynn = true;
                    betterWynnCount++;
                }
            }
        }

        // Display results
        if (hasWynntils) {
            context.getSource().sendFeedback(Text.literal("✓ Wynntils (" + wynntilsCount + " keybinds saved)").formatted(Formatting.GREEN));
        } else {
            context.getSource().sendFeedback(Text.literal("✗ Wynntils (no keybinds saved)").formatted(Formatting.RED));
        }

        if (hasWynncraft) {
            context.getSource().sendFeedback(Text.literal("✓ Wynncraft Spell Caster (" + wynncraftCount + " keybinds saved)").formatted(Formatting.GREEN));
        } else {
            context.getSource().sendFeedback(Text.literal("✗ Wynncraft Spell Caster (no keybinds saved)").formatted(Formatting.RED));
        }

        if (hasBetterWynn) {
            context.getSource().sendFeedback(Text.literal("✓ BetterWynnMacros (" + betterWynnCount + " keybinds saved)").formatted(Formatting.GREEN));
        } else {
            context.getSource().sendFeedback(Text.literal("✗ BetterWynnMacros (no keybinds saved)").formatted(Formatting.RED));
        }

        return 1;
    }

    private static int executeSaveCurrentClass(CommandContext<FabricClientCommandSource> context) {
        String currentClass = ClassKeybindProfiles.getCurrentClass();

        if (currentClass.equals("NONE")) {
            context.getSource().sendFeedback(Text.literal("No class currently selected. Please pick a class or use '/ckp save <className>' instead.").formatted(Formatting.RED));
            return 0;
        }

        ClassType classType = ClassType.fromName(currentClass);
        if (classType == ClassType.NONE) {
            context.getSource().sendFeedback(Text.literal("No valid class detected. Please pick a class or use '/ckp save <className>' instead.").formatted(Formatting.RED));
            return 0;
        }

        String properClassName = classType.getName().substring(0, 1).toUpperCase()
                + classType.getName().substring(1).toLowerCase();

        ClassKeybindProfiles.saveCurrentKeybindsForClass(classType.getName());
        context.getSource().sendFeedback(Text.literal("Successfully saved keybind profile for " + properClassName)
                .formatted(Formatting.GREEN));
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

            boolean hasProfile = profiles.containsKey(classType.getName()) && !profiles.get(classType.getName()).isEmpty();
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

    private static int executeExport(CommandContext<FabricClientCommandSource> context, ClassType classType) {
        if (classType == ClassType.NONE) {
            context.getSource().sendFeedback(Text.literal("Invalid class name.").formatted(Formatting.RED));
            return 0;
        }

        String properClassName = classType.getName().substring(0, 1).toUpperCase()
                + classType.getName().substring(1).toLowerCase();

        String code = ClassKeybindProfiles.exportProfile(classType.getName());
        if (code == null) {
            context.getSource().sendFeedback(Text.literal("No profile found for " + properClassName)
                    .formatted(Formatting.RED));
            return 0;
        }

        // Copy to clipboard
        MinecraftClient.getInstance().keyboard.setClipboard(code);

        context.getSource().sendFeedback(Text.literal("=== Profile Export ===").formatted(Formatting.GOLD));
        context.getSource().sendFeedback(Text.literal("Successfully exported profile for " + properClassName)
                .formatted(Formatting.GREEN));
        context.getSource().sendFeedback(Text.literal("Code copied to clipboard!").formatted(Formatting.YELLOW));
        context.getSource().sendFeedback(Text.literal("Share this code with others or use /ckp import <code> to restore it")
                .formatted(Formatting.GRAY));

        return 1;
    }

    private static int executeImport(CommandContext<FabricClientCommandSource> context, String code) {
        String className = ClassKeybindProfiles.importProfile(code);
        if (className == null) {
            context.getSource().sendFeedback(Text.literal("Failed to import profile. Invalid code format.")
                    .formatted(Formatting.RED));
            context.getSource().sendFeedback(Text.literal("Make sure the code starts with 'CKP_' and is not corrupted")
                    .formatted(Formatting.GRAY));
            return 0;
        }

        String properClassName = className.substring(0, 1).toUpperCase()
                + className.substring(1).toLowerCase();

        context.getSource().sendFeedback(Text.literal("=== Profile Import ===").formatted(Formatting.GOLD));
        context.getSource().sendFeedback(Text.literal("Successfully imported profile for " + properClassName)
                .formatted(Formatting.GREEN));
        context.getSource().sendFeedback(Text.literal("The profile will be applied when you switch to " + properClassName)
                .formatted(Formatting.GRAY));

        return 1;
    }
}