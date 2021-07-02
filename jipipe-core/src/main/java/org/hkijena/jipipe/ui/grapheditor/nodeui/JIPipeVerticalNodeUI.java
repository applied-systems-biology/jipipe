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

package org.hkijena.jipipe.ui.grapheditor.nodeui;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.core.nodes.JIPipeCommentNode;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.ZoomFlatIconButton;
import org.hkijena.jipipe.ui.components.ZoomIcon;
import org.hkijena.jipipe.ui.components.ZoomLabel;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphViewMode;
import org.hkijena.jipipe.ui.grapheditor.NodeHotKeyStorage;
import org.hkijena.jipipe.ui.grapheditor.actions.OpenContextMenuAction;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.NodeUIContextAction;
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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An algorithm UI for vertical display
 */
public class JIPipeVerticalNodeUI extends JIPipeNodeUI {

    private final boolean compact;
    private List<JIPipeDataSlotUI> slotUIList = new ArrayList<>();
    private BiMap<String, JIPipeDataSlotUI> inputSlotUIs = HashBiMap.create();
    private BiMap<String, JIPipeDataSlotUI> outputSlotUIs = HashBiMap.create();
    private JPanel inputSlotPanel;
    private JPanel outputSlotPanel;
    private JLabel nameLabel;
    private JButton openSettingsButton;
    private JButton addInputSlotButton;
    private JButton addOutputSlotButton;

    private String cachedGridSizeNodeName;
    private Dimension cachedGridSize;

    /**
     * Creates a new UI
     *
     * @param workbench the workbench
     * @param graphUI   The graph UI that contains this UI
     * @param algorithm The algorithm
     * @param compact   if the vertical view should be compact
     */
    public JIPipeVerticalNodeUI(JIPipeWorkbench workbench, JIPipeGraphCanvasUI graphUI, JIPipeGraphNode algorithm, boolean compact) {
        super(workbench, graphUI, algorithm, JIPipeGraphViewMode.Vertical);
        this.compact = compact;
        initialize();
        updateAlgorithmSlotUIs();
        updateActivationStatus();
        updateHotkeyInfo();
        if (getNode() instanceof JIPipeCommentNode) {
            updateCommentNodeDesign();
        }
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

        // Create open settings button
        openSettingsButton = new ZoomFlatIconButton(UIUtils.getIconFromResources("actions/wrench.png"), getGraphUI());
        openSettingsButton.setBorder(null);
        openSettingsButton.addActionListener(e -> {
            getGraphUI().selectOnly(this);
            getEventBus().post(new JIPipeGraphCanvasUI.NodeUIActionRequestedEvent(this, new OpenContextMenuAction()));
        });

        // Create run button
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
                gridy = 0;
                gridwidth = 3;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
            }
        });
        addVerticalGlue(1);
        if (getGraphUI().getSettings().isShowRunNodeButton() && isNodeRunnable()) {
            add(runButton, new GridBagConstraints() {
                {
                    gridx = row.getAndIncrement();
                    gridy = 2;
                    anchor = GridBagConstraints.EAST;
                }
            });
        }
        add(openSettingsButton, new GridBagConstraints() {
            {
                gridx = row.getAndIncrement();
                gridy = 2;
                anchor = GridBagConstraints.EAST;
            }
        });
        add(nameLabel, new GridBagConstraints() {
            {
                gridy = 2;
                gridx = row.getAndIncrement();
                fill = GridBagConstraints.HORIZONTAL;
                insets = new Insets(0, 4, 0, 0);
                weightx = 1;
            }
        });
        addVerticalGlue(3);
        add(outputSlotPanel, new GridBagConstraints() {
            {
                gridy = 4;
                gridwidth = 3;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
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

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        g.setColor(getBorderColor());
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
    }

    @Override
    public boolean needsRecalculateGridSize() {
        if(cachedGridSize == null)
            return true;
        if(!Objects.equals(cachedGridSizeNodeName, getNode().getName()))
            return true;
        for (JIPipeDataSlotUI ui : slotUIList) {
            if(ui.needsRecalculateGridSize())
                return true;
        }
        return false;
    }

    @Override
    public Dimension calculateGridSize() {
        if(needsRecalculateGridSize()) {
            JIPipeGraphViewMode graphViewMode = compact ? JIPipeGraphViewMode.VerticalCompact : JIPipeGraphViewMode.Vertical;
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
                    Dimension realSize = graphViewMode.gridToRealSize(ui.calculateGridSize(), 1.0);
                    maxWidth = Math.max(maxWidth, realSize.width);
                }

                width = Math.max(width, maxWidth * Math.max(getDisplayedInputColumns(), getDisplayedOutputColumns()));
            }
            width += 100;
            Point inGrid = graphViewMode.realLocationToGrid(new Point((int) width, graphViewMode.getGridHeight() * 3), 1.0);
            cachedGridSize = new Dimension(inGrid.x, 3);
            cachedGridSizeNodeName = getNode().getName();
        }
        return cachedGridSize;
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
        addInputSlotButton = null;
        addOutputSlotButton = null;
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
                JIPipeDataSlotUI ui = new JIPipeVerticalDataSlotUI(getWorkbench(), this, getGraphUI().getCompartment(), slot, compact);
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
                JIPipeDataSlotUI ui = new JIPipeVerticalDataSlotUI(getWorkbench(), this, getGraphUI().getCompartment(), slot, compact);
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
            addInputSlotButton = createAddSlotButton(JIPipeSlotType.Input);
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, rightBorder, getBorderColor()));
            panel.add(addInputSlotButton, BorderLayout.CENTER);
            inputSlotPanel.add(panel);
        }
        if (createAddOutputSlotButton) {
            int rightBorder = 0;
            if (createdOutputSlots < displayedOutputColumns - 1)
                rightBorder = 1;
            addOutputSlotButton = createAddSlotButton(JIPipeSlotType.Output);
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, rightBorder, getBorderColor()));
            panel.add(addOutputSlotButton, BorderLayout.CENTER);
            outputSlotPanel.add(panel);
        }

        updateSize();
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
                    nameLabel.setForeground(UIManager.getColor("Label.foreground"));
                    openSettingsButton.setIcon(UIUtils.getIconFromResources("actions/wrench.png"));
                } else {
//                    setBackground(UIManager.getColor("TextArea.background"));
                    nameLabel.setForeground(UIManager.getColor("Label.foreground"));
                    openSettingsButton.setIcon(UIUtils.getIconFromResources("emblems/pass-through.png"));
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
        JIPipeGraphViewMode viewMode = compact ? JIPipeGraphViewMode.VerticalCompact : JIPipeGraphViewMode.Vertical;
        Dimension realSize = new Dimension((int) Math.round(gridSize.width * viewMode.getGridWidth() * getGraphUI().getZoom()),
                (int) Math.round(gridSize.height * viewMode.getGridHeight() * getGraphUI().getZoom()));
        Dimension slotSize = viewMode.gridToRealSize(new Dimension(1, 1), getGraphUI().getZoom());
        if (compact) {
            slotSize.height = 24;
        }
        slotSize.width = realSize.width;
        if (inputSlotPanel.getComponentCount() > 0) {
            inputSlotPanel.setMinimumSize(slotSize);
            inputSlotPanel.setMaximumSize(slotSize);
            inputSlotPanel.setPreferredSize(slotSize);
        } else {
            inputSlotPanel.setMinimumSize(new Dimension());
            inputSlotPanel.setMaximumSize(new Dimension());
            inputSlotPanel.setMaximumSize(new Dimension());
        }
        if (outputSlotPanel.getComponentCount() > 0) {
            outputSlotPanel.setMinimumSize(slotSize);
            outputSlotPanel.setMaximumSize(slotSize);
            outputSlotPanel.setPreferredSize(slotSize);
        } else {
            outputSlotPanel.setMinimumSize(new Dimension());
            outputSlotPanel.setMaximumSize(new Dimension());
            outputSlotPanel.setMaximumSize(new Dimension());
        }
        if (!Objects.equals(getSize(), realSize)) {
            setSize(realSize);
            revalidate();
            getGraphUI().repaint();
        }
    }

    @Override
    public PointRange getSlotLocation(JIPipeDataSlot slot) {
        JIPipeGraphViewMode graphViewMode = compact ? JIPipeGraphViewMode.VerticalCompact : JIPipeGraphViewMode.Vertical;
        Dimension unzoomedSize = graphViewMode.gridToRealSize(calculateGridSize(), 1.0);
        if (slot.isInput()) {
            int nColumns = getDisplayedInputColumns();
            int columnWidth = unzoomedSize.width / nColumns;
            int minX = getNode().getInputSlots().indexOf(slot) * columnWidth;
            return new PointRange(new Point(minX + columnWidth / 2, 3),
                    new Point(minX + 20, 3),
                    new Point(minX + columnWidth - 20, 3)).zoom(getGraphUI().getZoom());
        } else if (slot.isOutput()) {
            int nColumns = getDisplayedOutputColumns();
            int columnWidth = unzoomedSize.width / nColumns;
            int minX = getNode().getOutputSlots().indexOf(slot) * columnWidth;
            return new PointRange(new Point(minX + columnWidth / 2, unzoomedSize.height - 3),
                    new Point(minX + 20, unzoomedSize.height - 3),
                    new Point(minX + columnWidth - 20, unzoomedSize.height - 3)).zoom(getGraphUI().getZoom());
        } else {
            throw new UnsupportedOperationException("Unknown slot type!");
        }
    }

    @Override
    public void onAlgorithmParametersChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        super.onAlgorithmParametersChanged(event);
        if (event.getSource() == getNode() && getNode() instanceof JIPipeCommentNode) {
            updateCommentNodeDesign();
        }
    }

    private void updateCommentNodeDesign() {
        JIPipeCommentNode commentNode = (JIPipeCommentNode) getNode();
        setBackground(commentNode.getBackgroundColor());
        nameLabel.setForeground(commentNode.getTextColor());
        nameLabel.setIcon(UIUtils.getIconFromResources(commentNode.getIcon().getIconName()));
    }

    @Override
    public Map<String, JIPipeDataSlotUI> getInputSlotUIs() {
        return Collections.unmodifiableMap(inputSlotUIs);
    }

    @Override
    public Map<String, JIPipeDataSlotUI> getOutputSlotUIs() {
        return Collections.unmodifiableMap(outputSlotUIs);
    }

    @Override
    public void refreshSlots() {
        for (JIPipeDataSlotUI ui : slotUIList) {
            ui.reloadButtonStatus();
        }
    }

    public boolean isCompact() {
        return compact;
    }
}