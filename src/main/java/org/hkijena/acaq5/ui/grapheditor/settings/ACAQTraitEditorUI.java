package org.hkijena.acaq5.ui.grapheditor.settings;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.traits.ACAQMutableTraitGenerator;
import org.hkijena.acaq5.ui.components.MarkdownReader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;

/**
 * An extended trait editor, as an alternative to the in-node editor
 * Also supports just viewing the traits
 */
public class ACAQTraitEditorUI extends JPanel {
    private ACAQAlgorithm algorithm;
    private ACAQAlgorithmGraph graph;
    private JComboBox<ACAQDataSlot> slotSelection;
    private MarkdownReader helpPanel;
    private JSplitPane splitPane;

    public ACAQTraitEditorUI(ACAQAlgorithm algorithm, ACAQAlgorithmGraph graph) {
        this.algorithm = algorithm;
        this.graph = graph;
        initialize();
        reloadList();
        algorithm.getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        slotSelection = new JComboBox<>();
        slotSelection.setRenderer(new ACAQDataSlotListCellRenderer());
        slotSelection.addActionListener(e -> updateEditor());
        toolBar.add(slotSelection);
        add(toolBar, BorderLayout.NORTH);

        helpPanel = new MarkdownReader(false);
        helpPanel.loadDefaultDocument("documentation/algorithm-traits.md");

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JPanel(), helpPanel);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.33);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.66);
            }
        });
        add(splitPane, BorderLayout.CENTER);
    }

    public void reloadList() {
        ACAQDataSlot selectedSlot = slotSelection.getSelectedItem() != null ? (ACAQDataSlot)slotSelection.getSelectedItem() : null;
        ArrayList<ACAQDataSlot> slots = new ArrayList<>();
        slots.addAll(algorithm.getInputSlots());
        slots.addAll(algorithm.getOutputSlots());
        DefaultComboBoxModel<ACAQDataSlot> model = new DefaultComboBoxModel<ACAQDataSlot>(slots.toArray(new ACAQDataSlot[0]));
        if(algorithm.getSlots().containsValue(selectedSlot)) {
            model.setSelectedItem(selectedSlot);
        }
        slotSelection.setModel(model);
        updateEditor();
    }

    public void updateEditor() {
        if(slotSelection.getSelectedItem() != null) {
            ACAQDataSlot selectedSlot = (ACAQDataSlot) slotSelection.getSelectedItem();
            if(algorithm.getTraitConfiguration() instanceof ACAQMutableTraitGenerator)
                splitPane.setLeftComponent(new ACAQTraitGeneratorUI(selectedSlot.getName(), (ACAQMutableTraitGenerator) algorithm.getTraitConfiguration(), graph));
            else
                splitPane.setLeftComponent(new ACAQTraitViewerUI(selectedSlot, graph));
            revalidate();
            repaint();
        }
    }

    @Subscribe
    public void onAlgorithmSlotsChanged(AlgorithmSlotsChangedEvent event) {
        reloadList();
    }
}
