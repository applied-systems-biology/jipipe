package org.hkijena.jipipe.utils.ui;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopVerticalToolBar;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
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
    private static final int RESIZE_HANDLE_SIZE = 4;
    private final JIPipeDesktopVerticalToolBar leftToolBar = new JIPipeDesktopVerticalToolBar();
    private final JIPipeDesktopVerticalToolBar rightToolBar = new JIPipeDesktopVerticalToolBar();
    private final JLayeredPane layeredPane = new JLayeredPane();
    private final JPanel layeredPaneBackground = new JPanel(new BorderLayout());
    private final JPanel leftPanel = new JPanel(new BorderLayout());
    private final JPanel rightPanel = new JPanel(new BorderLayout());
    private final JPanel leftResizerPanel = new JPanel();
    private final JPanel rightResizerPanel = new JPanel();
    private final Map<String, Panel> panels = new LinkedHashMap<>();
    private final Map<String, JToggleButton> panelVisibilityToggles = new HashMap<>();
    private final JIPipeDesktopSplitPane leftSplitPane = new JIPipeDesktopSplitPane(JSplitPane.VERTICAL_SPLIT, new JIPipeDesktopSplitPane.FixedRatio(0.33, true));
    private final JIPipeDesktopSplitPane rightSplitPane = new JIPipeDesktopSplitPane(JSplitPane.VERTICAL_SPLIT, new JIPipeDesktopSplitPane.FixedRatio(0.66, true));
    private final StateSavedEventEmitter stateSavedEventEmitter = new StateSavedEventEmitter();
    private final PanelSideVisibilityChangedEventEmitter panelSideVisibilityChangedEventEmitter = new PanelSideVisibilityChangedEventEmitter();
    private final JIPipeParameterCollection.ParameterChangedEventEmitter parameterChangedEventEmitter = new JIPipeParameterCollection.ParameterChangedEventEmitter();
    private final JCheckBoxMenuItem showToolbarLabelsMenuItem = new JCheckBoxMenuItem("Show Toolbar Labels");
    private int floatingPanelMarginLeftRight = 8;
    private int floatingPanelMarginTop = 8;
    private int floatingPanelMarginBottom = 8;
    private int leftPanelWidth = 350;
    private int rightPanelWidth = 500;
    private int minimumPanelWidth = 150;
    private int minimumBackgroundWidth = 150;
    private JComponent leftPanelContent;
    private JComponent rightPanelContent;
    private boolean leftPanelIsFloating = false;
    private boolean rightPanelIsFloating = false;
    private State savedState = new State();
    private boolean showToolbarLabels = true;
    private int toolbarWithLabelsWidth = 92;
    private int toolbarWithoutLabelsWidth = 42;
    private boolean hideToolbars = false;
    private boolean alwaysShowLeftPanel = false;
    private boolean alwaysShowRightPanel = false;

    public JIPipeDesktopDockPanel() {
        super(new BorderLayout());
        initialize();
        updateAll();
    }

    private void initialize() {
        add(leftToolBar, BorderLayout.WEST);
        add(rightToolBar, BorderLayout.EAST);

        rightToolBar.setBorder(UIUtils.createPanelBorder(0, 0, 1, 0));
        leftToolBar.setBorder(UIUtils.createPanelBorder(1, 0, 0, 0));

        JPopupMenu toolbarContextMenu = new JPopupMenu();
        UIUtils.addReloadableRightClickPopupMenuToComponent(leftToolBar, toolbarContextMenu, (menu) -> reloadContextMenu(menu, false));
        UIUtils.addReloadableRightClickPopupMenuToComponent(rightToolBar, toolbarContextMenu, (menu) -> reloadContextMenu(menu, true));

        showToolbarLabelsMenuItem.setState(showToolbarLabels);
        showToolbarLabelsMenuItem.addActionListener(e -> {
            setShowToolbarLabels(showToolbarLabelsMenuItem.getState());
        });

//        layeredPane.setLayout(new OverlayLayout(layeredPane));
        add(layeredPane, BorderLayout.CENTER);

        layeredPane.add(layeredPaneBackground, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(leftPanel, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(rightPanel, JLayeredPane.PALETTE_LAYER);

        initializeLeftFloatingPanel();
        initializeRightFloatingPanel();

        leftSplitPane.setDividerSize(12);
        rightSplitPane.setDividerSize(12);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateSizes();
            }
        });
    }

    private void reloadContextMenu(JPopupMenu menu, boolean right) {
        menu.removeAll();
        menu.add(showToolbarLabelsMenuItem);
        if(right) {
            JCheckBoxMenuItem pin = new JCheckBoxMenuItem("Always show right panel");
            pin.setState(alwaysShowRightPanel);
            pin.addActionListener(e -> {
                setAlwaysShowRightPanel(pin.getState());
                saveState();
            });
            menu.add(pin);
        }
        else {
            JCheckBoxMenuItem pin = new JCheckBoxMenuItem("Always show left panel");
            pin.setState(alwaysShowLeftPanel);
            pin.addActionListener(e -> {
                setAlwaysShowLeftPanel(pin.getState());
                saveState();
            });
            menu.add(pin);
        }
    }

    private void initializeRightFloatingPanel() {
        rightPanel.setBorder(UIUtils.createPanelBorder(1, 0, 0, 0));
        rightPanel.add(rightResizerPanel, BorderLayout.WEST);
        rightResizerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        rightResizerPanel.setPreferredSize(new Dimension(RESIZE_HANDLE_SIZE, 64));
        rightResizerPanel.setMinimumSize(new Dimension(RESIZE_HANDLE_SIZE, 32));
//        rightResizerPanel.setBackground(Color.RED);
        rightResizerPanel.setOpaque(false);

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

    private void initializeLeftFloatingPanel() {
//        leftFloatingPanel.setOpaque(false);
        leftPanel.setBorder(UIUtils.createPanelBorder(0, 0, 1, 0));
        leftPanel.add(leftResizerPanel, BorderLayout.EAST);
        leftResizerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        leftResizerPanel.setPreferredSize(new Dimension(RESIZE_HANDLE_SIZE, 64));
        leftResizerPanel.setMinimumSize(new Dimension(RESIZE_HANDLE_SIZE, 32));
//        leftResizerPanel.setBackground(Color.RED);
        leftResizerPanel.setOpaque(false);

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

    public void setBackgroundComponent(JComponent component) {
        layeredPaneBackground.removeAll();
        layeredPaneBackground.add(component, BorderLayout.CENTER);
        revalidate();
        repaint();
        updateSizes();
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

        int leftMarginLeft = leftPanelIsFloating ? floatingPanelMarginLeftRight : 0;
        int leftMarginTop = leftPanelIsFloating ? floatingPanelMarginTop : 0;
        int leftMarginBottom = leftPanelIsFloating ? floatingPanelMarginBottom : 0;
        int rightMarginRight = rightPanelIsFloating ? floatingPanelMarginLeftRight : 0;
        int rightMarginTop = rightPanelIsFloating ? floatingPanelMarginTop : 0;
        int rightMarginBottom = rightPanelIsFloating ? floatingPanelMarginBottom : 0;


        Dimension leftSize = new Dimension(leftPanelWidth, getHeight() - leftMarginTop - leftMarginBottom);
        Dimension rightSize = new Dimension(rightPanelWidth, getHeight() - rightMarginTop - rightMarginBottom);

        leftPanel.setPreferredSize(leftSize);
        rightPanel.setPreferredSize(rightSize);
        leftPanel.setMaximumSize(leftSize);
        rightPanel.setMaximumSize(rightSize);

        leftPanel.setBounds(leftMarginLeft, leftMarginTop, leftSize.width, leftSize.height);
        rightPanel.setBounds(availableWidth - rightMarginRight - rightPanelWidth - 2, rightMarginTop, rightSize.width, rightSize.height);

        int backgroundLeft;
        int backgroundWidth;

        if (leftPanel.isVisible() && rightPanel.isVisible()) {
            backgroundLeft = leftPanelIsFloating ? 0 : leftSize.width;
            backgroundWidth = layeredPane.getWidth() - (rightPanelIsFloating ? 0 : rightSize.width) - backgroundLeft;
        } else if (leftPanel.isVisible()) {
            backgroundLeft = leftPanelIsFloating ? 0 : leftSize.width;
            backgroundWidth = layeredPane.getWidth() - backgroundLeft;
        } else if (rightPanel.isVisible()) {
            backgroundLeft = 0;
            backgroundWidth = layeredPane.getWidth() - rightSize.width;
        } else {
            backgroundLeft = 0;
            backgroundWidth = layeredPane.getWidth();
        }

        layeredPaneBackground.setBounds(backgroundLeft, 0, Math.max(backgroundWidth, minimumBackgroundWidth), getHeight());
//        layeredPaneBackground.setBounds(100,100,100,100);
//        layeredPaneBackground.setBounds(0,0,layeredPane.getWidth(),getHeight());

        revalidate();
        repaint();
    }

    private void updateContent(List<PanelSideVisibilityChangedEvent> panelVisibilityChangedEvents) {
        if (leftPanelContent != null) {
            leftPanel.remove(leftPanelContent);
        }
        if (rightPanelContent != null) {
            rightPanel.remove(rightPanelContent);
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
            leftSplitPane.setLeftComponent(leftContent.get(0));
            leftSplitPane.setRightComponent(leftContent.get(1));
            leftPanelContent = leftSplitPane;
        } else if (leftContent.size() == 1) {
            // use directly
            leftPanelContent = leftContent.get(0);
            leftSplitPane.setLeftComponent(new JPanel());
            leftSplitPane.setRightComponent(new JPanel());
        } else {
            leftSplitPane.setLeftComponent(new JPanel());
            leftSplitPane.setRightComponent(new JPanel());
        }

        if (rightContent.size() >= 2) {
            // create split pane
            rightSplitPane.setLeftComponent(rightContent.get(0));
            rightSplitPane.setRightComponent(rightContent.get(1));
            rightPanelContent = rightSplitPane;
        } else if (rightContent.size() == 1) {
            // use directly
            rightPanelContent = rightContent.get(0);
            rightSplitPane.setLeftComponent(new JPanel());
            rightSplitPane.setRightComponent(new JPanel());
        } else {
            rightSplitPane.setLeftComponent(new JPanel());
            rightSplitPane.setRightComponent(new JPanel());
        }

        leftSplitPane.applyRatio();
        rightSplitPane.applyRatio();

        // Rebuild panel
        boolean oldLeftPanelVisible = leftPanel.isVisible();
        boolean oldRightPanelVisible = rightPanel.isVisible();
        if (leftPanelContent != null) {
            leftPanel.setVisible(true);
            leftPanel.add(leftPanelContent, BorderLayout.CENTER);
        } else {
            leftPanel.setVisible(alwaysShowLeftPanel);
        }
        if (rightPanelContent != null) {
            rightPanel.setVisible(true);
            rightPanel.add(rightPanelContent, BorderLayout.CENTER);
        } else {
            rightPanel.setVisible(alwaysShowRightPanel);
        }

        if (oldLeftPanelVisible != leftPanel.isVisible()) {
            panelVisibilityChangedEvents.add(new PanelSideVisibilityChangedEvent(this, PanelSide.Left, leftPanel.isVisible()));
        }
        if (oldRightPanelVisible != rightPanel.isVisible()) {
            panelVisibilityChangedEvents.add(new PanelSideVisibilityChangedEvent(this, PanelSide.Right, rightPanel.isVisible()));
        }

        // Revalidate and repaint
        revalidate();
        repaint(50);
    }

    private void updateToolbars() {

        boolean leftPanelIsUsed = false;
        boolean rightPanelIsUsed = false;

        for (Panel value : panels.values()) {
            if (value.location == PanelLocation.TopLeft || value.location == PanelLocation.BottomLeft) {
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
        for (Panel panel : getPanelsAtLocation(PanelLocation.TopLeft).stream().sorted().collect(Collectors.toList())) {
            leftToolBar.add(createToggleButton(panel));
        }
        leftToolBar.add(Box.createVerticalGlue());
        for (Panel panel : getPanelsAtLocation(PanelLocation.BottomLeft).stream().sorted().collect(Collectors.toList())) {
            leftToolBar.add(createToggleButton(panel));
        }

        for (Panel panel : getPanelsAtLocation(PanelLocation.TopRight).stream().sorted().collect(Collectors.toList())) {
            rightToolBar.add(createToggleButton(panel));
        }
        rightToolBar.add(Box.createVerticalGlue());
        for (Panel panel : getPanelsAtLocation(PanelLocation.BottomRight).stream().sorted().collect(Collectors.toList())) {
            rightToolBar.add(createToggleButton(panel));
        }
    }

    private JToggleButton createToggleButton(Panel panel) {
        JToggleButton button = new JToggleButton(panel.getIcon());
        button.setSelected(panel.isDisplayed());
        button.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
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
            button.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
            button.setMaximumSize(new Dimension(Short.MAX_VALUE, 64));
        }
        JPopupMenu popupMenu = UIUtils.addRightClickPopupMenuToButton(button);
        popupMenu.add(UIUtils.createMenuItem("Top left", "Move the panel to the top left anchor", UIUtils.getIconFromResources("actions/dock-top-left.png"), () -> {
            movePanelToLocation(panel, PanelLocation.TopLeft, true);
        }));
        popupMenu.add(UIUtils.createMenuItem("Bottom left", "Move the panel to the bottom left anchor", UIUtils.getIconFromResources("actions/dock-bottom-left.png"), () -> {
            movePanelToLocation(panel, PanelLocation.BottomLeft, true);
        }));
        popupMenu.add(UIUtils.createMenuItem("Top right", "Move the panel to the top right anchor", UIUtils.getIconFromResources("actions/dock-top-right.png"), () -> {
            movePanelToLocation(panel, PanelLocation.TopRight, true);
        }));
        popupMenu.add(UIUtils.createMenuItem("Bottom right", "Move the panel to the bottom right anchor", UIUtils.getIconFromResources("actions/dock-bottom-right.png"), () -> {
            movePanelToLocation(panel, PanelLocation.BottomRight, true);
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

    public boolean isLeftPanelIsFloating() {
        return leftPanelIsFloating;
    }

    public void setLeftPanelIsFloating(boolean leftPanelIsFloating) {
        this.leftPanelIsFloating = leftPanelIsFloating;
        updateSizes();
    }

    public boolean isRightPanelIsFloating() {
        return rightPanelIsFloating;
    }

    public void setRightPanelIsFloating(boolean rightPanelIsFloating) {
        this.rightPanelIsFloating = rightPanelIsFloating;
        updateSizes();
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

    public boolean isLeftPanelVisible() {
        return leftPanel.isVisible();
    }

    public boolean isRightPanelVisible() {
        return rightPanel.isVisible();
    }

    public void activatePanel(String id, boolean saveState) {
        Panel panel = panels.get(id);
        if (panel != null) {
            activatePanel(panel, saveState);
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

    public int getFloatingPanelMarginTop() {
        return floatingPanelMarginTop;
    }

    public void setFloatingPanelMarginTop(int floatingPanelMarginTop) {
        this.floatingPanelMarginTop = floatingPanelMarginTop;
    }

    public int getMinimumBackgroundWidth() {
        return minimumBackgroundWidth;
    }

    public void setMinimumBackgroundWidth(int minimumBackgroundWidth) {
        this.minimumBackgroundWidth = minimumBackgroundWidth;
    }

    public int getFloatingPanelMarginBottom() {
        return floatingPanelMarginBottom;
    }

    public void setFloatingPanelMarginBottom(int floatingPanelMarginBottom) {
        this.floatingPanelMarginBottom = floatingPanelMarginBottom;
    }

    public int getFloatingPanelMarginLeftRight() {
        return floatingPanelMarginLeftRight;
    }

    public void setFloatingPanelMarginLeftRight(int floatingPanelMarginLeftRight) {
        this.floatingPanelMarginLeftRight = floatingPanelMarginLeftRight;
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
        BottomRight
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

        @JsonGetter("always-show-right-panel")
        public boolean isAlwaysShowRightPanel() {
            return alwaysShowRightPanel;
        }

        @JsonSetter("always-show-right-panel")
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
