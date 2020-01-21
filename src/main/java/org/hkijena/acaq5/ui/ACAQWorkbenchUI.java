package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.ACAQCommand;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class ACAQWorkbenchUI extends JFrame {

    private ACAQCommand command;
    private DocumentTabPane documentTabPane;
    private ACAQInfoUI infoUI;

    public ACAQWorkbenchUI(ACAQCommand command) {
        this.command = command;
        initialize();
    }

    private void initialize() {
        getContentPane().setLayout(new BorderLayout(8, 8));
        setTitle("ACAQ 5");
        setIconImage(UIUtils.getIconFromResources("acaq5.png").getImage());
        UIUtils.setToAskOnClose(this, "Do you really want to close ACAQ5?", "Close window");

        infoUI = new ACAQInfoUI();

        documentTabPane = new DocumentTabPane();
        documentTabPane.addSingletonTab("INTRODUCTION", "Introduction", UIUtils.getIconFromResources("info.png"), infoUI, false);
        add(documentTabPane, BorderLayout.CENTER);

        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();

        toolBar.add(Box.createHorizontalGlue());
        initializeToolbarHelpMenu(toolBar);

        add(toolBar, BorderLayout.NORTH);
    }

    private void initializeToolbarHelpMenu(JToolBar toolBar) {
        JButton helpButton = new JButton(UIUtils.getIconFromResources("help.png"));
        JPopupMenu menu = UIUtils.addPopupMenuToComponent(helpButton);

        JMenuItem quickHelp = new JMenuItem("Quick introduction", UIUtils.getIconFromResources("quickload.png"));
        quickHelp.addActionListener(e -> documentTabPane.selectSingletonTab("INTRODUCTION"));
        menu.add(quickHelp);

        toolBar.add(helpButton);
    }
}
