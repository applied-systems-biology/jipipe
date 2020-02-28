package org.hkijena.acaq5.ui.testbench;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.FormPanel;

public class ACAQTestBenchResultUI extends ACAQUIPanel {
    private ACAQAlgorithm runAlgorithm;
    private FormPanel formPanel;

    public ACAQTestBenchResultUI(ACAQWorkbenchUI workbenchUI, ACAQAlgorithm runAlgorithm) {
        super(workbenchUI);
        this.runAlgorithm = runAlgorithm;
        initialize();
    }

    private void initialize() {
//        setLayout(new BorderLayout());
//        formPanel = new FormPanel(null, false, false);
//
//        for(ACAQDataSlot slot : runAlgorithm.getInputSlots()) {
//            ACAQDataSlot source = runSample.getRun().getGraph().getSourceSlot(slot);
//            addSlotToForm("Input: " + source.getName(), source, null);
//        }
//        for(ACAQDataSlot slot : runAlgorithm.getOutputSlots()) {
//            addSlotToForm("Output: " + slot.getName(), slot, null);
//        }
//        formPanel.addVerticalGlue();
//        add(formPanel, BorderLayout.CENTER);
    }

//    private void addSlotToForm(String name, ACAQDataSlot slot, String documentationPath) {
//        Component ui = ACAQUIDatatypeRegistry.getInstance().getUIForResultSlot(getWorkbenchUI(), runSample, slot);
//        formPanel.addToForm(ui,
//                new JLabel(name,
//                        ACAQUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()),
//                        JLabel.LEFT),
//                documentationPath);
//    }
}
