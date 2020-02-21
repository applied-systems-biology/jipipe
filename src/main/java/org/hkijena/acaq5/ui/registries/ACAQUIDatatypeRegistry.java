package org.hkijena.acaq5.ui.registries;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.ACAQRunSample;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
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
        URL uri = icons.getOrDefault(klass, ResourceUtils.getPluginResource("icons/data-types/data-type.png"));
        return new ImageIcon(uri);
    }

    /**
     * Generates a UI for a result data slot
     * @param slot
     * @return
     */
    public ACAQResultDataSlotUI<?> getUIForResultSlot(ACAQWorkbenchUI workbenchUI, ACAQRunSample sample, ACAQDataSlot<?> slot) {
        Class<? extends ACAQResultDataSlotUI<?>> uiClass = resultUIs.getOrDefault(slot.getClass(), null);
        if(uiClass != null) {
            try {
                return ConstructorUtils.getMatchingAccessibleConstructor(uiClass, ACAQWorkbenchUI.class, ACAQRunSample.class, slot.getClass())
                        .newInstance(workbenchUI, sample, slot);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            return new ACAQDefaultDataSlotResultUI(workbenchUI, sample, slot);
        }
    }

    public URL getIconURLFor(Class<? extends ACAQData> klass) {
        return icons.getOrDefault(klass, ResourceUtils.getPluginResource("icons/data-types/data-type.png"));
    }

    public URL getIconURLForSlot(Class<? extends ACAQDataSlot<?>> klass) {
        return getIconURLFor(ACAQRegistryService.getInstance().getDatatypeRegistry().getRegisteredSlotDataTypes().inverse().get(klass));
    }
}
