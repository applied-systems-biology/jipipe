package org.hkijena.acaq5.ui.grapheditor.algorithmfinder;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.events.AlgorithmFinderSuccessEvent;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ACAQAlgorithmFinderUI extends JPanel {
    private ACAQDataSlot<?> outputSlot;
    private ACAQAlgorithm algorithm;
    private ACAQAlgorithmGraph graph;
    private JXTextField searchField;
    private FormPanel formPanel;
    private EventBus eventBus = new EventBus();

    public ACAQAlgorithmFinderUI(ACAQDataSlot<?> outputSlot, ACAQAlgorithmGraph graph) {
        if(!outputSlot.isOutput())
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

        JLabel algorithmNameLabel = new JLabel(algorithm.getName(), new ColorIcon(16, 16, algorithm.getCategory().getColor(0.1f, 0.9f)), JLabel.LEFT);
        algorithmNameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(algorithm.getClass()));
        toolBar.add(algorithmNameLabel);
        toolBar.add(Box.createHorizontalStrut(5));
        JLabel slotNameLabel = new JLabel(outputSlot.getName(), ACAQRegistryService.getInstance().getUIDatatypeRegistry().getIconFor(outputSlot.getAcceptedDataType()), JLabel.LEFT);
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

        List<Class<? extends ACAQAlgorithm>> algorithms = getFilteredAndSortedCompatibleTargetAlgorithms();

        if(!algorithms.isEmpty()) {
            Map<Class<? extends ACAQAlgorithm>, Integer> scores = new HashMap<>();
            for (Class<? extends ACAQAlgorithm> targetAlgorithm : algorithms) {
                scores.put(targetAlgorithm, scoreAlgorithmForOutputSlot(targetAlgorithm, outputSlot, graph));
            }
            int maxScore = scores.values().stream().max(Integer::compareTo).orElse(0);

            for (Class<? extends ACAQAlgorithm> targetAlgorithmClass : algorithms) {
                int score = scores.get(targetAlgorithmClass);
                // Add a generic one for creating a new instance
                {
                    ACAQAlgorithmFinderAlgorithmUI algorithmUI = new ACAQAlgorithmFinderAlgorithmUI(outputSlot, graph, targetAlgorithmClass, score, maxScore);
                    algorithmUI.getEventBus().register(this);
                    formPanel.addToForm(algorithmUI, null);
                }

                // Add existing instances
                for(ACAQAlgorithm existing : graph.getAlgorithmNodes().values().stream().filter(a -> a.getClass().equals(targetAlgorithmClass)).collect(Collectors.toList())) {
                    if(existing == outputSlot.getAlgorithm())
                        continue;
                    ACAQAlgorithmFinderAlgorithmUI algorithmUI = new ACAQAlgorithmFinderAlgorithmUI(outputSlot, graph, existing, score, maxScore);
                    algorithmUI.getEventBus().register(this);
                    formPanel.addToForm(algorithmUI, null);
                }

            }
        }

        formPanel.addVerticalGlue();
    }

    private List<Class<? extends ACAQAlgorithm>> getFilteredAndSortedCompatibleTargetAlgorithms() {
        String[] searchStrings = getSearchStrings();
        Predicate<Class<? extends ACAQAlgorithm>> filterFunction = aClass -> {
            if(searchStrings != null && searchStrings.length > 0) {
                String name = ACAQAlgorithm.getNameOf(aClass);
                for (String searchString : searchStrings) {
                    if(name.toLowerCase().contains(searchString.toLowerCase()))
                        return true;
                }
                return false;
            }
            else {
                return true;
            }
        };
        return findCompatibleTargetAlgorithms(outputSlot).stream().filter(filterFunction).sorted(this::compareAlgorithmScore).collect(Collectors.toList());
    }

    private int compareAlgorithmScore(Class<? extends ACAQAlgorithm> algorithmClass, Class<? extends ACAQAlgorithm> algorithmClass2) {
        return -Integer.compare(scoreAlgorithmForOutputSlot(algorithmClass, outputSlot, graph),
                scoreAlgorithmForOutputSlot(algorithmClass2, outputSlot, graph));
    }

    private String[] getSearchStrings() {
        String[] searchStrings = null;
        if(searchField.getText() != null ) {
            String str = searchField.getText().trim();
            if(!str.isEmpty()) {
                searchStrings = str.split(" ");
            }
        }
        return searchStrings;
    }

    @Subscribe
    public void onAlgorithmFinderSuccess(AlgorithmFinderSuccessEvent event) {
        eventBus.post(event);
    }

    public static int scoreAlgorithmForOutputSlot(Class<? extends ACAQAlgorithm> algorithmClass, ACAQDataSlot<?> slot, ACAQAlgorithmGraph graph) {
        Set<Class<? extends ACAQTrait>> preferredTraits = ACAQRegistryService.getInstance().getAlgorithmRegistry().getPreferredTraitsOf(algorithmClass);
        Set<Class<? extends ACAQTrait>> unwantedTraits = ACAQRegistryService.getInstance().getAlgorithmRegistry().getUnwantedTraitsOf(algorithmClass);
        int score = 0;
        for (Class<? extends ACAQTrait> trait : graph.getAlgorithmTraits().getOrDefault(slot, Collections.emptySet())) {
            if(preferredTraits.contains(trait)) {
                score += 10;
            }
            else if(unwantedTraits.contains(trait)) {
                score -= 20;
            }
            else {
                score += 5;
            }
        }
        return score;
    }

    public static List<Class<? extends ACAQAlgorithm>> findCompatibleTargetAlgorithms(ACAQDataSlot<?> slot) {
        List<Class<? extends ACAQAlgorithm>> result = new ArrayList<>();
        for (Class<? extends ACAQAlgorithm> algorithmClass : ACAQRegistryService.getInstance().getAlgorithmRegistry().getRegisteredAlgorithms()) {
            for (Class<? extends ACAQDataSlot<?>> inputSlotClass : ACAQRegistryService.getInstance().getAlgorithmRegistry().getInputTypesOf(algorithmClass)) {
                if(inputSlotClass.isAssignableFrom(slot.getClass())) {
                    result.add(algorithmClass);
                    break;
                }
            }
        }
        return result;
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}
