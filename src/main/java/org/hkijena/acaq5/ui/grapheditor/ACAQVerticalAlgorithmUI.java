package org.hkijena.acaq5.ui.grapheditor;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQCompartmentOutput;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.ui.registries.ACAQUIAlgorithmRegistry;
import org.hkijena.acaq5.utils.PointRange;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * An algorithm UI for vertical display
 */
public class ACAQVerticalAlgorithmUI extends ACAQAlgorithmUI {

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
    public ACAQVerticalAlgorithmUI(ACAQAlgorithmGraphCanvasUI graphUI, ACAQGraphNode algorithm) {
        super(graphUI, algorithm, ACAQAlgorithmGraphCanvasUI.ViewMode.Vertical);
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
        nameLabel.setIcon(ACAQUIAlgorithmRegistry.getInstance().getIconFor(getAlgorithm().getDeclaration()));
        JButton openSettingsButton = new JButton(UIUtils.getIconFromResources("wrench.png"));
        UIUtils.makeFlat25x25(openSettingsButton);
        UIUtils.addPopupMenuToComponent(openSettingsButton, getContextMenu());


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
            TextLayout layout = new TextLayout(getAlgorithm().getName(), getFont(), frc);
            width += layout.getBounds().getWidth();
        }

        // Measure slot widths
        {
            double maxWidth = 0;
            for (ACAQDataSlotUI ui : slotUIList) {
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
        int inputRows = getAlgorithm().getInputSlots().size();
        if (getAlgorithm().getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            ACAQMutableSlotConfiguration configuration = (ACAQMutableSlotConfiguration) getAlgorithm().getSlotConfiguration();
            if (configuration.canAddInputSlot() && getAlgorithm().getInputSlots().size() > 0) {
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
        int outputRows = getAlgorithm().getOutputSlots().size();
        if (getAlgorithm().getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            ACAQMutableSlotConfiguration configuration = (ACAQMutableSlotConfiguration) getAlgorithm().getSlotConfiguration();
            if (configuration.canAddOutputSlot() && getAlgorithm().getOutputSlots().size() > 0) {
                outputRows += 1;
            }
        }
        return outputRows;
    }

    @Override
    public void updateAlgorithmSlotUIs() {
        slotUIList.clear();
        inputSlotPanel.removeAll();
        outputSlotPanel.removeAll();
        inputSlotPanel.setLayout(new GridLayout(1, getDisplayedInputColumns()));
        outputSlotPanel.setLayout(new GridLayout(1, getDisplayedOutputColumns()));

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

        final int displayedInputColumns = getDisplayedInputColumns();
        final int displayedOutputColumns = getDisplayedOutputColumns();
        int createdOutputSlots = 0;
        int createdInputSlots = 0;

        if (createInputSlots && getAlgorithm().getInputSlots().size() > 0) {
            List<ACAQDataSlot> slots = getAlgorithm().getInputSlots();
            for (int i = 0; i < slots.size(); ++i) {
                int rightBorder = 0;
                if (i < displayedInputColumns - 1)
                    rightBorder = 1;

                ACAQDataSlot slot = slots.get(i);
                ACAQDataSlotUI ui = new ACAQVerticalDataSlotUI(this, getGraphUI().getAlgorithmGraph(), getGraphUI().getCompartment(), slot);
                ui.setBorder(BorderFactory.createMatteBorder(0, 0, 1, rightBorder, getBorderColor()));
                slotUIList.add(ui);
                inputSlotPanel.add(ui);
                ++createdInputSlots;
            }
        }
        if (createOutputSlots && getAlgorithm().getOutputSlots().size() > 0) {
            List<ACAQDataSlot> slots = getAlgorithm().getOutputSlots();
            for (int i = 0; i < slots.size(); ++i) {
                int rightBorder = 0;
                if (i < displayedOutputColumns - 1)
                    rightBorder = 1;
                ACAQDataSlot slot = slots.get(i);
                ACAQDataSlotUI ui = new ACAQVerticalDataSlotUI(this, getGraphUI().getAlgorithmGraph(), getGraphUI().getCompartment(), slot);
                ui.setBorder(BorderFactory.createMatteBorder(1, 0, 0, rightBorder, getBorderColor()));
                slotUIList.add(ui);
                outputSlotPanel.add(ui);
                ++createdOutputSlots;
            }
        }

        // Create slot for adding new output
        if (createAddInputSlotButton) {
            int rightBorder = 0;
            if (createdInputSlots < displayedInputColumns - 1)
                rightBorder = 1;
            JButton addInputSlotButton = createAddSlotButton(ACAQDataSlot.SlotType.Input);
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, rightBorder, getBorderColor()));
            panel.add(addInputSlotButton, BorderLayout.CENTER);
            inputSlotPanel.add(panel);
        }
        if (createAddOutputSlotButton) {
            int rightBorder = 0;
            if (createdOutputSlots < displayedOutputColumns - 1)
                rightBorder = 1;
            JButton addOutputSlotButton = createAddSlotButton(ACAQDataSlot.SlotType.Output);
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
        nameLabel.setText(getAlgorithm().getName());
    }

    @Override
    protected void updateActivationStatus() {
        if (getAlgorithm() instanceof ACAQAlgorithm) {
            if (((ACAQAlgorithm) getAlgorithm()).isEnabled()) {
                setBackground(getFillColor());
                nameLabel.setForeground(Color.BLACK);
            } else {
                setBackground(Color.LIGHT_GRAY);
                nameLabel.setForeground(Color.GRAY);
            }
        } else {
            setBackground(getFillColor());
            nameLabel.setForeground(Color.BLACK);
        }
    }

    @Override
    public void updateSize() {
        setSize(calculateWidth(), calculateHeight());
    }

    @Override
    public PointRange getSlotLocation(ACAQDataSlot slot) {
        if (slot.isInput()) {
            int nColumns = getDisplayedInputColumns();
            int columnWidth = getWidth() / nColumns;
            int minX = getAlgorithm().getInputSlots().indexOf(slot) * columnWidth;
            return new PointRange(new Point(minX + columnWidth / 2, 3),
                    new Point(minX + 20, 3),
                    new Point(minX + columnWidth - 20, 3));
        } else if (slot.isOutput()) {
            int nColumns = getDisplayedOutputColumns();
            int columnWidth = getWidth() / nColumns;
            int minX = getAlgorithm().getOutputSlots().indexOf(slot) * columnWidth;
            return new PointRange(new Point(minX + columnWidth / 2, getHeight() - 3),
                    new Point(minX + 20, getHeight() - 3),
                    new Point(minX + columnWidth - 20, getHeight() - 3));
        } else {
            throw new UnsupportedOperationException("Unknown slot type!");
        }
    }
}
