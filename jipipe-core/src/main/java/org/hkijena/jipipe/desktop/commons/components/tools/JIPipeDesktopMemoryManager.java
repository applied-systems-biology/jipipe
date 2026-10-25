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

package org.hkijena.jipipe.desktop.commons.components.tools;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.tools.memorymonitor.JIPipeDesktopMemoryMonitorWindow;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Component for managing and reviewing the memory usageBytes
 */
public class JIPipeDesktopMemoryManager extends JIPipeDesktopProjectWorkbenchPanel {

    private final JIPipeDesktopMemoryGraph memoryGraph;
    private final JPopupMenu optionsMenu = new JPopupMenu();

    public JIPipeDesktopMemoryManager(JIPipeDesktopProjectWorkbench workbenchUI) {
        super(workbenchUI);
        memoryGraph = new JIPipeDesktopMemoryGraph(ThemeUtils.getCurrentStyle().getFontSizeTiny(), false);
        initialize();
        memoryGraph.startRepaintTimer();
    }

    private void initialize() {
        setOpaque(false);
        setLayout(new BorderLayout());
        int width = 200;
        memoryGraph.setPreferredSize(new Dimension(width, 16));
        memoryGraph.setMinimumSize(new Dimension(width, 16));
        memoryGraph.setMaximumSize(new Dimension(width, Short.MAX_VALUE));
        add(createOptionsButton(), BorderLayout.WEST);
        add(memoryGraph, BorderLayout.CENTER);

        memoryGraph.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 0, ThemeUtils.getCurrentStyle().getBorderColor()));
        memoryGraph.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                rebuildOptionsMenu();
                optionsMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    public JButton createOptionsButton() {
        JButton button = new JButton(JIPipe.RESOURCES.getIcon16("devices/media-memory.png"));
        UIUtils.makeButtonFlat25x25(button);
        UIUtils.addReloadablePopupMenuToButton(button, optionsMenu, this::rebuildOptionsMenu);
        return button;
    }

    private void rebuildOptionsMenu() {
        optionsMenu.removeAll();
        optionsMenu.add(UIUtils.createMenuItem("Run garbage collector",
                "Runs the garbage collector (GC) that attempts to clean unused memory. Please note that this will shortly freeze the application.",
                JIPipe.RESOURCES.getIcon16("actions/clear-brush.png"),
                this::doMemoryCleanup));
        optionsMenu.add(UIUtils.createMenuItem("Open memory monitor",
                "Opens a monitor tool for the memory.",
                JIPipe.RESOURCES.getIcon16("actions/preview.png"),
                this::doOpenMemoryMonitor));
    }

    private void doOpenMemoryMonitor() {
        JIPipeDesktopMemoryMonitorWindow window = new JIPipeDesktopMemoryMonitorWindow(getDesktopWorkbench());
        window.pack();
        window.setSize(1024, 768);
        window.setLocationRelativeTo(getDesktopWorkbench().getWindow());
        window.setVisible(true);
    }

    private void doMemoryCleanup() {
        System.gc();
        getDesktopWorkbench().sendStatusBarText("Unused memory was freed");
    }
}
