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
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistant;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistantCondition;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistantConditionStatus;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

public class AuthorsAssistantCondition extends JIPipeDesktopPublisherAssistantCondition {
    public AuthorsAssistantCondition(JIPipeDesktopPublisherAssistant assistant) {
        super(assistant);
        initialize();
    }

    private void initialize() {
        addButton(UIUtils.createButton("Edit project metadata", JIPipe.RESOURCES.getIcon16("actions/edit.png"), this::editProjectMetadata));
    }

    private void editProjectMetadata() {
        getDesktopProjectWorkbench().openProjectSettings("/General/Project metadata");
        getAssistant().updateAssistant();
    }

    @Override
    public JIPipeDesktopPublisherAssistantConditionStatus getStatus() {
        if (getProject().getMetadata().getAuthors().isEmpty()) {
            return JIPipeDesktopPublisherAssistantConditionStatus.Invalid;
        }
        for (JIPipeAuthorMetadata author : getProject().getMetadata().getAuthors()) {
            if (StringUtils.isNullOrEmpty(author.getOrcidUrl())) {
                return JIPipeDesktopPublisherAssistantConditionStatus.Invalid;
            }
        }
        return JIPipeDesktopPublisherAssistantConditionStatus.Valid;
    }

    @Override
    public String getAssistantTitle(JIPipeDesktopPublisherAssistantConditionStatus status) {
        if (status == JIPipeDesktopPublisherAssistantConditionStatus.Valid) {
            return "Authors are valid";
        } else if (getProject().getMetadata().getAuthors().isEmpty()) {
            return "No authors provided";
        } else {
            return "Authors require valid ORCID";
        }
    }

    @Override
    public HTMLText getAssistantDescription(JIPipeDesktopPublisherAssistantConditionStatus status) {
        if (status == JIPipeDesktopPublisherAssistantConditionStatus.Valid) {
            return new HTMLText("Project authors will be attached to the RO-Crate metadata");
        } else if (getProject().getMetadata().getAuthors().isEmpty()) {
            return new HTMLText("You have to at least provide one author with associated ORCID");
        } else {
            return new HTMLText("Authors need to be uniquely identified by their ORCID. Please add the ORCID ID or URL into the author's metadata field.");
        }
    }
}
