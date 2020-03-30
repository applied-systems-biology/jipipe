package org.hkijena.acaq5.ui.grapheditor.algorithmfinder;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.events.AlgorithmFinderSuccessEvent;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ACAQAlgorithmFinderUI extends JPanel {
    private ACAQDataSlot outputSlot;
    private ACAQAlgorithm algorithm;
    private ACAQAlgorithmGraph graph;
    private String compartment;
    private JXTextField searchField;
    private FormPanel formPanel;
    private EventBus eventBus = new EventBus();

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

        formPanel = new FormPanel(null, false, false);
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
        slotNameLabel.setToolTipText(TooltipUtils.getSlotInstanceTooltip(outputSlot, graph, true));
        toolBar.add(slotNameLabel);

        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(Box.createHorizontalStrut(16));
        searchField = new JXTextField("Search ...");
        searchField.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                reloadAlgorithmList();
            }
        });
        toolBar.add(searchField);

        JButton clearSearchButton = new JButton(UIUtils.getIconFromResources("clear.png"));
        clearSearchButton.addActionListener(e -> searchField.setText(null));
        toolBar.add(clearSearchButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void reloadAlgorithmList() {
        formPanel.clear();

        // Add possible algorithms
        List<ACAQAlgorithmDeclaration> algorithms = getFilteredAndSortedCompatibleTargetAlgorithms();

        // Add open slots
        Set<ACAQAlgorithm> knownTargetAlgorithms = graph.getTargetSlots(outputSlot).stream().map(ACAQDataSlot::getAlgorithm).collect(Collectors.toSet());

        // Add algorithms that allow adding slots of given type
        for (ACAQAlgorithm algorithm : graph.getAlgorithmNodes().values()) {
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
                if (targetAlgorithm.getCategory() != ACAQAlgorithmCategory.Internal) {
                    ACAQAlgorithmFinderAlgorithmUI algorithmUI = new ACAQAlgorithmFinderAlgorithmUI(outputSlot, graph, compartment, targetAlgorithm, score, maxScore);
                    algorithmUI.getEventBus().register(this);
                    formPanel.addToForm(algorithmUI, null);
                }

                // Add existing instances
                for (ACAQAlgorithm existing : graph.getAlgorithmNodes().values().stream().filter(a -> a.getDeclaration() == targetAlgorithm).collect(Collectors.toList())) {
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
        String[] searchStrings = getSearchStrings();
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

    private String[] getSearchStrings() {
        String[] searchStrings = null;
        if (searchField.getText() != null) {
            String str = searchField.getText().trim();
            if (!str.isEmpty()) {
                searchStrings = str.split(" ");
            }
        }
        return searchStrings;
    }

    @Subscribe
    public void onAlgorithmFinderSuccess(AlgorithmFinderSuccessEvent event) {
        eventBus.post(event);
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public String getCompartment() {
        return compartment;
    }

    public static int scoreAlgorithmForOutputSlot(ACAQAlgorithmDeclaration declaration, ACAQDataSlot slot, ACAQAlgorithmGraph graph) {
        Set<ACAQTraitDeclaration> preferredTraits = declaration.getPreferredTraits();
        Set<ACAQTraitDeclaration> unwantedTraits = declaration.getUnwantedTraits();
        int score = 0;
        for (ACAQTraitDeclaration trait : slot.getSlotAnnotations()) {
            if (preferredTraits.contains(trait)) {
                score += 10;
            } else if (unwantedTraits.contains(trait)) {
                score -= 20;
            } else {
                score += 5;
            }
        }
        return score;
    }

    public static List<ACAQAlgorithmDeclaration> findCompatibleTargetAlgorithms(ACAQDataSlot slot) {
        Class<? extends ACAQData> outputSlotDataClass = slot.getAcceptedDataType();
        List<ACAQAlgorithmDeclaration> result = new ArrayList<>();
        for (ACAQAlgorithmDeclaration declaration : ACAQAlgorithmRegistry.getInstance().getRegisteredAlgorithms().values()) {
            for (Class<? extends ACAQData> inputSlotDataClass : declaration.getInputSlots().stream().map(AlgorithmInputSlot::value).collect(Collectors.toList())) {
                if (inputSlotDataClass.isAssignableFrom(outputSlotDataClass)) {
                    result.add(declaration);
                    break;
                }
            }
        }
        return result;
    }
}
