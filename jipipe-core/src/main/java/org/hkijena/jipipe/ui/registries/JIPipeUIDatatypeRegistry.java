/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.registries;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.jipipe.JIPipeDefaultRegistry;
import org.hkijena.jipipe.api.nodes.JIPipeNodeCategory;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeDefaultResultDataSlotCellUI;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeDefaultResultDataSlotRowUI;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeResultDataSlotCellUI;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeResultDataSlotRowUI;
import org.hkijena.jipipe.utils.ResourceUtils;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for data types
 */
public class JIPipeUIDatatypeRegistry {
    private JIPipeResultDataSlotCellUI defaultResultTableRowUI = new JIPipeDefaultResultDataSlotCellUI();
    private Map<Class<? extends JIPipeData>, URL> icons = new HashMap<>();
    private Map<Class<? extends JIPipeData>, Class<? extends JIPipeResultDataSlotRowUI>> resultUIs = new HashMap<>();
    private Map<Class<? extends JIPipeData>, JIPipeResultDataSlotCellUI> resultTableCellUIs = new HashMap<>();

    /**
     * New registry
     */
    public JIPipeUIDatatypeRegistry() {

    }

    /**
     * Registers a custom icon for a datatype
     *
     * @param klass        data class
     * @param resourcePath icon resource
     */
    public void registerIcon(Class<? extends JIPipeData> klass, URL resourcePath) {
        icons.put(klass, resourcePath);
    }

    /**
     * Registers a custom UI for a result data slot
     *
     * @param klass   data class
     * @param uiClass slot ui
     */
    public void registerResultSlotUI(Class<? extends JIPipeData> klass, Class<? extends JIPipeResultDataSlotRowUI> uiClass) {
        resultUIs.put(klass, uiClass);
    }

    /**
     * Registers a custom renderer for the data displayed in the dataslot result table
     *
     * @param klass    data class
     * @param renderer cell renderer
     */
    public void registerResultTableCellUI(Class<? extends JIPipeData> klass, JIPipeResultDataSlotCellUI renderer) {
        resultTableCellUIs.put(klass, renderer);
    }

    /**
     * Returns the icon for a datatype
     *
     * @param klass data class
     * @return icon instance
     */
    public ImageIcon getIconFor(Class<? extends JIPipeData> klass) {
        URL uri = icons.getOrDefault(klass, ResourceUtils.getPluginResource("icons/data-types/data-type.png"));
        return new ImageIcon(uri);
    }

    /**
     * Generates a UI for a result data slot
     *
     * @param workbenchUI workbench UI
     * @param slot        data slot
     * @param row         table row
     * @return slot UI
     */
    public JIPipeResultDataSlotRowUI getUIForResultSlot(JIPipeProjectWorkbench workbenchUI, JIPipeDataSlot slot, JIPipeExportedDataTable.Row row) {
        Class<? extends JIPipeResultDataSlotRowUI> uiClass = resultUIs.getOrDefault(slot.getAcceptedDataType(), null);
        if (uiClass != null) {
            try {
                return ConstructorUtils.getMatchingAccessibleConstructor(uiClass, JIPipeProjectWorkbench.class, JIPipeDataSlot.class, JIPipeExportedDataTable.Row.class)
                        .newInstance(workbenchUI, slot, row);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else {
            return new JIPipeDefaultResultDataSlotRowUI(workbenchUI, slot, row);
        }
    }

    /**
     * Returns a cell renderer for dataslot result table
     *
     * @param klass data class
     * @return cell renderer
     */
    public JIPipeResultDataSlotCellUI getCellRendererFor(Class<? extends JIPipeData> klass) {
        return resultTableCellUIs.getOrDefault(klass, defaultResultTableRowUI);
    }

    /**
     * Gets an icon for each algorithm category
     *
     * @param category algorithm category
     * @return icon for the category
     */
    public URL getIconURLFor(JIPipeNodeCategory category) {
        switch (category) {
            case DataSource:
                return ResourceUtils.getPluginResource("icons/database.png");
            case FileSystem:
                return ResourceUtils.getPluginResource("icons/tree.png");
            case Annotation:
                return ResourceUtils.getPluginResource("icons/label.png");
            case Processor:
                return ResourceUtils.getPluginResource("icons/magic.png");
            case Converter:
                return ResourceUtils.getPluginResource("icons/convert.png");
            case Analysis:
                return ResourceUtils.getPluginResource("icons/statistics.png");
            default:
                return ResourceUtils.getPluginResource("icons/module.png");
        }
    }

    /**
     * @param klass data class
     * @return icon resource
     */
    public URL getIconURLFor(Class<? extends JIPipeData> klass) {
        return icons.getOrDefault(klass, ResourceUtils.getPluginResource("icons/data-types/data-type.png"));
    }

    /**
     * @param info data info
     * @return icon resource
     */
    public URL getIconURLFor(JIPipeDataInfo info) {
        return getIconURLFor(info.getDataClass());
    }

    public static JIPipeUIDatatypeRegistry getInstance() {
        return JIPipeDefaultRegistry.getInstance().getUIDatatypeRegistry();
    }
}
