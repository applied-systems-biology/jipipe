package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQAlgorithmGraphEdge;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.events.AlgorithmGraphConnectedEvent;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.ui.components.PickAlgorithmDialog;
import org.hkijena.acaq5.ui.events.AlgorithmSelectedEvent;
import org.hkijena.acaq5.ui.events.DefaultUIActionRequestedEvent;
import org.hkijena.acaq5.utils.ScreenImage;
import org.hkijena.acaq5.utils.UIUtils;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

/**
 * UI that displays an {@link ACAQAlgorithmGraph}
 */
public class ACAQAlgorithmGraphCanvasUI extends JPanel implements MouseMotionListener, MouseListener {
    private ACAQAlgorithmGraph algorithmGraph;
    private ACAQAlgorithmUI currentlyDragged;
    private Point currentlyDraggedOffset = new Point();
    private BiMap<ACAQAlgorithm, ACAQAlgorithmUI> nodeUIs = HashBiMap.create();
    private EventBus eventBus = new EventBus();
    private int newEntryLocationX = ACAQAlgorithmUI.SLOT_UI_WIDTH * 4;
    private boolean layoutHelperEnabled;
    private String compartment;
    private JPopupMenu contextMenu;
    private Direction currentDirection = Direction.Horizontal;

    /**
     * Creates a new UI
     *
     * @param algorithmGraph The algorithm graph
     * @param compartment    The compartment to show
     */
    public ACAQAlgorithmGraphCanvasUI(ACAQAlgorithmGraph algorithmGraph, String compartment) {
        super(null);
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

    private void moveNodeHere() {
        Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(mouseLocation, this);
        ACAQAlgorithm algorithm = PickAlgorithmDialog.showDialog(this, nodeUIs.keySet(), "Move node");
        if (algorithm != null) {
            ACAQAlgorithmUI ui = nodeUIs.getOrDefault(algorithm, null);
            if (ui != null) {
                ui.trySetLocationInGrid(mouseLocation.x, mouseLocation.y);
                repaint();
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
     * Removes node UIs that are not valid anymore
     */
    private void removeOldNodes() {
        Set<ACAQAlgorithm> toRemove = new HashSet<>();
        for (Map.Entry<ACAQAlgorithm, ACAQAlgorithmUI> kv : nodeUIs.entrySet()) {
            if (!algorithmGraph.containsNode(kv.getKey()) || !kv.getKey().isVisibleIn(compartment))
                toRemove.add(kv.getKey());
        }
        for (ACAQAlgorithm algorithm : toRemove) {
            ACAQAlgorithmUI ui = nodeUIs.get(algorithm);
            remove(ui);
            nodeUIs.remove(algorithm);
        }
        if (!toRemove.isEmpty()) {
            revalidate();
            repaint();
        }
    }

    /**
     * Adds node UIs that are not in the canvas yet
     */
    private void addNewNodes() {
        for (ACAQAlgorithm algorithm : algorithmGraph.traverseAlgorithms()) {
            if (!algorithm.isVisibleIn(compartment))
                continue;
            if (nodeUIs.containsKey(algorithm))
                continue;

            ACAQAlgorithmUI ui = new ACAQAlgorithmUI(this, algorithm, currentDirection);
            ui.getEventBus().register(this);
            add(ui);
            nodeUIs.put(algorithm, ui);
            Point location = algorithm.getLocationWithin(compartment, currentDirection.toString());
            if (location == null || !ui.trySetLocationNoGrid(location.x, location.y)) {
                autoPlaceAlgorithm(ui);
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
    }

    /**
     * Detects components overlapping each other.
     * Auto-places components when an overlap was detected
     */
    private void removeComponentOverlaps() {
        List<ACAQAlgorithm> traversed = algorithmGraph.traverseAlgorithms();
        for (int i = traversed.size() - 1; i >= 0; --i) {
            ACAQAlgorithm algorithm = traversed.get(i);
            if (!algorithm.isVisibleIn(compartment))
                continue;
            ACAQAlgorithmUI ui = nodeUIs.getOrDefault(algorithm, null);
            if (ui != null) {
                if (ui.isOverlapping()) {
                    autoPlaceAlgorithm(ui);
                }
            }
        }
    }

    /**
     * Auto-layouts all UIs
     */
    public void autoLayoutAll() {
//        int backup = newEntryLocationX;
//        newEntryLocationX = 0;
//        for (ACAQAlgorithm algorithm : ImmutableList.copyOf(nodeUIs.keySet())) {
//            algorithm.setLocationWithin(compartment, null);
//            remove(nodeUIs.get(algorithm));
//        }
//        nodeUIs.clear();
//        addNewNodes();
//        newEntryLocationX = backup;
        autoLayoutSugiyama();
    }

    private void autoPlaceAlgorithm(ACAQAlgorithmUI ui) {
        ACAQAlgorithm targetAlgorithm = ui.getAlgorithm();

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

        if (ui.getX() < minX || ui.isOverlapping()) {
            if (!ui.trySetLocationNoGrid(minX, minY)) {
                int y = nodeUIs.values().stream().map(ACAQAlgorithmUI::getBottomY).max(Integer::compareTo).orElse(0);
                if (y == 0)
                    y += 2 * ACAQAlgorithmUI.SLOT_UI_HEIGHT;
                else
                    y += ACAQAlgorithmUI.SLOT_UI_HEIGHT;

                ui.trySetLocationNoGrid(minX, y);
            }
        }
    }

    private void autoPlaceTargetAdjacent(ACAQAlgorithmUI sourceAlgorithmUI, ACAQDataSlot source, ACAQAlgorithmUI targetAlgorithmUI, ACAQDataSlot target) {
        int sourceSlotIndex = source.getAlgorithm().getOutputSlots().indexOf(source);
        int targetSlotIndex = target.getAlgorithm().getInputSlots().indexOf(target);
        if (sourceSlotIndex < 0 || targetSlotIndex < 0) {
            autoPlaceAlgorithm(targetAlgorithmUI);
            return;
        }

        int sourceSlotInternalY = sourceSlotIndex * ACAQAlgorithmUI.SLOT_UI_HEIGHT;
        int targetSlotInternalY = targetSlotIndex * ACAQAlgorithmUI.SLOT_UI_HEIGHT;

        int minX = sourceAlgorithmUI.getWidth() + sourceAlgorithmUI.getX() + ACAQAlgorithmUI.SLOT_UI_WIDTH * 2;
        int targetY = sourceAlgorithmUI.getY() + sourceSlotInternalY - targetSlotInternalY;

        int x = (int) (minX * 1.0 / ACAQAlgorithmUI.SLOT_UI_WIDTH) * ACAQAlgorithmUI.SLOT_UI_WIDTH;
        int y = (int) (targetY * 1.0 / ACAQAlgorithmUI.SLOT_UI_HEIGHT) * ACAQAlgorithmUI.SLOT_UI_HEIGHT;
        if (!targetAlgorithmUI.trySetLocationNoGrid(x, y)) {
            autoPlaceAlgorithm(targetAlgorithmUI);
        }
    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        if (currentlyDragged != null) {
            int x = Math.max(0, currentlyDraggedOffset.x + mouseEvent.getX());
            int y = Math.max(0, currentlyDraggedOffset.y + mouseEvent.getY());
            currentlyDragged.trySetLocationInGrid(x, y);
            repaint();
//            updateEdgeUI();
            if (getParent() != null)
                getParent().revalidate();
        }
    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        if (SwingUtilities.isLeftMouseButton(mouseEvent) && mouseEvent.getClickCount() == 2) {
            ACAQAlgorithmUI ui = pickComponent(mouseEvent);
            if (ui != null)
                eventBus.post(new DefaultUIActionRequestedEvent(ui));
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
            autoPlaceTargetAdjacent(sourceNode, event.getSource(), targetNode, event.getTarget());
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
        RenderingHints rh = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHints(rh);

        // Draw the edges to outside compartments
        // The edges should be at the center input point of the node
        // They are not covered by the other method below
        g.setStroke(new BasicStroke(2));
        graphics.setColor(Color.LIGHT_GRAY);
        for (ACAQAlgorithmUI ui : nodeUIs.values()) {
            if (!ui.getAlgorithm().getVisibleCompartments().isEmpty()) {
                Point sourcePoint = new Point();
                Point targetPoint = new Point();
                if (compartment.equals(ui.getAlgorithm().getCompartment())) {
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
                drawEdge(g, sourcePoint, targetPoint);
            }
        }

        // Draw edges between loaded algorithm UIs
        graphics.setColor(Color.DARK_GRAY);
        for (Map.Entry<ACAQDataSlot, ACAQDataSlot> kv : algorithmGraph.getSlotEdges()) {
            ACAQDataSlot source = kv.getKey();
            ACAQDataSlot target = kv.getValue();
            ACAQAlgorithmUI sourceUI = nodeUIs.getOrDefault(source.getAlgorithm(), null);
            ACAQAlgorithmUI targetUI = nodeUIs.getOrDefault(target.getAlgorithm(), null);

            if (sourceUI == null && targetUI == null)
                continue;
            if (ACAQDatatypeRegistry.isTriviallyConvertible(source.getAcceptedDataType(), target.getAcceptedDataType()))
                graphics.setColor(Color.DARK_GRAY);
            else if (ACAQDatatypeRegistry.getInstance().isConvertible(source.getAcceptedDataType(), target.getAcceptedDataType()))
                graphics.setColor(Color.BLUE);
            else
                graphics.setColor(Color.RED);

            Point sourcePoint = new Point();
            Point targetPoint = new Point();

            if (sourceUI != null) {
                sourcePoint = sourceUI.getSlotLocation(source);
                sourcePoint.x += sourceUI.getX();
                sourcePoint.y += sourceUI.getY();
            }

            if (targetUI != null) {
                targetPoint = targetUI.getSlotLocation(target);
                targetPoint.x += targetUI.getX();
                targetPoint.y += targetUI.getY();
            }

            if (sourceUI == null) {
                sourcePoint.x = 0;
                sourcePoint.y = targetPoint.y;
            }
            if (targetUI == null) {
                targetPoint.x = getWidth();
                targetPoint.y = sourcePoint.y;
            }

            // Draw arrow
            drawEdge(g, sourcePoint, targetPoint);
        }

        g.setStroke(new BasicStroke(1));
    }

    private void drawEdge(Graphics2D g, Point sourcePoint, Point targetPoint) {
        Path2D.Float path = new Path2D.Float();
        path.moveTo(sourcePoint.x, sourcePoint.y);
        float dx = targetPoint.x - sourcePoint.x;
        path.curveTo(sourcePoint.x + 0.4f * dx, sourcePoint.y,
                sourcePoint.x + 0.6f * dx, targetPoint.y,
                targetPoint.x, targetPoint.y);
        g.draw(path);
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
    public BufferedImage createScreenshot() {
        return ScreenImage.createImage(this);
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
     * The direction how a canvas renders the nodes
     */
    public enum Direction {
        Horizontal,
        Vertical
    }
}
