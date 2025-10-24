package org.hkijena.jipipe.utils.ui;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopVerticalToolBar;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class JIPipeDesktopDockPanel extends JPanel implements JIPipeDesktopSplitPane.RatioUpdatedEventListener {

    public static final int UI_ORDER_PINNED = -100;
    public static final int UI_ORDER_DEFAULT = 0;
    public static final int BUTTON_MAX_WIDTH = 64;
    private static final int RESIZE_HANDLE_SIZE = 6;
    private final JIPipeDesktopVerticalToolBar leftToolBar = new JIPipeDesktopVerticalToolBar();
    private final JIPipeDesktopVerticalToolBar rightToolBar = new JIPipeDesktopVerticalToolBar();
    private final JLayeredPane layeredPane = new JLayeredPane();
    private final JPanel layeredPaneMain = new JPanel(new BorderLayout());
    private final JPanel layeredPaneLeft = new JPanel(new BorderLayout());
    private final JPanel layeredPaneRight = new JPanel(new BorderLayout());
    private final JPanel leftResizerPanel = new JPanel();
    private final JPanel rightResizerPanel = new JPanel();
    private final Map<String, Panel> panels = new LinkedHashMap<>();
    private final Map<String, JToggleButton> panelVisibilityToggles = new HashMap<>();
    private final JIPipeDesktopSplitPane leftSplitPane = new JIPipeDesktopSplitPane(JIPipeDesktopSplitPane.TOP_BOTTOM, new JIPipeDesktopSplitPane.FixedRatio(0.33, true));
    private final JIPipeDesktopSplitPane rightSplitPane = new JIPipeDesktopSplitPane(JIPipeDesktopSplitPane.TOP_BOTTOM, new JIPipeDesktopSplitPane.FixedRatio(0.66, true));
    private final JIPipeDesktopSplitPane mainSplitPane = new JIPipeDesktopSplitPane(JIPipeDesktopSplitPane.TOP_BOTTOM, new JIPipeDesktopSplitPane.FixedRatio(0.66, true));
    private final StateSavedEventEmitter stateSavedEventEmitter = new StateSavedEventEmitter();
    private final PanelSideVisibilityChangedEventEmitter panelSideVisibilityChangedEventEmitter = new PanelSideVisibilityChangedEventEmitter();
    private final JIPipeParameterCollection.ParameterChangedEventEmitter parameterChangedEventEmitter = new JIPipeParameterCollection.ParameterChangedEventEmitter();
    private final JCheckBoxMenuItem showToolbarLabelsMenuItem = new JCheckBoxMenuItem("Show Toolbar Labels");
    private final boolean usingModernTheme = ThemeUtils.isUsingModernTheme();
    private int leftPanelWidth = 350;
    private int rightPanelWidth = 500;
    private int minimumPanelWidth = 150;
    private int minimumPanelHeight = 150;
    private int minimumBackgroundWidth = 150;
    private JComponent leftPanelContent;
    private JComponent rightPanelContent;
    private State savedState = new State();
    private boolean showToolbarLabels = true;
    private int toolbarWithLabelsWidth = 92;
    private int toolbarWithoutLabelsWidth = 42;
    private boolean hideToolbars = false;
    private boolean alwaysShowLeftPanel = false;
    private boolean alwaysShowRightPanel = false;
    private JComponent mainComponent;

    public JIPipeDesktopDockPanel() {
        super(new BorderLayout());
        initialize();
        updateAll();
    }

    private void initialize() {
        add(leftToolBar, BorderLayout.WEST);
        add(rightToolBar, BorderLayout.EAST);

        if (usingModernTheme) {
            setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());

            rightToolBar.setOpaque(false);
            rightToolBar.setBorder(BorderFactory.createEmptyBorder());
            leftToolBar.setOpaque(false);
            leftToolBar.setBorder(BorderFactory.createEmptyBorder());
        } else {
            rightToolBar.setBorder(UIUtils.createPanelBorder(0, 0, 1, 0));
            leftToolBar.setBorder(UIUtils.createPanelBorder(1, 0, 0, 0));
        }

        JPopupMenu toolbarContextMenu = new JPopupMenu();
        UIUtils.addReloadableRightClickPopupMenuToComponent(leftToolBar, toolbarContextMenu, (menu) -> reloadContextMenu(menu, false));
        UIUtils.addReloadableRightClickPopupMenuToComponent(rightToolBar, toolbarContextMenu, (menu) -> reloadContextMenu(menu, true));

        showToolbarLabelsMenuItem.setState(showToolbarLabels);
        showToolbarLabelsMenuItem.addActionListener(e -> {
            setShowToolbarLabels(showToolbarLabelsMenuItem.getState());
        });

//        layeredPane.setLayout(new OverlayLayout(layeredPane));
        add(layeredPane, BorderLayout.CENTER);

        layeredPane.add(layeredPaneMain, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(layeredPaneLeft, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(layeredPaneRight, JLayeredPane.PALETTE_LAYER);

        initializeLeftPanel();
        initializeRightPanel();
        initializeBottomPanel();

        leftSplitPane.setDividerSize(RESIZE_HANDLE_SIZE);
        rightSplitPane.setDividerSize(RESIZE_HANDLE_SIZE);
        mainSplitPane.setDividerSize(RESIZE_HANDLE_SIZE);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateSizes();
            }
        });
    }

    private void initializeBottomPanel() {
        mainSplitPane.getRatioUpdatedEventEmitter().subscribe(this);
    }

    private void reloadContextMenu(JPopupMenu menu, boolean right) {
        menu.removeAll();
        menu.add(showToolbarLabelsMenuItem);
        if (right) {
            JCheckBoxMenuItem pin = new JCheckBoxMenuItem("Always show right panel");
            pin.setState(alwaysShowRightPanel);
            pin.addActionListener(e -> {
                setAlwaysShowRightPanel(pin.getState());
                saveState();
            });
            menu.add(pin);
        } else {
            JCheckBoxMenuItem pin = new JCheckBoxMenuItem("Always show left panel");
            pin.setState(alwaysShowLeftPanel);
            pin.addActionListener(e -> {
                setAlwaysShowLeftPanel(pin.getState());
                saveState();
            });
            menu.add(pin);
        }
    }

    private void initializeRightPanel() {
        layeredPaneRight.setBorder(null);
        layeredPaneRight.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
        layeredPaneRight.add(rightResizerPanel, BorderLayout.WEST);
        rightResizerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        rightResizerPanel.setPreferredSize(new Dimension(RESIZE_HANDLE_SIZE, 64));
        rightResizerPanel.setMinimumSize(new Dimension(RESIZE_HANDLE_SIZE, 32));
        rightResizerPanel.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());

        rightResizerPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point convertedPoint = SwingUtilities.convertPoint(rightResizerPanel, e.getPoint(), layeredPane);
                rightPanelWidth = Math.max(minimumPanelWidth, layeredPane.getWidth() - convertedPoint.x);
                saveState();
                updateSizes();
            }


        });
        rightSplitPane.getRatioUpdatedEventEmitter().subscribe(this);
    }

    private void initializeLeftPanel() {
//        leftFloatingPanel.setOpaque(false);
        layeredPaneLeft.setBorder(null);
        layeredPaneLeft.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
        layeredPaneLeft.add(leftResizerPanel, BorderLayout.EAST);
        leftResizerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        leftResizerPanel.setPreferredSize(new Dimension(RESIZE_HANDLE_SIZE, 64));
        leftResizerPanel.setMinimumSize(new Dimension(RESIZE_HANDLE_SIZE, 32));
        leftResizerPanel.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());

        leftResizerPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point convertedPoint = SwingUtilities.convertPoint(leftResizerPanel, e.getPoint(), layeredPane);
                leftPanelWidth = Math.max(minimumPanelWidth, convertedPoint.x);
                saveState();
                updateSizes();
            }
        });
        leftSplitPane.getRatioUpdatedEventEmitter().subscribe(this);
    }

    public void setMainComponent(JComponent component) {
        this.mainComponent = component;
        revalidate();
        repaint();
        updateAll();
    }

    public boolean isShowToolbarLabels() {
        return showToolbarLabels;
    }

    public void setShowToolbarLabels(boolean showToolbarLabels) {
        if (showToolbarLabels != this.showToolbarLabels) {
            this.showToolbarLabels = showToolbarLabels;
            updateToolbars();
            SwingUtilities.invokeLater(() -> {
                updateSizes();
                SwingUtilities.invokeLater(this::updateSizes);
            });
            parameterChangedEventEmitter.emit(new JIPipeParameterCollection.ParameterChangedEvent(this, "show-toolbar-labels"));
        }
        if (showToolbarLabelsMenuItem.getState() != showToolbarLabels) {
            showToolbarLabelsMenuItem.setState(showToolbarLabels);
        }
    }

    public JIPipeParameterCollection.ParameterChangedEventEmitter getParameterChangedEventEmitter() {
        return parameterChangedEventEmitter;
    }

    public int getToolbarWithLabelsWidth() {
        return toolbarWithLabelsWidth;
    }

    public void setToolbarWithLabelsWidth(int toolbarWithLabelsWidth) {
        this.toolbarWithLabelsWidth = toolbarWithLabelsWidth;
        updateAll();
    }

    public int getToolbarWithoutLabelsWidth() {
        return toolbarWithoutLabelsWidth;
    }

    public void setToolbarWithoutLabelsWidth(int toolbarWithoutLabelsWidth) {
        this.toolbarWithoutLabelsWidth = toolbarWithoutLabelsWidth;
        updateAll();
    }

    public void updateSizes() {

        int availableWidth = layeredPane.getWidth();

        int leftMarginLeft = 0;
        int leftMarginTop = 0;
        int leftMarginBottom = 0;
        int rightMarginRight = 0;
        int rightMarginTop = 0;
        int rightMarginBottom = 0;


        Dimension leftSize = new Dimension(leftPanelWidth, getHeight() - leftMarginTop - leftMarginBottom);
        Dimension rightSize = new Dimension(rightPanelWidth, getHeight() - rightMarginTop - rightMarginBottom);

        layeredPaneLeft.setPreferredSize(leftSize);
        layeredPaneRight.setPreferredSize(rightSize);
        layeredPaneLeft.setMaximumSize(leftSize);
        layeredPaneRight.setMaximumSize(rightSize);

        layeredPaneLeft.setBounds(leftMarginLeft, leftMarginTop, leftSize.width, leftSize.height);
        layeredPaneRight.setBounds(availableWidth - rightMarginRight - rightPanelWidth - 2, rightMarginTop, rightSize.width, rightSize.height);

        int backgroundLeft;
        int backgroundWidth;

        if (layeredPaneLeft.isVisible() && layeredPaneRight.isVisible()) {
            backgroundLeft = leftSize.width;
            backgroundWidth = layeredPane.getWidth() - (rightSize.width) - backgroundLeft;
        } else if (layeredPaneLeft.isVisible()) {
            backgroundLeft = leftSize.width;
            backgroundWidth = layeredPane.getWidth() - backgroundLeft;
        } else if (layeredPaneRight.isVisible()) {
            backgroundLeft = 0;
            backgroundWidth = layeredPane.getWidth() - rightSize.width;
        } else {
            backgroundLeft = 0;
            backgroundWidth = layeredPane.getWidth();
        }

        layeredPaneMain.setBounds(backgroundLeft, 0, Math.max(backgroundWidth, minimumBackgroundWidth) - 2, getHeight());
//        layeredPaneBackground.setBounds(100,100,100,100);
//        layeredPaneBackground.setBounds(0,0,layeredPane.getWidth(),getHeight());

        revalidate();
        repaint();
    }

    private void updateContent(List<PanelSideVisibilityChangedEvent> panelVisibilityChangedEvents) {
        if (leftPanelContent != null) {
            layeredPaneLeft.remove(leftPanelContent);
        }
        if (rightPanelContent != null) {
            layeredPaneRight.remove(rightPanelContent);
        }
        leftPanelContent = null;
        rightPanelContent = null;

        List<JComponent> leftContent = new ArrayList<>();
        List<JComponent> rightContent = new ArrayList<>();

        if (!hideToolbars) {
            for (Panel panel : getPanelsAtLocation(PanelLocation.TopLeft)) {
                if (panel.isDisplayed()) {
                    leftContent.add(panel.getComponent());
                    break;
                }
            }
            for (Panel panel : getPanelsAtLocation(PanelLocation.BottomLeft)) {
                if (panel.isDisplayed()) {
                    leftContent.add(panel.getComponent());
                    break;
                }
            }
            for (Panel panel : getPanelsAtLocation(PanelLocation.TopRight)) {
                if (panel.isDisplayed()) {
                    rightContent.add(panel.getComponent());
                    break;
                }
            }
            for (Panel panel : getPanelsAtLocation(PanelLocation.BottomRight)) {
                if (panel.isDisplayed()) {
                    rightContent.add(panel.getComponent());
                    break;
                }
            }
        }

        if (leftContent.size() >= 2) {
            // create split pane
            leftSplitPane.setLeftComponent(UIUtils.wrapInIslandPanelIfNeeded(leftContent.get(0)));
            leftSplitPane.setRightComponent(UIUtils.wrapInIslandPanelIfNeeded(leftContent.get(1)));
            leftPanelContent = leftSplitPane;
        } else if (leftContent.size() == 1) {
            // use directly
            leftPanelContent = UIUtils.wrapInIslandPanelIfNeeded(leftContent.get(0));
            leftSplitPane.setLeftComponent(new JPanel());
            leftSplitPane.setRightComponent(new JPanel());
        } else {
            leftSplitPane.setLeftComponent(new JPanel());
            leftSplitPane.setRightComponent(new JPanel());
        }

        if (rightContent.size() >= 2) {
            // create split pane
            rightSplitPane.setLeftComponent(UIUtils.wrapInIslandPanelIfNeeded(rightContent.get(0)));
            rightSplitPane.setRightComponent(UIUtils.wrapInIslandPanelIfNeeded(rightContent.get(1)));
            rightPanelContent = rightSplitPane;
        } else if (rightContent.size() == 1) {
            // use directly
            rightPanelContent = UIUtils.wrapInIslandPanelIfNeeded(rightContent.get(0));
            rightSplitPane.setLeftComponent(new JPanel());
            rightSplitPane.setRightComponent(new JPanel());
        } else {
            rightSplitPane.setLeftComponent(new JPanel());
            rightSplitPane.setRightComponent(new JPanel());
        }

        leftSplitPane.applyRatio();
        rightSplitPane.applyRatio();

        // Rebuild panel
        boolean oldLeftPanelVisible = layeredPaneLeft.isVisible();
        boolean oldRightPanelVisible = layeredPaneRight.isVisible();
        if (leftPanelContent != null) {
            layeredPaneLeft.setVisible(true);
            layeredPaneLeft.add(leftPanelContent, BorderLayout.CENTER);
        } else {
            layeredPaneLeft.setVisible(alwaysShowLeftPanel);
        }
        if (rightPanelContent != null) {
            layeredPaneRight.setVisible(true);
            layeredPaneRight.add(rightPanelContent, BorderLayout.CENTER);
        } else {
            layeredPaneRight.setVisible(alwaysShowRightPanel);
        }

        if (oldLeftPanelVisible != layeredPaneLeft.isVisible()) {
            panelVisibilityChangedEvents.add(new PanelSideVisibilityChangedEvent(this, PanelSide.Left, layeredPaneLeft.isVisible()));
        }
        if (oldRightPanelVisible != layeredPaneRight.isVisible()) {
            panelVisibilityChangedEvents.add(new PanelSideVisibilityChangedEvent(this, PanelSide.Right, layeredPaneRight.isVisible()));
        }

        // Rebuild the central panel if needed
        layeredPaneMain.removeAll();
        if (mainComponent != null) {
            Panel bottomPanel = null;
            for (Panel panel : getPanelsAtLocation(PanelLocation.BottomBottom)) {
                if (panel.isVisible()) {
                    bottomPanel = panel;
                    break;
                }
            }

            if (bottomPanel != null) {
                mainSplitPane.setTopComponent(UIUtils.wrapInIslandPanelIfNeeded(mainComponent));
                mainSplitPane.setBottomComponent(UIUtils.wrapInIslandPanelIfNeeded(bottomPanel.getComponent()));
                mainSplitPane.applyRatio();
                layeredPaneMain.add(mainSplitPane, BorderLayout.CENTER);
            } else {
                layeredPaneMain.add(UIUtils.wrapInIslandPanelIfNeeded(mainComponent), BorderLayout.CENTER);
            }
        }

        // Revalidate and repaint
        revalidate();
        repaint(50);
    }

    private void updateToolbars() {

        boolean leftPanelIsUsed = false;
        boolean rightPanelIsUsed = false;

        for (Panel value : panels.values()) {
            if (value.location == PanelLocation.TopLeft || value.location == PanelLocation.BottomLeft || value.location == PanelLocation.BottomBottom) {
                leftPanelIsUsed = true;
            } else if (value.location == PanelLocation.TopRight || value.location == PanelLocation.BottomRight) {
                rightPanelIsUsed = true;
            }
        }

        if (hideToolbars) {
            leftPanelIsUsed = false;
            rightPanelIsUsed = false;
        }

        leftToolBar.setVisible(leftPanelIsUsed);
        rightToolBar.setVisible(rightPanelIsUsed);

        // Update toolbar sizes
        if (showToolbarLabels) {
            leftToolBar.setMaximumSize(new Dimension(toolbarWithLabelsWidth, Short.MAX_VALUE));
            rightToolBar.setMaximumSize(new Dimension(toolbarWithLabelsWidth, Short.MAX_VALUE));
            leftToolBar.setPreferredSize(new Dimension(toolbarWithLabelsWidth, Short.MAX_VALUE));
            rightToolBar.setPreferredSize(new Dimension(toolbarWithLabelsWidth, Short.MAX_VALUE));
            leftToolBar.setMinimumSize(new Dimension(toolbarWithLabelsWidth, 32));
            rightToolBar.setMinimumSize(new Dimension(toolbarWithLabelsWidth, 32));
        } else {
            leftToolBar.setMaximumSize(new Dimension(toolbarWithoutLabelsWidth, Short.MAX_VALUE));
            rightToolBar.setMaximumSize(new Dimension(toolbarWithoutLabelsWidth, Short.MAX_VALUE));
            leftToolBar.setPreferredSize(new Dimension(toolbarWithoutLabelsWidth, Short.MAX_VALUE));
            rightToolBar.setPreferredSize(new Dimension(toolbarWithoutLabelsWidth, Short.MAX_VALUE));
            leftToolBar.setMinimumSize(new Dimension(toolbarWithoutLabelsWidth, 32));
            rightToolBar.setMinimumSize(new Dimension(toolbarWithoutLabelsWidth, 32));
        }

        leftToolBar.removeAll();
        rightToolBar.removeAll();

        // Left toolbar
        for (Panel panel : getPanelsAtLocation(PanelLocation.TopLeft).stream().sorted().toList()) {
            leftToolBar.add(createToggleButton(panel));
        }
        List<Panel> bottomLeftPanels = getPanelsAtLocation(PanelLocation.BottomLeft).stream().sorted().toList();
        if (!bottomLeftPanels.isEmpty()) {
            leftToolBar.add(Box.createVerticalStrut(32));
            for (Panel panel : bottomLeftPanels) {
                leftToolBar.add(createToggleButton(panel));
            }
        }
        leftToolBar.add(Box.createVerticalGlue());
        for (Panel panel : getPanelsAtLocation(PanelLocation.BottomBottom).stream().sorted().toList()) {
            leftToolBar.add(createToggleButton(panel));
        }


        // Right toolbar
        for (Panel panel : getPanelsAtLocation(PanelLocation.TopRight).stream().sorted().toList()) {
            rightToolBar.add(createToggleButton(panel));
        }
        rightToolBar.add(Box.createVerticalGlue());
        for (Panel panel : getPanelsAtLocation(PanelLocation.BottomRight).stream().sorted().toList()) {
            rightToolBar.add(createToggleButton(panel));
        }
    }

    private JToggleButton createToggleButton(Panel panel) {
        JToggleButton button = new JToggleButton(panel.getIcon());
        button.setOpaque(false);
        button.setSelected(panel.isDisplayed());
        button.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        button.setForeground(ThemeUtils.getCurrentStyle().getIconBaseColor());
        button.setToolTipText(panel.getName());
        button.addActionListener(e -> {
            if (button.isSelected()) {
                activatePanel(panel, true);
            } else {
                deactivatePanel(panel, true);
            }
        });
        if (showToolbarLabels) {
            button.setText(panel.getName());
            button.setVerticalTextPosition(SwingConstants.BOTTOM);
            button.setHorizontalTextPosition(SwingConstants.CENTER);
            button.setFont(new Font(Font.DIALOG, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeTiny()));
            button.setMaximumSize(new Dimension(Short.MAX_VALUE, BUTTON_MAX_WIDTH));
        }

        JPopupMenu popupMenu = UIUtils.addRightClickPopupMenuToButton(button);
        popupMenu.add(UIUtils.createMenuItem("Top left", "Move the panel to the top left anchor", JIPipe.RESOURCES.getIcon16("actions/dock-top-left.png"), () -> {
            movePanelToLocation(panel, PanelLocation.TopLeft, true);
        }));
        popupMenu.add(UIUtils.createMenuItem("Bottom left", "Move the panel to the bottom left anchor", JIPipe.RESOURCES.getIcon16("actions/dock-bottom-left.png"), () -> {
            movePanelToLocation(panel, PanelLocation.BottomLeft, true);
        }));
        popupMenu.add(UIUtils.createMenuItem("Top right", "Move the panel to the top right anchor", JIPipe.RESOURCES.getIcon16("actions/dock-top-right.png"), () -> {
            movePanelToLocation(panel, PanelLocation.TopRight, true);
        }));
        popupMenu.add(UIUtils.createMenuItem("Bottom right", "Move the panel to the bottom right anchor", JIPipe.RESOURCES.getIcon16("actions/dock-bottom-right.png"), () -> {
            movePanelToLocation(panel, PanelLocation.BottomRight, true);
        }));
        popupMenu.add(UIUtils.createMenuItem("Bottom", "Move the panel to the bottom anchor", JIPipe.RESOURCES.getIcon16("actions/dock-bottom-bottom.png"), () -> {
            movePanelToLocation(panel, PanelLocation.BottomBottom, true);
        }));

        panelVisibilityToggles.put(panel.getId(), button);
        return button;
    }

    public void deactivatePanel(Panel panel, boolean saveState) {
        panel.setVisible(false);
        updateAll();
        if (saveState) {
            saveState();
        }
    }

    private void saveState() {
        savedState = getCurrentState();
        stateSavedEventEmitter.emit(new StateSavedEvent(this, savedState));
    }

    private void activatePanel(Panel panel, boolean saveState) {
        if (panel.getComponent() == null && panel.getComponentSupplier() != null) {
            panel.setComponent(panel.getComponentSupplier().get());
            panel.setComponentSupplier(null);
        }

        // Deactivate all other buttons
        setPanelVisible(panel);

        updateAll();
        if (saveState) {
            saveState();
        }
    }

    private void setPanelVisible(Panel panel) {
        for (Panel otherPanel : getPanelsAtLocation(panel.getLocation())) {
            if (otherPanel != panel) {
                otherPanel.setVisible(false);
                panelVisibilityToggles.get(otherPanel.getId()).setSelected(false);
            }
        }
        panel.setVisible(true);
    }

    public boolean isPanelLocationVisible(PanelLocation panelLocation) {
        return panels.values().stream().anyMatch(p -> p.getLocation() == panelLocation && p.isVisible());
    }

    public List<Panel> getPanelsAtLocation(PanelLocation location) {
        return panels.values().stream().filter(panel -> panel.getLocation() == location).collect(Collectors.toList());
    }

    public void movePanelToLocation(String id, PanelLocation newLocation, boolean saveState) {
        movePanelToLocation(panels.get(id), newLocation, saveState);
    }

    private void movePanelToLocation(Panel panel, PanelLocation newLocation, boolean saveState) {
        PanelLocation oldLocation = panel.getLocation();
        if (oldLocation != null && !oldLocation.equals(newLocation)) {

            if (panel.isDisplayed()) {
                deactivatePanels(newLocation, false);
            }

            panel.setLocation(newLocation);

            updateAll();

            if (saveState) {
                saveState();
            }
        }
    }

    /**
     * Deactivates all panels at a given location
     *
     * @param location  the location
     * @param saveState save the state
     */
    public void deactivatePanels(PanelLocation location, boolean saveState) {
        for (Panel panel : getPanelsAtLocation(location)) {
            if (panel.isDisplayed()) {
                deactivatePanel(panel, false);
            }
        }
        if (saveState) {
            saveState();
        }
    }

    public String getCurrentlyVisiblePanelId(PanelLocation location, boolean withHidden) {
        for (Panel panel : getPanelsAtLocation(location)) {
            if (withHidden) {
                if (panel.isVisible()) {
                    return panel.getId();
                }
            } else {
                if (panel.isDisplayed()) {
                    return panel.getId();
                }
            }
        }
        return null;
    }

    public void removeDockPanel(String id) {
        if (panels.remove(id) != null) {
            updateAll();
        }
    }

    public void addDockPanel(String id, String name, Icon icon, PanelLocation location, boolean visible, int uiOrder, JComponent component) {
        visible = tryRestoreVisibilityState(savedState, id, visible);
        location = tryRestoreLocationState(savedState, id, location);
        removeDockPanel(id);

        Panel panel = new Panel(id);
        panel.setLocation(location);
        panel.setComponent(component);
        panel.setIcon(icon);
        panel.setName(name);
        panel.setUiOrder(uiOrder);
        panels.put(id, panel);

        if (visible) {
            activatePanel(panel, false);
        } else {
            updateToolbars();
        }
    }

    private PanelLocation tryRestoreLocationState(State state, String id, PanelLocation location) {
        return state.getLocations().getOrDefault(id, location);
    }

    private boolean tryRestoreVisibilityState(State state, String id, boolean defaultValue) {
        return state.getVisibilities().getOrDefault(id, defaultValue);
    }

    public void addDockPanel(String id, String name, Icon icon, PanelLocation location, boolean visible, int uiOrder, Supplier<JComponent> component) {
        visible = tryRestoreVisibilityState(savedState, id, visible);
        location = tryRestoreLocationState(savedState, id, location);
        removeDockPanel(id);


        Panel panel = new Panel(id);
        panel.setLocation(location);
        panel.setComponentSupplier(component);
        panel.setIcon(icon);
        panel.setName(name);
        panel.setUiOrder(uiOrder);
        panels.put(id, panel);

        if (visible) {
            activatePanel(panel, false);
        } else {
            updateToolbars();
        }
    }

    public State getSavedState() {
        return savedState;
    }

    public void setSavedState(State savedState) {
        this.savedState = savedState;
    }

    public void restoreState(State state) {
        for (Panel panel : panels.values()) {
            boolean visible = state.getVisibilities().getOrDefault(panel.getId(), panel.isVisible());
            PanelLocation location = state.getLocations().getOrDefault(panel.getId(), panel.getLocation());
            panel.setLocation(location);
            panel.setVisible(visible);
        }
        for (Panel panel : panels.values()) {
            if (panel.visible) {
                setPanelVisible(panel);
            }
        }
        savedState = state;
        leftPanelWidth = Math.max(minimumPanelWidth, state.leftPanelWidth);
        rightPanelWidth = Math.max(minimumPanelWidth, state.rightPanelWidth);
        alwaysShowLeftPanel = state.alwaysShowLeftPanel;
        alwaysShowRightPanel = state.alwaysShowRightPanel;
        if (state.leftSplitPaneRatio > 0) {
            ((JIPipeDesktopSplitPane.FixedRatio) leftSplitPane.getRatio()).setRatio(Math.max(0.01, Math.min(0.99, state.leftSplitPaneRatio)));
        }
        if (state.rightSplitPaneRatio > 0) {
            ((JIPipeDesktopSplitPane.FixedRatio) rightSplitPane.getRatio()).setRatio(Math.max(0.01, Math.min(0.99, state.rightSplitPaneRatio)));
        }
        if (state.getMainSplitPaneRatio() > 0) {
            ((JIPipeDesktopSplitPane.FixedRatio) mainSplitPane.getRatio()).setRatio(Math.max(0.01, Math.min(0.99, state.getMainSplitPaneRatio())));
        }
        updateAll();
    }

    private void updateAll() {
        List<PanelSideVisibilityChangedEvent> panelVisibilityChangedEvents = new ArrayList<>();
        updateToolbars();
        updateContent(panelVisibilityChangedEvents);
        updateSizes();

        // Fire all events after updates
        for (PanelSideVisibilityChangedEvent panelVisibilityChangedEvent : panelVisibilityChangedEvents) {
            panelSideVisibilityChangedEventEmitter.emit(panelVisibilityChangedEvent);
        }
    }

    public int getMinimumPanelHeight() {
        return minimumPanelHeight;
    }

    public void setMinimumPanelHeight(int minimumPanelHeight) {
        this.minimumPanelHeight = minimumPanelHeight;
    }

    public boolean isLeftPanelVisible() {
        return layeredPaneLeft.isVisible();
    }

    public boolean isRightPanelVisible() {
        return layeredPaneRight.isVisible();
    }

    public void activatePanel(String id, boolean saveState) {
        Panel panel = panels.get(id);
        if (panel != null) {
            activatePanel(panel, saveState);
        }
    }

    public void deactivatePanel(String id, boolean saveState) {
        Panel panel = panels.get(id);
        if (panel != null) {
            deactivatePanel(panel, saveState);
        }
    }

    public <T extends JComponent> T getPanelComponent(String id, Class<T> klass) {
        Panel panel = panels.getOrDefault(id, null);
        if (panel != null) {
            return (T) panel.getComponent();
        } else {
            return null;
        }
    }

    public StateSavedEventEmitter getStateSavedEventEmitter() {
        return stateSavedEventEmitter;
    }

    @Override
    public void onSplitPaneRatioUpdated(JIPipeDesktopSplitPane.RatioUpdatedEvent event) {
        saveState();
    }

    public PanelSideVisibilityChangedEventEmitter getPanelSideVisibilityChangedEventEmitter() {
        return panelSideVisibilityChangedEventEmitter;
    }

    public int getLeftPanelWidth() {
        return leftPanelWidth;
    }

    public void setLeftPanelWidth(int leftPanelWidth) {
        this.leftPanelWidth = leftPanelWidth;
        updateSizes();
    }

    public int getRightPanelWidth() {
        return rightPanelWidth;
    }

    public void setRightPanelWidth(int rightPanelWidth) {
        this.rightPanelWidth = rightPanelWidth;
        updateSizes();
    }

    public int getMinimumPanelWidth() {
        return minimumPanelWidth;
    }

    public void setMinimumPanelWidth(int minimumPanelWidth) {
        this.minimumPanelWidth = minimumPanelWidth;
        updateSizes();
    }

    public int getMinimumBackgroundWidth() {
        return minimumBackgroundWidth;
    }

    public void setMinimumBackgroundWidth(int minimumBackgroundWidth) {
        this.minimumBackgroundWidth = minimumBackgroundWidth;
    }

    public void removeDockPanelsIf(Predicate<Panel> predicate) {
        boolean found = false;
        for (Panel panel : ImmutableList.copyOf(panels.values())) {
            if (predicate.test(panel)) {
                panelVisibilityToggles.remove(panel.getId());
                panels.remove(panel.getId());
                found = true;
            }
        }
        if (found) {
            updateAll();
        }
    }

    public State getCurrentState() {
        State state = new State();
        for (Panel panel : panels.values()) {
            state.visibilities.put(panel.getId(), panel.isVisible());
            state.locations.put(panel.getId(), panel.getLocation());
        }
        state.setLeftPanelWidth(leftPanelWidth);
        state.setRightPanelWidth(rightPanelWidth);
        state.setMainSplitPaneRatio(((JIPipeDesktopSplitPane.FixedRatio) mainSplitPane.getRatio()).getRatio());
        state.setLeftSplitPaneRatio(((JIPipeDesktopSplitPane.FixedRatio) leftSplitPane.getRatio()).getRatio());
        state.setRightSplitPaneRatio(((JIPipeDesktopSplitPane.FixedRatio) rightSplitPane.getRatio()).getRatio());
        state.setAlwaysShowLeftPanel(alwaysShowLeftPanel);
        state.setAlwaysShowRightPanel(alwaysShowRightPanel);
        return state;
    }

    public void removeAllPanels() {
        boolean found = false;
        for (Panel panel : ImmutableList.copyOf(panels.values())) {
            panelVisibilityToggles.remove(panel.getId());
            panels.remove(panel.getId());
            found = true;
        }
        if (found) {
            updateAll();
        }
    }

    public boolean containsPanel(String id) {
        return panels.containsKey(id);
    }

    public Map<String, Panel> getPanels() {
        return new HashMap<>(panels);
    }

    public boolean isHideToolbars() {
        return hideToolbars;
    }

    public void setHideToolbars(boolean hideToolbars) {
        this.hideToolbars = hideToolbars;
        updateAll();
    }

    public boolean isAlwaysShowLeftPanel() {
        return alwaysShowLeftPanel;
    }

    public void setAlwaysShowLeftPanel(boolean alwaysShowLeftPanel) {
        this.alwaysShowLeftPanel = alwaysShowLeftPanel;
        updateAll();
    }

    public boolean isAlwaysShowRightPanel() {
        return alwaysShowRightPanel;
    }

    public void setAlwaysShowRightPanel(boolean alwaysShowRightPanel) {
        this.alwaysShowRightPanel = alwaysShowRightPanel;
        updateAll();
    }

    public enum PanelLocation {
        TopLeft,
        BottomLeft,
        TopRight,
        BottomRight,
        BottomBottom
    }

    public enum PanelSide {
        Left,
        Right
    }

    public interface StateSavedEventListener {
        void onDockPanelStateSaved(StateSavedEvent event);
    }

    public interface PanelSideVisibilityChangedEventListener {
        void onPanelSideVisibilityChanged(PanelSideVisibilityChangedEvent event);
    }

    public static class State {
        private double mainSplitPaneRatio;
        private Map<String, Boolean> visibilities = new HashMap<>();
        private Map<String, PanelLocation> locations = new HashMap<>();
        private int leftPanelWidth;
        private int rightPanelWidth;
        private double leftSplitPaneRatio;
        private double rightSplitPaneRatio;
        private boolean alwaysShowLeftPanel;
        private boolean alwaysShowRightPanel;

        @JsonGetter("always-show-left-panel")
        public boolean isAlwaysShowLeftPanel() {
            return alwaysShowLeftPanel;
        }

        @JsonSetter("always-show-left-panel")
        public void setAlwaysShowLeftPanel(boolean alwaysShowLeftPanel) {
            this.alwaysShowLeftPanel = alwaysShowLeftPanel;
        }

        @JsonGetter("always-show-right-panel-v2")
        public boolean isAlwaysShowRightPanel() {
            return alwaysShowRightPanel;
        }

        @JsonSetter("always-show-right-panel-v2")
        public void setAlwaysShowRightPanel(boolean alwaysShowRightPanel) {
            this.alwaysShowRightPanel = alwaysShowRightPanel;
        }

        @JsonGetter("left-split-pane-ratio")
        public double getLeftSplitPaneRatio() {
            return leftSplitPaneRatio;
        }

        @JsonSetter("left-split-pane-ratio")
        public void setLeftSplitPaneRatio(double leftSplitPaneRatio) {
            this.leftSplitPaneRatio = leftSplitPaneRatio;
        }

        @JsonGetter("right-split-pane-ratio")
        public double getRightSplitPaneRatio() {
            return rightSplitPaneRatio;
        }

        @JsonSetter("right-split-pane-ratio")
        public void setRightSplitPaneRatio(double rightSplitPaneRatio) {
            this.rightSplitPaneRatio = rightSplitPaneRatio;
        }

        @JsonGetter("left-panel-width")
        public int getLeftPanelWidth() {
            return leftPanelWidth;
        }

        @JsonSetter("left-panel-width")
        public void setLeftPanelWidth(int leftPanelWidth) {
            this.leftPanelWidth = leftPanelWidth;
        }

        @JsonGetter("right-panel-width")
        public int getRightPanelWidth() {
            return rightPanelWidth;
        }

        @JsonSetter("right-panel-width")
        public void setRightPanelWidth(int rightPanelWidth) {
            this.rightPanelWidth = rightPanelWidth;
        }

        @JsonGetter("visibilities")
        public Map<String, Boolean> getVisibilities() {
            return visibilities;
        }

        @JsonSetter("visibilities")
        public void setVisibilities(Map<String, Boolean> visibilities) {
            this.visibilities = visibilities;
        }

        @JsonGetter("locations")
        public Map<String, PanelLocation> getLocations() {
            return locations;
        }

        @JsonSetter("locations")
        public void setLocations(Map<String, PanelLocation> locations) {
            this.locations = locations;
        }

        public void put(String id, boolean visible, PanelLocation panelLocation) {
            locations.put(id, panelLocation);
            visibilities.put(id, visible);
        }

        @JsonGetter("main-split-pane-ratio")
        public double getMainSplitPaneRatio() {
            return mainSplitPaneRatio;
        }

        @JsonSetter("main-split-pane-ratio")
        public void setMainSplitPaneRatio(double mainSplitPaneRatio) {
            this.mainSplitPaneRatio = mainSplitPaneRatio;
        }
    }

    public static class Panel implements Comparable<Panel> {
        private final String id;
        private Icon icon;
        private JComponent component;
        private Supplier<JComponent> componentSupplier;
        private String name;
        private PanelLocation location;
        private boolean visible;
        private int uiOrder;

        public Panel(String id) {
            this.id = id;
        }

        public int getUiOrder() {
            return uiOrder;
        }

        public void setUiOrder(int uiOrder) {
            this.uiOrder = uiOrder;
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

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isVisible() {
            return visible;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public boolean isDisplayed() {
            return visible;
        }

        public Supplier<JComponent> getComponentSupplier() {
            return componentSupplier;
        }

        public void setComponentSupplier(Supplier<JComponent> componentSupplier) {
            this.componentSupplier = componentSupplier;
        }

        @Override
        public int compareTo(@NotNull JIPipeDesktopDockPanel.Panel panel) {
            int byUIOrder = Integer.compare(uiOrder, panel.getUiOrder());
            if (byUIOrder == 0) {
                return name.compareTo(panel.getName());
            }
            return byUIOrder;
        }

        public <T extends JComponent> T getComponent(Class<T> klass) {
            return (T) getComponent();
        }
    }

    public static class StateSavedEvent extends AbstractJIPipeEvent {

        private final State savedState;

        public StateSavedEvent(Object source, State savedState) {
            super(source);
            this.savedState = savedState;
        }

        public State getSavedState() {
            return savedState;
        }
    }

    public static class StateSavedEventEmitter extends JIPipeEventEmitter<StateSavedEvent, StateSavedEventListener> {

        @Override
        protected void call(StateSavedEventListener stateSavedEventListener, StateSavedEvent event) {
            stateSavedEventListener.onDockPanelStateSaved(event);
        }
    }

    public static class PanelSideVisibilityChangedEvent extends AbstractJIPipeEvent {

        private final PanelSide panelSide;
        private final boolean visible;

        public PanelSideVisibilityChangedEvent(Object source, PanelSide panelSide, boolean visible) {
            super(source);
            this.panelSide = panelSide;
            this.visible = visible;
        }

        public PanelSide getPanelSide() {
            return panelSide;
        }

        public boolean isVisible() {
            return visible;
        }
    }

    public static class PanelSideVisibilityChangedEventEmitter extends JIPipeEventEmitter<PanelSideVisibilityChangedEvent, PanelSideVisibilityChangedEventListener> {

        @Override
        protected void call(PanelSideVisibilityChangedEventListener panelSideVisibilityChangedEventListener, PanelSideVisibilityChangedEvent event) {
            panelSideVisibilityChangedEventListener.onPanelSideVisibilityChanged(event);
        }
    }
}
