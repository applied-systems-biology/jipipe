/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.publish.conditions;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistant;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistantCondition;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistantConditionStatus;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.LicenseUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Arrays;

public class LicenseAssistantCondition extends JIPipeDesktopPublisherAssistantCondition {
    public LicenseAssistantCondition(JIPipeDesktopPublisherAssistant assistant) {
        super(assistant);
        initialize();
    }

    private void initialize() {
        JButton button = UIUtils.makeButtonTransparent(UIUtils.createButton("Choose a license", JIPipe.RESOURCES.getIcon16("actions/edit.png"), () -> {
        }));
        JPopupMenu popupMenu = UIUtils.addPopupMenuToButton(button);
        for (String license : Arrays.asList(
                "CC-BY-4.0",
                "MIT",
                "Apache-2.0",
                "GPL-3.0",
                "GPL-2.0",
                "LGPL-3.0",
                "LGPL-2.1",
                "AGPL-3.0",
                "BSD-2-Clause",
                "BSD-3-Clause",
                "MPL-2.0",
                "EPL-2.0",
                "Unlicense"
        )) {
            popupMenu.add(UIUtils.createMenuItem(license, "Set the license to " + license, JIPipe.RESOURCES.getIcon16("actions/copyright.png"), () -> {
                getProject().getMetadata().setLicense(license);
                getAssistant().updateAssistant();
            }));
        }
        popupMenu.add(UIUtils.createMenuItem("Custom ...", "Enter a custom ID", JIPipe.RESOURCES.getIcon16("actions/edit.png"), () -> {
            String newLicense = JOptionPane.showInputDialog(this, "Please enter a valid license ID:", "Set license");
            if (!StringUtils.isNullOrEmpty(newLicense)) {
                getProject().getMetadata().setLicense(newLicense);
                getAssistant().updateAssistant();
            }
        }));

        popupMenu.addSeparator();
        popupMenu.add(UIUtils.createMenuItem("Learn more ...", "Open https://choosealicense.com/", JIPipe.RESOURCES.getIcon16("actions/web-browser.png"), () -> {
            UIUtils.desktopOpenURL("https://choosealicense.com/", true);
        }));
        addButton(button);
    }

    private void saveProject() {
        getDesktopProjectWorkbench().getProjectWindow().saveProjectAs(true, true);
        getAssistant().updateAssistant();
    }

    @Override
    public JIPipeDesktopPublisherAssistantConditionStatus getStatus() {
        String currentId = StringUtils.nullToEmpty(getProject().getMetadata().getLicense()).trim();
        if (StringUtils.isNullOrEmpty(currentId)) {
            return JIPipeDesktopPublisherAssistantConditionStatus.Invalid;
        } else if (!LicenseUtils.KNOWN_SPDX_LICENSES.contains(currentId)) {
            return JIPipeDesktopPublisherAssistantConditionStatus.Warning;
        }
        return JIPipeDesktopPublisherAssistantConditionStatus.Valid;
    }

    @Override
    public String getAssistantTitle(JIPipeDesktopPublisherAssistantConditionStatus status) {
        return switch (status) {
            case Valid, Warning -> "License is set to " + getProject().getMetadata().getLicense();
            case Invalid -> "No license set";
        };
    }

    @Override
    public HTMLText getAssistantDescription(JIPipeDesktopPublisherAssistantConditionStatus status) {
        return switch (status) {
            case Valid -> new HTMLText("The project has a valid license");
            case Invalid ->
                    new HTMLText("Please setup a license for your project and the resulting RO-Crate. We recommend CC-BY-4.0.");
            case Warning ->
                    new HTMLText("The license ID '" + getProject().getMetadata().getLicense() + "' is not a known SPDX license ID. See https://spdx.org/licenses/ for a list. We recommend CC-BY-4.0.");
        };
    }
}
