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

package org.hkijena.acaq5.ui.extensions;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.events.ExtensionRegisteredEvent;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;

/**
 * Button that checks the plugin validity and shows the report if clicked
 */
public class ACAQPluginValidityCheckerButton extends JButton {

    private ACAQValidityReport report = new ACAQValidityReport();

    /**
     * Creates new instance
     */
    public ACAQPluginValidityCheckerButton() {
        recheckValidity();
        ACAQDefaultRegistry.getInstance().getEventBus().register(this);
    }

    /**
     * Triggers a validity check
     */
    public void recheckValidity() {
        report.clear();
        ACAQDefaultRegistry.getInstance().reportValidity(report);

        if (report.isValid()) {
            setText("All plugins valid");
            setIcon(UIUtils.getIconFromResources("check-circle-green.png"));
        } else {
            setText("Some plugins could not be loaded");
            setIcon(UIUtils.getIconFromResources("error.png"));
        }
    }

    /**
     * Triggered when an extension is registered.
     * Rechecks validity.
     *
     * @param event Generated event
     */
    @Subscribe
    public void onExtensionRegistered(ExtensionRegisteredEvent event) {
        recheckValidity();
    }
}
