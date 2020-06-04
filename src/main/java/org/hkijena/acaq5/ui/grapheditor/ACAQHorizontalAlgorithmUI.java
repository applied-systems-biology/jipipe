package org.hkijena.acaq5.ui.grapheditor;

import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQCompartmentOutput;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.utils.PointRange;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * An algorithm UI for horizontal display
 */
public class ACAQHorizontalAlgorithmUI extends ACAQAlgorithmUI {

    private List<ACAQDataSlotUI> slotUIList = new ArrayList<>();
    private JPanel inputSlotPanel;
    private JPanel outputSlotPanel;
    private JLabel nameLabel;

    /**
     * Creates a new UI
     *
     * @param graphUI   The graph UI that contains this UI
     * @param algorithm The algorithm
     */
    public ACAQHorizontalAlgorithmUI(ACAQAlgorithmGraphCanvasUI graphUI, ACAQGraphNode algorithm) {
        super(graphUI, algorithm, ACAQAlgorithmGraphCanvasUI.ViewMode.Horizontal);
        initialize();
        initializeContextMenu();
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

        nameLabel = new JLabel(getAlgorithm().getName());
        JButton openSettingsButton = new JButton(UIUtils.getIconFromResources("wrench.png"));
        UIUtils.makeFlat25x25(openSettingsButton);
        UIUtils.addPopupMenuToComponent(openSettingsButton, getContextMenu());


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
        int inputRows = getAlgorithm().getInputSlots().size();
        int outputRows = getAlgorithm().getOutputSlots().size();
        if (getAlgorithm().getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            ACAQMutableSlotConfiguration configuration = (ACAQMutableSlotConfiguration) getAlgorithm().getSlotConfiguration();
            if (configuration.canAddInputSlot() && getAlgorithm().getInputSlots().size() > 0) {
                inputRows += 1;
            }
            if (configuration.canAddOutputSlot() && getAlgorithm().getOutputSlots().size() > 0) {
                outputRows += 1;
            }
        }
        return Math.max(inputRows, outputRows);
    }

    private int calculateHeight() {
        return Math.max(SLOT_UI_HEIGHT, SLOT_UI_HEIGHT * getDisplayedRows());
    }

    private int calculateWidth() {
        FontRenderContext frc = new FontRenderContext(null, false, false);
        double width = 0;

        // Measure width of center
        {
            TextLayout layout = new TextLayout(getAlgorithm().getName(), getFont(), frc);
            width += layout.getBounds().getWidth();
        }

        // Measure slot widths
        {
            double maxInputSlotWidth = 0;
            double maxOutputSlotWidth = 0;
            for (ACAQDataSlotUI ui : slotUIList) {
                if (ui.getSlot().isInput()) {
                    maxInputSlotWidth = Math.max(maxInputSlotWidth, ui.calculateWidth());
                } else if (ui.getSlot().isOutput()) {
                    maxOutputSlotWidth = Math.max(maxOutputSlotWidth, ui.calculateWidth());
                }
            }

            width += maxInputSlotWidth + maxOutputSlotWidth;
        }

        return (int) Math.ceil(width * 1.0 / SLOT_UI_WIDTH) * SLOT_UI_WIDTH + 150;
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

        if (getAlgorithm().getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) getAlgorithm().getSlotConfiguration();
            createAddInputSlotButton = slotConfiguration.canAddInputSlot();
            createAddOutputSlotButton = slotConfiguration.canAddOutputSlot();
        }

        // For ACAQCompartmentOutput, we want to hide creating outputs / inputs depending on the current compartment
        if (getAlgorithm() instanceof ACAQCompartmentOutput) {
            if (getAlgorithm().getCompartment().equals(getGraphUI().getCompartment())) {
                createAddOutputSlotButton = false;
                createOutputSlots = false;
            } else {
                createAddInputSlotButton = false;
                createInputSlots = false;
            }
        }

        final int displayedRows = getDisplayedRows();
        int createdOutputSlots = 0;
        int createdInputSlots = 0;

        if (createInputSlots && getAlgorithm().getInputSlots().size() > 0) {
            List<ACAQDataSlot> slots = getAlgorithm().getInputSlots();
            for (int i = 0; i < slots.size(); ++i) {
                int bottomBorder = 0;
                if (i < displayedRows - 1)
                    bottomBorder = 1;

                ACAQDataSlot slot = slots.get(i);
                ACAQDataSlotUI ui = new ACAQHorizontalDataSlotUI(this, getGraphUI().getAlgorithmGraph(), getGraphUI().getCompartment(), slot);
                ui.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, bottomBorder, 1, getBorderColor()),
                        BorderFactory.createEmptyBorder(0, 0, 0, 4)));
                slotUIList.add(ui);
                inputSlotPanel.add(ui);
                ++createdInputSlots;
            }
        }
        if (createOutputSlots && getAlgorithm().getOutputSlots().size() > 0) {
            List<ACAQDataSlot> slots = getAlgorithm().getOutputSlots();
            for (int i = 0; i < slots.size(); ++i) {
                int bottomBorder = 0;
                if (i < displayedRows - 1)
                    bottomBorder = 1;
                ACAQDataSlot slot = slots.get(i);
                ACAQDataSlotUI ui = new ACAQHorizontalDataSlotUI(this, getGraphUI().getAlgorithmGraph(), getGraphUI().getCompartment(), slot);
                ui.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, bottomBorder, 0, getBorderColor()),
                        BorderFactory.createEmptyBorder(0, 4, 0, 0)));
                slotUIList.add(ui);
                outputSlotPanel.add(ui);
                ++createdOutputSlots;
            }
        }

        // Create slot for adding new output
        if (createAddInputSlotButton) {
            int bottomBorder = 0;
            if (createdInputSlots < displayedRows - 1)
                bottomBorder = 1;
            JButton addInputSlotButton = createAddSlotButton(ACAQDataSlot.SlotType.Input);
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
            JButton addOutputSlotButton = createAddSlotButton(ACAQDataSlot.SlotType.Output);
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
    public PointRange getSlotLocation(ACAQDataSlot slot) {
        if (slot.isInput()) {
            return new PointRange(0, getAlgorithm().getInputSlots().indexOf(slot) * SLOT_UI_HEIGHT + SLOT_UI_HEIGHT / 2);
        } else if (slot.isOutput()) {
            return new PointRange(getWidth(), getAlgorithm().getOutputSlots().indexOf(slot) * SLOT_UI_HEIGHT + SLOT_UI_HEIGHT / 2);
        } else {
            throw new UnsupportedOperationException("Unknown slot type!");
        }
    }

    @Override
    protected void updateName() {
        nameLabel.setText(getAlgorithm().getName());
    }

    @Override
    public void updateSize() {
        setSize(calculateWidth(), calculateHeight());
    }
}
