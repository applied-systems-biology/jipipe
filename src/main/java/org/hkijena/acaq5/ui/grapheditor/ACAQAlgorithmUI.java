package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQIOSlotConfiguration;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQCompartmentOutput;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.events.TraitConfigurationChangedEvent;
import org.hkijena.acaq5.ui.events.AlgorithmSelectedEvent;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ACAQAlgorithmUI extends JPanel {

    /**
     * Height assigned for one slot
     */
    public static final int SLOT_UI_HEIGHT = 50;

    /**
     * Grid width for horizontal direction
     */
    public static final int SLOT_UI_WIDTH = 25;

    private ACAQAlgorithmGraphCanvasUI graphUI;
    private ACAQAlgorithm algorithm;
    private JPanel inputSlotPanel;
    private JPanel outputSlotPanel;
    private List<ACAQDataSlotUI> slotUIList = new ArrayList<>();
    private EventBus eventBus = new EventBus();
    private JLabel nameLabel;

    private Color fillColor;
    private Color borderColor;

    private boolean selected;

    public ACAQAlgorithmUI(ACAQAlgorithmGraphCanvasUI graphUI, ACAQAlgorithm algorithm) {
        this.graphUI = graphUI;
        this.algorithm = algorithm;
        this.algorithm.getEventBus().register(this);
        this.algorithm.getTraitConfiguration().getEventBus().register(this);
        this.fillColor = UIUtils.getFillColorFor(algorithm.getDeclaration());
        this.borderColor = UIUtils.getBorderColorFor(algorithm.getDeclaration());
        initialize();
        updateAlgorithmSlotUIs();
    }

    private void initialize() {
        setBackground(fillColor);
        updateBorder();
        setSize(new Dimension(calculateWidth(), calculateHeight()));
        setLayout(new GridBagLayout());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        inputSlotPanel = new JPanel();
        inputSlotPanel.setOpaque(false);
        outputSlotPanel = new JPanel();
        outputSlotPanel.setOpaque(false);

        nameLabel = new JLabel(algorithm.getName());
        JButton openSettingsButton = new JButton(UIUtils.getIconFromResources("wrench.png"));
        UIUtils.makeFlat25x25(openSettingsButton);
        initializeContextMenu(UIUtils.addPopupMenuToComponent(openSettingsButton));

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

    private void initializeContextMenu(JPopupMenu menu) {
        JMenuItem selectOnlyButton = new JMenuItem("Open settings", UIUtils.getIconFromResources("cog.png"));
        selectOnlyButton.addActionListener(e -> eventBus.post(new AlgorithmSelectedEvent(this, false)));
        menu.add(selectOnlyButton);

        JMenuItem addToSelectionButton = new JMenuItem("Add to selection", UIUtils.getIconFromResources("select.png"));
        addToSelectionButton.addActionListener(e -> eventBus.post(new AlgorithmSelectedEvent(this, true)));
        menu.add(addToSelectionButton);

        menu.addSeparator();

        if (algorithm instanceof ACAQProjectCompartment) {
            JMenuItem deleteButton = new JMenuItem("Delete compartment", UIUtils.getIconFromResources("delete.png"));
            deleteButton.addActionListener(e -> removeCompartment());
            menu.add(deleteButton);
        } else {
            JMenuItem deleteButton = new JMenuItem("Delete algorithm", UIUtils.getIconFromResources("delete.png"));
            deleteButton.setEnabled(graphUI.getAlgorithmGraph().canUserDelete(algorithm));
            deleteButton.addActionListener(e -> removeAlgorithm());
            menu.add(deleteButton);
        }

    }

    private void removeCompartment() {
        ACAQProjectCompartment compartment = (ACAQProjectCompartment) algorithm;
        if (JOptionPane.showConfirmDialog(this, "Do you really want to delete the compartment '" + compartment.getName() + "'?\n" +
                "You will lose all nodes stored in this compartment.", "Delete compartment", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            compartment.getProject().removeCompartment(compartment);
        }
    }

    private void removeAlgorithm() {
        if (JOptionPane.showConfirmDialog(this,
                "Do you really want to remove the algorithm '" + algorithm.getName() + "'?", "Delete algorithm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            graphUI.getAlgorithmGraph().removeNode(algorithm);
        }
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

    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }

    private JButton createAddSlotButton(ACAQDataSlot.SlotType slotType) {
        JButton button = new JButton(UIUtils.getIconFromResources("add.png"));
        button.setPreferredSize(new Dimension(25, SLOT_UI_HEIGHT));
        UIUtils.makeFlat(button);

        JPopupMenu menu = UIUtils.addPopupMenuToComponent(button);
        ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration();

        Set<Class<? extends ACAQData>> allowedDataTypes;
        switch (slotType) {
            case Input:
                allowedDataTypes = slotConfiguration.getAllowedInputSlotTypes();
                break;
            case Output:
                allowedDataTypes = slotConfiguration.getAllowedOutputSlotTypes();
                break;
            default:
                throw new RuntimeException();
        }

        for (Class<? extends ACAQData> dataClass : allowedDataTypes) {
            JMenuItem item = new JMenuItem(ACAQData.getNameOf(dataClass), ACAQUIDatatypeRegistry.getInstance().getIconFor(dataClass));
            item.addActionListener(e -> addNewSlot(slotType, dataClass));
            menu.add(item);
        }

        return button;
    }

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

        if (algorithm.getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration();
            createAddInputSlotButton = slotConfiguration.canAddInputSlot();
            createAddOutputSlotButton = slotConfiguration.canAddOutputSlot();
        }

        // For ACAQCompartmentOutput, we want to hide creating outputs / inputs depending on the current compartment
        if (algorithm instanceof ACAQCompartmentOutput) {
            if (algorithm.getCompartment().equals(graphUI.getCompartment())) {
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

        if (createInputSlots && algorithm.getInputSlots().size() > 0) {
            List<ACAQDataSlot> slots = algorithm.getInputSlots();
            for (int i = 0; i < slots.size(); ++i) {
                int bottomBorder = 0;
                if (i < displayedRows - 1)
                    bottomBorder = 1;

                ACAQDataSlot slot = slots.get(i);
                ACAQDataSlotUI ui = new ACAQDataSlotUI(graphUI.getAlgorithmGraph(), graphUI.getCompartment(), slot);
                ui.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, bottomBorder, 1, borderColor),
                        BorderFactory.createEmptyBorder(0, 0, 0, 4)));
                slotUIList.add(ui);
                inputSlotPanel.add(ui);
                ++createdInputSlots;
            }
        }
        if (createOutputSlots && algorithm.getOutputSlots().size() > 0) {
            List<ACAQDataSlot> slots = algorithm.getOutputSlots();
            for (int i = 0; i < slots.size(); ++i) {
                int bottomBorder = 0;
                if (i < displayedRows - 1)
                    bottomBorder = 1;
                ACAQDataSlot slot = slots.get(i);
                ACAQDataSlotUI ui = new ACAQDataSlotUI(graphUI.getAlgorithmGraph(), graphUI.getCompartment(), slot);
                ui.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, bottomBorder, 0, borderColor),
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
            panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, bottomBorder, 1, borderColor),
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
            panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, bottomBorder, 0, borderColor),
                    BorderFactory.createEmptyBorder(0, 4, 0, 0)));
            panel.add(addOutputSlotButton, BorderLayout.EAST);
            outputSlotPanel.add(panel);
        }

        setSize(new Dimension(calculateWidth(), calculateHeight()));
        revalidate();
        repaint();
    }

    private void addNewSlot(ACAQDataSlot.SlotType slotType, Class<? extends ACAQData> klass) {
        if (algorithm.getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration();

            int existingSlots = slotType == ACAQDataSlot.SlotType.Input ? algorithm.getInputSlots().size() : algorithm.getOutputSlots().size();
            String initialValue = slotType + " data ";

            // This is general
            if (getAlgorithm().getSlotConfiguration() instanceof ACAQIOSlotConfiguration) {
                initialValue = "Data ";
            }

            String name = null;
            while (name == null) {
                String newName = JOptionPane.showInputDialog(this, "Please a data slot name", initialValue + (existingSlots + 1));
                if (newName == null || newName.trim().isEmpty())
                    return;
                newName = StringUtils.makeFilesystemCompatible(newName);
                if (slotConfiguration.hasSlot(newName))
                    continue;
                name = newName;
            }
            switch (slotType) {
                case Input:
                    slotConfiguration.addInputSlot(name, klass);
                    break;
                case Output:
                    slotConfiguration.addOutputSlot(name, klass);
                    break;
            }
        }
    }

    /**
     * Contains the number of displayed rows. This includes the number of slot rows, and optionally additional rows for adding
     *
     * @return
     */
    private int getDisplayedRows() {
        int inputRows = algorithm.getInputSlots().size();
        int outputRows = algorithm.getOutputSlots().size();
        if (algorithm.getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            ACAQMutableSlotConfiguration configuration = (ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration();
            if (configuration.canModifyInputSlots() && algorithm.getInputSlots().size() > 0) {
                inputRows += 1;
            }
            if (configuration.canModifyOutputSlots() && algorithm.getOutputSlots().size() > 0) {
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
            TextLayout layout = new TextLayout(algorithm.getName(), getFont(), frc);
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

    /**
     * Tries to move the node to the provided location
     * A grid is applied to the input coordinates
     *
     * @param x
     * @param y
     * @return
     */
    public boolean trySetLocationInGrid(int x, int y) {
        y = (int) Math.rint(y * 1.0 / ACAQAlgorithmUI.SLOT_UI_HEIGHT) * ACAQAlgorithmUI.SLOT_UI_HEIGHT;
        x = (int) Math.rint(x * 1.0 / ACAQAlgorithmUI.SLOT_UI_WIDTH) * ACAQAlgorithmUI.SLOT_UI_WIDTH;
        return trySetLocationNoGrid(x, y);
    }

    /**
     * Returns true if this component overlaps with another component
     *
     * @return
     */
    public boolean isOverlapping() {
        for (int i = 0; i < graphUI.getComponentCount(); ++i) {
            Component component = graphUI.getComponent(i);
            if (component instanceof ACAQAlgorithmUI) {
                ACAQAlgorithmUI ui = (ACAQAlgorithmUI) component;
                if (ui != this) {
                    if (ui.getBounds().intersects(getBounds())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Tries to move the node to the provided location
     * A grid is applied to the input coordinates
     *
     * @param x
     * @param y
     * @return
     */
    public boolean trySetLocationNoGrid(int x, int y) {
        // Check for collisions
        Rectangle futureBounds = new Rectangle(x, y, getWidth(), getHeight());
        for (int i = 0; i < graphUI.getComponentCount(); ++i) {
            Component component = graphUI.getComponent(i);
            if (component instanceof ACAQAlgorithmUI) {
                ACAQAlgorithmUI ui = (ACAQAlgorithmUI) component;
                if (ui != this) {
                    if (ui.getBounds().intersects(futureBounds)) {
                        return false;
                    }
                }
            }
        }

        setLocation(x, y);
        return true;
    }

//    @Override
//    protected void paintComponent(Graphics g) {
//        super.paintComponent(g);
//
//        Graphics2D graphics = (Graphics2D)g;
////        RenderingHints rh = new RenderingHints(
////                RenderingHints.KEY_ANTIALIASING,
////                RenderingHints.VALUE_ANTIALIAS_ON);
////        graphics.setRenderingHints(rh);
//        for(int x = inputSlotPanel.getWidth(); x < getWidth() - outputSlotPanel.getWidth(); ++x) {
//            for(int y = 0; y < getHeight(); ++y) {
//                if((x - y) % 3 == 0) {
//                    if(y % 2 == 0)
//                        graphics.setColor(Color.GRAY);
//                    else
//                        graphics.setColor(Color.WHITE);
//                    graphics.fillRect(x, y, 1, 1);
//                }
//            }
//        }
//    }

    /**
     * Get the Y location of the bottom part
     *
     * @return
     */
    public int getBottomY() {
        return getY() + getHeight();
    }

    @Subscribe
    public void onAlgorithmSlotsChanged(AlgorithmSlotsChangedEvent event) {
        updateAlgorithmSlotUIs();
    }

    @Subscribe
    public void onTraitsChanged(TraitConfigurationChangedEvent event) {
        setSize(calculateWidth(), calculateHeight());
        revalidate();
        repaint();
    }

    @Subscribe
    public void onAlgorithmParametersChanged(ParameterChangedEvent event) {
        if (event.getParameterHolder() == algorithm && "name".equals(event.getKey())) {
            setSize(calculateWidth(), calculateHeight());
            nameLabel.setText(algorithm.getName());
            revalidate();
            repaint();
        }
    }

    @Override
    public void setLocation(int x, int y) {
        super.setLocation(x, y);
        algorithm.setLocationWithin(graphUI.getCompartment(), new Point(x, y));
    }

    @Override
    public void setLocation(Point p) {
        super.setLocation(p);
        algorithm.setLocationWithin(graphUI.getCompartment(), p);
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        updateBorder();
    }

    private void updateBorder() {
        if (selected) {
            setBorder(BorderFactory.createLineBorder(borderColor, 2));
        } else {
            setBorder(BorderFactory.createLineBorder(borderColor));
        }
    }
}
