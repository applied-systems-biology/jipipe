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

package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Provides a standard result slot UI that can be also further extended
 */
public class ACAQDefaultResultDataSlotRowUI extends ACAQResultDataSlotRowUI {

    private List<SlotAction> registeredSlotActions = new ArrayList<>();

    /**
     * Creates a new UI
     *
     * @param workbenchUI The workbench UI
     * @param slot        The data slot
     * @param row         The data slow row
     */
    public ACAQDefaultResultDataSlotRowUI(ACAQProjectWorkbench workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
        super(workbenchUI, slot, row);
        registerActions();
        initialize();
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(Box.createHorizontalGlue());

        if (!registeredSlotActions.isEmpty()) {
            SlotAction mainSlotAction = registeredSlotActions.get(registeredSlotActions.size() - 1);
            JButton mainActionButton = new JButton(mainSlotAction.getName(), mainSlotAction.getIcon());
            mainActionButton.setToolTipText(mainSlotAction.getDescription());
            mainActionButton.addActionListener(e -> mainSlotAction.action.accept(getSlot()));
            add(mainActionButton);

            if (registeredSlotActions.size() > 1) {
                JButton menuButton = new JButton("...");
                menuButton.setMaximumSize(new Dimension(1, (int) mainActionButton.getPreferredSize().getHeight()));
                menuButton.setToolTipText("More actions ...");
                JPopupMenu menu = UIUtils.addPopupMenuToComponent(menuButton);
                for (int i = registeredSlotActions.size() - 2; i >= 0; --i) {
                    SlotAction otherSlotAction = registeredSlotActions.get(i);
                    JMenuItem item = new JMenuItem(otherSlotAction.getName(), otherSlotAction.getIcon());
                    item.setToolTipText(otherSlotAction.getDescription());
                    item.addActionListener(e -> otherSlotAction.getAction().accept(getSlot()));
                    menu.add(item);
                }
                add(menuButton);
            }
        }
    }

    /**
     * Override this method to add actions
     * The last added action is displayed as full button
     */
    protected void registerActions() {
        if (getSlot().getStoragePath() != null) {
            registerAction("Open folder", "Opens the folder that contains the data files.", UIUtils.getIconFromResources("open.png"), s -> openFolder());
        }
    }

    /**
     * Registers an action for the data slot
     *
     * @param name        The name of the action
     * @param description A description of the action
     * @param icon        An icon
     * @param action      A method called when the action is activated
     */
    protected void registerAction(String name, String description, Icon icon, Consumer<ACAQDataSlot> action) {
        registeredSlotActions.add(new SlotAction(name, description, icon, action));
    }

    private void openFolder() {
        try {
            Desktop.getDesktop().open(Objects.requireNonNull(getRowStorageFolder().toFile()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleDefaultAction() {
        if (!registeredSlotActions.isEmpty()) {
            SlotAction mainSlotAction = registeredSlotActions.get(registeredSlotActions.size() - 1);
            mainSlotAction.action.accept(getSlot());
        }
    }

    /**
     * Encapsulates a slot action
     */
    private static class SlotAction {
        private String name;
        private String description;
        private Icon icon;
        private Consumer<ACAQDataSlot> action;

        private SlotAction(String name, String description, Icon icon, Consumer<ACAQDataSlot> action) {
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.action = action;
        }

        public String getName() {
            return name;
        }

        public Icon getIcon() {
            return icon;
        }

        public Consumer<ACAQDataSlot> getAction() {
            return action;
        }

        public String getDescription() {
            return description;
        }
    }
}
