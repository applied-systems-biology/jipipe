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

package org.hkijena.jipipe.ui.grapheditor.general.nodeui;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeGraphType;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.cache.JIPipeCache;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.algorithmfinder.AlgorithmFinderSuccessEvent;
import org.hkijena.jipipe.ui.algorithmfinder.JIPipeAlgorithmSourceFinderUI;
import org.hkijena.jipipe.ui.algorithmfinder.JIPipeAlgorithmTargetFinderUI;
import org.hkijena.jipipe.ui.components.AddAlgorithmSlotPanel;
import org.hkijena.jipipe.ui.components.EditAlgorithmSlotPanel;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphViewMode;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.contextmenu.*;
import org.hkijena.jipipe.utils.*;
import org.hkijena.jipipe.utils.ui.ViewOnlyMenuItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * UI around an {@link JIPipeGraphNode} instance
 */
public class JIPipeNodeUI extends JIPipeWorkbenchPanel implements MouseListener, MouseMotionListener {
    public static final Color COLOR_DISABLED_1 = new Color(227, 86, 86);
    public static final Color COLOR_DISABLED_2 = new Color(0xc36262);

    public static final Color COLOR_SLOT_CACHED = new Color(0x95c2a8);

    public static final Color COLOR_SLOT_DISCONNECTED = new Color(0xc36262);

    public static final Color COLOR_RUN_BUTTON_ICON = new Color(0x22A02D);

    public static final NodeUIContextAction[] RUN_NODE_CONTEXT_MENU_ENTRIES = new NodeUIContextAction[]{
            new UpdateCacheNodeUIContextAction(),
            new UpdateCacheShowIntermediateNodeUIContextAction(),
            NodeUIContextAction.SEPARATOR,
            new RunAndShowResultsNodeUIContextAction(),
            new RunAndShowIntermediateResultsNodeUIContextAction(),
            NodeUIContextAction.SEPARATOR,
            new ClearCacheNodeUIContextAction()
    };

    private final JIPipeGraphViewMode viewMode = JIPipeGraphViewMode.VerticalCompact;

    private final JIPipeGraphCanvasUI graphCanvasUI;
    private final JIPipeGraphNode node;
    private final EventBus eventBus = new EventBus();
    private final Color nodeFillColor;
    private final Color nodeBorderColor;
    private final Color highlightedNodeBorderColor;
    private final LinearGradientPaint nodeDisabledPaint;
    private final LinearGradientPaint nodePassThroughPaint;
    private final Color slotFillColor;
    private final Color buttonFillColor;

    private final Color buttonFillColorDarker;
    private final boolean slotsInputsEditable;
    private final boolean slotsOutputsEditable;
    private final Map<String, JIPipeNodeUISlotActiveArea> inputSlotMap = new HashMap<>();
    private final Map<String, JIPipeNodeUISlotActiveArea> outputSlotMap = new HashMap<>();
    private JIPipeNodeUIAddSlotButtonActiveArea addInputSlotArea;
    private JIPipeNodeUIAddSlotButtonActiveArea addOutputSlotArea;

    private final Image nodeIcon;
    private boolean mouseIsEntered = false;

    private double zoom = 1;

    private final Font nativeMainFont = new Font(Font.DIALOG, Font.PLAIN, 12);

    private final Font nativeSecondaryFont = new Font(Font.DIALOG, Font.PLAIN, 11);

    private final Color mainTextColor;

    private final Color secondaryTextColor;

    private Font zoomedMainFont;

    private Font zoomedSecondaryFont;

    private boolean nodeIsRunnable;

    private final List<JIPipeNodeUIActiveArea> activeAreas = new ArrayList<>();
    private JIPipeNodeUIActiveArea currentActiveArea;

    /**
     * Creates a new UI
     *
     * @param workbench thr workbench
     * @param graphCanvasUI   The graph UI that contains this UI
     * @param node      The algorithm
     */
    public JIPipeNodeUI(JIPipeWorkbench workbench, JIPipeGraphCanvasUI graphCanvasUI, JIPipeGraphNode node) {
        super(workbench);
        this.graphCanvasUI = graphCanvasUI;
        this.node = node;
        this.node.getEventBus().register(this);
        this.graphCanvasUI.getGraph().getEventBus().register(this);
        if(workbench instanceof JIPipeProjectWorkbench) {
            ((JIPipeProjectWorkbench) workbench).getProject().getCache().getEventBus().register(this);
        }

        // Node information
        nodeIsRunnable = node.getInfo().isRunnable() || node instanceof JIPipeAlgorithm || node instanceof JIPipeProjectCompartment;

        // Slot information
        if (node.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
            slotsInputsEditable = ((JIPipeMutableSlotConfiguration) node.getSlotConfiguration()).canModifyInputSlots();
            slotsOutputsEditable = ((JIPipeMutableSlotConfiguration) node.getSlotConfiguration()).canModifyOutputSlots();
        } else {
            slotsInputsEditable = false;
            slotsOutputsEditable = false;
        }

        // Generate colors, icons
        this.nodeIcon = JIPipe.getNodes().getIconFor(node.getInfo()).getImage();
        this.nodeFillColor = UIUtils.getFillColorFor(node.getInfo());
        this.nodeBorderColor = UIUtils.getBorderColorFor(node.getInfo());
        this.highlightedNodeBorderColor = COLOR_DISABLED_2;
        this.slotFillColor = UIManager.getColor("Panel.background");
        this.mainTextColor = UIManager.getColor("Label.foreground");
        this.secondaryTextColor = nodeBorderColor;
        this.nodeDisabledPaint = new LinearGradientPaint(
                (float) 0, (float) 0, (float) (8), (float) (8),
                new float[]{0, 0.5f, 0.5001f, 1}, new Color[]{COLOR_DISABLED_1, COLOR_DISABLED_1, COLOR_DISABLED_2, COLOR_DISABLED_2}, MultipleGradientPaint.CycleMethod.REPEAT);
        Color desaturatedFillColor = ColorUtils.multiplyHSB(nodeFillColor, 1f, 0.25f, 0.8f);
        this.nodePassThroughPaint = new LinearGradientPaint(
                (float) 0, (float) 0, (float) (8), (float) (8),
                new float[]{0, 0.5f, 0.5001f, 1}, new Color[]{desaturatedFillColor, desaturatedFillColor, nodeFillColor, nodeFillColor}, MultipleGradientPaint.CycleMethod.REPEAT);
        this.buttonFillColor = ColorUtils.multiplyHSB(slotFillColor, 1, 1, 0.95f);
        this.buttonFillColorDarker = ColorUtils.multiplyHSB(slotFillColor, 1, 1, 0.85f);

        // Initialization
        initialize();
        updateView(true, true, true);
    }

    private void initialize() {
        setBackground(getFillColor());
        setBorder(null);
//        setBorder(BorderFactory.createLineBorder(getBorderColor()));
        setLayout(new GridBagLayout());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public boolean isNodeRunnable() {
        if (!node.getInfo().isRunnable())
            return false;
        if (!(node instanceof JIPipeAlgorithm))
            return false;
        return node.getParentGraph().getAttachment(JIPipeGraphType.class) == JIPipeGraphType.Project;
    }

    public double getZoom() {
        return zoom;
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
        updateView(true, false, true);
    }

    public void updateView(boolean assets, boolean slots, boolean size) {
        if (assets) {
            updateAssets();
        }
        if (slots) {
            updateSlots();
        }
        if (slots || size) {
            updateSize();
            getGraphCanvasUI().repaint(50);
        }
        updateActiveAreas();
    }

    private void updateActiveAreas() {
        activeAreas.clear();

        // Add whole node
        updateWholeNodeActiveAreas();

        // Add slots
        updateSlotActiveAreas();

        // Sort
        activeAreas.sort(Comparator.naturalOrder());
    }

    public Map<String, JIPipeNodeUISlotActiveArea> getInputSlotMap() {
        return inputSlotMap;
    }

    public Map<String, JIPipeNodeUISlotActiveArea> getOutputSlotMap() {
        return outputSlotMap;
    }

    private void updateSlotActiveAreas() {
        for (JIPipeNodeUISlotActiveArea slotState : inputSlotMap.values()) {
            Rectangle slotArea = new Rectangle((int) Math.round(slotState.getNativeLocation().x * zoom),
                    (int) Math.round(slotState.getNativeLocation().y * zoom),
                    (int) Math.round(slotState.getNativeWidth() * zoom),
                    (int) Math.round(viewMode.getGridHeight() * zoom));
            slotState.setZoomedHitArea(slotArea);
            activeAreas.add(slotState);

            // Slot button
            double centerY = slotState.getZoomedHitArea().y + slotState.getZoomedHitArea().height / 2.0;
            Rectangle slotButtonArea = new Rectangle((int) Math.round(slotState.getNativeLocation().x * zoom + 8 * zoom),
                    (int) Math.round(centerY - 11 * zoom),
                    (int) Math.round(22 * zoom),
                    (int) Math.round(22 * zoom));
            JIPipeNodeUISlotButtonActiveArea slotButtonActiveArea = new JIPipeNodeUISlotButtonActiveArea(slotState);
            slotButtonActiveArea.setZoomedHitArea(slotButtonArea);
            activeAreas.add(slotButtonActiveArea);
        }
        if(addInputSlotArea != null) {
            Rectangle slotArea = new Rectangle((int) Math.round(addInputSlotArea.getNativeLocation().x * zoom),
                    (int) Math.round(addInputSlotArea.getNativeLocation().y * zoom),
                    (int) Math.round(addInputSlotArea.getNativeWidth() * zoom),
                    (int) Math.round(viewMode.getGridHeight() * zoom));
            addInputSlotArea.setZoomedHitArea(slotArea);
            activeAreas.add(addInputSlotArea);
        }
        for (JIPipeNodeUISlotActiveArea slotState : outputSlotMap.values()) {
            Rectangle slotArea = new Rectangle((int) Math.round(slotState.getNativeLocation().x * zoom),
                    (int) Math.round(slotState.getNativeLocation().y * zoom),
                    (int) Math.round(slotState.getNativeWidth() * zoom),
                    (int) Math.round(viewMode.getGridHeight() * zoom));
            slotState.setZoomedHitArea(slotArea);
            activeAreas.add(slotState);

            // Slot button
            double centerY = slotState.getZoomedHitArea().y + slotState.getZoomedHitArea().height / 2.0;
            Rectangle slotButtonArea = new Rectangle((int) Math.round(slotState.getNativeLocation().x * zoom + 8 * zoom),
                    (int) Math.round(centerY - 11 * zoom),
                    (int) Math.round(22 * zoom),
                    (int) Math.round(22 * zoom));
            JIPipeNodeUISlotButtonActiveArea slotButtonActiveArea = new JIPipeNodeUISlotButtonActiveArea(slotState);
            slotButtonActiveArea.setZoomedHitArea(slotButtonArea);
            activeAreas.add(slotButtonActiveArea);
        }
        if(addOutputSlotArea != null) {
            Rectangle slotArea = new Rectangle((int) Math.round(addOutputSlotArea.getNativeLocation().x * zoom),
                    (int) Math.round(addOutputSlotArea.getNativeLocation().y * zoom),
                    (int) Math.round(addOutputSlotArea.getNativeWidth() * zoom),
                    (int) Math.round(viewMode.getGridHeight() * zoom));
            addOutputSlotArea.setZoomedHitArea(slotArea);
            activeAreas.add(addOutputSlotArea);
        }
    }

    private void updateWholeNodeActiveAreas() {
        FontMetrics mainFontMetrics;

        if (getGraphics() != null) {
            mainFontMetrics = getGraphics().getFontMetrics(zoomedMainFont);
        } else {
            Canvas c = new Canvas();
            mainFontMetrics = c.getFontMetrics(zoomedMainFont);
        }

        // Whole node
        JIPipeNodeUIWholeNodeActiveArea wholeNodeActiveArea = new JIPipeNodeUIWholeNodeActiveArea();
        wholeNodeActiveArea.setZoomedHitArea(new Rectangle(0, 0, getWidth(), getHeight()));
        activeAreas.add(wholeNodeActiveArea);

        // Node button
        if(nodeIsRunnable) {
            int realSlotHeight = viewMode.gridToRealSize(new Dimension(1, 1), zoom).height;
            boolean hasInputs = node.getInputSlots().size() > 0 || slotsInputsEditable;
            boolean hasOutputs = node.getOutputSlots().size() > 0 || slotsOutputsEditable;

            int centerY;
            if (hasInputs && !hasOutputs) {
                centerY = (getHeight() - realSlotHeight) / 2 + realSlotHeight;
            } else if (!hasInputs && hasOutputs) {
                centerY = (getHeight() - realSlotHeight) / 2;
            } else {
                centerY = getHeight() / 2;
            }

            String nameLabel = node.getName();
            int centerNativeWidth = (int) Math.round((nodeIsRunnable ? 22 : 0) * zoom + 22 * zoom + mainFontMetrics.stringWidth(nameLabel));
            double startX = getWidth() / 2.0 - centerNativeWidth / 2.0;

            JIPipeNodeUIRunNodeActiveArea activeArea = new JIPipeNodeUIRunNodeActiveArea();
            activeArea.setZoomedHitArea(new Rectangle((int)Math.round(startX), (int)Math.round(centerY - 11 * zoom), (int)Math.round(22 * zoom), (int)Math.round(22 * zoom)));

            activeAreas.add(activeArea);
        }
    }

    private void updateAssets() {
        // Update fonts
        zoomedMainFont = new Font(Font.DIALOG, Font.PLAIN, (int) Math.round(12 * zoom));
        zoomedSecondaryFont = new Font(Font.DIALOG, Font.PLAIN, (int) Math.round(11 * zoom));
    }

    /**
     * @return The displayed algorithm
     */
    public JIPipeGraphNode getNode() {
        return node;
    }

    public void updateHotkeyInfo() {
        // TODO
    }

    /**
     * Get the Y location of the bottom part
     *
     * @return y coordinate
     */
    public int getBottomY() {
        return getY() + getHeight();
    }


    @Subscribe
    public void onNodeSlotsChanged(JIPipeGraph.NodeSlotsChangedEvent event) {
        if (event.getNode() == node) {
            updateView(false, true, true);
        }
    }

    @Subscribe
    public void onNodeConnected(JIPipeGraph.NodeConnectedEvent event) {
        if (event.getSource().getNode() == node || event.getTarget().getNode() == node) {
            updateView(false, true, true);
        }
    }

    @Subscribe
    public void onNodeDisconnected(JIPipeGraph.NodeDisconnectedEvent event) {
        if (event.getSource().getNode() == node || event.getTarget().getNode() == node) {
            updateView(false, true, true);
        }
    }

    @Subscribe
    public void onSlotNameChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        updateView(false, true, true);
    }

    /**
     * Should be triggered when an algorithm's name parameter is changed
     *
     * @param event The generated event
     */
    @Subscribe
    public void onNodeParametersChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (event.getSource() == node && "jipipe:node:name".equals(event.getKey())) {
            updateView(false, false, true);
        } else if (event.getSource() == node && "jipipe:algorithm:enabled".equals(event.getKey())) {
            getGraphCanvasUI().repaint(50);
        } else if (event.getSource() == node && "jipipe:algorithm:pass-through".equals(event.getKey())) {
            getGraphCanvasUI().repaint(50);
        } else if (event.getSource() == node && "jipipe:node:bookmarked".equals(event.getKey())) {
            getGraphCanvasUI().repaint(50);
        }
    }

    /**
     * Recalculates the UI size
     */
    private void updateSize() {

        FontMetrics mainFontMetrics;
        FontMetrics secondaryFontMetrics;

        if (getGraphics() != null) {
            mainFontMetrics = getGraphics().getFontMetrics(nativeMainFont);
            secondaryFontMetrics = getGraphics().getFontMetrics(nativeSecondaryFont);
        } else {
            Canvas c = new Canvas();
            mainFontMetrics = c.getFontMetrics(nativeMainFont);
            secondaryFontMetrics = c.getFontMetrics(nativeSecondaryFont);
        }

        double mainWidth = (nodeIsRunnable ? 22 : 0) + 22 + mainFontMetrics.stringWidth(node.getName()) + 16;

        // Slot widths
        double sumInputSlotWidths = 0;
        double sumOutputSlotWidths = 0;

        for (JIPipeInputDataSlot inputSlot : node.getInputSlots()) {
            JIPipeNodeUISlotActiveArea slotState = inputSlotMap.get(inputSlot.getName());
            double nativeWidth = secondaryFontMetrics.stringWidth(slotState.getSlotLabel()) + 22 * 2 + 16;
            slotState.setNativeWidth(nativeWidth);
            slotState.setNativeLocation(new Point((int) sumInputSlotWidths, 0));
            sumInputSlotWidths += nativeWidth;
        }
        if (slotsInputsEditable) {
            addInputSlotArea = new JIPipeNodeUIAddSlotButtonActiveArea(JIPipeSlotType.Input);
            double nativeWidth = 22;
            addInputSlotArea.setNativeWidth(nativeWidth);
            addInputSlotArea.setNativeLocation(new Point((int) sumInputSlotWidths, 0));
            sumInputSlotWidths += nativeWidth;
        }
        else {
            addInputSlotArea = null;
        }

        for (JIPipeDataSlot outputSlots : node.getOutputSlots()) {
            JIPipeNodeUISlotActiveArea slotState = outputSlotMap.get(outputSlots.getName());
            double nativeWidth = secondaryFontMetrics.stringWidth(slotState.getSlotLabel()) + 22 * 2 + 16;
            slotState.setNativeWidth(nativeWidth);
            slotState.setNativeLocation(new Point((int) sumOutputSlotWidths, viewMode.getGridHeight() * 2));
            sumOutputSlotWidths += nativeWidth;
        }
        if (slotsOutputsEditable) {
            addOutputSlotArea = new JIPipeNodeUIAddSlotButtonActiveArea(JIPipeSlotType.Output);
            double nativeWidth = 22;
            addOutputSlotArea.setNativeWidth(nativeWidth);
            addOutputSlotArea.setNativeLocation(new Point((int) sumOutputSlotWidths, viewMode.getGridHeight() * 2));
            sumOutputSlotWidths += nativeWidth;
        }
        else {
            addOutputSlotArea = null;
        }

        // Calculate the grid width
        double maxWidth = Math.max(mainWidth, Math.max(sumInputSlotWidths, sumOutputSlotWidths));
        int gridWidth = (int) Math.ceil(maxWidth / viewMode.getGridWidth());

        // Correct the slot width to fit the actual native width of the control
        int nativeWidth = viewMode.getGridWidth() * gridWidth;

        scaleSlotsNativeWidth(inputSlotMap, addInputSlotArea, nativeWidth, sumInputSlotWidths, slotsInputsEditable);
        scaleSlotsNativeWidth(outputSlotMap, addOutputSlotArea, nativeWidth, sumOutputSlotWidths, slotsOutputsEditable);

        // Update the real size of the control
        Dimension gridSize = new Dimension(gridWidth, 3);
        Dimension realSize = viewMode.gridToRealSize(gridSize, zoom);
        setSize(realSize);
        revalidate();

        // Update the active areas
        updateActiveAreas();
    }

    private void scaleSlotsNativeWidth(Map<String, JIPipeNodeUISlotActiveArea> slotStateMap, JIPipeNodeUIAddSlotButtonActiveArea addSlotActiveArea, double nodeWidth, double sumWidth, boolean hasButton) {
//        if(slotStateMap.size() == 1 && hasButton) {
//            return;
//        }
        boolean excludeButton = false;
        if (slotStateMap.size() > 1 && hasButton) {
            nodeWidth -= 22;
            sumWidth -= 22;
            excludeButton = true;
        }
        double factor = nodeWidth / sumWidth;
        for (JIPipeNodeUISlotActiveArea slotState : slotStateMap.values()) {
            slotState.getNativeLocation().x = (int) Math.round(slotState.getNativeLocation().x * factor);
            slotState.setNativeWidth(slotState.getNativeWidth() * factor);
        }
        if(!excludeButton && addSlotActiveArea != null) {
            addSlotActiveArea.setNativeWidth(addSlotActiveArea.getNativeWidth() * factor);
        }
    }

    /**
     * Moves the node to a grid location
     *
     * @param gridLocation the grid location
     * @param force        if false, no overlap check is applied
     * @param save         save the grid location to the node
     * @return if setting the location was successful
     */
    public boolean moveToGridLocation(Point gridLocation, boolean force, boolean save) {
        Point location = graphCanvasUI.getViewMode().gridToRealLocation(gridLocation, graphCanvasUI.getZoom());
        if (!force) {
            Rectangle futureBounds = new Rectangle(location.x, location.y, getWidth(), getHeight());
            for (int i = 0; i < graphCanvasUI.getComponentCount(); ++i) {
                Component component = graphCanvasUI.getComponent(i);
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
        if (location.x != getX() || location.y != getY()) {
            setLocation(location);
        }
        if (save) {
            node.setLocationWithin(graphCanvasUI.getCompartment(), gridLocation, graphCanvasUI.getViewMode().name());
            getGraphCanvasUI().getWorkbench().setProjectModified(true);
        }
        return true;
    }

    public Point getStoredGridLocation() {
        return node.getLocationWithin(StringUtils.nullToEmpty(graphCanvasUI.getCompartment()), graphCanvasUI.getViewMode().name());
    }

    /**
     * Moves the UI back to the stored grid location
     *
     * @param force if false, no overlap check is applied
     * @return either the location was not set or no stored location is available
     */
    @SuppressWarnings("deprecation")
    public boolean moveToStoredGridLocation(boolean force) {
        Point point = node.getLocationWithin(StringUtils.nullToEmpty(graphCanvasUI.getCompartment()), graphCanvasUI.getViewMode().name());
        if (point == null) {
            // Try to get the point from vertical layout (migrate to compact)
            point = node.getLocationWithin(StringUtils.nullToEmpty(graphCanvasUI.getCompartment()), JIPipeGraphViewMode.Vertical.name());
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
    public PointRange getSlotLocation(JIPipeDataSlot slot) {
        JIPipeNodeUISlotActiveArea slotState;
        if (slot.isInput()) {
            slotState = inputSlotMap.get(slot.getName());
        } else if (slot.isOutput()) {
            slotState = outputSlotMap.get(slot.getName());
        } else {
            return new PointRange(0, 0);
        }
        if (slotState == null || slotState.getNativeLocation() == null) {
            return new PointRange(0, 0);
        }
        if (slot.isInput()) {
            int y = (int) (zoom * slotState.getNativeLocation().y);
            return new PointRange(new Point((int) (zoom * (slotState.getNativeLocation().x + slotState.getNativeWidth() / 2)), y),
                    new Point((int) (zoom * slotState.getNativeLocation().x) + 8, y),
                    new Point((int) (zoom * (slotState.getNativeLocation().x + slotState.getNativeWidth())) - 8, y));
        } else {
            int y = (int) (zoom * (slotState.getNativeLocation().y + viewMode.getGridHeight()));
            return new PointRange(new Point((int) (zoom * (slotState.getNativeLocation().x + slotState.getNativeWidth() / 2)), y),
                    new Point((int) (zoom * slotState.getNativeLocation().x) + 8, y),
                    new Point((int) (zoom * (slotState.getNativeLocation().x + slotState.getNativeWidth())) - 8, y));
        }
    }

    public Color getFillColor() {
        return nodeFillColor;
    }

    public Color getBorderColor() {
        return nodeBorderColor;
    }

    public JIPipeGraphCanvasUI getGraphCanvasUI() {
        return graphCanvasUI;
    }

    public int getRightX() {
        return getX() + getWidth();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        super.paintComponent(g);

        // Paint disabled/pass-through
        if (node instanceof JIPipeAlgorithm) {
            JIPipeAlgorithm algorithm = (JIPipeAlgorithm) node;
            if (!algorithm.isEnabled()) {
                g2.setPaint(nodeDisabledPaint);
                g2.fillRect(0, 0, getWidth(), getHeight());
            } else if (algorithm.isPassThrough()) {
                g2.setPaint(nodePassThroughPaint);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        }

        g2.setFont(zoomedMainFont);
        FontMetrics fontMetrics = g2.getFontMetrics();

        int realSlotHeight = viewMode.gridToRealSize(new Dimension(1, 1), zoom).height;
        boolean hasInputs = node.getInputSlots().size() > 0 || slotsInputsEditable;
        boolean hasOutputs = node.getOutputSlots().size() > 0 || slotsOutputsEditable;

        // Paint controls
        paintNodeControls(g2, fontMetrics, realSlotHeight, hasInputs, hasOutputs);

        // Paint slots
        g2.setFont(zoomedSecondaryFont);
        g2.setStroke(JIPipeGraphCanvasUI.STROKE_UNIT);

        if (hasInputs) {
            paintInputSlots(g2, realSlotHeight);
        }
        if (hasOutputs) {
            paintOutputSlots(g2, realSlotHeight);
        }

        // Paint outside border
        g2.setStroke(JIPipeGraphCanvasUI.STROKE_UNIT);
        g2.setColor(currentActiveArea instanceof JIPipeNodeUIWholeNodeActiveArea ? highlightedNodeBorderColor : nodeBorderColor);
        g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

//        for (ActiveArea activeArea : activeAreas) {
//            g2.setPaint(Color.RED);
//            g2.draw(activeArea.zoomedHitArea);
//        }
    }

    private void paintOutputSlots(Graphics2D g2, int realSlotHeight) {
        List<JIPipeOutputDataSlot> outputSlots = node.getOutputSlots();

        // Slot main filling
        g2.setPaint(slotFillColor);
        g2.fillRect(0, getHeight() - realSlotHeight, getWidth(), realSlotHeight);

        int startX = 0;

        for (int i = 0; i < outputSlots.size(); i++) {
            JIPipeDataSlot outputSlot = outputSlots.get(i);
            JIPipeNodeUISlotActiveArea slotState = outputSlotMap.get(outputSlot.getName());

            if (slotState == null)
                continue;

            int slotWidth = (int) Math.round(slotState.getNativeWidth() * zoom);

            // Draw highlight
            if (slotState == currentActiveArea) {
                g2.setPaint(buttonFillColor);
                g2.fillRect(startX, getHeight() - realSlotHeight, slotWidth, realSlotHeight);
            }

            // Draw separator
            if (i > 0) {
                g2.setStroke(JIPipeGraphCanvasUI.STROKE_UNIT);
                g2.setPaint(nodeBorderColor);
                g2.drawLine(startX, getHeight() - realSlotHeight, startX, getHeight());
            }

            // Draw slot itself
            paintSlot(g2,
                    slotState,
                    realSlotHeight,
                    startX,
                    slotWidth,
                    slotState.getSlotStatus() == SlotStatus.Cached ? COLOR_SLOT_CACHED : null,
                    null,
                    getHeight() - realSlotHeight - 1,
                    (int) Math.round(getHeight() - 4 * zoom),
                    (int) Math.round(getHeight() - 1 - realSlotHeight / 2.0));

            startX += slotWidth;
        }
        if (slotsOutputsEditable) {

            // Draw separator
            if (outputSlots.size() > 1) {
                g2.setStroke(JIPipeGraphCanvasUI.STROKE_UNIT);
                g2.setPaint(nodeBorderColor);
                g2.drawLine(startX, getHeight() - realSlotHeight, startX, getHeight());
            }

            // Draw button
            if (addOutputSlotArea != null) {
                int slotWidth = (int) Math.round(addOutputSlotArea.getNativeWidth() * zoom);
                paintAddSlotButton(g2, addOutputSlotArea, slotWidth, realSlotHeight, startX, getHeight() - realSlotHeight);
            }
        }

        // Line above the slots
        g2.setStroke(JIPipeGraphCanvasUI.STROKE_UNIT);
        g2.setPaint(nodeBorderColor);
        g2.drawLine(0, getHeight() - realSlotHeight, getWidth(), getHeight() - realSlotHeight);
    }

    private void paintAddSlotButton(Graphics2D g2, JIPipeNodeUIAddSlotButtonActiveArea activeArea, int slotWidth, int realSlotHeight, int startX, int y) {

        boolean isHighlighted = currentActiveArea instanceof JIPipeNodeUIAddSlotButtonActiveArea && ((JIPipeNodeUIAddSlotButtonActiveArea) currentActiveArea).getSlotType() == activeArea.getSlotType();

        g2.setStroke(JIPipeGraphCanvasUI.STROKE_UNIT);
        g2.setPaint(isHighlighted ? buttonFillColorDarker : buttonFillColor);
        g2.fillRect(startX, y, slotWidth, realSlotHeight);
        g2.setPaint(nodeBorderColor);
        g2.drawRect(startX, y, slotWidth, realSlotHeight);

        int zoomedSize = (int) Math.round(16 * zoom);

        g2.drawImage(UIUtils.getIconFromResources("actions/add.png").getImage(), startX + slotWidth / 2 - zoomedSize / 2, y + realSlotHeight / 2 - zoomedSize / 2, zoomedSize, zoomedSize, null);
    }

    private void paintInputSlots(Graphics2D g2, int realSlotHeight) {
        List<JIPipeInputDataSlot> inputSlots = node.getInputSlots();

        // Slot main filling
        g2.setPaint(slotFillColor);
        g2.fillRect(0, 0, getWidth(), realSlotHeight);

        int startX = 0;

        for (int i = 0; i < inputSlots.size(); i++) {
            JIPipeInputDataSlot inputSlot = inputSlots.get(i);
            JIPipeNodeUISlotActiveArea slotState = inputSlotMap.get(inputSlot.getName());

            if (slotState == null)
                continue;

            int slotWidth = (int) Math.round(slotState.getNativeWidth() * zoom);

            // Draw highlight
            if (slotState == currentActiveArea) {
                g2.setPaint(buttonFillColor);
                g2.fillRect(startX, 0, slotWidth, realSlotHeight);
            }

            // Draw separator
            if (i > 0) {
                g2.setStroke(JIPipeGraphCanvasUI.STROKE_UNIT);
                g2.setPaint(nodeBorderColor);
                g2.drawLine(startX, 0, startX, realSlotHeight);
            }

            // Draw slot itself
            paintSlot(g2,
                    slotState,
                    realSlotHeight,
                    startX,
                    slotWidth,
                    slotState.getSlotStatus() == SlotStatus.Unconnected ? COLOR_SLOT_DISCONNECTED : null,
                    slotState.getSlotStatus() == SlotStatus.Unconnected ? COLOR_SLOT_DISCONNECTED : null,
                    0,
                    (int) Math.round(2 * zoom),
                    (int) Math.round(realSlotHeight / 2.0));

            startX += slotWidth;
        }
        if (slotsInputsEditable) {

            // Draw separator
            if (inputSlots.size() > 1) {
                g2.setStroke(JIPipeGraphCanvasUI.STROKE_UNIT);
                g2.setPaint(nodeBorderColor);
                g2.drawLine(startX, 0, startX, realSlotHeight);
            }

            // Draw button
            if (addInputSlotArea != null) {
                int slotWidth = (int) Math.round(addInputSlotArea.getNativeWidth() * zoom);
                paintAddSlotButton(g2, addInputSlotArea, slotWidth, realSlotHeight, startX, 0);
            }
        }

        // Line below the slots
        g2.setStroke(JIPipeGraphCanvasUI.STROKE_UNIT);
        g2.setPaint(nodeBorderColor);
        g2.drawLine(0, realSlotHeight, getWidth(), realSlotHeight);
    }

    private void paintSlot(Graphics2D g2, JIPipeNodeUISlotActiveArea slotState, int realSlotHeight, double startX, int slotWidth, Color indicatorColor, Color indicatorTextColor, int slotY, int indicatorY, int centerY) {

        final double originalStartX = startX;
        boolean hasMouseOver = currentActiveArea == slotState || (currentActiveArea instanceof JIPipeNodeUISlotButtonActiveArea && ((JIPipeNodeUISlotButtonActiveArea) currentActiveArea).getUISlot() == slotState);
        boolean hasButtonHover = (currentActiveArea instanceof JIPipeNodeUISlotButtonActiveArea && ((JIPipeNodeUISlotButtonActiveArea) currentActiveArea).getUISlot() == slotState);

        // Draw selection
//        g2.setStroke(STROKE_MOUSE_OVER);
//        g2.setPaint(nodeBorderColor);
//        g2.drawRect((int) Math.round(startX) + 1, slotY + 1,slotWidth - 3, realSlotHeight - 3);

        if (indicatorColor != null && !hasMouseOver) {
            g2.setPaint(indicatorColor);
            int x = (int) Math.round(startX + 2 * zoom);
            int width = slotWidth - (int) Math.round(2 * 2 * zoom);
            int height = (int) Math.round(2 * zoom);
            int arc = (int) Math.round(2 * zoom);
            g2.fillRoundRect(x, indicatorY, width, height, arc, arc);
        }
        startX += 8 * zoom;
        Image icon;

        if (hasMouseOver) {
            icon = UIUtils.getIconFromResources("actions/configure.png").getImage();
        } else {
            icon = slotState.getIcon();
        }
        if (hasButtonHover) {
            g2.setPaint(buttonFillColorDarker);
            g2.fillRoundRect((int) Math.round(startX + 2 * zoom), (int) Math.round(centerY - 9 * zoom), (int) Math.round(18 * zoom), (int) Math.round(18 * zoom), 1, 1);
        }
        g2.drawImage(icon, (int) Math.round(startX + 3 * zoom), (int) Math.round(centerY - 8 * zoom), (int) Math.round(16 * zoom), (int) Math.round(16 * zoom), null);
        if (hasMouseOver) {
            g2.setPaint(nodeBorderColor);
            g2.drawRoundRect((int) Math.round(startX + 2 * zoom), (int) Math.round(centerY - 9 * zoom), (int) Math.round(18 * zoom), (int) Math.round(18 * zoom), 1, 1);
        }

        startX += 22 * zoom;

        // Draw name
        if (indicatorTextColor != null) {
            g2.setPaint(indicatorTextColor);
        } else {
            g2.setPaint(mainTextColor);
        }
        FontMetrics fontMetrics = g2.getFontMetrics();
        drawStringVerticallyCentered(g2, slotState.getSlotLabel(), (int) Math.round(startX + 3 * zoom), (int) Math.round(centerY - 1 * zoom), fontMetrics);

        if(slotState.getSlotStatus() == SlotStatus.Cached) {
            startX =  originalStartX + slotWidth - 8 * zoom - 12 * zoom;
            g2.drawImage(UIUtils.getIconInverted12FromResources("actions/database.png").getImage(),
                    (int)Math.round(startX),
                    (int)Math.round(centerY - 6 * zoom),
                    (int) Math.round(12 * zoom),
                    (int) Math.round(12 * zoom),
                    null);
        }
    }

    private void paintNodeControls(Graphics2D g2, FontMetrics fontMetrics, int realSlotHeight, boolean hasInputs, boolean hasOutputs) {
        int centerY;
        if (hasInputs && !hasOutputs) {
            centerY = (getHeight() - realSlotHeight) / 2 + realSlotHeight;
        } else if (!hasInputs && hasOutputs) {
            centerY = (getHeight() - realSlotHeight) / 2;
        } else {
            centerY = getHeight() / 2;
        }
        {
            String nameLabel = node.getName();
            int centerNativeWidth = (int) Math.round((nodeIsRunnable ? 22 : 0) * zoom + 22 * zoom + fontMetrics.stringWidth(nameLabel));
            double startX = getWidth() / 2.0 - centerNativeWidth / 2.0;

            if (nodeIsRunnable) {
                boolean isButtonHighlighted = currentActiveArea instanceof JIPipeNodeUIRunNodeActiveArea;

                // Draw button
                g2.setPaint(isButtonHighlighted ? buttonFillColorDarker : buttonFillColor);
                g2.fillOval((int) Math.round(startX + 3 * zoom), (int) Math.round(centerY - 8 * zoom), (int) Math.round(16 * zoom), (int) Math.round(16 * zoom));
                g2.setPaint(nodeBorderColor);
                g2.drawOval((int) Math.round(startX + 3 * zoom), (int) Math.round(centerY - 8 * zoom), (int) Math.round(16 * zoom), (int) Math.round(16 * zoom));

                // Draw icon
                g2.setPaint(COLOR_RUN_BUTTON_ICON);
                g2.fillPolygon(new int[]{(int) Math.round(startX + (6 + 3) * zoom), (int) Math.round(startX + (13 + 3) * zoom), (int) Math.round(startX + (6 + 3) * zoom)},
                        new int[]{(int) Math.round(centerY - (5) * zoom), centerY, (int) Math.round(centerY + (5 + 1) * zoom)},
                        3);

                startX += 22 * zoom;
            }

            // Draw icon
            g2.drawImage(nodeIcon, (int) Math.round(startX + 3 * zoom), (int) Math.round(centerY - 8 * zoom), (int) Math.round(16 * zoom), (int) Math.round(16 * zoom), null);
            startX += 22 * zoom;

            // Draw name
            g2.setPaint(mainTextColor);
            drawStringVerticallyCentered(g2, node.getName(), (int) Math.round(startX + 3 * zoom), centerY, fontMetrics);
        }
    }

    private void drawStringVerticallyCentered(Graphics2D g2, String text, int x, int y, FontMetrics fontMetrics) {
        int metricHeight = fontMetrics.getAscent() - fontMetrics.getLeading();
        g2.drawString(text, x, y + metricHeight / 2);
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
        Point gridPoint = graphCanvasUI.getViewMode().realLocationToGrid(location, graphCanvasUI.getZoom());
        return moveToGridLocation(gridPoint, force, save);
    }

    public JIPipeNodeUISlotActiveArea pickSlotAtMousePosition(MouseEvent event) {
        MouseEvent converted = SwingUtilities.convertMouseEvent(getGraphCanvasUI(), event, this);
        Point mousePosition = converted.getPoint();
        for (JIPipeNodeUIActiveArea activeArea : activeAreas) {
            if (activeArea instanceof JIPipeNodeUISlotActiveArea) {
                if (activeArea.getZoomedHitArea().contains(mousePosition)) {
                    return (JIPipeNodeUISlotActiveArea) activeArea;
                }
            }
        }
        return null;
    }

    private void updateSlots() {
        JIPipeGraph graph = node.getParentGraph();

        inputSlotMap.clear();
        outputSlotMap.clear();

        for (JIPipeInputDataSlot inputSlot : node.getInputSlots()) {

            inputSlot.getInfo().getEventBus().register(this);

            JIPipeNodeUISlotActiveArea slotState = new JIPipeNodeUISlotActiveArea(this, JIPipeSlotType.Input, inputSlot.getName(), inputSlot);
            slotState.setIcon(JIPipe.getDataTypes().getIconFor(inputSlot.getAcceptedDataType()).getImage());
            inputSlotMap.put(inputSlot.getName(), slotState);
            if (StringUtils.isNullOrEmpty(inputSlot.getInfo().getCustomName())) {
                slotState.setSlotLabel(inputSlot.getName());
                slotState.setSlotLabelIsCustom(false);
            } else {
                slotState.setSlotLabel(inputSlot.getInfo().getCustomName());
                slotState.setSlotLabelIsCustom(true);
            }
            if (graph != null) {
                if (!inputSlot.getInfo().isOptional() && graph.getGraph().inDegreeOf(inputSlot) <= 0) {
                    slotState.setSlotStatus(SlotStatus.Unconnected);
                } else {
                    slotState.setSlotStatus(SlotStatus.Default);
                }
            }
        }

        Map<String, JIPipeDataTable> cachedData = null;
        if (graph != null && graph.getProject() != null) {
            cachedData = graph.getProject().getCache().query(node, node.getUUIDInParentGraph(), new JIPipeProgressInfo());
        }
        for (JIPipeDataSlot outputSlot : node.getOutputSlots()) {

            outputSlot.getInfo().getEventBus().register(this);

            JIPipeNodeUISlotActiveArea slotState = new JIPipeNodeUISlotActiveArea(this, JIPipeSlotType.Output, outputSlot.getName(), outputSlot);
            slotState.setIcon(JIPipe.getDataTypes().getIconFor(outputSlot.getAcceptedDataType()).getImage());
            outputSlotMap.put(outputSlot.getName(), slotState);
            if (StringUtils.isNullOrEmpty(outputSlot.getInfo().getCustomName())) {
                slotState.setSlotLabel(outputSlot.getName());
                slotState.setSlotLabelIsCustom(false);
            } else {
                slotState.setSlotLabel(outputSlot.getInfo().getCustomName());
                slotState.setSlotLabelIsCustom(true);
            }

            if (cachedData != null && cachedData.containsKey(outputSlot.getName())) {
                slotState.setSlotStatus(SlotStatus.Cached);
            } else {
                slotState.setSlotStatus(SlotStatus.Default);
            }
        }

        updateSize();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(currentActiveArea instanceof JIPipeNodeUISlotButtonActiveArea || currentActiveArea instanceof JIPipeNodeUISlotActiveArea && SwingUtilities.isRightMouseButton(e)) {
            JIPipeNodeUISlotActiveArea slotState;
            if(currentActiveArea instanceof JIPipeNodeUISlotButtonActiveArea) {
                slotState = ((JIPipeNodeUISlotButtonActiveArea) currentActiveArea).getUISlot();
            }
            else if(currentActiveArea instanceof JIPipeNodeUISlotActiveArea) {
                slotState = (JIPipeNodeUISlotActiveArea) currentActiveArea;
            }
            else {
                return;
            }
            openSlotMenu(slotState, e);
            e.consume();
        }
        else if(currentActiveArea instanceof JIPipeNodeUIAddSlotButtonActiveArea) {
            openAddSlotDialog(((JIPipeNodeUIAddSlotButtonActiveArea) currentActiveArea).getSlotType());
            e.consume();
        }
        else if(currentActiveArea instanceof JIPipeNodeUIRunNodeActiveArea) {
            openRunNodeMenu(e);
            e.consume();
        }
    }

    private void openRunNodeMenu(MouseEvent event) {
        JPopupMenu menu = new JPopupMenu();
        for (NodeUIContextAction entry : RUN_NODE_CONTEXT_MENU_ENTRIES) {
            if (entry == null)
                UIUtils.addSeparatorIfNeeded(menu);
            else {
                JMenuItem item = new JMenuItem(entry.getName(), entry.getIcon());
                item.setToolTipText(entry.getDescription());
                item.setAccelerator(entry.getKeyboardShortcut());
                item.addActionListener(e -> {
                    if (entry.matches(Collections.singleton(this))) {
                        entry.run(getGraphCanvasUI(), Collections.singleton(this));
                    } else {
                        JOptionPane.showMessageDialog(getWorkbench().getWindow(),
                                "Could not run this operation",
                                entry.getName(),
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
                menu.add(item);
            }
        }
        MouseEvent convertMouseEvent = SwingUtilities.convertMouseEvent(graphCanvasUI, event, this);
        Point mousePosition = convertMouseEvent.getPoint();
        menu.show(this, mousePosition.x, mousePosition.y);
    }

    private void openSlotMenu(JIPipeNodeUISlotActiveArea slotState, MouseEvent mouseEvent) {
        JIPipeDataSlot slot = slotState.getSlot();
        if(slot == null) {
            return;
        }
        JPopupMenu menu = new JPopupMenu();

        openSlotMenuGenerateInformationItems(slotState, slot, menu);

        UIUtils.addSeparatorIfNeeded(menu);

        // Connection menus
        if(slotState.isInput()) {
            openSlotMenuAddInputSlotMenuItems(slot, menu);
        }
        else {
            openSlotMenuAddOutputSlotMenuItems(slot, menu);
        }

        // Customization

        UIUtils.addSeparatorIfNeeded(menu);

        // Global actions at the end
        JMenuItem relabelButton = new JMenuItem("Label this slot", UIUtils.getIconFromResources("actions/tag.png"));
        relabelButton.setToolTipText("Sets a custom name for this slot without deleting it");
        relabelButton.addActionListener(e -> relabelSlot(slotState.getSlot()));
        menu.add(relabelButton);

        if ((slot.isInput() && getNode().getInputSlots().size() > 1) || (slot.isOutput() && getNode().getOutputSlots().size() > 1)) {
            JMenuItem moveUpButton = new JMenuItem("Move to the left",
                    UIUtils.getIconFromResources("actions/go-left.png"));
            moveUpButton.setToolTipText("Reorders the slots");
            moveUpButton.addActionListener(e -> moveSlotUp(slotState.getSlot()));
            menu.add(moveUpButton);

            JMenuItem moveDownButton = new JMenuItem("Move to the right",
                    UIUtils.getIconFromResources("actions/go-right.png"));
            moveDownButton.setToolTipText("Reorders the slots");
            moveDownButton.addActionListener(e -> moveSlotDown(slotState.getSlot()));
            menu.add(moveDownButton);
        }

        MouseEvent convertMouseEvent = SwingUtilities.convertMouseEvent(graphCanvasUI, mouseEvent, this);
        Point mousePosition = convertMouseEvent.getPoint();
        menu.show(this, mousePosition.x, mousePosition.y);
    }

    private void openSlotMenuAddOutputSlotMenuItems(JIPipeDataSlot slot, JPopupMenu menu) {
        Set<JIPipeDataSlot> targetSlots = getGraphCanvasUI().getGraph().getOutputOutgoingTargetSlots(slot);
        if (!targetSlots.isEmpty()) {

            boolean allowDisconnect = false;
            for (JIPipeDataSlot targetSlot : targetSlots) {
                if (getGraphCanvasUI().getGraph().canUserDisconnect(slot, targetSlot)) {
                    allowDisconnect = true;
                    break;
                }
            }

            if (allowDisconnect) {
                JMenuItem disconnectButton = new JMenuItem("Disconnect all", UIUtils.getIconFromResources("actions/cancel.png"));
                disconnectButton.addActionListener(e -> getGraphCanvasUI().disconnectAll(slot, targetSlots));
                openSlotMenuInstallHighlightForDisconnect(disconnectButton, targetSlots);
                menu.add(disconnectButton);

                UIUtils.addSeparatorIfNeeded(menu);
            }

            Set<JIPipeGraphEdge> hiddenEdges = new HashSet<>();
            Set<JIPipeGraphEdge> visibleEdges = new HashSet<>();
            for (JIPipeDataSlot targetSlot : targetSlots) {
                JIPipeGraphEdge edge = getGraphCanvasUI().getGraph().getGraph().getEdge(slot, targetSlot);
                if (edge.isUiHidden()) {
                    hiddenEdges.add(edge);
                } else {
                    visibleEdges.add(edge);
                }
            }

            if (!hiddenEdges.isEmpty()) {
                JMenuItem showButton = new JMenuItem("Show all outgoing edges", UIUtils.getIconFromResources("actions/eye.png"));
                showButton.setToolTipText("Un-hides all outgoing edges");
                showButton.addActionListener(e -> {
                    if (getGraphCanvasUI().getHistoryJournal() != null) {
                        getGraphCanvasUI().getHistoryJournal().snapshot("Un-hide edges",
                                "Shown all edges of slot " + slot.getDisplayName(),
                                slot.getNode().getCompartmentUUIDInParentGraph(),
                                UIUtils.getIconFromResources("actions/eye.png"));
                    }
                    for (JIPipeGraphEdge target : hiddenEdges) {
                        target.setUiHidden(false);
                    }
                    getGraphCanvasUI().repaint(50);
                });
                menu.add(showButton);
            }
            if (!visibleEdges.isEmpty()) {
                JMenuItem showButton = new JMenuItem("Hide all outgoing edges", UIUtils.getIconFromResources("actions/eye-slash.png"));
                showButton.setToolTipText("Hides all outgoing edges");
                showButton.addActionListener(e -> {
                    if (getGraphCanvasUI().getHistoryJournal() != null) {
                        getGraphCanvasUI().getHistoryJournal().snapshot("Hide edges",
                                "Hidden all edges of slot " + slot.getDisplayName(),
                                slot.getNode().getCompartmentUUIDInParentGraph(),
                                UIUtils.getIconFromResources("actions/eye-slash.png"));
                    }
                    for (JIPipeGraphEdge target : visibleEdges) {
                        target.setUiHidden(true);
                    }
                    getGraphCanvasUI().repaint(50);
                });
                menu.add(showButton);
            }
        }

        UUID compartment = graphCanvasUI.getCompartment();
        Set<JIPipeDataSlot> availableTargets = getGraphCanvasUI().getGraph().getAvailableTargets(slot, true, true);
        availableTargets.removeIf(s -> !s.getNode().isVisibleIn(compartment));

        JMenuItem findAlgorithmButton = new JMenuItem("Find matching algorithm ...", UIUtils.getIconFromResources("actions/find.png"));
        findAlgorithmButton.setToolTipText("Opens a tool to find a matching algorithm based on the data");
        findAlgorithmButton.addActionListener(e -> openOutputAlgorithmFinder(slot));
        menu.add(findAlgorithmButton);

        if(!availableTargets.isEmpty()) {
            JMenu connectMenu = new JMenu("Connect to ...");
            openSlotMenuAddOutputConnectTargetSlotItems(slot, availableTargets, connectMenu);
            menu.add(connectMenu);
        }
        if(!targetSlots.isEmpty()) {
            JMenu manageMenu = new JMenu("Manage existing connections ...");
            openSlotMenuAddOutputTargetSlotItems(slot, targetSlots, manageMenu);
            menu.add(manageMenu);
        }

        UIUtils.addSeparatorIfNeeded(menu);

        openSlotMenuAddOutputSlotEditItems(slot, menu);

        UIUtils.addSeparatorIfNeeded(menu);

        if (!(getNode() instanceof JIPipeProjectCompartment)) {
            if (slot.getInfo().isSaveOutputs()) {
                JMenuItem toggleSaveOutputsButton = new JMenuItem("Disable saving outputs", UIUtils.getIconFromResources("actions/no-save.png"));
                toggleSaveOutputsButton.setToolTipText("Makes that the data stored in this slot are not saved in a full analysis. Does not have an effect when updating the cache.");
                toggleSaveOutputsButton.addActionListener(e -> setSaveOutputs(slot, false));
                menu.add(toggleSaveOutputsButton);
            } else {
                JMenuItem toggleSaveOutputsButton = new JMenuItem("Enable saving outputs", UIUtils.getIconFromResources("actions/save.png"));
                toggleSaveOutputsButton.setToolTipText("Makes that the data stored in this slot are saved in a full analysis.");
                toggleSaveOutputsButton.addActionListener(e -> setSaveOutputs(slot, true));
                menu.add(toggleSaveOutputsButton);
            }

            if (slot.isNewDataVirtual()) {
                JMenuItem toggleVirtualButton = new JMenuItem("Store in memory", UIUtils.getIconFromResources("devices/media-memory.png"));
                toggleVirtualButton.setToolTipText("Makes that data generated by this slot is stored in memory during a full analysis. This raises the requirements on system memory significantly if the data is very large.");
                toggleVirtualButton.addActionListener(e -> makeNonVirtual(slot));
                menu.add(toggleVirtualButton);
            } else {
                JMenuItem toggleVirtualButton = new JMenuItem("Store on hard drive", UIUtils.getIconFromResources("actions/rabbitvcs-drive.png"));
                toggleVirtualButton.setToolTipText("Makes that data generated by this slot is stored in a temporary folder if not used during a full analysis. This is slower than memory access and requires disk space on the hard drive. Useful if you have large amounts of data. Only takes effect if you enable 'Reduce memory'.");
                toggleVirtualButton.addActionListener(e -> makeVirtual(slot));
                menu.add(toggleVirtualButton);
            }
        }

    }

    private void openSlotMenuAddOutputTargetSlotItems(JIPipeDataSlot slot, Set<JIPipeDataSlot> targetSlots, JMenu menu) {
        for (JIPipeDataSlot targetSlot : sortSlotsByDistance(slot, targetSlots)) {
            JMenu targetSlotMenu = new JMenu(targetSlot.getDisplayName());
            targetSlotMenu.setIcon(JIPipe.getDataTypes().getIconFor(targetSlot.getAcceptedDataType()));

            JMenuItem disconnectButton = new JMenuItem("Disconnect", UIUtils.getIconFromResources("actions/cancel.png"));
            disconnectButton.addActionListener(e -> getGraphCanvasUI().disconnectAll(slot, Collections.singleton(targetSlot)));
            openSlotMenuInstallHighlightForDisconnect(disconnectButton, Collections.singleton(targetSlot));
            targetSlotMenu.add(disconnectButton);

            JIPipeGraphEdge edge = getGraphCanvasUI().getGraph().getGraph().getEdge(slot, targetSlot);
            if (edge.isUiHidden()) {
                JMenuItem showButton = new JMenuItem("Show outgoing edge", UIUtils.getIconFromResources("actions/eye.png"));
                showButton.setToolTipText("Un-hides the outgoing edge");
                showButton.addActionListener(e -> {
                    if (getGraphCanvasUI().getHistoryJournal() != null) {
                        getGraphCanvasUI().getHistoryJournal().snapshot("Un-hide edge",
                                "Shown the edge " + slot.getDisplayName() + " -> " + targetSlot.getDisplayName(),
                                getNode().getCompartmentUUIDInParentGraph(),
                                UIUtils.getIconFromResources("actions/eye.png"));
                    }
                    edge.setUiHidden(false);
                    getGraphCanvasUI().repaint(50);
                });
                openSlotMenuInstallHighlightForConnect(targetSlot, showButton);
                targetSlotMenu.add(showButton);
            } else {
                JMenuItem hideButton = new JMenuItem("Hide outgoing edge", UIUtils.getIconFromResources("actions/eye-slash.png"));
                hideButton.setToolTipText("Hides the outgoing edge");
                hideButton.addActionListener(e -> {
                    if (getGraphCanvasUI().getHistoryJournal() != null) {
                        getGraphCanvasUI().getHistoryJournal().snapshot("Hide edge",
                                "Hidden the edge " + slot.getDisplayName() + " -> " + targetSlot.getDisplayName(),
                                getNode().getCompartmentUUIDInParentGraph(),
                                UIUtils.getIconFromResources("actions/eye-slash.png"));
                    }
                    edge.setUiHidden(true);
                    getGraphCanvasUI().repaint(50);
                });
                openSlotMenuInstallHighlightForDisconnect(hideButton, Collections.singleton(targetSlot));
                targetSlotMenu.add(hideButton);
            }
            openSlotMenuAddShapeToggle(slot, targetSlotMenu, edge);
            menu.add(targetSlotMenu);
        }
    }

    private void openSlotMenuAddInputSlotMenuItems(JIPipeDataSlot slot, JPopupMenu menu) {

        JMenuItem findAlgorithmButton = new JMenuItem("Find matching algorithm ...", UIUtils.getIconFromResources("actions/find.png"));
        findAlgorithmButton.setToolTipText("Opens a tool to find a matching algorithm based on the data");
        findAlgorithmButton.addActionListener(e -> openInputAlgorithmFinder(slot));
        menu.add(findAlgorithmButton);

        Set<JIPipeDataSlot> availableSources = getGraphCanvasUI().getGraph().getAvailableSources(slot, true, false);
        if(!availableSources.isEmpty()) {
            JMenu connectMenu = new JMenu("Connect to ...");
            openSlotMenuAddInputConnectSourceSloItems(slot, availableSources, connectMenu);
            menu.add(connectMenu);
        }

        Set<JIPipeDataSlot> sourceSlots = getGraphCanvasUI().getGraph().getInputIncomingSourceSlots(slot);

        if (!sourceSlots.isEmpty()) {
            JMenuItem disconnectButton = new JMenuItem("Disconnect all", UIUtils.getIconFromResources("actions/cancel.png"));
            disconnectButton.addActionListener(e -> getGraphCanvasUI().disconnectAll(slot, sourceSlots));
            openSlotMenuInstallHighlightForDisconnect(disconnectButton, sourceSlots);
            menu.add(disconnectButton);
        }

        UIUtils.addSeparatorIfNeeded(menu);

        openSlotMenuAddInputSourceSlotItems(slot, sourceSlots, menu);

        UIUtils.addSeparatorIfNeeded(menu);

        openSlotMenuAddInputSlotEditItems(slot, sourceSlots, menu);
    }

    private void openSlotMenuAddOutputConnectTargetSlotItems(JIPipeDataSlot slot, Set<JIPipeDataSlot> availableTargets, JMenu menu) {
        Object currentMenu = menu;
        int itemCount = 0;
        for (JIPipeDataSlot target : sortSlotsByDistance(slot, availableTargets)) {
            if (itemCount >= 6) {
                JMenu moreMenu = new JMenu("More targets ...");
                if (currentMenu instanceof JMenu)
                    ((JMenu) currentMenu).add(moreMenu);
                else
                    ((JPopupMenu) currentMenu).add(moreMenu);
                currentMenu = moreMenu;
                itemCount = 0;
            }
            JMenuItem connectButton = new JMenuItem("<html>" + target.getNode().getName() + "<br/><small>" + target.getName() + "</html>",
                    JIPipe.getDataTypes().getIconFor(target.getAcceptedDataType()));
            connectButton.addActionListener(e -> getGraphCanvasUI().connectSlot(slot, target));
            connectButton.setToolTipText(TooltipUtils.getAlgorithmTooltip(target.getNode().getInfo()));
//            connectButton.addMouseListener(new MouseAdapter() {
//                @Override
//                public void mouseEntered(MouseEvent e) {
//                    JIPipeNodeUI targetNodeUI = getGraphUI().getNodeUIs().getOrDefault(target.getNode(), null);
//                    if (targetNodeUI != null) {
//                        if (target.isInput()) {
//                            JIPipeDataSlotUI targetUI = targetNodeUI.getInputSlotUIs().getOrDefault(target.getName(), null);
//                            if (targetUI != null) {
//                                getGraphUI().setCurrentConnectionDragTarget(targetUI);
//                                getGraphUI().setCurrentConnectionDragSource(JIPipeDataSlotUI.this);
//                            }
//                        }
//                    }
//                    getGraphUI().repaint(50);
//                }
//
//                @Override
//                public void mouseExited(MouseEvent e) {
//                    getGraphUI().setCurrentConnectionDragSource(null);
//                    getGraphUI().setCurrentConnectionDragTarget(null);
//                    getGraphUI().repaint(50);
//                }
//            });
            if (currentMenu instanceof JMenu)
                ((JMenu) currentMenu).add(connectButton);
            else
                ((JPopupMenu) currentMenu).add(connectButton);
            ++itemCount;
        }
    }

    private void openSlotMenuAddInputSlotEditItems(JIPipeDataSlot slot, Set<JIPipeDataSlot> sourceSlots, JPopupMenu menu) {
        if (slot.getInfo().isUserModifiable() && slot.getNode().getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
            JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) slot.getNode().getSlotConfiguration();
            if (slotConfiguration.canModifyInputSlots()) {
                UIUtils.addSeparatorIfNeeded(menu);
                JMenuItem deleteButton = new JMenuItem("Delete this slot", UIUtils.getIconFromResources("actions/delete.png"));
                deleteButton.addActionListener(e -> deleteSlot(slot));
                openSlotMenuInstallHighlightForDisconnect(deleteButton, sourceSlots);
                menu.add(deleteButton);

                JMenuItem editButton = new JMenuItem("Edit this slot", UIUtils.getIconFromResources("actions/edit.png"));
                editButton.addActionListener(e -> editSlot(slot));
                menu.add(editButton);
            }
        }
    }

    private List<JIPipeDataSlot> sortSlotsByDistance(JIPipeDataSlot slot, Set<JIPipeDataSlot> unsorted) {
        Point thisLocation = getGraphCanvasUI().getSlotLocation(slot);
        if (thisLocation == null)
            return new ArrayList<>(unsorted);
        Map<JIPipeDataSlot, Double> distances = new HashMap<>();
        for (JIPipeDataSlot dataSlot : unsorted) {
            Point location = getGraphCanvasUI().getSlotLocation(dataSlot);
            if (location != null) {
                distances.put(dataSlot, Math.pow(location.x - thisLocation.x, 2) + Math.pow(location.y - thisLocation.y, 2));
            } else {
                distances.put(dataSlot, Double.POSITIVE_INFINITY);
            }
        }
        return unsorted.stream().sorted(Comparator.comparing(distances::get)).collect(Collectors.toList());
    }

    private void openSlotMenuAddInputSourceSlotItems(JIPipeDataSlot slot, Set<JIPipeDataSlot> sourceSlots, JPopupMenu menu) {
        JPopupMenu currentMenu = menu;
        for (JIPipeDataSlot sourceSlot : sortSlotsByDistance(slot, sourceSlots)) {
            JMenu sourceSlotMenu = new JMenu(sourceSlot.getDisplayName());
            sourceSlotMenu.setIcon(JIPipe.getDataTypes().getIconFor(sourceSlot.getAcceptedDataType()));

            JMenuItem disconnectButton = new JMenuItem("Disconnect", UIUtils.getIconFromResources("actions/cancel.png"));
            disconnectButton.addActionListener(e -> getGraphCanvasUI().disconnectAll(slot, Collections.singleton(sourceSlot)));
            openSlotMenuInstallHighlightForDisconnect(disconnectButton, Collections.singleton(sourceSlot));
            sourceSlotMenu.add(disconnectButton);

            JIPipeGraphEdge edge = getGraphCanvasUI().getGraph().getGraph().getEdge(sourceSlot, slot);
            if (edge.isUiHidden()) {
                JMenuItem showButton = new JMenuItem("Show incoming edge", UIUtils.getIconFromResources("actions/eye.png"));
                showButton.setToolTipText("Un-hides the incoming edge");
                showButton.addActionListener(e -> {
                    if (getGraphCanvasUI().getHistoryJournal() != null) {
                        getGraphCanvasUI().getHistoryJournal().snapshot("Un-hide edge",
                                "Shown the edge " + sourceSlot.getDisplayName() + " -> " + slot.getDisplayName(),
                                getNode().getCompartmentUUIDInParentGraph(),
                                UIUtils.getIconFromResources("actions/eye.png"));
                    }
                    edge.setUiHidden(false);
                    getGraphCanvasUI().repaint(50);
                });
                openSlotMenuInstallHighlightForConnect(sourceSlot, showButton);
                sourceSlotMenu.add(showButton);
            } else {
                JMenuItem hideButton = new JMenuItem("Hide incoming edge", UIUtils.getIconFromResources("actions/eye-slash.png"));
                hideButton.setToolTipText("Hides the incoming edge");
                hideButton.addActionListener(e -> {
                    if (getGraphCanvasUI().getHistoryJournal() != null) {
                        getGraphCanvasUI().getHistoryJournal().snapshot("Hide edge",
                                "Hidden the edge " + sourceSlot.getDisplayName() + " -> " + slot.getDisplayName(),
                                getNode().getCompartmentUUIDInParentGraph(),
                                UIUtils.getIconFromResources("actions/eye-slash.png"));
                    }
                    edge.setUiHidden(true);
                    getGraphCanvasUI().repaint(50);
                });
                openSlotMenuInstallHighlightForDisconnect(hideButton, Collections.singleton(sourceSlot));
                sourceSlotMenu.add(hideButton);
            }
            openSlotMenuAddShapeToggle(slot, sourceSlotMenu, edge);
            currentMenu.add(sourceSlotMenu);
        }
    }

    private void openSlotMenuAddInputConnectSourceSloItems(JIPipeDataSlot slot, Set<JIPipeDataSlot> availableSources, JMenu menu) {
        UUID compartment = graphCanvasUI.getCompartment();
        availableSources.removeIf(s -> !s.getNode().isVisibleIn(compartment));

        Object currentMenu = menu;
        int itemCount = 0;
        for (JIPipeDataSlot source : sortSlotsByDistance(slot, availableSources)) {
            if (!source.getNode().isVisibleIn(compartment))
                continue;
            if (itemCount >= 6) {
                JMenu moreMenu = new JMenu("More sources ...");
                if (currentMenu instanceof JMenu)
                    ((JMenu) currentMenu).add(moreMenu);
                else
                    ((JPopupMenu) currentMenu).add(moreMenu);
                currentMenu = moreMenu;
                itemCount = 0;
            }
            JMenuItem connectButton = new JMenuItem("<html>" + source.getNode().getName() + "<br/><small>" + source.getName() + "</html>",
                    JIPipe.getDataTypes().getIconFor(source.getAcceptedDataType()));
            connectButton.addActionListener(e -> getGraphCanvasUI().connectSlot(source, slot));
            openSlotMenuInstallHighlightForConnect(source, connectButton);
            if (currentMenu instanceof JMenu)
                ((JMenu) currentMenu).add(connectButton);
            else
                ((JPopupMenu) currentMenu).add(connectButton);
            ++itemCount;
        }
    }

    private void openSlotMenuAddShapeToggle(JIPipeDataSlot slot, JMenu menu, JIPipeGraphEdge edge) {
        UIUtils.addSeparatorIfNeeded(menu);
        if (edge.getUiShape() != JIPipeGraphEdge.Shape.Elbow) {
            JMenuItem setShapeItem = new JMenuItem("Draw as elbow", UIUtils.getIconFromResources("actions/standard-connector.png"));
            setShapeItem.addActionListener(e -> {
                if (getGraphCanvasUI().getHistoryJournal() != null) {
                    getGraphCanvasUI().getHistoryJournal().snapshot("Draw edge as elbow",
                            slot.getDisplayName(),
                            getNode().getCompartmentUUIDInParentGraph(),
                            UIUtils.getIconFromResources("actions/standard-connector.png"));
                }
                edge.setUiShape(JIPipeGraphEdge.Shape.Elbow);
                getGraphCanvasUI().repaint(50);
            });
            menu.add(setShapeItem);
        }
        if (edge.getUiShape() != JIPipeGraphEdge.Shape.Line) {
            JMenuItem setShapeItem = new JMenuItem("Draw as line", UIUtils.getIconFromResources("actions/draw-line.png"));
            setShapeItem.addActionListener(e -> {
                if (getGraphCanvasUI().getHistoryJournal() != null) {
                    getGraphCanvasUI().getHistoryJournal().snapshot("Draw edge as line",
                            slot.getDisplayName(),
                            getNode().getCompartmentUUIDInParentGraph(),
                            UIUtils.getIconFromResources("actions/draw-line.png"));
                }
                edge.setUiShape(JIPipeGraphEdge.Shape.Line);
                getGraphCanvasUI().repaint(50);
            });
            menu.add(setShapeItem);
        }
    }

    private void openSlotMenuAddOutputSlotEditItems(JIPipeDataSlot slot, JPopupMenu menu) {
        if (slot.getInfo().isUserModifiable() && slot.getNode().getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
            JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) slot.getNode().getSlotConfiguration();
            if (slotConfiguration.canModifyOutputSlots()) {
                UIUtils.addSeparatorIfNeeded(menu);

                JMenuItem deleteButton = new JMenuItem("Delete this slot", UIUtils.getIconFromResources("actions/delete.png"));
                deleteButton.addActionListener(e -> deleteSlot(slot));
                menu.add(deleteButton);

                JMenuItem editButton = new JMenuItem("Edit this slot", UIUtils.getIconFromResources("actions/edit.png"));
                editButton.addActionListener(e -> editSlot(slot));
                menu.add(editButton);
            }
        }
    }

    private void openSlotMenuInstallHighlightForConnect(JIPipeDataSlot source, JMenuItem connectButton) {
        // TODO
//        connectButton.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseEntered(MouseEvent e) {
//                JIPipeNodeUI sourceNodeUI = getGraphUI().getNodeUIs().getOrDefault(source.getNode(), null);
//                if (sourceNodeUI != null) {
//                    if (source.isOutput()) {
//                        JIPipeDataSlotUI sourceUI = sourceNodeUI.getOutputSlotUIs().getOrDefault(source.getName(), null);
//                        if (sourceUI != null) {
//                            getGraphUI().setCurrentConnectionDragTarget(JIPipeDataSlotUI.this);
//                            getGraphUI().setCurrentConnectionDragSource(sourceUI);
//                        }
//                    }
//                }
//                getGraphUI().repaint(50);
//            }
//
//            @Override
//            public void mouseExited(MouseEvent e) {
//                getGraphUI().setCurrentConnectionDragSource(null);
//                getGraphUI().setCurrentConnectionDragTarget(null);
//                getGraphUI().repaint(50);
//            }
//        });
    }

    private void openSlotMenuInstallHighlightForDisconnect(JMenuItem disconnectButton, Set<JIPipeDataSlot> sourceSlots) {
        // TODO
//        disconnectButton.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseEntered(MouseEvent e) {
//                getGraphUI().setCurrentHighlightedForDisconnect(JIPipeDataSlotUI_old.this, sourceSlots);
//                getGraphUI().repaint(50);
//            }
//
//            @Override
//            public void mouseExited(MouseEvent e) {
//                getGraphUI().setCurrentHighlightedForDisconnect(null, Collections.emptySet());
//                getGraphUI().repaint(50);
//            }
//        });
    }


    private void setSaveOutputs(JIPipeDataSlot slot, boolean saveOutputs) {
        slot.getInfo().setSaveOutputs(saveOutputs);
    }

    private void makeVirtual(JIPipeDataSlot slot) {
        slot.getInfo().setVirtual(true);
        slot.setNewDataVirtual(true);
    }

    private void makeNonVirtual(JIPipeDataSlot slot) {
        slot.getInfo().setVirtual(false);
        slot.setNewDataVirtual(false);
    }

    private void openSlotMenuGenerateInformationItems(JIPipeNodeUISlotActiveArea slotState, JIPipeDataSlot slot, JPopupMenu menu) {
        // Information item
        JIPipeDataInfo dataInfo = JIPipeDataInfo.getInstance(slot.getAcceptedDataType());
        ViewOnlyMenuItem infoItem = new ViewOnlyMenuItem("<html>" + dataInfo.getName() + "<br><small>" + dataInfo.getDescription() + "</small></html>", JIPipe.getDataTypes().getIconFor(slot.getAcceptedDataType()));
        menu.add(infoItem);

        // Cache info
        if(node.getParentGraph() != null) {
            JIPipeGraph graph = node.getParentGraph();
            Map<String, JIPipeDataTable> cachedData = null;
            if (graph != null && graph.getProject() != null) {
                cachedData = graph.getProject().getCache().query(node, node.getUUIDInParentGraph(), new JIPipeProgressInfo());
            }
            if(cachedData != null && cachedData.containsKey(slotState.getSlotName())) {
                int itemCount = cachedData.get(slotState.getSlotName()).getRowCount();
                ViewOnlyMenuItem cacheInfoItem = new ViewOnlyMenuItem("<html>Outputs are cached<br/><small>" + (itemCount == 1 ? "1 item" : itemCount + " items") + " </small></html>",
                        UIUtils.getIconFromResources("actions/database.png"));
                menu.add(cacheInfoItem);
            }
        }
    }

    private void editSlot(JIPipeDataSlot slot) {
        if (!JIPipeProjectWorkbench.canModifySlots(getWorkbench()))
            return;
        EditAlgorithmSlotPanel.showDialog(this, getGraphCanvasUI().getHistoryJournal(), slot);
    }

    private void relabelSlot(JIPipeDataSlot slot) {
        String newLabel = JOptionPane.showInputDialog(this,
                "Please enter a new label for the slot.\nLeave the text empty to remove an existing label.",
                slot.getInfo().getCustomName());
        if (newLabel == null)
            return;
        if (getGraphCanvasUI().getHistoryJournal() != null) {
            getGraphCanvasUI().getHistoryJournal().snapshotBeforeLabelSlot(slot, slot.getNode().getCompartmentUUIDInParentGraph());
        }
        slot.getInfo().setCustomName(newLabel);
        getGraphCanvasUI().getWorkbench().setProjectModified(true);
    }

    private void deleteSlot(JIPipeDataSlot slot) {
        if (!JIPipeProjectWorkbench.canModifySlots(getWorkbench()))
            return;
        JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) slot.getNode().getSlotConfiguration();
        if (getGraphCanvasUI().getHistoryJournal() != null) {
            getGraphCanvasUI().getHistoryJournal().snapshotBeforeRemoveSlot(slot.getNode(), slot.getInfo(), slot.getNode().getCompartmentUUIDInParentGraph());
        }
        if (slot.isInput())
            slotConfiguration.removeInputSlot(slot.getName(), true);
        else if (slot.isOutput())
            slotConfiguration.removeOutputSlot(slot.getName(), true);
    }

    private void moveSlotDown(JIPipeDataSlot slot) {
        if (slot != null) {
            if (getGraphCanvasUI().getHistoryJournal() != null) {
                getGraphCanvasUI().getHistoryJournal().snapshotBeforeMoveSlot(slot, slot.getNode().getCompartmentUUIDInParentGraph());
            }
            ((JIPipeMutableSlotConfiguration) getNode().getSlotConfiguration()).moveDown(slot.getName(), slot.getSlotType());
            getGraphCanvasUI().repaint(50);
        }
    }

    private void moveSlotUp(JIPipeDataSlot slot) {
        if (slot != null) {
            if (getGraphCanvasUI().getHistoryJournal() != null) {
                getGraphCanvasUI().getHistoryJournal().snapshotBeforeMoveSlot(slot, slot.getNode().getCompartmentUUIDInParentGraph());
            }
            ((JIPipeMutableSlotConfiguration) getNode().getSlotConfiguration()).moveUp(slot.getName(), slot.getSlotType());
            getGraphCanvasUI().repaint(50);
        }
    }

    private void openOutputAlgorithmFinder(JIPipeDataSlot slot) {
        JIPipeAlgorithmTargetFinderUI algorithmFinderUI = new JIPipeAlgorithmTargetFinderUI(getGraphCanvasUI(), slot);
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Find matching algorithm");
        UIUtils.addEscapeListener(dialog);
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(algorithmFinderUI);
        dialog.pack();
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(this);

        algorithmFinderUI.getEventBus().register(new Consumer<AlgorithmFinderSuccessEvent>() {
            @Override
            @Subscribe
            public void accept(AlgorithmFinderSuccessEvent event) {
                dialog.dispose();
            }
        });
        boolean layoutHelperEnabled = getGraphCanvasUI().getSettings() != null && getGraphCanvasUI().getSettings().isLayoutAfterAlgorithmFinder();
        if (layoutHelperEnabled) {
            Point cursorLocation = new Point();
            Point slotLocation = getSlotLocation(slot).min;
            cursorLocation.x = getX() + slotLocation.x;
            cursorLocation.y = getBottomY() + getGraphCanvasUI().getViewMode().getGridHeight();
            getGraphCanvasUI().setGraphEditCursor(cursorLocation);
            getGraphCanvasUI().repaint(50);
        }

        dialog.setVisible(true);
    }

    private void openInputAlgorithmFinder(JIPipeDataSlot slot) {
        JIPipeAlgorithmSourceFinderUI algorithmFinderUI = new JIPipeAlgorithmSourceFinderUI(getGraphCanvasUI(), slot);
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Find matching algorithm");
        UIUtils.addEscapeListener(dialog);
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(algorithmFinderUI);
        dialog.pack();
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(this);

        algorithmFinderUI.getEventBus().register(new Consumer<AlgorithmFinderSuccessEvent>() {
            @Override
            @Subscribe
            public void accept(AlgorithmFinderSuccessEvent event) {
                dialog.dispose();
            }
        });
        boolean layoutHelperEnabled = getGraphCanvasUI().getSettings() != null && getGraphCanvasUI().getSettings().isLayoutAfterAlgorithmFinder();
        if (layoutHelperEnabled) {
            Point cursorLocation = new Point();
            Point slotLocation = getSlotLocation(slot).min;
            cursorLocation.x = getX() + slotLocation.x;
            cursorLocation.y = getY() - getGraphCanvasUI().getViewMode().getGridHeight() * 4;
            getGraphCanvasUI().setGraphEditCursor(cursorLocation);
            getGraphCanvasUI().repaint(50);
        }

        dialog.setVisible(true);
    }

    private void openAddSlotDialog(JIPipeSlotType slotType) {
        if (!JIPipeProjectWorkbench.canModifySlots(getWorkbench())) {
            JOptionPane.showMessageDialog(graphCanvasUI.getWorkbench().getWindow(), "");
            return;
        }
        AddAlgorithmSlotPanel.showDialog(this, graphCanvasUI.getHistoryJournal(), node, slotType);
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        mouseIsEntered = true;
        updateCurrentActiveArea(e);
        repaint(50);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseIsEntered = false;
        updateCurrentActiveArea(e);
        repaint(50);
    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        updateCurrentActiveArea(e);
    }

    @Subscribe
    public void onCacheModified(JIPipeCache.ModifiedEvent event) {
        updateView(false, true, false);
    }

    private void updateCurrentActiveArea(MouseEvent e) {
        if (mouseIsEntered) {
            MouseEvent mouseEvent = SwingUtilities.convertMouseEvent(getGraphCanvasUI(), e, this);
            Point mousePosition = mouseEvent.getPoint();
            for (JIPipeNodeUIActiveArea activeArea : activeAreas) {
                if (activeArea.getZoomedHitArea() != null && activeArea.getZoomedHitArea().contains(mousePosition)) {
                    if (currentActiveArea != activeArea) {
                        currentActiveArea = activeArea;
                        repaint(50);
                    }
                    break;
                }
            }
        } else {
            if (currentActiveArea != null) {
                currentActiveArea = null;
                repaint(50);
            }
        }
    }

    public enum SlotStatus {
        Default,
        Unconnected,
        Cached
    }

    /**
     * An event around {@link JIPipeNodeUI}
     */
    public static class AlgorithmEvent {
        private final JIPipeNodeUI ui;

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
