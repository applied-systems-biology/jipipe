package org.hkijena.jipipe.utils.ui;

import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopVerticalToolBar;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class FloatingDockPanel extends JPanel {

    private final JIPipeDesktopVerticalToolBar leftToolBar = new JIPipeDesktopVerticalToolBar();
    private final JIPipeDesktopVerticalToolBar rightToolBar = new JIPipeDesktopVerticalToolBar();
    private final JLayeredPane layeredPane = new JLayeredPane();
    private final JPanel layeredPaneBackground = new JPanel(new BorderLayout());
    private final JPanel leftFloatingPanel = new JPanel(new BorderLayout());
    private final JPanel rightFloatingPanel = new JPanel(new BorderLayout());
    private final JPanel leftResizerPanel = new JPanel();
    private final JPanel rightResizerPanel = new JPanel();
    private int floatingMargin = 8;
    private int leftFloatingSize = 350;
    private int rightFloatingSize = 350;
    private int minimumFloatingSize = 50;
    private JComponent leftFloatingPanelContent;
    private JComponent rightFloatingPanelContent;
    private final Map<String, Panel> floatingPanels = new HashMap<>();
    private final Map<String, JToggleButton> floatingPanelButtons = new HashMap<>();

    public FloatingDockPanel() {
        super(new BorderLayout());
        initialize();
        updateToolbars();
        updateContent();
    }

    private void initialize() {
        add(leftToolBar, BorderLayout.WEST);
        add(rightToolBar, BorderLayout.EAST);

//        layeredPane.setLayout(new OverlayLayout(layeredPane));
        add(layeredPane, BorderLayout.CENTER);

        layeredPane.add(layeredPaneBackground, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(leftFloatingPanel, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(rightFloatingPanel, JLayeredPane.PALETTE_LAYER);

        initializeLeftFloatingPanel();
        initializeRightFloatingPanel();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateSizes();
            }
        });
    }

    private void initializeRightFloatingPanel() {
        rightFloatingPanel.setBorder(UIUtils.createPanelBorder());
        rightFloatingPanel.add(rightResizerPanel, BorderLayout.WEST);
        rightResizerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        rightResizerPanel.setPreferredSize(new Dimension(8, 64));
        rightResizerPanel.setMinimumSize(new Dimension(8, 32));
//        rightResizerPanel.setBackground(Color.RED);
        rightResizerPanel.setOpaque(false);

        rightResizerPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point convertedPoint = SwingUtilities.convertPoint(rightResizerPanel, e.getPoint(), layeredPane);
                rightFloatingSize = Math.max(minimumFloatingSize, layeredPane.getWidth() - convertedPoint.x);
                updateSizes();
            }


        });
    }

    private void initializeLeftFloatingPanel() {
//        leftFloatingPanel.setOpaque(false);
        leftFloatingPanel.setBorder(UIUtils.createPanelBorder());
        leftFloatingPanel.add(leftResizerPanel, BorderLayout.EAST);
        leftResizerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        leftResizerPanel.setPreferredSize(new Dimension(8, 64));
        leftResizerPanel.setMinimumSize(new Dimension(8, 32));
//        leftResizerPanel.setBackground(Color.RED);
        leftResizerPanel.setOpaque(false);

        leftResizerPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point convertedPoint = SwingUtilities.convertPoint(leftResizerPanel, e.getPoint(), layeredPane);
                leftFloatingSize = Math.max(minimumFloatingSize, convertedPoint.x);
                updateSizes();
            }
        });
    }

    public void setBackgroundComponent(JComponent component) {
        layeredPaneBackground.removeAll();
        layeredPaneBackground.add(component, BorderLayout.CENTER);
        revalidate();
        repaint();
        updateSizes();
    }

    private void updateSizes() {

        layeredPaneBackground.setBounds(0, 0, layeredPane.getWidth(), getHeight());

        int availableWidth = layeredPaneBackground.getWidth();
        Dimension leftSize = new Dimension(leftFloatingSize, getHeight() - 2 * floatingMargin);
        Dimension rightSize = new Dimension(rightFloatingSize, getHeight() - 2 * floatingMargin);


        leftFloatingPanel.setPreferredSize(leftSize);
        rightFloatingPanel.setPreferredSize(rightSize);
        leftFloatingPanel.setMaximumSize(leftSize);
        rightFloatingPanel.setMaximumSize(rightSize);


//        leftFloatingPanel.revalidate();
//        rightFloatingPanel.revalidate();

        leftFloatingPanel.setBounds(floatingMargin, floatingMargin, leftSize.width, leftSize.height);
        rightFloatingPanel.setBounds(availableWidth - floatingMargin - rightFloatingSize - 2, floatingMargin, rightSize.width, rightSize.height);

        revalidate();
        repaint();
    }

    private void updateContent() {
        if(leftFloatingPanelContent != null) {
            leftFloatingPanel.remove(leftFloatingPanelContent);
        }
        if(rightFloatingPanelContent != null) {
            rightFloatingPanel.remove(rightFloatingPanelContent);
        }
        leftFloatingPanelContent = null;
        rightFloatingPanelContent = null;

        List<JComponent> leftContent = new ArrayList<>();
        List<JComponent> rightContent = new ArrayList<>();

        for (Panel panel : getPanelsAtLocation(PanelLocation.TopLeft)) {
            if(panel.isDisplayed()) {
                leftContent.add(panel.getComponent());
                break;
            }
        }
        for (Panel panel : getPanelsAtLocation(PanelLocation.BottomLeft)) {
            if(panel.isDisplayed()) {
                leftContent.add(panel.getComponent());
                break;
            }
        }
        for (Panel panel : getPanelsAtLocation(PanelLocation.TopRight)) {
            if(panel.isDisplayed()) {
                rightContent.add(panel.getComponent());
                break;
            }
        }
        for (Panel panel : getPanelsAtLocation(PanelLocation.BottomRight)) {
            if(panel.isDisplayed()) {
                rightContent.add(panel.getComponent());
                break;
            }
        }

        if(leftContent.size() >= 2) {
            // create split pane
            leftFloatingPanelContent = new AutoResizeSplitPane(AutoResizeSplitPane.TOP_BOTTOM, leftContent.get(0), leftContent.get(1), 0.5);
        }
        else if(leftContent.size() == 1) {
            // use directly
            leftFloatingPanelContent = leftContent.get(0);
        }

        if(rightContent.size() >= 2) {
            // create split pane
            rightFloatingPanelContent = new AutoResizeSplitPane(AutoResizeSplitPane.TOP_BOTTOM, rightContent.get(0), rightContent.get(1), 0.5);
        }
        else if(rightContent.size() == 1) {
            // use directly
            rightFloatingPanelContent = rightContent.get(0);
        }

        // Rebuild panel
        if(leftFloatingPanelContent != null) {
            leftFloatingPanel.setVisible(true);
            leftFloatingPanel.add(leftFloatingPanelContent, BorderLayout.CENTER);
        }
        else {
            leftFloatingPanel.setVisible(false);
        }
        if(rightFloatingPanelContent != null) {
            rightFloatingPanel.setVisible(true);
            rightFloatingPanel.add(rightFloatingPanelContent, BorderLayout.CENTER);
        }
        else {
            rightFloatingPanel.setVisible(false);
        }

        // Revalidate and repaint
        layeredPane.revalidate();
        layeredPane.repaint(50);
    }

    private void updateToolbars() {
        leftToolBar.removeAll();
        rightToolBar.removeAll();
        for (Panel panel : getPanelsAtLocation(PanelLocation.TopLeft)) {
            leftToolBar.add(createToggleButton(panel));
        }
        leftToolBar.add(Box.createVerticalGlue());
        for (Panel panel : getPanelsAtLocation(PanelLocation.BottomLeft)) {
            leftToolBar.add(createToggleButton(panel));
        }

        for (Panel panel : getPanelsAtLocation(PanelLocation.TopRight)) {
            rightToolBar.add(createToggleButton(panel));
        }
        rightToolBar.add(Box.createVerticalGlue());
        for (Panel panel : getPanelsAtLocation(PanelLocation.BottomRight)) {
            rightToolBar.add(createToggleButton(panel));
        }
    }

    private JToggleButton createToggleButton(Panel panel) {
        JToggleButton button = new JToggleButton(panel.getIcon());
        button.setSelected(panel.isDisplayed());
        button.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        button.setToolTipText(panel.getName());
        button.addActionListener(e -> {
            if(button.isSelected()) {
                // Deactivate all other buttons
                for (Panel otherPanel : getPanelsAtLocation(panel.getLocation())) {
                    if(otherPanel != panel) {
                        otherPanel.setVisible(false);
                        floatingPanelButtons.get(otherPanel.getId()).setSelected(false);
                    }
                }

                panel.setVisible(true);
            }
            else {
                panel.setVisible(false);
            }
            updateContent();
        });
        JPopupMenu popupMenu = UIUtils.addRightClickPopupMenuToButton(button);
        popupMenu.add(UIUtils.createMenuItem("Top left", "Move the panel to the top left anchor", UIUtils.getIconFromResources("actions/dock-top-left.png"), () -> {
            movePanelToLocation(panel, PanelLocation.TopLeft);
        }));
        popupMenu.add(UIUtils.createMenuItem("Bottom left", "Move the panel to the bottom left anchor", UIUtils.getIconFromResources("actions/dock-bottom-left.png"), () -> {
            movePanelToLocation(panel, PanelLocation.BottomLeft);
        }));
        popupMenu.add(UIUtils.createMenuItem("Top right", "Move the panel to the top right anchor", UIUtils.getIconFromResources("actions/dock-top-right.png"), () -> {
            movePanelToLocation(panel, PanelLocation.TopRight);
        }));
        popupMenu.add(UIUtils.createMenuItem("Bottom right", "Move the panel to the bottom right anchor", UIUtils.getIconFromResources("actions/dock-bottom-right.png"), () -> {
            movePanelToLocation(panel, PanelLocation.BottomRight);
        }));

        floatingPanelButtons.put(panel.getId(), button);
        return button;
    }

    public List<Panel> getPanelsAtLocation(PanelLocation location) {
        return floatingPanels.values().stream().filter(panel -> panel.getLocation() == location).collect(Collectors.toList());
    }

    private void movePanelToLocation(Panel panel, PanelLocation newLocation) {
        PanelLocation oldLocation = panel.getLocation();
        if(oldLocation != null && !oldLocation.equals(newLocation)) {

            if(panel.isDisplayed()) {
                if(getCurrentlyVisiblePanelId(newLocation, false) == null) {
                    for (Panel otherPanel : getPanelsAtLocation(newLocation)) {
                        otherPanel.setVisible(false);
                    }
                    panel.setVisible(true);
                }
            }

            panel.setLocation(newLocation);

            updateToolbars();
            updateContent();
        }
    }

    public String getCurrentlyVisiblePanelId(PanelLocation location, boolean withHidden) {
        for (Panel panel : getPanelsAtLocation(location)) {
            if(withHidden) {
                if(panel.isVisible()) {
                    return panel.getId();
                }
            }
            else {
                if(panel.isDisplayed()) {
                    return panel.getId();
                }
            }
        }
        return null;
    }

    public void removeDockPanel(String id) {
        if(floatingPanels.remove(id) != null) {
            updateToolbars();
            updateContent();
        }
    }

    public void addDockPanel(String id, String name, Icon icon, PanelLocation location, boolean visible, JComponent component) {
        removeDockPanel(id);

        Panel panel = new Panel(id);
        panel.setLocation(location);
        panel.setComponent(component);
        panel.setIcon(icon);
        panel.setName(name);

        if(visible) {
            String visiblePanelId = getCurrentlyVisiblePanelId(location, true);
            if(visiblePanelId != null && !id.equals(visiblePanelId)) {
                floatingPanels.get(visiblePanelId).setVisible(false);
            }
            panel.setVisible(true);
        }

        floatingPanels.put(id, panel);
        updateToolbars();
        updateContent();
    }

    public enum PanelLocation {
        TopLeft,
        BottomLeft,
        TopRight,
        BottomRight
    }

    public int getLeftFloatingSize() {
        return leftFloatingSize;
    }

    public void setLeftFloatingSize(int leftFloatingSize) {
        this.leftFloatingSize = leftFloatingSize;
        updateSizes();
    }

    public int getRightFloatingSize() {
        return rightFloatingSize;
    }

    public void setRightFloatingSize(int rightFloatingSize) {
        this.rightFloatingSize = rightFloatingSize;
        updateSizes();
    }

    public int getMinimumFloatingSize() {
        return minimumFloatingSize;
    }

    public void setMinimumFloatingSize(int minimumFloatingSize) {
        this.minimumFloatingSize = minimumFloatingSize;
        updateSizes();
    }

    public int getFloatingMargin() {
        return floatingMargin;
    }

    public void setFloatingMargin(int floatingMargin) {
        this.floatingMargin = floatingMargin;
        updateSizes();
    }

    public static class Panel {
        private final String id;
        private Icon icon;
        private JComponent component;
        private String name;
        private PanelLocation location;
        private boolean visible;

        public Panel(String id) {
            this.id = id;
        }

        public PanelLocation getLocation() {
            return location;
        }

        public void setLocation(PanelLocation location) {
            this.location = location;
        }

        public String getId() {
            return id;
        }

        public Icon getIcon() {
            return icon;
        }

        public void setIcon(Icon icon) {
            this.icon = icon;
        }

        public JComponent getComponent() {
            return component;
        }

        public void setComponent(JComponent component) {
            this.component = component;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public boolean isVisible() {
            return visible;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public boolean isDisplayed() {
            return visible && component != null;
        }
    }
}
