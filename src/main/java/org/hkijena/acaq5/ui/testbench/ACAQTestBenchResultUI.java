package org.hkijena.acaq5.ui.testbench;

import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.ACAQRunSample;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.FormPanel;

import javax.swing.*;
import java.awt.*;

public class ACAQTestBenchResultUI extends ACAQUIPanel {
    private ACAQAlgorithm runAlgorithm;
    private ACAQRunSample runSample;
    private FormPanel formPanel;

    public ACAQTestBenchResultUI(ACAQWorkbenchUI workbenchUI, ACAQAlgorithm runAlgorithm, ACAQRunSample runSample) {
        super(workbenchUI);
        this.runAlgorithm = runAlgorithm;
        this.runSample = runSample;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        formPanel = new FormPanel(null, false, false);

        for(ACAQDataSlot<?> slot : runAlgorithm.getInputSlots()) {
            ACAQDataSlot<?> source = runSample.getRun().getGraph().getSourceSlot(slot);
            addSlotToForm("Input: " + source.getName(), source, null);
        }
        for(ACAQDataSlot<?> slot : runAlgorithm.getOutputSlots()) {
            addSlotToForm("Output: " + slot.getName(), slot, null);
        }
        formPanel.addVerticalGlue();
        add(formPanel, BorderLayout.CENTER);
    }

    private void addSlotToForm(String name, ACAQDataSlot<?> slot, String documentationPath) {
        Component ui = ACAQRegistryService.getInstance().getUIDatatypeRegistry().getUIForResultSlot(getWorkbenchUI(), runSample, slot);
        formPanel.addToForm(ui,
                new JLabel(name,
                        ACAQRegistryService.getInstance().getUIDatatypeRegistry().getIconFor(slot.getAcceptedDataType()),
                        JLabel.LEFT),
                documentationPath);
    }
}
