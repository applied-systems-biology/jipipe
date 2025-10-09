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

package org.hkijena.jipipe.desktop.app.customizer;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUIThemeBrightness;
import org.hkijena.jipipe.utils.ThemeUtils;

import java.nio.file.Path;

public class JIPipeDesktopThemeEditorDocument extends AbstractJIPipeParameterCollection {

    private String id;
    private Path savePath;

    private final CategoryBasics categoryBasics = new CategoryBasics();

    public JIPipeDesktopThemeEditorDocument() {
        this(new JIPipeDesktopModernThemeStyle());
    }

    public JIPipeDesktopThemeEditorDocument(JIPipeDesktopModernThemeStyle style) {
        this.id = style.getId();
        this.savePath = style.getSavePath();

        registerSubParameters(categoryBasics);
    }

    public String getId() {
        return id;
    }

    public Path getSavePath() {
        return savePath;
    }

    public JIPipeDesktopModernThemeStyle toStyle() {
        JIPipeDesktopModernThemeStyle result = new JIPipeDesktopModernThemeStyle();

        result.setBrightness(categoryBasics.getBrightness());

        return result;
    }

    public void save(String id) {
        JIPipeDesktopModernThemeStyle result = ThemeUtils.saveStyle(toStyle(), id);
        this.id = result.getId();
        this.savePath = result.getSavePath();
    }

    @SetJIPipeDocumentation(name = "Basics", description = "Basic theme settings")
    @JIPipeParameter("category-basics")
    public CategoryBasics getCategoryBasics() {
        return categoryBasics;
    }

    public static class CategoryBasics extends AbstractJIPipeParameterCollection {
        private JIPipeDesktopUIThemeBrightness brightness = JIPipeDesktopUIThemeBrightness.Light;

        @SetJIPipeDocumentation(name = "Brightness", description = "The general brightness of the theme (light/dark). " +
                "Determines which icons are used.")
        @JIPipeParameter("brightness")
        public JIPipeDesktopUIThemeBrightness getBrightness() {
            return brightness;
        }

        @JIPipeParameter("brightness")
        public void setBrightness(JIPipeDesktopUIThemeBrightness brightness) {
            this.brightness = brightness;
        }
    }
}
