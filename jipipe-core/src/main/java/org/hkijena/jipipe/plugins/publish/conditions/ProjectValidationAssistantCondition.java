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
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistant;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistantCondition;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistantConditionStatus;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.UIUtils;

public class ProjectValidationAssistantCondition extends JIPipeDesktopPublisherAssistantCondition {
    private JIPipeValidationReport lastReport;

    public ProjectValidationAssistantCondition(JIPipeDesktopPublisherAssistant assistant) {
        super(assistant);
        initialize();
    }

    private void initialize() {
        addButton(UIUtils.createButton("Show report", JIPipe.RESOURCES.getIcon16("actions/document-preview.png"), this::showReport));
    }

    private void showReport() {
        UIUtils.showValidityReportDialog(getDesktopProjectWorkbench(),
                getDesktopProjectWorkbench().getProjectWindow(),
                lastReport,
                "Project validation report (strict)",
                "Here you can find a list of all issues that have been found:",
                false);
    }

    @Override
    public JIPipeDesktopPublisherAssistantConditionStatus getStatus() {
        this.lastReport = getProject().generateValidityReport(new UnspecifiedValidationReportContext(), JIPipeValidationReportSettings.STRICT);
        if (lastReport.isValid()) {
            if (lastReport.getNumberOf(JIPipeValidationReportEntryLevel.Warning) > 0) {
                return JIPipeDesktopPublisherAssistantConditionStatus.Warning;
            } else {
                return JIPipeDesktopPublisherAssistantConditionStatus.Valid;
            }
        } else {
            return JIPipeDesktopPublisherAssistantConditionStatus.Invalid;
        }
    }

    @Override
    public String getAssistantTitle(JIPipeDesktopPublisherAssistantConditionStatus status) {
        return switch (status) {
            case Valid -> "No issues with the project detected";
            case Warning -> "Potential issues with the project found";
            case Invalid -> "Project is invalid";
        };
    }

    @Override
    public HTMLText getAssistantDescription(JIPipeDesktopPublisherAssistantConditionStatus status) {
        return switch (status) {
            case Valid -> new HTMLText("The project passed the validation check");
            case Warning ->
                    new HTMLText(lastReport.getNumberOf(JIPipeValidationReportEntryLevel.Warning) + " warnings were generated. Please click the 'Show report' button to review them.");
            case Invalid ->
                    new HTMLText("Issues with the project have been detected. Please click the 'Show report' button to review them.");
        };
    }
}
