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
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.components.AddAlgorithmSlotPanel;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;


/**
 * UI for finding algorithms
 */
public class JIPipeAlgorithmSourceFinderAlgorithmUI extends JPanel {
    private final JIPipeGraphCanvasUI canvasUI;
    private final JIPipeDataSlot inputSlot;
    private final JIPipeGraphNode algorithm;
    private final boolean isExistingInstance;
    private final EventBus eventBus = new EventBus();
    private JPanel slotPanel;

    /**
     * Creates an algorithm UI for one target algorithm
     *
     * @param canvasUI  the canvas
     * @param inputSlot The output slot to connect
     * @param info      The target algorithm
     */
    public JIPipeAlgorithmSourceFinderAlgorithmUI(JIPipeGraphCanvasUI canvasUI, JIPipeDataSlot inputSlot, JIPipeNodeInfo info) {
        this.canvasUI = canvasUI;
        this.inputSlot = inputSlot;
        this.algorithm = info.newInstance();
        this.isExistingInstance = false;

        initialize();
    }

    /**
     * Creates an algorithm UI for one target algorithm
     *
     * @param canvasUI  the canvas
     * @param inputSlot The output slot to connect
     * @param algorithm The target algorithm
     */
    public JIPipeAlgorithmSourceFinderAlgorithmUI(JIPipeGraphCanvasUI canvasUI, JIPipeDataSlot inputSlot, JIPipeGraphNode algorithm) {
        this.canvasUI = canvasUI;
        this.inputSlot = inputSlot;
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

        JPanel centerPanel = new JPanel(new GridBagLayout());
        initializeCenterPanel(centerPanel);

        add(centerPanel, BorderLayout.CENTER);

        JPanel colorPanel = new JPanel();
        colorPanel.setBackground(UIUtils.getFillColorFor(algorithm.getInfo()));
        colorPanel.setPreferredSize(new Dimension(16, 1));
        add(colorPanel, BorderLayout.WEST);

        slotPanel = new JPanel();
        slotPanel.setLayout(new BoxLayout(slotPanel, BoxLayout.Y_AXIS));
        add(slotPanel, BorderLayout.EAST);
    }

    private void initializeCenterPanel(JPanel centerPanel) {

        // Label showing if a new or existing instance is shown
        JLabel actionLabel = new JLabel();
        if (isExistingInstance) {
            actionLabel.setText("Existing");
            actionLabel.setForeground(Color.BLUE);
        } else {
            actionLabel.setText("Create");
            actionLabel.setForeground(new Color(0, 128, 0));
        }
        centerPanel.add(actionLabel, new GridBagConstraints() {
            {
                anchor = WEST;
                gridx = 0;
                gridy = 0;
                insets = UIUtils.UI_PADDING;
            }
        });

        // The title and menu
        JLabel titleLabel = new JLabel(algorithm.getName());
        centerPanel.add(titleLabel, new GridBagConstraints() {
            {
                anchor = WEST;
                gridx = 1;
                gridy = 0;
                weightx = 1;
                fill = HORIZONTAL;
                insets = UIUtils.UI_PADDING;
            }
        });

        JIPipeNodeInfo info = algorithm.getInfo();
        String menuPath = info.getCategory().getName();
        if (!StringUtils.isNullOrEmpty(info.getMenuPath())) {
            menuPath += " > " + String.join(" > ", info.getMenuPath().split("\n"));
        }
        JLabel menuLabel = new JLabel(menuPath);
        menuLabel.setForeground(Color.GRAY);
        menuLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
        centerPanel.add(menuLabel, new GridBagConstraints() {
            {
                anchor = WEST;
                gridx = 1;
                gridy = 1;
                weightx = 1;
                fill = HORIZONTAL;
                insets = UIUtils.UI_PADDING;
            }
        });

        // The description
        JTextPane description = UIUtils.makeBorderlessReadonlyTextPane(info.getDescription().getHtml());
        description.setOpaque(false);
        centerPanel.add(description, new GridBagConstraints() {
            {
                anchor = WEST;
                gridx = 1;
                gridy = 2;
                weightx = 1;
                fill = HORIZONTAL;
                insets = UIUtils.UI_PADDING;
            }
        });

        // Outputs
        if (!algorithm.getInputSlots().isEmpty()) {
            int row = 3;
            centerPanel.add(new JLabel("Has following inputs:"), new GridBagConstraints() {
                {
                    anchor = WEST;
                    gridx = 1;
                    gridy = 3;
                    weightx = 1;
                    fill = HORIZONTAL;
                    insets = UIUtils.UI_PADDING;
                }
            });
            for (JIPipeDataSlot slot : algorithm.getInputSlots()) {
                ++row;
                int finalRow = row;
                JLabel label = new JLabel(slot.getName(), JIPipe.getDataTypes().getIconFor(slot.getAcceptedDataType()), JLabel.LEFT);
                label.setToolTipText(TooltipUtils.getDataTableTooltip(slot));
                centerPanel.add(label, new GridBagConstraints() {
                    {
                        anchor = WEST;
                        gridx = 1;
                        gridy = finalRow;
                        weightx = 1;
                        fill = HORIZONTAL;
                        insets = UIUtils.UI_PADDING;
                    }
                });
            }
        }
    }

    /**
     * Reloads the slots
     */
    public void reloadSlotUI() {
        slotPanel.removeAll();

        boolean createAddOutputSlotButton = false;

        if (algorithm.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
            JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) algorithm.getSlotConfiguration();
            createAddOutputSlotButton = slotConfiguration.canAddOutputSlot();
        }

        for (JIPipeDataSlot slot : algorithm.getOutputSlots()) {
            JIPipeAlgorithmSourceFinderSlotUI ui = new JIPipeAlgorithmSourceFinderSlotUI(canvasUI, inputSlot, slot, isExistingInstance);
            ui.getEventBus().register(this);
            slotPanel.add(ui);
        }

        if (createAddOutputSlotButton) {
            JButton addOutputSlotButton = createAddSlotButton(JIPipeSlotType.Output);
            addOutputSlotButton.setPreferredSize(new Dimension(25, 50));
            JPanel panel = new JPanel(new BorderLayout());
//            panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,0,1, getAlgorithmBorderColor()),
//                    BorderFactory.createEmptyBorder(0,0,0,4)));
            panel.add(addOutputSlotButton, BorderLayout.EAST);
            slotPanel.add(panel);
        }

        slotPanel.revalidate();
        slotPanel.repaint();
    }

    private JButton createAddSlotButton(JIPipeSlotType slotType) {
        JButton button = new JButton(UIUtils.getIconFromResources("actions/list-add.png"));
        UIUtils.makeFlat(button);
        button.addActionListener(e -> {
            if (!JIPipeProjectWorkbench.canModifySlots(canvasUI.getWorkbench()))
                return;
            AddAlgorithmSlotPanel.showDialog(this, canvasUI.getHistoryJournal(), algorithm, slotType);
        });
        return button;
    }

    /**
     * Should trigger when the target algorithm slots are changed
     *
     * @param event Generated event
     */
    @Subscribe
    public void onAlgorithmSlotsChanged(JIPipeGraph.NodeSlotsChangedEvent event) {
        if (!isDisplayable())
            return;
        reloadSlotUI();
    }

    /**
     * Should trigger when a successful connection was made. Passes the event to the parent.
     *
     * @param event Generated event
     */
    @Subscribe
    public void onAlgorithmFinderSuccess(AlgorithmFinderSuccessEvent event) {
        if (!isDisplayable())
            return;
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
