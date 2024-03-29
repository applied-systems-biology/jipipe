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

package org.hkijena.jipipe.api.nodes;

/**
 * Describes where a node is located in the menu
 */
public class JIPipeNodeMenuLocation {
    private final JIPipeNodeTypeCategory category;
    private final String menuPath;

    private final String alternativeName;

    public JIPipeNodeMenuLocation(JIPipeNodeTypeCategory category, String menuPath, String alternativeName) {
        this.category = category;
        this.menuPath = menuPath;
        this.alternativeName = alternativeName;
    }

    public JIPipeNodeTypeCategory getCategory() {
        return category;
    }

    public String getMenuPath() {
        return menuPath;
    }

    public String getAlternativeName() {
        return alternativeName;
    }
}
