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

package org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.properties;

import org.hkijena.jipipe.api.nodes.database.CreateNewCompartmentNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.addnodepanel.JIPipeDesktopAddNodePanelEntryListCellRenderer;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.JIPipeDesktopCompartmentsGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.dragdrop.JIPipeDesktopCompartmentsAddCompartmentTransferHandler;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanelImageComponent;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class JIPipeDesktopCompartmentsAddCompartmentsPanel extends JIPipeDesktopWorkbenchPanel {

    private final JIPipeDesktopCompartmentsGraphEditorUI graphEditorUI;

    public JIPipeDesktopCompartmentsAddCompartmentsPanel(JIPipeDesktopWorkbench desktopWorkbench, JIPipeDesktopCompartmentsGraphEditorUI graphEditorUI) {
        super(desktopWorkbench);
        this.graphEditorUI = graphEditorUI;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        formPanel.getScrollPane().setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        add(formPanel, BorderLayout.CENTER);

        formPanel.addGroupHeader("Adding new compartments", UIUtils.getIconFromResources("actions/polygon-add-nodes.png"));

        formPanel.addWideToForm(UIUtils.createInfoLabel("Drag the following button to the workflow",
                "Alternatively, you can double-click the button",
                UIUtils.getIcon32FromResources("actions/input-mouse-click-left.png")));

        // Hack around existing node addition framework
        DefaultListModel<JIPipeNodeDatabaseEntry> model = new DefaultListModel<>();
        model.addElement(new CreateNewCompartmentNodeDatabaseEntry());

        JList<JIPipeNodeDatabaseEntry> algorithmList = new JList<>();
        algorithmList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        algorithmList.setBorder(UIUtils.createEmptyBorder(8));
        algorithmList.setOpaque(false);
        algorithmList.setModel(model);
        algorithmList.setDragEnabled(true);
        algorithmList.setTransferHandler(new JIPipeDesktopCompartmentsAddCompartmentTransferHandler());
        algorithmList.setCellRenderer(new JIPipeDesktopAddNodePanelEntryListCellRenderer(formPanel,null));

        algorithmList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isLeftMouseButton(e)) {
                    if(e.getClickCount() == 2) {
                        graphEditorUI.addCompartment();
                    }
                }
            }
        });
        formPanel.addWideToForm(algorithmList);


        // Basic explanation
        formPanel.addGroupHeader("Graph compartments", UIUtils.getIconFromResources("actions/graph-compartments.png"));
        formPanel.addWideToForm(new JLabel("The 'Compartments' view allows you to organize your workflow into multiple units", UIUtils.getIconFromResources("actions/view-sort.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Each compartment node contains a workflow graph", UIUtils.getIconFromResources("actions/graph-compartment.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Double-click compartments to open them", UIUtils.getIconFromResources("actions/arrow-pointer.png"), JLabel.LEFT));

        formPanel.addWideToForm(new JIPipeDesktopFormPanelImageComponent(UIUtils.getImageFromResources("documentation/graph-editor-overview-compartments.png"), true));




        formPanel.addVerticalGlue();
    }

}
