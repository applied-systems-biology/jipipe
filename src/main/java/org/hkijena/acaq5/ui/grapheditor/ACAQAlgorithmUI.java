package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.ACAQAlgorithm;
import org.hkijena.acaq5.api.ACAQData;
import org.hkijena.acaq5.api.ACAQDataSlot;
import org.hkijena.acaq5.api.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.ui.events.ACAQAlgorithmUIOpenSettingsRequested;
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

    public ACAQAlgorithmUI(ACAQAlgorithmGraphCanvasUI graphUI, ACAQAlgorithm algorithm) {
        this.graphUI = graphUI;
        this.algorithm = algorithm;
        this.algorithm.getEventBus().register(this);
        initialize();
        updateAlgorithmUI();
    }

    private void initialize() {
        setBackground(getAlgorithmColor());
        setBorder(BorderFactory.createLineBorder(getAlgorithmBorderColor()));
        setSize(new Dimension(calculateWidth(), calculateHeight()));
        setLayout(new GridBagLayout());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        inputSlotPanel = new JPanel();
        inputSlotPanel.setOpaque(false);
        outputSlotPanel = new JPanel();
        outputSlotPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(ACAQAlgorithm.getName(algorithm.getClass()));
        JButton openSettingsButton = new JButton(UIUtils.getIconFromResources("wrench.png"));
        UIUtils.makeFlat(openSettingsButton);
        openSettingsButton.setPreferredSize(new Dimension(21,21));
        openSettingsButton.addActionListener(e -> eventBus.post(new ACAQAlgorithmUIOpenSettingsRequested(this)));

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
        ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration)algorithm.getSlotConfiguration();

        Set<Class<? extends ACAQDataSlot<?>>> allowedSlotTypes;
        switch(slotType) {
            case Input:
                allowedSlotTypes = slotConfiguration.getAllowedInputSlotTypes();
                break;
            case Output:
                allowedSlotTypes = slotConfiguration.getAllowedOutputSlotTypes();
                break;
            default:
                throw new RuntimeException();
        }

        for(Class<? extends ACAQDataSlot<?>> slotClass : allowedSlotTypes) {
            Class<? extends ACAQData> dataClass = ACAQRegistryService.getInstance().getDatatypeRegistry().getRegisteredSlotDataTypes().inverse().get(slotClass);
            JMenuItem item = new JMenuItem(ACAQData.getName(dataClass), ACAQRegistryService.getInstance().getUIDatatypeRegistry().getIconFor(dataClass));
            item.addActionListener(e -> addNewSlot(slotType, slotClass));
            menu.add(item);
        }

        return button;
    }

    public void updateAlgorithmUI() {
        slotUIList.clear();
        inputSlotPanel.removeAll();
        outputSlotPanel.removeAll();
        inputSlotPanel.setLayout(new GridLayout(getDisplayedRows(), 1));
        outputSlotPanel.setLayout(new GridLayout(getDisplayedRows(), 1));
        if(algorithm.getInputSlots().size() > 0) {
            for(ACAQDataSlot<?> slot : algorithm.getInputSlots().values()) {
                ACAQDataSlotUI ui = new ACAQDataSlotUI(slot);
                ui.setOpaque(false);
                slotUIList.add(ui);
                inputSlotPanel.add(ui);
            }
        }
        if(algorithm.getOutputSlots().size() > 0) {
            for(ACAQDataSlot<?> slot : algorithm.getOutputSlots().values()) {
                ACAQDataSlotUI ui = new ACAQDataSlotUI(slot);
                ui.setOpaque(false);
                slotUIList.add(ui);
                outputSlotPanel.add(ui);
            }
        }
        if(algorithm.getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration)algorithm.getSlotConfiguration();
            if(slotConfiguration.allowsInputSlots() && !slotConfiguration.isInputSlotsSealed()) {
                JButton addInputSlotButton = createAddSlotButton(ACAQDataSlot.SlotType.Input);
                JPanel panel = new JPanel(new BorderLayout());
                panel.setOpaque(false);
                panel.add(addInputSlotButton, BorderLayout.WEST);
                inputSlotPanel.add(panel);
            }
            if(slotConfiguration.allowsOutputSlots() && !slotConfiguration.isOutputSlotsSealed()) {
                JButton addOutputSlotButton = createAddSlotButton(ACAQDataSlot.SlotType.Output);
                JPanel panel = new JPanel(new BorderLayout());
                panel.setOpaque(false);
                panel.add(addOutputSlotButton, BorderLayout.EAST);
                outputSlotPanel.add(panel);
            }
        }
        setSize(new Dimension(calculateWidth(), calculateHeight()));
        revalidate();
        repaint();
    }

    private void addNewSlot(ACAQDataSlot.SlotType slotType, Class<? extends ACAQDataSlot<?>> klass) {
        if(algorithm.getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration();

            int existingSlots = slotType == ACAQDataSlot.SlotType.Input ? algorithm.getInputSlots().size() : algorithm.getOutputSlots().size();

            String name = null;
            while(name == null) {
                String newName = JOptionPane.showInputDialog(this,"Please a data slot name", slotType + " data " + (existingSlots + 1));
                if(newName == null || newName.trim().isEmpty())
                    return;
                if(slotConfiguration.hasSlot(newName))
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
     * Returns the number of rows that contain the slots
     * @return
     */
    private int getSlotRows() {
        return Math.max(algorithm.getInputSlots().size(), algorithm.getOutputSlots().size());
    }

    /**
     * Contains the number of displayed rows. This includes the number of slot rows, and optionally additional rows for adding
     * @return
     */
    private int getDisplayedRows() {
        int rows = getSlotRows();
        if(algorithm.getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            ACAQMutableSlotConfiguration configuration = (ACAQMutableSlotConfiguration)algorithm.getSlotConfiguration();
            if(configuration.canAddInputSlot() && algorithm.getInputSlots().size() > 0 ||
                    configuration.canAddOutputSlot() && algorithm.getOutputSlots().size() > 0) {
                rows += 1;
            }
        }
        return rows;
    }

    private int calculateHeight() {
        return Math.max(SLOT_UI_HEIGHT, SLOT_UI_HEIGHT * getDisplayedRows());
    }

    private int calculateWidth() {
        FontRenderContext frc = new FontRenderContext(null, false, false);
        double width = 0;

        // Measure width of center
        {
            TextLayout layout = new TextLayout(ACAQAlgorithm.getName(algorithm.getClass()), getFont(), frc);
            width += layout.getBounds().getWidth();
        }

        // Measure slot widths
        {
            double maxInputSlotWidth = 0;
            double maxOutputSlotWidth = 0;
            for(ACAQDataSlotUI ui : slotUIList) {
                if(ui.getSlot().isInput()) {
                    maxInputSlotWidth = Math.max(maxInputSlotWidth, ui.calculateWidth());
                }
                else if(ui.getSlot().isOutput()) {
                    maxOutputSlotWidth = Math.max(maxOutputSlotWidth, ui.calculateWidth());
                }
            }

            width += maxInputSlotWidth + maxOutputSlotWidth;
        }

        return (int)Math.ceil(width * 1.0 / SLOT_UI_WIDTH) * SLOT_UI_WIDTH + 150;
    }

    /**
     * Gets a hashed color value for the algorithm.
     * The color is the same for all algorithms of the same type
     * @return
     */
    public Color getAlgorithmColor() {
        return ACAQAlgorithm.getCategory(algorithm.getClass()).getColor(0.1f, 0.9f);
    }

    public Color getAlgorithmBorderColor() {
        return ACAQAlgorithm.getCategory(algorithm.getClass()).getColor(0.1f, 0.5f);
    }

    /**
     * Tries to move the node to the provided location
     * A grid is applied to the input coordinates
     * @param x
     * @param y
     * @return
     */
    public boolean trySetLocationInGrid(int x, int y) {
        y = (int)Math.rint(y * 1.0 / ACAQAlgorithmUI.SLOT_UI_HEIGHT) * ACAQAlgorithmUI.SLOT_UI_HEIGHT;
        x = (int)Math.rint(x * 1.0 / ACAQAlgorithmUI.SLOT_UI_WIDTH) * ACAQAlgorithmUI.SLOT_UI_WIDTH;
       return trySetLocationNoGrid(x, y);
    }

    /**
     * Tries to move the node to the provided location
     * A grid is applied to the input coordinates
     * @param x
     * @param y
     * @return
     */
    public boolean trySetLocationNoGrid(int x, int y) {
        // Check for collisions
        Rectangle futureBounds = new Rectangle(x, y, getWidth(), getHeight());
        for(int i = 0; i < graphUI.getComponentCount(); ++i) {
            Component component = graphUI.getComponent(i);
            if(component instanceof ACAQAlgorithmUI) {
                ACAQAlgorithmUI ui = (ACAQAlgorithmUI)component;
                if(ui != this) {
                    if(ui.getBounds().intersects(futureBounds)) {
                        return false;
                    }
                }
            }
        }

        setLocation(x, y);
        return true;
    }

    /**
     * Get the Y location of the bottom part
     * @return
     */
    public int getBottomY() {
        return getY() + getHeight();
    }

    @Subscribe
    public void onAlgorithmSlotsChanged(AlgorithmSlotsChangedEvent event) {
        updateAlgorithmUI();
    }

    @Override
    public void setLocation(int x, int y) {
        super.setLocation(x, y);
        algorithm.setLocation(new Point(x, y));
    }

    @Override
    public void setLocation(Point p) {
        super.setLocation(p);
        algorithm.setLocation(p);
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}
