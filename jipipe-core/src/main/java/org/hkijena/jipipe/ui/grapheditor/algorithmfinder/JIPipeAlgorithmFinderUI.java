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
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.registries.JIPipeAlgorithmRegistry;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.ui.components.ColorIcon;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.SearchTextField;
import org.hkijena.jipipe.ui.events.AlgorithmFinderSuccessEvent;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.registries.JIPipeUIDatatypeRegistry;
import org.hkijena.jipipe.utils.RankingFunction;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
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
    private final EventBus eventBus = new EventBus();
    private SearchTextField searchField;
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
        initialize();
        reloadAlgorithmList();
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
        List<JIPipeNodeInfo> algorithms = getFilteredAndSortedCompatibleTargetAlgorithms();

        // Add open slots
        Set<JIPipeGraphNode> knownTargetAlgorithms = graph.getTargetSlots(outputSlot).stream().map(JIPipeDataSlot::getNode).collect(Collectors.toSet());

        // Add algorithms that allow adding slots of given type
        for (JIPipeGraphNode algorithm : graph.getAlgorithmNodes().values()) {
            if (algorithm.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
                JIPipeMutableSlotConfiguration configuration = (JIPipeMutableSlotConfiguration) algorithm.getSlotConfiguration();
                if (configuration.canCreateCompatibleInputSlot(outputSlot.getAcceptedDataType())) {
                    knownTargetAlgorithms.add(algorithm);
                }
            }
        }

        if (!algorithms.isEmpty()) {
            Map<JIPipeNodeInfo, Integer> scores = new HashMap<>();
            for (JIPipeNodeInfo targetAlgorithm : algorithms) {
                scores.put(targetAlgorithm, scoreAlgorithmForOutputSlot(targetAlgorithm, outputSlot, graph));
            }
            int maxScore = scores.values().stream().max(Integer::compareTo).orElse(0);

            for (JIPipeNodeInfo targetAlgorithm : algorithms) {
                int score = scores.get(targetAlgorithm);
                // Add a generic one for creating a new instance
                if (targetAlgorithm.getCategory() != JIPipeAlgorithmCategory.Internal && !targetAlgorithm.isHidden()) {
                    JIPipeAlgorithmFinderAlgorithmUI algorithmUI = new JIPipeAlgorithmFinderAlgorithmUI(canvasUI, outputSlot, targetAlgorithm, score, maxScore);
                    algorithmUI.getEventBus().register(this);
                    formPanel.addToForm(algorithmUI, null);
                }

                // Add existing instances
                for (JIPipeGraphNode existing : graph.getAlgorithmNodes().values().stream().filter(a -> a.getInfo() == targetAlgorithm).collect(Collectors.toList())) {
                    if (existing == outputSlot.getNode())
                        continue;
                    if (!algorithm.isVisibleIn(compartment))
                        continue;
                    if (knownTargetAlgorithms.contains(existing)) {
                        if (existing.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
                            if (!((JIPipeMutableSlotConfiguration) existing.getSlotConfiguration()).canModifyInputSlots())
                                continue;
                        } else {
                            continue;
                        }
                    }
                    JIPipeAlgorithmFinderAlgorithmUI algorithmUI = new JIPipeAlgorithmFinderAlgorithmUI(canvasUI, outputSlot, existing, score, maxScore);
                    algorithmUI.getEventBus().register(this);
                    formPanel.addToForm(algorithmUI, null);
                }

            }
        }

        formPanel.addVerticalGlue();
        SwingUtilities.invokeLater(() -> formPanel.getScrollPane().getVerticalScrollBar().setValue(0));
    }

    private List<JIPipeNodeInfo> getFilteredAndSortedCompatibleTargetAlgorithms() {
        Predicate<JIPipeNodeInfo> filterFunction = info -> searchField.test(info.getName());
        return findCompatibleTargetAlgorithms(outputSlot).stream().filter(filterFunction).sorted(this::compareAlgorithmScore).collect(Collectors.toList());
    }

    private int compareAlgorithmScore(JIPipeNodeInfo algorithmClass, JIPipeNodeInfo algorithmClass2) {
        return -Integer.compare(scoreAlgorithmForOutputSlot(algorithmClass, outputSlot, graph),
                scoreAlgorithmForOutputSlot(algorithmClass2, outputSlot, graph));
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
     * Scores how well an algorithm fits to the specified target slot
     *
     * @param info The algorithm
     * @param slot        The target slot
     * @param graph       The algorithm graph
     * @return Non-normalized score
     */
    public static int scoreAlgorithmForOutputSlot(JIPipeNodeInfo info, JIPipeDataSlot slot, JIPipeGraph graph) {
        int score = 0;
        return score;
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
        for (JIPipeNodeInfo info : JIPipeAlgorithmRegistry.getInstance().getRegisteredAlgorithms().values()) {
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
     * - data type compatibility (0=nontrivial, -1=trivial, -2=exact match)
     * - already occupied or not
     */
    public static class Ranking implements RankingFunction<Object> {
        private final JIPipeDataSlot sourceSlot;

        public Ranking(JIPipeDataSlot sourceSlot) {
            this.sourceSlot = sourceSlot;
        }

        @Override
        public int[] rank(Object value, String[] filterStrings) {
            int[] rank = new int[4];
            if(value instanceof JIPipeGraphNode) {

            }
            else if(value instanceof JIPipeNodeInfo) {

            }
            return rank;
        }
    }
}
