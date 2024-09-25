package me.jamino.classkeybindprofiles;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.HashMap;
import java.util.Map;

@Config(name = "classkeybindprofiles")
public class ClassKeybindProfilesConfig implements ConfigData {

    @ConfigEntry.Gui.Excluded
    private Map<String, Map<String, String>> profiles = new HashMap<>();

    @ConfigEntry.Gui.Tooltip
    private boolean disableToastNotifications = false;

    public void saveProfile(String className, Map<String, String> keybinds) {
        profiles.put(className, new HashMap<>(keybinds));
    }

    public Map<String, String> getProfile(String className) {
        return profiles.get(className);
    }

    public Map<String, Map<String, String>> getAllProfiles() {
        return new HashMap<>(profiles);
    }

    public boolean isToastNotificationsDisabled() {
        return disableToastNotifications;
    }

    public void setDisableToastNotifications(boolean disableToastNotifications) {
        this.disableToastNotifications = disableToastNotifications;
    }

    public void clearProfile(String className) {
        profiles.remove(className);
    }
}