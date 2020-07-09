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

package org.hkijena.pipelinej.ui.registries;

import org.hkijena.pipelinej.ACAQDefaultRegistry;
import org.hkijena.pipelinej.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.pipelinej.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.pipelinej.utils.ResourceUtils;

import javax.swing.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for algorithms
 */
public class ACAQUIAlgorithmRegistry {
    private Map<ACAQAlgorithmDeclaration, URL> icons = new HashMap<>();

    /**
     * Creates new instance
     */
    public ACAQUIAlgorithmRegistry() {

    }

    /**
     * Registers a custom icon for a trait
     *
     * @param declaration  the trait type
     * @param resourcePath icon url
     */
    public void registerIcon(ACAQAlgorithmDeclaration declaration, URL resourcePath) {
        icons.put(declaration, resourcePath);
    }

    /**
     * Returns the icon resource path URL for a trait
     *
     * @param klass trait type
     * @return icon url
     */
    public URL getIconURLFor(ACAQAlgorithmDeclaration klass) {
        return icons.getOrDefault(klass, ResourceUtils.getPluginResource("icons/cog.png"));
    }

    /**
     * Returns the icon for a trait
     *
     * @param declaration trait type
     * @return icon instance
     */
    public ImageIcon getIconFor(ACAQAlgorithmDeclaration declaration) {
        URL uri = icons.getOrDefault(declaration, null);
        if (uri == null) {
            URL defaultIcon;
            if (declaration.getCategory() == ACAQAlgorithmCategory.DataSource) {
                if (!declaration.getOutputSlots().isEmpty()) {
                    defaultIcon = ACAQUIDatatypeRegistry.getInstance().getIconURLFor(declaration.getOutputSlots().get(0).value());
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

    public static ACAQUIAlgorithmRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getUIAlgorithmRegistry();
    }
}
