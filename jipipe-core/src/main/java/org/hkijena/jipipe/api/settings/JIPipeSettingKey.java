/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.settings;

import java.util.Objects;

/**
 * A typed reference to one setting: one sub-path within one block of a
 * scoped settings document.
 * <p>
 * Keys are created through the static type factories
 * ({@link #bool(String, String, Class)}, {@link #string(String, String, Class)},
 * {@link #integer(String, String, Class)}, {@link #doub(String, String, Class)},
 * {@link #object(String, String, Class)}) and configured with the fluent
 * builder methods, which mutate the key and return {@code this}. The factory
 * already applies the defaults — {@code scope = APPLICATION},
 * {@code access = USER_EDITABLE}, {@code migrate = true}, empty name and
 * description, and a default value of {@code null}, which
 * {@code object(...)} keys may keep and which keys for primitive wrapper
 * types should replace via {@link #defaultValue(Object)} — so a key is
 * usually written as one fluent chain assigned to a {@code static final}
 * constant.
 * <p>
 * The block path is given as the full canonical block path including its
 * kind prefix (e.g. {@code "registry:ui-graph-editor"}). The sub-path is
 * slash-separated and addressed relative to the block, so
 * {@code "a//b"} and {@code "a/b"} denote the same location.
 *
 * @param <T> the Java type of the setting's value
 */
public final class JIPipeSettingKey<T> {
    private final String blockPath;
    private final String subPath;
    private final Class<T> type;

    private T defaultValue;
    private SettingsScope scope;
    private SettingsAccessPolicy access;
    private String name;
    private String description;
    private boolean migrate;

    private JIPipeSettingKey(String blockPath, String subPath, Class<T> type) {
        this.blockPath = Objects.requireNonNull(blockPath, "blockPath");
        this.subPath = Objects.requireNonNull(subPath, "subPath");
        this.type = Objects.requireNonNull(type, "type");
        this.scope = SettingsScope.APPLICATION;
        this.access = SettingsAccessPolicy.USER_EDITABLE;
        this.name = "";
        this.description = "";
        this.migrate = true;
    }

    /**
     * Creates a key for a boolean setting.
     *
     * @param blockPath the full canonical block path, including its kind prefix
     * @param subPath slash-separated sub-path within the block
     * @param type the value type, e.g. {@link Boolean}
     */
    public static JIPipeSettingKey<Boolean> bool(String blockPath, String subPath, Class<Boolean> type) {
        return new JIPipeSettingKey<>(blockPath, subPath, type);
    }

    /**
     * Creates a key for a string setting.
     *
     * @param blockPath the full canonical block path, including its kind prefix
     * @param subPath slash-separated sub-path within the block
     * @param type the value type, e.g. {@link String}
     */
    public static JIPipeSettingKey<String> string(String blockPath, String subPath, Class<String> type) {
        return new JIPipeSettingKey<>(blockPath, subPath, type);
    }

    /**
     * Creates a key for an integer setting.
     *
     * @param blockPath the full canonical block path, including its kind prefix
     * @param subPath slash-separated sub-path within the block
     * @param type the value type, e.g. {@link Integer}
     */
    public static JIPipeSettingKey<Integer> integer(String blockPath, String subPath, Class<Integer> type) {
        return new JIPipeSettingKey<>(blockPath, subPath, type);
    }

    /**
     * Creates a key for a double setting.
     *
     * @param blockPath the full canonical block path, including its kind prefix
     * @param subPath slash-separated sub-path within the block
     * @param type the value type, e.g. {@link Double}
     */
    public static JIPipeSettingKey<Double> doub(String blockPath, String subPath, Class<Double> type) {
        return new JIPipeSettingKey<>(blockPath, subPath, type);
    }

    /**
     * Creates a key for a structured setting, e.g. one whose value is stored
     * and returned as a raw JSON node.
     *
     * @param blockPath the full canonical block path, including its kind prefix
     * @param subPath slash-separated sub-path within the block
     * @param type the value type, e.g. {@code JsonNode.class}
     */
    public static <T> JIPipeSettingKey<T> object(String blockPath, String subPath, Class<T> type) {
        return new JIPipeSettingKey<>(blockPath, subPath, type);
    }

    /**
     * Sets the value reported when the setting is absent.
     *
     * @return this key
     */
    public JIPipeSettingKey<T> defaultValue(T value) {
        this.defaultValue = value;
        return this;
    }

    /**
     * Sets the scope this setting lives in.
     *
     * @return this key
     */
    public JIPipeSettingKey<T> scope(SettingsScope value) {
        this.scope = Objects.requireNonNull(value, "value");
        return this;
    }

    /**
     * Sets who may write the setting.
     *
     * @return this key
     */
    public JIPipeSettingKey<T> access(SettingsAccessPolicy value) {
        this.access = Objects.requireNonNull(value, "value");
        return this;
    }

    /**
     * Sets the user-facing display name.
     *
     * @return this key
     */
    public JIPipeSettingKey<T> name(String value) {
        this.name = Objects.requireNonNull(value, "value");
        return this;
    }

    /**
     * Sets the user-facing description.
     *
     * @return this key
     */
    public JIPipeSettingKey<T> description(String value) {
        this.description = Objects.requireNonNull(value, "value");
        return this;
    }

    /**
     * Sets whether the value should be migrated from the legacy settings
     * store at first load.
     *
     * @return this key
     */
    public JIPipeSettingKey<T> migrate(boolean value) {
        this.migrate = value;
        return this;
    }

    /**
     * @return the full canonical block path, including its kind prefix
     */
    public String blockPath() {
        return blockPath;
    }

    /**
     * @return the slash-separated sub-path within the block
     */
    public String subPath() {
        return subPath;
    }

    /**
     * @return the Java type of the setting's value
     */
    public Class<T> type() {
        return type;
    }

    /**
     * @return the value reported when the setting is absent
     */
    public T defaultValue() {
        return defaultValue;
    }

    /**
     * @return the scope this setting lives in
     */
    public SettingsScope scope() {
        return scope;
    }

    /**
     * @return who may write the setting
     */
    public SettingsAccessPolicy access() {
        return access;
    }

    /**
     * @return the user-facing display name, possibly empty
     */
    public String name() {
        return name;
    }

    /**
     * @return the user-facing description, possibly empty
     */
    public String description() {
        return description;
    }

    /**
     * @return whether the value should be migrated from the legacy settings
     * store at first load
     */
    public boolean migrates() {
        return migrate;
    }

    @Override
    public String toString() {
        return blockPath + "/" + subPath;
    }
}
