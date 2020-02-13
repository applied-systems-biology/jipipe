package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQAlgorithm;
import org.hkijena.acaq5.api.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.ACAQDataSlot;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.ui.events.ACAQAlgorithmUIOpenSettingsRequested;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;
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
     * Assigns slot connections if slots are directly next to each other in the UI
     */
    private void autoAssignConnections() {

    }

    private void tryMoveToAutoAssign() {

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
        List<ACAQAlgorithm> newNodes = algorithmGraph.getNodes().stream().filter(x -> !nodeUIs.containsKey(x)).collect(Collectors.toList());
        for(ACAQAlgorithm algorithm : newNodes) {
            //TODO: More auto-layout-like placement. Only use the brute force method as last measurement
            int y = nodeUIs.values().stream().map(ACAQAlgorithmUI::getBottomY).max(Integer::compareTo).orElse(0);
            if(y == 0)
                y += 2 * ACAQAlgorithmUI.SLOT_UI_HEIGHT;
            ACAQAlgorithmUI ui = new ACAQAlgorithmUI(this, algorithm);
            ui.getEventBus().register(this);
            add(ui);
            nodeUIs.put(algorithm, ui);
            if(algorithm.getLocation() == null || !ui.trySetLocationNoGrid(algorithm.getLocation().x, algorithm.getLocation().y)) {
                ui.setLocation(ACAQAlgorithmUI.SLOT_UI_WIDTH * 4, y);
            }
        }
        revalidate();
        repaint();
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
        if(mouseEvent.getButton() == MouseEvent.BUTTON1) {
            for(int i = 0; i < getComponentCount(); ++i) {
                Component component = getComponent(i);
                if(component.getBounds().contains(mouseEvent.getX(), mouseEvent.getY())) {
                    if(component instanceof ACAQAlgorithmUI) {
                        currentlyDragged = (ACAQAlgorithmUI)component;
                        currentlyDraggedOffset.x = component.getX() - mouseEvent.getX();
                        currentlyDraggedOffset.y = component.getY() - mouseEvent.getY();
                        eventBus.post(new ACAQAlgorithmUIOpenSettingsRequested(currentlyDragged));
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

    @Override
    public Dimension getPreferredSize() {
        int width = 0;
        int height = 0;
        for(int i = 0; i < getComponentCount(); ++i) {
            Component component = getComponent(i);
            width = Math.max(width, component.getX() + component.getWidth());
            height = Math.max(height, component.getY() + component.getHeight());
        }
        return new Dimension(width, height);
    }

    @Subscribe
    public void onAlgorithmGraphChanged(AlgorithmGraphChangedEvent event) {
        addNewNodes();
        removeOldNodes();
    }

    @Subscribe
    public void onOpenAlgorithmSettings(ACAQAlgorithmUIOpenSettingsRequested event) {
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
            ACAQAlgorithmUI sourceUI = nodeUIs.get(source.getAlgorithm());
            ACAQAlgorithmUI targetUI = nodeUIs.get(target.getAlgorithm());

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
}
