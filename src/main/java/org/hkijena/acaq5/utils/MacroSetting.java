package org.hkijena.acaq5.utils;

/**
 * Helper class for ImageJ macro parameters
 */
public class MacroSetting {
    private String key;
    private String value;

    /**
     * @param key   setting key
     * @param value setting value
     */
    public MacroSetting(String key, Object value) {
        this.key = key;
        this.value = "" + value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return key + "=" + value;
    }
}
