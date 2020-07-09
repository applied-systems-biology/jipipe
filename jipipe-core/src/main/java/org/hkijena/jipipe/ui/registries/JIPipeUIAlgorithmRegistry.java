/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.registries;

import org.hkijena.jipipe.JIPipeDefaultRegistry;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.utils.ResourceUtils;

import javax.swing.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for algorithms
 */
public class JIPipeUIAlgorithmRegistry {
    private Map<JIPipeAlgorithmDeclaration, URL> icons = new HashMap<>();

    /**
     * Creates new instance
     */
    public JIPipeUIAlgorithmRegistry() {

    }

    /**
     * Registers a custom icon for a trait
     *
     * @param declaration  the trait type
     * @param resourcePath icon url
     */
    public void registerIcon(JIPipeAlgorithmDeclaration declaration, URL resourcePath) {
        icons.put(declaration, resourcePath);
    }

    /**
     * Returns the icon resource path URL for a trait
     *
     * @param klass trait type
     * @return icon url
     */
    public URL getIconURLFor(JIPipeAlgorithmDeclaration klass) {
        return icons.getOrDefault(klass, ResourceUtils.getPluginResource("icons/cog.png"));
    }

    /**
     * Returns the icon for a trait
     *
     * @param declaration trait type
     * @return icon instance
     */
    public ImageIcon getIconFor(JIPipeAlgorithmDeclaration declaration) {
        URL uri = icons.getOrDefault(declaration, null);
        if (uri == null) {
            URL defaultIcon;
            if (declaration.getCategory() == JIPipeAlgorithmCategory.DataSource) {
                if (!declaration.getOutputSlots().isEmpty()) {
                    defaultIcon = JIPipeUIDatatypeRegistry.getInstance().getIconURLFor(declaration.getOutputSlots().get(0).value());
                } else {
                    defaultIcon = ResourceUtils.getPluginResource("icons/cog.png");
                }
            } else {
                defaultIcon = ResourceUtils.getPluginResource("icons/cog.png");
            }
            icons.put(declaration, defaultIcon);
            uri = defaultIcon;
        }
        return new ImageIcon(uri);
    }

    public static JIPipeUIAlgorithmRegistry getInstance() {
        return JIPipeDefaultRegistry.getInstance().getUIAlgorithmRegistry();
    }
}
