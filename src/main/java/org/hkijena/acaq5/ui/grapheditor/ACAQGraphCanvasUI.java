/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQGraphEdge;
import org.hkijena.acaq5.api.algorithm.ACAQGraph;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.events.AlgorithmGraphConnectedEvent;
import org.hkijena.acaq5.api.history.ACAQGraphHistory;
import org.hkijena.acaq5.api.history.MoveNodesGraphHistorySnapshot;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.extensions.settings.GraphEditorUISettings;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbenchPanel;
import org.hkijena.acaq5.ui.events.AlgorithmEvent;
import org.hkijena.acaq5.ui.events.AlgorithmSelectedEvent;
import org.hkijena.acaq5.ui.events.AlgorithmSelectionChangedEvent;
import org.hkijena.acaq5.ui.events.AlgorithmUIActionRequestedEvent;
import org.hkijena.acaq5.ui.events.DefaultAlgorithmUIActionRequestedEvent;
import org.hkijena.acaq5.ui.grapheditor.connections.NonCollidingRectangularLineDrawer;
import org.hkijena.acaq5.ui.grapheditor.contextmenu.AlgorithmUIAction;
import org.hkijena.acaq5.ui.grapheditor.layout.SugiyamaGraphAutoLayoutMethod;
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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UI that displays an {@link ACAQGraph}
 */
public class ACAQGraphCanvasUI extends ACAQWorkbenchPanel implements MouseMotionListener, MouseListener {
    private final ImageIcon cursorImage = UIUtils.getIconFromResources("target.png");
    private final ACAQGraph graph;
    private final Point currentlyDraggedOffset = new Point();
    private final BiMap<ACAQGraphNode, ACAQNodeUI> nodeUIs = HashBiMap.create();
    private final Set<ACAQNodeUI> selection = new HashSet<>();
    private final EventBus eventBus = new EventBus();
    private final String compartment;
    private final ACAQGraphHistory graphHistory = new ACAQGraphHistory();
    private ACAQNodeUI currentlyDragged;
    private boolean layoutHelperEnabled;
    private ViewMode currentViewMode = GraphEditorUISettings.getInstance().getDefaultViewMode();
    private ACAQGraphDragAndDropBehavior dragAndDropBehavior;
    private Point cursor;
    private long lastTimeExpandedNegative = 0;
    private List<AlgorithmUIAction> contextActions = new ArrayList<>();
    private MoveNodesGraphHistorySnapshot currentlyDraggedSnapshot;

    /**
     * Used to store the minimum dimensions of the canvas to reduce user disruption
     */
    private Dimension minDimensions = null;

    /**
     * Creates a new UI
     *
     * @param workbench      the workbench
     * @param graph The algorithm graph
     * @param compartment    The compartment to show
     */
    public ACAQGraphCanvasUI(ACAQWorkbench workbench, ACAQGraph graph, String compartment) {
        super(workbench);
        setLayout(null);
        this.graph = graph;
        this.compartment = compartment;
        initialize();
        addNewNodes();
        graph.getEventBus().register(this);
    }

    private void initialize() {
        setBackground(Color.WHITE);
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    /**
     * @return The displayed graph
     */
    public ACAQGraph getGraph() {
        return graph;
    }

    /**
     * Removes all node UIs
     */
    private void removeAllNodes() {
        for (ACAQNodeUI ui : ImmutableList.copyOf(nodeUIs.values())) {
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
        for (Map.Entry<ACAQGraphNode, ACAQNodeUI> kv : nodeUIs.entrySet()) {
            if (!graph.containsNode(kv.getKey()) || !kv.getKey().isVisibleIn(compartment))
                toRemove.add(kv.getKey());
        }
        for (ACAQGraphNode algorithm : toRemove) {
            ACAQNodeUI ui = nodeUIs.get(algorithm);
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
        ACAQNodeUI ui = null;
        for (ACAQGraphNode algorithm : graph.traverseAlgorithms()) {
            if (!algorithm.isVisibleIn(compartment))
                continue;
            if (nodeUIs.containsKey(algorithm))
                continue;

            switch (currentViewMode) {
                case Horizontal:
                    ui = new ACAQHorizontalNodeUI(getWorkbench(), this, algorithm);
                    break;
                case Vertical:
                    ui = new ACAQVerticalNodeUI(getWorkbench(), this, algorithm);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown view mode!");
            }

            ui.getEventBus().register(this);
            add(ui);
            nodeUIs.put(algorithm, ui);
            Point location = algorithm.getLocationWithin(compartment, currentViewMode.toString());
            if (location == null || !ui.trySetLocationNoGrid(location.x, location.y)) {
                autoPlaceCloseToCursor(ui);
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
        List<ACAQGraphNode> traversed = graph.traverseAlgorithms();
        boolean detected = false;
        for (int i = traversed.size() - 1; i >= 0; --i) {
            ACAQGraphNode algorithm = traversed.get(i);
            if (!algorithm.isVisibleIn(compartment))
                continue;
            ACAQNodeUI ui = nodeUIs.getOrDefault(algorithm, null);
            if (ui != null) {
                if (ui.isOverlapping()) {
                    autoPlaceCloseToCursor(ui);
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
        autoLayout();
        if (getParent() != null)
            getParent().revalidate();
    }

    /**
     * Applies a full auto-layout method
     */
    public void autoLayout() {
        switch(GraphEditorUISettings.getInstance().getAutoLayout()) {
            case Sugiyama:
                (new SugiyamaGraphAutoLayoutMethod()).accept(this);
                break;
        }
        repaint();
    }

    /**
     * Triggers when an {@link ACAQNodeUI} requests an action.
     * Passes the action to its own event bus
     *
     * @param event event
     */
    @Subscribe
    public void onActionRequested(AlgorithmUIActionRequestedEvent event) {
        if (ACAQNodeUI.REQUEST_OPEN_CONTEXT_MENU.equals(event.getAction())) {
            if (event.getUi() != null) {
                openContextMenu(getMousePosition());
            }
        }
        getEventBus().post(event);
    }

    private void autoPlaceCloseToCursor(ACAQNodeUI ui) {
        int minX = 0;
        int minY = 0;
        if (cursor != null) {
            minX = cursor.x;
            minY = cursor.y;
        }

        Set<Rectangle> otherShapes = new HashSet<>();
        for (ACAQNodeUI otherUi : nodeUIs.values()) {
            if (ui != otherUi) {
                otherShapes.add(otherUi.getBounds());
            }
        }

        Rectangle currentShape = new Rectangle(minX, minY, ui.getWidth(), ui.getHeight());

        boolean found;
        do {
            found = true;
            for (Rectangle otherShape : otherShapes) {
                if (otherShape.intersects(currentShape)) {
                    found = false;
                    break;
                }
            }
            if (currentViewMode == ViewMode.Horizontal) {
                currentShape.y += ACAQNodeUI.SLOT_UI_HEIGHT;
            } else {
                currentShape.x += ACAQNodeUI.SLOT_UI_WIDTH;
            }
        }
        while (!found);

        ui.trySetLocationInGrid(currentShape.x, currentShape.y);

    }

    private void autoPlaceTargetAdjacent(ACAQNodeUI sourceAlgorithmUI, ACAQDataSlot source, ACAQNodeUI targetAlgorithmUI, ACAQDataSlot target) {
        int sourceSlotIndex = source.getNode().getOutputSlots().indexOf(source);
        int targetSlotIndex = target.getNode().getInputSlots().indexOf(target);
        if (sourceSlotIndex < 0 || targetSlotIndex < 0) {
            autoPlaceCloseToCursor(targetAlgorithmUI);
            return;
        }

        if (currentViewMode == ViewMode.Horizontal) {
            int sourceSlotInternalY = sourceSlotIndex * ACAQNodeUI.SLOT_UI_HEIGHT;
            int targetSlotInternalY = targetSlotIndex * ACAQNodeUI.SLOT_UI_HEIGHT;

            int minX = sourceAlgorithmUI.getWidth() + sourceAlgorithmUI.getX() + ACAQNodeUI.SLOT_UI_WIDTH * 2;
            int targetY = sourceAlgorithmUI.getY() + sourceSlotInternalY - targetSlotInternalY;

            int x = (int) (minX * 1.0 / ACAQNodeUI.SLOT_UI_WIDTH) * ACAQNodeUI.SLOT_UI_WIDTH;
            int y = (int) (targetY * 1.0 / ACAQNodeUI.SLOT_UI_HEIGHT) * ACAQNodeUI.SLOT_UI_HEIGHT;
            if (!targetAlgorithmUI.trySetLocationNoGrid(x, y)) {
                autoPlaceCloseToCursor(targetAlgorithmUI);
            }
        } else {
            int x = sourceAlgorithmUI.getSlotLocation(source).center.x + sourceAlgorithmUI.getX();
            x -= targetAlgorithmUI.getSlotLocation(target).center.x;
            int y = sourceAlgorithmUI.getBottomY() + ACAQNodeUI.SLOT_UI_HEIGHT;
            if (!targetAlgorithmUI.trySetLocationNoGrid(x, y)) {
                autoPlaceCloseToCursor(targetAlgorithmUI);
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
                int ex = xu < 0 ? ACAQNodeUI.SLOT_UI_WIDTH : 0;
                int ey = yu < 0 ? ACAQNodeUI.SLOT_UI_HEIGHT : 0;
                if (currentTimeMillis - lastTimeExpandedNegative > 100) {
                    for (ACAQNodeUI value : nodeUIs.values()) {
                        if (value != currentlyDragged) {
                            value.setLocation(value.getX() + ex, value.getY() + ey);
                        }
                    }
                    lastTimeExpandedNegative = currentTimeMillis;
                }
            }

            int x = Math.max(0, currentlyDraggedOffset.x + mouseEvent.getX());
            int y = Math.max(0, currentlyDraggedOffset.y + mouseEvent.getY());

            if (currentlyDraggedSnapshot != null) {
                // Check if something would change
                if (!Objects.equals(currentlyDragged.getLocation(), ACAQNodeUI.toGridLocation(new Point(x, y)))) {
                    graphHistory.addSnapshotBefore(currentlyDraggedSnapshot);
                    currentlyDraggedSnapshot = null;
                }
            }

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
        int ex = left ? ACAQNodeUI.SLOT_UI_WIDTH : 0;
        int ey = top ? ACAQNodeUI.SLOT_UI_HEIGHT : 0;
        for (ACAQNodeUI value : nodeUIs.values()) {
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
            ACAQNodeUI ui = pickComponent(mouseEvent);
            if (ui != null)
                eventBus.post(new DefaultAlgorithmUIActionRequestedEvent(ui));
        } else if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
            cursor = new Point(mouseEvent.getX(), mouseEvent.getY());
            requestFocusInWindow();
            repaint();
        } else if (SwingUtilities.isRightMouseButton(mouseEvent)) {
            if (selection.isEmpty()) {
                ACAQNodeUI ui = pickComponent(mouseEvent);
                selectOnly(ui);
            }
            openContextMenu(new Point(mouseEvent.getX(), mouseEvent.getY()));
        }
    }

    /**
     * Opens the context menu at the location.
     * The menu is generated based on the current node selection
     *
     * @param point the location
     */
    public void openContextMenu(Point point) {
        setGraphEditorCursor(new Point(point.x, point.y));
        JPopupMenu menu = new JPopupMenu();
        boolean scheduleSeparator = false;
        for (AlgorithmUIAction action : contextActions) {
            if (action == null) {
                scheduleSeparator = true;
                continue;
            }
            boolean matches = action.matches(selection);
            if (!matches && !action.disableOnNonMatch())
                continue;
            if (scheduleSeparator) {
                scheduleSeparator = false;
                menu.addSeparator();
            }
            JMenuItem item = new JMenuItem(action.getName(), action.getIcon());
            item.setToolTipText(action.getDescription());
            if (matches)
                item.addActionListener(e -> action.run(this, ImmutableSet.copyOf(selection)));
            else
                item.setEnabled(false);
            menu.add(item);
        }
        menu.show(this, point.x, point.y);
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
            ACAQNodeUI ui = pickComponent(mouseEvent);
            if (ui != null) {
                currentlyDragged = ui;
                currentlyDraggedOffset.x = ui.getX() - mouseEvent.getX();
                currentlyDraggedOffset.y = ui.getY() - mouseEvent.getY();
                currentlyDraggedSnapshot = new MoveNodesGraphHistorySnapshot(graph, "Move node");
            }
            eventBus.post(new AlgorithmSelectedEvent(ui, mouseEvent.isShiftDown()));
        }
    }

    private ACAQNodeUI pickComponent(MouseEvent mouseEvent) {
        for (int i = 0; i < getComponentCount(); ++i) {
            Component component = getComponent(i);
            if (component.getBounds().contains(mouseEvent.getX(), mouseEvent.getY())) {
                if (component instanceof ACAQNodeUI) {
                    return (ACAQNodeUI) component;
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
        // Update the location of existing nodes
        for (ACAQNodeUI ui : nodeUIs.values()) {
            Point point = ui.getNode().getLocationWithin(compartment, currentViewMode.name());
            if (point != null) {
                ui.setLocation(point);
            }
        }
        removeOldNodes();
        addNewNodes();
    }

    /**
     * Should be triggered when a connection is made
     *
     * @param event The generated event
     */
    @Subscribe
    public void onAlgorithmConnected(AlgorithmGraphConnectedEvent event) {
        ACAQNodeUI sourceNode = nodeUIs.getOrDefault(event.getSource().getNode(), null);
        ACAQNodeUI targetNode = nodeUIs.getOrDefault(event.getTarget().getNode(), null);

        // Check if we actually need to auto-place
        if(currentViewMode == ViewMode.Horizontal) {
            if(targetNode.getX() >= sourceNode.getRightX() + ACAQNodeUI.SLOT_UI_WIDTH) {
                return;
            }
        }
        else if(currentViewMode == ViewMode.Vertical) {
            if(targetNode.getY() >= sourceNode.getBottomY() + ACAQNodeUI.SLOT_UI_HEIGHT) {
                return;
            }
        }

        if (sourceNode != null && targetNode != null && layoutHelperEnabled) {
            Point cursorBackup = cursor;
            try {
                if(currentViewMode == ViewMode.Horizontal)
                    this.cursor = new Point(targetNode.getRightX() + 4 * ACAQNodeUI.SLOT_UI_WIDTH,
                            targetNode.getY());
                else
                    this.cursor = new Point(targetNode.getX(), targetNode.getBottomY() + 4 * ACAQNodeUI.SLOT_UI_HEIGHT);
                autoPlaceTargetAdjacent(sourceNode, event.getSource(), targetNode, event.getTarget());
            } finally {
                this.cursor = cursorBackup;
            }
        }
    }


    /**
     * Should be triggered when an {@link ACAQNodeUI} requests that the algorithm settings should be opened
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
        for (ACAQNodeUI ui : nodeUIs.values()) {
            if (!ui.getNode().getVisibleCompartments().isEmpty()) {
                Point sourcePoint = new Point();
                Point targetPoint = new Point();
                if (currentViewMode == ViewMode.Horizontal) {
                    if (compartment == null || compartment.equals(ui.getNode().getCompartment())) {
                        sourcePoint.x = ui.getX() + ui.getWidth();
                        sourcePoint.y = ui.getY() + ACAQNodeUI.SLOT_UI_HEIGHT / 2;
                        targetPoint.x = getWidth();
                        targetPoint.y = sourcePoint.y;
                    } else {
                        sourcePoint.x = 0;
                        sourcePoint.y = ui.getY() + ACAQNodeUI.SLOT_UI_HEIGHT / 2;
                        targetPoint.x = ui.getX();
                        targetPoint.y = sourcePoint.y;
                    }
                } else {
                    if (compartment == null || compartment.equals(ui.getNode().getCompartment())) {
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
        for (Map.Entry<ACAQDataSlot, ACAQDataSlot> kv : graph.getSlotEdges()) {
            ACAQDataSlot source = kv.getKey();
            ACAQDataSlot target = kv.getValue();
            ACAQNodeUI sourceUI = nodeUIs.getOrDefault(source.getNode(), null);
            ACAQNodeUI targetUI = nodeUIs.getOrDefault(target.getNode(), null);

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
        for (ACAQNodeUI ui : selection) {
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
        ACAQNodeUI algorithmUI = nodeUIs.getOrDefault(slot.getNode(), null);
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
            buffer = ACAQNodeUI.SLOT_UI_WIDTH;
            sourceA = sourcePoint.x;
            targetA = targetPoint.x;
            sourceB = sourcePoint.y;
            targetB = targetPoint.y;
            componentStartB = sourceBounds.y;
            componentEndB = sourceBounds.y + sourceBounds.height;
        } else {
            buffer = ACAQNodeUI.SLOT_UI_HEIGHT / 2;
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

    public BiMap<ACAQGraphNode, ACAQNodeUI> getNodeUIs() {
        return ImmutableBiMap.copyOf(nodeUIs);
    }

    public ACAQGraphDragAndDropBehavior getDragAndDropBehavior() {
        return dragAndDropBehavior;
    }

    public void setDragAndDropBehavior(ACAQGraphDragAndDropBehavior dragAndDropBehavior) {
        this.dragAndDropBehavior = dragAndDropBehavior;
        dragAndDropBehavior.setCanvas(this);
        new DropTarget(this, dragAndDropBehavior);
    }

    /**
     * @return the list of selected {@link ACAQNodeUI}
     */
    public Set<ACAQNodeUI> getSelection() {
        return Collections.unmodifiableSet(selection);
    }

    /**
     * @return the list of selected nodes
     */
    public Set<ACAQGraphNode> getSelectedNodes() {
        return selection.stream().map(ACAQNodeUI::getNode).collect(Collectors.toSet());
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
    public void selectOnly(ACAQNodeUI ui) {
        if (ui == null) {
            clearSelection();
            return;
        }
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
    public void removeFromSelection(ACAQNodeUI ui) {
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
    public void addToSelection(ACAQNodeUI ui) {
        selection.add(ui);
        updateSelection();
    }

    /**
     * Sets node positions to make the top left to 0, 0
     */
    public void crop() {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (ACAQNodeUI ui : nodeUIs.values()) {
            minX = Math.min(ui.getX(), minX);
            minY = Math.min(ui.getY(), minY);
        }
        for (ACAQNodeUI ui : nodeUIs.values()) {
            ui.setLocation(ui.getX() - minX + ACAQNodeUI.SLOT_UI_WIDTH,
                    ui.getY() - minY + ACAQNodeUI.SLOT_UI_HEIGHT);
        }
        cursor = new Point(ACAQNodeUI.SLOT_UI_WIDTH, ACAQNodeUI.SLOT_UI_HEIGHT);
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
     * Removes all UIs and adds them back in
     */
    public void fullRedraw() {
        removeAllNodes();
        addNewNodes();
    }

    public List<AlgorithmUIAction> getContextActions() {
        return contextActions;
    }

    public void setContextActions(List<AlgorithmUIAction> contextActions) {
        this.contextActions = contextActions;
    }

    public <T extends ACAQGraphNode> Set<ACAQNodeUI> getNodeUIsFor(Set<T> nodes) {
        Set<ACAQNodeUI> uis = new HashSet<>();
        for (T node : nodes) {
            ACAQNodeUI ui = nodeUIs.getOrDefault(node, null);
            if (ui != null) {
                uis.add(ui);
            }
        }
        return uis;
    }

    public ACAQGraphHistory getGraphHistory() {
        return graphHistory;
    }

    /**
     * The direction how a canvas renders the nodes
     */
    public enum ViewMode {
        Horizontal,
        Vertical
    }

}
