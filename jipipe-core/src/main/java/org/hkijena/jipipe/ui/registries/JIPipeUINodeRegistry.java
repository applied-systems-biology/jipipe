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
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.utils.ResourceUtils;

import javax.swing.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for algorithms
 */
public class JIPipeUINodeRegistry {
    private Map<JIPipeNodeInfo, URL> icons = new HashMap<>();

    /**
     * Creates new instance
     */
    public JIPipeUINodeRegistry() {

    }

    /**
     * Registers a custom icon for a trait
     *
     * @param info         the trait type
     * @param resourcePath icon url
     */
    public void registerIcon(JIPipeNodeInfo info, URL resourcePath) {
        icons.put(info, resourcePath);
    }

    /**
     * Returns the icon resource path URL for a trait
     *
     * @param klass trait type
     * @return icon url
     */
    public URL getIconURLFor(JIPipeNodeInfo klass) {
        return icons.getOrDefault(klass, ResourceUtils.getPluginResource("icons/actions/configure.png"));
    }

    /**
     * Returns the icon for a trait
     *
     * @param info trait type
     * @return icon instance
     */
    public ImageIcon getIconFor(JIPipeNodeInfo info) {
        URL uri = icons.getOrDefault(info, null);
        if (uri == null) {
            URL defaultIcon;
            if (info.getCategory() instanceof DataSourceNodeTypeCategory) {
                if (!info.getOutputSlots().isEmpty()) {
                    defaultIcon = JIPipeUIDatatypeRegistry.getInstance().getIconURLFor(info.getOutputSlots().get(0).value());
                } else {
                    defaultIcon = ResourceUtils.getPluginResource("icons/actions/configure.png");
                }
            } else {
                defaultIcon = ResourceUtils.getPluginResource("icons/actions/configure.png");
            }
            icons.put(info, defaultIcon);
            uri = defaultIcon;
        }
        return new ImageIcon(uri);
    }

    public static JIPipeUINodeRegistry getInstance() {
        return JIPipeDefaultRegistry.getInstance().getUIAlgorithmRegistry();
    }
}
