package me.jamino.classkeybindprofiles;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.util.*;

// Wynntils imports
import com.wynntils.core.components.Models;
import com.wynntils.models.character.CharacterModel;

@Environment(EnvType.CLIENT)
public class ClassKeybindProfiles implements ClientModInitializer, ModMenuApi {
    public static final String MOD_ID = "classkeybindprofiles";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static ClassKeybindProfilesConfig config;
    public static final String[] CLASS_TYPES = {"ARCHER", "ASSASSIN", "MAGE", "SHAMAN", "WARRIOR"};
    private static boolean wynntilsLoaded = false;
    private String lastClass = "NONE";  // Store the last detected class

    @Override
    public void onInitializeClient() {
        // Register the config with AutoConfig
        AutoConfig.register(ClassKeybindProfilesConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ClassKeybindProfilesConfig.class).getConfig();

        wynntilsLoaded = FabricLoader.getInstance().isModLoaded("wynntils");
        if (!wynntilsLoaded) {
            LOGGER.warn("Wynntils mod not detected. Class detection will not work.");
            return;
        }

        // Register a tick event handler to check for class changes
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                checkForClassChange(client);
            }
        });
    }

    // Method that checks if the player's class has changed
    private void checkForClassChange(MinecraftClient client) {
        String currentClass = getCurrentClass();
        if (!currentClass.equals(lastClass) && !currentClass.equals("NONE")) {
            // Class has changed, update keybind profile
            updateKeybindProfile(client, currentClass);
            lastClass = currentClass;  // Update the lastClass to prevent redundant updates
        }
    }

    private void updateKeybindProfile(MinecraftClient client, String currentClass) {
        Map<String, String> savedProfile = config.getProfile(currentClass);
        if (savedProfile != null) {
            updateOptionsFile(savedProfile);
            client.options.load();

            // Show a toast notification for keybind update
            client.execute(() -> {
                SystemToast.add(
                        client.getToastManager(),
                        SystemToast.Type.WORLD_BACKUP,  // Use a valid toast type
                        Text.of("Keybind profile updated"),
                        Text.of("Keybind profile updated for " + currentClass)
                );
            });
        }
    }

    private String getCurrentClass() {
        if (!wynntilsLoaded) {
            LOGGER.warn("Wynntils not loaded. Cannot detect current class.");
            return "NONE";
        }

        try {
            CharacterModel character = Models.Character;
            if (character.hasCharacter()) {
                String className = character.getActualName();
                LOGGER.info("The player is currently playing: " + className);
                return className.toUpperCase();
            } else {
                LOGGER.info("The player has no active character.");
                return "NONE";
            }
        } catch (Exception e) {
            LOGGER.error("Error while trying to get current class from Wynntils", e);
            return "NONE";
        }
    }

    private void updateOptionsFile(Map<String, String> profile) {
        try {
            Path optionsPath = Paths.get(MinecraftClient.getInstance().runDirectory.getPath(), "options.txt");
            List<String> lines = Files.readAllLines(optionsPath);
            List<String> updatedLines = new ArrayList<>();

            for (String line : lines) {
                String updatedLine = line;
                for (Map.Entry<String, String> entry : profile.entrySet()) {
                    if (line.startsWith(entry.getKey())) {
                        updatedLine = entry.getKey() + ":" + entry.getValue();
                        break;
                    }
                }
                updatedLines.add(updatedLine);
            }

            Files.write(optionsPath, updatedLines);
        } catch (IOException e) {
            LOGGER.error("Failed to update options.txt", e);
        }
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ClassKeybindProfilesScreen::createConfigScreen;
    }
}
