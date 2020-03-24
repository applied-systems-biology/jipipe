package org.hkijena.acaq5.ui.registries;

import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.utils.ResourceUtils;

import javax.swing.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ACAQUITraitRegistry {
    private Map<ACAQTraitDeclaration, URL> icons = new HashMap<>();

    public ACAQUITraitRegistry() {

    }

    /**
     * Registers a custom icon for a trait
     *
     * @param declaration
     * @param resourcePath
     */
    public void registerIcon(ACAQTraitDeclaration declaration, URL resourcePath) {
        icons.put(declaration, resourcePath);
    }

    /**
     * Returns the icon resource path URL for a trait
     *
     * @param klass
     * @return
     */
    public URL getIconURLFor(ACAQTraitDeclaration klass) {
        return icons.getOrDefault(klass, ResourceUtils.getPluginResource("icons/traits/trait.png"));
    }

    /**
     * Returns the icon for a trait
     *
     * @param declaration
     * @return
     */
    public ImageIcon getIconFor(ACAQTraitDeclaration declaration) {
        URL uri = icons.getOrDefault(declaration, ResourceUtils.getPluginResource("icons/traits/trait.png"));
        return new ImageIcon(uri);
    }

    public static ACAQUITraitRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getUITraitRegistry();
    }
}
