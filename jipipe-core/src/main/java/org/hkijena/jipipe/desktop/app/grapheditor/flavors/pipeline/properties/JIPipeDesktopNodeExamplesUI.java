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

package org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.properties;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.plugins.nodeexamples.JIPipeNodeExampleListCellRenderer;
import org.hkijena.jipipe.plugins.nodetemplate.NodeTemplatesRefreshedEvent;
import org.hkijena.jipipe.plugins.nodetemplate.NodeTemplatesRefreshedEventListener;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class JIPipeDesktopNodeExamplesUI extends JIPipeDesktopProjectWorkbenchPanel implements NodeTemplatesRefreshedEventListener {

    private final JIPipeAlgorithm algorithm;
    private final JIPipeDesktopDockPanel dockPanel;
    private final JList<JIPipeNodeExample> exampleJList = new JList<>();

    public JIPipeDesktopNodeExamplesUI(JIPipeDesktopProjectWorkbench workbench, JIPipeAlgorithm algorithm, JIPipeDesktopDockPanel dockPanel) {
        super(workbench);
        this.algorithm = algorithm;
        this.dockPanel = dockPanel;
        initialize();
        reloadList();
        JIPipe.getNodeTemplates().getNodeTemplatesRefreshedEventEmitter().subscribeWeak(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        exampleJList.setCellRenderer(new JIPipeNodeExampleListCellRenderer());
        exampleJList.setToolTipText("Double-click to load an example");
        exampleJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    loadExample();
                }
            }
        });
        add(new JScrollPane(exampleJList), BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);

        toolBar.add(Box.createHorizontalGlue());

        JButton loadExampleButton = new JButton("Load example", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        loadExampleButton.addActionListener(e -> loadExample());
        toolBar.add(loadExampleButton);
    }

    private void loadExample() {
        JIPipeNodeExample example = exampleJList.getSelectedValue();
        if (example == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this), "Do you really want to load the example '" + example.getNodeTemplate().getName() + "'?\n" +
                "This will override all your existing settings.", "Load example", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
            return;
        }
        algorithm.loadExample(example);
        getDesktopWorkbench().sendStatusBarText("Loaded example '" + example.getNodeTemplate().getName() + "' into " + algorithm.getDisplayName());
        if (dockPanel != null) {
            dockPanel.activatePanel("PARAMETERS", true);
        }
    }

    private void reloadList() {
        DefaultListModel<JIPipeNodeExample> model = new DefaultListModel<>();
        for (JIPipeNodeExample example : getProject().getNodeExamples(algorithm.getInfo().getId())) {
            model.addElement(example);
        }
        exampleJList.setModel(model);
    }

    @Override
    public void onJIPipeNodeTemplatesRefreshed(NodeTemplatesRefreshedEvent event) {
        reloadList();
    }
}
