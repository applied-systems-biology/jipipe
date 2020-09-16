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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.events.NodeSlotsChangedEvent;
import org.hkijena.jipipe.api.events.ParameterChangedEvent;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.AddAlgorithmSlotPanel;
import org.hkijena.jipipe.ui.components.ZoomIcon;
import org.hkijena.jipipe.utils.PointRange;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Map;

/**
 * UI around an {@link JIPipeGraphNode} instance
 */
public abstract class JIPipeNodeUI extends JIPipeWorkbenchPanel {

    public static final String REQUEST_RUN_AND_SHOW_RESULTS = "RUN_AND_SHOW_RESULTS";
    public static final String REQUEST_UPDATE_CACHE = "RUN_ONLY";
    public static final String REQUEST_OPEN_CONTEXT_MENU = "OPEN_CONTEXT_MENU";

    private JIPipeGraphCanvasUI graphUI;
    private JIPipeGraphNode node;
    private JIPipeGraphViewMode viewMode;
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
    public JIPipeNodeUI(JIPipeWorkbench workbench, JIPipeGraphCanvasUI graphUI, JIPipeGraphNode node, JIPipeGraphViewMode viewMode) {
        super(workbench);
        this.graphUI = graphUI;
        this.node = node;
        this.viewMode = viewMode;
        this.node.getEventBus().register(this);
        this.fillColor = UIUtils.getFillColorFor(node.getInfo());
        this.borderColor = UIUtils.getBorderColorFor(node.getInfo());
    }

    /**
     * @return The displayed algorithm
     */
    public JIPipeGraphNode getNode() {
        return node;
    }

    /**
     * Function that creates the "Add slot" button
     *
     * @param slotType slot type
     * @return the button
     */
    protected JButton createAddSlotButton(JIPipeSlotType slotType) {
        JButton addSlotButton = new JButton(new ZoomIcon(UIUtils.getIconFromResources("actions/list-add.png"), graphUI));
        UIUtils.makeFlat(addSlotButton, Color.GRAY, 0, 0, 0, 0);
        addSlotButton.addActionListener(e -> AddAlgorithmSlotPanel.showDialog(this, graphUI.getGraphHistory(), node, slotType));

        return addSlotButton;
    }

    /**
     * Updates the slots
     */
    public abstract void updateAlgorithmSlotUIs();

    /**
     * Returns true if this component overlaps with another component
     *
     * @return True if an overlap was found
     */
    public boolean isOverlapping() {
        for (int i = 0; i < graphUI.getComponentCount(); ++i) {
            Component component = graphUI.getComponent(i);
            if (component instanceof JIPipeNodeUI) {
                JIPipeNodeUI ui = (JIPipeNodeUI) component;
                if (ui != this) {
                    if (ui.getBounds().intersects(getBounds())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        // Draw additional border if zoom < 1
        if (graphUI.getZoom() < 1) {
            Graphics2D graphics2D = (Graphics2D) g;
            graphics2D.setStroke(JIPipeGraphCanvasUI.STROKE_UNIT);
            graphics2D.setColor(borderColor);
            graphics2D.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
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
    public void onAlgorithmSlotsChanged(NodeSlotsChangedEvent event) {
        updateAlgorithmSlotUIs();
    }

    /**
     * Should be triggered when an algorithm's name parameter is changed
     *
     * @param event The generated event
     */
    @Subscribe
    public void onAlgorithmParametersChanged(ParameterChangedEvent event) {
        if (event.getSource() == node && "jipipe:node:name".equals(event.getKey())) {
            updateSize();
            updateName();
            revalidate();
            repaint();
        } else if (event.getSource() == node && "jipipe:algorithm:enabled".equals(event.getKey())) {
            updateActivationStatus();
        } else if (event.getSource() == node && "jipipe:algorithm:pass-through".equals(event.getKey())) {
            updateActivationStatus();
        }
    }

    /**
     * Calculates the size in grid coordinates
     *
     * @return the size
     */
    public abstract Dimension calculateGridSize();

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

    /**
     * Moves the node to a grid location
     *
     * @param gridLocation the grid location
     * @param force        if false, no overlap check is applied
     * @param save         save the grid location to the node
     * @return if setting the location was successful
     */
    public boolean moveToGridLocation(Point gridLocation, boolean force, boolean save) {
        Point location = graphUI.getViewMode().gridToRealLocation(gridLocation, graphUI.getZoom());
        if (!force) {
            Rectangle futureBounds = new Rectangle(location.x, location.y, getWidth(), getHeight());
            for (int i = 0; i < graphUI.getComponentCount(); ++i) {
                Component component = graphUI.getComponent(i);
                if (component instanceof JIPipeNodeUI) {
                    JIPipeNodeUI ui = (JIPipeNodeUI) component;
                    if (ui != this) {
                        if (ui.getBounds().intersects(futureBounds)) {
                            return false;
                        }
                    }
                }
            }
        }
        setLocation(location);
        if (save) {
            node.setLocationWithin(graphUI.getCompartment(), gridLocation, graphUI.getViewMode().name());
        }
        return true;
    }

    /**
     * Moves the UI back to the stored grid location
     *
     * @param force if false, no overlap check is applied
     * @return either the location was not set or no stored location is available
     */
    public boolean moveToStoredGridLocation(boolean force) {
        Point point = node.getLocationWithin(graphUI.getCompartment(), graphUI.getViewMode().name());
        if (point != null) {
            return moveToGridLocation(point, force, false);
        } else {
            return false;
        }
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
    public abstract PointRange getSlotLocation(JIPipeDataSlot slot);

    public Color getFillColor() {
        return fillColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public JIPipeGraphCanvasUI getGraphUI() {
        return graphUI;
    }

    public int getRightX() {
        return getX() + getWidth();
    }

    public abstract Map<String, JIPipeDataSlotUI> getInputSlotUIs();

    public abstract Map<String, JIPipeDataSlotUI> getOutputSlotUIs();


    /**
     * Moves the node to the next grid location, given a real location
     *
     * @param location a real location
     * @param force    whether to disable checking for overlaps
     * @param save     store the location in the node
     * @return if setting the location was successful
     */
    public boolean moveToNextGridPoint(Point location, boolean force, boolean save) {
        Point gridPoint = graphUI.getViewMode().realLocationToGrid(location, graphUI.getZoom());
        return moveToGridLocation(gridPoint, force, save);
    }
}
