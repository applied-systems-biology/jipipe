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

package org.hkijena.jipipe.ui.grapheditor.nodeui;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeGraphType;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.AddAlgorithmSlotPanel;
import org.hkijena.jipipe.ui.components.ZoomIcon;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphViewMode;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.RunAndShowIntermediateResultsNodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.RunAndShowResultsNodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.UpdateCacheNodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.UpdateCacheShowIntermediateNodeUIContextAction;
import org.hkijena.jipipe.utils.PointRange;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * UI around an {@link JIPipeGraphNode} instance
 */
public abstract class JIPipeNodeUI extends JIPipeWorkbenchPanel {

    public static final NodeUIContextAction[] RUN_NODE_CONTEXT_MENU_ENTRIES = new NodeUIContextAction[]{
            new UpdateCacheNodeUIContextAction(),
            new UpdateCacheShowIntermediateNodeUIContextAction(),
            NodeUIContextAction.SEPARATOR,
            new RunAndShowResultsNodeUIContextAction(),
            new RunAndShowIntermediateResultsNodeUIContextAction()
    };
    private JIPipeGraphCanvasUI graphUI;
    private JIPipeGraphNode node;
    private JIPipeGraphViewMode viewMode;
    private EventBus eventBus = new EventBus();
    private Color fillColor;
    private Color borderColor;
    private LinearGradientPaint disabledPaint;
    private LinearGradientPaint passThroughPaint;

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
        Color disabledRed1 = new Color(227, 86, 86);
        Color disabledRed2 = new Color(0xc36262);
        this.disabledPaint = new LinearGradientPaint(
                (float) 0, (float) 0, (float) (8), (float) (8),
                new float[]{0, 0.5f, 0.5001f, 1}, new Color[]{disabledRed1, disabledRed1, disabledRed2, disabledRed2}, MultipleGradientPaint.CycleMethod.REPEAT);
        float[] hsb = Color.RGBtoHSB(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), null);
        Color desaturatedFillColor = Color.getHSBColor(hsb[0], hsb[1] / 4, hsb[2] * 0.8f);
        this.passThroughPaint = new LinearGradientPaint(
                (float) 0, (float) 0, (float) (8), (float) (8),
                new float[]{0, 0.5f, 0.5001f, 1}, new Color[]{desaturatedFillColor, desaturatedFillColor, fillColor, fillColor}, MultipleGradientPaint.CycleMethod.REPEAT);
    }

    public boolean isNodeRunnable() {
        if (!node.getInfo().isRunnable())
            return false;
        if (!(node instanceof JIPipeAlgorithm))
            return false;
        return node.getGraph().getAttachment(JIPipeGraphType.class) == JIPipeGraphType.Project;
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
        addSlotButton.addActionListener(e -> {
            if (!JIPipeProjectWorkbench.canModifySlots(getWorkbench()))
                return;
            AddAlgorithmSlotPanel.showDialog(this, graphUI.getGraphHistory(), node, slotType);
        });

        return addSlotButton;
    }

    public abstract void updateHotkeyInfo();

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
    public void onAlgorithmSlotsChanged(JIPipeGraph.NodeSlotsChangedEvent event) {
        updateAlgorithmSlotUIs();
    }

    /**
     * Should be triggered when an algorithm's name parameter is changed
     *
     * @param event The generated event
     */
    @Subscribe
    public void onAlgorithmParametersChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
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

    public abstract boolean needsRecalculateGridSize();

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
            getGraphUI().getWorkbench().setProjectModified(true);
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
        Point point = node.getLocationWithin(StringUtils.nullToEmpty(graphUI.getCompartment()), graphUI.getViewMode().name());
        if (point == null && graphUI.getViewMode() == JIPipeGraphViewMode.VerticalCompact) {
            // Try to get the point from vertical layout
            point = node.getLocationWithin(StringUtils.nullToEmpty(graphUI.getCompartment()), JIPipeGraphViewMode.Vertical.name());
        }
        if (point == null && graphUI.getViewMode() == JIPipeGraphViewMode.Vertical) {
            // Try to get the point from vertical compact layout
            point = node.getLocationWithin(StringUtils.nullToEmpty(graphUI.getCompartment()), JIPipeGraphViewMode.VerticalCompact.name());
        }
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (node instanceof JIPipeAlgorithm) {
            JIPipeAlgorithm algorithm = (JIPipeAlgorithm) node;
            if (!algorithm.isEnabled()) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(disabledPaint);
                g2.fillRect(0, 0, getWidth(), getHeight());
            } else if (algorithm.isPassThrough()) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(passThroughPaint);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }

    /**
     * Moves the node to the next grid location, given a real location
     *
     * @param location a real location
     * @param force    whether to disable checking for overlaps
     * @param save     store the location in the node
     * @return if setting the location was successful
     */
    public boolean moveToClosestGridPoint(Point location, boolean force, boolean save) {
        Point gridPoint = graphUI.getViewMode().realLocationToGrid(location, graphUI.getZoom());
        return moveToGridLocation(gridPoint, force, save);
    }

    /**
     * Updates the UIs for slots
     */
    public abstract void refreshSlots();

    /**
     * An event around {@link JIPipeNodeUI}
     */
    public static class AlgorithmEvent {
        private JIPipeNodeUI ui;

        /**
         * Creates a new event
         *
         * @param ui the algorithm
         */
        public AlgorithmEvent(JIPipeNodeUI ui) {
            this.ui = ui;
        }

        public JIPipeNodeUI getUi() {
            return ui;
        }
    }
}
