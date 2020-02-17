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

public class ACAQUIDatatypeRegistry {
    private Map<Class<? extends ACAQData>, URL> icons = new HashMap<>();
    private Map<Class<? extends ACAQDataSlot<?>>, Class<? extends ACAQResultDataSlotUI<?>>> resultUIs = new HashMap<>();

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
     * Registers a custom UI for a result data slot
     * @param klass
     * @param uiClass
     */
    public void registerResultSlotUI(Class<? extends ACAQDataSlot<?>> klass, Class<? extends ACAQResultDataSlotUI<?>> uiClass) {
        resultUIs.put(klass, uiClass);
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

    /**
     * Generates a UI for a result data slot
     * @param slot
     * @return
     */
    public ACAQResultDataSlotUI<?> getUIForResultSlot(ACAQWorkbenchUI workbenchUI, ACAQDataSlot<?> slot) {
        Class<? extends ACAQResultDataSlotUI<?>> uiClass = resultUIs.getOrDefault(slot.getClass(), null);
        if(uiClass != null) {
            try {
                return ConstructorUtils.getMatchingAccessibleConstructor(uiClass, ACAQWorkbenchUI.class, slot.getClass())
                        .newInstance(workbenchUI, slot);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            return new ACAQDefaultDataSlotResultUI(workbenchUI, slot);
        }
    }
}
