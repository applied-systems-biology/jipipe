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

package org.hkijena.jipipe.extensions.settings;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.scijava.plugin.Plugin;

/**
 * Extension that provides all JIPipe included settings sheets
 */
@Plugin(type = JIPipeJavaExtension.class)
public class StandardSettingsExtension extends JIPipePrepackagedDefaultJavaExtension {

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Standard settings";
    }

    @Override
    public String getDescription() {
        return "Provides a collection of settings sheets";
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:settings";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }

    @Override
    public void register() {
        registerSettingsSheet(RuntimeSettings.ID, "Runtime", null, null, new RuntimeSettings());
        registerSettingsSheet(ProjectsSettings.ID, "Recent projects", null, null, new ProjectsSettings());
        registerSettingsSheet(GeneralUISettings.ID, "General", "UI", null, new GeneralUISettings());
        registerSettingsSheet(GraphEditorUISettings.ID, "Graph editor", "UI", null, new GraphEditorUISettings());
        registerSettingsSheet(FileChooserSettings.ID, "File chooser", "UI", null, new FileChooserSettings());

        registerEnumParameterType("jipipe:settings:projects:starter-project",
                ProjectsSettings.StarterProject.class,
                "Empty project configuration",
                "Available ways how an empty project is initialized");
    }

}
