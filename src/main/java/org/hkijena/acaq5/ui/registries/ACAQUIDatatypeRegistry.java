package org.hkijena.acaq5.ui.registries;

import org.hkijena.acaq5.api.ACAQData;
import org.hkijena.acaq5.utils.ResourceUtils;

import javax.swing.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ACAQUIDatatypeRegistry {
    private Map<Class<? extends ACAQData>, URL> icons = new HashMap<>();

    public ACAQUIDatatypeRegistry() {

    }

    /**
     * Registers a custom icon for a datatype
     * @param klass
     * @param resourcePath
     */
    public void registerIcon(Class<? extends ACAQData> klass, URL resourcePath) {
        icons.put(klass, resourcePath);
    }

    /**
     * Returns the icon for a datatype
     * @param klass
     * @return
     */
    public ImageIcon getIconFor(Class<? extends ACAQData> klass) {
        URL uri = icons.getOrDefault(klass, ResourceUtils.getPluginResource("icons/data-type-unknown.png"));
        return new ImageIcon(uri);
    }
}
