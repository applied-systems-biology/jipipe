package org.hkijena.acaq5.ui.registries;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataDeclaration;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.ui.ACAQProjectUI;
import org.hkijena.acaq5.ui.resultanalysis.ACAQDefaultResultDataSlotCellUI;
import org.hkijena.acaq5.ui.resultanalysis.ACAQDefaultResultDataSlotRowUI;
import org.hkijena.acaq5.ui.resultanalysis.ACAQResultDataSlotCellUI;
import org.hkijena.acaq5.ui.resultanalysis.ACAQResultDataSlotRowUI;
import org.hkijena.acaq5.utils.ResourceUtils;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ACAQUIDatatypeRegistry {
    private ACAQResultDataSlotCellUI defaultResultTableRowUI = new ACAQDefaultResultDataSlotCellUI();
    private Map<Class<? extends ACAQData>, URL> icons = new HashMap<>();
    private Map<Class<? extends ACAQData>, Class<? extends ACAQResultDataSlotRowUI>> resultUIs = new HashMap<>();
    private Map<Class<? extends ACAQData>, ACAQResultDataSlotCellUI> resultTableCellUIs = new HashMap<>();

    public ACAQUIDatatypeRegistry() {

    }

    /**
     * Registers a custom icon for a datatype
     *
     * @param klass
     * @param resourcePath
     */
    public void registerIcon(Class<? extends ACAQData> klass, URL resourcePath) {
        icons.put(klass, resourcePath);
    }

    /**
     * Registers a custom UI for a result data slot
     *
     * @param klass
     * @param uiClass
     */
    public void registerResultSlotUI(Class<? extends ACAQData> klass, Class<? extends ACAQResultDataSlotRowUI> uiClass) {
        resultUIs.put(klass, uiClass);
    }

    /**
     * Registers a custom renderer for the data displayed in the dataslot result table
     *
     * @param klass
     * @param renderer
     */
    public void registerResultTableCellUI(Class<? extends ACAQData> klass, ACAQResultDataSlotCellUI renderer) {
        resultTableCellUIs.put(klass, renderer);
    }

    /**
     * Returns the icon for a datatype
     *
     * @param klass
     * @return
     */
    public ImageIcon getIconFor(Class<? extends ACAQData> klass) {
        URL uri = icons.getOrDefault(klass, ResourceUtils.getPluginResource("icons/data-types/data-type.png"));
        return new ImageIcon(uri);
    }

    /**
     * Generates a UI for a result data slot
     *
     * @param slot
     * @return
     */
    public ACAQResultDataSlotRowUI getUIForResultSlot(ACAQProjectUI workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
        Class<? extends ACAQResultDataSlotRowUI> uiClass = resultUIs.getOrDefault(slot.getAcceptedDataType(), null);
        if (uiClass != null) {
            try {
                return ConstructorUtils.getMatchingAccessibleConstructor(uiClass, ACAQProjectUI.class, ACAQDataSlot.class, ACAQExportedDataTable.Row.class)
                        .newInstance(workbenchUI, slot, row);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else {
            return new ACAQDefaultResultDataSlotRowUI(workbenchUI, slot, row);
        }
    }

    /**
     * Returns a cell renderer for dataslot result table
     *
     * @param klass
     * @return
     */
    public ACAQResultDataSlotCellUI getCellRendererFor(Class<? extends ACAQData> klass) {
        return resultTableCellUIs.getOrDefault(klass, defaultResultTableRowUI);
    }

    public URL getIconURLFor(ACAQAlgorithmCategory category) {
        switch (category) {
            case DataSource:
                return ResourceUtils.getPluginResource("icons/database.png");
            case FileSystem:
                return ResourceUtils.getPluginResource("icons/tree.png");
            case Annotation:
                return ResourceUtils.getPluginResource("icons/label.png");
            case Enhancer:
                return ResourceUtils.getPluginResource("icons/magic.png");
            case Segmenter:
                return ResourceUtils.getPluginResource("icons/binary.png");
            case Converter:
                return ResourceUtils.getPluginResource("icons/convert.png");
            case Quantifier:
                return ResourceUtils.getPluginResource("icons/statistics.png");
            default:
                return ResourceUtils.getPluginResource("icons/module.png");
        }
    }

    public URL getIconURLFor(Class<? extends ACAQData> klass) {
        return icons.getOrDefault(klass, ResourceUtils.getPluginResource("icons/data-types/data-type.png"));
    }

    public URL getIconURLFor(ACAQDataDeclaration declaration) {
        return getIconURLFor(declaration.getDataClass());
    }

    public static ACAQUIDatatypeRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getUIDatatypeRegistry();
    }
}
