package org.hkijena.acaq5.ui.registries;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.acaq5.api.ACAQData;
import org.hkijena.acaq5.api.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.resultanalysis.ACAQDefaultDataSlotResultUI;
import org.hkijena.acaq5.ui.resultanalysis.ACAQResultDataSlotUI;
import org.hkijena.acaq5.utils.ResourceUtils;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ACAQUITraitRegistry {
    private Map<Class<? extends ACAQData>, URL> icons = new HashMap<>();

    public ACAQUITraitRegistry() {

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
        URL uri = icons.getOrDefault(klass, ResourceUtils.getPluginResource("icons/traits/trait.png"));
        return new ImageIcon(uri);
    }
}
