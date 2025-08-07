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
import org.hkijena.jipipe.api.metadata.JIPipeOrganizationMetadata;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistant;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistantCondition;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistantConditionStatus;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

public class AuthorAffiliationsAssistantCondition extends JIPipeDesktopPublisherAssistantCondition {
    public AuthorAffiliationsAssistantCondition(JIPipeDesktopPublisherAssistant assistant) {
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
        for (JIPipeAuthorMetadata author : getProject().getMetadata().getAuthors()) {
            for (JIPipeOrganizationMetadata affiliation : author.getAffiliations()) {
                if(StringUtils.isNullOrEmpty(affiliation.getRorUrl()) && StringUtils.isNullOrEmpty(affiliation.getWebsite())) {
                    return JIPipeDesktopPublisherAssistantConditionStatus.Invalid;
                }
                else if(StringUtils.isNullOrEmpty(affiliation.getRorUrl())) {
                    return JIPipeDesktopPublisherAssistantConditionStatus.Warning;
                }
            }
        }
        return JIPipeDesktopPublisherAssistantConditionStatus.Valid;
    }

    @Override
    public String getAssistantTitle(JIPipeDesktopPublisherAssistantConditionStatus status) {
        if (status == JIPipeDesktopPublisherAssistantConditionStatus.Valid) {
            return "Author affiliations are valid";
        } else if(status == JIPipeDesktopPublisherAssistantConditionStatus.Warning) {
            return "Author affiliations should have a ROR";
        }
        else {
            return "Author affiliations require a ROR or website";
        }
    }

    @Override
    public HTMLText getAssistantDescription(JIPipeDesktopPublisherAssistantConditionStatus status) {
        if (status == JIPipeDesktopPublisherAssistantConditionStatus.Valid) {
            return new HTMLText("Project authors and affiliated organizations will be attached to the RO-Crate metadata");
        } else if(getProject().getMetadata().getAuthors().isEmpty()) {
            return new HTMLText("We recommend that affiliations are provided with a ROR identifier (see https://ror.org/)");
        }
        else {
            return new HTMLText("Affiliations should at least have an organization website or preferably a ROR (see https://ror.org/)");
        }
    }
}
