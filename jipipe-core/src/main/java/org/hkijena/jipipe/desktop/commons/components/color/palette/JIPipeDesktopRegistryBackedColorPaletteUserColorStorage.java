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

import org.hkijena.jipipe.JIPipe;

import java.nio.file.Path;
import java.util.*;

public class JIPipeDesktopRegistryBackedColorPaletteUserColorStorage implements JIPipeDesktopColorPaletteUserColorStorage {

    /**
     * Deserialized lists that we are working on.
     * Assuming that we call them from EDT, should not have thread safety issues
     */
    private static final Map<String, List<JIPipeDesktopColorPaletteColor>> LISTS = new HashMap<>();

    private final JIPipeDesktopColorPaletteUI paletteUI;
    private final Path registryStoragePath;
    private final List<JIPipeDesktopColorPaletteColor> paletteColors;

    public JIPipeDesktopRegistryBackedColorPaletteUserColorStorage(JIPipeDesktopColorPaletteUI paletteUI) {
        this.paletteUI = paletteUI;
        this.registryStoragePath = getRegistryStoragePath(paletteUI);
        this.paletteColors = loadFromRegistry("ui-palette", registryStoragePath);
    }

    private List<JIPipeDesktopColorPaletteColor> loadFromRegistry(String databaseId, Path registryStoragePath) {
        List<JIPipeDesktopColorPaletteColor> result = LISTS.get(databaseId + "-" + registryStoragePath);
        if (result == null) {
            try {
                result = JIPipe.getSettings().getListFromRegistry(databaseId, registryStoragePath, JIPipeDesktopColorPaletteColor.class, true);
                LISTS.put(databaseId + "-" + registryStoragePath, result);
            } catch (Exception ignored) {
            }
        }
        if (result == null) {
            result = new ArrayList<>();
            LISTS.put(databaseId + "-" + registryStoragePath, result);
        }
        return result;
    }

    private Path getRegistryStoragePath(JIPipeDesktopColorPaletteUI paletteUI) {
        if (paletteUI.isEnableBackgroundColorSelection()) {
            if (paletteUI.isEnableAlphaColorSelection()) {
                return Path.of("user-palette-colors", "fg-bg-a");
            } else {
                return Path.of("user-palette-colors", "fg-bg");
            }
        } else {
            if (paletteUI.isEnableAlphaColorSelection()) {
                return Path.of("user-palette-colors", "fg-a");
            } else {
                return Path.of("user-palette-colors", "fg");
            }
        }
    }

    @Override
    public void addColor(JIPipeDesktopColorPaletteColor color) {
        paletteColors.add(color);
        serializeParameter();
    }

    private void serializeParameter() {
        JIPipe.getSettings().putIntoRegistry("ui-palette", registryStoragePath, paletteColors);
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
