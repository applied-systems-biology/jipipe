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

package org.hkijena.jipipe.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

/**
 * Metadata for a {@link JIPipeProject}
 */
public class JIPipeProjectMetadata extends JIPipeMetadata {
    private JIPipeImageJUpdateSiteDependency.List updateSiteDependencies = new JIPipeImageJUpdateSiteDependency.List();
    private String templateDescription = "";

    @JIPipeDocumentation(name = "ImageJ update site dependencies", description = "ImageJ update sites that should be enabled for the project to work. Use this if you rely on " +
            "third-party methods that are not referenced in a JIPipe extension (e.g. within a script or macro node). " +
            "Users will get a notification if a site is not activated or found. Both name and URL should be set. The URL is only used if a site with the same name " +
            "does not already exist in the user's repository.")
    @JIPipeParameter(value = "update-site-dependencies", uiOrder = 10)
    @JsonGetter("update-site-dependencies")
    public JIPipeImageJUpdateSiteDependency.List getUpdateSiteDependencies() {
        return updateSiteDependencies;
    }

    @JIPipeParameter("update-site-dependencies")
    @JsonSetter("update-site-dependencies")
    public void setUpdateSiteDependencies(JIPipeImageJUpdateSiteDependency.List updateSiteDependencies) {
        this.updateSiteDependencies = updateSiteDependencies;
    }

    @JIPipeDocumentation(name = "Template description", description = "Description used in the 'New from template' list if this project is used as custom project template.")
    @JIPipeParameter("template-description")
    @JsonGetter("template-description")
    public String getTemplateDescription() {
        return templateDescription;
    }

    @JIPipeParameter("template-description")
    @JsonSetter("template-description")
    public void setTemplateDescription(String templateDescription) {
        this.templateDescription = templateDescription;
    }
}
