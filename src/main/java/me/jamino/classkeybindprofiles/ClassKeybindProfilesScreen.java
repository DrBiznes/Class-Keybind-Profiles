package me.jamino.classkeybindprofiles;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.toast.SystemToast;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassKeybindProfilesScreen {
    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.of("Class Keybind Profiles"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        for (String classType : ClassKeybindProfiles.CLASS_TYPES) {
            ConfigCategory category = builder.getOrCreateCategory(Text.of(classType));

            category.addEntry(entryBuilder.startTextDescription(Text.of("Current keybinds:"))
                    .build());

            Map<String, String> currentKeybinds = getCurrentKeybinds();
            for (Map.Entry<String, String> entry : currentKeybinds.entrySet()) {
                category.addEntry(entryBuilder.startTextDescription(Text.of(entry.getKey() + ": " + entry.getValue()))
                        .build());
            }

            category.addEntry(entryBuilder.startBooleanToggle(Text.of("Save Current Keybinds"), false)
                    .setSaveConsumer(value -> {
                        if (value) {
                            Map<String, String> keybindsToSave = getCurrentKeybinds();
                            ClassKeybindProfiles.config.saveProfile(classType, keybindsToSave);
                            ClassKeybindProfiles.LOGGER.info("Saved keybind profile for " + classType);

                            // Show a toast notification
                            MinecraftClient.getInstance().execute(() -> {
                                SystemToast.add(
                                        MinecraftClient.getInstance().getToastManager(),
                                        SystemToast.Type.PERIODIC_NOTIFICATION,
                                        Text.of("Profile Saved"),
                                        Text.of("Keybind profile saved for " + classType)
                                );
                            });
                        }
                    })
                    .setDefaultValue(false)
                    .build());
        }

        builder.setSavingRunnable(() -> {
            try {
                ClassKeybindProfiles.config.save();
            } catch (Exception e) {
                ClassKeybindProfiles.LOGGER.error("Failed to save config", e);
            }
        });

        return builder.build();
    }

    private static Map<String, String> getCurrentKeybinds() {
        Map<String, String> keybinds = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(MinecraftClient.getInstance().runDirectory.getPath(), "options.txt"));
            for (String line : lines) {
                if (line.startsWith("key_key.wynncraft-spell-caster")) {
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        keybinds.put(parts[0], parts[1]);
                    }
                }
            }
        } catch (Exception e) {
            ClassKeybindProfiles.LOGGER.error("Failed to read options.txt", e);
        }
        return keybinds;
    }
}