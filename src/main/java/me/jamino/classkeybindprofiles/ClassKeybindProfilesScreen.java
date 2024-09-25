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

public class ClassKeybindProfilesScreen {

    private static final Map<String, String> FRIENDLY_NAMES = new HashMap<>();

    static {
        FRIENDLY_NAMES.put("key.wynncraft-spell-caster.spell.first", "First Spell");
        FRIENDLY_NAMES.put("key.wynncraft-spell-caster.spell.second", "Second Spell");
        FRIENDLY_NAMES.put("key.wynncraft-spell-caster.spell.third", "Third Spell");
        FRIENDLY_NAMES.put("key.wynncraft-spell-caster.spell.fourth", "Fourth Spell");
        FRIENDLY_NAMES.put("key.wynncraft-spell-caster.spell.melee", "Melee Attack");
        FRIENDLY_NAMES.put("key.wynncraft-spell-caster.config", "Open Spell Caster Config");
    }

    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.of("Class Keybind Profiles"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        for (ClassType classType : ClassType.values()) {
            if (classType == ClassType.NONE) continue; // Skip the NONE class type

            ConfigCategory category = builder.getOrCreateCategory(Text.of(classType.getFullName()));

            category.addEntry(entryBuilder.startTextDescription(Text.of("Current keybinds:"))
                    .build());

            Map<String, String> currentProfile = ClassKeybindProfiles.config.getProfile(classType.getName());
            if (currentProfile != null) {
                for (Map.Entry<String, String> entry : currentProfile.entrySet()) {
                    String friendlyName = getFriendlyName(entry.getKey());
                    String friendlyKey = getFriendlyKeyName(entry.getValue());
                    category.addEntry(entryBuilder.startTextDescription(Text.of(friendlyName + ": " + friendlyKey))
                            .build());
                }
            }

            category.addEntry(entryBuilder.startBooleanToggle(Text.of("Save Current Keybinds"), false)
                    .setSaveConsumer(value -> {
                        if (value) {
                            ClassKeybindProfiles.saveCurrentKeybindsForClass(classType.getName());
                            ClassKeybindProfiles.LOGGER.info("Saved keybind profile for " + classType.getFullName());

                            // Show a toast notification
                            MinecraftClient.getInstance().execute(() -> {
                                SystemToast.add(
                                        MinecraftClient.getInstance().getToastManager(),
                                        SystemToast.Type.WORLD_BACKUP,
                                        Text.of("Profile Saved"),
                                        Text.of("Keybind profile saved for " + classType.getFullName())
                                );
                            });
                        }
                    })
                    .setDefaultValue(false)
                    .build());
        }

        builder.setSavingRunnable(ClassKeybindProfiles::saveConfig);

        return builder.build();
    }

    private static String getFriendlyName(String key) {
        return FRIENDLY_NAMES.getOrDefault(key, key);
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