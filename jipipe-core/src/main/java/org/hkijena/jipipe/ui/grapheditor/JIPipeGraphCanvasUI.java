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

package org.hkijena.jipipe.ui.grapheditor;

import com.google.common.collect.*;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.algorithm.JIPipeGraph;
import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.events.GraphChangedEvent;
import org.hkijena.jipipe.api.events.NodeConnectedEvent;
import org.hkijena.jipipe.api.history.JIPipeGraphHistory;
import org.hkijena.jipipe.api.history.MoveNodesGraphHistorySnapshot;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.events.*;
import org.hkijena.jipipe.ui.grapheditor.connections.RectangularLineDrawer;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.layout.MSTGraphAutoLayoutMethod;
import org.hkijena.jipipe.ui.grapheditor.layout.SugiyamaGraphAutoLayoutMethod;
import org.hkijena.jipipe.utils.PointRange;
import org.hkijena.jipipe.utils.ScreenImage;
import org.hkijena.jipipe.utils.ScreenImageSVG;
import org.hkijena.jipipe.utils.UIUtils;
import org.jfree.graphics2d.svg.SVGGraphics2D;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UI that displays an {@link JIPipeGraph}
 */
public class JIPipeGraphCanvasUI extends JIPipeWorkbenchPanel implements MouseMotionListener, MouseListener {
    private final ImageIcon cursorImage = UIUtils.getIconFromResources("target.png");
    private final JIPipeGraph graph;
    private final BiMap<JIPipeGraphNode, JIPipeNodeUI> nodeUIs = HashBiMap.create();
    private final Set<JIPipeNodeUI> selection = new HashSet<>();
    private final EventBus eventBus = new EventBus();
    private final String compartment;
    private final JIPipeGraphHistory graphHistory = new JIPipeGraphHistory();
    private boolean layoutHelperEnabled;
    private JIPipeGraphViewMode viewMode = GraphEditorUISettings.getInstance().getDefaultViewMode();
    private JIPipeGraphDragAndDropBehavior dragAndDropBehavior;
    private Point cursor;
    private long lastTimeExpandedNegative = 0;
    private List<NodeUIContextAction> contextActions = new ArrayList<>();
    private MoveNodesGraphHistorySnapshot currentlyDraggedSnapshot;
    private Map<JIPipeNodeUI, Point> currentlyDraggedOffsets = new HashMap<>();
    private JIPipeDataSlotUI currentConnectionDragSource;
    private JIPipeDataSlotUI currentConnectionDragTarget;
    private JIPipeDataSlotUI currentHighlightedForDisconnect;

    /**
     * Used to store the minimum dimensions of the canvas to reduce user disruption
     */
    private Dimension minDimensions = null;

    /**
     * Creates a new UI
     *
     * @param workbench   the workbench
     * @param graph       The algorithm graph
     * @param compartment The compartment to show
     */
    public JIPipeGraphCanvasUI(JIPipeWorkbench workbench, JIPipeGraph graph, String compartment) {
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
    public JIPipeGraph getGraph() {
        return graph;
    }

    /**
     * Removes all node UIs
     */
    private void removeAllNodes() {
        for (JIPipeNodeUI ui : ImmutableList.copyOf(nodeUIs.values())) {
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
        Set<JIPipeGraphNode> toRemove = new HashSet<>();
        for (Map.Entry<JIPipeGraphNode, JIPipeNodeUI> kv : nodeUIs.entrySet()) {
            if (!graph.containsNode(kv.getKey()) || !kv.getKey().isVisibleIn(compartment))
                toRemove.add(kv.getKey());
        }
        for (JIPipeGraphNode algorithm : toRemove) {
            JIPipeNodeUI ui = nodeUIs.get(algorithm);
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
        JIPipeNodeUI ui = null;
        for (JIPipeGraphNode algorithm : graph.traverseAlgorithms()) {
            if (!algorithm.isVisibleIn(compartment))
                continue;
            if (nodeUIs.containsKey(algorithm))
                continue;

            switch (viewMode) {
                case Horizontal:
                    ui = new JIPipeHorizontalNodeUI(getWorkbench(), this, algorithm);
                    break;
                case Vertical:
                    ui = new JIPipeVerticalNodeUI(getWorkbench(), this, algorithm);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown view mode!");
            }

            ui.getEventBus().register(this);
            add(ui);
            nodeUIs.put(algorithm, ui);
            Point location = algorithm.getLocationWithin(compartment, viewMode.toString());
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
        List<JIPipeGraphNode> traversed = graph.traverseAlgorithms();
        boolean detected = false;
        for (int i = traversed.size() - 1; i >= 0; --i) {
            JIPipeGraphNode algorithm = traversed.get(i);
            if (!algorithm.isVisibleIn(compartment))
                continue;
            JIPipeNodeUI ui = nodeUIs.getOrDefault(algorithm, null);
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
//        for (JIPipeAlgorithm algorithm : ImmutableList.copyOf(nodeUIs.keySet())) {
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
        switch (GraphEditorUISettings.getInstance().getAutoLayout()) {
            case Sugiyama:
                (new SugiyamaGraphAutoLayoutMethod()).accept(this);
                break;
            case MST:
                (new MSTGraphAutoLayoutMethod()).accept(this);
                break;
        }
        repaint();
    }

    /**
     * Triggers when an {@link JIPipeNodeUI} requests an action.
     * Passes the action to its own event bus
     *
     * @param event event
     */
    @Subscribe
    public void onActionRequested(AlgorithmUIActionRequestedEvent event) {
        if (JIPipeNodeUI.REQUEST_OPEN_CONTEXT_MENU.equals(event.getAction())) {
            if (event.getUi() != null) {
                openContextMenu(getMousePosition());
            }
        }
        getEventBus().post(event);
    }

    private void autoPlaceCloseToCursor(JIPipeNodeUI ui) {
        int minX = 0;
        int minY = 0;
        if (cursor != null) {
            minX = cursor.x;
            minY = cursor.y;
        }

        Set<Rectangle> otherShapes = new HashSet<>();
        for (JIPipeNodeUI otherUi : nodeUIs.values()) {
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
            if (viewMode == JIPipeGraphViewMode.Horizontal) {
                currentShape.y += viewMode.getGridHeight();
            } else {
                currentShape.x += viewMode.getGridWidth();
            }
        }
        while (!found);

        ui.trySetLocationAtNextGridPoint(currentShape.x, currentShape.y);

    }

    private void autoPlaceTargetAdjacent(JIPipeNodeUI sourceAlgorithmUI, JIPipeDataSlot source, JIPipeNodeUI targetAlgorithmUI, JIPipeDataSlot target) {
        int sourceSlotIndex = source.getNode().getOutputSlots().indexOf(source);
        int targetSlotIndex = target.getNode().getInputSlots().indexOf(target);
        if (sourceSlotIndex < 0 || targetSlotIndex < 0) {
            autoPlaceCloseToCursor(targetAlgorithmUI);
            return;
        }

        if (viewMode == JIPipeGraphViewMode.Horizontal) {
            int sourceSlotInternalY = sourceSlotIndex * viewMode.getGridHeight();
            int targetSlotInternalY = targetSlotIndex * viewMode.getGridHeight();

            int minX = sourceAlgorithmUI.getWidth() + sourceAlgorithmUI.getX() + viewMode.getGridWidth() * 2;
            int targetY = sourceAlgorithmUI.getY() + sourceSlotInternalY - targetSlotInternalY;

            int x = (int) (minX * 1.0 / viewMode.getGridWidth()) * viewMode.getGridWidth();
            int y = (int) (targetY * 1.0 / viewMode.getGridHeight()) * viewMode.getGridHeight();
            if (!targetAlgorithmUI.trySetLocationNoGrid(x, y)) {
                autoPlaceCloseToCursor(targetAlgorithmUI);
            }
        } else {
            int x = sourceAlgorithmUI.getSlotLocation(source).center.x + sourceAlgorithmUI.getX();
            x -= targetAlgorithmUI.getSlotLocation(target).center.x;
            int y = sourceAlgorithmUI.getBottomY() + viewMode.getGridHeight();
            if (!targetAlgorithmUI.trySetLocationNoGrid(x, y)) {
                autoPlaceCloseToCursor(targetAlgorithmUI);
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        if (!currentlyDraggedOffsets.isEmpty()) {
            for (Map.Entry<JIPipeNodeUI, Point> entry : currentlyDraggedOffsets.entrySet()) {
                JIPipeNodeUI currentlyDragged = entry.getKey();
                Point currentlyDraggedOffset = entry.getValue();

                int xu = currentlyDraggedOffset.x + mouseEvent.getX();
                int yu = currentlyDraggedOffset.y + mouseEvent.getY();
                if (xu < 0 || yu < 0) {
                    long currentTimeMillis = System.currentTimeMillis();
                    int ex = xu < 0 ? viewMode.getGridWidth() : 0;
                    int ey = yu < 0 ? viewMode.getGridHeight() : 0;
                    if (currentTimeMillis - lastTimeExpandedNegative > 100) {
                        for (JIPipeNodeUI value : nodeUIs.values()) {
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
                    if (!Objects.equals(currentlyDragged.getLocation(), viewMode.getNextGridPoint(new Point(x, y)))) {
                        graphHistory.addSnapshotBefore(currentlyDraggedSnapshot);
                        currentlyDraggedSnapshot = null;
                    }
                }

                currentlyDragged.trySetLocationAtNextGridPoint(x, y);
            }
            repaint();
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
        int ex = left ? viewMode.getGridWidth() : 0;
        int ey = top ? viewMode.getGridHeight() : 0;
        for (JIPipeNodeUI value : nodeUIs.values()) {
            if (!currentlyDraggedOffsets.containsKey(value)) {
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
            JIPipeNodeUI ui = pickComponent(mouseEvent);
            if (ui != null)
                eventBus.post(new DefaultAlgorithmUIActionRequestedEvent(ui));
        } else if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
            cursor = new Point(mouseEvent.getX(), mouseEvent.getY());
            requestFocusInWindow();
            repaint();
        } else if (SwingUtilities.isRightMouseButton(mouseEvent)) {
            if (selection.isEmpty()) {
                JIPipeNodeUI ui = pickComponent(mouseEvent);
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
        for (NodeUIContextAction action : contextActions) {
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
            if (currentlyDraggedOffsets.isEmpty()) {
                JIPipeNodeUI ui = pickComponent(mouseEvent);
                if (ui != null) {
                    if (mouseEvent.isShiftDown()) {
                        addToSelection(ui);
                    } else {
                        if (selection.isEmpty() || selection.size() == 1) {
                            selectOnly(ui);
                        } else {
                            if (!selection.contains(ui)) {
                                selectOnly(ui);
                            }
                        }
                    }
                    for (JIPipeNodeUI nodeUI : selection) {
                        Point offset = new Point();
                        offset.x = nodeUI.getX() - mouseEvent.getX();
                        offset.y = nodeUI.getY() - mouseEvent.getY();
                        currentlyDraggedOffsets.put(nodeUI, offset);
                        currentlyDraggedSnapshot = new MoveNodesGraphHistorySnapshot(graph, "Move node");
                    }

                }
            }
        }
    }

    private JIPipeNodeUI pickComponent(MouseEvent mouseEvent) {
        for (int i = 0; i < getComponentCount(); ++i) {
            Component component = getComponent(i);
            if (component.getBounds().contains(mouseEvent.getX(), mouseEvent.getY())) {
                if (component instanceof JIPipeNodeUI) {
                    return (JIPipeNodeUI) component;
                }
            }
        }
        return null;
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
        currentlyDraggedOffsets.clear();
        JIPipeNodeUI ui = pickComponent(mouseEvent);
        if (ui == null) {
            selectOnly(null);
        }
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {

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
    public void onAlgorithmGraphChanged(GraphChangedEvent event) {
        // Update the location of existing nodes
        for (JIPipeNodeUI ui : nodeUIs.values()) {
            Point point = ui.getNode().getLocationWithin(compartment, viewMode.name());
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
    public void onAlgorithmConnected(NodeConnectedEvent event) {
        JIPipeNodeUI sourceNode = nodeUIs.getOrDefault(event.getSource().getNode(), null);
        JIPipeNodeUI targetNode = nodeUIs.getOrDefault(event.getTarget().getNode(), null);

        // Check if we actually need to auto-place
        if (viewMode == JIPipeGraphViewMode.Horizontal) {
            if (sourceNode != null && targetNode != null && targetNode.getX() >= sourceNode.getRightX() + viewMode.getGridWidth()) {
                return;
            }
        } else if (viewMode == JIPipeGraphViewMode.Vertical) {
            if (sourceNode != null && targetNode != null && targetNode.getY() >= sourceNode.getBottomY() + viewMode.getGridHeight()) {
                return;
            }
        }

        if (sourceNode != null && targetNode != null && layoutHelperEnabled) {
            Point cursorBackup = cursor;
            try {
                if (viewMode == JIPipeGraphViewMode.Horizontal)
                    this.cursor = new Point(targetNode.getRightX() + 4 * viewMode.getGridWidth(),
                            targetNode.getY());
                else
                    this.cursor = new Point(targetNode.getX(), targetNode.getBottomY() + 4 * viewMode.getGridHeight());
                autoPlaceTargetAdjacent(sourceNode, event.getSource(), targetNode, event.getTarget());
            } finally {
                this.cursor = cursorBackup;
            }
        }
    }


    /**
     * Should be triggered when an {@link JIPipeNodeUI} requests that the algorithm settings should be opened
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

        RectangularLineDrawer drawer = new RectangularLineDrawer();

        g.setStroke(new BasicStroke(2));
        graphics.setColor(Color.LIGHT_GRAY);
        if(GraphEditorUISettings.getInstance().isDrawOutsideEdges())
            paintOutsideEdges(g, drawer, false);
        paintEdges(graphics, g, drawer, false);

        g.setStroke(new BasicStroke(7));
        if(GraphEditorUISettings.getInstance().isDrawOutsideEdges())
            paintOutsideEdges(g, drawer, true);
        paintEdges(graphics, g, drawer, true);

        // Draw selections
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0));
        for (JIPipeNodeUI ui : selection) {
            Rectangle bounds = ui.getBounds();
            bounds.x -= 4;
            bounds.y -= 4;
            bounds.width += 8;
            bounds.height += 8;
            g.setColor(ui.getBorderColor());
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        // Draw currently dragged connection
        if (currentConnectionDragSource != null) {
            g.setStroke(new BasicStroke(7));
            graphics.setColor(new Color(0, 128, 0));
            PointRange sourcePoint;
            PointRange targetPoint;

            sourcePoint = currentConnectionDragSource.getNodeUI().getSlotLocation(currentConnectionDragSource.getSlot());
            sourcePoint.add(currentConnectionDragSource.getNodeUI().getLocation());

            if (currentConnectionDragTarget == null || currentConnectionDragTarget == currentConnectionDragSource ||
                    currentConnectionDragTarget.getNodeUI().getNode() == currentConnectionDragSource.getNodeUI().getNode()) {
                targetPoint = new PointRange(getMousePosition().x, getMousePosition().y);
            } else {
                targetPoint = currentConnectionDragTarget.getNodeUI().getSlotLocation(currentConnectionDragTarget.getSlot());
                targetPoint.add(currentConnectionDragTarget.getNodeUI().getLocation());
            }

            // Tighten the point ranges: Bringing the centers together
            PointRange.tighten(sourcePoint, targetPoint);

            // Draw arrow
            if (currentConnectionDragSource.getSlot().isOutput())
                drawEdge(g, sourcePoint.center, currentConnectionDragSource.getNodeUI().getBounds(), targetPoint.center, drawer);
            else
                drawEdge(g, targetPoint.center, currentConnectionDragSource.getNodeUI().getBounds(), sourcePoint.center, drawer);
        }

        if (currentHighlightedForDisconnect != null) {
            g.setStroke(new BasicStroke(7));
            g.setColor(Color.RED);
            if (currentHighlightedForDisconnect.getSlot().isInput()) {
                JIPipeDataSlot source = getGraph().getSourceSlot(currentHighlightedForDisconnect.getSlot());
                if (source != null) {
                    JIPipeDataSlot target = currentHighlightedForDisconnect.getSlot();
                    JIPipeNodeUI sourceUI = nodeUIs.getOrDefault(source.getNode(), null);
                    JIPipeNodeUI targetUI = nodeUIs.getOrDefault(target.getNode(), null);

                    if (sourceUI != null && targetUI != null) {
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
                }
            } else if (currentHighlightedForDisconnect.getSlot().isOutput()) {
                JIPipeDataSlot source = currentHighlightedForDisconnect.getSlot();
                for (JIPipeDataSlot target : getGraph().getTargetSlots(source)) {
                    JIPipeNodeUI sourceUI = nodeUIs.getOrDefault(source.getNode(), null);
                    JIPipeNodeUI targetUI = nodeUIs.getOrDefault(target.getNode(), null);

                    if (sourceUI != null && targetUI != null) {
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
                }
            }
        }

        g.setStroke(new BasicStroke(1));

        if (cursor != null) {
            g.drawImage(cursorImage.getImage(),
                    cursor.x - cursorImage.getIconWidth() / 2,
                    cursor.y - cursorImage.getIconHeight() / 2,
                    null);
        }
    }

    private void paintEdges(Graphics graphics, Graphics2D g, RectangularLineDrawer drawer, boolean onlySelected) {
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> kv : graph.getSlotEdges()) {
            JIPipeDataSlot source = kv.getKey();
            JIPipeDataSlot target = kv.getValue();
            JIPipeNodeUI sourceUI = nodeUIs.getOrDefault(source.getNode(), null);
            JIPipeNodeUI targetUI = nodeUIs.getOrDefault(target.getNode(), null);

            if (sourceUI == null || targetUI == null)
                continue;
            if (onlySelected) {
                if (!selection.contains(sourceUI) && !selection.contains(targetUI))
                    continue;
            } else {
                if (selection.contains(sourceUI) || selection.contains(targetUI))
                    continue;
            }
            if (JIPipeDatatypeRegistry.isTriviallyConvertible(source.getAcceptedDataType(), target.getAcceptedDataType()))
                graphics.setColor(Color.DARK_GRAY);
            else if (JIPipeDatatypeRegistry.getInstance().isConvertible(source.getAcceptedDataType(), target.getAcceptedDataType()))
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
    }

    private void paintOutsideEdges(Graphics2D g, RectangularLineDrawer drawer, boolean onlySelected) {
        for (JIPipeNodeUI ui : nodeUIs.values()) {
            if (!ui.getNode().getVisibleCompartments().isEmpty()) {
                if (onlySelected) {
                    if (!selection.contains(ui))
                        continue;
                } else {
                    if (selection.contains(ui))
                        continue;
                }
                Point sourcePoint = new Point();
                Point targetPoint = new Point();
                if (viewMode == JIPipeGraphViewMode.Horizontal) {
                    if (compartment == null || compartment.equals(ui.getNode().getCompartment())) {
                        sourcePoint.x = ui.getX() + ui.getWidth();
                        sourcePoint.y = ui.getY() + viewMode.getGridHeight() / 2;
                        targetPoint.x = getWidth();
                        targetPoint.y = sourcePoint.y;
                    } else {
                        sourcePoint.x = 0;
                        sourcePoint.y = ui.getY() + viewMode.getGridHeight() / 2;
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
    }

    /**
     * Returns the UI location of a data slot that is located in the graph
     *
     * @param slot the data slot
     * @return the center slot location. Null if the algorithm has no UI or the returned location is null
     */
    public Point getSlotLocation(JIPipeDataSlot slot) {
        JIPipeNodeUI algorithmUI = nodeUIs.getOrDefault(slot.getNode(), null);
        if (algorithmUI != null) {
            PointRange location = algorithmUI.getSlotLocation(slot);
            if (location != null) {
                return new Point(algorithmUI.getX() + location.center.x, algorithmUI.getY() + location.center.y);
            }
        }
        return null;
    }

    private void drawOutsideEdge(Graphics2D g, Point sourcePoint, Point targetPoint, RectangularLineDrawer drawer) {
        int sourceA;
        int targetA;
        int sourceB;

        if (viewMode == JIPipeGraphViewMode.Horizontal) {
            sourceA = sourcePoint.x;
            targetA = targetPoint.x;
            sourceB = sourcePoint.y;
        } else {
            sourceA = sourcePoint.y;
            targetA = targetPoint.y;
            sourceB = sourcePoint.x;
        }

        drawer.start(sourceA, sourceB);
        drawer.moveToMajor(targetA);
        drawer.drawCurrentSegment(g, viewMode);
    }

    private void drawEdge(Graphics2D g, Point sourcePoint, Rectangle sourceBounds, Point targetPoint, RectangularLineDrawer drawer) {
        int buffer;
        int sourceA;
        int targetA;
        int sourceB;
        int targetB;
        int componentStartB;
        int componentEndB;

        if (viewMode == JIPipeGraphViewMode.Horizontal) {
            buffer = viewMode.getGridWidth();
            sourceA = sourcePoint.x;
            targetA = targetPoint.x;
            sourceB = sourcePoint.y;
            targetB = targetPoint.y;
            componentStartB = sourceBounds.y;
            componentEndB = sourceBounds.y + sourceBounds.height;
        } else {
            buffer = viewMode.getGridHeight() / 2;
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
            drawer.addToMajor(buffer);

            // Go left or right
            if (targetB <= drawer.getLastSegment().b1) {
                drawer.moveToMinor(Math.max(0, componentStartB - buffer));
            } else {
                drawer.moveToMinor(componentEndB + buffer);
            }

            // Go to target height
            drawer.moveToMajor(Math.max(0, targetA - buffer));
        } else if (sourceB != targetB) {
            // Add some space in major direction
            int dA = targetA - sourceA;
            drawer.moveToMajor(Math.min(sourceA + buffer, sourceA + dA / 2));
        }

        // Target point X is shifted
        if (drawer.getLastSegment().b1 != targetB) {
            drawer.moveToMinor(targetB);
        }

        // Go to end point
        drawer.moveToMajor(targetA);
        drawer.drawCurrentSegment(g, viewMode);
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

    public JIPipeGraphViewMode getViewMode() {
        return viewMode;
    }

    public void setViewMode(JIPipeGraphViewMode viewMode) {
        if (viewMode != this.viewMode) {
            this.viewMode = viewMode;
            removeAllNodes();
            addNewNodes();
        }
    }

    public BiMap<JIPipeGraphNode, JIPipeNodeUI> getNodeUIs() {
        return ImmutableBiMap.copyOf(nodeUIs);
    }

    public JIPipeGraphDragAndDropBehavior getDragAndDropBehavior() {
        return dragAndDropBehavior;
    }

    public void setDragAndDropBehavior(JIPipeGraphDragAndDropBehavior dragAndDropBehavior) {
        this.dragAndDropBehavior = dragAndDropBehavior;
        dragAndDropBehavior.setCanvas(this);
        new DropTarget(this, dragAndDropBehavior);
    }

    /**
     * @return the list of selected {@link JIPipeNodeUI}
     */
    public Set<JIPipeNodeUI> getSelection() {
        return Collections.unmodifiableSet(selection);
    }

    /**
     * @return the list of selected nodes
     */
    public Set<JIPipeGraphNode> getSelectedNodes() {
        return selection.stream().map(JIPipeNodeUI::getNode).collect(Collectors.toSet());
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
    public void selectOnly(JIPipeNodeUI ui) {
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
    public void removeFromSelection(JIPipeNodeUI ui) {
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
    public void addToSelection(JIPipeNodeUI ui) {
        selection.add(ui);
        updateSelection();
    }

    /**
     * Sets node positions to make the top left to 0, 0
     */
    public void crop() {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (JIPipeNodeUI ui : nodeUIs.values()) {
            minX = Math.min(ui.getX(), minX);
            minY = Math.min(ui.getY(), minY);
        }
        for (JIPipeNodeUI ui : nodeUIs.values()) {
            ui.setLocation(ui.getX() - minX + viewMode.getGridWidth(),
                    ui.getY() - minY + viewMode.getGridHeight());
        }
        cursor = viewMode.getGridPoint(new Point(1, 1));
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

    public List<NodeUIContextAction> getContextActions() {
        return contextActions;
    }

    public void setContextActions(List<NodeUIContextAction> contextActions) {
        this.contextActions = contextActions;
    }

    public <T extends JIPipeGraphNode> Set<JIPipeNodeUI> getNodeUIsFor(Set<T> nodes) {
        Set<JIPipeNodeUI> uis = new HashSet<>();
        for (T node : nodes) {
            JIPipeNodeUI ui = nodeUIs.getOrDefault(node, null);
            if (ui != null) {
                uis.add(ui);
            }
        }
        return uis;
    }

    public JIPipeGraphHistory getGraphHistory() {
        return graphHistory;
    }

    public JIPipeDataSlotUI getCurrentConnectionDragSource() {
        return currentConnectionDragSource;
    }

    public void setCurrentConnectionDragSource(JIPipeDataSlotUI currentConnectionDragSource) {
        this.currentConnectionDragSource = currentConnectionDragSource;
    }

    public JIPipeDataSlotUI getCurrentConnectionDragTarget() {
        return currentConnectionDragTarget;
    }

    public void setCurrentConnectionDragTarget(JIPipeDataSlotUI currentConnectionDragTarget) {
        this.currentConnectionDragTarget = currentConnectionDragTarget;
    }

    public JIPipeDataSlotUI getCurrentHighlightedForDisconnect() {
        return currentHighlightedForDisconnect;
    }

    public void setCurrentHighlightedForDisconnect(JIPipeDataSlotUI currentHighlightedForDisconnect) {
        this.currentHighlightedForDisconnect = currentHighlightedForDisconnect;
    }

}
