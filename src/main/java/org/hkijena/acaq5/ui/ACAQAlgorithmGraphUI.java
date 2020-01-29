package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.api.ACAQAlgorithmGraph;
import org.hkijena.acaq5.ui.components.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class ACAQAlgorithmGraphUI extends ACAQUIPanel  {

    private ACAQAlgorithmGraphCanvasUI graphUI;
    protected  JToolBar toolBar = new JToolBar();
    private ACAQAlgorithmGraph algorithmGraph;

    public ACAQAlgorithmGraphUI(ACAQWorkbenchUI workbenchUI, ACAQAlgorithmGraph algorithmGraph) {
        super(workbenchUI);
        this.algorithmGraph = algorithmGraph;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        graphUI = new ACAQAlgorithmGraphCanvasUI(algorithmGraph);
        add(new JScrollPane(graphUI) {
            {
                setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
                setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            }
        }, BorderLayout.CENTER);

        add(toolBar, BorderLayout.NORTH);
        initializeToolbar();
    }

    protected void initializeToolbar() {
        toolBar.add(Box.createHorizontalGlue());

        JButton autoLayoutButton = new JButton("Auto layout", UIUtils.getIconFromResources("sort.png"));
        toolBar.add(autoLayoutButton);
    }

    public ACAQAlgorithmGraph getAlgorithmGraph() {
        return algorithmGraph;
    }
}
