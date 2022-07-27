package org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.properties;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.extensions.nodeexamples.JIPipeNodeExampleListCellRenderer;
import org.hkijena.jipipe.extensions.settings.NodeTemplateSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class NodeExamplesUI extends JIPipeProjectWorkbenchPanel {

    private final JIPipeAlgorithm algorithm;
    private final JList<JIPipeNodeExample> exampleJList = new JList<>();

    public NodeExamplesUI(JIPipeProjectWorkbench workbench, JIPipeAlgorithm algorithm) {
        super(workbench);
        this.algorithm = algorithm;
        initialize();
        reloadList();
        NodeTemplateSettings.getInstance().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        exampleJList.setCellRenderer(new JIPipeNodeExampleListCellRenderer());
        exampleJList.setToolTipText("Double-click to load an example");
        exampleJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if(mouseEvent.getClickCount() == 2) {
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
        loadExampleButton.addActionListener(e->loadExample());
        toolBar.add(loadExampleButton);
    }

    private void loadExample() {
        JIPipeNodeExample example = exampleJList.getSelectedValue();
        if(example == null) {
            return;
        }
        if(JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this), "Do you really want to load the example '" + example.getNodeTemplate().getName() + "'?\n" +
                "This will override all your existing settings.", "Load example", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
            return;
        }
        algorithm.loadExample(example);
        getWorkbench().sendStatusBarText("Loaded example '" + example.getNodeTemplate().getName() + "' into " + algorithm.getDisplayName());
    }

    private void reloadList() {
        DefaultListModel<JIPipeNodeExample> model = new DefaultListModel<>();
        for (JIPipeNodeExample example : getProject().getNodeExamples(algorithm.getInfo().getId())) {
            model.addElement(example);
        }
        exampleJList.setModel(model);
    }

    @Subscribe
    public void onNodeTemplatesRefreshed(NodeTemplateSettings.NodeTemplatesRefreshedEvent event) {
        reloadList();
    }
}