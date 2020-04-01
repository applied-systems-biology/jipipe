package org.hkijena.acaq5.utils;

/**
 * Helper for optional macro flags
 */
public class MacroFlag {
    private String key;
    private boolean visible;

    /**
     * @param key     macro parameter key
     * @param visible if the parameter is visible
     */
    public MacroFlag(String key, boolean visible) {
        this.key = key;
        this.visible = visible;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public String toString() {
        if (visible)
            return key;
        else
            return "";
    }
}
