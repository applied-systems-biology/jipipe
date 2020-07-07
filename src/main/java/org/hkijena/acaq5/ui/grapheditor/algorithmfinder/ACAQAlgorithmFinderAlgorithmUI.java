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

package org.hkijena.acaq5.ui.grapheditor.algorithmfinder;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQGraph;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotType;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.ui.components.AddAlgorithmSlotPanel;
import org.hkijena.acaq5.ui.events.AlgorithmFinderSuccessEvent;
import org.hkijena.acaq5.ui.grapheditor.ACAQGraphCanvasUI;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import static org.hkijena.acaq5.ui.grapheditor.ACAQNodeUI.SLOT_UI_HEIGHT;

/**
 * UI for finding algorithms
 */
public class ACAQAlgorithmFinderAlgorithmUI extends JPanel {
    private final ACAQGraphCanvasUI canvasUI;
    private final ACAQDataSlot outputSlot;
    private final ACAQGraph graph;
    private final ACAQGraphNode algorithm;
    private final int score;
    private final int maxScore;
    private final boolean isExistingInstance;
    private final EventBus eventBus = new EventBus();
    private final String compartment;
    private JPanel slotPanel;

    /**
     * Creates an algorithm UI for one target algorithm
     *
     * @param canvasUI    the canvas
     * @param outputSlot  The output slot to connect
     * @param declaration The target algorithm
     * @param score       Score of the target algorithm
     * @param maxScore    Maximum score that was possible
     */
    public ACAQAlgorithmFinderAlgorithmUI(ACAQGraphCanvasUI canvasUI, ACAQDataSlot outputSlot, ACAQAlgorithmDeclaration declaration, int score, int maxScore) {
        this.canvasUI = canvasUI;
        this.outputSlot = outputSlot;
        this.graph = canvasUI.getGraph();
        this.compartment = canvasUI.getCompartment();
        this.score = score;
        this.maxScore = maxScore;
        this.algorithm = declaration.newInstance();
        this.isExistingInstance = false;

        initialize();
    }

    /**
     * Creates an algorithm UI for one target algorithm
     *
     * @param canvasUI   the canvas
     * @param outputSlot The output slot to connect
     * @param algorithm  The target algorithm
     * @param score      Score of the target algorithm
     * @param maxScore   Maximum score that was possible
     */
    public ACAQAlgorithmFinderAlgorithmUI(ACAQGraphCanvasUI canvasUI, ACAQDataSlot outputSlot, ACAQGraphNode algorithm, int score, int maxScore) {
        this.canvasUI = canvasUI;
        this.outputSlot = outputSlot;
        this.graph = canvasUI.getGraph();
        this.compartment = canvasUI.getCompartment();
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
            title.append("<span style=\"color: blue;\">Existing </span>");
        else
            title.append("<span style=\"color: green;\">Create </span>");
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

    /**
     * Reloads the slots
     */
    public void reloadSlotUI() {
        slotPanel.removeAll();

        boolean createAddInputSlotButton = false;

        if (algorithm.getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration();
            createAddInputSlotButton = slotConfiguration.canAddInputSlot();
        }

        for (ACAQDataSlot slot : algorithm.getInputSlots()) {
            ACAQAlgorithmFinderSlotUI ui = new ACAQAlgorithmFinderSlotUI(canvasUI, outputSlot, slot, isExistingInstance);
            ui.getEventBus().register(this);
            slotPanel.add(ui);
        }

        if (createAddInputSlotButton) {
            JButton addInputSlotButton = createAddSlotButton(ACAQSlotType.Input);
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

    private JButton createAddSlotButton(ACAQSlotType slotType) {
        JButton button = new JButton(UIUtils.getIconFromResources("add.png"));
        UIUtils.makeFlat(button);
        button.addActionListener(e -> AddAlgorithmSlotPanel.showDialog(this, canvasUI.getGraphHistory(), algorithm, slotType));
        return button;
    }

    /**
     * Should trigger when the target algorithm slots are changed
     *
     * @param event Generated event
     */
    @Subscribe
    public void onAlgorithmSlotsChanged(AlgorithmSlotsChangedEvent event) {
        reloadSlotUI();
    }

    /**
     * Should trigger when a successful connection was made. Passes the event to the parent.
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
     * @return Event Bus instance
     */
    public EventBus getEventBus() {
        return eventBus;
    }
}
