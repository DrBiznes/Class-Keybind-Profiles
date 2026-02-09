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
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

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
        ClassKeybindProfilesCommands.register();

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

            // Auto-save keybinds for the previous class if enabled
            if (config.isAutoSaveOnClassChange() && !lastClass.equals("NONE")) {
                saveCurrentKeybindsForClass(lastClass);
                LOGGER.info("Auto-saved keybinds for " + lastClass);

                // Show toast notification if not disabled
                if (!config.isSaveToastNotificationsDisabled()) {
                    client.getToastManager().add(
                            SystemToast.create(client,
                                    SystemToast.Type.WORLD_BACKUP,
                                    Text.literal("Class Keybind Profiles"),
                                    Text.literal("Auto-saved keybinds for " + lastClass)));
                }
            }

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
                String savedKey = savedProfile.get(keyBinding.getId());
                if (savedKey != null) {
                    InputUtil.Key key = InputUtil.fromTranslationKey(savedKey);
                    if (key != null && !keyBinding.getBoundKeyTranslationKey().equals(savedKey)) {
                        LOGGER.info("Updating keybinding: " + keyBinding.getId() + " from " + keyBinding.getBoundKeyTranslationKey() + " to " + savedKey);
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

                // Show a toast notification for keybind update if not disabled
                if (!config.isUpdateToastNotificationsDisabled()) {
                    client.execute(() -> {
                        SystemToast.add(
                                client.getToastManager(),
                                SystemToast.Type.WORLD_BACKUP,
                                Text.of("Keybind Profile Updated"),
                                Text.of("Keybind profile updated for " + currentClass)
                        );
                    });
                }

                LOGGER.info("Updated keybind profile for " + currentClass);
            }
        } else {
            LOGGER.warn("No saved profile found for " + currentClass);
        }
    }

    public static String getCurrentClass() {
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
        Map<String, String> currentKeybinds = getAllSupportedKeybinds();

        config.saveProfile(classType.getName(), currentKeybinds);
        saveConfig();
        LOGGER.info("Saved current keybinds for class " + className + ": " + currentKeybinds);

        // Show a toast notification if not disabled
        if (!config.isSaveToastNotificationsDisabled()) {
            MinecraftClient.getInstance().execute(() -> {
                SystemToast.add(
                        MinecraftClient.getInstance().getToastManager(),
                        SystemToast.Type.WORLD_BACKUP,
                        Text.of("Profile Saved"),
                        Text.of("Keybind profile saved for " + className)
                );
            });
        }
    }

    private static Map<String, String> getAllSupportedKeybinds() {
        return Arrays.stream(MinecraftClient.getInstance().options.allKeys)
                .filter(kb -> {
                    String key = kb.getId();
                    return key.startsWith("key.wynncraft-spell-caster") ||
                            key.startsWith("Cast ") ||  // Wynntils: "Cast 1st Spell", etc.
                            key.startsWith("key.ktnwynnmacros");  // Fixed: BetterWynnMacros uses "key.ktnwynnmacros", not "key_key.ktnwynnmacros"
                })
                .collect(Collectors.toMap(
                        KeyBinding::getId,
                        KeyBinding::getBoundKeyTranslationKey
                ));
    }

    public static void clearProfileForClass(String className) {
        config.clearProfile(className);
        saveConfig();
        LOGGER.info("Cleared keybind profile for class " + className);

        // Show a toast notification if not disabled
        if (!config.isSaveToastNotificationsDisabled()) {
            MinecraftClient.getInstance().execute(() -> {
                SystemToast.add(
                        MinecraftClient.getInstance().getToastManager(),
                        SystemToast.Type.WORLD_BACKUP,
                        Text.of("Profile Cleared"),
                        Text.of("Keybind profile cleared for " + className)
                );
            });
        }
    }

    /**
     * Exports a keybind profile as an encoded string
     * @param className The class name to export
     * @return The encoded profile string with "CKP_" prefix, or null if profile doesn't exist
     */
    public static String exportProfile(String className) {
        Map<String, String> profile = config.getProfile(className);
        if (profile == null || profile.isEmpty()) {
            return null;
        }

        try {
            // Create a wrapper object that includes the class name
            Map<String, Object> exportData = new HashMap<>();
            exportData.put("className", className);
            exportData.put("keybinds", profile);

            // Convert to JSON
            Gson gson = new Gson();
            String json = gson.toJson(exportData);

            // Encode to Base64
            String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

            // Add prefix for validation
            return "CKP_" + encoded;
        } catch (Exception e) {
            LOGGER.error("Failed to export profile for " + className, e);
            return null;
        }
    }

    /**
     * Imports a keybind profile from an encoded string
     * @param code The encoded profile string
     * @return The class name of the imported profile, or null if import failed
     */
    public static String importProfile(String code) {
        try {
            // Validate prefix
            if (!code.startsWith("CKP_")) {
                LOGGER.error("Invalid profile code: missing CKP_ prefix");
                return null;
            }

            // Remove prefix
            String encoded = code.substring(4);

            // Decode from Base64
            byte[] decoded = Base64.getDecoder().decode(encoded);
            String json = new String(decoded, StandardCharsets.UTF_8);

            // Parse JSON
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> exportData = gson.fromJson(json, type);

            // Extract class name and keybinds
            String className = (String) exportData.get("className");
            if (className == null) {
                LOGGER.error("Invalid profile code: missing className");
                return null;
            }

            // Convert keybinds (Gson might parse as Map<String, Object>)
            Object keybindsObj = exportData.get("keybinds");
            if (keybindsObj == null) {
                LOGGER.error("Invalid profile code: missing keybinds");
                return null;
            }

            Map<String, String> keybinds = new HashMap<>();
            if (keybindsObj instanceof Map) {
                Map<?, ?> keybindsMap = (Map<?, ?>) keybindsObj;
                for (Map.Entry<?, ?> entry : keybindsMap.entrySet()) {
                    keybinds.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }

            // Save the profile
            config.saveProfile(className, keybinds);
            saveConfig();

            LOGGER.info("Imported keybind profile for " + className + " with " + keybinds.size() + " keybinds");
            return className;

        } catch (Exception e) {
            LOGGER.error("Failed to import profile", e);
            return null;
        }
    }
}