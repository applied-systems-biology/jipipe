/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeGraphType;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.cache.JIPipeCache;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartmentOutput;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.grouping.GraphWrapperAlgorithmInput;
import org.hkijena.jipipe.api.grouping.GraphWrapperAlgorithmOutput;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartition;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.JIPipeGraphViewMode;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.actions.JIPipeDesktopNodeUIAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.*;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.triggers.*;
import org.hkijena.jipipe.desktop.app.grapheditor.nodefinder.JIPipeDesktopNodeFinderDialogUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopAddAlgorithmSlotPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopEditAlgorithmSlotPanel;
import org.hkijena.jipipe.utils.*;
import org.hkijena.jipipe.utils.ui.ViewOnlyMenuItem;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UI around an {@link JIPipeGraphNode} instance
 */
public class JIPipeDesktopGraphNodeUI extends JIPipeDesktopWorkbenchPanel implements MouseListener, MouseMotionListener,
        JIPipeCache.ModifiedEventListener, JIPipeGraphNode.NodeSlotsChangedEventListener, JIPipeGraph.NodeConnectedEventListener,
        JIPipeGraph.NodeDisconnectedEventListener, JIPipeParameterCollection.ParameterChangedEventListener {
    public static final Color COLOR_DISABLED_1 = new Color(227, 86, 86);
    public static final Color COLOR_DISABLED_2 = new Color(0xc36262);

    public static final Color COLOR_SLOT_CACHED = new Color(0x95c2a8);

    public static final Color COLOR_SLOT_DISCONNECTED = new Color(0xc36262);

    public static final Color COLOR_RUN_BUTTON_ICON = new Color(0x22A02D);

    public static final NodeUIContextAction[] RUN_NODE_CONTEXT_MENU_ENTRIES = new NodeUIContextAction[]{
            new UpdateCacheNodeUIContextAction(),
            new UpdateCacheShowIntermediateNodeUIContextAction(),
            new UpdateCacheOnlyPredecessorsNodeUIContextAction(),
            NodeUIContextAction.SEPARATOR,
            new RunAndShowResultsNodeUIContextAction(),
            new RunAndShowIntermediateResultsNodeUIContextAction(),
            NodeUIContextAction.SEPARATOR,
            new ClearCacheNodeUIContextAction()
    };
    protected final List<JIPipeDesktopGraphNodeUIActiveArea> activeAreas = new ArrayList<>();
    private final JIPipeGraphViewMode viewMode = JIPipeGraphViewMode.VerticalCompact;
    private final JIPipeDesktopGraphCanvasUI graphCanvasUI;
    private final JIPipeGraphNode node;
    private final Color nodeFillColor;
    private final Color highlightedNodeBorderColor;
    private final LinearGradientPaint nodeDisabledPaint;
    private final LinearGradientPaint nodePassThroughPaint;
    private final boolean slotsInputsEditable;
    private final boolean slotsOutputsEditable;
    private final Map<String, JIPipeDesktopGraphNodeUISlotActiveArea> inputSlotMap = new HashMap<>();
    private final Map<String, JIPipeDesktopGraphNodeUISlotActiveArea> outputSlotMap = new HashMap<>();
    private final Color slotParametersFillColor;
    private final Image nodeIcon;
    private final Font nativeMainFont = new Font(Font.DIALOG, Font.PLAIN, 12);
    private final Font nativeSecondaryFont = new Font(Font.DIALOG, Font.PLAIN, 11);
    private final Font nativeTertiaryFont = new Font(Font.DIALOG, Font.PLAIN, 10);
    private final Font nativeSecondaryHighlightedFont = new Font(Font.DIALOG, Font.ITALIC, 11);
    private final Color mainTextColor;
    private final Color secondaryTextColor;
    private final boolean showInputs;
    private final boolean showOutputs;
    private final NodeUIActionRequestedEventEmitter nodeUIActionRequestedEventEmitter = new NodeUIActionRequestedEventEmitter();
    private final boolean nodeIsRunnable;
    private Color nodeBorderColor;
    private Color slotFillColor;
    private Color buttonFillColor;
    private Color buttonFillColorDarker;
    private JIPipeDesktopGraphNodeUIAddSlotButtonActiveArea addInputSlotArea;
    private JIPipeDesktopGraphNodeUIAddSlotButtonActiveArea addOutputSlotArea;
    private boolean mouseIsEntered = false;
    private double zoom = 1;
    private Font zoomedMainFont;
    private Font zoomedSecondaryFont;
    private Font zoomedTertiaryFont;
    private Font zoomedHighlightedSecondaryFont;
    private JIPipeDesktopGraphNodeUIActiveArea currentActiveArea;
    private BufferedImage nodeBuffer;
    private boolean nodeBufferInvalid = true;
    private boolean buffered = true;
    private Color partitionColor;

    /**
     * Creates a new UI
     *
     * @param workbench     thr workbench
     * @param graphCanvasUI The graph UI that contains this UI
     * @param node          The algorithm
     */
    public JIPipeDesktopGraphNodeUI(JIPipeDesktopWorkbench workbench, JIPipeDesktopGraphCanvasUI graphCanvasUI, JIPipeGraphNode node) {
        super(workbench);
        this.graphCanvasUI = graphCanvasUI;
        this.node = node;
        this.zoom = graphCanvasUI.getZoom();

        this.node.getParameterChangedEventEmitter().subscribeWeak(this);
        this.node.getNodeSlotsChangedEventEmitter().subscribeWeak(this);

        JIPipeGraph graph = this.graphCanvasUI.getGraph();
        graph.getNodeConnectedEventEmitter().subscribeWeak(this);
        graph.getNodeDisconnectedEventEmitter().subscribeWeak(this);

        if (workbench instanceof JIPipeDesktopProjectWorkbench) {
            ((JIPipeDesktopProjectWorkbench) workbench).getProject().getCache().getModifiedEventEmitter().subscribeWeak(this);
        }

        // Node information
        nodeIsRunnable = node.getInfo().isRunnable() || node instanceof JIPipeAlgorithm || node instanceof JIPipeProjectCompartment;

        if(node instanceof JIPipeProjectCompartmentOutput)  {
            if(isDisplayedInForeignCompartment()) {
                showInputs = false;
                showOutputs = true;
            }
            else {
                showInputs = true;
                showOutputs = true;
            }
        }
        else if (node instanceof GraphWrapperAlgorithmInput) {
            showInputs = false;
            showOutputs = true;
        } else if (node instanceof GraphWrapperAlgorithmOutput) {
            showInputs = true;
            showOutputs = false;
        } else {
            showInputs = true;
            showOutputs = true;
        }

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
        this.highlightedNodeBorderColor = UIUtils.DARK_THEME ? new Color(0xBBBBBF) : new Color(0x444444);
        this.slotFillColor = UIManager.getColor("Panel.background");
        this.slotParametersFillColor = ColorUtils.mix(slotFillColor, nodeFillColor, 0.5);
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

    public boolean isBuffered() {
        return buffered;
    }

    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
    }

    public NodeUIActionRequestedEventEmitter getNodeUIActionRequestedEventEmitter() {
        return nodeUIActionRequestedEventEmitter;
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
        updateColors();
        if (assets) {
            updateAssets();
        }
        if (slots) {
            updateSlots();
        }
        if (slots || size) {
            updateSize();
            invalidateAndRepaint(true, true);
        }
        updateActiveAreas();
    }

    protected void updateActiveAreas() {
        activeAreas.clear();

        // Add whole node
        updateWholeNodeActiveAreas();

        // Add slots
        updateSlotActiveAreas();

        // Sort
        activeAreas.sort(Comparator.naturalOrder());
    }

    public Map<String, JIPipeDesktopGraphNodeUISlotActiveArea> getInputSlotMap() {
        return inputSlotMap;
    }

    public Map<String, JIPipeDesktopGraphNodeUISlotActiveArea> getOutputSlotMap() {
        return outputSlotMap;
    }

    protected void updateSlotActiveAreas() {
        for (JIPipeDesktopGraphNodeUISlotActiveArea slotState : inputSlotMap.values()) {
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
            JIPipeDesktopGraphNodeUISlotButtonActiveArea slotButtonActiveArea = new JIPipeDesktopGraphNodeUISlotButtonActiveArea(this, slotState);
            slotButtonActiveArea.setZoomedHitArea(slotButtonArea);
            activeAreas.add(slotButtonActiveArea);
        }
        if (addInputSlotArea != null) {
            Rectangle slotArea = new Rectangle((int) Math.round(addInputSlotArea.getNativeLocation().x * zoom),
                    (int) Math.round(addInputSlotArea.getNativeLocation().y * zoom),
                    (int) Math.round(addInputSlotArea.getNativeWidth() * zoom),
                    (int) Math.round(viewMode.getGridHeight() * zoom));
            addInputSlotArea.setZoomedHitArea(slotArea);
            activeAreas.add(addInputSlotArea);
        }
        for (JIPipeDesktopGraphNodeUISlotActiveArea slotState : outputSlotMap.values()) {
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
            JIPipeDesktopGraphNodeUISlotButtonActiveArea slotButtonActiveArea = new JIPipeDesktopGraphNodeUISlotButtonActiveArea(this, slotState);
            slotButtonActiveArea.setZoomedHitArea(slotButtonArea);
            activeAreas.add(slotButtonActiveArea);
        }
        if (addOutputSlotArea != null) {
            Rectangle slotArea = new Rectangle((int) Math.round(addOutputSlotArea.getNativeLocation().x * zoom),
                    (int) Math.round(addOutputSlotArea.getNativeLocation().y * zoom),
                    (int) Math.round(addOutputSlotArea.getNativeWidth() * zoom),
                    (int) Math.round(viewMode.getGridHeight() * zoom));
            addOutputSlotArea.setZoomedHitArea(slotArea);
            activeAreas.add(addOutputSlotArea);
        }
    }

    protected void updateWholeNodeActiveAreas() {
        FontMetrics mainFontMetrics;

        if (getGraphics() != null) {
            mainFontMetrics = getGraphics().getFontMetrics(zoomedMainFont);
        } else {
            Canvas c = new Canvas();
            mainFontMetrics = c.getFontMetrics(zoomedMainFont);
        }

        // Whole node
        JIPipeDesktopGraphNodeUIWholeNodeActiveArea wholeNodeActiveArea = new JIPipeDesktopGraphNodeUIWholeNodeActiveArea(this);
        wholeNodeActiveArea.setZoomedHitArea(new Rectangle(0, 0, getWidth(), getHeight()));
        activeAreas.add(wholeNodeActiveArea);

        // Node button
        if (nodeIsRunnable) {
            int realSlotHeight = viewMode.gridToRealSize(new Dimension(1, 1), zoom).height;
            boolean hasInputs = (!node.getInputSlots().isEmpty() || slotsInputsEditable) && showInputs;
            boolean hasOutputs = (!node.getOutputSlots().isEmpty() || slotsOutputsEditable) && showOutputs;

            int centerY;
            if (hasInputs && !hasOutputs) {
                centerY = (getHeight() - realSlotHeight) / 2 + realSlotHeight;
            } else if (!hasInputs && hasOutputs) {
                centerY = (getHeight() - realSlotHeight) / 2;
            } else {
                centerY = getHeight() / 2;
            }

            String nameLabel = node.getName();
            int centerNativeWidth = (int) Math.round(22 * zoom + 22 * zoom + mainFontMetrics.stringWidth(nameLabel));
            double startX = getWidth() / 2.0 - centerNativeWidth / 2.0;

            JIPipeDesktopGraphNodeUIRunNodeActiveArea activeArea = new JIPipeDesktopGraphNodeUIRunNodeActiveArea(this);
            activeArea.setZoomedHitArea(new Rectangle((int) Math.round(startX), (int) Math.round(centerY - 11 * zoom), (int) Math.round(22 * zoom), (int) Math.round(22 * zoom)));

            activeAreas.add(activeArea);
        }
    }

    protected void updateColors() {
        // Border colors (partitioning)
        int partition = 0;
        if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
            if (node instanceof JIPipeAlgorithm) {
                partition = ((JIPipeAlgorithm) node).getRuntimePartition().getIndex();
            } else if (node instanceof JIPipeProjectCompartment) {
//                // Color after output
//                JIPipeCompartmentOutput outputNode = ((JIPipeProjectCompartment) node).getOutputNodes();
//                partition = outputNode.getRuntimePartition().getIndex();
                partition = 0; // Do no coloring
            }
            JIPipeRuntimePartition runtimePartition = getDesktopWorkbench().getProject().getRuntimePartitions().get(partition);
            if (runtimePartition.getColor().isEnabled()) {
                this.partitionColor = runtimePartition.getColor().getContent();
            } else {
                this.partitionColor = null;
            }
        } else {
            this.partitionColor = null;
        }

        this.buttonFillColor = ColorUtils.multiplyHSB(slotFillColor, 1, 1, 0.95f);
        this.buttonFillColorDarker = ColorUtils.multiplyHSB(slotFillColor, 1, 1, 0.85f);

        nodeBufferInvalid = true;
        repaint(50);
        if (SystemUtils.IS_OS_LINUX) {
            Toolkit.getDefaultToolkit().sync();
        }
    }

    protected void updateAssets() {
        // Update fonts
        zoomedMainFont = new Font(Font.DIALOG, Font.PLAIN, (int) Math.round(12 * zoom));
        zoomedSecondaryFont = new Font(Font.DIALOG, Font.PLAIN, (int) Math.round(11 * zoom));
        zoomedTertiaryFont = new Font(Font.DIALOG, Font.PLAIN, (int) Math.round(10 * zoom));
        zoomedHighlightedSecondaryFont = new Font(Font.DIALOG, Font.ITALIC, (int) Math.round(11 * zoom));
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


    @Override
    public void onNodeSlotsChanged(JIPipeGraphNode.NodeSlotsChangedEvent event) {
        if (event.getNode() == node) {
            updateView(false, true, true);
        }
    }

    @Override
    public void onNodeConnected(JIPipeGraph.NodeConnectedEvent event) {
        if (event.getSource().getNode() == node || event.getTarget().getNode() == node) {
            updateView(false, true, true);
        }
    }

    @Override
    public void onNodeDisconnected(JIPipeGraph.NodeDisconnectedEvent event) {
        if (event.getSource().getNode() == node || event.getTarget().getNode() == node) {
            updateView(false, true, true);
        }
    }

    /**
     * Should be triggered when an algorithm's name parameter is changed
     *
     * @param event The generated event
     */
    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (event.getSource() == node && "jipipe:node:name".equals(event.getKey())) {
            updateView(false, false, true);
        } else if (event.getSource() == node && "jipipe:algorithm:enabled".equals(event.getKey())) {
            invalidateAndRepaint(true, false);
        } else if (event.getSource() == node && "jipipe:algorithm:pass-through".equals(event.getKey())) {
            invalidateAndRepaint(true, false);
        } else if (event.getSource() == node && "jipipe:node:bookmarked".equals(event.getKey())) {
            invalidateAndRepaint(false, true);
        } else if (event.getSource() instanceof JIPipeDataSlotInfo) {
            updateView(false, true, true);
        } else if (event.getSource() == node && "jipipe:algorithm:runtime-partition".equals(event.getKey())) {
            updateColors();
            invalidateAndRepaint(true, false);
        }
    }

    /**
     * Recalculates the UI size
     */
    protected void updateSize() {

        FontMetrics mainFontMetrics;
        FontMetrics secondaryFontMetrics;
        FontMetrics tertiaryFontMetrics;

        if (getGraphics() != null) {
            mainFontMetrics = getGraphics().getFontMetrics(nativeMainFont);
            secondaryFontMetrics = getGraphics().getFontMetrics(nativeSecondaryFont);
            tertiaryFontMetrics = getGraphics().getFontMetrics(nativeTertiaryFont);
        } else {
            Canvas c = new Canvas();
            mainFontMetrics = c.getFontMetrics(nativeMainFont);
            secondaryFontMetrics = c.getFontMetrics(nativeSecondaryFont);
            tertiaryFontMetrics = c.getFontMetrics(nativeTertiaryFont);
        }

        double nameWidth;
        if(isDisplayedInForeignCompartment()) {
            JIPipeProject project = getGraphCanvasUI().getWorkbench().getProject();
            JIPipeProjectCompartment projectCompartment = project.getCompartments().get(node.getCompartmentUUIDInParentGraph());
            nameWidth = Math.max(tertiaryFontMetrics.stringWidth(node.getName()),
                    tertiaryFontMetrics.stringWidth("⮤ " + projectCompartment.getName()));
        }
        else {
            nameWidth = mainFontMetrics.stringWidth(node.getName());
        }
        double mainWidth = (nodeIsRunnable ? 22 : 0) + 22 + nameWidth + 16;

        // Slot widths
        double sumInputSlotWidths = 0;
        double sumOutputSlotWidths = 0;

        if (showInputs) {
            for (JIPipeInputDataSlot inputSlot : node.getInputSlots()) {
                JIPipeDesktopGraphNodeUISlotActiveArea slotState = inputSlotMap.get(inputSlot.getName());
                double nativeWidth = secondaryFontMetrics.stringWidth(slotState.getSlotLabel()) + 22 * 2 + 16;
                slotState.setNativeWidth(nativeWidth);
                slotState.setNativeLocation(new Point((int) sumInputSlotWidths, 0));
                sumInputSlotWidths += nativeWidth;
            }
            if (slotsInputsEditable) {
                addInputSlotArea = new JIPipeDesktopGraphNodeUIAddSlotButtonActiveArea(this, JIPipeSlotType.Input);
                double nativeWidth = 22;
                addInputSlotArea.setNativeWidth(nativeWidth);
                addInputSlotArea.setNativeLocation(new Point((int) sumInputSlotWidths, 0));
                sumInputSlotWidths += nativeWidth;
            } else {
                addInputSlotArea = null;
            }
        }
        if (showOutputs) {
            for (JIPipeDataSlot outputSlots : node.getOutputSlots()) {
                JIPipeDesktopGraphNodeUISlotActiveArea slotState = outputSlotMap.get(outputSlots.getName());
                if (slotState == null)
                    continue;
                double nativeWidth = secondaryFontMetrics.stringWidth(slotState.getSlotLabel()) + 22 * 2 + 16;
                slotState.setNativeWidth(nativeWidth);
                slotState.setNativeLocation(new Point((int) sumOutputSlotWidths, viewMode.getGridHeight() * 2));
                sumOutputSlotWidths += nativeWidth;
            }
            if (slotsOutputsEditable) {
                addOutputSlotArea = new JIPipeDesktopGraphNodeUIAddSlotButtonActiveArea(this, JIPipeSlotType.Output);
                double nativeWidth = 22;
                addOutputSlotArea.setNativeWidth(nativeWidth);
                addOutputSlotArea.setNativeLocation(new Point((int) sumOutputSlotWidths, viewMode.getGridHeight() * 2));
                sumOutputSlotWidths += nativeWidth;
            } else {
                addOutputSlotArea = null;
            }
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

    public JIPipeGraphViewMode getViewMode() {
        return viewMode;
    }


    private void scaleSlotsNativeWidth(Map<String, JIPipeDesktopGraphNodeUISlotActiveArea> slotStateMap, JIPipeDesktopGraphNodeUIAddSlotButtonActiveArea addSlotActiveArea, double nodeWidth, double sumWidth, boolean hasButton) {
//        if(slotStateMap.size() == 1 && hasButton) {
//            return;
//        }
        boolean excludeButton = false;
        if (!slotStateMap.isEmpty() && hasButton) {
            nodeWidth -= 22;
            sumWidth -= 22;
            excludeButton = true;
        }
        double factor = nodeWidth / sumWidth;
        for (JIPipeDesktopGraphNodeUISlotActiveArea slotState : slotStateMap.values()) {
            slotState.getNativeLocation().x = (int) Math.round(slotState.getNativeLocation().x * factor);
            slotState.setNativeWidth(slotState.getNativeWidth() * factor);
        }
        if (addSlotActiveArea != null) {
            if (!excludeButton) {
                addSlotActiveArea.setNativeWidth(addSlotActiveArea.getNativeWidth() * factor);
            } else {
                addSlotActiveArea.getNativeLocation().x = (int) Math.round(addSlotActiveArea.getNativeLocation().x * factor);
            }
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
                if (component instanceof JIPipeDesktopGraphNodeUI) {
                    JIPipeDesktopGraphNodeUI ui = (JIPipeDesktopGraphNodeUI) component;
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
            node.setLocationWithin(graphCanvasUI.getCompartmentUUID(), gridLocation, graphCanvasUI.getViewMode().name());
            getGraphCanvasUI().getDesktopWorkbench().setProjectModified(true);
        }
        return true;
    }

    public Point getStoredGridLocation() {
        return node.getLocationWithin(StringUtils.nullToEmpty(graphCanvasUI.getCompartmentUUID()), graphCanvasUI.getViewMode().name());
    }

    /**
     * Moves the UI back to the stored grid location
     *
     * @param force if false, no overlap check is applied
     * @return either the location was not set or no stored location is available
     */
    @SuppressWarnings("deprecation")
    public boolean moveToStoredGridLocation(boolean force) {
        Point point = node.getLocationWithin(StringUtils.nullToEmpty(graphCanvasUI.getCompartmentUUID()), graphCanvasUI.getViewMode().name());
        if (point == null) {
            // Try to get the point from vertical layout (migrate to compact)
            point = node.getLocationWithin(StringUtils.nullToEmpty(graphCanvasUI.getCompartmentUUID()), JIPipeGraphViewMode.Vertical.name());
        }
        if (point != null) {
            return moveToGridLocation(point, force, false);
        } else {
            return false;
        }
    }

    /**
     * Returns the location of a slot in relative coordinates
     *
     * @param slot the slot
     * @return coordinates relative to this algorithm UI
     */
    public PointRange getSlotLocation(JIPipeDataSlot slot) {
        JIPipeDesktopGraphNodeUISlotActiveArea slotState;
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

    public JIPipeDesktopGraphCanvasUI getGraphCanvasUI() {
        return graphCanvasUI;
    }

    public int getRightX() {
        return getX() + getWidth();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        super.paintComponent(g);

        if (buffered) {
            if (nodeBufferInvalid || nodeBuffer == null || nodeBuffer.getWidth() != getWidth() || nodeBuffer.getHeight() != getHeight()) {
                if (nodeBuffer == null || nodeBuffer.getWidth() != getWidth() || nodeBuffer.getHeight() != getHeight()) {
                    nodeBuffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_BGR);
                }
                Graphics2D bufferGraphics = nodeBuffer.createGraphics();
//            bufferGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                bufferGraphics.setRenderingHints(graphCanvasUI.getDesktopRenderingHints());
                bufferGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                bufferGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                paintNode(bufferGraphics);
                bufferGraphics.dispose();
                nodeBufferInvalid = false;
            }

            g2.drawImage(nodeBuffer, 0, 0, getWidth(), getHeight(), null);
        } else {
            paintNode(g2);
        }

//        for (JIPipeNodeUIActiveArea activeArea : activeAreas) {
//            g2.setPaint(Color.RED);
//            g2.draw(activeArea.getZoomedHitArea());
//        }
    }

    public boolean isDrawShadow() {
        return true;
    }

    public boolean isDisplayedInForeignCompartment() {
        if(graphCanvasUI.getDesktopWorkbench().getProject() == null)
            return false;
        if (node.getCompartmentUUIDInParentGraph() != null && graphCanvasUI.getCompartmentUUID() != null) {
            return !Objects.equals(node.getCompartmentUUIDInParentGraph(), graphCanvasUI.getCompartmentUUID());
        } else {
            return false;
        }
    }

    protected void paintNode(Graphics2D g2) {

        g2.setPaint(nodeFillColor);
        g2.fillRect(0, 0, getWidth(), getHeight());

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

        if(isDisplayedInForeignCompartment()) {
            g2.setFont(zoomedTertiaryFont);
        }
        else {
            g2.setFont(zoomedMainFont);
        }

        FontMetrics fontMetrics = g2.getFontMetrics();

        int realSlotHeight = viewMode.gridToRealSize(new Dimension(1, 1), zoom).height;
        boolean hasInputs = !node.getInputSlots().isEmpty() || slotsInputsEditable;
        boolean hasOutputs = !node.getOutputSlots().isEmpty() || slotsOutputsEditable;

        // Paint controls
        paintNodeControls(g2, fontMetrics, realSlotHeight, hasInputs && showInputs, hasOutputs && showOutputs);

        // Paint slots
        g2.setStroke(JIPipeDesktopGraphCanvasUI.STROKE_UNIT);

        if (hasInputs && showInputs) {
            paintInputSlots(g2, realSlotHeight);
        }
        if (hasOutputs && showOutputs) {
            paintOutputSlots(g2, realSlotHeight);
        }

        // Paint outside border
        g2.setStroke(JIPipeDesktopGraphCanvasUI.STROKE_UNIT);
        g2.setColor(currentActiveArea instanceof JIPipeDesktopGraphNodeUIWholeNodeActiveArea ? highlightedNodeBorderColor : nodeBorderColor);
        g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
    }


    private void paintOutputSlots(Graphics2D g2, int realSlotHeight) {
        List<JIPipeOutputDataSlot> outputSlots = node.getOutputSlots();

        // Slot main filling
        g2.setPaint(slotFillColor);
        g2.fillRect(0, getHeight() - realSlotHeight, getWidth(), realSlotHeight);

        int startX = 0;

        for (int i = 0; i < outputSlots.size(); i++) {
            JIPipeDataSlot outputSlot = outputSlots.get(i);
            JIPipeDesktopGraphNodeUISlotActiveArea slotState = outputSlotMap.get(outputSlot.getName());

            if (slotState == null)
                continue;

            int slotWidth = (int) Math.round(slotState.getNativeWidth() * zoom);

            // Save the highlight for later
            slotState.setLastFillRect(new Rectangle(startX, getHeight() - realSlotHeight, slotWidth, realSlotHeight));

            // Draw highlight
            if (slotState == currentActiveArea && graphCanvasUI.currentToolAllowsConnectionDragging()) {
                g2.setPaint(buttonFillColor);
                g2.fillRect(startX, getHeight() - realSlotHeight, slotWidth, realSlotHeight);
            }

            // Draw separator
            if (i > 0) {
                g2.setStroke(JIPipeDesktopGraphCanvasUI.STROKE_UNIT);
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
                g2.setStroke(JIPipeDesktopGraphCanvasUI.STROKE_UNIT);
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
        g2.setStroke(JIPipeDesktopGraphCanvasUI.STROKE_UNIT);
        g2.setPaint(nodeBorderColor);
        g2.drawLine(0, getHeight() - realSlotHeight, getWidth(), getHeight() - realSlotHeight);
    }

    private void paintAddSlotButton(Graphics2D g2, JIPipeDesktopGraphNodeUIAddSlotButtonActiveArea activeArea, int slotWidth, int realSlotHeight, int startX, int y) {

        boolean isHighlighted = currentActiveArea instanceof JIPipeDesktopGraphNodeUIAddSlotButtonActiveArea && ((JIPipeDesktopGraphNodeUIAddSlotButtonActiveArea) currentActiveArea).getSlotType() == activeArea.getSlotType();

        g2.setStroke(JIPipeDesktopGraphCanvasUI.STROKE_UNIT);
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
            JIPipeDesktopGraphNodeUISlotActiveArea slotState = inputSlotMap.get(inputSlot.getName());

            if (slotState == null)
                continue;

            int slotWidth = (int) Math.round(slotState.getNativeWidth() * zoom);

            // Draw parameter slots differently
            JIPipeDataSlotRole slotRole = slotState.getSlot().getInfo().getRole();
            if (slotRole == JIPipeDataSlotRole.Parameters || slotRole == JIPipeDataSlotRole.ParametersLooping) {
                g2.setPaint(slotParametersFillColor);
                g2.fillRect(startX, 0, slotWidth, realSlotHeight);
            }

            // Save the highlight for later
            slotState.setLastFillRect(new Rectangle(startX, 0, slotWidth, realSlotHeight));

            // Draw highlight
            if (slotState == currentActiveArea && graphCanvasUI.currentToolAllowsConnectionDragging()) {
                g2.setPaint(buttonFillColor);
                g2.fillRect(startX, 0, slotWidth, realSlotHeight);
            }

            // Draw separator
            if (i > 0) {
                g2.setStroke(JIPipeDesktopGraphCanvasUI.STROKE_UNIT);
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
                g2.setStroke(JIPipeDesktopGraphCanvasUI.STROKE_UNIT);
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
        g2.setStroke(JIPipeDesktopGraphCanvasUI.STROKE_UNIT);
        g2.setPaint(nodeBorderColor);
        g2.drawLine(0, realSlotHeight, getWidth(), realSlotHeight);
    }

    private void paintSlot(Graphics2D g2, JIPipeDesktopGraphNodeUISlotActiveArea slotState, int realSlotHeight, double startX, int slotWidth, Color indicatorColor, Color indicatorTextColor, int slotY, int indicatorY, int centerY) {

        if (slotState.isSlotLabelIsCustom()) {
            g2.setFont(zoomedHighlightedSecondaryFont);
        } else {
            g2.setFont(zoomedSecondaryFont);
        }

        final double originalStartX = startX;
        boolean hasMouseOver = currentActiveArea == slotState || (currentActiveArea instanceof JIPipeDesktopGraphNodeUISlotButtonActiveArea && ((JIPipeDesktopGraphNodeUISlotButtonActiveArea) currentActiveArea).getUISlot() == slotState);
        boolean hasButtonHover = (currentActiveArea instanceof JIPipeDesktopGraphNodeUISlotButtonActiveArea && ((JIPipeDesktopGraphNodeUISlotButtonActiveArea) currentActiveArea).getUISlot() == slotState);

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
        } else if (slotState.getSlot().getInfo().isOptional()) {
            g2.setPaint(Color.GRAY);
        } else {
            g2.setPaint(mainTextColor);
        }
        FontMetrics fontMetrics = g2.getFontMetrics();
        UIUtils.drawStringVerticallyCentered(g2, slotState.getSlotLabel(), (int) Math.round(startX + 3 * zoom), (int) Math.round(centerY - 1 * zoom), fontMetrics);

        if (slotState.getSlotStatus() == SlotStatus.Cached) {
            startX = originalStartX + slotWidth - 8 * zoom - 12 * zoom;
            g2.drawImage(UIUtils.getIconInverted12FromResources("actions/database.png").getImage(),
                    (int) Math.round(startX),
                    (int) Math.round(centerY - 6 * zoom),
                    (int) Math.round(12 * zoom),
                    (int) Math.round(12 * zoom),
                    null);
        } else if (slotState.getSlot().isInput()) {
            ImageIcon uiInputSlotIcon = node.getUIInputSlotIcon(slotState.getSlotName());
            if (uiInputSlotIcon != null) {
                Dimension dimension = node.getUIInputSlotIconBaseDimensions(slotState.getSlotName());
                startX = originalStartX + slotWidth - 8 * zoom - dimension.width * zoom;
                g2.drawImage(uiInputSlotIcon.getImage(),
                        (int) Math.round(startX),
                        (int) Math.round(centerY - dimension.height / 2.0 * zoom),
                        (int) Math.round(dimension.width * zoom),
                        (int) Math.round(dimension.width * zoom),
                        null);
            }
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
            double nameWidth;

            if(isDisplayedInForeignCompartment()) {
                JIPipeProject project = getGraphCanvasUI().getWorkbench().getProject();
                JIPipeProjectCompartment projectCompartment = project.getCompartments().get(node.getCompartmentUUIDInParentGraph());
                nameWidth = Math.max(fontMetrics.stringWidth(nameLabel),
                        fontMetrics.stringWidth("⮤ " + projectCompartment.getName()));
            }
            else {
                nameWidth = fontMetrics.stringWidth(nameLabel);
            }

            int centerNativeWidth = (int) Math.round((nodeIsRunnable ? 22 : 0) * zoom + 22 * zoom + nameWidth);
            double startX = getWidth() / 2.0 - centerNativeWidth / 2.0;

            if (nodeIsRunnable) {
                boolean isButtonHighlighted = currentActiveArea instanceof JIPipeDesktopGraphNodeUIRunNodeActiveArea;

                // Draw button
                g2.setPaint(isButtonHighlighted ? buttonFillColorDarker : buttonFillColor);
                g2.fillOval((int) Math.round(startX + 3 * zoom), (int) Math.round(centerY - 8 * zoom), (int) Math.round(16 * zoom), (int) Math.round(16 * zoom));
                g2.setPaint(nodeBorderColor);
                g2.drawOval((int) Math.round(startX + 3 * zoom), (int) Math.round(centerY - 8 * zoom), (int) Math.round(16 * zoom), (int) Math.round(16 * zoom));

                // Draw play button
                g2.setPaint(COLOR_RUN_BUTTON_ICON);
                g2.fillPolygon(new int[]{(int) Math.round(startX + (6 + 3) * zoom), (int) Math.round(startX + (13 + 3) * zoom), (int) Math.round(startX + (6 + 3) * zoom)},
                        new int[]{(int) Math.round(centerY - (5) * zoom), centerY, (int) Math.round(centerY + (5 + 1) * zoom)},
                        3);

                startX += 22 * zoom;
            }

            // Draw partition
            if (partitionColor != null) {
                g2.setPaint(partitionColor);
                g2.fillRoundRect((int) Math.round(startX + 1 * zoom), (int) Math.round(centerY - 9 * zoom), (int) Math.round(20 * zoom), (int) Math.round(18 * zoom), 4, 4);
                g2.setPaint(nodeBorderColor);
                g2.drawRoundRect((int) Math.round(startX + 1 * zoom), (int) Math.round(centerY - 9 * zoom), (int) Math.round(20 * zoom), (int) Math.round(18 * zoom), 4, 4);
            }

            // Draw icon
            g2.drawImage(nodeIcon, (int) Math.round(startX + 3 * zoom), (int) Math.round(centerY - 8 * zoom), (int) Math.round(16 * zoom), (int) Math.round(16 * zoom), null);
            startX += 22 * zoom;

            // Draw name
            g2.setPaint(mainTextColor);
            if(isDisplayedInForeignCompartment()) {
                UIUtils.drawStringVerticallyCentered(g2, nameLabel, (int) Math.round(startX + 3 * zoom), centerY - (fontMetrics.getAscent() + fontMetrics.getLeading()) / 2, fontMetrics);
                JIPipeProject project = getGraphCanvasUI().getWorkbench().getProject();
                JIPipeProjectCompartment projectCompartment = project.getCompartments().get(node.getCompartmentUUIDInParentGraph());
                UIUtils.drawStringVerticallyCentered(g2, "⮤ " + projectCompartment.getName(), (int) Math.round(startX + 3 * zoom), centerY + (fontMetrics.getAscent() + fontMetrics.getLeading()) / 2, fontMetrics);
            }
            else {
                UIUtils.drawStringVerticallyCentered(g2, nameLabel, (int) Math.round(startX + 3 * zoom), centerY, fontMetrics);
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
        Point gridPoint = graphCanvasUI.getViewMode().realLocationToGrid(location, graphCanvasUI.getZoom());
        return moveToGridLocation(gridPoint, force, save);
    }

    public JIPipeDesktopGraphNodeUISlotActiveArea pickSlotAtMousePosition(MouseEvent event) {
        MouseEvent converted = SwingUtilities.convertMouseEvent(getGraphCanvasUI(), event, this);
        Point mousePosition = converted.getPoint();
        return pickSlotAtMousePosition(mousePosition);
    }

    public JIPipeDesktopGraphNodeUISlotActiveArea pickSlotAtMousePosition(Point mousePosition) {
        for (JIPipeDesktopGraphNodeUIActiveArea activeArea : activeAreas) {
            if (activeArea instanceof JIPipeDesktopGraphNodeUISlotActiveArea) {
                if (activeArea.getZoomedHitArea().contains(mousePosition)) {
                    return (JIPipeDesktopGraphNodeUISlotActiveArea) activeArea;
                }
            }
        }
        return null;
    }

    public JIPipeDesktopGraphNodeUIActiveArea pickAddSlotAtMousePosition(MouseEvent event) {
        if (addInputSlotArea != null || addOutputSlotArea != null) {
            MouseEvent converted = SwingUtilities.convertMouseEvent(getGraphCanvasUI(), event, this);
            Point mousePosition = converted.getPoint();
            if (addInputSlotArea != null && addInputSlotArea.getZoomedHitArea().contains(mousePosition)) {
                return addInputSlotArea;
            }
            if (addOutputSlotArea != null && addOutputSlotArea.getZoomedHitArea().contains(mousePosition)) {
                return addOutputSlotArea;
            }
        }
        return null;
    }

    public boolean isSlotsInputsEditable() {
        return slotsInputsEditable;
    }

    public boolean isSlotsOutputsEditable() {
        return slotsOutputsEditable;
    }

    public JIPipeDesktopGraphNodeUIAddSlotButtonActiveArea getAddInputSlotArea() {
        return addInputSlotArea;
    }

    public JIPipeDesktopGraphNodeUIAddSlotButtonActiveArea getAddOutputSlotArea() {
        return addOutputSlotArea;
    }

    protected void updateSlots() {
        JIPipeGraph graph = node.getParentGraph();

        inputSlotMap.clear();
        outputSlotMap.clear();

        if (showInputs) {
            for (JIPipeInputDataSlot inputSlot : node.getInputSlots()) {
                inputSlot.getInfo().getParameterChangedEventEmitter().subscribeWeak(this);

                JIPipeDesktopGraphNodeUISlotActiveArea slotState = new JIPipeDesktopGraphNodeUISlotActiveArea(this, JIPipeSlotType.Input, inputSlot.getName(), inputSlot);
                slotState.setIcon(JIPipe.getDataTypes().getIconFor(inputSlot.getAcceptedDataType()).getImage());
                inputSlotMap.put(inputSlot.getName(), slotState);
                if (StringUtils.isNullOrEmpty(inputSlot.getInfo().getCustomName())) {
                    slotState.setSlotLabel(inputSlot.getName());
                    slotState.setSlotLabelIsCustom(false);
                } else {
                    slotState.setSlotLabel(inputSlot.getInfo().getCustomName());
                    slotState.setSlotLabelIsCustom(true);
                }
                if (slotState.getSlot().getInfo().getRole() == JIPipeDataSlotRole.Parameters && slotState.getSlotName().equals(JIPipeParameterSlotAlgorithm.SLOT_PARAMETERS)) {
                    slotState.setSlotLabel("Parameters");
                }
                if (graph != null && graph.containsNode(inputSlot)) {
                    if (!inputSlot.getInfo().isOptional() && graph.getGraph().inDegreeOf(inputSlot) <= 0 && !(inputSlot.getNode() instanceof GraphWrapperAlgorithmInput)) {
                        slotState.setSlotStatus(SlotStatus.Unconnected);
                    } else {
                        slotState.setSlotStatus(SlotStatus.Default);
                    }
                }
            }
        }

        Map<String, JIPipeDataTable> cachedData = null;
        if (graph != null && graph.getProject() != null) {
            cachedData = graph.getProject().getCache().query(node, node.getUUIDInParentGraph(), new JIPipeProgressInfo());
        }

        if (showOutputs) {
            for (JIPipeDataSlot outputSlot : node.getOutputSlots()) {
                outputSlot.getInfo().getParameterChangedEventEmitter().subscribeWeak(this);

                JIPipeDesktopGraphNodeUISlotActiveArea slotState = new JIPipeDesktopGraphNodeUISlotActiveArea(this, JIPipeSlotType.Output, outputSlot.getName(), outputSlot);
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
        }

        // Special case for project compartments
        if (graph != null && graph.getProject() != null && node instanceof JIPipeProjectCompartment) {
            for (Map.Entry<String, JIPipeDesktopGraphNodeUISlotActiveArea> entry : outputSlotMap.entrySet()) {
                JIPipeProjectCompartmentOutput outputNode = ((JIPipeProjectCompartment) node).getOutputNode(entry.getKey());
                if(outputNode != null) {
                    cachedData = graph.getProject().getCache().query(outputNode, outputNode.getUUIDInParentGraph(), new JIPipeProgressInfo());
                    if (cachedData != null && !cachedData.isEmpty()) {
                        entry.getValue().setSlotStatus(SlotStatus.Cached);
                    }
                }
            }
        }

        updateSize();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 1) {
            if (currentActiveArea instanceof JIPipeDesktopGraphNodeUISlotButtonActiveArea || currentActiveArea instanceof JIPipeDesktopGraphNodeUISlotActiveArea && SwingUtilities.isRightMouseButton(e)) {
                JIPipeDesktopGraphNodeUISlotActiveArea slotState;
                if (currentActiveArea instanceof JIPipeDesktopGraphNodeUISlotButtonActiveArea) {
                    slotState = ((JIPipeDesktopGraphNodeUISlotButtonActiveArea) currentActiveArea).getUISlot();
                } else if (currentActiveArea instanceof JIPipeDesktopGraphNodeUISlotActiveArea) {
                    slotState = (JIPipeDesktopGraphNodeUISlotActiveArea) currentActiveArea;
                } else {
                    return;
                }
                openSlotMenu(slotState, e);
                e.consume();
            } else if (currentActiveArea instanceof JIPipeDesktopGraphNodeUIAddSlotButtonActiveArea) {
                openAddSlotDialog(((JIPipeDesktopGraphNodeUIAddSlotButtonActiveArea) currentActiveArea).getSlotType());
                e.consume();
            } else if (currentActiveArea instanceof JIPipeDesktopGraphNodeUIRunNodeActiveArea) {
                openRunNodeMenu(e);
                e.consume();
            }
        } else {
            if (currentActiveArea instanceof JIPipeDesktopGraphNodeUISlotButtonActiveArea || currentActiveArea instanceof JIPipeDesktopGraphNodeUISlotActiveArea && SwingUtilities.isLeftMouseButton(e)) {
                JIPipeDesktopGraphNodeUISlotActiveArea slotState;
                if (currentActiveArea instanceof JIPipeDesktopGraphNodeUISlotButtonActiveArea) {
                    slotState = ((JIPipeDesktopGraphNodeUISlotButtonActiveArea) currentActiveArea).getUISlot();
                } else if (currentActiveArea instanceof JIPipeDesktopGraphNodeUISlotActiveArea) {
                    slotState = (JIPipeDesktopGraphNodeUISlotActiveArea) currentActiveArea;
                } else {
                    return;
                }
                if (slotState.isInput()) {
                    e.consume();
                    openInputAlgorithmFinder(slotState.getSlot());
                } else {
                    e.consume();
                    openOutputAlgorithmFinder(slotState.getSlot());
                }
            }
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
                        JOptionPane.showMessageDialog(getDesktopWorkbench().getWindow(),
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

    private void openSlotMenu(JIPipeDesktopGraphNodeUISlotActiveArea slotState, MouseEvent mouseEvent) {
        JIPipeDataSlot slot = slotState.getSlot();
        if (slot == null) {
            return;
        }
        JPopupMenu menu = new JPopupMenu();

        openSlotMenuGenerateInformationItems(slotState, slot, menu);

        UIUtils.addSeparatorIfNeeded(menu);

        // Connection menus
        if (slotState.isInput()) {
            openSlotMenuAddInputSlotMenuItems(slot, menu);
        } else {
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
            moveUpButton.addActionListener(e -> moveSlotLeft(slotState.getSlot()));
            menu.add(moveUpButton);

            JMenuItem moveDownButton = new JMenuItem("Move to the right",
                    UIUtils.getIconFromResources("actions/go-right.png"));
            moveDownButton.setToolTipText("Reorders the slots");
            moveDownButton.addActionListener(e -> moveSlotRight(slotState.getSlot()));
            menu.add(moveDownButton);
        }

        MouseEvent convertMouseEvent = SwingUtilities.convertMouseEvent(graphCanvasUI, mouseEvent, this);
        Point mousePosition = convertMouseEvent.getPoint();

        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {

            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                getGraphCanvasUI().setCurrentConnectionDragSource(null);
                getGraphCanvasUI().setCurrentConnectionDragTarget(null);
                getGraphCanvasUI().setDisconnectHighlight(null);
                getGraphCanvasUI().setConnectHighlight(null);
                invalidateAndRepaint(false, true);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                getGraphCanvasUI().setCurrentConnectionDragSource(null);
                getGraphCanvasUI().setCurrentConnectionDragTarget(null);
                getGraphCanvasUI().setDisconnectHighlight(null);
                getGraphCanvasUI().setConnectHighlight(null);
                invalidateAndRepaint(false, true);
            }
        });

        menu.show(this, mousePosition.x, mousePosition.y);
    }

    public JIPipeDesktopGraphNodeUISlotActiveArea getSlotActiveArea(JIPipeDataSlot slot) {
        return slot.isInput() ? inputSlotMap.get(slot.getName()) : outputSlotMap.get(slot.getName());
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
                JIPipeDesktopGraphNodeUISlotActiveArea slotActiveArea = getSlotActiveArea(slot);
                if (slotActiveArea != null) {
                    openSlotMenuInstallHighlightForDisconnect(slotActiveArea, disconnectButton, targetSlots);
                }
                menu.add(disconnectButton);

                UIUtils.addSeparatorIfNeeded(menu);
            }
        }

        UUID compartment = graphCanvasUI.getCompartmentUUID();
        Set<JIPipeDataSlot> availableTargets = getGraphCanvasUI().getGraph().getAvailableTargets(slot, true, true);
        availableTargets.removeIf(s -> !s.getNode().isVisibleIn(compartment));

        JMenuItem findAlgorithmButton = new JMenuItem("Find matching algorithm ...", UIUtils.getIconFromResources("actions/find.png"));
        findAlgorithmButton.setToolTipText("Opens a tool to find a matching algorithm based on the data");
        findAlgorithmButton.addActionListener(e -> openOutputAlgorithmFinder(slot));
        menu.add(findAlgorithmButton);

        if (!availableTargets.isEmpty()) {
            JMenu connectMenu = new JMenu("Connect to ...");
            connectMenu.setIcon(UIUtils.getIconFromResources("actions/plug.png"));
            openSlotMenuAddOutputConnectTargetSlotItems(slot, availableTargets, connectMenu);
            menu.add(connectMenu);
        }
        if (!targetSlots.isEmpty()) {
            JMenu manageMenu = new JMenu("Manage existing connections ...");
            manageMenu.setIcon(UIUtils.getIconFromResources("actions/lines-connector.png"));
            openSlotMenuAddOutputManageExistingConnectionsMenuItems(slot, targetSlots, manageMenu);
            menu.add(manageMenu);
        }
        if (!targetSlots.isEmpty()) {
            // Customize menu
            JMenu edgeMenu = new JMenu("Customize edges");
            edgeMenu.setIcon(UIUtils.getIconFromResources("actions/draw-connector.png"));
            edgeMenu.add(UIUtils.createMenuItem("Draw all outputs as elbow",
                    "All outgoing edges will be drawn as elbow",
                    UIUtils.getIconFromResources("actions/standard-connector.png"),
                    () -> setOutputEdgesShape(JIPipeGraphEdge.Shape.Elbow)));
            edgeMenu.add(UIUtils.createMenuItem("Draw all outputs as line",
                    "All outgoing edges will be drawn as line",
                    UIUtils.getIconFromResources("actions/draw-line.png"),
                    () -> setOutputEdgesShape(JIPipeGraphEdge.Shape.Line)));
            edgeMenu.addSeparator();
            edgeMenu.add(UIUtils.createMenuItem("Always show all outputs",
                    "All output edges are shown regardless of their length",
                    UIUtils.getIconFromResources("actions/eye.png"),
                    () -> setOutputEdgesVisibility(JIPipeGraphEdge.Visibility.AlwaysVisible)));
//            edgeMenu.add(UIUtils.createMenuItem("Always hide all outputs (with label)",
//                    "All output edges are hidden (displayed as dashed line) regardless of their length. A label is displayed at the targets that contains information about the source.",
//                    UIUtils.getIconFromResources("actions/eye-slash.png"),
//                    () -> setOutputEdgesVisibility(JIPipeGraphEdge.Visibility.AlwaysHiddenWithLabel)));
            edgeMenu.add(UIUtils.createMenuItem("Always hide all outputs ",
                    "All output edges are hidden (displayed as dashed line) regardless of their length",
                    UIUtils.getIconFromResources("actions/eye-slash.png"),
                    () -> setOutputEdgesVisibility(JIPipeGraphEdge.Visibility.AlwaysHidden)));
            edgeMenu.add(UIUtils.createMenuItem("Auto-hide long output edges",
                    "Long edges are automatically hidden (displayed as dashed line).",
                    UIUtils.getIconFromResources("actions/fcitx-remind-active.png"),
                    () -> setOutputEdgesVisibility(JIPipeGraphEdge.Visibility.Smart)));
//            edgeMenu.add(UIUtils.createMenuItem("Auto-hide long output edges (without label)",
//                    "Long edges are automatically hidden (displayed as dashed line).",
//                    UIUtils.getIconFromResources("actions/fcitx-remind-active.png"),
//                    () -> setOutputEdgesVisibility(JIPipeGraphEdge.Visibility.SmartSilent)));
            menu.add(edgeMenu);
        }

        UIUtils.addSeparatorIfNeeded(menu);

        openSlotMenuAddOutputSlotEditItems(slot, menu);

        UIUtils.addSeparatorIfNeeded(menu);

        if (!(getNode() instanceof JIPipeProjectCompartment)) {
            if (slot.getInfo().isStoreToDisk()) {
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
        }

    }

    private void setOutputEdgesVisibility(JIPipeGraphEdge.Visibility visibility) {
        if (getGraphCanvasUI().getHistoryJournal() != null) {
            getGraphCanvasUI().getHistoryJournal().snapshot("Set edge visibility",
                    "Set the visibility of all output edges of " + node.getDisplayName(),
                    getNode().getCompartmentUUIDInParentGraph(),
                    UIUtils.getIconFromResources("actions/eye.png"));
        }
        for (JIPipeOutputDataSlot outputSlot : node.getOutputSlots()) {
            for (JIPipeGraphEdge graphEdge : node.getParentGraph().getGraph().outgoingEdgesOf(outputSlot)) {
                graphEdge.setUiVisibility(visibility);
            }
        }
        invalidateAndRepaint(false, true);
    }

    private void setOutputEdgesShape(JIPipeGraphEdge.Shape shape) {
        if (getGraphCanvasUI().getHistoryJournal() != null) {
            getGraphCanvasUI().getHistoryJournal().snapshot("Set edge shape",
                    "Set the shape of all output edges of " + node.getDisplayName(),
                    getNode().getCompartmentUUIDInParentGraph(),
                    UIUtils.getIconFromResources("actions/eye.png"));
        }
        for (JIPipeOutputDataSlot outputSlot : node.getOutputSlots()) {
            for (JIPipeGraphEdge graphEdge : node.getParentGraph().getGraph().outgoingEdgesOf(outputSlot)) {
                graphEdge.setUiShape(shape);
            }
        }
        invalidateAndRepaint(false, true);
    }

    private void setInputEdgesVisibility(JIPipeGraphEdge.Visibility visibility) {
        if (getGraphCanvasUI().getHistoryJournal() != null) {
            getGraphCanvasUI().getHistoryJournal().snapshot("Set edge visibility",
                    "Set the visibility of all input edges of " + node.getDisplayName(),
                    getNode().getCompartmentUUIDInParentGraph(),
                    UIUtils.getIconFromResources("actions/eye.png"));
        }
        for (JIPipeDataSlot inputSlot : node.getInputSlots()) {
            for (JIPipeGraphEdge graphEdge : node.getParentGraph().getGraph().incomingEdgesOf(inputSlot)) {
                graphEdge.setUiVisibility(visibility);
            }
        }
        invalidateAndRepaint(false, true);
    }

    private void setInputEdgesShape(JIPipeGraphEdge.Shape shape) {
        if (getGraphCanvasUI().getHistoryJournal() != null) {
            getGraphCanvasUI().getHistoryJournal().snapshot("Set edge edge",
                    "Set the shape of all input edges of " + node.getDisplayName(),
                    getNode().getCompartmentUUIDInParentGraph(),
                    UIUtils.getIconFromResources("actions/eye.png"));
        }
        for (JIPipeDataSlot inputSlot : node.getInputSlots()) {
            for (JIPipeGraphEdge graphEdge : node.getParentGraph().getGraph().incomingEdgesOf(inputSlot)) {
                graphEdge.setUiShape(shape);
            }
        }
        invalidateAndRepaint(false, true);
    }

    private void openSlotMenuAddOutputManageExistingConnectionsMenuItems(JIPipeDataSlot slot, Set<JIPipeDataSlot> targetSlots, JMenu menu) {

        if (!targetSlots.isEmpty()) {
            JMenuItem rewireItem = new JMenuItem("Rewire to different output ...", UIUtils.getIconFromResources("actions/go-jump.png"));
            rewireItem.setToolTipText("Opens a tool that allows to rewire the connections of this slot to another output.");
            rewireItem.addActionListener(e -> openRewireOutputTool(slot, targetSlots));
            menu.add(rewireItem);
        }

        for (JIPipeDataSlot targetSlot : sortSlotsByDistance(slot, targetSlots)) {
            JMenu targetSlotMenu = new JMenu("<html>" + targetSlot.getName() + "<br><small>" + targetSlot.getNode().getDisplayName() + "</small></html>");
            targetSlotMenu.setIcon(JIPipe.getDataTypes().getIconFor(targetSlot.getAcceptedDataType()));

            JMenuItem disconnectButton = new JMenuItem("Disconnect", UIUtils.getIconFromResources("actions/cancel.png"));
            disconnectButton.addActionListener(e -> getGraphCanvasUI().disconnectAll(slot, Collections.singleton(targetSlot)));
            JIPipeDesktopGraphNodeUISlotActiveArea slotActiveArea = getSlotActiveArea(slot);
            if (slotActiveArea != null) {
                openSlotMenuInstallHighlightForDisconnect(slotActiveArea, disconnectButton, Collections.singleton(targetSlot));
            }
            targetSlotMenu.add(disconnectButton);

            JIPipeGraphEdge edge = getGraphCanvasUI().getGraph().getGraph().getEdge(slot, targetSlot);

            // Shape menu
            openSlotMenuAddShapeToggle(slot, targetSlotMenu, edge);

            // Visibility options
            openSlotMenuAddVisibilityToggle(slot, targetSlotMenu, edge);

            menu.add(targetSlotMenu);
        }
    }

    private void openRewireOutputTool(JIPipeDataSlot slot, Set<JIPipeDataSlot> targetSlots) {
        JIPipeDesktopRewireConnectionsToolUI ui = new JIPipeDesktopRewireConnectionsToolUI(graphCanvasUI, slot, targetSlots);
        ui.setTitle("Rewire output");
        ui.setLocationRelativeTo(graphCanvasUI.getGraphEditorUI());
        ui.setVisible(true);
        ui.revalidate();
        ui.repaint();
    }

    private void openSlotMenuAddVisibilityToggle(JIPipeDataSlot slot, JMenu menu, JIPipeGraphEdge edge) {

        menu.addSeparator();

        if (edge.getUiVisibility() != JIPipeGraphEdge.Visibility.AlwaysVisible) {
            menu.add(UIUtils.createMenuItem("Show all outputs",
                    "The edge shown regardless of its length",
                    UIUtils.getIconFromResources("actions/eye.png"),
                    () -> setEdgeVisibility(slot, edge, JIPipeGraphEdge.Visibility.AlwaysVisible)));
        }
        if (edge.getUiVisibility() != JIPipeGraphEdge.Visibility.AlwaysHidden) {
            menu.add(UIUtils.createMenuItem("Always hide all outputs",
                    "All output edges are hidden (displayed as dashed line) regardless of their length",
                    UIUtils.getIconFromResources("actions/eye-slash.png"),
                    () -> setEdgeVisibility(slot, edge, JIPipeGraphEdge.Visibility.AlwaysHidden)));
        }
//        if (edge.getUiVisibility() != JIPipeGraphEdge.Visibility.AlwaysHiddenWithLabel) {
//            menu.add(UIUtils.createMenuItem("Always hide all outputs (with label)",
//                    "All output edges are hidden (displayed as dashed line) regardless of their length. A label is displayed at the targets that contains information about the source.",
//                    UIUtils.getIconFromResources("actions/eye-slash.png"),
//                    () -> setEdgeVisibility(slot, edge, JIPipeGraphEdge.Visibility.AlwaysHiddenWithLabel)));
//        }
        if (edge.getUiVisibility() != JIPipeGraphEdge.Visibility.Smart) {
            menu.add(UIUtils.createMenuItem("Auto-hide long output edges",
                    "Long edges are automatically hidden (displayed as dashed line).",
                    UIUtils.getIconFromResources("actions/fcitx-remind-active.png"),
                    () -> setEdgeVisibility(slot, edge, JIPipeGraphEdge.Visibility.Smart)));
        }
//        if (edge.getUiVisibility() != JIPipeGraphEdge.Visibility.SmartSilent) {
//            menu.add(UIUtils.createMenuItem("Auto-hide long output edges (without label)",
//                    "Long edges are automatically hidden (displayed as dashed line).",
//                    UIUtils.getIconFromResources("actions/fcitx-remind-active.png"),
//                    () -> setEdgeVisibility(slot, edge, JIPipeGraphEdge.Visibility.SmartSilent)));
//        }
    }

    private void openSlotMenuAddInputSlotMenuItems(JIPipeDataSlot slot, JPopupMenu menu) {

        Set<JIPipeDataSlot> sourceSlots = getGraphCanvasUI().getGraph().getInputIncomingSourceSlots(slot);

        if (!sourceSlots.isEmpty()) {
            JMenuItem disconnectButton = new JMenuItem("Disconnect all", UIUtils.getIconFromResources("actions/cancel.png"));
            disconnectButton.addActionListener(e -> getGraphCanvasUI().disconnectAll(slot, sourceSlots));
            JIPipeDesktopGraphNodeUISlotActiveArea slotActiveArea = getSlotActiveArea(slot);
            if (slotActiveArea != null) {
                openSlotMenuInstallHighlightForDisconnect(slotActiveArea, disconnectButton, sourceSlots);
            }
            menu.add(disconnectButton);
        }

        UIUtils.addSeparatorIfNeeded(menu);

        JMenuItem findAlgorithmButton = new JMenuItem("Find matching algorithm ...", UIUtils.getIconFromResources("actions/find.png"));
        findAlgorithmButton.setToolTipText("Opens a tool to find a matching algorithm based on the data");
        findAlgorithmButton.addActionListener(e -> openInputAlgorithmFinder(slot));
        menu.add(findAlgorithmButton);

        Set<JIPipeDataSlot> availableSources = getGraphCanvasUI().getGraph().getAvailableSources(slot, true, false);
        if (!availableSources.isEmpty()) {
            JMenu connectMenu = new JMenu("Connect to ...");
            connectMenu.setIcon(UIUtils.getIconFromResources("actions/plug.png"));
            openSlotMenuAddInputConnectSourceSlotItems(slot, availableSources, connectMenu);
            menu.add(connectMenu);
        }

        if (!sourceSlots.isEmpty()) {
            JMenu manageMenu = new JMenu("Manage existing connections ...");
            manageMenu.setIcon(UIUtils.getIconFromResources("actions/lines-connector.png"));
            openSlotMenuAddInputManageExistingConnectionsMenuItems(slot, sourceSlots, manageMenu);
            menu.add(manageMenu);
        }
        if (!sourceSlots.isEmpty()) {
            // Customize menu
            JMenu edgeMenu = new JMenu("Customize edges");
            edgeMenu.setIcon(UIUtils.getIconFromResources("actions/draw-connector.png"));
            edgeMenu.add(UIUtils.createMenuItem("Draw all inputs as elbow",
                    "All outgoing edges will be drawn as elbow",
                    UIUtils.getIconFromResources("actions/standard-connector.png"),
                    () -> setInputEdgesShape(JIPipeGraphEdge.Shape.Elbow)));
            edgeMenu.add(UIUtils.createMenuItem("Draw all inputs as line",
                    "All outgoing edges will be drawn as line",
                    UIUtils.getIconFromResources("actions/draw-line.png"),
                    () -> setInputEdgesShape(JIPipeGraphEdge.Shape.Line)));
            edgeMenu.addSeparator();
            edgeMenu.add(UIUtils.createMenuItem("Always show all inputs",
                    "All output edges are shown regardless of their length",
                    UIUtils.getIconFromResources("actions/eye.png"),
                    () -> setInputEdgesVisibility(JIPipeGraphEdge.Visibility.AlwaysVisible)));
//            edgeMenu.add(UIUtils.createMenuItem("Always hide all inputs (with label)",
//                    "All output edges are hidden (displayed as dashed line) regardless of their length. A label is displayed at the targets that contains information about the source.",
//                    UIUtils.getIconFromResources("actions/eye-slash.png"),
//                    () -> setInputEdgesVisibility(JIPipeGraphEdge.Visibility.AlwaysHiddenWithLabel)));
            edgeMenu.add(UIUtils.createMenuItem("Always hide all inputs",
                    "All output edges are hidden (displayed as dashed line) regardless of their length",
                    UIUtils.getIconFromResources("actions/eye-slash.png"),
                    () -> setInputEdgesVisibility(JIPipeGraphEdge.Visibility.AlwaysHidden)));
            edgeMenu.add(UIUtils.createMenuItem("Auto-hide long input edges",
                    "Long edges are automatically hidden (displayed as dashed line).",
                    UIUtils.getIconFromResources("actions/fcitx-remind-active.png"),
                    () -> setInputEdgesVisibility(JIPipeGraphEdge.Visibility.Smart)));
//            edgeMenu.add(UIUtils.createMenuItem("Auto-hide long input edges (without label)",
//                    "Long edges are automatically hidden (displayed as dashed line).",
//                    UIUtils.getIconFromResources("actions/fcitx-remind-active.png"),
//                    () -> setInputEdgesVisibility(JIPipeGraphEdge.Visibility.SmartSilent)));
            menu.add(edgeMenu);
        }

        UIUtils.addSeparatorIfNeeded(menu);

        openSlotMenuAddInputSlotEditItems(slot, sourceSlots, menu);
    }

    private void openSlotMenuAddOutputConnectTargetSlotItems(JIPipeDataSlot slot, Set<JIPipeDataSlot> availableTargets, JMenu menu) {
        JIPipeDesktopGraphNodeUISlotActiveArea slotActiveArea = getSlotActiveArea(slot);
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
            JIPipeDesktopGraphNodeUI targetNodeUI = getGraphCanvasUI().getNodeUIs().getOrDefault(target.getNode(), null);

            if (targetNodeUI != null) {
                openSlotMenuInstallHighlightForConnect(slotActiveArea, target, connectButton);
            }

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
                JIPipeDesktopGraphNodeUISlotActiveArea slotActiveArea = getSlotActiveArea(slot);
                if (slotActiveArea != null) {
                    openSlotMenuInstallHighlightForDisconnect(slotActiveArea, deleteButton, sourceSlots);
                }
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

    private void openSlotMenuAddInputManageExistingConnectionsMenuItems(JIPipeDataSlot slot, Set<JIPipeDataSlot> sourceSlots, JMenu menu) {

        if (!sourceSlots.isEmpty()) {
            JMenuItem rewireItem = new JMenuItem("Rewire to different input ...", UIUtils.getIconFromResources("actions/go-jump.png"));
            rewireItem.setToolTipText("Opens a tool that allows to rewire the connections of this slot to another input.");
            rewireItem.addActionListener(e -> openRewireInputTool(slot, sourceSlots));
            menu.add(rewireItem);
        }

        JIPipeDesktopGraphNodeUISlotActiveArea slotActiveArea = getSlotActiveArea(slot);
        for (JIPipeDataSlot sourceSlot : sortSlotsByDistance(slot, sourceSlots)) {
            JMenu sourceSlotMenu = new JMenu("<html>" + sourceSlot.getName() + "<br><small>" + sourceSlot.getNode().getDisplayName() + "</small></html>");
            sourceSlotMenu.setIcon(JIPipe.getDataTypes().getIconFor(sourceSlot.getAcceptedDataType()));

            JMenuItem disconnectButton = new JMenuItem("Disconnect", UIUtils.getIconFromResources("actions/cancel.png"));
            disconnectButton.addActionListener(e -> getGraphCanvasUI().disconnectAll(slot, Collections.singleton(sourceSlot)));
            if (slotActiveArea != null) {
                openSlotMenuInstallHighlightForDisconnect(slotActiveArea, disconnectButton, Collections.singleton(sourceSlot));
            }
            sourceSlotMenu.add(disconnectButton);

            JIPipeGraphEdge edge = getGraphCanvasUI().getGraph().getGraph().getEdge(sourceSlot, slot);

            // Shape menu
            openSlotMenuAddShapeToggle(slot, sourceSlotMenu, edge);

            // Visibility options
            openSlotMenuAddVisibilityToggle(slot, sourceSlotMenu, edge);

            menu.add(sourceSlotMenu);
        }
    }

    private void openRewireInputTool(JIPipeDataSlot slot, Set<JIPipeDataSlot> sourceSlots) {
        JIPipeDesktopRewireConnectionsToolUI ui = new JIPipeDesktopRewireConnectionsToolUI(graphCanvasUI, slot, sourceSlots);
        ui.setTitle("Rewire input");
        ui.setLocationRelativeTo(graphCanvasUI.getGraphEditorUI());
        ui.setVisible(true);
        ui.revalidate();
        ui.repaint();
    }

    private void openSlotMenuAddInputConnectSourceSlotItems(JIPipeDataSlot slot, Set<JIPipeDataSlot> availableSources, JMenu menu) {
        UUID compartment = graphCanvasUI.getCompartmentUUID();
        availableSources.removeIf(s -> !s.getNode().isVisibleIn(compartment));
        JIPipeDesktopGraphNodeUISlotActiveArea slotActiveArea = getSlotActiveArea(slot);

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
            if (slotActiveArea != null) {
                openSlotMenuInstallHighlightForConnect(slotActiveArea, source, connectButton);
            }
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
                invalidateAndRepaint(false, true);
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
                invalidateAndRepaint(false, true);
            });
            menu.add(setShapeItem);
        }
    }

    private void setEdgeVisibility(JIPipeDataSlot slot, JIPipeGraphEdge edge, JIPipeGraphEdge.Visibility visible) {
        if (getGraphCanvasUI().getHistoryJournal() != null) {
            getGraphCanvasUI().getHistoryJournal().snapshot("Set edge visibility",
                    slot.getDisplayName(),
                    getNode().getCompartmentUUIDInParentGraph(),
                    UIUtils.getIconFromResources("actions/eye.png"));
        }
        edge.setUiVisibility(visible);
        invalidateAndRepaint(false, true);
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

    private void openSlotMenuInstallHighlightForConnect(JIPipeDesktopGraphNodeUISlotActiveArea current, JIPipeDataSlot source, JMenuItem connectButton) {
        connectButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                JIPipeDesktopGraphNodeUI sourceNodeUI = getGraphCanvasUI().getNodeUIs().getOrDefault(source.getNode(), null);
                if (sourceNodeUI != null) {
                    if (source.isOutput()) {
                        JIPipeDesktopGraphNodeUISlotActiveArea sourceUI = sourceNodeUI.getOutputSlotMap().getOrDefault(source.getName(), null);
                        if (sourceUI != null) {
                            getGraphCanvasUI().setConnectHighlight(new JIPipeDesktopGraphCanvasUI.ConnectHighlight(sourceUI, current));
                        }
                    } else {
                        JIPipeDesktopGraphNodeUISlotActiveArea sourceUI = sourceNodeUI.getInputSlotMap().getOrDefault(source.getName(), null);
                        if (sourceUI != null) {
                            getGraphCanvasUI().setConnectHighlight(new JIPipeDesktopGraphCanvasUI.ConnectHighlight(current, sourceUI));
                        }
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                getGraphCanvasUI().setConnectHighlight(null);
            }
        });
    }

    private void openSlotMenuInstallHighlightForDisconnect(JIPipeDesktopGraphNodeUISlotActiveArea slotActiveArea, JMenuItem disconnectButton, Set<JIPipeDataSlot> sourceSlots) {
        disconnectButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                getGraphCanvasUI().setDisconnectHighlight(new JIPipeDesktopGraphCanvasUI.DisconnectHighlight(slotActiveArea, sourceSlots));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                getGraphCanvasUI().setDisconnectHighlight(null);
            }
        });
    }


    private void setSaveOutputs(JIPipeDataSlot slot, boolean saveOutputs) {
        slot.getInfo().setStoreToDisk(saveOutputs);
    }

    private void openSlotMenuGenerateInformationItems(JIPipeDesktopGraphNodeUISlotActiveArea slotState, JIPipeDataSlot slot, JPopupMenu menu) {
        // Information item
        JIPipeDataInfo dataInfo = JIPipeDataInfo.getInstance(slot.getAcceptedDataType());
        ViewOnlyMenuItem infoItem = new ViewOnlyMenuItem("<html>" + dataInfo.getName() + "<br><small>" + StringUtils.orElse(dataInfo.getDescription(), "No description provided") + "</small></html>", JIPipe.getDataTypes().getIconFor(slot.getAcceptedDataType()));
        menu.add(infoItem);

        // Optional info
        if (slot.getInfo().isOptional()) {
            menu.add(new ViewOnlyMenuItem("<html>Optional slot<br><small>This slot requires no input connections.</small></html>", UIUtils.getIconFromResources("actions/checkbox.png")));
        }

        // Role information item
        if (slot.getInfo().getRole() == JIPipeDataSlotRole.Parameters) {
            ViewOnlyMenuItem roleInfoItem = new ViewOnlyMenuItem("<html>Parameter-like data<br><small>This slot contains parametric data that is not considered for iteration step generation.</small></html>", UIUtils.getIconFromResources("actions/wrench.png"));
            menu.add(roleInfoItem);
        }
        else if(slot.getInfo().getRole() == JIPipeDataSlotRole.ParametersLooping) {
            ViewOnlyMenuItem roleInfoItem = new ViewOnlyMenuItem("<html>Parameter-like data<br><small>This slot contains parametric data that is not considered for iteration step generation. Workloads may be repeated per input of this slot.</small></html>",
                    UIUtils.getIconFromResources("actions/wrench.png"));
            menu.add(roleInfoItem);
        }

        // Input info
        List<ViewOnlyMenuItem> additionalItems = new ArrayList<>();
        node.createUIInputSlotIconDescriptionMenuItems(slot.getName(), additionalItems);
        for (ViewOnlyMenuItem additionalItem : additionalItems) {
            menu.add(additionalItem);
        }

        // Missing input item
        if (slotState.getSlotStatus() == SlotStatus.Unconnected) {
            menu.add(new ViewOnlyMenuItem("<html>This slot is not connected to an output!<br/><small>The node will not be able to work with a missing input.</small></html>", UIUtils.getIconFromResources("emblems/warning.png")));
        }

        // Cache info
        if (node.getParentGraph() != null) {
            JIPipeGraph graph = node.getParentGraph();
            Map<String, JIPipeDataTable> cachedData = null;
            if (graph != null && graph.getProject() != null) {
                cachedData = graph.getProject().getCache().query(node, node.getUUIDInParentGraph(), new JIPipeProgressInfo());
            }
            if (cachedData != null && cachedData.containsKey(slotState.getSlotName())) {
                int itemCount = cachedData.get(slotState.getSlotName()).getRowCount();
                ViewOnlyMenuItem cacheInfoItem = new ViewOnlyMenuItem("<html>Outputs are cached<br/><small>" + (itemCount == 1 ? "1 item" : itemCount + " items") + " </small></html>",
                        UIUtils.getIconFromResources("actions/database.png"));
                menu.add(cacheInfoItem);
            }
        }
    }

    private void editSlot(JIPipeDataSlot slot) {
        if (!JIPipeDesktopProjectWorkbench.canModifySlots(getDesktopWorkbench()))
            return;
        JIPipeDesktopEditAlgorithmSlotPanel.showDialog(this, getGraphCanvasUI().getHistoryJournal(), slot);
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
        updateView(false, true, true);
        getGraphCanvasUI().getDesktopWorkbench().setProjectModified(true);
    }

    private void deleteSlot(JIPipeDataSlot slot) {
        if (!JIPipeDesktopProjectWorkbench.canModifySlots(getDesktopWorkbench()))
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

    private void moveSlotRight(JIPipeDataSlot slot) {
        if (slot != null) {
            if (getGraphCanvasUI().getHistoryJournal() != null) {
                getGraphCanvasUI().getHistoryJournal().snapshotBeforeMoveSlot(slot, slot.getNode().getCompartmentUUIDInParentGraph());
            }
            ((JIPipeMutableSlotConfiguration) getNode().getSlotConfiguration()).moveDown(slot.getName(), slot.getSlotType());
            invalidateAndRepaint(true, true);
        }
    }

    private void moveSlotLeft(JIPipeDataSlot slot) {
        if (slot != null) {
            if (getGraphCanvasUI().getHistoryJournal() != null) {
                getGraphCanvasUI().getHistoryJournal().snapshotBeforeMoveSlot(slot, slot.getNode().getCompartmentUUIDInParentGraph());
            }
            ((JIPipeMutableSlotConfiguration) getNode().getSlotConfiguration()).moveUp(slot.getName(), slot.getSlotType());
            invalidateAndRepaint(true, true);
        }
    }

    private void openOutputAlgorithmFinder(JIPipeDataSlot slot) {
//        JIPipeAlgorithmTargetFinderUI algorithmFinderUI = new JIPipeAlgorithmTargetFinderUI(getGraphCanvasUI(), slot);
//        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Find matching algorithm");
//        UIUtils.addEscapeListener(dialog);
//        dialog.setModal(true);
//        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
//        dialog.setContentPane(algorithmFinderUI);
//        dialog.pack();
//        dialog.setSize(800, 600);
//        dialog.setLocationRelativeTo(this);
//
//        algorithmFinderUI.getAlgorithmFinderSuccessEventEmitter().subscribeLambda((emitter, event) -> dialog.dispose());
//
        boolean layoutHelperEnabled = getGraphCanvasUI().getSettings() != null && getGraphCanvasUI().getSettings().isLayoutAfterAlgorithmFinder();
        if (layoutHelperEnabled) {
            Point cursorLocation = new Point();
            Point slotLocation = getSlotLocation(slot).min;
            cursorLocation.x = getX() + slotLocation.x;
            cursorLocation.y = getBottomY() + getGraphCanvasUI().getViewMode().getGridHeight();
            getGraphCanvasUI().setGraphEditCursor(cursorLocation);
            invalidateAndRepaint(false, true);
        }
//
//        dialog.setVisible(true);
        JIPipeDesktopNodeFinderDialogUI dialogUI = new JIPipeDesktopNodeFinderDialogUI(getGraphCanvasUI(), slot);
        dialogUI.setVisible(true);
    }

    private void openInputAlgorithmFinder(JIPipeDataSlot slot) {
//        JIPipeAlgorithmSourceFinderUI algorithmFinderUI = new JIPipeAlgorithmSourceFinderUI(getGraphCanvasUI(), slot);
//        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Find matching algorithm");
//        UIUtils.addEscapeListener(dialog);
//        dialog.setModal(true);
//        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
//        dialog.setContentPane(algorithmFinderUI);
//        dialog.pack();
//        dialog.setSize(800, 600);
//        dialog.setLocationRelativeTo(this);
//
//        algorithmFinderUI.getAlgorithmFinderSuccessEventEmitter().subscribeLambda((emitter, event) -> dialog.dispose());
//
        boolean layoutHelperEnabled = getGraphCanvasUI().getSettings() != null && getGraphCanvasUI().getSettings().isLayoutAfterAlgorithmFinder();
        if (layoutHelperEnabled) {
            Point cursorLocation = new Point();
            Point slotLocation = getSlotLocation(slot).min;
            cursorLocation.x = getX() + slotLocation.x;
            cursorLocation.y = getY() - getGraphCanvasUI().getViewMode().getGridHeight() * 4;
            getGraphCanvasUI().setGraphEditCursor(cursorLocation);
            invalidateAndRepaint(false, true);
        }
//
//        dialog.setVisible(true);
        JIPipeDesktopNodeFinderDialogUI dialogUI = new JIPipeDesktopNodeFinderDialogUI(getGraphCanvasUI(), slot);
        dialogUI.setVisible(true);
    }

    private void openAddSlotDialog(JIPipeSlotType slotType) {
        if (!JIPipeDesktopProjectWorkbench.canModifySlots(getDesktopWorkbench())) {
            JOptionPane.showMessageDialog(graphCanvasUI.getDesktopWorkbench().getWindow(), "Slots cannot be modified!", "Add slot", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JIPipeDesktopAddAlgorithmSlotPanel.showDialog(this, graphCanvasUI.getHistoryJournal(), node, slotType);
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
        invalidateAndRepaint(true, false);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseIsEntered = false;
        updateCurrentActiveArea(e);
        invalidateAndRepaint(true, false);
    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        updateCurrentActiveArea(e);
    }

    @Override
    public void onCacheModified(JIPipeCache.ModifiedEvent event) {
        updateView(false, true, false);
    }

    private void updateCurrentActiveArea(MouseEvent e) {
        if (mouseIsEntered) {
            MouseEvent mouseEvent = SwingUtilities.convertMouseEvent(getGraphCanvasUI(), e, this);
            Point mousePosition = mouseEvent.getPoint();
            for (JIPipeDesktopGraphNodeUIActiveArea activeArea : ImmutableList.copyOf(activeAreas)) {
                if (activeArea.getZoomedHitArea() != null && activeArea.getZoomedHitArea().contains(mousePosition)) {
                    if (currentActiveArea != activeArea) {
                        currentActiveArea = activeArea;
                        invalidateAndRepaint(true, false);
                    }
                    break;
                }
            }
        } else {
            if (currentActiveArea != null) {
                currentActiveArea = null;
                invalidateAndRepaint(true, false);
            }
        }
    }

    private void invalidateAndRepaint(boolean node, boolean canvas) {
        if (node) {
            nodeBufferInvalid = true;
        }
        if (canvas) {
            graphCanvasUI.repaintLowLag();
        } else {
            repaint(50);
        }
    }

    public void paintMinimap(Graphics2D graphics2D, int x, int y, int width, int height, BasicStroke defaultStroke, BasicStroke selectedStroke, Set<JIPipeDesktopGraphNodeUI> selection) {
        // Fill
        graphics2D.setColor(getFillColor());
        graphics2D.fillRect(x, y, width, height);

        // Runtime partition color
        if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
            if (getNode() instanceof JIPipeAlgorithm) {
                JIPipeRuntimePartition runtimePartition = ((JIPipeDesktopProjectWorkbench) getDesktopWorkbench()).getProject()
                        .getRuntimePartitions().get(((JIPipeAlgorithm) getNode()).getRuntimePartition().getIndex());
                if (runtimePartition.getColor().isEnabled()) {
                    graphics2D.setColor(runtimePartition.getColor().getContent());
                    graphics2D.fillPolygon(new int[]{x, x + width, x + width}, new int[]{y, y, y + height}, 3);
                }
            }
        }

        // Outline
        graphics2D.setStroke(selection.contains(this) ? selectedStroke : defaultStroke);
        if (getNode().isBookmarked()) {
            graphics2D.setColor(new Color(0x33cc33));
        } else {
            graphics2D.setColor(getBorderColor());
        }

        graphics2D.drawRect(x, y, width, height);

        // Icon
        ImageIcon icon = JIPipe.getInstance().getNodeRegistry().getIconFor(getNode().getInfo());
        int iconSize = Math.min(16, Math.min(width, height)) - 3;
        if (iconSize > 4) {
            graphics2D.drawImage(icon.getImage(),
                    x + (int) Math.round((width / 2.0) - (iconSize / 2.0)),
                    y + (int) Math.round((height / 2.0) - (iconSize / 2.0)),
                    iconSize,
                    iconSize,
                    null);
        }
    }

    public JIPipeDesktopGraphNodeUIActiveArea getCurrentActiveArea() {
        return currentActiveArea;
    }

    public enum SlotStatus {
        Default,
        Unconnected,
        Cached
    }

    public interface NodeUIActionRequestedEventListener {
        void onNodeUIActionRequested(NodeUIActionRequestedEvent event);
    }

    public interface DefaultNodeUIActionRequestedEventListener {
        void onDefaultNodeUIActionRequested(DefaultNodeUIActionRequestedEvent event);
    }

    /**
     * An action that is requested by an {@link JIPipeDesktopGraphNodeUI} and passed down to a {@link JIPipeDesktopGraphEditorUI}
     */
    public static class NodeUIActionRequestedEvent extends AbstractJIPipeEvent {
        private final JIPipeDesktopGraphNodeUI ui;
        private final JIPipeDesktopNodeUIAction action;

        /**
         * Initializes a new instance
         *
         * @param ui     the requesting UI
         * @param action the action parameter
         */
        public NodeUIActionRequestedEvent(JIPipeDesktopGraphNodeUI ui, JIPipeDesktopNodeUIAction action) {
            super(ui);
            this.ui = ui;
            this.action = action;
        }

        public JIPipeDesktopGraphNodeUI getUi() {
            return ui;
        }

        public JIPipeDesktopNodeUIAction getAction() {
            return action;
        }
    }

    public static class NodeUIActionRequestedEventEmitter extends JIPipeEventEmitter<NodeUIActionRequestedEvent, NodeUIActionRequestedEventListener> {

        @Override
        protected void call(NodeUIActionRequestedEventListener nodeUIActionRequestedEventListener, NodeUIActionRequestedEvent event) {
            nodeUIActionRequestedEventListener.onNodeUIActionRequested(event);
        }
    }

    /**
     * Triggered when an {@link JIPipeDesktopGraphNodeUI} requests a default action (double click)
     */
    public static class DefaultNodeUIActionRequestedEvent extends AbstractJIPipeEvent {

        private final JIPipeDesktopGraphNodeUI ui;

        /**
         * @param ui event source
         */
        public DefaultNodeUIActionRequestedEvent(JIPipeDesktopGraphNodeUI ui) {
            super(ui);
            this.ui = ui;
        }

        public JIPipeDesktopGraphNodeUI getUi() {
            return ui;
        }
    }

    public static class DefaultNodeUIActionRequestedEventEmitter extends JIPipeEventEmitter<DefaultNodeUIActionRequestedEvent, DefaultNodeUIActionRequestedEventListener> {

        @Override
        protected void call(DefaultNodeUIActionRequestedEventListener defaultNodeUIActionRequestedEventListener, DefaultNodeUIActionRequestedEvent event) {
            defaultNodeUIActionRequestedEventListener.onDefaultNodeUIActionRequested(event);
        }
    }
}
