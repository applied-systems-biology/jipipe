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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQSlotType;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbenchPanel;
import org.hkijena.acaq5.ui.components.AddAlgorithmSlotPanel;
import org.hkijena.acaq5.utils.PointRange;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * UI around an {@link ACAQGraphNode} instance
 */
public abstract class ACAQNodeUI extends ACAQWorkbenchPanel {

    /**
     * Height assigned for one slot
     */
    public static final int SLOT_UI_HEIGHT = 50;

    /**
     * Grid width for horizontal direction
     */
    public static final int SLOT_UI_WIDTH = 25;

    public static final String REQUEST_RUN_AND_SHOW_RESULTS = "RUN_AND_SHOW_RESULTS";
    public static final String REQUEST_RUN_ONLY = "RUN_ONLY";
    public static final String REQUEST_OPEN_CONTEXT_MENU = "OPEN_CONTEXT_MENU";

    private ACAQGraphCanvasUI graphUI;
    private ACAQGraphNode node;
    private ACAQGraphCanvasUI.ViewMode viewMode;
    private EventBus eventBus = new EventBus();

    private Color fillColor;
    private Color borderColor;

    /**
     * Creates a new UI
     *
     * @param workbench thr workbench
     * @param graphUI   The graph UI that contains this UI
     * @param node      The algorithm
     * @param viewMode  Directionality of the canvas UI
     */
    public ACAQNodeUI(ACAQWorkbench workbench, ACAQGraphCanvasUI graphUI, ACAQGraphNode node, ACAQGraphCanvasUI.ViewMode viewMode) {
        super(workbench);
        this.graphUI = graphUI;
        this.node = node;
        this.viewMode = viewMode;
        this.node.getEventBus().register(this);
        this.fillColor = UIUtils.getFillColorFor(node.getDeclaration());
        this.borderColor = UIUtils.getBorderColorFor(node.getDeclaration());
    }

    /**
     * @return The displayed algorithm
     */
    public ACAQGraphNode getNode() {
        return node;
    }

    /**
     * Function that creates the "Add slot" button
     *
     * @param slotType slot type
     * @return the button
     */
    protected JButton createAddSlotButton(ACAQSlotType slotType) {
        JButton button = new JButton(UIUtils.getIconFromResources("add.png"));
        button.setPreferredSize(new Dimension(25, SLOT_UI_HEIGHT));
        UIUtils.makeFlat(button);
        button.addActionListener(e -> AddAlgorithmSlotPanel.showDialog(this, graphUI.getGraphHistory(), node, slotType));

        return button;
    }

    /**
     * Updates the slots
     */
    public abstract void updateAlgorithmSlotUIs();

    /**
     * Tries to move the node to the provided location
     * A grid is applied to the input coordinates
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return True if setting the location was successful
     */
    public boolean trySetLocationInGrid(int x, int y) {
        y = (int) Math.rint(y * 1.0 / ACAQNodeUI.SLOT_UI_HEIGHT) * ACAQNodeUI.SLOT_UI_HEIGHT;
        x = (int) Math.rint(x * 1.0 / ACAQNodeUI.SLOT_UI_WIDTH) * ACAQNodeUI.SLOT_UI_WIDTH;
        return trySetLocationNoGrid(x, y);
    }

    /**
     * Returns true if this component overlaps with another component
     *
     * @return True if an overlap was found
     */
    public boolean isOverlapping() {
        for (int i = 0; i < graphUI.getComponentCount(); ++i) {
            Component component = graphUI.getComponent(i);
            if (component instanceof ACAQNodeUI) {
                ACAQNodeUI ui = (ACAQNodeUI) component;
                if (ui != this) {
                    if (ui.getBounds().intersects(getBounds())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Tries to move the node to the provided location
     * A grid is applied to the input coordinates
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return True if setting the location was successful
     */
    public boolean trySetLocationNoGrid(int x, int y) {
        // Check for collisions
        Rectangle futureBounds = new Rectangle(x, y, getWidth(), getHeight());
        for (int i = 0; i < graphUI.getComponentCount(); ++i) {
            Component component = graphUI.getComponent(i);
            if (component instanceof ACAQNodeUI) {
                ACAQNodeUI ui = (ACAQNodeUI) component;
                if (ui != this) {
                    if (ui.getBounds().intersects(futureBounds)) {
                        return false;
                    }
                }
            }
        }

        setLocation(x, y);
        return true;
    }

    /**
     * Get the Y location of the bottom part
     *
     * @return y coordinate
     */
    public int getBottomY() {
        return getY() + getHeight();
    }

    /**
     * Should be triggered when the algorithm's slots are changed.
     * Triggers slot UI updates
     *
     * @param event Generated event
     */
    @Subscribe
    public void onAlgorithmSlotsChanged(AlgorithmSlotsChangedEvent event) {
        updateAlgorithmSlotUIs();
    }

    /**
     * Should be triggered when an algorithm's name parameter is changed
     *
     * @param event The generated event
     */
    @Subscribe
    public void onAlgorithmParametersChanged(ParameterChangedEvent event) {
        if (event.getSource() == node && "acaq:node:name".equals(event.getKey())) {
            updateSize();
            updateName();
            revalidate();
            repaint();
        } else if (event.getSource() == node && "acaq:algorithm:enabled".equals(event.getKey())) {
            updateActivationStatus();
        } else if (event.getSource() == node && "acaq:algorithm:pass-through".equals(event.getKey())) {
            updateActivationStatus();
        }
    }

    /**
     * Called when the algorithm name was updated
     */
    protected abstract void updateName();

    /**
     * Called when the algorithm was enabled/disabled
     */
    protected abstract void updateActivationStatus();

    /**
     * Recalculates the UI size
     */
    public abstract void updateSize();

    @Override
    public void setLocation(int x, int y) {
        super.setLocation(x, y);
        node.setLocationWithin(graphUI.getCompartment(), new Point(x, y), viewMode.toString());
    }

    @Override
    public void setLocation(Point p) {
        super.setLocation(p);
        node.setLocationWithin(graphUI.getCompartment(), p, viewMode.toString());
    }

    /**
     * @return The event bus
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Returns the location of a slot in relative coordinates
     *
     * @param slot the slot
     * @return coordinates relative to this algorithm UI
     */
    public abstract PointRange getSlotLocation(ACAQDataSlot slot);

    public Color getFillColor() {
        return fillColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public ACAQGraphCanvasUI getGraphUI() {
        return graphUI;
    }

    public int getRightX() {
        return getX() + getWidth();
    }

    /**
     * Rounds the input coordinates, so they fit into the grid
     *
     * @param point the coordinates
     * @return rounded coordinates
     */
    public static Point toGridLocation(Point point) {
        int y = (int) Math.rint(point.y * 1.0 / ACAQNodeUI.SLOT_UI_HEIGHT) * ACAQNodeUI.SLOT_UI_HEIGHT;
        int x = (int) Math.rint(point.x * 1.0 / ACAQNodeUI.SLOT_UI_WIDTH) * ACAQNodeUI.SLOT_UI_WIDTH;
        return new Point(x, y);
    }
}
