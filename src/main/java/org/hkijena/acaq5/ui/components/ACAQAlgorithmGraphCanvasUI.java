package org.hkijena.acaq5.ui.components;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.hkijena.acaq5.api.ACAQAlgorithm;
import org.hkijena.acaq5.api.ACAQAlgorithmGraph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ACAQAlgorithmGraphCanvasUI extends JPanel implements MouseMotionListener, MouseListener {
    private ACAQAlgorithmGraph algorithmGraph;
    private ACAQAlgorithmUI currentlyDragged;
    private Point currentlyDraggedOffset = new Point();
    private BiMap<ACAQAlgorithm, ACAQAlgorithmUI> nodeUIs = HashBiMap.create();

    public ACAQAlgorithmGraphCanvasUI(ACAQAlgorithmGraph algorithmGraph) {
        super(null);
        this.algorithmGraph = algorithmGraph;
        initialize();
        addNewNodes();
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
            add(ui);
            ui.setLocation(ACAQAlgorithmUI.SLOT_UI_WIDTH * 4, y);
            nodeUIs.put(algorithm, ui);
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
        if(mouseEvent.getButton() == MouseEvent.BUTTON1) {
            for(int i = 0; i < getComponentCount(); ++i) {
                Component component = getComponent(i);
                if(component.getBounds().contains(mouseEvent.getX(), mouseEvent.getY())) {
                    if(component instanceof ACAQAlgorithmUI) {
                        currentlyDragged = (ACAQAlgorithmUI)component;
                        currentlyDraggedOffset.x = component.getX() - mouseEvent.getX();
                        currentlyDraggedOffset.y = component.getY() - mouseEvent.getY();
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
}
