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
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.search.RankedData;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A user interface to find a matching algorithm for the specified target slot
 */
public class JIPipeAlgorithmTargetFinderUI extends JPanel {
    private final JIPipeGraphCanvasUI canvasUI;
    private final JIPipeDataSlot outputSlot;
    private final JIPipeGraphNode algorithm;
    private final JIPipeGraph graph;
    private final UUID compartment;
    private final JIPipeAlgorithmTargetRanking ranking;
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
     * @param canvasUI   the canvas
     * @param outputSlot The target slot
     */
    public JIPipeAlgorithmTargetFinderUI(JIPipeGraphCanvasUI canvasUI, JIPipeDataSlot outputSlot) {
        this.canvasUI = canvasUI;
        this.compartment = canvasUI.getCompartment();
        if (!outputSlot.isOutput())
            throw new IllegalArgumentException();
        this.outputSlot = outputSlot;
        this.algorithm = outputSlot.getNode();
        this.graph = canvasUI.getGraph();
        this.ranking = new JIPipeAlgorithmTargetRanking(outputSlot);
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
        Class<? extends JIPipeData> outputSlotDataClass = slot.getAcceptedDataType();
        List<JIPipeNodeInfo> result = new ArrayList<>();
        for (JIPipeNodeInfo info : JIPipe.getNodes().getRegisteredNodeInfos().values()) {
            for (Class<? extends JIPipeData> inputSlotDataClass : info.getInputSlots().stream().map(JIPipeInputSlot::value).collect(Collectors.toList())) {
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
        formPanel.getScrollPane().getVerticalScrollBar().addAdjustmentListener(e -> {
            updateInfiniteScroll();
        });
        add(formPanel, BorderLayout.CENTER);
    }

    private void initializeToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JLabel algorithmNameLabel = new JLabel(algorithm.getName(), new SolidColorIcon(16, 16, UIUtils.getFillColorFor(algorithm.getInfo())), JLabel.LEFT);
        algorithmNameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(algorithm.getInfo()));
        toolBar.add(algorithmNameLabel);
        toolBar.add(Box.createHorizontalStrut(5));
        JLabel slotNameLabel = new JLabel(outputSlot.getName(), JIPipe.getDataTypes().getIconFor(outputSlot.getAcceptedDataType()), JLabel.LEFT);
        slotNameLabel.setToolTipText(TooltipUtils.getSlotInstanceTooltip(outputSlot));
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
        Set<JIPipeGraphNode> knownTargetAlgorithms = graph.getTargetSlots(outputSlot).stream().map(JIPipeDataSlot::getNode).collect(Collectors.toSet());

        // Add algorithms that allow adding slots of given type
        for (JIPipeGraphNode algorithm : graph.getGraphNodes()) {
            if (algorithm.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
                JIPipeMutableSlotConfiguration configuration = (JIPipeMutableSlotConfiguration) algorithm.getSlotConfiguration();
                if (configuration.canCreateCompatibleInputSlot(outputSlot.getAcceptedDataType())) {
                    knownTargetAlgorithms.add(algorithm);
                }
            }
        }

        for (RankedData<Object> data : rankedData) {
            if (data.getData() instanceof JIPipeNodeInfo) {
                JIPipeNodeInfo info = (JIPipeNodeInfo) data.getData();
//                JIPipeAlgorithmTargetFinderAlgorithmUI algorithmUI = new JIPipeAlgorithmTargetFinderAlgorithmUI(canvasUI, outputSlot, info);
//                algorithmUI.getEventBus().register(this);
//                formPanel.addToForm(algorithmUI, null);
                infiniteScrollingQueue.addLast(info);
            } else if (data.getData() instanceof JIPipeGraphNode) {
                JIPipeGraphNode node = (JIPipeGraphNode) data.getData();
                if (knownTargetAlgorithms.contains(node)) {
                    if (node.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
                        if (!((JIPipeMutableSlotConfiguration) node.getSlotConfiguration()).canModifyInputSlots())
                            continue;
                    } else {
                        continue;
                    }
                }
//                JIPipeAlgorithmTargetFinderAlgorithmUI algorithmUI = new JIPipeAlgorithmTargetFinderAlgorithmUI(canvasUI, outputSlot, node);
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
                JIPipeAlgorithmTargetFinderAlgorithmUI algorithmUI = new JIPipeAlgorithmTargetFinderAlgorithmUI(canvasUI, outputSlot, (JIPipeNodeInfo) value);
                algorithmUI.getEventBus().register(this);
                formPanel.addToForm(algorithmUI, null);
            } else {
                JIPipeAlgorithmTargetFinderAlgorithmUI algorithmUI = new JIPipeAlgorithmTargetFinderAlgorithmUI(canvasUI, outputSlot, (JIPipeGraphNode) value);
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
