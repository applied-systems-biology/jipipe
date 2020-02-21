package org.hkijena.acaq5.ui.registries;

import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
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

    /**
     * Registers a custom icon for a datatype
     * @param klass
     * @param resourcePath
     */
    public void registerIcon(Class<? extends ACAQTrait> klass, URL resourcePath) {
        icons.put(klass, resourcePath);
    }


    /**
     * Returns the icon for a datatype
     * @param klass
     * @return
     */
    public ImageIcon getIconFor(Class<? extends ACAQTrait> klass) {
        URL uri = icons.getOrDefault(klass, ResourceUtils.getPluginResource("icons/traits/trait.png"));
        return new ImageIcon(uri);
    }
}
