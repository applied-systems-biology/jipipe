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
 *
 */

package org.hkijena.jipipe.ui.extensions.legacy;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

/**
 * Button that checks the plugin validity and shows the report if clicked
 */
public class JIPipePluginValidityCheckerButton extends JButton {

    private JIPipeIssueReport report = new JIPipeIssueReport();

    /**
     * Creates new instance
     */
    public JIPipePluginValidityCheckerButton() {
        recheckValidity();
        JIPipe.getInstance().getEventBus().register(this);
    }

    /**
     * Triggers a validity check
     */
    public void recheckValidity() {
        report.clearAll();
        JIPipe.getInstance().reportValidity(report);

        if (report.isValid()) {
            setText("All plugins valid");
            setIcon(UIUtils.getIconFromResources("emblems/vcs-normal.png"));
        } else {
            setText("Some plugins could not be loaded");
            setIcon(UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
        }
    }

    /**
     * Triggered when an extension is registered.
     * Rechecks validity.
     *
     * @param event Generated event
     */
    @Subscribe
    public void onExtensionRegistered(JIPipe.ExtensionRegisteredEvent event) {
        recheckValidity();
    }
}
