package org.hkijena.jipipe.utils.ui;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopVerticalToolBar;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

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

public class JIPipeDesktopDockPanel extends JPanel {

    private static final int RESIZE_HANDLE_SIZE = 4;
    private final JIPipeDesktopVerticalToolBar leftToolBar = new JIPipeDesktopVerticalToolBar();
    private final JIPipeDesktopVerticalToolBar rightToolBar = new JIPipeDesktopVerticalToolBar();
    private final JLayeredPane layeredPane = new JLayeredPane();
    private final JPanel layeredPaneBackground = new JPanel(new BorderLayout());
    private final JPanel leftFloatingPanel = new JPanel(new BorderLayout());
    private final JPanel rightFloatingPanel = new JPanel(new BorderLayout());
    private final JPanel leftResizerPanel = new JPanel();
    private final JPanel rightResizerPanel = new JPanel();
    private int floatingMarginLeftRight = 8;
    private int floatingMarginTop = 8;
    private int floatingMarginBottom = 8;
    private int leftFloatingSize = 350;
    private int rightFloatingSize = 500;
    private int minimumFloatingSize = 50;
    private JComponent leftFloatingPanelContent;
    private JComponent rightFloatingPanelContent;
    private final Map<String, Panel> floatingPanels = new LinkedHashMap<>();
    private final Map<String, JToggleButton> floatingPanelToggles = new HashMap<>();
    private boolean floatingLeft = false;
    private boolean floatingRight = false;
    private final JIPipeDesktopSplitPane leftSplitPane = new JIPipeDesktopSplitPane(JSplitPane.VERTICAL_SPLIT, new JIPipeDesktopSplitPane.FixedRatio(0.33, true));
    private final JIPipeDesktopSplitPane rightSplitPane = new JIPipeDesktopSplitPane(JSplitPane.VERTICAL_SPLIT, new JIPipeDesktopSplitPane.FixedRatio(0.66, true));
    private State savedState = new State();

    public JIPipeDesktopDockPanel() {
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

        leftSplitPane.setDividerSize(12);
        rightSplitPane.setDividerSize(12);

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
        rightResizerPanel.setPreferredSize(new Dimension(RESIZE_HANDLE_SIZE, 64));
        rightResizerPanel.setMinimumSize(new Dimension(RESIZE_HANDLE_SIZE, 32));
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
        leftResizerPanel.setPreferredSize(new Dimension(RESIZE_HANDLE_SIZE, 64));
        leftResizerPanel.setMinimumSize(new Dimension(RESIZE_HANDLE_SIZE, 32));
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

        int availableWidth = layeredPane.getWidth();

        int leftMarginLeft = floatingLeft ? floatingMarginLeftRight : 0;
        int leftMarginTop = floatingLeft ? floatingMarginTop : 0;
        int leftMarginBottom = floatingLeft ? floatingMarginBottom : 0;
        int rightMarginRight = floatingRight ? floatingMarginLeftRight : 0;
        int rightMarginTop = floatingRight ? floatingMarginTop : 0;
        int rightMarginBottom = floatingRight ? floatingMarginBottom : 0;


        Dimension leftSize = new Dimension(leftFloatingSize, getHeight() - leftMarginTop - leftMarginBottom);
        Dimension rightSize = new Dimension(rightFloatingSize, getHeight() - rightMarginTop - rightMarginBottom);

        leftFloatingPanel.setPreferredSize(leftSize);
        rightFloatingPanel.setPreferredSize(rightSize);
        leftFloatingPanel.setMaximumSize(leftSize);
        rightFloatingPanel.setMaximumSize(rightSize);

        leftFloatingPanel.setBounds(leftMarginLeft, leftMarginTop, leftSize.width, leftSize.height);
        rightFloatingPanel.setBounds(availableWidth - rightMarginRight - rightFloatingSize - 2, rightMarginTop, rightSize.width, rightSize.height);

        int backgroundLeft;
        int backgroundWidth;

        if(leftFloatingPanel.isVisible() && rightFloatingPanel.isVisible()) {
            backgroundLeft = floatingLeft ? 0 : leftSize.width;
            backgroundWidth = layeredPane.getWidth() - (floatingRight ? 0 : rightSize.width) - backgroundLeft;
        }
        else if(leftFloatingPanel.isVisible()) {
            backgroundLeft = floatingLeft ? 0 : leftSize.width;
            backgroundWidth = layeredPane.getWidth() - backgroundLeft;
        }
        else {
            backgroundLeft = 0;
            backgroundWidth = layeredPane.getWidth();
        }

        layeredPaneBackground.setBounds(backgroundLeft, 0, backgroundWidth, getHeight());
//        layeredPaneBackground.setBounds(100,100,100,100);
//        layeredPaneBackground.setBounds(0,0,layeredPane.getWidth(),getHeight());

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
            leftSplitPane.setLeftComponent(leftContent.get(0));
            leftSplitPane.setRightComponent(leftContent.get(1));
            leftFloatingPanelContent = leftSplitPane;
        }
        else if(leftContent.size() == 1) {
            // use directly
            leftFloatingPanelContent = leftContent.get(0);
            leftSplitPane.setLeftComponent(new JPanel());
            leftSplitPane.setRightComponent(new JPanel());
        }
        else {
            leftSplitPane.setLeftComponent(new JPanel());
            leftSplitPane.setRightComponent(new JPanel());
        }

        if(rightContent.size() >= 2) {
            // create split pane
            rightSplitPane.setLeftComponent(rightContent.get(0));
            rightSplitPane.setRightComponent(rightContent.get(1));
            rightFloatingPanelContent = rightSplitPane;
        }
        else if(rightContent.size() == 1) {
            // use directly
            rightFloatingPanelContent = rightContent.get(0);
            rightSplitPane.setLeftComponent(new JPanel());
            rightSplitPane.setRightComponent(new JPanel());
        }
        else {
            rightSplitPane.setLeftComponent(new JPanel());
            rightSplitPane.setRightComponent(new JPanel());
        }

        leftSplitPane.applyRatio();
        rightSplitPane.applyRatio();

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
        revalidate();
        repaint(50);
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
                activatePanel(panel, true);
            }
            else {
                deactivatePanel(panel, true);
            }
        });
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

        floatingPanelToggles.put(panel.getId(), button);
        return button;
    }

    public void deactivatePanel(Panel panel, boolean saveState) {
        panel.setVisible(false);
        updateContent();
        updateSizes();
        updateToolbars();
        if(saveState) {
            saveState();
        }
    }

    private void saveState() {
        savedState = getCurrentState();
    }

    private void activatePanel(Panel panel, boolean saveState) {
        if(panel.getComponent() == null && panel.getComponentSupplier() != null) {
            panel.setComponent(panel.getComponentSupplier().get());
            panel.setComponentSupplier(null);
        }

        // Deactivate all other buttons
        setPanelVisible(panel);

        updateContent();
        updateSizes();
        updateToolbars();
        if(saveState) {
            saveState();
        }
    }

    private void setPanelVisible(Panel panel) {
        for (Panel otherPanel : getPanelsAtLocation(panel.getLocation())) {
            if(otherPanel != panel) {
                otherPanel.setVisible(false);
                floatingPanelToggles.get(otherPanel.getId()).setSelected(false);
            }
        }
        panel.setVisible(true);
    }

    public boolean isFloatingLeft() {
        return floatingLeft;
    }

    public void setFloatingLeft(boolean floatingLeft) {
        this.floatingLeft = floatingLeft;
        updateSizes();
    }

    public boolean isFloatingRight() {
        return floatingRight;
    }

    public void setFloatingRight(boolean floatingRight) {
        this.floatingRight = floatingRight;
        updateSizes();
    }

    public List<Panel> getPanelsAtLocation(PanelLocation location) {
        return floatingPanels.values().stream().filter(panel -> panel.getLocation() == location).collect(Collectors.toList());
    }

    public void movePanelToLocation(String id, PanelLocation newLocation, boolean saveState) {
        movePanelToLocation(floatingPanels.get(id), newLocation, saveState);
    }

    private void movePanelToLocation(Panel panel, PanelLocation newLocation, boolean saveState) {
        PanelLocation oldLocation = panel.getLocation();
        if(oldLocation != null && !oldLocation.equals(newLocation)) {

            if(panel.isDisplayed()) {
                deactivatePanels(newLocation, false);
            }

            panel.setLocation(newLocation);

            updateToolbars();
            updateContent();
            updateSizes();

            if(saveState) {
                saveState();
            }
        }
    }

    /**
     * Deactivates all panels at a given location
     * @param location the location
     * @param saveState save the state
     */
    public void deactivatePanels(PanelLocation location, boolean saveState) {
        for (Panel panel : getPanelsAtLocation(location)) {
            if(panel.isDisplayed()) {
                deactivatePanel(panel, false);
            }
        }
        if(saveState) {
            saveState();
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
            updateSizes();
        }
    }

    public void addDockPanel(String id, String name, Icon icon, PanelLocation location, boolean visible, JComponent component) {
        visible = tryRestoreVisibilityState(savedState, id, visible);
        removeDockPanel(id);

        Panel panel = new Panel(id);
        panel.setLocation(location);
        panel.setComponent(component);
        panel.setIcon(icon);
        panel.setName(name);
        floatingPanels.put(id, panel);

        if(visible) {
           activatePanel(panel, false);
        }
        else {
            updateToolbars();
        }
    }

    private boolean tryRestoreVisibilityState(State state, String id, boolean defaultValue) {
        return state.getVisibilities().getOrDefault(id, defaultValue);
    }

    public void addDockPanel(String id, String name, Icon icon, PanelLocation location, boolean visible, Supplier<JComponent> component) {
        visible = tryRestoreVisibilityState(savedState, id, visible);
        removeDockPanel(id);


        Panel panel = new Panel(id);
        panel.setLocation(location);
        panel.setComponentSupplier(component);
        panel.setIcon(icon);
        panel.setName(name);
        floatingPanels.put(id, panel);

        if(visible) {
            activatePanel(panel, false);
        }
        else {
            updateToolbars();
        }
    }

    public void restoreState(State state) {
        for (Panel panel : floatingPanels.values()) {
            boolean visible = state.getVisibilities().getOrDefault(panel.getId(), panel.isVisible());
            if(visible) {
                setPanelVisible(panel);
            }
            else {
                panel.setVisible(false);
            }
        }
        updateToolbars();
        updateSizes();
        updateContent();
    }

    public void activatePanel(String id, boolean saveState) {
        Panel panel = floatingPanels.get(id);
        if(panel != null) {
            activatePanel(panel, saveState);
        }
    }

    public <T extends JComponent> T getPanel(String id, Class<T> klass) {
        Panel panel = floatingPanels.getOrDefault(id, null);
        if(panel != null) {
            return (T) panel.getComponent();
        }
        else {
            return null;
        }
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

    public int getFloatingMarginTop() {
        return floatingMarginTop;
    }

    public void setFloatingMarginTop(int floatingMarginTop) {
        this.floatingMarginTop = floatingMarginTop;
    }

    public int getFloatingMarginBottom() {
        return floatingMarginBottom;
    }

    public void setFloatingMarginBottom(int floatingMarginBottom) {
        this.floatingMarginBottom = floatingMarginBottom;
    }

    public int getFloatingMarginLeftRight() {
        return floatingMarginLeftRight;
    }

    public void setFloatingMarginLeftRight(int floatingMarginLeftRight) {
        this.floatingMarginLeftRight = floatingMarginLeftRight;
    }

    public void removeDockPanelsIf(Predicate<Panel> predicate) {
        boolean found = false;
        for (Panel panel : ImmutableList.copyOf(floatingPanels.values())) {
            if (predicate.test(panel)) {
                floatingPanelToggles.remove(panel.getId());
                floatingPanels.remove(panel.getId());
                found = true;
            }
        }
        if(found) {
            updateToolbars();
            updateContent();
            updateSizes();
        }
    }

    public State getCurrentState() {
        State state = new State();
        for (Panel panel : floatingPanels.values()) {
            state.visibilities.put(panel.getId(), panel.isVisible());
        }
        return state;
    }

    public static class State {
        private Map<String, Boolean> visibilities = new HashMap<>();

        @JsonGetter("visibilities")
        public Map<String, Boolean> getVisibilities() {
            return visibilities;
        }

        @JsonSetter("visibilities")
        public void setVisibilities(Map<String, Boolean> visibilities) {
            this.visibilities = visibilities;
        }
    }

    public static class Panel {
        private final String id;
        private Icon icon;
        private JComponent component;
        private Supplier<JComponent> componentSupplier;
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

        public Supplier<JComponent> getComponentSupplier() {
            return componentSupplier;
        }

        public void setComponentSupplier(Supplier<JComponent> componentSupplier) {
            this.componentSupplier = componentSupplier;
        }
    }
}