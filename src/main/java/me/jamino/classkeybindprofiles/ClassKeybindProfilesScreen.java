package me.jamino.classkeybindprofiles;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.toast.SystemToast;

import com.wynntils.models.character.type.ClassType;

import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class ClassKeybindProfilesScreen {

    private static final Map<String, String> WYNNTILS_NAMES = new HashMap<>();
    private static final Map<String, String> WYNNCRAFT_SPELL_CASTER_NAMES = new HashMap<>();
    private static final Map<String, String> BETTER_WYNN_MACROS_NAMES = new HashMap<>();

    static {
        // Wynntils friendly names (updated with correct keys)
        WYNNTILS_NAMES.put("Cast 1st Spell", "Cast 1st Spell");
        WYNNTILS_NAMES.put("Cast 2nd Spell", "Cast 2nd Spell");
        WYNNTILS_NAMES.put("Cast 3rd Spell", "Cast 3rd Spell");
        WYNNTILS_NAMES.put("Cast 4th Spell", "Cast 4th Spell");

        // Wynncraft Spell Caster friendly names
        WYNNCRAFT_SPELL_CASTER_NAMES.put("key.wynncraft-spell-caster.spell.first", "First Spell");
        WYNNCRAFT_SPELL_CASTER_NAMES.put("key.wynncraft-spell-caster.spell.second", "Second Spell");
        WYNNCRAFT_SPELL_CASTER_NAMES.put("key.wynncraft-spell-caster.spell.third", "Third Spell");
        WYNNCRAFT_SPELL_CASTER_NAMES.put("key.wynncraft-spell-caster.spell.fourth", "Fourth Spell");
        WYNNCRAFT_SPELL_CASTER_NAMES.put("key.wynncraft-spell-caster.spell.melee", "Melee Attack");
        WYNNCRAFT_SPELL_CASTER_NAMES.put("key.wynncraft-spell-caster.config", "Open Spell Caster Config");

        // BetterWynnMacros friendly names (updated with correct keys)
        BETTER_WYNN_MACROS_NAMES.put("key.ktnwynnmacros.spell.1", "Spell 1");
        BETTER_WYNN_MACROS_NAMES.put("key.ktnwynnmacros.spell.2", "Spell 2");
        BETTER_WYNN_MACROS_NAMES.put("key.ktnwynnmacros.spell.3", "Spell 3");
        BETTER_WYNN_MACROS_NAMES.put("key.ktnwynnmacros.spell.4", "Spell 4");
    }

    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.of("Class Keybind Profiles"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // Add the "Config" category
        ConfigCategory configCategory = builder.getOrCreateCategory(Text.of("Config"));

        // Add option to disable save toast notifications
        configCategory.addEntry(entryBuilder.startBooleanToggle(Text.of("Disable Profile Save Toasts"), ClassKeybindProfiles.config.isSaveToastNotificationsDisabled())
                .setDefaultValue(false)
                .setTooltip(Text.of("Disables toasts when saving/auto-saving profiles"))
                .setSaveConsumer(ClassKeybindProfiles.config::setDisableSaveToastNotifications)
                .build());

        // Add option to disable update toast notifications
        configCategory.addEntry(entryBuilder.startBooleanToggle(Text.of("Disable Profile Update Toasts"), ClassKeybindProfiles.config.isUpdateToastNotificationsDisabled())
                .setDefaultValue(false)
                .setTooltip(Text.of("Disables toasts when switching classes and loading profiles"))
                .setSaveConsumer(ClassKeybindProfiles.config::setDisableUpdateToastNotifications)
                .build());

        // Add option for auto-save
        configCategory.addEntry(entryBuilder.startBooleanToggle(Text.of("Auto-save on Class Change"), ClassKeybindProfiles.config.isAutoSaveOnClassChange())
                .setDefaultValue(true)
                .setTooltip(Text.of("Automatically saves keybinds when switching classes"))
                .setSaveConsumer(ClassKeybindProfiles.config::setAutoSaveOnClassChange)
                .build());

        // Add clear profile buttons for each class type
        for (ClassType classType : ClassType.values()) {
            if (classType == ClassType.NONE) continue; // Skip the NONE class type

            configCategory.addEntry(entryBuilder.startBooleanToggle(Text.of("Clear " + classType.getFullName() + " Profile"), false)
                    .setSaveConsumer(value -> {
                        if (value) {
                            ClassKeybindProfiles.clearProfileForClass(classType.getName());
                        }
                    })
                    .setDefaultValue(false)
                    .build());
        }

        // Add categories for each class type
        for (ClassType classType : ClassType.values()) {
            if (classType == ClassType.NONE) continue; // Skip the NONE class type

            ConfigCategory category = builder.getOrCreateCategory(Text.of(classType.getFullName()));

            Map<String, String> currentProfile = ClassKeybindProfiles.config.getProfile(classType.getName());
            if (currentProfile != null && !currentProfile.isEmpty()) {
                // Group keybinds by mod type
                Map<String, String> wynntilsKeybinds = currentProfile.entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith("Cast "))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                Map<String, String> wynncraftKeybinds = currentProfile.entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith("key.wynncraft-spell-caster"))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                Map<String, String> betterWynnKeybinds = currentProfile.entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith("key.ktnwynnmacros"))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                // Show Wynntils first (if any)
                if (!wynntilsKeybinds.isEmpty()) {
                    category.addEntry(entryBuilder.startTextDescription(Text.of("Wynntils:"))
                            .build());
                    for (Map.Entry<String, String> entry : wynntilsKeybinds.entrySet()) {
                        String friendlyName = WYNNTILS_NAMES.getOrDefault(entry.getKey(), entry.getKey());
                        String friendlyKey = getFriendlyKeyName(entry.getValue());
                        category.addEntry(entryBuilder.startTextDescription(Text.of("  " + friendlyName + ": " + friendlyKey))
                                .build());
                    }
                }

                // Show Wynncraft Spell Caster (if any)
                if (!wynncraftKeybinds.isEmpty()) {
                    category.addEntry(entryBuilder.startTextDescription(Text.of("Wynncraft Spell Caster:"))
                            .build());
                    for (Map.Entry<String, String> entry : wynncraftKeybinds.entrySet()) {
                        String friendlyName = WYNNCRAFT_SPELL_CASTER_NAMES.getOrDefault(entry.getKey(), entry.getKey());
                        String friendlyKey = getFriendlyKeyName(entry.getValue());
                        category.addEntry(entryBuilder.startTextDescription(Text.of("  " + friendlyName + ": " + friendlyKey))
                                .build());
                    }
                }

                // Show BetterWynnMacros (if any)
                if (!betterWynnKeybinds.isEmpty()) {
                    category.addEntry(entryBuilder.startTextDescription(Text.of("BetterWynnMacros:"))
                            .build());
                    for (Map.Entry<String, String> entry : betterWynnKeybinds.entrySet()) {
                        String friendlyName = BETTER_WYNN_MACROS_NAMES.getOrDefault(entry.getKey(), entry.getKey());
                        String friendlyKey = getFriendlyKeyName(entry.getValue());
                        category.addEntry(entryBuilder.startTextDescription(Text.of("  " + friendlyName + ": " + friendlyKey))
                                .build());
                    }
                }
            } else {
                category.addEntry(entryBuilder.startTextDescription(Text.of("No saved keybinds"))
                        .build());
            }

            category.addEntry(entryBuilder.startBooleanToggle(Text.of("Save Current Keybinds"), false)
                    .setSaveConsumer(value -> {
                        if (value) {
                            ClassKeybindProfiles.saveCurrentKeybindsForClass(classType.getName());
                        }
                    })
                    .setDefaultValue(false)
                    .build());
        }

        builder.setSavingRunnable(ClassKeybindProfiles::saveConfig);

        return builder.build();
    }

    private static String getFriendlyKeyName(String keyValue) {
        if (keyValue.startsWith("key.keyboard.")) {
            return capitalize(keyValue.substring("key.keyboard.".length()));
        } else if (keyValue.startsWith("key.mouse.")) {
            return "Mouse Button " + keyValue.substring("key.mouse.".length());
        } else if (keyValue.equals("key.keyboard.unknown")) {
            return "Unbound";
        }
        return keyValue;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}