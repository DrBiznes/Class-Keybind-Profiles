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
    private boolean disableSaveToastNotifications = false;

    @ConfigEntry.Gui.Tooltip
    private boolean disableUpdateToastNotifications = false;

    @ConfigEntry.Gui.Tooltip
    private boolean autoSaveOnClassChange = true;

    public void saveProfile(String className, Map<String, String> keybinds) {
        profiles.put(className, new HashMap<>(keybinds));
    }

    public Map<String, String> getProfile(String className) {
        return profiles.get(className);
    }

    public Map<String, Map<String, String>> getAllProfiles() {
        return new HashMap<>(profiles);
    }

    public boolean isSaveToastNotificationsDisabled() {
        return disableSaveToastNotifications;
    }

    public void setDisableSaveToastNotifications(boolean disableSaveToastNotifications) {
        this.disableSaveToastNotifications = disableSaveToastNotifications;
    }

    public boolean isUpdateToastNotificationsDisabled() {
        return disableUpdateToastNotifications;
    }

    public void setDisableUpdateToastNotifications(boolean disableUpdateToastNotifications) {
        this.disableUpdateToastNotifications = disableUpdateToastNotifications;
    }

    public boolean isAutoSaveOnClassChange() {
        return autoSaveOnClassChange;
    }

    public void setAutoSaveOnClassChange(boolean autoSaveOnClassChange) {
        this.autoSaveOnClassChange = autoSaveOnClassChange;
    }

    public void clearProfile(String className) {
        profiles.remove(className);
    }
}