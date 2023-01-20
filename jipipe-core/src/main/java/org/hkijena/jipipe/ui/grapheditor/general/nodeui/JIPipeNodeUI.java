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
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.AddAlgorithmSlotPanel;
import org.hkijena.jipipe.ui.components.icons.ZoomIcon;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphViewMode;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.contextmenu.*;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.PointRange;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.*;
import java.util.List;

/**
 * UI around an {@link JIPipeGraphNode} instance
 */
public class JIPipeNodeUI extends JIPipeWorkbenchPanel implements MouseListener, MouseMotionListener {

    private static final String ADD_SLOT_BUTTON_NAME = "{{+}}";
    public static final Stroke STROKE_MOUSE_OVER = new BasicStroke(2);
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

    private final JIPipeGraphCanvasUI graphUI;
    private final JIPipeGraphNode node;
    private final EventBus eventBus = new EventBus();
    private final Color nodeFillColor;
    private final Color nodeBorderColor;
    private final LinearGradientPaint nodeDisabledPaint;
    private final LinearGradientPaint nodePassThroughPaint;
    private final Color slotFillColor;
    private final Color buttonFillColor;
    private final boolean slotsInputsEditable;
    private final boolean slotsOutputsEditable;
    private final Map<String, SlotState> inputSlotStatusMap = new HashMap<>();
    private final Map<String, SlotState> outputSlotStatusMap = new HashMap<>();
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

    private final Set<ActiveArea> activeAreaSet = new TreeSet<>();

    /**
     * Creates a new UI
     *
     * @param workbench thr workbench
     * @param graphUI   The graph UI that contains this UI
     * @param node      The algorithm
     */
    public JIPipeNodeUI(JIPipeWorkbench workbench, JIPipeGraphCanvasUI graphUI, JIPipeGraphNode node) {
        super(workbench);
        this.graphUI = graphUI;
        this.node = node;
        this.node.getEventBus().register(this);

        // Node information
        nodeIsRunnable = node.getInfo().isRunnable() || node instanceof JIPipeAlgorithm || node instanceof JIPipeProjectCompartment;

        // Slot information
        if(node.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
            slotsInputsEditable = ((JIPipeMutableSlotConfiguration) node.getSlotConfiguration()).canModifyInputSlots();
            slotsOutputsEditable = ((JIPipeMutableSlotConfiguration) node.getSlotConfiguration()).canModifyOutputSlots();
        }
        else {
            slotsInputsEditable = false;
            slotsOutputsEditable = false;
        }

        // Generate colors, icons
        this.nodeIcon = JIPipe.getNodes().getIconFor(node.getInfo()).getImage();
        this.nodeFillColor = UIUtils.getFillColorFor(node.getInfo());
        this.nodeBorderColor = UIUtils.getBorderColorFor(node.getInfo());
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

        // Initialization
        initialize();
        updateView(true, true, true);
    }

    private void initialize() {
        setBackground(getFillColor());
        setBorder(BorderFactory.createLineBorder(getBorderColor()));
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
        if(assets) {
            updateAssets();
        }
        if(slots) {
            updateSlots();
        }
        if(slots || size) {
            updateSize();
            getGraphUI().repaint(50);
        }
        updateActivateAreas();
    }

    private void updateActivateAreas() {
        activeAreaSet.clear();

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
            AddAlgorithmSlotPanel.showDialog(this, graphUI.getHistoryJournal(), node, slotType);
        });

        return addSlotButton;
    }

    public void updateHotkeyInfo() {
        // TODO
    }

    /**
     * Updates the slots
     */
    public void updateAlgorithmSlotUIs() {
        // TODO
    }

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
    public void onNodeSlotsChanged(JIPipeGraph.NodeSlotsChangedEvent event) {
        updateView(false, true, true);
    }

    /**
     * Should be triggered when an algorithm's name parameter is changed
     *
     * @param event The generated event
     */
    @Subscribe
    public void onAlgorithmParametersChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (event.getSource() == node && "jipipe:node:name".equals(event.getKey())) {
            updateView(false, false, true);
        } else if (event.getSource() == node && "jipipe:algorithm:enabled".equals(event.getKey())) {
            getGraphUI().repaint(50);
        } else if (event.getSource() == node && "jipipe:algorithm:pass-through".equals(event.getKey())) {
            getGraphUI().repaint(50);
        } else if (event.getSource() == node && "jipipe:node:bookmarked".equals(event.getKey())) {
            getGraphUI().repaint(50);
        }
    }

    /**
     * Recalculates the UI size
     */
    private void updateSize() {

        FontMetrics mainFontMetrics;
        FontMetrics secondaryFontMetrics;

        if(getGraphics() != null) {
            mainFontMetrics = getGraphics().getFontMetrics(nativeMainFont);
            secondaryFontMetrics = getGraphics().getFontMetrics(nativeSecondaryFont);
        }
        else {
            Canvas c = new Canvas();
            mainFontMetrics = c.getFontMetrics(nativeMainFont);
            secondaryFontMetrics = c.getFontMetrics(nativeSecondaryFont);
        }

        double mainWidth = (nodeIsRunnable ? 22 : 0) + 22 + mainFontMetrics.stringWidth(node.getName()) + 16;

        // Slot widths
        double sumInputSlotWidths = 0;
        double sumOutputSlotWidths = 0;

        if(slotsInputsEditable) {
            SlotState slotState = inputSlotStatusMap.get(ADD_SLOT_BUTTON_NAME);
            double nativeWidth = 22;
            sumInputSlotWidths += nativeWidth;
            slotState.setNativeWidth(nativeWidth);
        }
        for (JIPipeInputDataSlot inputSlot : node.getInputSlots()) {
            SlotState slotState = inputSlotStatusMap.get(inputSlot.getName());
            double nativeWidth = secondaryFontMetrics.stringWidth(slotState.getSlotLabel()) + 22 * 2 + 16;
            slotState.setNativeWidth(nativeWidth);
            sumInputSlotWidths += nativeWidth;
        }

        if(slotsOutputsEditable) {
            SlotState slotState = outputSlotStatusMap.get(ADD_SLOT_BUTTON_NAME);
            double nativeWidth = 22;
            sumOutputSlotWidths += nativeWidth;
            slotState.setNativeWidth(nativeWidth);
        }
        for (JIPipeDataSlot outputSlots : node.getOutputSlots()) {
            SlotState slotState = outputSlotStatusMap.get(outputSlots.getName());
            double nativeWidth = secondaryFontMetrics.stringWidth(slotState.getSlotLabel()) + 22 * 2 + 16;
            slotState.setNativeWidth(nativeWidth);
            sumOutputSlotWidths += nativeWidth;
        }

        // Calculate the grid width
        double maxWidth = Math.max(mainWidth, Math.max(sumInputSlotWidths, sumOutputSlotWidths));
        int gridWidth = (int) Math.ceil(maxWidth / viewMode.getGridWidth());

        // Correct the slot width to fit the actual native width of the control
        int nativeWidth = viewMode.getGridWidth() * gridWidth;

        scaleSlotsNativeWidth(inputSlotStatusMap, nativeWidth, sumInputSlotWidths, slotsInputsEditable);
        scaleSlotsNativeWidth(outputSlotStatusMap, nativeWidth, sumOutputSlotWidths, slotsOutputsEditable);

        // Update the real size of the control
        Dimension gridSize = new Dimension(gridWidth, 3);
        Dimension realSize = viewMode.gridToRealSize(gridSize, zoom);
        setSize(realSize);
        revalidate();
    }

    private void scaleSlotsNativeWidth(Map<String, SlotState> slotStateMap, double nodeWidth, double sumWidth, boolean hasButton) {
//        if(slotStateMap.size() == 1 && hasButton) {
//            return;
//        }
        boolean excludeButton = false;
        if(slotStateMap.size() > 1 && hasButton) {
            nodeWidth -= 22;
            sumWidth -= 22;
            excludeButton = true;
        }
        double factor = nodeWidth / sumWidth;
        for (SlotState slotState : slotStateMap.values()) {
            if(excludeButton && ADD_SLOT_BUTTON_NAME.equals(slotState.slotName))
                continue;
            slotState.nativeWidth *= factor;
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
        if (location.x != getX() || location.y != getY()) {
            setLocation(location);
        }
        if (save) {
            node.setLocationWithin(graphUI.getCompartment(), gridLocation, graphUI.getViewMode().name());
            getGraphUI().getWorkbench().setProjectModified(true);
        }
        return true;
    }

    public Point getStoredGridLocation() {
        return node.getLocationWithin(StringUtils.nullToEmpty(graphUI.getCompartment()), graphUI.getViewMode().name());
    }

    /**
     * Moves the UI back to the stored grid location
     *
     * @param force if false, no overlap check is applied
     * @return either the location was not set or no stored location is available
     */
    @SuppressWarnings("deprecation")
    public boolean moveToStoredGridLocation(boolean force) {
        Point point = node.getLocationWithin(StringUtils.nullToEmpty(graphUI.getCompartment()), graphUI.getViewMode().name());
        if (point == null) {
            // Try to get the point from vertical layout (migrate to compact)
            point = node.getLocationWithin(StringUtils.nullToEmpty(graphUI.getCompartment()), JIPipeGraphViewMode.Vertical.name());
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
        // TODO
        return new PointRange(0,0);
    }

    public Color getFillColor() {
        return nodeFillColor;
    }

    public Color getBorderColor() {
        return nodeBorderColor;
    }

    public JIPipeGraphCanvasUI getGraphUI() {
        return graphUI;
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

        int realSlotHeight = viewMode.gridToRealSize(new Dimension(1,1), zoom).height;
        boolean hasInputs = node.getInputSlots().size() > 0 || slotsInputsEditable;
        boolean hasOutputs = node.getOutputSlots().size() > 0 || slotsOutputsEditable;

        // Paint controls
        paintNodeControls(g2, fontMetrics, realSlotHeight, hasInputs, hasOutputs);

        // Paint slots
        g2.setFont(zoomedSecondaryFont);
        g2.setStroke(JIPipeGraphCanvasUI.STROKE_UNIT);

        if(hasInputs) {
            paintInputSlots(g2, realSlotHeight);
        }
        if(hasOutputs) {
            paintOutputSlots(g2, realSlotHeight);
        }

        // Paint outside border
        if(mouseIsEntered) {
            g2.setStroke(STROKE_MOUSE_OVER);
            g2.setColor(nodeBorderColor);
            g2.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
        }
        else {
            g2.setStroke(JIPipeGraphCanvasUI.STROKE_UNIT);
            g2.setColor(nodeBorderColor);
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    }

    private void paintOutputSlots(Graphics2D g2, int realSlotHeight) {
        List<JIPipeOutputDataSlot> outputSlots = node.getOutputSlots();

        // Slot main filling
        g2.setPaint(slotFillColor);
        g2.fillRect(0, getHeight() - realSlotHeight, getWidth(), realSlotHeight);

        int startX = 0;

        for (int i = 0; i < outputSlots.size(); i++) {
            JIPipeDataSlot outputSlot = outputSlots.get(i);
            SlotState slotState = outputSlotStatusMap.get(outputSlot.getName());

            if(slotState == null)
                continue;

            int slotWidth = (int) Math.round(slotState.nativeWidth * zoom);

            // Draw separator
            if(i > 0) {
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
                    slotState.slotStatus == SlotStatus.Cached ? COLOR_SLOT_CACHED : null,
                    getHeight() - realSlotHeight,
                    (int) Math.round(getHeight() - 2 * zoom),
                    (int)Math.round(getHeight() - realSlotHeight / 2.0));

            startX += slotWidth;
        }
        if(slotsOutputsEditable) {

            // Draw separator
            if(outputSlots.size() > 1) {
                g2.setStroke(JIPipeGraphCanvasUI.STROKE_UNIT);
                g2.setPaint(nodeBorderColor);
                g2.drawLine(startX, getHeight() - realSlotHeight, startX, getHeight());
            }

            // Draw button
            SlotState slotState = outputSlotStatusMap.get(ADD_SLOT_BUTTON_NAME);
            if(slotState != null) {
                int slotWidth = (int) Math.round(slotState.nativeWidth * zoom);
                paintAddSlotButton(g2, slotState, slotWidth, realSlotHeight, startX, getHeight() - realSlotHeight);
            }
        }

        // Line above the slots
        g2.setStroke(JIPipeGraphCanvasUI.STROKE_UNIT);
        g2.setPaint(nodeBorderColor);
        g2.drawLine(0, getHeight() - realSlotHeight, getWidth(), getHeight() - realSlotHeight);
    }

    private void paintAddSlotButton(Graphics2D g2, SlotState slotState, int slotWidth, int realSlotHeight, int startX, int y) {
        g2.setStroke(JIPipeGraphCanvasUI.STROKE_UNIT);
        g2.setPaint(buttonFillColor);
        g2.fillRect(startX, y, slotWidth, realSlotHeight);
        g2.setPaint(nodeBorderColor);
        g2.drawRect(startX, y, slotWidth, realSlotHeight);

        int zoomedSize = (int) Math.round(16 * zoom);

        g2.drawImage(UIUtils.getIconFromResources("actions/add.png").getImage(), startX + slotWidth / 2 - zoomedSize / 2, y+ (int)Math.round(3 * zoom), zoomedSize, zoomedSize, null);
    }

    private void paintInputSlots(Graphics2D g2, int realSlotHeight) {
        List<JIPipeInputDataSlot> inputSlots = node.getInputSlots();

        // Slot main filling
        g2.setPaint(slotFillColor);
        g2.fillRect(0, 0, getWidth(), realSlotHeight);

        int startX = 0;

        for (int i = 0; i < inputSlots.size(); i++) {
            JIPipeInputDataSlot inputSlot = inputSlots.get(i);
            SlotState slotState = inputSlotStatusMap.get(inputSlot.getName());

            if(slotState == null)
                continue;

            int slotWidth = (int) Math.round(slotState.nativeWidth * zoom);

            // Draw separator
            if(i > 0) {
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
                    slotState.slotStatus == SlotStatus.Unconnected ? COLOR_SLOT_DISCONNECTED : null,
                    0,
                    (int) Math.round(2 * zoom),
                    (int)Math.round(realSlotHeight / 2.0));

            startX += slotWidth;
        }
        if(slotsInputsEditable) {

            // Draw separator
            if(inputSlots.size() > 1) {
                g2.setStroke(JIPipeGraphCanvasUI.STROKE_UNIT);
                g2.setPaint(nodeBorderColor);
                g2.drawLine(startX, 0, startX, realSlotHeight);
            }

            // Draw button
            SlotState slotState = inputSlotStatusMap.get(ADD_SLOT_BUTTON_NAME);
            if(slotState != null) {
                int slotWidth = (int) Math.round(slotState.nativeWidth * zoom);
                paintAddSlotButton(g2, slotState, slotWidth, realSlotHeight, startX, 0);
            }
        }

        // Line below the slots
        g2.setStroke(JIPipeGraphCanvasUI.STROKE_UNIT);
        g2.setPaint(nodeBorderColor);
        g2.drawLine(0, realSlotHeight, getWidth(), realSlotHeight);
    }

    private void paintSlot(Graphics2D g2, SlotState slotState, int realSlotHeight, double startX, int slotWidth, Color indicatorColor, int slotY, int indicatorY, int centerY) {

        // Draw selection
//        g2.setStroke(STROKE_MOUSE_OVER);
//        g2.setPaint(nodeBorderColor);
//        g2.drawRect((int) Math.round(startX) + 1, slotY + 1,slotWidth - 3, realSlotHeight - 3);

        if(indicatorColor != null) {
            g2.setPaint(indicatorColor);
            int x = (int) Math.round(startX + 2 * zoom);
            int width = slotWidth - (int) Math.round(2 * 2 * zoom);
            int height = (int) Math.round(2 * zoom);
            int arc = (int) Math.round(2 * zoom);
            g2.fillRoundRect(x, indicatorY,width, height, arc, arc);
        }
        startX += 8 * zoom;
        g2.drawImage(slotState.icon, (int) Math.round(startX + 3 * zoom), (int) Math.round(centerY - 8 * zoom), (int) Math.round(16 * zoom), (int) Math.round(16 * zoom), null);
        startX += 22 * zoom;

        // Draw name
        if(indicatorColor != null) {
            g2.setPaint(indicatorColor);
        }
        else {
            g2.setPaint(mainTextColor);
        }
        FontMetrics fontMetrics = g2.getFontMetrics();
        drawStringVerticallyCentered(g2, slotState.slotName, (int) Math.round(startX + 3 * zoom), (int) Math.round(centerY - 1 * zoom), fontMetrics);
    }

    private void paintNodeControls(Graphics2D g2, FontMetrics fontMetrics, int realSlotHeight, boolean hasInputs, boolean hasOutputs) {
        int centerY;
        if(hasInputs && !hasOutputs) {
            centerY = (getHeight() - realSlotHeight) / 2 + realSlotHeight;
        }
        else if(!hasInputs && hasOutputs) {
            centerY = (getHeight() - realSlotHeight) / 2;
        }
        else {
            centerY = getHeight() / 2;
        }
        {
            String nameLabel = node.getName();
            int centerNativeWidth = (int)Math.round ((nodeIsRunnable ? 22 : 0) * zoom + 22 * zoom + fontMetrics.stringWidth(nameLabel));
            double startX = getWidth() / 2.0 - centerNativeWidth / 2.0;

            if(nodeIsRunnable) {
                // Draw button
                g2.setPaint(buttonFillColor);
                g2.fillOval((int) Math.round(startX + 3 * zoom), (int) Math.round(centerY - 8 * zoom), (int) Math.round(16 * zoom), (int) Math.round(16 * zoom));
                g2.setPaint(nodeBorderColor);
                g2.drawOval((int) Math.round(startX + 3 * zoom), (int) Math.round(centerY - 8 * zoom), (int) Math.round(16 * zoom), (int) Math.round(16 * zoom));

                // Draw icon
                g2.setPaint(COLOR_RUN_BUTTON_ICON);
                g2.fillPolygon(new int[] { (int) Math.round(startX + (6 + 3) * zoom), (int) Math.round(startX + (13 + 3) * zoom), (int) Math.round(startX + (6 + 3) * zoom) },
                        new int[] { (int) Math.round(centerY - (5) * zoom), centerY, (int) Math.round(centerY + (5 + 1) * zoom) },
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
        int metricHeight = fontMetrics.getAscent() -  fontMetrics.getLeading();
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
        Point gridPoint = graphUI.getViewMode().realLocationToGrid(location, graphUI.getZoom());
        return moveToGridLocation(gridPoint, force, save);
    }

    @Deprecated
    public JIPipeDataSlotUI pickSlotComponent(MouseEvent mouseEvent) {
//        for (JIPipeDataSlotUI ui : getInputSlotUIs().values()) {
//            MouseEvent converted = SwingUtilities.convertMouseEvent(getGraphUI(), mouseEvent, ui);
//            if (ui.contains(converted.getX(), converted.getY()))
//                return ui;
//        }
//        for (JIPipeDataSlotUI ui : getOutputSlotUIs().values()) {
//            MouseEvent converted = SwingUtilities.convertMouseEvent(getGraphUI(), mouseEvent, ui);
//            if (ui.contains(converted.getX(), converted.getY()))
//                return ui;
//        }
        // TODO
        return null;
    }

    private void updateSlots() {
        JIPipeGraph graph = node.getParentGraph();

        inputSlotStatusMap.clear();
        outputSlotStatusMap.clear();

        if(slotsInputsEditable) {
            // Use a dummy slot for this
            SlotState slotState = new SlotState(JIPipeSlotType.Input, ADD_SLOT_BUTTON_NAME);
            inputSlotStatusMap.put(ADD_SLOT_BUTTON_NAME, slotState);
        }
        if(slotsOutputsEditable) {
            // Use a dummy slot for this
            SlotState slotState = new SlotState(JIPipeSlotType.Output, ADD_SLOT_BUTTON_NAME);
            outputSlotStatusMap.put(ADD_SLOT_BUTTON_NAME, slotState);
        }

        for (JIPipeInputDataSlot inputSlot : node.getInputSlots()) {
            SlotState slotState = new SlotState(JIPipeSlotType.Input, inputSlot.getName());
            slotState.setIcon(JIPipe.getDataTypes().getIconFor(inputSlot.getAcceptedDataType()).getImage());
            inputSlotStatusMap.put(inputSlot.getName(), slotState);
            if(StringUtils.isNullOrEmpty(inputSlot.getInfo().getCustomName())) {
               slotState.setSlotLabel(inputSlot.getName());
               slotState.setSlotLabelIsCustom(false);
            }
            else {
                slotState.setSlotLabel(inputSlot.getInfo().getCustomName());
                slotState.setSlotLabelIsCustom(true);
            }
            if(graph != null) {
                if(!inputSlot.getInfo().isOptional() && graph.getGraph().inDegreeOf(inputSlot) <= 0) {
                    slotState.setSlotStatus(SlotStatus.Unconnected);
                }
                else {
                    slotState.setSlotStatus(SlotStatus.Default);
                }
            }
        }

        Map<String, JIPipeDataTable> cachedData = null;
        if(graph != null && graph.getProject() != null) {
            cachedData = graph.getProject().getCache().query(node, node.getUUIDInParentGraph(), new JIPipeProgressInfo());
        }
        for (JIPipeDataSlot outputSlot : node.getOutputSlots()) {
            SlotState slotState = new SlotState(JIPipeSlotType.Output, outputSlot.getName());
            slotState.setIcon(JIPipe.getDataTypes().getIconFor(outputSlot.getAcceptedDataType()).getImage());
            outputSlotStatusMap.put(outputSlot.getName(), slotState);
            if(StringUtils.isNullOrEmpty(outputSlot.getInfo().getCustomName())) {
                slotState.setSlotLabel(outputSlot.getName());
                slotState.setSlotLabelIsCustom(false);
            }
            else {
                slotState.setSlotLabel(outputSlot.getInfo().getCustomName());
                slotState.setSlotLabelIsCustom(true);
            }

            if(cachedData != null && cachedData.containsKey(outputSlot.getName())) {
                slotState.setSlotStatus(SlotStatus.Cached);
            }
            else {
                slotState.setSlotStatus(SlotStatus.Default);
            }
        }

        updateSize();
    }

    @Override
    public void mouseClicked(MouseEvent e) {

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
        repaint(50);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseIsEntered = false;
        repaint(50);
    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    public static class ActiveArea implements Comparable<ActiveArea> {

        private final int priority;

        private Rectangle zoomedHitArea;

        public ActiveArea(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }

        @Override
        public int compareTo(JIPipeNodeUI.ActiveArea o) {
            return Integer.compare(priority, o.priority);
        }

        public Rectangle getZoomedHitArea() {
            return zoomedHitArea;
        }

        public void setZoomedHitArea(Rectangle zoomedHitArea) {
            this.zoomedHitArea = zoomedHitArea;
        }
    }

    /**
     * Active area that stores the state of a slot
     */
    public static class SlotState extends ActiveArea {

        private final JIPipeSlotType slotType;
        private final String slotName;

        private String slotLabel;

        private boolean slotLabelIsCustom;
        private SlotStatus slotStatus = SlotStatus.Default;

        private double nativeWidth;

        private Image icon;

        public SlotState(JIPipeSlotType slotType, String slotName) {
            super(10);
            this.slotType = slotType;
            this.slotName = slotName;
        }

        public JIPipeSlotType getSlotType() {
            return slotType;
        }

        public String getSlotLabel() {
            return slotLabel;
        }

        public void setSlotLabel(String slotLabel) {
            this.slotLabel = slotLabel;
        }

        public String getSlotName() {
            return slotName;
        }

        public SlotStatus getSlotStatus() {
            return slotStatus;
        }

        public void setSlotStatus(SlotStatus slotStatus) {
            this.slotStatus = slotStatus;
        }

        public boolean isSlotLabelIsCustom() {
            return slotLabelIsCustom;
        }

        public void setSlotLabelIsCustom(boolean slotLabelIsCustom) {
            this.slotLabelIsCustom = slotLabelIsCustom;
        }

        public double getNativeWidth() {
            return nativeWidth;
        }

        public void setNativeWidth(double nativeWidth) {
            this.nativeWidth = nativeWidth;
        }

        public Image getIcon() {
            return icon;
        }

        public void setIcon(Image icon) {
            this.icon = icon;
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
