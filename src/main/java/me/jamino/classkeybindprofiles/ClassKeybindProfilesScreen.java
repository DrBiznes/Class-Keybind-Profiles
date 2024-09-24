package me.jamino.classkeybindprofiles;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.toast.SystemToast;

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

            Map<String, String> currentProfile = ClassKeybindProfiles.config.getProfile(classType);
            if (currentProfile != null) {
                for (Map.Entry<String, String> entry : currentProfile.entrySet()) {
                    category.addEntry(entryBuilder.startTextDescription(Text.of(entry.getKey() + ": " + entry.getValue()))
                            .build());
                }
            }

            category.addEntry(entryBuilder.startBooleanToggle(Text.of("Save Current Keybinds"), false)
                    .setSaveConsumer(value -> {
                        if (value) {
                            ClassKeybindProfiles.saveCurrentKeybindsForClass(classType);
                            ClassKeybindProfiles.LOGGER.info("Saved keybind profile for " + classType);

                            // Show a toast notification
                            MinecraftClient.getInstance().execute(() -> {
                                SystemToast.add(
                                        MinecraftClient.getInstance().getToastManager(),
                                        SystemToast.Type.WORLD_BACKUP,
                                        Text.of("Profile Saved"),
                                        Text.of("Keybind profile saved for " + classType)
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
}