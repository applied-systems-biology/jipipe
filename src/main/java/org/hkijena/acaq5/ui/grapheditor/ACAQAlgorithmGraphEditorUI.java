package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.events.AlgorithmRegisteredEvent;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbenchPanel;
import org.hkijena.acaq5.ui.events.AlgorithmSelectedEvent;
import org.hkijena.acaq5.utils.UIUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
        ACAQAlgorithmRegistry.getInstance().getEventBus().register(this);
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
        ACAQAlgorithmGraphUIDragAndDrop.install(graphUI);
        scrollPane = new JScrollPane(graphUI);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getHorizontalScrollBar().addAdjustmentListener(e -> {
            graphUI.setNewEntryLocationX(scrollPane.getHorizontalScrollBar().getValue());
        });
        splitPane.setLeftComponent(scrollPane);
        splitPane.setRightComponent(new JPanel());
        add(splitPane, BorderLayout.CENTER);

        add(menuBar, BorderLayout.NORTH);
        algorithmGraph.getEventBus().register(this);
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
}
