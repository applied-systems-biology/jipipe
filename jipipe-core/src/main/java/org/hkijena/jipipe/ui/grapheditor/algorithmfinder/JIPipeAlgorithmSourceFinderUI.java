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

package org.hkijena.jipipe.ui.grapheditor.algorithmfinder;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.components.ColorIcon;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.SearchTextField;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.utils.RankedData;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A user interface to find a matching algorithm for the specified target slot
 */
public class JIPipeAlgorithmSourceFinderUI extends JPanel {
    private final JIPipeGraphCanvasUI canvasUI;
    private final JIPipeDataSlot inputSlot;
    private final JIPipeGraphNode algorithm;
    private final JIPipeGraph graph;
    private final UUID compartment;
    private final JIPipeAlgorithmSourceRanking ranking;
    private final EventBus eventBus = new EventBus();
    private final List<Object> availableContents = new ArrayList<>();
    /**
     * Contains {@link JIPipeNodeInfo} or {@link JIPipeGraphNode} instances
     */
    private final ArrayDeque<Object> infiniteScrollingQueue = new ArrayDeque<>();
    private SearchTextField searchField;
    private FormPanel formPanel;
    private final Timer scrollToBeginTimer = new Timer(200, e -> scrollToBeginning());

    /**
     * Creates a new UI
     *
     * @param canvasUI  the canvas
     * @param inputSlot The target slot
     */
    public JIPipeAlgorithmSourceFinderUI(JIPipeGraphCanvasUI canvasUI, JIPipeDataSlot inputSlot) {
        this.canvasUI = canvasUI;
        this.compartment = canvasUI.getCompartment();
        if (!inputSlot.isInput())
            throw new IllegalArgumentException();
        this.inputSlot = inputSlot;
        this.algorithm = inputSlot.getNode();
        this.graph = canvasUI.getGraph();
        this.ranking = new JIPipeAlgorithmSourceRanking(inputSlot);
        this.scrollToBeginTimer.setRepeats(false);
        initialize();
        initializeAvailableContents();
        reloadAlgorithmList();
    }

    /**
     * Finds all algorithms that fit to the slot according to the information in {@link JIPipeNodeInfo}
     *
     * @param slot The target slot
     * @return Unsorted list of algorithm infos
     */
    public static List<JIPipeNodeInfo> findCompatibleTargetAlgorithms(JIPipeDataSlot slot) {
        Class<? extends JIPipeData> inputSlotDataClass = slot.getAcceptedDataType();
        List<JIPipeNodeInfo> result = new ArrayList<>();
        for (JIPipeNodeInfo info : JIPipe.getNodes().getRegisteredNodeInfos().values()) {
            for (Class<? extends JIPipeData> outputSlotDataClass : info.getOutputSlots().stream().map(JIPipeOutputSlot::value).collect(Collectors.toList())) {
                if (JIPipe.getDataTypes().isConvertible(outputSlotDataClass, inputSlotDataClass)) {
                    result.add(info);
                    break;
                }
            }
        }
        return result;
    }

    private void scrollToBeginning() {
        formPanel.getScrollPane().getVerticalScrollBar().setValue(0);
        formPanel.getScrollPane().getVerticalScrollBar().addAdjustmentListener(e -> {
            updateInfiniteScroll();
        });
    }

    private void initializeAvailableContents() {
        boolean canCreateNewNodes = true;
        if (canvasUI.getWorkbench() instanceof JIPipeProjectWorkbench) {
            canCreateNewNodes = !((JIPipeProjectWorkbench) canvasUI.getWorkbench()).getProject().getMetadata().getPermissions().isPreventAddingDeletingNodes();
        }
        for (JIPipeGraphNode node : canvasUI.getGraph().getGraphNodes()) {
            if (node.isVisibleIn(canvasUI.getCompartment())) {
                availableContents.add(node);
            }
        }
        if (canCreateNewNodes) {
            for (JIPipeNodeInfo info : JIPipe.getNodes().getRegisteredNodeInfos().values()) {
                if (!info.isHidden())
                    availableContents.add(info);
            }
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());
        initializeToolBar();

        formPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);
        add(formPanel, BorderLayout.CENTER);
    }

    private void initializeToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JLabel algorithmNameLabel = new JLabel(algorithm.getName(), new ColorIcon(16, 16, UIUtils.getFillColorFor(algorithm.getInfo())), JLabel.LEFT);
        algorithmNameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(algorithm.getInfo()));
        toolBar.add(algorithmNameLabel);
        toolBar.add(Box.createHorizontalStrut(5));
        JLabel slotNameLabel = new JLabel(inputSlot.getName(), JIPipe.getDataTypes().getIconFor(inputSlot.getAcceptedDataType()), JLabel.LEFT);
        slotNameLabel.setToolTipText(TooltipUtils.getSlotInstanceTooltip(inputSlot));
        toolBar.add(slotNameLabel);

        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(Box.createHorizontalStrut(16));
        searchField = new SearchTextField();
        searchField.addActionListener(e -> reloadAlgorithmList());
        toolBar.add(searchField);

        add(toolBar, BorderLayout.NORTH);
    }

    private void reloadAlgorithmList() {
        infiniteScrollingQueue.clear();
        formPanel.clear();

        // Add possible algorithms
        List<RankedData<Object>> rankedData = new ArrayList<>();
        for (Object content : availableContents) {
            int[] rank = ranking.rank(content, searchField.getSearchStrings());
            if (rank == null)
                continue;
            String asString;
            if (content instanceof JIPipeNodeInfo) {
                asString = ((JIPipeNodeInfo) content).getName();
            } else if (content instanceof JIPipeGraphNode) {
                asString = ((JIPipeGraphNode) content).getName();
            } else {
                asString = "" + content;
            }
            rankedData.add(new RankedData<>(content, asString, rank));
        }
        rankedData.sort(Comparator.naturalOrder());

        // Add open slots
        Set<JIPipeGraphNode> knownTargetAlgorithms = new HashSet<>();

        // Add algorithms that allow adding slots of given type
        for (JIPipeGraphNode algorithm : graph.getGraphNodes()) {
            if (algorithm.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
                JIPipeMutableSlotConfiguration configuration = (JIPipeMutableSlotConfiguration) algorithm.getSlotConfiguration();
                if (configuration.canCreateCompatibleOutputSlot(inputSlot.getAcceptedDataType())) {
                    knownTargetAlgorithms.add(algorithm);
                }
            }
        }

        for (RankedData<Object> data : rankedData) {
            if (data.getData() instanceof JIPipeNodeInfo) {
                JIPipeNodeInfo info = (JIPipeNodeInfo) data.getData();
//                JIPipeAlgorithmSourceFinderAlgorithmUI algorithmUI = new JIPipeAlgorithmSourceFinderAlgorithmUI(canvasUI, inputSlot, info);
//                algorithmUI.getEventBus().register(this);
//                formPanel.addToForm(algorithmUI, null);
                infiniteScrollingQueue.addLast(info);
            } else if (data.getData() instanceof JIPipeGraphNode) {
                JIPipeGraphNode node = (JIPipeGraphNode) data.getData();
                if (knownTargetAlgorithms.contains(node)) {
                    if (node.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
                        if (!((JIPipeMutableSlotConfiguration) node.getSlotConfiguration()).canModifyOutputSlots())
                            continue;
                    } else {
                        continue;
                    }
                }
//                JIPipeAlgorithmSourceFinderAlgorithmUI algorithmUI = new JIPipeAlgorithmSourceFinderAlgorithmUI(canvasUI, inputSlot, node);
//                algorithmUI.getEventBus().register(this);
//                formPanel.addToForm(algorithmUI, null);
                infiniteScrollingQueue.addLast(node);
            }
        }

        formPanel.addVerticalGlue();
        SwingUtilities.invokeLater(this::updateInfiniteScroll);
        scrollToBeginTimer.restart();
    }

    private void updateInfiniteScroll() {
        JScrollBar scrollBar = formPanel.getScrollPane().getVerticalScrollBar();
        if ((!scrollBar.isVisible() || (scrollBar.getValue() + scrollBar.getVisibleAmount()) > (scrollBar.getMaximum() - 32)) && !infiniteScrollingQueue.isEmpty()) {
            formPanel.removeLastRow();
            Object value = infiniteScrollingQueue.removeFirst();
            if (value instanceof JIPipeNodeInfo) {
                JIPipeAlgorithmSourceFinderAlgorithmUI algorithmUI = new JIPipeAlgorithmSourceFinderAlgorithmUI(canvasUI, inputSlot, (JIPipeNodeInfo) value);
                algorithmUI.getEventBus().register(this);
                formPanel.addToForm(algorithmUI, null);
            } else {
                JIPipeAlgorithmSourceFinderAlgorithmUI algorithmUI = new JIPipeAlgorithmSourceFinderAlgorithmUI(canvasUI, inputSlot, (JIPipeGraphNode) value);
                algorithmUI.getEventBus().register(this);
                formPanel.addToForm(algorithmUI, null);
            }
            formPanel.addVerticalGlue();
            formPanel.revalidate();
            formPanel.repaint();
            SwingUtilities.invokeLater(this::updateInfiniteScroll);
        }
    }

    /**
     * Should trigger when a target slot was successfully found.
     *
     * @param event Generated event
     */
    @Subscribe
    public void onAlgorithmFinderSuccess(AlgorithmFinderSuccessEvent event) {
        eventBus.post(event);
    }

    /**
     * Returns the event bus
     *
     * @return Event bus instance
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Returns the graph compartment
     *
     * @return Compartment ID
     */
    public UUID getCompartment() {
        return compartment;
    }

}
