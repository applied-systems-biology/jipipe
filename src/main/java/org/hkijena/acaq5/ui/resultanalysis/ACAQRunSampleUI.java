package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.ACAQRunSample;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;

import javax.swing.*;
import java.awt.*;

public class ACAQRunSampleUI extends ACAQUIPanel {
    private ACAQRunSample sample;
    private FormPanel formPanel;

    public ACAQRunSampleUI(ACAQWorkbenchUI workbenchUI, ACAQRunSample sample) {
        super(workbenchUI);
        this.sample = sample;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        formPanel = new FormPanel("documentation/result-analysis.md", false);

        // Add UIs for data slots
        for(ACAQDataSlot<?> slot : sample.getOutputData()) {
            addSlotToForm(slot.getFullName(), slot, null);
        }
        formPanel.addVerticalGlue();

        add(formPanel, BorderLayout.CENTER);
    }

    public ACAQRunSample getSample() {
        return sample;
    }

    private void addSlotToForm(String name, ACAQDataSlot<?> slot, String documentationPath) {
        Component ui = ACAQUIDatatypeRegistry.getInstance().getUIForResultSlot(getWorkbenchUI(), getSample(), slot);
        formPanel.addToForm(ui,
                new JLabel(name,
                        ACAQUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()),
                        JLabel.LEFT),
                documentationPath);
    }
}
