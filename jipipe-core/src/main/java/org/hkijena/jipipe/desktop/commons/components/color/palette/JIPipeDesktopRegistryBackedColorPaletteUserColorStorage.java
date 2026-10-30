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

package org.hkijena.jipipe.desktop.commons.components.color.palette;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.settings.JIPipeSettingKey;
import org.hkijena.jipipe.api.settings.SettingsBinding;
import org.hkijena.jipipe.plugins.settings.SettingsKeys;
import org.hkijena.jipipe.utils.json.JIPipePathMetadataStore;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.*;

public class JIPipeDesktopRegistryBackedColorPaletteUserColorStorage implements JIPipeDesktopColorPaletteUserColorStorage {

    /**
     * Deserialized lists that we are working on.
     * Assuming that we call them from EDT, should not have thread safety issues
     */
    private static final Map<String, List<JIPipeDesktopColorPaletteColor>> LISTS = new HashMap<>();

    private final JIPipeDesktopColorPaletteUI paletteUI;
    private final JIPipePathMetadataStore.Key registryStoragePath;
    private final List<JIPipeDesktopColorPaletteColor> paletteColors;
    /**
     * The binding to the palette's settings key, created once per storage
     * instance (one per palette UI) and released by {@link #dispose()}. The
     * value is held as a raw JSON node (see {@link SettingsKeys#USER_PALETTE_COLORS_FG})
     * and converted with the typed list reader the legacy path used, so the
     * stored shape stays byte-identical.
     */
    private SettingsBinding<JsonNode> colorsBinding;

    public JIPipeDesktopRegistryBackedColorPaletteUserColorStorage(JIPipeDesktopColorPaletteUI paletteUI) {
        this.paletteUI = paletteUI;
        this.registryStoragePath = getRegistryStoragePath(paletteUI);
        this.paletteColors = loadFromRegistry(getDatabaseId(), registryStoragePath);
    }

    /**
     * Returns the binding to this storage's palette setting, registering the
     * palette's key on first use. Created once per storage instance — never
     * per read/write — because a binding owns a document path listener.
     * <p>
     * Without a running settings service (e.g. headless tests) no binding is
     * created and the palette works in memory.
     *
     * @return the binding, or {@code null} if the settings service is absent
     */
    private SettingsBinding<JsonNode> colorsBinding() {
        if (colorsBinding == null && JIPipe.isInstantiated()) {
            colorsBinding = JIPipe.getSettingsService().bind(getKey(registryStoragePath));
        }
        return colorsBinding;
    }

    /**
     * Releases the binding (its document path listener). Called when the
     * owning palette UI dies; until then the binding is tracked by the
     * settings service, which closes it as a fallback. Closing is idempotent,
     * and reads/writes still work on a closed binding (it only stops
     * receiving change events).
     */
    public void dispose() {
        if (colorsBinding != null) {
            colorsBinding.close();
            colorsBinding = null;
        }
    }

    /**
     * @param registryStoragePath this storage's palette variant sub-path
     * @return the settings key for this storage's palette variant
     */
    private static JIPipeSettingKey<JsonNode> getKey(JIPipePathMetadataStore.Key registryStoragePath) {
        String path = String.join("/", registryStoragePath.getItems());
        switch (path) {
            case "user-palette-colors/fg-a":
                return SettingsKeys.USER_PALETTE_COLORS_FG_A;
            case "user-palette-colors/fg-bg":
                return SettingsKeys.USER_PALETTE_COLORS_FG_BG;
            case "user-palette-colors/fg-bg-a":
                return SettingsKeys.USER_PALETTE_COLORS_FG_BG_A;
            case "user-palette-colors/fg":
                return SettingsKeys.USER_PALETTE_COLORS_FG;
            default:
                throw new IllegalStateException("Unexpected palette storage path: " + path);
        }
    }

    private String getDatabaseId() {
        // The frozen legacy registry database key of these keys: SettingsKeys
        // addresses "registry:ui-palette", while the in-memory LISTS cache is
        // still keyed by the legacy database id.
        return getKey(registryStoragePath).blockPath().substring("registry:".length());
    }

    private List<JIPipeDesktopColorPaletteColor> loadFromRegistry(String databaseId, JIPipePathMetadataStore.Key registryStoragePath) {
        List<JIPipeDesktopColorPaletteColor> result = LISTS.get(databaseId + "-" + registryStoragePath);
        if (result == null) {
            try {
                SettingsBinding<JsonNode> binding = colorsBinding();
                if (binding != null) {
                    JsonNode stored = binding.get();
                    if (stored != null && stored.isArray()) {
                        // The exact legacy read: a concrete-element typed list.
                        result = JsonUtils.getObjectMapper().readerForListOf(JIPipeDesktopColorPaletteColor.class).readValue(stored);
                        LISTS.put(databaseId + "-" + registryStoragePath, result);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (result == null) {
            result = new ArrayList<>();
            LISTS.put(databaseId + "-" + registryStoragePath, result);
        }
        return result;
    }

    private JIPipePathMetadataStore.Key getRegistryStoragePath(JIPipeDesktopColorPaletteUI paletteUI) {
        if (paletteUI.isEnableBackgroundColorSelection()) {
            if (paletteUI.isEnableAlphaColorSelection()) {
                return JIPipePathMetadataStore.key("user-palette-colors", "fg-bg-a");
            } else {
                return JIPipePathMetadataStore.key("user-palette-colors", "fg-bg");
            }
        } else {
            if (paletteUI.isEnableAlphaColorSelection()) {
                return JIPipePathMetadataStore.key("user-palette-colors", "fg-a");
            } else {
                return JIPipePathMetadataStore.key("user-palette-colors", "fg");
            }
        }
    }

    @Override
    public void addColor(JIPipeDesktopColorPaletteColor color) {
        paletteColors.add(color);
        serializeParameter();
    }

    private void serializeParameter() {
        SettingsBinding<JsonNode> binding = colorsBinding();
        if (binding != null) {
            // The same JSON the legacy putIntoRegistry wrote: valueToTree of
            // the concrete-element list.
            binding.set(JsonUtils.getObjectMapper().valueToTree(paletteColors));
        }
    }

    @Override
    public List<JIPipeDesktopColorPaletteColor> getColors() {
        return Collections.unmodifiableList(paletteColors);
    }

    @Override
    public void setColors(List<JIPipeDesktopColorPaletteColor> colors) {
        paletteColors.clear();
        paletteColors.addAll(colors);
        serializeParameter();
    }

    @Override
    public void removeColor(JIPipeDesktopColorPaletteColor color) {
        paletteColors.remove(color);
        serializeParameter();
    }

    @Override
    public void replaceColor(JIPipeDesktopColorPaletteColor oldColor, JIPipeDesktopColorPaletteColor newColor) {
        int i = this.paletteColors.indexOf(oldColor);
        if (i >= 0) {
            this.paletteColors.set(i, newColor);
            serializeParameter();
        }
    }
}
