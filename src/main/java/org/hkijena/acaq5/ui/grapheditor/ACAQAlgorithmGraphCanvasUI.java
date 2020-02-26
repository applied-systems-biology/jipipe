package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.events.AlgorithmGraphConnectedEvent;
import org.hkijena.acaq5.ui.events.OpenSettingsUIRequestedEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Path2D;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ACAQAlgorithmGraphCanvasUI extends JPanel implements MouseMotionListener, MouseListener {
    private ACAQAlgorithmGraph algorithmGraph;
    private ACAQAlgorithmUI currentlyDragged;
    private Point currentlyDraggedOffset = new Point();
    private BiMap<ACAQAlgorithm, ACAQAlgorithmUI> nodeUIs = HashBiMap.create();
    private EventBus eventBus = new EventBus();
    private int newEntryLocationX = ACAQAlgorithmUI.SLOT_UI_WIDTH * 4;
    private boolean layoutHelperEnabled;

    public ACAQAlgorithmGraphCanvasUI(ACAQAlgorithmGraph algorithmGraph) {
        super(null);
        this.algorithmGraph = algorithmGraph;
        initialize();
        addNewNodes();
        algorithmGraph.getEventBus().register(this);
    }

    private void initialize() {
        setBackground(Color.WHITE);
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public ACAQAlgorithmGraph getAlgorithmGraph() {
        return algorithmGraph;
    }

    /**
     * Removes node UIs that are not valid anymore
     */
    private void removeOldNodes() {
        Set<ACAQAlgorithm> toRemove = new HashSet<>();
        for(Map.Entry<ACAQAlgorithm, ACAQAlgorithmUI> kv : nodeUIs.entrySet()) {
            if(!algorithmGraph.containsNode(kv.getKey()))
                toRemove.add(kv.getKey());
        }
        for(ACAQAlgorithm algorithm : toRemove) {
            ACAQAlgorithmUI ui = nodeUIs.get(algorithm);
            remove(ui);
            nodeUIs.remove(algorithm);
        }
        if(!toRemove.isEmpty()) {
            revalidate();
            repaint();
        }
    }

    /**
     * Adds node UIs that are not in the canvas yet
     */
    private void addNewNodes() {
        for(ACAQAlgorithm algorithm : algorithmGraph.traverseAlgorithms()) {
            if(nodeUIs.containsKey(algorithm))
                continue;

            ACAQAlgorithmUI ui = new ACAQAlgorithmUI(this, algorithm);
            ui.getEventBus().register(this);
            add(ui);
            nodeUIs.put(algorithm, ui);
            if(algorithm.getLocation() == null || !ui.trySetLocationNoGrid(algorithm.getLocation().x, algorithm.getLocation().y)) {
                autoPlaceAlgorithm(ui);
            }
        }
        revalidate();
        repaint();
    }

    private void autoPlaceAlgorithm(ACAQAlgorithmUI ui) {
        ACAQAlgorithm targetAlgorithm = ui.getAlgorithm();

        // Find the source algorithm that is right-most
        ACAQAlgorithmUI rightMostSource = null;
        for (ACAQDataSlot<?> target : targetAlgorithm.getInputSlots()) {
            ACAQDataSlot<?> source = algorithmGraph.getSourceSlot(target);
            if(source != null) {
                ACAQAlgorithmUI sourceUI = nodeUIs.getOrDefault(source.getAlgorithm(), null);
                if(sourceUI != null) {
                    if(rightMostSource == null || sourceUI.getX() > rightMostSource.getX())
                        rightMostSource = sourceUI;
                }
            }
        }

        // Auto-place
        int minX = 4 * ACAQAlgorithmUI.SLOT_UI_WIDTH;
        if(rightMostSource != null) {
            minX += rightMostSource.getX() + rightMostSource.getWidth();
            minX += 4 * ACAQAlgorithmUI.SLOT_UI_WIDTH;
        }

        int minY = Math.max(ui.getY(), 2 * ACAQAlgorithmUI.SLOT_UI_HEIGHT);

        if(ui.getX() < minX) {
            if(!ui.trySetLocationNoGrid(minX, minY)) {
                int y = nodeUIs.values().stream().map(ACAQAlgorithmUI::getBottomY).max(Integer::compareTo).orElse(0);
                if(y == 0)
                    y += 2 * ACAQAlgorithmUI.SLOT_UI_HEIGHT;
                else
                    y += ACAQAlgorithmUI.SLOT_UI_HEIGHT;

                ui.trySetLocationNoGrid(minX, y);
            }
        }

    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        if(currentlyDragged != null) {
            int x = Math.max(0, currentlyDraggedOffset.x + mouseEvent.getX());
            int y = Math.max(0, currentlyDraggedOffset.y + mouseEvent.getY());
            currentlyDragged.trySetLocationInGrid(x, y);
            repaint();
//            updateEdgeUI();
            if(getParent() != null)
                getParent().revalidate();
        }
    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {

    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        if(SwingUtilities.isLeftMouseButton(mouseEvent)) {
            for(int i = 0; i < getComponentCount(); ++i) {
                Component component = getComponent(i);
                if(component.getBounds().contains(mouseEvent.getX(), mouseEvent.getY())) {
                    if(component instanceof ACAQAlgorithmUI) {
                        currentlyDragged = (ACAQAlgorithmUI)component;
                        currentlyDraggedOffset.x = component.getX() - mouseEvent.getX();
                        currentlyDraggedOffset.y = component.getY() - mouseEvent.getY();
                        eventBus.post(new OpenSettingsUIRequestedEvent(currentlyDragged));
                        break;
                    }
                }
            }
        }
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

    public boolean isDragging() {
        return currentlyDragged != null;
    }

    @Override
    public Dimension getPreferredSize() {
        int width = 0;
        int height = 0;
        for(int i = 0; i < getComponentCount(); ++i) {
            Component component = getComponent(i);
            width = Math.max(width, component.getX() + 2 * component.getWidth());
            height = Math.max(height, component.getY() + 2 * component.getHeight());
        }
        return new Dimension(width, height);
    }

    @Subscribe
    public void onAlgorithmGraphChanged(AlgorithmGraphChangedEvent event) {
        addNewNodes();
        removeOldNodes();
    }

    @Subscribe
    public void onAlgorithmConnected(AlgorithmGraphConnectedEvent event) {
        ACAQAlgorithmUI sourceNode = nodeUIs.getOrDefault(event.getSource().getAlgorithm(), null);
        ACAQAlgorithmUI targetNode = nodeUIs.getOrDefault(event.getTarget().getAlgorithm(), null);

        if(sourceNode != null && targetNode != null && layoutHelperEnabled) {
            autoPlaceAlgorithm(targetNode);
        }
    }

    @Subscribe
    public void onOpenAlgorithmSettings(OpenSettingsUIRequestedEvent event) {
        eventBus.post(event);
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D)graphics;
        RenderingHints rh = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHints(rh);

        // Draw the edges
        graphics.setColor(Color.DARK_GRAY);
        g.setStroke(new BasicStroke(2));
        for(Map.Entry<ACAQDataSlot<?>, ACAQDataSlot<?>> kv : algorithmGraph.getSlotEdges()) {
            ACAQDataSlot<?> source = kv.getKey();
            ACAQDataSlot<?> target = kv.getValue();
            ACAQAlgorithmUI sourceUI = nodeUIs.getOrDefault(source.getAlgorithm(), null);
            ACAQAlgorithmUI targetUI = nodeUIs.getOrDefault(target.getAlgorithm(), null);

            if(sourceUI == null || targetUI == null)
                continue;

            int sourceY = 0;
            int targetY = 0;
            int sourceX = 0;
            int targetX = 0;

            if(source.isInput()) {
                sourceX = 0;
                sourceY = source.getAlgorithm().getInputSlots().indexOf(source);
            }
            else if(source.isOutput()) {
                sourceX = sourceUI.getWidth();
                sourceY = source.getAlgorithm().getOutputSlots().indexOf(source);
            }

            if(target.isInput()) {
                targetX = 0;
                targetY = target.getAlgorithm().getInputSlots().indexOf(target);
            }
            else if(target.isOutput()) {
                targetX = targetUI.getWidth();
                targetY = target.getAlgorithm().getOutputSlots().indexOf(target);
            }

            // Convert into slot coordinates
            sourceY = sourceY * ACAQAlgorithmUI.SLOT_UI_HEIGHT + ACAQAlgorithmUI.SLOT_UI_HEIGHT / 2;
            targetY = targetY * ACAQAlgorithmUI.SLOT_UI_HEIGHT + ACAQAlgorithmUI.SLOT_UI_HEIGHT / 2;

            // Convert into global coordinates
            sourceX += sourceUI.getX();
            sourceY += sourceUI.getY();
            targetX += targetUI.getX();
            targetY += targetUI.getY();

            // Draw arrow
            Path2D.Float path = new Path2D.Float();
            path.moveTo(sourceX, sourceY);
            float dx = targetX - sourceX;
            path.curveTo(sourceX + 0.4f * dx, sourceY,
                    sourceX + 0.6f * dx , targetY,
                    targetX, targetY);
            g.draw(path);
//            g.drawLine(sourceX, sourceY, targetX, targetY);
        }

        g.setStroke(new BasicStroke(1));
    }

    /**
     * Gets the X position where new entries are placed automatically
     * @return
     */
    public int getNewEntryLocationX() {
        return newEntryLocationX;
    }

    /**
     * Sets the X position where new entries are placed automatically
     * This can be set by parent components to for example place algorithms into the current view
     * @param newEntryLocationX
     */
    public void setNewEntryLocationX(int newEntryLocationX) {
        this.newEntryLocationX = newEntryLocationX;
    }

    public boolean isLayoutHelperEnabled() {
        return layoutHelperEnabled;
    }

    public void setLayoutHelperEnabled(boolean layoutHelperEnabled) {
        this.layoutHelperEnabled = layoutHelperEnabled;
    }
}
