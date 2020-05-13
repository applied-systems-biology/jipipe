package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.events.AlgorithmRegisteredEvent;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbenchPanel;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.SearchComboBox;
import org.hkijena.acaq5.ui.events.AlgorithmEvent;
import org.hkijena.acaq5.ui.events.AlgorithmSelectedEvent;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A panel around {@link ACAQAlgorithmGraphCanvasUI} that comes with scrolling/panning, properties panel,
 * and a menu bar
 */
public class ACAQAlgorithmGraphEditorUI extends ACAQWorkbenchPanel implements MouseListener, MouseMotionListener {

    protected JMenuBar menuBar = new JMenuBar();
    private ACAQAlgorithmGraphCanvasUI graphUI;
    private ACAQAlgorithmGraph algorithmGraph;
    private String compartment;

    private JSplitPane splitPane;
    private JScrollPane scrollPane;
    private Point panningOffset = null;
    private Point panningScrollbarOffset = null;
    private boolean isPanning = false;
    private JToggleButton switchPanningDirectionButton;


    private Set<ACAQAlgorithmUI> selection = new HashSet<>();
    private Set<ACAQAlgorithmDeclaration> addableAlgorithms = new HashSet<>();
    private SearchComboBox<Object> navigator = new SearchComboBox<>();
    private JMenuItem cutContextMenuItem;
    private JMenuItem copyContextMenuItem;
    private JMenuItem pasteContextMenuItem;

    /**
     * @param workbenchUI    the workbench
     * @param algorithmGraph the algorithm graph
     * @param compartment    the graph compartment to display. Set to null to display all compartments
     */
    public ACAQAlgorithmGraphEditorUI(ACAQWorkbench workbenchUI, ACAQAlgorithmGraph algorithmGraph, String compartment) {
        super(workbenchUI);
        this.algorithmGraph = algorithmGraph;
        this.compartment = compartment;
        initialize();
        reloadMenuBar();
        updateContextMenu();
        ACAQAlgorithmRegistry.getInstance().getEventBus().register(this);
        algorithmGraph.getEventBus().register(this);
    }

    /**
     * Changes properties of the context menu.
     * You should not add new items, unless you always replace them
     */
    public void updateContextMenu() {
        cutContextMenuItem.setEnabled(graphUI.getCopyPasteBehavior() != null && !selection.isEmpty());
        copyContextMenuItem.setEnabled(graphUI.getCopyPasteBehavior() != null && !selection.isEmpty());
        pasteContextMenuItem.setEnabled(graphUI.getCopyPasteBehavior() != null);
    }

    public ACAQAlgorithmGraphCanvasUI getGraphUI() {
        return graphUI;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        splitPane = new JSplitPane();
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.33);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.66);
            }
        });

        graphUI = new ACAQAlgorithmGraphCanvasUI(algorithmGraph, compartment);
        graphUI.getEventBus().register(this);
        graphUI.addMouseListener(this);
        graphUI.addMouseMotionListener(this);
        scrollPane = new JScrollPane(graphUI);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getHorizontalScrollBar().addAdjustmentListener(e -> {
            graphUI.setNewEntryLocationX(scrollPane.getHorizontalScrollBar().getValue());
        });
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            graphUI.setNewEntryLocationY(scrollPane.getVerticalScrollBar().getValue());
        });
        splitPane.setLeftComponent(scrollPane);
        splitPane.setRightComponent(new JPanel());
        add(splitPane, BorderLayout.CENTER);

        add(menuBar, BorderLayout.NORTH);
        navigator.setModel(new DefaultComboBoxModel<>());
        navigator.setRenderer(new NavigationRenderer());
        navigator.addItemListener(e -> navigatorNavigate());
        navigator.setFilterFunction(ACAQAlgorithmGraphEditorUI::filterNavigationEntry);

        initializeContextMenu();
    }

    private void initializeContextMenu() {
        graphUI.getContextMenu().addSeparator();

        cutContextMenuItem = new JMenuItem("Cut", UIUtils.getIconFromResources("cut.png"));
        cutContextMenuItem.addActionListener(e -> graphUI.getCopyPasteBehavior()
                .cut(selection.stream().map(ACAQAlgorithmUI::getAlgorithm).collect(Collectors.toSet())));
        graphUI.getContextMenu().add(cutContextMenuItem);

        copyContextMenuItem = new JMenuItem("Copy", UIUtils.getIconFromResources("copy.png"));
        copyContextMenuItem.addActionListener(e -> graphUI.getCopyPasteBehavior()
                .copy(selection.stream().map(ACAQAlgorithmUI::getAlgorithm).collect(Collectors.toSet())));
        graphUI.getContextMenu().add(copyContextMenuItem);

        pasteContextMenuItem = new JMenuItem("Paste", UIUtils.getIconFromResources("paste.png"));
        pasteContextMenuItem.addActionListener(e -> graphUI.getCopyPasteBehavior().paste());
        graphUI.getContextMenu().add(pasteContextMenuItem);
    }

    private void navigatorNavigate() {
        if (navigator.getSelectedItem() instanceof ACAQAlgorithmUI) {
            selectOnly((ACAQAlgorithmUI) navigator.getSelectedItem());
            navigator.setSelectedItem(null);
        } else if (navigator.getSelectedItem() instanceof ACAQAlgorithmDeclaration) {
            ACAQAlgorithmDeclaration declaration = (ACAQAlgorithmDeclaration) navigator.getSelectedItem();
            algorithmGraph.insertNode(declaration.newInstance(), compartment);
            navigator.setSelectedItem(null);
        }

    }

    public Set<ACAQAlgorithmUI> getSelection() {
        return Collections.unmodifiableSet(selection);
    }

    /**
     * Reloads the menu bar
     */
    public void reloadMenuBar() {
        menuBar.removeAll();
        initializeCommonActions();
    }

    /**
     * Initializes the tool bar
     */
    protected void initializeCommonActions() {
        menuBar.add(Box.createHorizontalGlue());

        menuBar.add(navigator);
        JButton clearNavigator = new JButton(UIUtils.getIconFromResources("clear.png"));
        UIUtils.makeFlat25x25(clearNavigator);
        clearNavigator.addActionListener(e -> navigator.clearSearch());
        menuBar.add(clearNavigator);
        menuBar.add(Box.createHorizontalStrut(8));

        ButtonGroup viewModeGroup = new ButtonGroup();

        JToggleButton viewModeHorizontalButton = new JToggleButton(UIUtils.getIconFromResources("view-horizontal.png"));
        viewModeHorizontalButton.setToolTipText("Display nodes horizontally");
        UIUtils.makeFlat25x25(viewModeHorizontalButton);
        viewModeHorizontalButton.setSelected(graphUI.getCurrentViewMode() == ACAQAlgorithmGraphCanvasUI.ViewMode.Horizontal);
        viewModeHorizontalButton.addActionListener(e -> graphUI.setCurrentViewMode(ACAQAlgorithmGraphCanvasUI.ViewMode.Horizontal));
        viewModeGroup.add(viewModeHorizontalButton);
        menuBar.add(viewModeHorizontalButton);

        JToggleButton viewModeVerticalButton = new JToggleButton(UIUtils.getIconFromResources("view-vertical.png"));
        viewModeVerticalButton.setToolTipText("Display nodes vertically");
        UIUtils.makeFlat25x25(viewModeVerticalButton);
        viewModeVerticalButton.setSelected(graphUI.getCurrentViewMode() == ACAQAlgorithmGraphCanvasUI.ViewMode.Vertical);
        viewModeVerticalButton.addActionListener(e -> graphUI.setCurrentViewMode(ACAQAlgorithmGraphCanvasUI.ViewMode.Vertical));
        viewModeGroup.add(viewModeVerticalButton);
        menuBar.add(viewModeVerticalButton);

        menuBar.add(Box.createHorizontalStrut(8));

        JButton autoLayoutButton = new JButton(UIUtils.getIconFromResources("auto-layout-all.png"));
        autoLayoutButton.setToolTipText("Auto-layout all nodes");
        UIUtils.makeFlat25x25(autoLayoutButton);
        autoLayoutButton.addActionListener(e -> graphUI.autoLayoutAll());
        menuBar.add(autoLayoutButton);

        switchPanningDirectionButton = new JToggleButton(UIUtils.getIconFromResources("cursor-arrow.png"));
        switchPanningDirectionButton.setToolTipText("Reverse panning direction");
        UIUtils.makeFlat25x25(switchPanningDirectionButton);
        switchPanningDirectionButton.setToolTipText("Changes the direction how panning (middle mouse button) affects the view.");
        menuBar.add(switchPanningDirectionButton);

        JToggleButton layoutHelperButton;
        layoutHelperButton = new JToggleButton(UIUtils.getIconFromResources("auto-layout-connections.png"), true);
        UIUtils.makeFlat25x25(layoutHelperButton);
        layoutHelperButton.setToolTipText("Auto-layout layout on making data slot connections");
        graphUI.setLayoutHelperEnabled(true);
        layoutHelperButton.addActionListener(e -> graphUI.setLayoutHelperEnabled(layoutHelperButton.isSelected()));
        menuBar.add(layoutHelperButton);

        JButton createScreenshotButton = new JButton(UIUtils.getIconFromResources("filetype-image.png"));
        createScreenshotButton.setToolTipText("Export graph as *.png");
        UIUtils.makeFlat25x25(createScreenshotButton);
        createScreenshotButton.addActionListener(e -> createScreenshot());
        menuBar.add(createScreenshotButton);
    }

    /**
     * @return The edited graph
     */
    public ACAQAlgorithmGraph getAlgorithmGraph() {
        return algorithmGraph;
    }

    private void createScreenshot() {
        BufferedImage screenshot = graphUI.createScreenshot();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export graph as *.png");
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ImageIO.write(screenshot, "PNG", fileChooser.getSelectedFile());
                getWorkbench().sendStatusBarText("Exported graph as " + fileChooser.getSelectedFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Should be triggered when new algorithms are registered.
     * Reloads the menu
     *
     * @param event Generated event
     */
    @Subscribe
    public void onAlgorithmRegistryChanged(AlgorithmRegisteredEvent event) {
        reloadMenuBar();
        getWorkbench().sendStatusBarText("Plugins were updated");
    }

    /**
     * Should be triggered when an algorithm was selected
     *
     * @param event The generated event
     */
    @Subscribe
    public void onAlgorithmSelected(AlgorithmSelectedEvent event) {
        if (event.getUi() != null) {
            if (event.isAddToSelection()) {
                if (selection.contains(event.getUi())) {
                    removeFromSelection(event.getUi());
                } else {
                    addToSelection(event.getUi());
                }
            } else {
                selectOnly(event.getUi());
            }
        } else {
            clearSelection();
        }
    }

    /**
     * Triggered when something interesting happens in the graph and the UI should scroll to it
     *
     * @param event generated event
     */
    @Subscribe
    public void onAlgorithmEvent(AlgorithmEvent event) {
        if (event.getUi() != null) {
            scrollToAlgorithm(event.getUi());
        }
    }

    /**
     * Scrolls to the specified algorithm UI
     *
     * @param ui the algorithm
     */
    public void scrollToAlgorithm(ACAQAlgorithmUI ui) {
        int minViewX = scrollPane.getHorizontalScrollBar().getValue();
        int maxViewX = minViewX + scrollPane.getHorizontalScrollBar().getVisibleAmount();
        int minViewY = scrollPane.getVerticalScrollBar().getValue();
        int maxViewY = minViewY + scrollPane.getVerticalScrollBar().getVisibleAmount();
        if (ui.getX() < minViewX || ui.getX() > maxViewX) {
            scrollPane.getHorizontalScrollBar().setValue(ui.getX());
        }
        if (ui.getY() < minViewY || ui.getY() > maxViewY) {
            scrollPane.getVerticalScrollBar().setValue(ui.getY());
        }
    }

    /**
     * Clears the algorithm selection
     */
    public void clearSelection() {
        for (ACAQAlgorithmUI ui : selection) {
            ui.setSelected(false);
        }
        selection.clear();
        updateSelection();
    }

    /**
     * Selects only the specified algorithm
     *
     * @param ui The algorithm UI
     */
    public void selectOnly(ACAQAlgorithmUI ui) {
        if (selection.isEmpty()) {
            addToSelection(ui);
        } else if (selection.size() == 1) {
            if (selection.iterator().next() != ui) {
                clearSelection();
                addToSelection(ui);
            }
        } else {
            clearSelection();
            addToSelection(ui);
        }
        scrollToAlgorithm(ui);
    }

    /**
     * Removes an algorithm from the selection
     *
     * @param ui The algorithm UI
     */
    public void removeFromSelection(ACAQAlgorithmUI ui) {
        if (selection.contains(ui)) {
            selection.remove(ui);
            ui.setSelected(false);

            updateSelection();
        }
    }

    /**
     * Sets the component displayed in the right property panel
     *
     * @param content the component
     */
    protected void setPropertyPanel(Component content) {
        int dividerLocation = splitPane.getDividerLocation();
        splitPane.setRightComponent(content);
        splitPane.setDividerLocation(dividerLocation);
    }

    /**
     * Called when the selection was changed and the UI should react
     */
    protected void updateSelection() {
        updateContextMenu();
    }

    /**
     * Adds an algorithm to the selection
     *
     * @param ui The algorithm UI
     */
    public void addToSelection(ACAQAlgorithmUI ui) {
        selection.add(ui);
        ui.setSelected(true);
        updateSelection();
    }

    /**
     * Should be triggered when the algorithm graph is changed
     *
     * @param event The generated event
     */
    @Subscribe
    public void onGraphChanged(AlgorithmGraphChangedEvent event) {
        if (selection.stream().anyMatch(ui -> !algorithmGraph.getAlgorithmNodes().containsValue(ui.getAlgorithm()))) {
            clearSelection();
        }
        updateNavigation();
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isMiddleMouseButton(e)) {
            isPanning = true;
            int x = e.getX() - scrollPane.getHorizontalScrollBar().getValue();
            int y = e.getY() - scrollPane.getVerticalScrollBar().getValue();
            panningOffset = new Point(x, y);
            panningScrollbarOffset = new Point(scrollPane.getHorizontalScrollBar().getValue(),
                    scrollPane.getVerticalScrollBar().getValue());
            graphUI.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (isPanning) {
            graphUI.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
        isPanning = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (isPanning && panningOffset != null && panningScrollbarOffset != null) {
            int x = e.getX() - scrollPane.getHorizontalScrollBar().getValue();
            int y = e.getY() - scrollPane.getVerticalScrollBar().getValue();
            int dx = x - panningOffset.x;
            int dy = y - panningOffset.y;
            if (!switchPanningDirectionButton.isSelected()) {
                dx = -dx;
                dy = -dy;
            }
            int nx = panningScrollbarOffset.x + dx;
            int ny = panningScrollbarOffset.y + dy;
            scrollPane.getHorizontalScrollBar().setValue(nx);
            scrollPane.getVerticalScrollBar().setValue(ny);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    /**
     * @return The displayed graph compartment
     */
    public String getCompartment() {
        return compartment;
    }

    protected JMenuBar getMenuBar() {
        return menuBar;
    }

    public Set<ACAQAlgorithmDeclaration> getAddableAlgorithms() {
        return addableAlgorithms;
    }

    public void setAddableAlgorithms(Set<ACAQAlgorithmDeclaration> addableAlgorithms) {
        this.addableAlgorithms = addableAlgorithms;
        updateNavigation();
    }

    /**
     * Updates the navigation list
     */
    public void updateNavigation() {
        DefaultComboBoxModel<Object> model = (DefaultComboBoxModel<Object>) navigator.getModel();
        model.removeAllElements();
        for (ACAQAlgorithmUI ui : graphUI.getNodeUIs().values().stream().sorted(Comparator.comparing(ui -> ui.getAlgorithm().getName())).collect(Collectors.toList())) {
            model.addElement(ui);
        }
        for (ACAQAlgorithmDeclaration declaration : addableAlgorithms.stream()
                .sorted(Comparator.comparing(ACAQAlgorithmDeclaration::getName)).collect(Collectors.toList())) {
            model.addElement(declaration);
        }
    }

    private static boolean filterNavigationEntry(Object entry, String searchString) {
        String haystack = "";
        ACAQAlgorithmDeclaration algorithmDeclaration = null;
        if (entry instanceof ACAQAlgorithmUI) {
            haystack += ((ACAQAlgorithmUI) entry).getAlgorithm().getName();
            algorithmDeclaration = ((ACAQAlgorithmUI) entry).getAlgorithm().getDeclaration();
        } else if (entry instanceof ACAQAlgorithmDeclaration) {
            algorithmDeclaration = (ACAQAlgorithmDeclaration) entry;
        }
        if (algorithmDeclaration != null) {
            haystack += algorithmDeclaration.getName() + algorithmDeclaration.getDescription()
                    + algorithmDeclaration.getMenuPath();
        }
        return haystack.toLowerCase().contains(searchString.toLowerCase());
    }

    /**
     * Renders items in the navigator
     */
    public static class NavigationRenderer extends JLabel implements ListCellRenderer<Object> {

        /**
         * Creates a new instance
         */
        public NavigationRenderer() {
            setOpaque(true);
            setFont(getFont().deriveFont(Font.PLAIN));
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            if (value instanceof ACAQAlgorithmDeclaration) {
                ACAQAlgorithmDeclaration declaration = (ACAQAlgorithmDeclaration) value;
                String menuPath = declaration.getCategory().toString();
                if (!StringUtils.isNullOrEmpty(declaration.getMenuPath())) {
                    menuPath += " &gt; " + String.join(" &gt; ", declaration.getMenuPath().split("\n"));
                }

                setIcon(new ColorIcon(16, 32, Color.WHITE, UIUtils.getFillColorFor(declaration)));
                setText("<html>Create new <strong>" + declaration.getName() + "</strong><br/><span style=\"color: gray;\">" + menuPath + "</span></html>");
            } else if (value instanceof ACAQAlgorithmUI) {
                ACAQAlgorithmUI ui = (ACAQAlgorithmUI) value;
                ACAQAlgorithmDeclaration declaration = ui.getAlgorithm().getDeclaration();
                String menuPath = declaration.getCategory().toString();
                if (!StringUtils.isNullOrEmpty(declaration.getMenuPath())) {
                    menuPath += " &gt; " + String.join(" &gt; ", declaration.getMenuPath().split("\n"));
                }
                setIcon(new ColorIcon(16, 32, UIUtils.getFillColorFor(ui.getAlgorithm().getDeclaration()),
                        UIUtils.getBorderColorFor(ui.getAlgorithm().getDeclaration())));
                setText("<html>Navigate to <strong>" + ui.getAlgorithm().getName() + "</strong><br/><span style=\"color: gray;\">" + menuPath + "</span></html>");
            } else {
                setText("<Null>");
            }

            if (isSelected) {
                setBackground(new Color(184, 207, 229));
            } else {
                setBackground(new Color(255, 255, 255));
            }
            return this;
        }
    }
}
