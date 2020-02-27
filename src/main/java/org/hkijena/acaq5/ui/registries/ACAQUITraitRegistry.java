package org.hkijena.acaq5.ui.registries;

import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.utils.ResourceUtils;

import javax.swing.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ACAQUITraitRegistry {
    private Map<Class<? extends ACAQTrait>, URL> icons = new HashMap<>();

    public ACAQUITraitRegistry() {

    }

    public static ACAQUITraitRegistry getInstance() {
        return ACAQRegistryService.getInstance().getUITraitRegistry();
    }

    /**
     * Registers a custom icon for a trait
     * @param klass
     * @param resourcePath
     */
    public void registerIcon(Class<? extends ACAQTrait> klass, URL resourcePath) {
        icons.put(klass, resourcePath);
    }

    /**
     * Returns the icon resource path URL for a trait
     * @param klass
     * @return
     */
    public URL getIconURLFor(Class<? extends ACAQTrait> klass) {
        return icons.getOrDefault(klass, ResourceUtils.getPluginResource("icons/traits/trait.png"));
    }

    /**
     * Returns the icon for a trait
     * @param klass
     * @return
     */
    public ImageIcon getIconFor(Class<? extends ACAQTrait> klass) {
        URL uri = icons.getOrDefault(klass, ResourceUtils.getPluginResource("icons/traits/trait.png"));
        return new ImageIcon(uri);
    }
}
