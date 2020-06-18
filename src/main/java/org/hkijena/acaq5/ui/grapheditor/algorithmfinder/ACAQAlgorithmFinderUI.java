package org.hkijena.acaq5.ui.grapheditor.algorithmfinder;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.components.SearchTextField;
import org.hkijena.acaq5.ui.events.AlgorithmFinderSuccessEvent;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A user interface to find a matching algorithm for the specified target slot
 */
public class ACAQAlgorithmFinderUI extends JPanel {
    private ACAQDataSlot outputSlot;
    private ACAQGraphNode algorithm;
    private ACAQAlgorithmGraph graph;
    private String compartment;
    private SearchTextField searchField;
    private FormPanel formPanel;
    private EventBus eventBus = new EventBus();

    /**
     * Creates a new UI
     *
     * @param outputSlot  The target slot
     * @param graph       The graph
     * @param compartment The graph compartment. Algorithms outside of the compartment are not detected.
     */
    public ACAQAlgorithmFinderUI(ACAQDataSlot outputSlot, ACAQAlgorithmGraph graph, String compartment) {
        this.compartment = compartment;
        if (!outputSlot.isOutput())
            throw new IllegalArgumentException();
        this.outputSlot = outputSlot;
        this.algorithm = outputSlot.getAlgorithm();
        this.graph = graph;
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

        JLabel algorithmNameLabel = new JLabel(algorithm.getName(), new ColorIcon(16, 16, UIUtils.getFillColorFor(algorithm.getDeclaration())), JLabel.LEFT);
        algorithmNameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(algorithm.getDeclaration()));
        toolBar.add(algorithmNameLabel);
        toolBar.add(Box.createHorizontalStrut(5));
        JLabel slotNameLabel = new JLabel(outputSlot.getName(), ACAQUIDatatypeRegistry.getInstance().getIconFor(outputSlot.getAcceptedDataType()), JLabel.LEFT);
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
        List<ACAQAlgorithmDeclaration> algorithms = getFilteredAndSortedCompatibleTargetAlgorithms();

        // Add open slots
        Set<ACAQGraphNode> knownTargetAlgorithms = graph.getTargetSlots(outputSlot).stream().map(ACAQDataSlot::getAlgorithm).collect(Collectors.toSet());

        // Add algorithms that allow adding slots of given type
        for (ACAQGraphNode algorithm : graph.getAlgorithmNodes().values()) {
            if (algorithm.getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
                ACAQMutableSlotConfiguration configuration = (ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration();
                if (configuration.canCreateCompatibleInputSlot(outputSlot.getAcceptedDataType())) {
                    knownTargetAlgorithms.add(algorithm);
                }
            }
        }

        if (!algorithms.isEmpty()) {
            Map<ACAQAlgorithmDeclaration, Integer> scores = new HashMap<>();
            for (ACAQAlgorithmDeclaration targetAlgorithm : algorithms) {
                scores.put(targetAlgorithm, scoreAlgorithmForOutputSlot(targetAlgorithm, outputSlot, graph));
            }
            int maxScore = scores.values().stream().max(Integer::compareTo).orElse(0);

            for (ACAQAlgorithmDeclaration targetAlgorithm : algorithms) {
                int score = scores.get(targetAlgorithm);
                // Add a generic one for creating a new instance
                if (targetAlgorithm.getCategory() != ACAQAlgorithmCategory.Internal && !targetAlgorithm.isHidden()) {
                    ACAQAlgorithmFinderAlgorithmUI algorithmUI = new ACAQAlgorithmFinderAlgorithmUI(outputSlot, graph, compartment, targetAlgorithm, score, maxScore);
                    algorithmUI.getEventBus().register(this);
                    formPanel.addToForm(algorithmUI, null);
                }

                // Add existing instances
                for (ACAQGraphNode existing : graph.getAlgorithmNodes().values().stream().filter(a -> a.getDeclaration() == targetAlgorithm).collect(Collectors.toList())) {
                    if (existing == outputSlot.getAlgorithm())
                        continue;
                    if (!algorithm.isVisibleIn(compartment))
                        continue;
                    if (knownTargetAlgorithms.contains(existing)) {
                        if (existing.getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
                            if (!((ACAQMutableSlotConfiguration) existing.getSlotConfiguration()).canModifyInputSlots())
                                continue;
                        } else {
                            continue;
                        }
                    }
                    ACAQAlgorithmFinderAlgorithmUI algorithmUI = new ACAQAlgorithmFinderAlgorithmUI(outputSlot, graph, compartment, existing, score, maxScore);
                    algorithmUI.getEventBus().register(this);
                    formPanel.addToForm(algorithmUI, null);
                }

            }
        }

        formPanel.addVerticalGlue();
    }

    private List<ACAQAlgorithmDeclaration> getFilteredAndSortedCompatibleTargetAlgorithms() {
        String[] searchStrings = searchField.getSearchStrings();
        Predicate<ACAQAlgorithmDeclaration> filterFunction = declaration -> {
            if (searchStrings != null && searchStrings.length > 0) {
                boolean matches = true;
                String name = declaration.getName();
                for (String searchString : searchStrings) {
                    if (!name.toLowerCase().contains(searchString.toLowerCase())) {
                        matches = false;
                        break;
                    }
                }
                return matches;
            } else {
                return true;
            }
        };
        return findCompatibleTargetAlgorithms(outputSlot).stream().filter(filterFunction).sorted(this::compareAlgorithmScore).collect(Collectors.toList());
    }

    private int compareAlgorithmScore(ACAQAlgorithmDeclaration algorithmClass, ACAQAlgorithmDeclaration algorithmClass2) {
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
     * @param declaration The algorithm
     * @param slot        The target slot
     * @param graph       The algorithm graph
     * @return Non-normalized score
     */
    public static int scoreAlgorithmForOutputSlot(ACAQAlgorithmDeclaration declaration, ACAQDataSlot slot, ACAQAlgorithmGraph graph) {
        int score = 0;
        return score;
    }

    /**
     * Finds all algorithms that fit to the slot according to the information in {@link ACAQAlgorithmDeclaration}
     *
     * @param slot The target slot
     * @return Unsorted list of algorithm declarations
     */
    public static List<ACAQAlgorithmDeclaration> findCompatibleTargetAlgorithms(ACAQDataSlot slot) {
        Class<? extends ACAQData> outputSlotDataClass = slot.getAcceptedDataType();
        List<ACAQAlgorithmDeclaration> result = new ArrayList<>();
        for (ACAQAlgorithmDeclaration declaration : ACAQAlgorithmRegistry.getInstance().getRegisteredAlgorithms().values()) {
            for (Class<? extends ACAQData> inputSlotDataClass : declaration.getInputSlots().stream().map(AlgorithmInputSlot::value).collect(Collectors.toList())) {
                if (ACAQDatatypeRegistry.getInstance().isConvertible(outputSlotDataClass, inputSlotDataClass)) {
                    result.add(declaration);
                    break;
                }
            }
        }
        return result;
    }
}
