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

package org.hkijena.jipipe.api.project;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterSerializationMode;
import org.hkijena.jipipe.plugins.settings.JIPipeProjectAuthorsApplicationSettings;

/**
 * Metadata for a {@link JIPipeProject}
 */
public class JIPipeProjectMetadata extends JIPipeStandardMetadata {
    private JIPipeImageJUpdateSiteDependency.List updateSiteDependencies = new JIPipeImageJUpdateSiteDependency.List();
    private String templateDescription = "";
    private JIPipeProjectPermissions permissions = new JIPipeProjectPermissions();
    private JIPipeProjectDirectories directories = new JIPipeProjectDirectories();
    private JIPipeDynamicParameterCollection globalParameters = new JIPipeDynamicParameterCollection(true);
    private JIPipeNodeTemplate.List nodeTemplates = new JIPipeNodeTemplate.List();
    private boolean restoreTabs = true;
    private boolean autoAddAuthors = true;
    private boolean showCompartmentsRunPanelInOverview = true;

    @SetJIPipeDocumentation(name = "Automatically add new authors", description = "If enabled, automatically add the configured authors in " +
            "the application settings to the project if enabled.")
    @JIPipeParameter("auto-add-authors")
    public boolean isAutoAddAuthors() {
        return autoAddAuthors;
    }

    @JIPipeParameter("auto-add-authors")
    public void setAutoAddAuthors(boolean autoAddAuthors) {
        this.autoAddAuthors = autoAddAuthors;
    }

    @SetJIPipeDocumentation(name = "Restore tabs", description = "If enabled, all tabs are restored on loading the project. Otherwise, the Project overview and Compartments " +
            "tab are opened.")
    @JIPipeParameter(value = "restore-tabs", uiOrder = -1)
    @JsonGetter("restore-tabs")
    public boolean isRestoreTabs() {
        return restoreTabs;
    }

    @JIPipeParameter("restore-tabs")
    @JsonSetter("restore-tabs")
    public void setRestoreTabs(boolean restoreTabs) {
        this.restoreTabs = restoreTabs;
    }

    @SetJIPipeDocumentation(name = "ImageJ update site dependencies", description = "ImageJ update sites that should be enabled for the project to work. Use this if you rely on " +
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

    @SetJIPipeDocumentation(name = "Template description", description = "Description used in the 'New from template' list if this project is used as custom project template.")
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

    @JIPipeParameter("permissions")
    @SetJIPipeDocumentation(name = "Permissions", description = "Here you can set various permissions that affect what parts of " +
            "each project users can change.")
    @JsonGetter("permissions")
    public JIPipeProjectPermissions getPermissions() {
        return permissions;
    }

    @JsonSetter("permissions")
    public void setPermissions(JIPipeProjectPermissions permissions) {
        this.permissions = permissions;
    }

    @JIPipeParameter("user-directories")
    @SetJIPipeDocumentation(name = "User directories", description = "User-defined directories")
    @JsonGetter("user-directories")
    public JIPipeProjectDirectories getDirectories() {
        return directories;
    }

    @JsonSetter("user-directories")
    public void setDirectories(JIPipeProjectDirectories directories) {
        this.directories = directories;
    }

    @SetJIPipeDocumentation(name = "Node templates", description = "A list of node templates that will be available for users who edit the project.")
    @JIPipeParameter("node-templates")
    @JsonGetter("node-templates")
    public JIPipeNodeTemplate.List getNodeTemplates() {
        return nodeTemplates;
    }

    @JIPipeParameter("node-templates")
    @JsonSetter("node-templates")
    public void setNodeTemplates(JIPipeNodeTemplate.List nodeTemplates) {
        this.nodeTemplates = nodeTemplates;
    }

    @JIPipeParameter(value = "global-parameters", persistence = JIPipeParameterSerializationMode.Object)
    @JsonGetter("global-parameters")
    @SetJIPipeDocumentation(name = "Global parameters", description = "Parameters that are passed into the global context within nodes")
    public JIPipeDynamicParameterCollection getGlobalParameters() {
        return globalParameters;
    }

    @JsonSetter("global-parameters")
    public void setGlobalParameters(JIPipeDynamicParameterCollection globalParameters) {
        this.globalParameters = globalParameters;
    }

    public boolean hasKnownGlobalAuthor() {
        for (OptionalJIPipeAuthorMetadata projectAuthor : JIPipeProjectAuthorsApplicationSettings.getInstance().getProjectAuthors()) {
            if (projectAuthor.isEnabled()) {
                for (JIPipeAuthorMetadata author : getAuthors()) {
                    if (author.fuzzyEquals(projectAuthor.getContent())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @SetJIPipeDocumentation(name = "Show compartment runs in project overview", description = "If enabled, show a 'Run compartment' panel in the project overview")
    @JIPipeParameter("show-compartments-run-panel-in-overview")
    public boolean isShowCompartmentsRunPanelInOverview() {
        return showCompartmentsRunPanelInOverview;
    }

    @JIPipeParameter("show-compartments-run-panel-in-overview")
    public void setShowCompartmentsRunPanelInOverview(boolean showCompartmentsRunPanelInOverview) {
        this.showCompartmentsRunPanelInOverview = showCompartmentsRunPanelInOverview;
    }
}
