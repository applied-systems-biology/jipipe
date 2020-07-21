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
 * An algorithm UI for horizontal display
 */
public class JIPipeHorizontalNodeUI extends JIPipeNodeUI {

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
    public JIPipeHorizontalNodeUI(JIPipeWorkbench workbench, JIPipeGraphCanvasUI graphUI, JIPipeGraphNode algorithm) {
        super(workbench, graphUI, algorithm, JIPipeGraphViewMode.Horizontal);
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
        nameLabel.setIcon(JIPipeUIAlgorithmRegistry.getInstance().getIconFor(getNode().getInfo()));
        openSettingsButton = new JButton(UIUtils.getIconFromResources("actions/wrench.png"));
        UIUtils.makeFlat25x25(openSettingsButton);
        openSettingsButton.setBorder(null);
        openSettingsButton.addActionListener(e -> getEventBus().post(new AlgorithmUIActionRequestedEvent(this, REQUEST_OPEN_CONTEXT_MENU)));


//        initializeContextMenu(UIUtils.addContextMenuToComponent(this));

        add(inputSlotPanel, new GridBagConstraints() {
            {
                gridx = 0;
                fill = GridBagConstraints.VERTICAL;
                weighty = 1;
            }
        });
        addHorizontalGlue(1);
        add(openSettingsButton, new GridBagConstraints() {
            {
                gridx = 2;
                insets = new Insets(0, 0, 0, 4);
            }
        });
        add(nameLabel, new GridBagConstraints() {
            {
                gridx = 3;
            }
        });
        addHorizontalGlue(4);
        add(outputSlotPanel, new GridBagConstraints() {
            {
                gridx = 5;
                fill = GridBagConstraints.VERTICAL;
                weighty = 1;
            }
        });
    }

    /**
     * Contains the number of displayed rows. This includes the number of slot rows, and optionally additional rows for adding
     *
     * @return Displayed rows
     */
    private int getDisplayedRows() {
        int inputRows = getNode().getInputSlots().size();
        int outputRows = getNode().getOutputSlots().size();
        if (getNode().getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
            JIPipeMutableSlotConfiguration configuration = (JIPipeMutableSlotConfiguration) getNode().getSlotConfiguration();
            if (configuration.canAddInputSlot() && getNode().getInputSlots().size() > 0) {
                inputRows += 1;
            }
            if (configuration.canAddOutputSlot() && getNode().getOutputSlots().size() > 0) {
                outputRows += 1;
            }
        }
        return Math.max(inputRows, outputRows);
    }

    private int calculateHeight() {
        return Math.max(JIPipeGraphViewMode.Horizontal.getGridHeight(),
                JIPipeGraphViewMode.Horizontal.getGridHeight() * getDisplayedRows());
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
            double maxInputSlotWidth = 0;
            double maxOutputSlotWidth = 0;
            for (JIPipeDataSlotUI ui : slotUIList) {
                if (ui.getSlot().isInput()) {
                    maxInputSlotWidth = Math.max(maxInputSlotWidth, ui.calculateWidth());
                } else if (ui.getSlot().isOutput()) {
                    maxOutputSlotWidth = Math.max(maxOutputSlotWidth, ui.calculateWidth());
                }
            }

            width += maxInputSlotWidth + maxOutputSlotWidth;
        }

        return (int) Math.ceil(width * 1.0 / JIPipeGraphViewMode.Horizontal.getGridWidth())
                * JIPipeGraphViewMode.Horizontal.getGridWidth() + 150;
    }

    private void addHorizontalGlue(int column) {
        add(new JPanel() {
            {
                setOpaque(false);
            }
        }, new GridBagConstraints() {
            {
                gridx = column;
                fill = GridBagConstraints.VERTICAL | GridBagConstraints.HORIZONTAL;
                weighty = 1;
                weightx = 1;
            }
        });
    }

    @Override
    public void updateAlgorithmSlotUIs() {
        slotUIList.clear();
        inputSlotPanel.removeAll();
        outputSlotPanel.removeAll();
        inputSlotPanel.setLayout(new GridLayout(getDisplayedRows(), 1));
        outputSlotPanel.setLayout(new GridLayout(getDisplayedRows(), 1));

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

        final int displayedRows = getDisplayedRows();
        int createdOutputSlots = 0;
        int createdInputSlots = 0;

        if (createInputSlots && getNode().getInputSlots().size() > 0) {
            List<JIPipeDataSlot> slots = getNode().getInputSlots();
            for (int i = 0; i < slots.size(); ++i) {
                int bottomBorder = 0;
                if (i < displayedRows - 1)
                    bottomBorder = 1;

                JIPipeDataSlot slot = slots.get(i);
                JIPipeDataSlotUI ui = new JIPipeHorizontalDataSlotUI(getWorkbench(), this, getGraphUI().getCompartment(), slot);
                ui.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, bottomBorder, 1, getBorderColor()),
                        BorderFactory.createEmptyBorder(0, 0, 0, 4)));
                slotUIList.add(ui);
                inputSlotPanel.add(ui);
                inputSlotUIs.put(slot.getName(), ui);
                ++createdInputSlots;
            }
        }
        if (createOutputSlots && getNode().getOutputSlots().size() > 0) {
            List<JIPipeDataSlot> slots = getNode().getOutputSlots();
            for (int i = 0; i < slots.size(); ++i) {
                int bottomBorder = 0;
                if (i < displayedRows - 1)
                    bottomBorder = 1;
                JIPipeDataSlot slot = slots.get(i);
                JIPipeDataSlotUI ui = new JIPipeHorizontalDataSlotUI(getWorkbench(), this, getGraphUI().getCompartment(), slot);
                ui.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, bottomBorder, 0, getBorderColor()),
                        BorderFactory.createEmptyBorder(0, 4, 0, 0)));
                slotUIList.add(ui);
                outputSlotPanel.add(ui);
                outputSlotUIs.put(slot.getName(), ui);
                ++createdOutputSlots;
            }
        }

        // Create slot for adding new output
        if (createAddInputSlotButton) {
            int bottomBorder = 0;
            if (createdInputSlots < displayedRows - 1)
                bottomBorder = 1;
            JButton addInputSlotButton = createAddSlotButton(JIPipeSlotType.Input);
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, bottomBorder, 1, getBorderColor()),
                    BorderFactory.createEmptyBorder(0, 0, 0, 4)));
            panel.add(addInputSlotButton, BorderLayout.WEST);
            inputSlotPanel.add(panel);
        }
        if (createAddOutputSlotButton) {
            int bottomBorder = 0;
            if (createdOutputSlots < displayedRows - 1)
                bottomBorder = 1;
            JButton addOutputSlotButton = createAddSlotButton(JIPipeSlotType.Output);
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, bottomBorder, 0, getBorderColor()),
                    BorderFactory.createEmptyBorder(0, 4, 0, 0)));
            panel.add(addOutputSlotButton, BorderLayout.EAST);
            outputSlotPanel.add(panel);
        }

        setSize(new Dimension(calculateWidth(), calculateHeight()));
        revalidate();
        repaint();
    }

    @Override
    public PointRange getSlotLocation(JIPipeDataSlot slot) {
        if (slot.isInput()) {
            return new PointRange(0, getNode().getInputSlots().indexOf(slot) * JIPipeGraphViewMode.Horizontal.getGridHeight() +
                    JIPipeGraphViewMode.Horizontal.getGridHeight() / 2);
        } else if (slot.isOutput()) {
            return new PointRange(getWidth(), getNode().getOutputSlots().indexOf(slot) * JIPipeGraphViewMode.Horizontal.getGridHeight() +
                    JIPipeGraphViewMode.Horizontal.getGridHeight() / 2);
        } else {
            throw new UnsupportedOperationException("Unknown slot type!");
        }
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
                    openSettingsButton.setIcon(UIUtils.getIconFromResources("actions/wrench.png"));
                } else {
                    setBackground(Color.WHITE);
                    nameLabel.setForeground(Color.BLACK);
                    openSettingsButton.setIcon(UIUtils.getIconFromResources("emblems/pass-through-h.png"));
                }
            } else {
                setBackground(new Color(227, 86, 86));
                nameLabel.setForeground(Color.WHITE);
                openSettingsButton.setIcon(UIUtils.getIconFromResources("emblems/block.png"));
            }
        } else {
            setBackground(getFillColor());
            nameLabel.setForeground(Color.BLACK);
            openSettingsButton.setIcon(UIUtils.getIconFromResources("actions/wrench.png"));
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
    public Map<String, JIPipeDataSlotUI> getInputSlotUIs() {
        return Collections.unmodifiableMap(inputSlotUIs);
    }

    @Override
    public Map<String, JIPipeDataSlotUI> getOutputSlotUIs() {
        return Collections.unmodifiableMap(outputSlotUIs);
    }
}
