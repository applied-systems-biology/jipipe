package org.hkijena.acaq5.ui.registries;

import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.utils.ResourceUtils;

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

    public static ACAQUIAlgorithmRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getUIAlgorithmRegistry();
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
}
