/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.commons.components;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeDesktopDataInfoListCellRenderer;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeDesktopInheritanceConversionListCellRenderer;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Objects;

/**
 * Editor for an inheritance conversion stored in an {@link JIPipeDataSlotInfo}
 */
public class JIPipeDesktopInheritanceConversionEditorUI extends JPanel {
    private Map<JIPipeDataInfo, JIPipeDataInfo> inheritanceConversions;
    private JList<Map.Entry<JIPipeDataInfo, JIPipeDataInfo>> list;

    /**
     * @param inheritanceConversions the inheritance conversion
     */
    public JIPipeDesktopInheritanceConversionEditorUI(Map<JIPipeDataInfo, JIPipeDataInfo> inheritanceConversions) {
        this.inheritanceConversions = inheritanceConversions;
        initialize();
        reload();
    }

    private void reload() {
        DefaultListModel<Map.Entry<JIPipeDataInfo, JIPipeDataInfo>> model = new DefaultListModel<>();
        for (Map.Entry<JIPipeDataInfo, JIPipeDataInfo> entry : inheritanceConversions.entrySet()) {
            model.addElement(entry);
        }
        list.setModel(model);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        list = new JList<>();
        list.setCellRenderer(new JIPipeDesktopInheritanceConversionListCellRenderer());
        add(list, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JButton addButton = new JButton("Add", UIUtils.getIconFromResources("actions/list-add.png"));
        addButton.addActionListener(e -> addEntry());
        toolBar.add(addButton);

        JButton removeButton = new JButton(UIUtils.getIconFromResources("actions/delete.png"));
        removeButton.addActionListener(e -> removeSelectedEntries());
        toolBar.add(removeButton);
        add(toolBar, BorderLayout.NORTH);
    }

    private void removeSelectedEntries() {
        for (Map.Entry<JIPipeDataInfo, JIPipeDataInfo> entry :
                list.getSelectedValuesList()) {
            inheritanceConversions.remove(entry.getKey());
        }
        reload();
    }

    private void addEntry() {
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.NONE);
        JIPipeDataInfo[] available = JIPipe.getDataTypes().getUnhiddenRegisteredDataTypes().values()
                .stream().map(JIPipeDataInfo::getInstance).toArray(JIPipeDataInfo[]::new);
        JComboBox<JIPipeDataInfo> from = new JComboBox<>(available);
        from.setRenderer(new JIPipeDesktopDataInfoListCellRenderer());
        JComboBox<JIPipeDataInfo> to = new JComboBox<>(available);
        to.setRenderer(new JIPipeDesktopDataInfoListCellRenderer());
        formPanel.addToForm(from, new JLabel("From"), null);
        formPanel.addToForm(to, new JLabel("To"), null);

        if (JOptionPane.showConfirmDialog(this,
                formPanel,
                "Add conversion",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            JIPipeDataInfo selectedFrom = (JIPipeDataInfo) from.getSelectedItem();
            JIPipeDataInfo selectedTo = (JIPipeDataInfo) to.getSelectedItem();
            if (!Objects.equals(selectedFrom, selectedTo)) {
                inheritanceConversions.put(selectedFrom, selectedTo);
                reload();
            }
        }
    }
}
