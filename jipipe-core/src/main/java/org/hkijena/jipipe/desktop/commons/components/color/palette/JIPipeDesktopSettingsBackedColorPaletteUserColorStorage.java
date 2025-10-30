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
import org.hkijena.jipipe.plugins.settings.application.JIPipePresetsApplicationSettings;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.*;

public class JIPipeDesktopSettingsBackedColorPaletteUserColorStorage implements  JIPipeDesktopColorPaletteUserColorStorage {

    /**
     * Deserialized lists that we are working on.
     * Assuming that we call them from EDT, should not have thread safety issues
     */
    private static final Map<String, List<JIPipeDesktopColorPaletteColor>> LISTS = new HashMap<>();

    private final JIPipeDesktopColorPaletteUI paletteUI;
    private final String parameterKey;
    private final List<JIPipeDesktopColorPaletteColor> paletteColors;

    public JIPipeDesktopSettingsBackedColorPaletteUserColorStorage(JIPipeDesktopColorPaletteUI paletteUI) {
        this.paletteUI = paletteUI;
        this.parameterKey = findParameterKey(paletteUI);
        this.paletteColors = deserializeParameterKey(parameterKey);
    }

    private List<JIPipeDesktopColorPaletteColor> deserializeParameterKey(String parameterKey) {
        List<JIPipeDesktopColorPaletteColor> result = LISTS.get(parameterKey);
        if(result == null) {
            try {
                JIPipePresetsApplicationSettings settings = JIPipePresetsApplicationSettings.getInstance();
                String json = settings.getParameter(parameterKey, String.class);
                if(!StringUtils.isNullOrEmpty(json)) {
                    result = JsonUtils.getObjectMapper().readerForListOf(JIPipeDesktopColorPaletteColor.class).readValue(json);
                    LISTS.put(parameterKey, result);
                }
            }
            catch (Exception ignored) {
            }
        }
        if(result == null) {
            result = new ArrayList<>();
            LISTS.put(parameterKey, result);
        }
        return result;
    }

    private String findParameterKey(JIPipeDesktopColorPaletteUI paletteUI) {
        if(paletteUI.isEnableBackgroundColorSelection()) {
            if(paletteUI.isEnableAlphaColorSelection()) {
                return "user-palette-colors-fg-bg-a";
            }
            else {
                return "user-palette-colors-fg-bg";
            }
        }
        else {
            if(paletteUI.isEnableAlphaColorSelection()) {
                return "user-palette-colors-fg-a";
            }
            else {
                return "user-palette-colors-fg";
            }
        }
    }

    @Override
    public void addColor(JIPipeDesktopColorPaletteColor color) {
        paletteColors.add(color);
        serializeParameter();
    }

    private void serializeParameter() {
        JIPipePresetsApplicationSettings settings = JIPipePresetsApplicationSettings.getInstance();
        settings.setParameter(parameterKey, JsonUtils.toJsonString(paletteColors));
        JIPipe.getSettings().saveLater();
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
        if(i >= 0) {
            this.paletteColors.set(i, newColor);
            serializeParameter();
        }
    }
}
