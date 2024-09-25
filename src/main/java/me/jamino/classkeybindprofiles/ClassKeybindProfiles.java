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
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

// Wynntils imports
import com.wynntils.core.components.Models;
import com.wynntils.models.character.CharacterModel;
import com.wynntils.models.character.type.ClassType;

@Environment(EnvType.CLIENT)
public class ClassKeybindProfiles implements ClientModInitializer, ModMenuApi {
    public static final String MOD_ID = "classkeybindprofiles";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static ClassKeybindProfilesConfig config;
    public static final ClassType[] CLASS_TYPES = ClassType.values();
    private static boolean wynntilsLoaded = false;
    private String lastClass = "NONE";
    private int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20;
    private boolean keybindsUpdated = false;

    @Override
    public void onInitializeClient() {
        AutoConfig.register(ClassKeybindProfilesConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ClassKeybindProfilesConfig.class).getConfig();

        wynntilsLoaded = FabricLoader.getInstance().isModLoaded("wynntils");
        if (!wynntilsLoaded) {
            LOGGER.warn("Wynntils mod not detected. Class detection will not work.");
            return;
        }

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player != null) {
            tickCounter++;
            if (tickCounter >= CHECK_INTERVAL) {
                checkForClassChange(client);
                tickCounter = 0;
            }
        }
    }

    private void checkForClassChange(MinecraftClient client) {
        String currentClass = getCurrentClass();
        if (!currentClass.equals(lastClass) && !currentClass.equals("NONE")) {
            LOGGER.info("Class changed from " + lastClass + " to " + currentClass);
            lastClass = currentClass;
            keybindsUpdated = false;  // Reset the flag when class changes
        }

        if (!keybindsUpdated && !currentClass.equals("NONE")) {
            updateKeybindProfile(client, currentClass);
            keybindsUpdated = true;  // Set the flag after updating keybinds
        }
    }

    private void updateKeybindProfile(MinecraftClient client, String currentClass) {
        ClassType classType = ClassType.fromName(currentClass);
        Map<String, String> savedProfile = config.getProfile(classType.getName());
        LOGGER.info("Updating keybind profile for " + currentClass + ". Saved profile: " + savedProfile);

        if (savedProfile != null && !savedProfile.isEmpty()) {
            boolean changes = false;
            for (KeyBinding keyBinding : client.options.allKeys) {
                String savedKey = savedProfile.get(keyBinding.getTranslationKey());
                if (savedKey != null) {
                    InputUtil.Key key = InputUtil.fromTranslationKey(savedKey);
                    if (key != null && !keyBinding.getBoundKeyTranslationKey().equals(savedKey)) {
                        LOGGER.info("Updating keybinding: " + keyBinding.getTranslationKey() + " from " + keyBinding.getBoundKeyTranslationKey() + " to " + savedKey);
                        keyBinding.setBoundKey(key);
                        changes = true;
                    }
                }
            }

            if (changes) {
                // Force a refresh of the keybinding system
                KeyBinding.updateKeysByCode();

                // Update the internal state of keybindings
                for (KeyBinding keyBinding : client.options.allKeys) {
                    keyBinding.setPressed(false);
                }

                // Save changes to options.txt
                client.options.write();

                // Show a toast notification for keybind update
                client.execute(() -> {
                    SystemToast.add(
                            client.getToastManager(),
                            SystemToast.Type.WORLD_BACKUP,
                            Text.of("Keybind Profile Updated"),
                            Text.of("Keybind profile updated for " + currentClass)
                    );
                });

                LOGGER.info("Updated keybind profile for " + currentClass);
            }
        } else {
            LOGGER.warn("No saved profile found for " + currentClass);
        }
    }

    private String getCurrentClass() {
        if (!wynntilsLoaded) {
            return "NONE";
        }

        try {
            CharacterModel character = Models.Character;
            if (character.hasCharacter()) {
                ClassType classType = character.getClassType();
                boolean isReskinned = ClassType.isReskinned(character.getActualName());
                return classType.getActualName(isReskinned);
            } else {
                return "NONE";
            }
        } catch (Exception e) {
            LOGGER.error("Error while trying to get current class from Wynntils", e);
            return "NONE";
        }
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ClassKeybindProfilesScreen::createConfigScreen;
    }

    public static void saveConfig() {
        AutoConfig.getConfigHolder(ClassKeybindProfilesConfig.class).save();
    }

    public static void saveCurrentKeybindsForClass(String className) {
        ClassType classType = ClassType.fromName(className);
        Map<String, String> currentKeybinds = Arrays.stream(MinecraftClient.getInstance().options.allKeys)
                .filter(kb -> kb.getTranslationKey().startsWith("key.wynncraft-spell-caster"))
                .collect(Collectors.toMap(
                        KeyBinding::getTranslationKey,
                        KeyBinding::getBoundKeyTranslationKey
                ));
        config.saveProfile(classType.getName(), currentKeybinds);
        saveConfig();
        LOGGER.info("Saved current keybinds for class " + className + ": " + currentKeybinds);
    }
}