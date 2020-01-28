package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.api.ACAQAlgorithmGraph;
import org.hkijena.acaq5.ui.components.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class ACAQAlgorithmGraphUI extends ACAQUIPanel  {

    private ACAQAlgorithmGraphCanvasUI graphUI;
    protected  JToolBar toolBar = new JToolBar();

    public ACAQAlgorithmGraphUI(ACAQWorkbenchUI workbenchUI) {
        super(workbenchUI);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        graphUI = new ACAQAlgorithmGraphCanvasUI(new ACAQAlgorithmGraph());
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

}
