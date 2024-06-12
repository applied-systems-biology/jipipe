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

package org.hkijena.jipipe.desktop.app.grapheditor;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class CreateParameterSetsNodeDialog extends JDialog {
    private final JIPipeDesktopGraphNodeUI targetNode;
    private final JList<Object> nodeSelectionList = new JList<>();
    private final JIPipeDesktopFormPanel parameterSelectionList;

    public CreateParameterSetsNodeDialog(JIPipeDesktopWorkbench workbench, JIPipeDesktopGraphNodeUI targetNode) {
        super(workbench.getWindow());
        this.targetNode = targetNode;
        this.parameterSelectionList = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        initialize();
    }

    private void initialize() {
        setTitle("Create parameter sets");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setModal(true);
        setIconImage(UIUtils.getJIPipeIcon128());
        getContentPane().setLayout(new BorderLayout());

        JPanel leftPanel = new JPanel(new BorderLayout());


        UIUtils.addEscapeListener(this);
        getContentPane().add(new AutoResizeSplitPane(AutoResizeSplitPane.LEFT_RIGHT,
                ))
        getContentPane().add(UIUtils.boxHorizontal(
                        Box.createHorizontalGlue(),
                        UIUtils.createButton("Cancel", UIUtils.getIconFromResources("actions/dialog-cancel.png"), () -> this.setVisible(false)),
                        UIUtils.createButton("Create", UIUtils.getIconFromResources("actions/dialog-ok.png"), this::createNodeAndClose)
                ),
                BorderLayout.SOUTH);
    }

    private void createNodeAndClose() {
        setVisible(false);
    }
}
