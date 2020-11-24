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

package org.hkijena.jipipe.ui.components;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeRun;
import org.hkijena.jipipe.api.JIPipeRunSettings;
import org.hkijena.jipipe.api.events.GraphChangedEvent;
import org.hkijena.jipipe.api.events.ParameterChangedEvent;
import org.hkijena.jipipe.api.events.SlotsChangedEvent;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.extensions.settings.VirtualDataSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.jipipe.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.jipipe.ui.events.RunUIWorkerStartedEvent;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;

/**
 * A tool that automatically runs 'update cache' when any parameter or graph property is changed
 */
public class VirtualDataControl extends JIPipeProjectWorkbenchPanel {

    private final VirtualDataSettings virtualDataSettings = VirtualDataSettings.getInstance();

    /**
     * @param workbenchUI The workbench UI
     */
    public VirtualDataControl(JIPipeProjectWorkbench workbenchUI) {
        super(workbenchUI);
        getProject().getGraph().getEventBus().register(this);
    }

    public JToggleButton createToggleButton() {
        JToggleButton button = new JToggleButton("Reduce memory", UIUtils.getIconFromResources("actions/rabbitvcs-drive.png"));
        button.setSelected(virtualDataSettings.isVirtualMode());
        button.setToolTipText("Enable/disable virtual mode. If enabled, any output indicated by the HDD icon is stored on the hard drive to reduce memory consumption. Increases the run time significantly.");
        button.addActionListener(e -> {
            if (button.isSelected() != virtualDataSettings.isVirtualMode()) {
                virtualDataSettings.setVirtualMode(button.isSelected());
                virtualDataSettings.getEventBus().post(new ParameterChangedEvent(virtualDataSettings, "virtual-mode"));
            }
        });
        return button;
    }
}
