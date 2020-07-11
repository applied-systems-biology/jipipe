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

package org.hkijena.jipipe.ui.grapheditor;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithm;
import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.events.AlgorithmUIActionRequestedEvent;
import org.hkijena.jipipe.ui.registries.JIPipeUIAlgorithmRegistry;
import org.hkijena.jipipe.utils.PointRange;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An algorithm UI for vertical display
 */
public class JIPipeVerticalNodeUI extends JIPipeNodeUI {

    private List<JIPipeDataSlotUI> slotUIList = new ArrayList<>();
    private BiMap<String, JIPipeDataSlotUI> inputSlotUIs = HashBiMap.create();
    private BiMap<String, JIPipeDataSlotUI> outputSlotUIs = HashBiMap.create();
    private JPanel inputSlotPanel;
    private JPanel outputSlotPanel;
    private JLabel nameLabel;
    private JButton openSettingsButton;

    /**
     * Creates a new UI
     *
     * @param workbench
     * @param graphUI   The graph UI that contains this UI
     * @param algorithm The algorithm
     */
    public JIPipeVerticalNodeUI(JIPipeWorkbench workbench, JIPipeGraphCanvasUI graphUI, JIPipeGraphNode algorithm) {
        super(workbench, graphUI, algorithm, JIPipeGraphCanvasUI.ViewMode.Vertical);
        initialize();
        updateAlgorithmSlotUIs();
    }

    private void initialize() {
        setBackground(getFillColor());
        setBorder(BorderFactory.createLineBorder(getBorderColor()));
        setSize(new Dimension(calculateWidth(), calculateHeight()));
        setLayout(new GridBagLayout());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        inputSlotPanel = new JPanel();
        inputSlotPanel.setOpaque(false);
        outputSlotPanel = new JPanel();
        outputSlotPanel.setOpaque(false);

        nameLabel = new JLabel(getNode().getName());
        nameLabel.setIcon(JIPipeUIAlgorithmRegistry.getInstance().getIconFor(getNode().getDeclaration()));
        openSettingsButton = new JButton(UIUtils.getIconFromResources("wrench.png"));
        UIUtils.makeFlat25x25(openSettingsButton);
        openSettingsButton.setBorder(null);
        openSettingsButton.addActionListener(e -> getEventBus().post(new AlgorithmUIActionRequestedEvent(this, REQUEST_OPEN_CONTEXT_MENU)));


//        initializeContextMenu(UIUtils.addContextMenuToComponent(this));

        add(inputSlotPanel, new GridBagConstraints() {
            {
                gridy = 0;
                gridwidth = 2;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
            }
        });
        addVerticalGlue(1);
        add(openSettingsButton, new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 2;
                insets = new Insets(0, 0, 0, 4);
                anchor = GridBagConstraints.EAST;
            }
        });
        add(nameLabel, new GridBagConstraints() {
            {
                gridy = 2;
                gridx = 1;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
            }
        });
        addVerticalGlue(3);
        add(outputSlotPanel, new GridBagConstraints() {
            {
                gridy = 4;
                gridwidth = 2;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
            }
        });
    }

    private int calculateHeight() {
        return 3 * SLOT_UI_HEIGHT;
    }

    private int calculateWidth() {
        FontRenderContext frc = new FontRenderContext(null, false, false);
        double width = 0;

        // Measure width of center
        {
            TextLayout layout = new TextLayout(getNode().getName(), getFont(), frc);
            width += layout.getBounds().getWidth();
        }

        // Measure slot widths
        {
            double maxWidth = 0;
            for (JIPipeDataSlotUI ui : slotUIList) {
                maxWidth = Math.max(maxWidth, ui.calculateWidth());
            }

            width = Math.max(width, maxWidth * Math.max(getDisplayedInputColumns(), getDisplayedOutputColumns()));
        }

        return (int) Math.ceil(width * 1.0 / SLOT_UI_WIDTH) * SLOT_UI_WIDTH + 150;
    }

    private void addVerticalGlue(int row) {
        add(new JPanel() {
            {
                setOpaque(false);
            }
        }, new GridBagConstraints() {
            {
                gridy = row;
                fill = GridBagConstraints.VERTICAL | GridBagConstraints.HORIZONTAL;
                weighty = 1;
                weightx = 1;
            }
        });
    }

    /**
     * Contains the number of displayed columns. This includes the number of slot columns, and optionally additional rows for adding
     *
     * @return Displayed rows
     */
    private int getDisplayedInputColumns() {
        int inputRows = getNode().getInputSlots().size();
        if (getNode().getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
            JIPipeMutableSlotConfiguration configuration = (JIPipeMutableSlotConfiguration) getNode().getSlotConfiguration();
            if (configuration.canAddInputSlot() && getNode().getInputSlots().size() > 0) {
                inputRows += 1;
            }
        }
        return inputRows;
    }

    /**
     * Contains the number of displayed columns. This includes the number of slot columns, and optionally additional rows for adding
     *
     * @return Displayed rows
     */
    private int getDisplayedOutputColumns() {
        int outputRows = getNode().getOutputSlots().size();
        if (getNode().getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
            JIPipeMutableSlotConfiguration configuration = (JIPipeMutableSlotConfiguration) getNode().getSlotConfiguration();
            if (configuration.canAddOutputSlot() && getNode().getOutputSlots().size() > 0) {
                outputRows += 1;
            }
        }
        return outputRows;
    }

    @Override
    public void updateAlgorithmSlotUIs() {
        slotUIList.clear();
        inputSlotUIs.clear();
        outputSlotUIs.clear();
        inputSlotPanel.removeAll();
        outputSlotPanel.removeAll();
        inputSlotPanel.setLayout(new GridLayout(1, getDisplayedInputColumns()));
        outputSlotPanel.setLayout(new GridLayout(1, getDisplayedOutputColumns()));

        boolean createAddInputSlotButton = false;
        boolean createAddOutputSlotButton = false;
        boolean createInputSlots = true;
        boolean createOutputSlots = true;

        if (getNode().getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
            JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) getNode().getSlotConfiguration();
            createAddInputSlotButton = slotConfiguration.canAddInputSlot();
            createAddOutputSlotButton = slotConfiguration.canAddOutputSlot();
        }

        // For JIPipeCompartmentOutput, we want to hide creating outputs / inputs depending on the current compartment
        if (getNode() instanceof JIPipeCompartmentOutput) {
            if (getNode().getCompartment().equals(getGraphUI().getCompartment())) {
                createAddOutputSlotButton = false;
                createOutputSlots = false;
            } else {
                createAddInputSlotButton = false;
                createInputSlots = false;
            }
        }
        if (!getNode().renderInputSlots()) {
            createAddInputSlotButton = false;
            createInputSlots = false;
        }
        if (!getNode().renderOutputSlots()) {
            createAddOutputSlotButton = false;
            createOutputSlots = false;
        }

        final int displayedInputColumns = getDisplayedInputColumns();
        final int displayedOutputColumns = getDisplayedOutputColumns();
        int createdOutputSlots = 0;
        int createdInputSlots = 0;

        if (createInputSlots && getNode().getInputSlots().size() > 0) {
            List<JIPipeDataSlot> slots = getNode().getInputSlots();
            for (int i = 0; i < slots.size(); ++i) {
                int rightBorder = 0;
                if (i < displayedInputColumns - 1)
                    rightBorder = 1;

                JIPipeDataSlot slot = slots.get(i);
                JIPipeDataSlotUI ui = new JIPipeVerticalDataSlotUI(getWorkbench(), this, getGraphUI().getCompartment(), slot);
                ui.setBorder(BorderFactory.createMatteBorder(0, 0, 1, rightBorder, getBorderColor()));
                slotUIList.add(ui);
                inputSlotPanel.add(ui);
                inputSlotUIs.put(slot.getName(), ui);
                ++createdInputSlots;
            }
        }
        if (createOutputSlots && getNode().getOutputSlots().size() > 0) {
            List<JIPipeDataSlot> slots = getNode().getOutputSlots();
            for (int i = 0; i < slots.size(); ++i) {
                int rightBorder = 0;
                if (i < displayedOutputColumns - 1)
                    rightBorder = 1;
                JIPipeDataSlot slot = slots.get(i);
                JIPipeDataSlotUI ui = new JIPipeVerticalDataSlotUI(getWorkbench(), this, getGraphUI().getCompartment(), slot);
                ui.setBorder(BorderFactory.createMatteBorder(1, 0, 0, rightBorder, getBorderColor()));
                slotUIList.add(ui);
                outputSlotPanel.add(ui);
                outputSlotUIs.put(slot.getName(), ui);
                ++createdOutputSlots;
            }
        }

        // Create slot for adding new output
        if (createAddInputSlotButton) {
            int rightBorder = 0;
            if (createdInputSlots < displayedInputColumns - 1)
                rightBorder = 1;
            JButton addInputSlotButton = createAddSlotButton(JIPipeSlotType.Input);
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, rightBorder, getBorderColor()));
            panel.add(addInputSlotButton, BorderLayout.CENTER);
            inputSlotPanel.add(panel);
        }
        if (createAddOutputSlotButton) {
            int rightBorder = 0;
            if (createdOutputSlots < displayedOutputColumns - 1)
                rightBorder = 1;
            JButton addOutputSlotButton = createAddSlotButton(JIPipeSlotType.Output);
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, rightBorder, getBorderColor()));
            panel.add(addOutputSlotButton, BorderLayout.CENTER);
            outputSlotPanel.add(panel);
        }

        setSize(new Dimension(calculateWidth(), calculateHeight()));
        revalidate();
        repaint();
    }

    @Override
    protected void updateName() {
        nameLabel.setText(getNode().getName());
    }

    @Override
    protected void updateActivationStatus() {
        if (getNode() instanceof JIPipeAlgorithm) {
            JIPipeAlgorithm algorithm = (JIPipeAlgorithm) getNode();
            if (algorithm.isEnabled()) {
                if (!algorithm.isPassThrough()) {
                    setBackground(getFillColor());
                    nameLabel.setForeground(Color.BLACK);
                    openSettingsButton.setIcon(UIUtils.getIconFromResources("wrench.png"));
                } else {
                    setBackground(Color.WHITE);
                    nameLabel.setForeground(Color.BLACK);
                    openSettingsButton.setIcon(UIUtils.getIconFromResources("pass-through.png"));
                }
            } else {
                setBackground(new Color(227, 86, 86));
                nameLabel.setForeground(Color.WHITE);
                openSettingsButton.setIcon(UIUtils.getIconFromResources("block.png"));
            }
        } else {
            setBackground(getFillColor());
            nameLabel.setForeground(Color.BLACK);
            openSettingsButton.setIcon(UIUtils.getIconFromResources("wrench.png"));
        }
    }

    @Override
    public void updateSize() {
        int oldWidth = getWidth();
        int oldHeight = getHeight();
        setSize(calculateWidth(), calculateHeight());
        if (getWidth() != oldWidth || oldHeight != getHeight())
            getGraphUI().repaint();
    }

    @Override
    public PointRange getSlotLocation(JIPipeDataSlot slot) {
        if (slot.isInput()) {
            int nColumns = getDisplayedInputColumns();
            int columnWidth = getWidth() / nColumns;
            int minX = getNode().getInputSlots().indexOf(slot) * columnWidth;
            return new PointRange(new Point(minX + columnWidth / 2, 3),
                    new Point(minX + 20, 3),
                    new Point(minX + columnWidth - 20, 3));
        } else if (slot.isOutput()) {
            int nColumns = getDisplayedOutputColumns();
            int columnWidth = getWidth() / nColumns;
            int minX = getNode().getOutputSlots().indexOf(slot) * columnWidth;
            return new PointRange(new Point(minX + columnWidth / 2, getHeight() - 3),
                    new Point(minX + 20, getHeight() - 3),
                    new Point(minX + columnWidth - 20, getHeight() - 3));
        } else {
            throw new UnsupportedOperationException("Unknown slot type!");
        }
    }

    @Override
    public Map<String, JIPipeDataSlotUI> getInputSlotUIs() {
        return Collections.unmodifiableMap(inputSlotUIs);
    }

    @Override
    public Map<String, JIPipeDataSlotUI> getOutputSlotUIs() {
        return Collections.unmodifiableMap(outputSlotUIs);
    }
}
