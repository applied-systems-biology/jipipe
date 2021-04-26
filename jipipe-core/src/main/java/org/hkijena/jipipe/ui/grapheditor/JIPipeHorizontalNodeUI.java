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
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.ZoomFlatIconButton;
import org.hkijena.jipipe.ui.components.ZoomIcon;
import org.hkijena.jipipe.ui.components.ZoomLabel;
import org.hkijena.jipipe.ui.grapheditor.actions.OpenContextMenuAction;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.utils.PointRange;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
    private JButton addInputSlotButton;
    private JButton addOutputSlotButton;

    /**
     * Creates a new UI
     *
     * @param workbench the workbench
     * @param graphUI   The graph UI that contains this UI
     * @param algorithm The algorithm
     */
    public JIPipeHorizontalNodeUI(JIPipeWorkbench workbench, JIPipeGraphCanvasUI graphUI, JIPipeGraphNode algorithm) {
        super(workbench, graphUI, algorithm, JIPipeGraphViewMode.Horizontal);
        initialize();
        updateAlgorithmSlotUIs();
        updateActivationStatus();
        updateHotkeyInfo();
    }

    private void initialize() {
        setBackground(getFillColor());
        setBorder(BorderFactory.createLineBorder(getBorderColor()));
        setLayout(new GridBagLayout());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        inputSlotPanel = new JPanel();
        inputSlotPanel.setOpaque(false);
        outputSlotPanel = new JPanel();
        outputSlotPanel.setOpaque(false);

        nameLabel = new ZoomLabel(getNode().getName(), null, getGraphUI());
        nameLabel.setIcon(new ZoomIcon(JIPipe.getNodes().getIconFor(getNode().getInfo()), getGraphUI()));

        openSettingsButton = new ZoomFlatIconButton(UIUtils.getIconFromResources("actions/wrench.png"), getGraphUI());
        openSettingsButton.setBorder(null);
        openSettingsButton.addActionListener(e -> {
            getGraphUI().selectOnly(this);
            getEventBus().post(new JIPipeGraphCanvasUI.NodeUIActionRequestedEvent(this, new OpenContextMenuAction()));
        });

        JButton runButton = new ZoomFlatIconButton(UIUtils.getIconFromResources("actions/run-play.png"), getGraphUI());
        runButton.setBorder(null);
        JPopupMenu runContextMenu = UIUtils.addPopupMenuToComponent(runButton);
        for (NodeUIContextAction entry : RUN_NODE_CONTEXT_MENU_ENTRIES) {
            if (entry == null)
                runContextMenu.addSeparator();
            else {
                JMenuItem item = new JMenuItem(entry.getName(), entry.getIcon());
                item.setToolTipText(entry.getDescription());
                item.setAccelerator(entry.getKeyboardShortcut());
                item.addActionListener(e -> {
                    if (entry.matches(Collections.singleton(this))) {
                        entry.run(getGraphUI(), Collections.singleton(this));
                    } else {
                        JOptionPane.showMessageDialog(getWorkbench().getWindow(),
                                "Could not run this operation",
                                entry.getName(),
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
                runContextMenu.add(item);
            }
        }

        final AtomicInteger row = new AtomicInteger(0);
        add(inputSlotPanel, new GridBagConstraints() {
            {
                gridx = row.getAndIncrement();
                fill = GridBagConstraints.VERTICAL;
                weighty = 1;
            }
        });
        addHorizontalGlue(row.getAndIncrement());
        if (getGraphUI().getSettings().isShowRunNodeButton() && isNodeRunnable()) {
            add(runButton, new GridBagConstraints() {
                {
                    gridx = row.getAndIncrement();
                }
            });
        }
        add(openSettingsButton, new GridBagConstraints() {
            {
                gridx = row.getAndIncrement();
            }
        });
        add(nameLabel, new GridBagConstraints() {
            {
                gridx = row.getAndIncrement();
                insets = new Insets(0, 4, 0, 0);
            }
        });
        addHorizontalGlue(row.getAndIncrement());
        add(outputSlotPanel, new GridBagConstraints() {
            {
                gridx = row.getAndIncrement();
                fill = GridBagConstraints.VERTICAL;
                weighty = 1;
            }
        });
    }

    @Override
    public void updateHotkeyInfo() {
        NodeHotKeyStorage.Hotkey hotkey = getGraphUI().getNodeHotKeyStorage().getHotkeyFor(getGraphUI().getCompartment(), getNode().getUUIDInGraph());
        openSettingsButton.setVisible(hotkey != NodeHotKeyStorage.Hotkey.None || getGraphUI().getSettings().isShowSettingsNodeButton());
        switch (hotkey) {
            case None:
                openSettingsButton.setIcon(UIUtils.getIconFromResources("actions/wrench.png"));
                break;
            case Slot0:
                openSettingsButton.setIcon(UIUtils.getIconFromResources("actions/0.png"));
                break;
            case Slot1:
                openSettingsButton.setIcon(UIUtils.getIconFromResources("actions/1.png"));
                break;
            case Slot2:
                openSettingsButton.setIcon(UIUtils.getIconFromResources("actions/2.png"));
                break;
            case Slot3:
                openSettingsButton.setIcon(UIUtils.getIconFromResources("actions/3.png"));
                break;
            case Slot4:
                openSettingsButton.setIcon(UIUtils.getIconFromResources("actions/4.png"));
                break;
            case Slot5:
                openSettingsButton.setIcon(UIUtils.getIconFromResources("actions/5.png"));
                break;
            case Slot6:
                openSettingsButton.setIcon(UIUtils.getIconFromResources("actions/6.png"));
                break;
            case Slot7:
                openSettingsButton.setIcon(UIUtils.getIconFromResources("actions/7.png"));
                break;
            case Slot8:
                openSettingsButton.setIcon(UIUtils.getIconFromResources("actions/8.png"));
                break;
            case Slot9:
                openSettingsButton.setIcon(UIUtils.getIconFromResources("actions/9.png"));
                break;
        }
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

    @Override
    public Dimension calculateGridSize() {
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
                Dimension realSize = JIPipeGraphViewMode.Horizontal.gridToRealSize(ui.calculateGridSize(), 1.0);
                if (ui.getSlot().isInput()) {
                    maxInputSlotWidth = Math.max(maxInputSlotWidth, realSize.width);
                } else if (ui.getSlot().isOutput()) {
                    maxOutputSlotWidth = Math.max(maxOutputSlotWidth, realSize.width);
                }
            }
            if (addInputSlotButton != null) {
                maxInputSlotWidth = Math.max(maxInputSlotWidth, JIPipeGraphViewMode.Horizontal.gridToRealSize(new Dimension(4, 1), 1.0).width);
            }
            if (addOutputSlotButton != null) {
                maxOutputSlotWidth = Math.max(maxOutputSlotWidth, JIPipeGraphViewMode.Horizontal.gridToRealSize(new Dimension(4, 1), 1.0).width);
            }

            width += maxInputSlotWidth + maxOutputSlotWidth;
        }
        width += 32;
        int height = Math.max(JIPipeGraphViewMode.Horizontal.getGridHeight(),
                JIPipeGraphViewMode.Horizontal.getGridHeight() * getDisplayedRows());
        Point inGrid = JIPipeGraphViewMode.Horizontal.realLocationToGrid(new Point((int) width, height), 1.0);
        return new Dimension(inGrid.x, inGrid.y);
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
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(getBorderColor());
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
    }

    @Override
    public void updateAlgorithmSlotUIs() {
        slotUIList.clear();
        inputSlotPanel.removeAll();
        outputSlotPanel.removeAll();
        inputSlotPanel.setLayout(new GridLayout(getDisplayedRows(), 1));
        outputSlotPanel.setLayout(new GridLayout(getDisplayedRows(), 1));
        addInputSlotButton = null;
        addOutputSlotButton = null;

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
            if (Objects.equals(getNode().getCompartmentUUIDInGraph(), getGraphUI().getCompartment())) {
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
            addInputSlotButton = createAddSlotButton(JIPipeSlotType.Input);
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
            addOutputSlotButton = createAddSlotButton(JIPipeSlotType.Output);
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, bottomBorder, 0, getBorderColor()),
                    BorderFactory.createEmptyBorder(0, 4, 0, 0)));
            panel.add(addOutputSlotButton, BorderLayout.EAST);
            outputSlotPanel.add(panel);
        }

        updateSize();
        revalidate();
        repaint();
    }

    @Override
    public PointRange getSlotLocation(JIPipeDataSlot slot) {
        Dimension unzoomedSize = JIPipeGraphViewMode.Vertical.gridToRealSize(calculateGridSize(), 1.0);
        if (slot.isInput()) {
            return new PointRange(0, getNode().getInputSlots().indexOf(slot) * JIPipeGraphViewMode.Horizontal.getGridHeight() +
                    JIPipeGraphViewMode.Horizontal.getGridHeight() / 2).zoom(getGraphUI().getZoom());
        } else if (slot.isOutput()) {
            return new PointRange(unzoomedSize.width, getNode().getOutputSlots().indexOf(slot) * JIPipeGraphViewMode.Horizontal.getGridHeight() +
                    JIPipeGraphViewMode.Horizontal.getGridHeight() / 2).zoom(getGraphUI().getZoom());
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
//                    setBackground(getFillColor());
                    nameLabel.setForeground(UIManager.getColor("Label.foreground"));
                    openSettingsButton.setIcon(UIUtils.getIconFromResources("actions/wrench.png"));
                } else {
//                    setBackground(Color.WHITE);
                    nameLabel.setForeground(UIManager.getColor("Label.foreground"));
                    openSettingsButton.setIcon(UIUtils.getIconFromResources("emblems/pass-through-h.png"));
                }
            } else {
//                setBackground(new Color(227, 86, 86));
                nameLabel.setForeground(Color.WHITE);
                openSettingsButton.setIcon(UIUtils.getIconFromResources("emblems/block.png"));
            }
        } else {
//            setBackground(getFillColor());
            nameLabel.setForeground(UIManager.getColor("Label.foreground"));
            openSettingsButton.setIcon(UIUtils.getIconFromResources("actions/wrench.png"));
        }
        repaint();
    }

    @Override
    public void updateSize() {
        Dimension gridSize = calculateGridSize();
        Dimension realSize = new Dimension((int) Math.round(gridSize.width * JIPipeGraphViewMode.Horizontal.getGridWidth() * getGraphUI().getZoom()),
                (int) Math.round(gridSize.height * JIPipeGraphViewMode.Horizontal.getGridHeight() * getGraphUI().getZoom()));
        Dimension inputRealSize = JIPipeGraphViewMode.Horizontal.gridToRealSize(new Dimension(1, 1), getGraphUI().getZoom());
        Dimension outputRealSize = JIPipeGraphViewMode.Horizontal.gridToRealSize(new Dimension(1, 1), getGraphUI().getZoom());
        for (JIPipeDataSlotUI ui : slotUIList) {
            Dimension slotGridSize = ui.calculateGridSize();
            Dimension slotRealSize = new Dimension((int) Math.round(slotGridSize.width * JIPipeGraphViewMode.Horizontal.getGridWidth() * getGraphUI().getZoom()),
                    (int) Math.round(slotGridSize.height * JIPipeGraphViewMode.Horizontal.getGridHeight() * getGraphUI().getZoom()));
            if (ui.getSlot().isInput()) {
                inputRealSize.width = Math.max(inputRealSize.width, slotRealSize.width);
            } else {
                outputRealSize.width = Math.max(inputRealSize.width, slotRealSize.width);
            }
        }
        inputSlotPanel.setSize(inputRealSize);
        outputSlotPanel.setSize(outputRealSize);

        if (!Objects.equals(getSize(), realSize)) {
            setSize(realSize);
            getGraphUI().repaint();
        }
    }

    @Override
    public void refreshSlots() {
        for (JIPipeDataSlotUI ui : slotUIList) {
            ui.reloadButtonStatus();
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
