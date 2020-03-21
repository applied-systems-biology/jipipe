package org.hkijena.acaq5.ui.grapheditor.algorithmfinder;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.ui.components.AddAlgorithmSlotPanel;
import org.hkijena.acaq5.ui.events.AlgorithmFinderSuccessEvent;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

import static org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI.SLOT_UI_HEIGHT;

public class ACAQAlgorithmFinderAlgorithmUI extends JPanel {
    private ACAQDataSlot outputSlot;
    private ACAQAlgorithmGraph graph;
    private ACAQAlgorithm algorithm;
    private int score;
    private int maxScore;
    private boolean isExistingInstance;
    private JPanel slotPanel;
    private EventBus eventBus = new EventBus();
    private String compartment;

    public ACAQAlgorithmFinderAlgorithmUI(ACAQDataSlot outputSlot, ACAQAlgorithmGraph graph, String compartment, ACAQAlgorithmDeclaration declaration, int score, int maxScore) {
        this.outputSlot = outputSlot;
        this.graph = graph;
        this.compartment = compartment;
        this.score = score;
        this.maxScore = maxScore;
        this.algorithm = declaration.newInstance();
        this.isExistingInstance = false;

        initialize();
    }

    public ACAQAlgorithmFinderAlgorithmUI(ACAQDataSlot outputSlot, ACAQAlgorithmGraph graph, String compartment, ACAQAlgorithm algorithm, int score, int maxScore) {
        this.outputSlot = outputSlot;
        this.graph = graph;
        this.compartment = compartment;
        this.score = score;
        this.maxScore = maxScore;
        this.algorithm = algorithm;
        this.isExistingInstance = true;

        initialize();
    }

    private void initialize() {
        initializeUI();
        reloadSlotUI();
        this.algorithm.getEventBus().register(this);
    }

    private void initializeUI() {
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4),
                BorderFactory.createLineBorder(Color.DARK_GRAY, 1, true)));
        setLayout(new BorderLayout());

        JPanel centerPanel = new JPanel(new BorderLayout());

        StringBuilder title = new StringBuilder();
        if (isExistingInstance)
            title.append("<span style=\"color: red;\">Existing </span>");
        else
            title.append("<span style=\"color: grey;\">Create </span>");
        title.append("<span style=\"font-size: 16pt;\">").append(algorithm.getName()).append("</font>");

        double stars = (maxScore > 0 ? (Math.max(0, score) * 1.0 / maxScore) : 1.0) * 5.0;
        JLabel starsLabel = UIUtils.createStarRatingLabel(title.toString(), stars, 5);
        centerPanel.add(starsLabel, BorderLayout.NORTH);

        JLabel label = new JLabel();
        label.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        label.setText(TooltipUtils.getAlgorithmTooltip(algorithm.getDeclaration(), false));
        centerPanel.add(label, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        JPanel colorPanel = new JPanel();
        colorPanel.setBackground(UIUtils.getFillColorFor(algorithm.getDeclaration()));
        colorPanel.setPreferredSize(new Dimension(16, 1));
        add(colorPanel, BorderLayout.WEST);

        slotPanel = new JPanel();
        slotPanel.setLayout(new BoxLayout(slotPanel, BoxLayout.Y_AXIS));
        add(slotPanel, BorderLayout.EAST);
    }

    public void reloadSlotUI() {
        slotPanel.removeAll();

        boolean createAddInputSlotButton = false;

        if (algorithm.getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration();
            createAddInputSlotButton = slotConfiguration.canAddInputSlot();
        }

        for (ACAQDataSlot slot : algorithm.getInputSlots()) {
            ACAQAlgorithmFinderSlotUI ui = new ACAQAlgorithmFinderSlotUI(outputSlot, graph, compartment, slot, isExistingInstance);
            ui.getEventBus().register(this);
            slotPanel.add(ui);
        }

        if (createAddInputSlotButton) {
            JButton addInputSlotButton = createAddSlotButton(ACAQDataSlot.SlotType.Input);
            addInputSlotButton.setPreferredSize(new Dimension(25, SLOT_UI_HEIGHT));
            JPanel panel = new JPanel(new BorderLayout());
//            panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,0,1, getAlgorithmBorderColor()),
//                    BorderFactory.createEmptyBorder(0,0,0,4)));
            panel.add(addInputSlotButton, BorderLayout.WEST);
            slotPanel.add(panel);
        }

        slotPanel.revalidate();
        slotPanel.repaint();
    }

    private JButton createAddSlotButton(ACAQDataSlot.SlotType slotType) {
        JButton button = new JButton(UIUtils.getIconFromResources("add.png"));
        UIUtils.makeFlat(button);
        button.addActionListener(e -> AddAlgorithmSlotPanel.showDialog(this, algorithm, slotType));
        return button;
    }

    @Subscribe
    public void onAlgorithmSlotsChanged(AlgorithmSlotsChangedEvent event) {
        reloadSlotUI();
    }

    @Subscribe
    public void onAlgorithmFinderSuccess(AlgorithmFinderSuccessEvent event) {
        eventBus.post(event);
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}
