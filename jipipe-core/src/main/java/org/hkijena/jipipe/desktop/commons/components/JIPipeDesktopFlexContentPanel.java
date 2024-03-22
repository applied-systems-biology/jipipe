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

package org.hkijena.jipipe.desktop.commons.components;

import org.apache.commons.lang3.ArrayUtils;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * A panel that holds a toolbar, ribbon, and sidebar.
 * Automatically creates the components when they are used
 */
public class JIPipeDesktopFlexContentPanel extends JPanel {

    public static final int NONE = 0;
    public static final int WITH_RIBBON = 1;
    public static final int WITH_TOOLBAR = 2;

    public static final int WITH_PIN_TOOLBAR = 4;
    public static final int WITH_SIDEBAR = 8;
    private final JToggleButton sideBarToggle = new JToggleButton(UIUtils.getIcon16FromResources("actions/sidebar.png"));
    private final JPanel contentPanel = new JPanel(new BorderLayout());
    private JIPipeDesktopRibbon ribbon;
    private JToolBar toolBar;
    private JToolBar pinToolBar;
    private JIPipeDesktopTabPane sideBar;
    private boolean sideBarVisible = true;

    public JIPipeDesktopFlexContentPanel() {
        this(NONE);
    }

    public JIPipeDesktopFlexContentPanel(int flags) {
        if ((flags & WITH_RIBBON) == WITH_RIBBON) {
            ribbon = new JIPipeDesktopRibbon();
        }
        if ((flags & WITH_TOOLBAR) == WITH_TOOLBAR) {
            toolBar = new JToolBar();
            toolBar.setFloatable(false);
        }
        if ((flags & WITH_PIN_TOOLBAR) == WITH_PIN_TOOLBAR) {
            pinToolBar = new JToolBar();
            pinToolBar.setFloatable(false);
        }
        if ((flags & WITH_SIDEBAR) == WITH_SIDEBAR) {
            sideBar = new JIPipeDesktopTabPane(false, JIPipeDesktopTabPane.TabPlacement.Right);
        }
        initialize();
        rebuildLayout();
    }

    private void initialize() {
        sideBarToggle.setToolTipText("Show/hide sidebar");
        sideBarToggle.addActionListener(e -> {
            setSideBarVisible(sideBarToggle.isSelected());
        });
        sideBarToggle.setSelected(sideBarVisible);
    }

    public JIPipeDesktopRibbon getRibbon() {
        if (ribbon == null) {
            ribbon = new JIPipeDesktopRibbon();
            rebuildLayout();
        }
        return ribbon;
    }

    public void rebuildLayout() {
        removeAll();
        setLayout(new BorderLayout());
        if (sideBar != null) {
            ensurePinToolBar();
        }
        if (pinToolBar != null) {
            ensureToolBar();
        }
        JPanel toolBarPanel = null;
        if (toolBar != null) {
            toolBar.setMaximumSize(new Dimension(Short.MAX_VALUE, 100));
            toolBarPanel = UIUtils.boxHorizontal(toolBar, pinToolBar);
        }
        JPanel topPanel = UIUtils.boxVertical(toolBarPanel, ribbon);
        add(topPanel, BorderLayout.NORTH);
        if (sideBar != null) {
            if (!ArrayUtils.contains(pinToolBar.getComponents(), sideBarToggle)) {
                pinToolBar.add(sideBarToggle);
            }
        }
        JComponent centerPanel;
        if (sideBar != null && sideBarVisible) {
            centerPanel = new AutoResizeSplitPane(AutoResizeSplitPane.LEFT_RIGHT, contentPanel, sideBar, new AutoResizeSplitPane.DynamicSidebarRatio(450, false));
        } else {
            centerPanel = contentPanel;
        }
        add(centerPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JToolBar ensureToolBar() {
        if (toolBar == null) {
            toolBar = new JToolBar();
            toolBar.setFloatable(false);
        }
        return toolBar;
    }

    public JToolBar getToolBar() {
        if (toolBar == null) {
            toolBar = new JToolBar();
            toolBar.setFloatable(false);
            rebuildLayout();
        }
        return toolBar;
    }

    private JToolBar ensurePinToolBar() {
        if (pinToolBar == null) {
            pinToolBar = new JToolBar();
            pinToolBar.setFloatable(false);
        }
        return pinToolBar;
    }

    public JToolBar getPinToolBar() {
        if (pinToolBar == null) {
            pinToolBar = new JToolBar();
            pinToolBar.setFloatable(false);
            rebuildLayout();
        }
        return pinToolBar;
    }

    private JIPipeDesktopTabPane ensureSideBar() {
        if (sideBar == null) {
            sideBar = new JIPipeDesktopTabPane(false, JIPipeDesktopTabPane.TabPlacement.Right);
        }
        return sideBar;
    }

    public JIPipeDesktopTabPane getSideBar() {
        if (sideBar == null) {
            sideBar = new JIPipeDesktopTabPane(false, JIPipeDesktopTabPane.TabPlacement.Right);
            rebuildLayout();
        }
        return sideBar;
    }

    public boolean isSideBarVisible() {
        return sideBarVisible;
    }

    public void setSideBarVisible(boolean sideBarVisible) {
        this.sideBarVisible = sideBarVisible;
        this.sideBarToggle.setSelected(sideBarVisible);
        rebuildLayout();
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }
}
