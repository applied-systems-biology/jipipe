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
import org.hkijena.jipipe.utils.UIUtils;

public class SavedProjectAssistantCondition extends JIPipeDesktopPublisherAssistantCondition {
    public SavedProjectAssistantCondition(JIPipeDesktopPublisherAssistant assistant) {
        super(assistant);
        addButton(UIUtils.createButton("Save now", JIPipe.RESOURCES.getIcon16("actions/filesave.png"), this::saveProject));
    }

    private void saveProject() {
        getDesktopProjectWorkbench().getProjectWindow().saveProjectAs(true, true);
        getAssistant().updateAssistant();
    }

    @Override
    public JIPipeDesktopPublisherAssistantConditionStatus getStatus() {
        if (getProject().getWorkDirectory() == null) {
            return JIPipeDesktopPublisherAssistantConditionStatus.Invalid;
        } else if (getDesktopProjectWorkbench().isProjectModified()) {
            return JIPipeDesktopPublisherAssistantConditionStatus.Warning;
        }
        return JIPipeDesktopPublisherAssistantConditionStatus.Valid;
    }

    @Override
    public String getAssistantTitle(JIPipeDesktopPublisherAssistantConditionStatus status) {
        return switch (status) {
            case Valid -> "Project is saved";
            case Invalid -> "Please save your project";
            case Warning -> "Project is saved, but modified";
        };
    }

    @Override
    public HTMLText getAssistantDescription(JIPipeDesktopPublisherAssistantConditionStatus status) {
        return switch (status) {
            case Valid -> new HTMLText("The current project is saved and no modifications were detected");
            case Invalid ->
                    new HTMLText("Please save your project somewhere. Otherwise the export will not be able to work.");
            case Warning ->
                    new HTMLText("The project was modified. It is recommended to save the project prior to the export.");
        };
    }
}
