package me.jamino.classkeybindprofiles;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.util.*;

// Wynntils imports
import com.wynntils.core.components.Models;
import com.wynntils.models.character.CharacterModel;

public class ClassKeybindProfiles implements ClientModInitializer, ModMenuApi {
    public static final String MOD_ID = "classkeybindprofiles";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static ClassKeybindProfilesConfig config;
    public static final String[] CLASS_TYPES = {"ARCHER", "ASSASSIN", "MAGE", "SHAMAN", "WARRIOR"};
    private static boolean wynntilsLoaded = false;

    @Override
    public void onInitializeClient() {
        AutoConfig.register(ClassKeybindProfilesConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ClassKeybindProfilesConfig.class).getConfig();

        wynntilsLoaded = FabricLoader.getInstance().isModLoaded("wynntils");
        if (!wynntilsLoaded) {
            LOGGER.warn("Wynntils mod not detected. Class detection will not work.");
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.world != null) {
                checkAndUpdateKeybinds(client);
            }
        });
    }

    private void checkAndUpdateKeybinds(MinecraftClient client) {
        String currentClass = getCurrentClass();
        if (currentClass != null && !currentClass.equals("NONE")) {
            Map<String, String> savedProfile = config.getProfile(currentClass);
            if (savedProfile != null) {
                updateOptionsFile(savedProfile);
                client.options.load();
                client.player.sendMessage(Text.of("Keybind profile updated for " + currentClass), true);
            }
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