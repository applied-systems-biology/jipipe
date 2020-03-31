package org.hkijena.acaq5.ui.registries;

import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.utils.ResourceUtils;

import javax.swing.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for annotations
 */
public class ACAQUITraitRegistry {
    private Map<ACAQTraitDeclaration, URL> icons = new HashMap<>();

    /**
     * Creates new instance
     */
    public ACAQUITraitRegistry() {

    }

    /**
     * Registers a custom icon for a trait
     *
     * @param declaration the trait type
     * @param resourcePath icon url
     */
    public void registerIcon(ACAQTraitDeclaration declaration, URL resourcePath) {
        icons.put(declaration, resourcePath);
    }

    /**
     * Returns the icon resource path URL for a trait
     *
     * @param klass trait type
     * @return icon url
     */
    public URL getIconURLFor(ACAQTraitDeclaration klass) {
        return icons.getOrDefault(klass, ResourceUtils.getPluginResource("icons/traits/trait.png"));
    }

    /**
     * Returns the icon for a trait
     *
     * @param declaration trait type
     * @return icon instance
     */
    public ImageIcon getIconFor(ACAQTraitDeclaration declaration) {
        URL uri = icons.getOrDefault(declaration, ResourceUtils.getPluginResource("icons/traits/trait.png"));
        return new ImageIcon(uri);
    }

    public static ACAQUITraitRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getUITraitRegistry();
    }
}
