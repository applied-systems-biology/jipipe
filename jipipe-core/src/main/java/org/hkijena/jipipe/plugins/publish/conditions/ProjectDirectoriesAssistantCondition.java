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
import org.hkijena.jipipe.api.project.JIPipeProjectUserPaths;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistant;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistantCondition;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistantConditionStatus;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.util.HashSet;
import java.util.Set;

public class ProjectDirectoriesAssistantCondition extends JIPipeDesktopPublisherAssistantCondition {
    public ProjectDirectoriesAssistantCondition(JIPipeDesktopPublisherAssistant assistant) {
        super(assistant);
        initialize();
    }

    private void initialize() {
        addButton(UIUtils.createButton("Go to project overview", JIPipe.RESOURCES.getIcon16("actions/go-jump.png"), this::goToProjectOverview));
    }

    private void goToProjectOverview() {
        getDesktopProjectWorkbench().getDocumentTabPane().selectSingletonTab(JIPipeDesktopProjectWorkbench.TAB_PROJECT_OVERVIEW);
    }

    @Override
    public JIPipeDesktopPublisherAssistantConditionStatus getStatus() {
        Set<String> knownKeys = new HashSet<>();
        JIPipeDesktopPublisherAssistantConditionStatus result = JIPipeDesktopPublisherAssistantConditionStatus.Valid;
        for (JIPipeProjectUserPaths.UserPathEntry userPathEntry : getProject().getMetadata().getUserPaths().getUserPathsAsInstance()) {
            if (StringUtils.isNullOrEmpty(userPathEntry.getKey())) {
                return JIPipeDesktopPublisherAssistantConditionStatus.Invalid;
            } else if (knownKeys.contains(userPathEntry.getKey())) {
                return JIPipeDesktopPublisherAssistantConditionStatus.Invalid;
            } else {
                knownKeys.add(userPathEntry.getKey());
            }

            // We want well-defined roles!
            if (userPathEntry.getRole() != JIPipeProjectUserPaths.Role.Input && userPathEntry.getRole() != JIPipeProjectUserPaths.Role.Output) {
                result = JIPipeDesktopPublisherAssistantConditionStatus.Warning;
            }
        }
        return result;
    }

    @Override
    public String getAssistantTitle(JIPipeDesktopPublisherAssistantConditionStatus status) {
        return switch (status) {
            case Valid -> "No issues with project directories detected";
            case Warning -> "Issues with project directories detected";
            default -> "Project directories are invalid";
        };
    }

    @Override
    public HTMLText getAssistantDescription(JIPipeDesktopPublisherAssistantConditionStatus status) {
        return switch (status) {
            case Valid -> new HTMLText("Project directories will be archived");
            case Warning -> new HTMLText("We recommend to specify the role of each project directory (input/output)");
            case Invalid -> new HTMLText("There are duplicate or empty project directory keys");
        };
    }
}
