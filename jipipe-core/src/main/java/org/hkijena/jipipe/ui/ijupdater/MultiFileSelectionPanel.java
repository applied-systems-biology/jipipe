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

package org.hkijena.jipipe.ui.ijupdater;

import net.imagej.updater.FileObject;
import net.imagej.updater.GroupAction;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * Opened when selecting one file
 */
public class MultiFileSelectionPanel extends JIPipeWorkbenchPanel {

    private final Set<FileObject> fileObjects;
    private final ManagerUI managerUI;
    private FormPanel actionButtons = new FormPanel(null, FormPanel.NONE);

    /**
     * @param workbench   the workbench
     * @param managerUI   the manager
     * @param fileObjects the object to be displayed
     */
    public MultiFileSelectionPanel(JIPipeWorkbench workbench, ManagerUI managerUI, Set<FileObject> fileObjects) {
        super(workbench);
        this.managerUI = managerUI;
        this.fileObjects = fileObjects;
        initialize();
        refreshContents();
    }

    private void refreshContents() {

        actionButtons.clear();
        for (GroupAction action : managerUI.getFilesCollection().getValidActions(fileObjects)) {
            JButton button = new JButton(action.getLabel(managerUI.getFilesCollection(), fileObjects));
            button.addActionListener(e -> {
                for (FileObject fileObject : fileObjects) {
                    action.setAction(managerUI.getFilesCollection(), fileObject);
                    managerUI.fireFileChanged(fileObject);
                }
                refreshContents();
            });
            actionButtons.addWideToForm(button, null);
        }

    }

    private void initialize() {
        setLayout(new BorderLayout());
        FormPanel formPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);

        formPanel.addGroupHeader("Change actions", UIUtils.getIconFromResources("actions/configure.png"));
        formPanel.addWideToForm(actionButtons, null);

        formPanel.addVerticalGlue();
        add(formPanel, BorderLayout.CENTER);
    }
}
