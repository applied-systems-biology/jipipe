package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraphEdge;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.events.AlgorithmGraphConnectedEvent;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.extensions.settings.GraphEditorUISettings;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbenchPanel;
import org.hkijena.acaq5.ui.components.PickAlgorithmDialog;
import org.hkijena.acaq5.ui.events.*;
import org.hkijena.acaq5.utils.PointRange;
import org.hkijena.acaq5.utils.ScreenImage;
import org.hkijena.acaq5.utils.ScreenImageSVG;
import org.hkijena.acaq5.utils.UIUtils;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

/**
 * UI that displays an {@link ACAQAlgorithmGraph}
 */
public class ACAQAlgorithmGraphCanvasUI extends ACAQWorkbenchPanel implements MouseMotionListener, MouseListener {
    private final ImageIcon cursorImage = UIUtils.getIconFromResources("target.png");
    private ACAQAlgorithmGraph algorithmGraph;
    private ACAQAlgorithmUI currentlyDragged;
    private Point currentlyDraggedOffset = new Point();
    private BiMap<ACAQGraphNode, ACAQAlgorithmUI> nodeUIs = HashBiMap.create();
    private Set<ACAQAlgorithmUI> selection = new HashSet<>();
    private EventBus eventBus = new EventBus();
    private int newEntryLocationX = ACAQAlgorithmUI.SLOT_UI_WIDTH * 4;
    private int newEntryLocationY = ACAQAlgorithmUI.SLOT_UI_HEIGHT;
    private boolean layoutHelperEnabled;
    private String compartment;
    private JPopupMenu contextMenu;
    private ViewMode currentViewMode = GraphEditorUISettings.getInstance().getDefaultViewMode();
    private ACAQAlgorithmGraphDragAndDropBehavior dragAndDropBehavior;
    private ACAQAlgorithmGraphCopyPasteBehavior copyPasteBehavior;
    private Point cursor;
    private long lastTimeExpandedNegative = 0;

    /**
     * Used to store the minimum dimensions of the canvas to reduce user disruption
     */
    private Dimension minDimensions = null;

    /**
     * Creates a new UI
     *
     * @param workbench
     * @param algorithmGraph The algorithm graph
     * @param compartment    The compartment to show
     */
    public ACAQAlgorithmGraphCanvasUI(ACAQWorkbench workbench, ACAQAlgorithmGraph algorithmGraph, String compartment) {
        super(workbench);
        setLayout(null);
        this.algorithmGraph = algorithmGraph;
        this.compartment = compartment;
        initialize();
        addNewNodes();
        algorithmGraph.getEventBus().register(this);
    }

    private void initialize() {
        setBackground(Color.WHITE);
        addMouseListener(this);
        addMouseMotionListener(this);
        initializeContextMenu();
    }

    private void initializeContextMenu() {
        contextMenu = new JPopupMenu();

        JMenuItem moveHereItem = new JMenuItem("Move node here ...", UIUtils.getIconFromResources("move.png"));
        moveHereItem.setToolTipText("Move a specified node to the mouse position");
        moveHereItem.addActionListener(e -> moveNodeHere());
        contextMenu.add(moveHereItem);
    }

    /**
     * Updates the context menus of all node UIs
     */
    public void updateContextMenus() {
        for (ACAQAlgorithmUI ui : nodeUIs.values()) {
            ui.updateContextMenu();
        }
    }

    /**
     * Attempts to move the node to the mouse position
     *
     * @param node the node
     */
    public void tryMoveNodeToMouse(ACAQGraphNode node) {
        Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(mouseLocation, this);
        ACAQAlgorithmUI ui = nodeUIs.getOrDefault(node, null);
        if (ui != null) {
            ui.trySetLocationInGrid(mouseLocation.x, mouseLocation.y);
            repaint();
            getEventBus().post(new AlgorithmEvent(ui));
        }
    }

    private void moveNodeHere() {
        Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(mouseLocation, this);
        setGraphEditorCursor(new Point(mouseLocation.x, mouseLocation.y));
        ACAQGraphNode algorithm = PickAlgorithmDialog.showDialog(this, nodeUIs.keySet(), "Move node");
        if (algorithm != null) {
            ACAQAlgorithmUI ui = nodeUIs.getOrDefault(algorithm, null);
            if (ui != null) {
                ui.trySetLocationInGrid(mouseLocation.x, mouseLocation.y);
                repaint();
                getEventBus().post(new AlgorithmEvent(ui));
            }
        }
    }

    /**
     * @return The displayed graph
     */
    public ACAQAlgorithmGraph getAlgorithmGraph() {
        return algorithmGraph;
    }

    /**
     * Removes all node UIs
     */
    private void removeAllNodes() {
        for (ACAQAlgorithmUI ui : ImmutableList.copyOf(nodeUIs.values())) {
            remove(ui);
        }
        nodeUIs.clear();
        selection.clear();
        revalidate();
        repaint();
        updateSelection();
    }

    /**
     * Removes node UIs that are not valid anymore
     */
    private void removeOldNodes() {
        Set<ACAQGraphNode> toRemove = new HashSet<>();
        for (Map.Entry<ACAQGraphNode, ACAQAlgorithmUI> kv : nodeUIs.entrySet()) {
            if (!algorithmGraph.containsNode(kv.getKey()) || !kv.getKey().isVisibleIn(compartment))
                toRemove.add(kv.getKey());
        }
        for (ACAQGraphNode algorithm : toRemove) {
            ACAQAlgorithmUI ui = nodeUIs.get(algorithm);
            selection.remove(ui);
            remove(ui);
            nodeUIs.remove(algorithm);
        }
        if (!toRemove.isEmpty()) {
            updateSelection();
            revalidate();
            repaint();
        }
    }

    /**
     * Adds node UIs that are not in the canvas yet
     */
    private void addNewNodes() {
        int newlyPlacedAlgorithms = 0;
        ACAQAlgorithmUI ui = null;
        for (ACAQGraphNode algorithm : algorithmGraph.traverseAlgorithms()) {
            if (!algorithm.isVisibleIn(compartment))
                continue;
            if (nodeUIs.containsKey(algorithm))
                continue;

            switch (currentViewMode) {
                case Horizontal:
                    ui = new ACAQHorizontalAlgorithmUI(getWorkbench(), this, algorithm);
                    break;
                case Vertical:
                    ui = new ACAQVerticalAlgorithmUI(getWorkbench(), this, algorithm);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown view mode!");
            }

            ui.getEventBus().register(this);
            add(ui);
            nodeUIs.put(algorithm, ui);
            Point location = algorithm.getLocationWithin(compartment, currentViewMode.toString());
            if (location == null || !ui.trySetLocationNoGrid(location.x, location.y)) {
                autoPlaceAlgorithm(ui);
                ++newlyPlacedAlgorithms;
            }
            ui.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    removeComponentOverlaps();
                }
            });
        }
        revalidate();
        repaint();

        if (newlyPlacedAlgorithms == nodeUIs.size()) {
            autoLayoutAll();
        }
        if (ui != null) {
            getEventBus().post(new AlgorithmEvent(ui));
        }
    }

    /**
     * Detects components overlapping each other.
     * Auto-places components when an overlap was detected
     */
    private void removeComponentOverlaps() {
        List<ACAQGraphNode> traversed = algorithmGraph.traverseAlgorithms();
        boolean detected = false;
        for (int i = traversed.size() - 1; i >= 0; --i) {
            ACAQGraphNode algorithm = traversed.get(i);
            if (!algorithm.isVisibleIn(compartment))
                continue;
            ACAQAlgorithmUI ui = nodeUIs.getOrDefault(algorithm, null);
            if (ui != null) {
                if (ui.isOverlapping()) {
                    autoPlaceAlgorithm(ui);
                    detected = true;
                }
            }
        }
        if (detected) {
            repaint();
        }
    }

    /**
     * Auto-layouts all UIs
     */
    public void autoLayoutAll() {
        if (nodeUIs.isEmpty())
            return;
//        int backup = newEntryLocationX;
//        newEntryLocationX = 0;
//        for (ACAQAlgorithm algorithm : ImmutableList.copyOf(nodeUIs.keySet())) {
//            algorithm.setLocationWithin(compartment, null);
//            remove(nodeUIs.get(algorithm));
//        }
//        nodeUIs.clear();
//        addNewNodes();
//        newEntryLocationX = backup;
        minDimensions = null;
        autoLayoutSugiyama();
        if (getParent() != null)
            getParent().revalidate();
    }

    /**
     * Triggers when an {@link ACAQAlgorithmUI} requests an action.
     * Passes the action to its own event bus
     *
     * @param event event
     */
    @Subscribe
    public void onActionRequested(AlgorithmUIActionRequestedEvent event) {
        getEventBus().post(event);
    }

    private void autoPlaceAlgorithm(ACAQAlgorithmUI ui) {
        ACAQGraphNode targetAlgorithm = ui.getAlgorithm();

        if (currentViewMode == ViewMode.Horizontal) {
            // Find the source algorithm that is right-most
            ACAQAlgorithmUI rightMostSource = null;
            for (ACAQDataSlot target : targetAlgorithm.getInputSlots()) {
                ACAQDataSlot source = algorithmGraph.getSourceSlot(target);
                if (source != null) {
                    ACAQAlgorithmUI sourceUI = nodeUIs.getOrDefault(source.getAlgorithm(), null);
                    if (sourceUI != null) {
                        if (rightMostSource == null || sourceUI.getX() > rightMostSource.getX())
                            rightMostSource = sourceUI;
                    }
                }
            }

            // Auto-place
            int minX = (int) (newEntryLocationX * 1.0 / ACAQAlgorithmUI.SLOT_UI_WIDTH) * ACAQAlgorithmUI.SLOT_UI_WIDTH;
            minX += ACAQAlgorithmUI.SLOT_UI_WIDTH * 4;
            if (rightMostSource != null) {
                minX = Math.max(minX, rightMostSource.getX() + rightMostSource.getWidth() + 2 * ACAQAlgorithmUI.SLOT_UI_WIDTH);
            }

            int minY = Math.max(ui.getY(), 2 * ACAQAlgorithmUI.SLOT_UI_HEIGHT);

            if (cursor != null && cursor.x >= 0 && cursor.y >= 0) {
                minX = (int) (cursor.x * 1.0 / ACAQAlgorithmUI.SLOT_UI_WIDTH) * ACAQAlgorithmUI.SLOT_UI_WIDTH;
                minY = (int) (cursor.y * 1.0 / ACAQAlgorithmUI.SLOT_UI_HEIGHT) * ACAQAlgorithmUI.SLOT_UI_HEIGHT;
            }

            if (ui.getX() < minX || ui.isOverlapping()) {
                if (!ui.trySetLocationNoGrid(minX, minY)) {
                    // Place anywhere
                    int y = nodeUIs.values().stream().map(ACAQAlgorithmUI::getBottomY).max(Integer::compareTo).orElse(0);
                    if (y == 0)
                        y += 2 * ACAQAlgorithmUI.SLOT_UI_HEIGHT;
                    else
                        y += ACAQAlgorithmUI.SLOT_UI_HEIGHT;

                    ui.trySetLocationNoGrid(minX, y);
                }
            }
        } else if (currentViewMode == ViewMode.Vertical) {
            // Find the source algorithm that is bottom-most
            ACAQAlgorithmUI bottomMostSource = null;
            for (ACAQDataSlot target : targetAlgorithm.getInputSlots()) {
                ACAQDataSlot source = algorithmGraph.getSourceSlot(target);
                if (source != null) {
                    ACAQAlgorithmUI sourceUI = nodeUIs.getOrDefault(source.getAlgorithm(), null);
                    if (sourceUI != null) {
                        if (bottomMostSource == null || sourceUI.getBottomY() > bottomMostSource.getBottomY())
                            bottomMostSource = sourceUI;
                    }
                }
            }

            // Auto-place
            int minY = (int) (newEntryLocationY * 1.0 / ACAQAlgorithmUI.SLOT_UI_HEIGHT) * ACAQAlgorithmUI.SLOT_UI_HEIGHT;
            if (bottomMostSource != null) {
                minY = Math.max(minY, bottomMostSource.getBottomY() + ACAQAlgorithmUI.SLOT_UI_HEIGHT);
            }

            int minX = Math.max(ui.getX(), 2 * ACAQAlgorithmUI.SLOT_UI_HEIGHT);

            if (cursor != null && cursor.x >= 0 && cursor.y >= 0) {
                minX = (int) (cursor.x * 1.0 / ACAQAlgorithmUI.SLOT_UI_WIDTH) * ACAQAlgorithmUI.SLOT_UI_WIDTH;
                minY = (int) (cursor.y * 1.0 / ACAQAlgorithmUI.SLOT_UI_HEIGHT) * ACAQAlgorithmUI.SLOT_UI_HEIGHT;
            }

            if (ui.getY() < minY || ui.isOverlapping()) {
                if (!ui.trySetLocationNoGrid(minX, minY)) {
                    // Place anywhere
                    int x = nodeUIs.values().stream().map(ACAQAlgorithmUI::getRightX).max(Integer::compareTo).orElse(0);
                    x += 2 * ACAQAlgorithmUI.SLOT_UI_WIDTH;

                    ui.trySetLocationNoGrid(x, minY);
                }
            }
        } else {
            throw new UnsupportedOperationException("Unknown view mode!");
        }

    }

    private void autoPlaceTargetAdjacent(ACAQAlgorithmUI sourceAlgorithmUI, ACAQDataSlot source, ACAQAlgorithmUI targetAlgorithmUI, ACAQDataSlot target) {
        int sourceSlotIndex = source.getAlgorithm().getOutputSlots().indexOf(source);
        int targetSlotIndex = target.getAlgorithm().getInputSlots().indexOf(target);
        if (sourceSlotIndex < 0 || targetSlotIndex < 0) {
            autoPlaceAlgorithm(targetAlgorithmUI);
            return;
        }

        if (currentViewMode == ViewMode.Horizontal) {
            int sourceSlotInternalY = sourceSlotIndex * ACAQAlgorithmUI.SLOT_UI_HEIGHT;
            int targetSlotInternalY = targetSlotIndex * ACAQAlgorithmUI.SLOT_UI_HEIGHT;

            int minX = sourceAlgorithmUI.getWidth() + sourceAlgorithmUI.getX() + ACAQAlgorithmUI.SLOT_UI_WIDTH * 2;
            int targetY = sourceAlgorithmUI.getY() + sourceSlotInternalY - targetSlotInternalY;

            int x = (int) (minX * 1.0 / ACAQAlgorithmUI.SLOT_UI_WIDTH) * ACAQAlgorithmUI.SLOT_UI_WIDTH;
            int y = (int) (targetY * 1.0 / ACAQAlgorithmUI.SLOT_UI_HEIGHT) * ACAQAlgorithmUI.SLOT_UI_HEIGHT;
            if (!targetAlgorithmUI.trySetLocationNoGrid(x, y)) {
                autoPlaceAlgorithm(targetAlgorithmUI);
            }
        } else {
            int x = sourceAlgorithmUI.getSlotLocation(source).center.x + sourceAlgorithmUI.getX();
            x -= targetAlgorithmUI.getSlotLocation(target).center.x;
            int y = sourceAlgorithmUI.getBottomY() + ACAQAlgorithmUI.SLOT_UI_HEIGHT;
            if (!targetAlgorithmUI.trySetLocationNoGrid(x, y)) {
                autoPlaceAlgorithm(targetAlgorithmUI);
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        if (currentlyDragged != null) {
            int xu = currentlyDraggedOffset.x + mouseEvent.getX();
            int yu = currentlyDraggedOffset.y + mouseEvent.getY();
            if (xu < 0 || yu < 0) {
                long currentTimeMillis = System.currentTimeMillis();
                int ex = xu < 0 ? ACAQAlgorithmUI.SLOT_UI_WIDTH : 0;
                int ey = yu < 0 ? ACAQAlgorithmUI.SLOT_UI_HEIGHT : 0;
                if (currentTimeMillis - lastTimeExpandedNegative > 100) {
                    for (ACAQAlgorithmUI value : nodeUIs.values()) {
                        if (value != currentlyDragged) {
                            value.setLocation(value.getX() + ex, value.getY() + ey);
                        }
                    }
                    lastTimeExpandedNegative = currentTimeMillis;
                }
            }

            int x = Math.max(0, currentlyDraggedOffset.x + mouseEvent.getX());
            int y = Math.max(0, currentlyDraggedOffset.y + mouseEvent.getY());
            currentlyDragged.trySetLocationInGrid(x, y);
            repaint();
//            updateEdgeUI();
            if (getParent() != null)
                getParent().revalidate();
        }
    }

    /**
     * Expands the canvas by moving all algorithms
     *
     * @param left expand left
     * @param top  expand top
     */
    public void expandLeftTop(boolean left, boolean top) {
        int ex = left ? ACAQAlgorithmUI.SLOT_UI_WIDTH : 0;
        int ey = top ? ACAQAlgorithmUI.SLOT_UI_HEIGHT : 0;
        for (ACAQAlgorithmUI value : nodeUIs.values()) {
            if (value != currentlyDragged) {
                value.setLocation(value.getX() + ex, value.getY() + ey);
            }
        }
        if (cursor != null) {
            cursor.x += ex;
            cursor.y += ey;
        }
        if (getParent() != null)
            getParent().revalidate();
        repaint();
    }

    /**
     * Expands the canvas
     *
     * @param right  expand right
     * @param bottom expand bottom
     */
    public void expandRightBottom(int right, int bottom) {
        if (minDimensions == null)
            minDimensions = new Dimension(getWidth(), getHeight());
        minDimensions.width += right;
        minDimensions.height += bottom;
        if (getParent() != null)
            getParent().revalidate();
    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        if (SwingUtilities.isLeftMouseButton(mouseEvent) && mouseEvent.getClickCount() == 2) {
            ACAQAlgorithmUI ui = pickComponent(mouseEvent);
            if (ui != null)
                eventBus.post(new DefaultAlgorithmUIActionRequestedEvent(ui));
        } else if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
            cursor = new Point(mouseEvent.getX(), mouseEvent.getY());
            requestFocusInWindow();
            repaint();
        } else if (SwingUtilities.isRightMouseButton(mouseEvent)) {
            ACAQAlgorithmUI ui = pickComponent(mouseEvent);
            if (ui != null) {
                ui.getContextMenu().show(this, mouseEvent.getX(), mouseEvent.getY());
            } else {
                contextMenu.show(this, mouseEvent.getX(), mouseEvent.getY());
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
            ACAQAlgorithmUI ui = pickComponent(mouseEvent);
            if (ui != null) {
                currentlyDragged = ui;
                currentlyDraggedOffset.x = ui.getX() - mouseEvent.getX();
                currentlyDraggedOffset.y = ui.getY() - mouseEvent.getY();
            }
            eventBus.post(new AlgorithmSelectedEvent(ui, mouseEvent.isShiftDown()));
        }
    }

    private ACAQAlgorithmUI pickComponent(MouseEvent mouseEvent) {
        for (int i = 0; i < getComponentCount(); ++i) {
            Component component = getComponent(i);
            if (component.getBounds().contains(mouseEvent.getX(), mouseEvent.getY())) {
                if (component instanceof ACAQAlgorithmUI) {
                    return (ACAQAlgorithmUI) component;
                }
            }
        }
        return null;
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
        currentlyDragged = null;
//        updateEdgeUI();
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {

    }

    /**
     * @return Returns true if the user is currently dragging an algorithm around
     */
    public boolean isDragging() {
        return currentlyDragged != null;
    }

    @Override
    public Dimension getPreferredSize() {
        int width = 0;
        int height = 0;
        for (int i = 0; i < getComponentCount(); ++i) {
            Component component = getComponent(i);
            width = Math.max(width, component.getX() + 2 * component.getWidth());
            height = Math.max(height, component.getY() + 2 * component.getHeight());
        }
        if (minDimensions != null) {
            width = Math.max(minDimensions.width, width);
            height = Math.max(minDimensions.height, height);
            minDimensions.width = width;
            minDimensions.height = height;
        } else {
            minDimensions = new Dimension(width, height);
        }
        return new Dimension(width, height);
    }

    /**
     * Should be triggered when the algorithm graph is changed.
     * Triggers a node update.
     *
     * @param event The generated event
     */
    @Subscribe
    public void onAlgorithmGraphChanged(AlgorithmGraphChangedEvent event) {
        addNewNodes();
        removeOldNodes();
    }

    /**
     * Should be triggered when a connection is made
     *
     * @param event The generated event
     */
    @Subscribe
    public void onAlgorithmConnected(AlgorithmGraphConnectedEvent event) {
        ACAQAlgorithmUI sourceNode = nodeUIs.getOrDefault(event.getSource().getAlgorithm(), null);
        ACAQAlgorithmUI targetNode = nodeUIs.getOrDefault(event.getTarget().getAlgorithm(), null);

        if (sourceNode != null && targetNode != null && layoutHelperEnabled) {
            Point cursorBackup = cursor;
            try {
                this.cursor = null;
                autoPlaceTargetAdjacent(sourceNode, event.getSource(), targetNode, event.getTarget());
            } finally {
                this.cursor = cursorBackup;
            }
        }
    }


    /**
     * Should be triggered when an {@link ACAQAlgorithmUI} requests that the algorithm settings should be opened
     *
     * @param event The generated event
     */
    @Subscribe
    public void onOpenAlgorithmSettings(AlgorithmSelectedEvent event) {
        eventBus.post(event);
    }

    /**
     * @return The event bus instance
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        NonCollidingRectangularLineDrawer drawer = new NonCollidingRectangularLineDrawer();

        // Draw the edges to outside compartments
        // The edges should be at the center input point of the node
        // They are not covered by the other method below
        g.setStroke(new BasicStroke(2));
        graphics.setColor(Color.LIGHT_GRAY);
        for (ACAQAlgorithmUI ui : nodeUIs.values()) {
            if (!ui.getAlgorithm().getVisibleCompartments().isEmpty()) {
                Point sourcePoint = new Point();
                Point targetPoint = new Point();
                if (currentViewMode == ViewMode.Horizontal) {
                    if (compartment == null || compartment.equals(ui.getAlgorithm().getCompartment())) {
                        sourcePoint.x = ui.getX() + ui.getWidth();
                        sourcePoint.y = ui.getY() + ACAQAlgorithmUI.SLOT_UI_HEIGHT / 2;
                        targetPoint.x = getWidth();
                        targetPoint.y = sourcePoint.y;
                    } else {
                        sourcePoint.x = 0;
                        sourcePoint.y = ui.getY() + ACAQAlgorithmUI.SLOT_UI_HEIGHT / 2;
                        targetPoint.x = ui.getX();
                        targetPoint.y = sourcePoint.y;
                    }
                } else {
                    if (compartment == null || compartment.equals(ui.getAlgorithm().getCompartment())) {
                        sourcePoint.x = ui.getX() + ui.getWidth() / 2;
                        sourcePoint.y = ui.getY() + ui.getHeight();
                        targetPoint.x = sourcePoint.x;
                        targetPoint.y = getHeight();
                    } else {
                        sourcePoint.x = ui.getX() + ui.getWidth() / 2;
                        sourcePoint.y = ui.getY();
                        targetPoint.x = sourcePoint.x;
                        targetPoint.y = 0;
                    }
                }
                drawOutsideEdge(g, sourcePoint, targetPoint, drawer);
            }
        }

        // Draw edges between loaded algorithm UIs
        graphics.setColor(Color.DARK_GRAY);
        for (Map.Entry<ACAQDataSlot, ACAQDataSlot> kv : algorithmGraph.getSlotEdges()) {
            ACAQDataSlot source = kv.getKey();
            ACAQDataSlot target = kv.getValue();
            ACAQAlgorithmUI sourceUI = nodeUIs.getOrDefault(source.getAlgorithm(), null);
            ACAQAlgorithmUI targetUI = nodeUIs.getOrDefault(target.getAlgorithm(), null);

            if (sourceUI == null || targetUI == null)
                continue;
            if (ACAQDatatypeRegistry.isTriviallyConvertible(source.getAcceptedDataType(), target.getAcceptedDataType()))
                graphics.setColor(Color.DARK_GRAY);
            else if (ACAQDatatypeRegistry.getInstance().isConvertible(source.getAcceptedDataType(), target.getAcceptedDataType()))
                graphics.setColor(Color.BLUE);
            else
                graphics.setColor(Color.RED);

            PointRange sourcePoint;
            PointRange targetPoint;

            sourcePoint = sourceUI.getSlotLocation(source);
            sourcePoint.add(sourceUI.getLocation());
            targetPoint = targetUI.getSlotLocation(target);
            targetPoint.add(targetUI.getLocation());

            // Tighten the point ranges: Bringing the centers together
            PointRange.tighten(sourcePoint, targetPoint);

            // Draw arrow
            drawEdge(g, sourcePoint.center, sourceUI.getBounds(), targetPoint.center, drawer);
        }

        // Draw selections
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0));
        for (ACAQAlgorithmUI ui : selection) {
            Rectangle bounds = ui.getBounds();
            bounds.x -= 4;
            bounds.y -= 4;
            bounds.width += 8;
            bounds.height += 8;
            g.setColor(ui.getBorderColor());
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        g.setStroke(new BasicStroke(1));

        if (cursor != null) {
            g.drawImage(cursorImage.getImage(),
                    cursor.x - cursorImage.getIconWidth() / 2,
                    cursor.y - cursorImage.getIconHeight() / 2,
                    null);
        }
    }

    /**
     * Returns the UI location of a data slot that is located in the graph
     *
     * @param slot the data slot
     * @return the center slot location. Null if the algorithm has no UI or the returned location is null
     */
    public Point getSlotLocation(ACAQDataSlot slot) {
        ACAQAlgorithmUI algorithmUI = nodeUIs.getOrDefault(slot.getAlgorithm(), null);
        if (algorithmUI != null) {
            PointRange location = algorithmUI.getSlotLocation(slot);
            if (location != null) {
                return new Point(algorithmUI.getX() + location.center.x, algorithmUI.getY() + location.center.y);
            }
        }
        return null;
    }

    private void drawOutsideEdge(Graphics2D g, Point sourcePoint, Point targetPoint, NonCollidingRectangularLineDrawer drawer) {
        int sourceA;
        int targetA;
        int sourceB;

        if (currentViewMode == ViewMode.Horizontal) {
            sourceA = sourcePoint.x;
            targetA = targetPoint.x;
            sourceB = sourcePoint.y;
        } else {
            sourceA = sourcePoint.y;
            targetA = targetPoint.y;
            sourceB = sourcePoint.x;
        }

        drawer.start(sourceA, sourceB);
        drawer.moveToMajor(targetA, false);
        drawer.drawCurrentSegment(g, currentViewMode);
    }

    private void drawEdge(Graphics2D g, Point sourcePoint, Rectangle sourceBounds, Point targetPoint, NonCollidingRectangularLineDrawer drawer) {
        int buffer;
        int sourceA;
        int targetA;
        int sourceB;
        int targetB;
        int componentStartB;
        int componentEndB;

        if (currentViewMode == ViewMode.Horizontal) {
            buffer = ACAQAlgorithmUI.SLOT_UI_WIDTH;
            sourceA = sourcePoint.x;
            targetA = targetPoint.x;
            sourceB = sourcePoint.y;
            targetB = targetPoint.y;
            componentStartB = sourceBounds.y;
            componentEndB = sourceBounds.y + sourceBounds.height;
        } else {
            buffer = ACAQAlgorithmUI.SLOT_UI_HEIGHT / 2;
            sourceA = sourcePoint.y;
            targetA = targetPoint.y;
            sourceB = sourcePoint.x;
            targetB = targetPoint.x;
            componentStartB = sourceBounds.x;
            componentEndB = sourceBounds.x + sourceBounds.width;
        }

        drawer.start(sourceA, sourceB);

        // Target point is above the source. We have to navigate around it
        if (sourceA > targetA) {
            // Add some space in major direction
            drawer.addToMajor(buffer, false);

            // Go left or right
            if (targetB <= drawer.getLastSegment().b1) {
                drawer.moveToMinor(Math.max(0, componentStartB - buffer), true);
            } else {
                drawer.moveToMinor(componentEndB + buffer, true);
            }

            // Go to target height
            drawer.moveToMajor(Math.max(0, targetA - buffer), false);
        } else if (sourceB != targetB) {
            // Add some space in major direction
            int dA = targetA - sourceA;
            drawer.moveToMajor(Math.min(sourceA + buffer, sourceA + dA / 2), false);
        }

        // Target point X is shifted
        if (drawer.getLastSegment().b1 != targetB) {
            drawer.moveToMinor(targetB, true);
        }

        // Go to end point
        drawer.moveToMajor(targetA, false);
        drawer.drawCurrentSegment(g, currentViewMode);
    }

    /**
     * Gets the X position where new entries are placed automatically
     *
     * @return X position
     */
    public int getNewEntryLocationX() {
        return newEntryLocationX;
    }

    /**
     * Sets the X position where new entries are placed automatically
     * This can be set by parent components to for example place algorithms into the current view
     *
     * @param newEntryLocationX X position
     */
    public void setNewEntryLocationX(int newEntryLocationX) {
        this.newEntryLocationX = newEntryLocationX;
    }

    /**
     * @return Returns true if the layout helper is enabled. It auto-layouts when connections are made
     */
    public boolean isLayoutHelperEnabled() {
        return layoutHelperEnabled;
    }

    /**
     * Enables or disables the layout helper
     *
     * @param layoutHelperEnabled If the layout helper should be enabled
     */
    public void setLayoutHelperEnabled(boolean layoutHelperEnabled) {
        this.layoutHelperEnabled = layoutHelperEnabled;
    }

    /**
     * @return The displayed compartment
     */
    public String getCompartment() {
        return compartment;
    }

    /**
     * Creates a screenshot of the whole graph compartment
     *
     * @return The screenshot image
     */
    public BufferedImage createScreenshotPNG() {
        return ScreenImage.createImage(this);
    }

    /**
     * Creates a screenshot of the whole graph compartment
     *
     * @return The screenshot image
     */
    public SVGGraphics2D createScreenshotSVG() {
        return ScreenImageSVG.createImage(this);
    }

    /**
     * Uses the Method by Sugiyama.
     * Code was adapted from https://blog.disy.net/sugiyama-method/
     */
    private void autoLayoutSugiyama() {
        // Create an algorithm UI graph
        DefaultDirectedGraph<SugiyamaVertex, DefaultEdge> sugiyamaGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
        Map<ACAQAlgorithmUI, SugiyamaVertex> vertexMap = new HashMap<>();
        for (ACAQAlgorithmUI ui : nodeUIs.values()) {
            SugiyamaVertex sugiyamaVertex = new SugiyamaVertex(ui);
            sugiyamaGraph.addVertex(sugiyamaVertex);
            vertexMap.put(ui, sugiyamaVertex);
        }
        for (ACAQAlgorithmGraphEdge edge : algorithmGraph.getGraph().edgeSet()) {
            ACAQAlgorithmUI sourceUI = nodeUIs.getOrDefault(algorithmGraph.getGraph().getEdgeSource(edge).getAlgorithm(), null);
            ACAQAlgorithmUI targetUI = nodeUIs.getOrDefault(algorithmGraph.getGraph().getEdgeTarget(edge).getAlgorithm(), null);
            if (sourceUI == null || targetUI == null)
                continue;
            if (sourceUI.getAlgorithm() == targetUI.getAlgorithm())
                continue;
            if (!sugiyamaGraph.containsEdge(vertexMap.get(sourceUI), vertexMap.get(targetUI)))
                sugiyamaGraph.addEdge(vertexMap.get(sourceUI), vertexMap.get(targetUI));
        }


        // Skip: Remove cycles. ACAQAlgorithmGraph ensures that there are none

        // Assign layerIndices
        int maxLayer = 0;
        for (SugiyamaVertex vertex : ImmutableList.copyOf(new TopologicalOrderIterator<>(sugiyamaGraph))) {
            for (DefaultEdge edge : sugiyamaGraph.incomingEdgesOf(vertex)) {
                SugiyamaVertex source = sugiyamaGraph.getEdgeSource(edge);
                vertex.layer = Math.max(vertex.layer, source.layer) + 1;
                maxLayer = Math.max(maxLayer, vertex.layer);
            }
        }

        // Create virtual vertices
        for (DefaultEdge edge : ImmutableList.copyOf(sugiyamaGraph.edgeSet())) {
            SugiyamaVertex source = sugiyamaGraph.getEdgeSource(edge);
            SugiyamaVertex target = sugiyamaGraph.getEdgeTarget(edge);
            int layerDifference = target.layer - source.layer;
            assert layerDifference >= 1;
            if (layerDifference > 1) {
                sugiyamaGraph.removeEdge(source, target);
                SugiyamaVertex lastLayer = source;
                for (int layer = source.layer + 1; layer < target.layer; ++layer) {
                    SugiyamaVertex virtual = new SugiyamaVertex();
                    virtual.layer = layer;
                    sugiyamaGraph.addVertex(virtual);
                    sugiyamaGraph.addEdge(lastLayer, virtual);
                    lastLayer = virtual;
                }
                sugiyamaGraph.addEdge(lastLayer, target);
            }
        }

        // Assign the row for each layer
        int maxIndex = 0;
        for (int layer = 0; layer <= maxLayer; ++layer) {
            int row = 0;
            for (SugiyamaVertex vertex : sugiyamaGraph.vertexSet()) {
                if (vertex.layer == layer) {
                    vertex.index = row++;
                    maxIndex = Math.max(maxIndex, vertex.index);
                }
            }
        }

//        // Reorder columns (23 is a magic number from the paper)
//        for(int i = 0; i < 23; ++i) {
//
//        }
        switch (currentViewMode) {
            case Horizontal:
                rearrangeSugiyamaHorizontal(sugiyamaGraph, maxLayer, maxIndex);
                break;
            case Vertical:
                rearrangeSugiyamaVertical(sugiyamaGraph, maxLayer, maxIndex);
                break;
        }

    }

    private void rearrangeSugiyamaHorizontal(DefaultDirectedGraph<SugiyamaVertex, DefaultEdge> sugiyamaGraph, int maxLayer, int maxIndex) {
        // Create a table of column -> row -> vertex
        int maxRow = maxIndex;
        int maxColumn = maxLayer;
        Map<Integer, Map<Integer, SugiyamaVertex>> vertexTable = new HashMap<>();
        for (SugiyamaVertex vertex : sugiyamaGraph.vertexSet()) {
            Map<Integer, SugiyamaVertex> column = vertexTable.getOrDefault(vertex.layer, null);
            if (column == null) {
                column = new HashMap<>();
                vertexTable.put(vertex.layer, column);
            }
            column.put(vertex.index, vertex);
        }

        // Calculate widths and heights
        Map<Integer, Integer> columnWidths = new HashMap<>();
        Map<Integer, Integer> rowHeights = new HashMap<>();
        for (SugiyamaVertex vertex : sugiyamaGraph.vertexSet()) {
            if (!vertex.virtual) {
                int column = vertex.layer;
                int row = vertex.index;
                int columnWidth = Math.max(vertex.algorithmUI.getWidth(), columnWidths.getOrDefault(column, 0));
                int rowHeight = Math.max(vertex.algorithmUI.getHeight(), rowHeights.getOrDefault(row, 0));
                columnWidths.put(column, columnWidth);
                rowHeights.put(row, rowHeight);
            }
        }
        for (int column : columnWidths.keySet()) {
            columnWidths.put(column, columnWidths.get(column) + 2 * ACAQAlgorithmUI.SLOT_UI_WIDTH);
        }
        for (int row : rowHeights.keySet()) {
            rowHeights.put(row, rowHeights.get(row) + ACAQAlgorithmUI.SLOT_UI_HEIGHT);
        }

        // Rearrange algorithms
        int x = ACAQAlgorithmUI.SLOT_UI_WIDTH;
        for (int column = 0; column <= maxColumn; ++column) {
            Map<Integer, SugiyamaVertex> columnMap = vertexTable.get(column);
            int y = ACAQAlgorithmUI.SLOT_UI_HEIGHT;
            for (int row = 0; row <= maxRow; ++row) {
                SugiyamaVertex vertex = columnMap.getOrDefault(row, null);
                if (vertex != null && !vertex.virtual) {
                    ACAQAlgorithmUI ui = vertex.algorithmUI;
                    ui.setLocation(x, y);
                }
                y += rowHeights.getOrDefault(row, 0);
            }
            x += columnWidths.get(column);
        }

        repaint();
    }

    private void rearrangeSugiyamaVertical(DefaultDirectedGraph<SugiyamaVertex, DefaultEdge> sugiyamaGraph, int maxLayer, int maxIndex) {
        // Create a table of column -> row -> vertex
        int maxRow = maxLayer;
        int maxColumn = maxIndex;
        Map<Integer, Map<Integer, SugiyamaVertex>> vertexTable = new HashMap<>();
        for (SugiyamaVertex vertex : sugiyamaGraph.vertexSet()) {
            Map<Integer, SugiyamaVertex> column = vertexTable.getOrDefault(vertex.index, null);
            if (column == null) {
                column = new HashMap<>();
                vertexTable.put(vertex.index, column);
            }
            column.put(vertex.layer, vertex);
        }

        // Calculate widths and heights
        Map<Integer, Integer> columnWidths = new HashMap<>();
        Map<Integer, Integer> rowHeights = new HashMap<>();
        for (SugiyamaVertex vertex : sugiyamaGraph.vertexSet()) {
            if (!vertex.virtual) {
                int column = vertex.index;
                int row = vertex.layer;
                int columnWidth = Math.max(vertex.algorithmUI.getWidth(), columnWidths.getOrDefault(column, 0));
                int rowHeight = Math.max(vertex.algorithmUI.getHeight(), rowHeights.getOrDefault(row, 0));
                columnWidths.put(column, columnWidth);
                rowHeights.put(row, rowHeight);
            }
        }
        for (int column : columnWidths.keySet()) {
            columnWidths.put(column, columnWidths.get(column) + 2 * ACAQAlgorithmUI.SLOT_UI_WIDTH);
        }
        for (int row : rowHeights.keySet()) {
            rowHeights.put(row, rowHeights.get(row) + ACAQAlgorithmUI.SLOT_UI_HEIGHT);
        }

        // Rearrange algorithms
        int x = ACAQAlgorithmUI.SLOT_UI_WIDTH;
        for (int column = 0; column <= maxColumn; ++column) {
            Map<Integer, SugiyamaVertex> columnMap = vertexTable.get(column);
            int y = ACAQAlgorithmUI.SLOT_UI_HEIGHT;
            for (int row = 0; row <= maxRow; ++row) {
                SugiyamaVertex vertex = columnMap.getOrDefault(row, null);
                if (vertex != null && !vertex.virtual) {
                    ACAQAlgorithmUI ui = vertex.algorithmUI;
                    ui.setLocation(x, y);
                }
                y += rowHeights.getOrDefault(row, 0);
            }
            x += columnWidths.get(column);
        }

        repaint();
    }

    public ViewMode getCurrentViewMode() {
        return currentViewMode;
    }

    public void setCurrentViewMode(ViewMode currentViewMode) {
        if (currentViewMode != this.currentViewMode) {
            this.currentViewMode = currentViewMode;
            removeAllNodes();
            addNewNodes();
        }
    }

    public BiMap<ACAQGraphNode, ACAQAlgorithmUI> getNodeUIs() {
        return ImmutableBiMap.copyOf(nodeUIs);
    }

    public ACAQAlgorithmGraphCopyPasteBehavior getCopyPasteBehavior() {
        return copyPasteBehavior;
    }

    public void setCopyPasteBehavior(ACAQAlgorithmGraphCopyPasteBehavior copyPasteBehavior) {
        this.copyPasteBehavior = copyPasteBehavior;
        updateContextMenus();
    }

    public ACAQAlgorithmGraphDragAndDropBehavior getDragAndDropBehavior() {
        return dragAndDropBehavior;
    }

    public void setDragAndDropBehavior(ACAQAlgorithmGraphDragAndDropBehavior dragAndDropBehavior) {
        this.dragAndDropBehavior = dragAndDropBehavior;
        dragAndDropBehavior.setCanvas(this);
        new DropTarget(this, dragAndDropBehavior);
    }

    public JPopupMenu getContextMenu() {
        return contextMenu;
    }

    public int getNewEntryLocationY() {
        return newEntryLocationY;
    }

    public void setNewEntryLocationY(int newEntryLocationY) {
        this.newEntryLocationY = newEntryLocationY;
    }

    /**
     * @return the list of selected algorithms
     */
    public Set<ACAQAlgorithmUI> getSelection() {
        return Collections.unmodifiableSet(selection);
    }

    /**
     * Clears the list of selected algorithms
     */
    public void clearSelection() {
        selection.clear();
        updateSelection();
    }

    private void updateSelection() {
        repaint();
        eventBus.post(new AlgorithmSelectionChangedEvent(this));
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
            updateSelection();
        }
    }

    /**
     * Adds an algorithm to the selection
     *
     * @param ui The algorithm UI
     */
    public void addToSelection(ACAQAlgorithmUI ui) {
        selection.add(ui);
        updateSelection();
    }

    /**
     * Sets node positions to make the top left to 0, 0
     */
    public void crop() {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (ACAQAlgorithmUI ui : nodeUIs.values()) {
            minX = Math.min(ui.getX(), minX);
            minY = Math.min(ui.getY(), minY);
        }
        for (ACAQAlgorithmUI ui : nodeUIs.values()) {
            ui.setLocation(ui.getX() - minX + ACAQAlgorithmUI.SLOT_UI_WIDTH,
                    ui.getY() - minY + ACAQAlgorithmUI.SLOT_UI_HEIGHT);
        }
        cursor = new Point(ACAQAlgorithmUI.SLOT_UI_WIDTH, ACAQAlgorithmUI.SLOT_UI_HEIGHT);
        minDimensions = null;
        if (getParent() != null)
            getParent().revalidate();
    }

    public Point getGraphEditorCursor() {
        return cursor;
    }

    public void setGraphEditorCursor(Point cursor) {
        this.cursor = cursor;
        repaint();
    }

    /**
     * The direction how a canvas renders the nodes
     */
    public enum ViewMode {
        Horizontal,
        Vertical
    }

    /**
     * Used for autoLayoutSugiyama()
     */
    private static class SugiyamaVertex {
        private ACAQAlgorithmUI algorithmUI;
        private int layer = 0;
        private int index = 0;
        private boolean virtual = false;

        private SugiyamaVertex(ACAQAlgorithmUI algorithmUI) {
            this.algorithmUI = algorithmUI;
        }

        public SugiyamaVertex() {
            this.virtual = true;
        }
    }

    /**
     * Encapsulates drawing rectangular lines
     */
    private static class NonCollidingRectangularLineDrawer {

        private final List<RectangularLine> currentSegments = new ArrayList<>();
        private final PriorityQueue<RectangularLine> majorCollisionLines = new PriorityQueue<>(Comparator.comparing(RectangularLine::getA1));
        private final PriorityQueue<RectangularLine> minorCollisionLines = new PriorityQueue<>(Comparator.comparing(RectangularLine::getB1));
        private int lineSpacer = 4;

        public void start(int a0, int b) {
            currentSegments.clear();
            currentSegments.add(new RectangularLine(0, a0, 0, b));
        }

        private void addSpacer(RectangularLine lastSegment, RectangularLine newSegment) {
            if (lastSegment.a1 == newSegment.a1) {
                lastSegment.a1 += lineSpacer;
                newSegment.a1 += lineSpacer;
                newSegment.a0 += lineSpacer;
            }
            if (lastSegment.b1 == newSegment.b1) {
                lastSegment.b1 += lineSpacer;
                newSegment.b1 += lineSpacer;
            }
        }

        public void moveToMajor(int a1, boolean withCollision) {
            RectangularLine lastSegment = getLastSegment();
            RectangularLine newSegment = new RectangularLine(lastSegment.a1, a1, lastSegment.b1, lastSegment.b1);
            if (withCollision) {
                for (RectangularLine collisionLine : majorCollisionLines) {
                    int intersection = collisionLine.intersect(newSegment);
                    if (intersection > lineSpacer) {
                        addSpacer(lastSegment, newSegment);
                    }
                }
            }
            currentSegments.add(newSegment);
        }

        public void moveToMinor(int b, boolean withCollision) {
            RectangularLine lastSegment = getLastSegment();
            RectangularLine newSegment = new RectangularLine(lastSegment.a1, lastSegment.a1, lastSegment.b1, b);
            if (withCollision) {
                for (RectangularLine collisionLine : minorCollisionLines) {
                    int intersection = collisionLine.intersect(newSegment);
                    if (intersection > lineSpacer) {
                        addSpacer(lastSegment, newSegment);
                    }
                }
            }
            currentSegments.add(newSegment);
        }

        public RectangularLine getLastSegment() {
            return currentSegments.get(currentSegments.size() - 1);
        }

        public RectangularLine getFirstSegment() {
            return currentSegments.get(0);
        }

        public void addToMajor(int addA1, boolean withCollision) {
            moveToMajor(getLastSegment().a1 + addA1, withCollision);
        }

        public void addToMinor(int addB, boolean withCollision) {
            moveToMinor(getLastSegment().b1 + addB, withCollision);
        }

        public void drawCurrentSegment(Graphics2D graphics2D, ViewMode viewMode) {
            Path2D.Float path = new Path2D.Float();
            if (viewMode == ViewMode.Horizontal) {
                // A = X, B = Y
                path.moveTo(getFirstSegment().a1, getFirstSegment().b1);
                for (int i = 1; i < currentSegments.size(); ++i) {
                    RectangularLine currentSegment = currentSegments.get(i);
                    minorCollisionLines.add(currentSegment);
                    majorCollisionLines.add(currentSegment);
                    path.lineTo(currentSegment.a1, currentSegment.b1);
                }
            } else if (viewMode == ViewMode.Vertical) {
                // A = Y, B = X
                path.moveTo(getFirstSegment().b1, getFirstSegment().a1);
                for (int i = 1; i < currentSegments.size(); ++i) {
                    RectangularLine currentSegment = currentSegments.get(i);
                    minorCollisionLines.add(currentSegment);
                    majorCollisionLines.add(currentSegment);
                    path.lineTo(currentSegment.b1, currentSegment.a1);
                }
            }
            graphics2D.draw(path);
        }
    }

    /**
     * Contains coordinates of a line that is rectangular
     * This abstracts away x and y for horizontal and vertical implementations
     */
    private static class RectangularLine {
        public int a0;
        public int a1;
        public int b0;
        public int b1;

        /**
         * @param a0 the first major coordinate (equivalent to x0 in horizontal)
         * @param a1 the second major coordinate (equivalent to x1 in horizontal).
         * @param b0 the first minor coordinate (equivalent to y0 in horizontal)
         * @param b1 the second minor coordinate (equivalent to y1 in horizontal)
         */
        public RectangularLine(int a0, int a1, int b0, int b1) {
            this.a0 = a0;
            this.a1 = a1;
            this.b0 = b0;
            this.b1 = b1;
        }

        public int getA1() {
            return a1;
        }

        public int getB1() {
            return b1;
        }

        public int getSmallerMajor() {
            return Math.min(a0, a1);
        }

        public int getLargerMajor() {
            return Math.max(a0, a1);
        }

        public int getSmallerMinor() {
            return Math.min(b0, b1);
        }

        public int getLargerMinor() {
            return Math.max(b0, b1);
        }

        /**
         * Returns true if this line intersects with the other line
         *
         * @param other the other line
         * @return how many pixels intersect. Can be negative
         */
        public int intersect(RectangularLine other) {
            if (other.b1 == b1) {
                return Math.min(other.getLargerMajor(), getLargerMajor()) - Math.max(other.getSmallerMajor(), getSmallerMajor());
            } else if (other.a1 == a1) {
                return Math.min(other.getLargerMinor(), getLargerMinor()) - Math.max(other.getSmallerMinor(), getSmallerMinor());
            }
            return 0;
        }
    }
}
