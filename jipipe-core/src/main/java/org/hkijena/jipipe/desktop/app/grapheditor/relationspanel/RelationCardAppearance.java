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

package org.hkijena.jipipe.desktop.app.grapheditor.relationspanel;

import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.NoFramework;
import org.hkijena.jipipe.desktop.commons.nodepatterns.NodePatternStyles;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.utils.ThemeUtils;

import java.awt.Color;

/**
 * Resolves the look of one Relations panel card: fill, border and framework
 * signature pattern (MR 3 of the navigator overhaul, #1358).
 * <p>
 * A pure value resolver over {@link JIPipeNodeInfo} and the current theme
 * style: it returns the exact values the card's {@code paintComponent}
 * needs, with the same gate and sources the canvas node uses
 * ({@code JIPipeDesktopGraphNodeUI}).
 */
public final class RelationCardAppearance {

    private RelationCardAppearance() {
    }

    /**
     * Everything the card's {@code paintComponent} needs.
     * <p>
     * {@code patternEnabled} is the single gate for painting the pattern
     * (false for the {@link NoFramework} sentinel or when the theme disables
     * the pattern); the other pattern fields carry the current settings and
     * are only meaningful when it is true. The pattern seed is the framework
     * id, the style id and the ink/opacity come from the framework and the
     * theme style.
     */
    public record Appearance(Color fill, Color border, boolean patternEnabled, String patternStyleId,
                             String patternSeed, float patternOpacity, Color patternInk) {
    }

    /**
     * Resolves the appearance of a node from its info and the current theme
     * style.
     *
     * @param info the node type shown by the card
     * @return the resolved appearance
     */
    public static Appearance of(JIPipeNodeInfo info) {
        JIPipeDesktopModernThemeStyle style = ThemeUtils.getCurrentStyle();
        JIPipeNodeTaxonomyFramework framework = info.getFramework();
        boolean patternEnabled = style != null
                && style.isShowNodeFrameworkPattern()
                && framework != null
                && !NoFramework.ID.equals(framework.getId())
                && NodePatternStyles.family(framework.getPatternStyleId()) != null;
        return new Appearance(
                ThemeUtils.getNodeFillColor(info),
                ThemeUtils.getNodeBorderColor(info),
                patternEnabled,
                framework != null ? framework.getPatternStyleId() : null,
                framework != null ? framework.getId() : null,
                style != null ? style.getNodePatternOpacity() : 0f,
                style != null ? style.getNodePatternInk() : null);
    }
}
