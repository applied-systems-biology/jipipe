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
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.api.registries.JIPipeNodeRegistry;
import org.hkijena.jipipe.ui.components.ColorIcon;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.SearchTextField;
import org.hkijena.jipipe.ui.events.AlgorithmFinderSuccessEvent;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.registries.JIPipeUIDatatypeRegistry;
import org.hkijena.jipipe.utils.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A user interface to find a matching algorithm for the specified target slot
 */
public class JIPipeAlgorithmFinderUI extends JPanel {
    private final JIPipeGraphCanvasUI canvasUI;
    private final JIPipeDataSlot outputSlot;
    private final JIPipeGraphNode algorithm;
    private final JIPipeGraph graph;
    private final String compartment;
    private final Ranking ranking;
    private final EventBus eventBus = new EventBus();
    private SearchTextField searchField;
    private List<Object> availableContents = new ArrayList<>();
    private FormPanel formPanel;

    /**
     * Creates a new UI
     *
     * @param canvasUI   the canvas
     * @param outputSlot The target slot
     */
    public JIPipeAlgorithmFinderUI(JIPipeGraphCanvasUI canvasUI, JIPipeDataSlot outputSlot) {
        this.canvasUI = canvasUI;
        this.compartment = canvasUI.getCompartment();
        if (!outputSlot.isOutput())
            throw new IllegalArgumentException();
        this.outputSlot = outputSlot;
        this.algorithm = outputSlot.getNode();
        this.graph = canvasUI.getGraph();
        this.ranking = new Ranking(outputSlot);
        initialize();
        initializeAvailableContents();
        reloadAlgorithmList();
    }

    private void initializeAvailableContents() {
        for (JIPipeGraphNode node : canvasUI.getGraph().getNodes().values()) {
            if (node.isVisibleIn(canvasUI.getCompartment())) {
                availableContents.add(node);
            }
        }
        for (JIPipeNodeInfo info : JIPipeNodeRegistry.getInstance().getRegisteredNodeInfos().values()) {
            if (!info.isHidden())
                availableContents.add(info);
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
        JLabel slotNameLabel = new JLabel(outputSlot.getName(), JIPipeUIDatatypeRegistry.getInstance().getIconFor(outputSlot.getAcceptedDataType()), JLabel.LEFT);
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
        formPanel.clear();

        // Add possible algorithms
        List<RankedData<Object>> rankedData = new ArrayList<>();
        for (Object content : availableContents) {
            int[] rank = ranking.rank(content, searchField.getSearchStrings());
            if (rank == null)
                continue;
            rankedData.add(new RankedData<>(content, rank));
        }
        rankedData.sort(Comparator.naturalOrder());

        // Add open slots
        Set<JIPipeGraphNode> knownTargetAlgorithms = graph.getTargetSlots(outputSlot).stream().map(JIPipeDataSlot::getNode).collect(Collectors.toSet());

        // Add algorithms that allow adding slots of given type
        for (JIPipeGraphNode algorithm : graph.getNodes().values()) {
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
                JIPipeAlgorithmFinderAlgorithmUI algorithmUI = new JIPipeAlgorithmFinderAlgorithmUI(canvasUI, outputSlot, info);
                algorithmUI.getEventBus().register(this);
                formPanel.addToForm(algorithmUI, null);
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
                JIPipeAlgorithmFinderAlgorithmUI algorithmUI = new JIPipeAlgorithmFinderAlgorithmUI(canvasUI, outputSlot, node);
                algorithmUI.getEventBus().register(this);
                formPanel.addToForm(algorithmUI, null);
            }
        }

        formPanel.addVerticalGlue();
        SwingUtilities.invokeLater(() -> formPanel.getScrollPane().getVerticalScrollBar().setValue(0));
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
    public String getCompartment() {
        return compartment;
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
        for (JIPipeNodeInfo info : JIPipeNodeRegistry.getInstance().getRegisteredNodeInfos().values()) {
            for (Class<? extends JIPipeData> inputSlotDataClass : info.getInputSlots().stream().map(JIPipeInputSlot::value).collect(Collectors.toList())) {
                if (JIPipeDatatypeRegistry.getInstance().isConvertible(outputSlotDataClass, inputSlotDataClass)) {
                    result.add(info);
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Ranks {@link JIPipeNodeInfo} or {@link JIPipeGraphNode} instances
     * It ranks by following values:
     * - search string (node name)
     * - search string (node description)
     * - data type compatibility (0 = not, -1=nontrivial, -2=trivial, -3=exact match)
     * - already occupied or not (no = 0, yes = 1)
     */
    public static class Ranking implements RankingFunction<Object> {
        private final JIPipeDataSlot sourceSlot;

        public Ranking(JIPipeDataSlot sourceSlot) {
            this.sourceSlot = sourceSlot;
        }

        @Override
        public int[] rank(Object value, String[] filterStrings) {
            if (value == null)
                return null;
            int[] ranks = new int[4];
            String nameHayStack;
            String descriptionHayStack;
            if (value instanceof JIPipeGraphNode) {
                JIPipeGraphNode node = ((JIPipeGraphNode) value);
                nameHayStack = node.getName();
                descriptionHayStack = StringUtils.orElse(node.getCustomDescription(), node.getInfo().getDescription());
            } else if (value instanceof JIPipeNodeInfo) {
                JIPipeNodeInfo info = (JIPipeNodeInfo) value;
                if (info.isHidden())
                    return null;
                nameHayStack = StringUtils.orElse(info.getName(), "").toLowerCase();
                descriptionHayStack = StringUtils.orElse(info.getDescription(), "").toLowerCase();
            } else {
                return null;
            }

            if(nameHayStack == null)
                nameHayStack = "";
            if(descriptionHayStack == null)
                descriptionHayStack = "";

            if (filterStrings != null && filterStrings.length > 0) {
                nameHayStack = nameHayStack.toLowerCase();
                descriptionHayStack = descriptionHayStack.toLowerCase();

                for (String string : filterStrings) {
                    if (nameHayStack.contains(string.toLowerCase()))
                        --ranks[0];
                    if (descriptionHayStack.contains(string.toLowerCase()))
                        --ranks[1];
                }

                // Name/description does not match -> ignore
                if (ranks[0] == 0 && ranks[1] == 0)
                    return null;
            }

            // Rank by data type compatibility
            if (value instanceof JIPipeGraphNode) {
                JIPipeGraphNode node = ((JIPipeGraphNode) value);
                for (JIPipeDataSlot targetSlot : node.getInputSlots()) {
                    int compatibilityRanking = 0;
                    if (targetSlot.getAcceptedDataType() == sourceSlot.getAcceptedDataType()) {
                        compatibilityRanking = -3;
                    } else if (JIPipeDatatypeRegistry.isTriviallyConvertible(sourceSlot.getAcceptedDataType(), targetSlot.getAcceptedDataType())) {
                        compatibilityRanking = -2;
                    } else if (JIPipeDatatypeRegistry.getInstance().isConvertible(sourceSlot.getAcceptedDataType(), targetSlot.getAcceptedDataType())) {
                        compatibilityRanking = -1;
                    }
                    ranks[2] = Math.min(compatibilityRanking, ranks[2]);
                    if (sourceSlot.getNode().getGraph().getSourceSlot(targetSlot) != null) {
                        ranks[3] = 1;
                    }
                }
            } else {
                JIPipeNodeInfo info = (JIPipeNodeInfo) value;
                if (info.isHidden() || !info.getCategory().userCanCreate())
                    return null;
                for (JIPipeInputSlot inputSlot : info.getInputSlots()) {
                    int compatibilityRanking = 0;
                    if (inputSlot.value() == sourceSlot.getAcceptedDataType()) {
                        compatibilityRanking = -3;
                    } else if (JIPipeDatatypeRegistry.isTriviallyConvertible(sourceSlot.getAcceptedDataType(), inputSlot.value())) {
                        compatibilityRanking = -2;
                    } else if (JIPipeDatatypeRegistry.getInstance().isConvertible(sourceSlot.getAcceptedDataType(), inputSlot.value())) {
                        compatibilityRanking = -1;
                    }
                    ranks[2] = Math.min(compatibilityRanking, ranks[2]);
                }
            }

            // Not compatible to slot
            if (ranks[2] == 0)
                return null;

            return ranks;
        }
    }
}
