package org.hkijena.acaq5.ui.grapheditor.algorithmfinder;

import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.utils.TooltipUtils;

import javax.swing.*;
import java.awt.*;

public class ACAQAlgorithmFinderUI extends JPanel {
    private ACAQDataSlot<?> outputSlot;
    private ACAQAlgorithm algorithm;
    private ACAQAlgorithmGraph graph;

    public ACAQAlgorithmFinderUI(ACAQDataSlot<?> outputSlot, ACAQAlgorithmGraph graph) {
        if(!outputSlot.isOutput())
            throw new IllegalArgumentException();
        this.outputSlot = outputSlot;
        this.algorithm = outputSlot.getAlgorithm();
        this.graph = graph;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JLabel algorithmNameLabel = new JLabel(algorithm.getName(), new ColorIcon(16, 16, algorithm.getCategory().getColor(0.1f, 0.9f)), JLabel.LEFT);
        algorithmNameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(algorithm.getClass()));
        toolBar.add(algorithmNameLabel);
        toolBar.add(Box.createHorizontalStrut(5));
        JLabel slotNameLabel = new JLabel(outputSlot.getName(), ACAQRegistryService.getInstance().getUIDatatypeRegistry().getIconFor(outputSlot.getAcceptedDataType()), JLabel.LEFT);
        slotNameLabel.setToolTipText(TooltipUtils.getSlotInstanceTooltip(outputSlot, graph));
        toolBar.add(slotNameLabel);

        add(toolBar, BorderLayout.NORTH);
    }
}
