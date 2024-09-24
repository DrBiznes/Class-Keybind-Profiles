package me.jamino.classkeybindprofiles;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.HashMap;
import java.util.Map;

@Config(name = ClassKeybindProfiles.MOD_ID)
public class ClassKeybindProfilesConfig implements ConfigData {

    @ConfigEntry.Gui.Excluded
    private Map<String, Map<String, String>> profiles = new HashMap<>();

    public void saveProfile(String className, Map<String, String> keybinds) {
        profiles.put(className, new HashMap<>(keybinds));
        ClassKeybindProfiles.LOGGER.info("Saving profile for " + className + ": " + keybinds);
        save();
    }

    public Map<String, String> getProfile(String className) {
        return profiles.get(className);
    }

    public Map<String, Map<String, String>> getAllProfiles() {
        return new HashMap<>(profiles);
    }

    public void save() {
        ClassKeybindProfiles.LOGGER.info("Saving config to disk");
        AutoConfig.getConfigHolder(ClassKeybindProfilesConfig.class).save();
    }
}