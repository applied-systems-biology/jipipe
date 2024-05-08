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

package org.hkijena.jipipe.plugins.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

/**
 * Extension that provides all JIPipe included settings sheets
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class StandardSettingsPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Standard settings";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides a collection of settings sheets");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:settings";
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerParameterType("jipipe:settings:downloads:downloader-environment", JIPipeDownloadsApplicationSettings.DownloadEnvironment.class, "Downloader process", "A downloader process");
        registerEnumParameterType("settings:" + JIPipeFileChooserApplicationSettings.ID + ":file-chooser-type",
                JIPipeFileChooserApplicationSettings.FileChooserType.class,
                "File chooser type",
                "Type of file chooser");
        registerParameterType("jipipe:settings:projects:new-project-template",
                JIPipeProjectDefaultsApplicationSettings.ProjectTemplateEnum.class,
                null,
                null,
                "New project template",
                "Template for new projects",
                null);


        //General
        registerSettingsSheet(new JIPipeDownloadsApplicationSettings());
        registerSettingsSheet(new JIPipeRuntimeApplicationSettings());
        registerSettingsSheet(new JIPipeProjectDefaultsApplicationSettings());
        registerSettingsSheet(new JIPipeBackupApplicationSettings());
        registerSettingsSheet(new JIPipeHistoryJournalApplicationSettings());
        registerSettingsSheet(new JIPipeExtensionApplicationSettings());
        registerSettingsSheet(new JIPipeNodeTemplateApplicationSettings());

        // UI
        registerSettingsSheet(new JIPipeGeneralUIApplicationSettings());
        registerSettingsSheet(new JIPipeNotificationUIApplicationSettings());
        registerSettingsSheet(new JIPipeGraphEditorUIApplicationSettings());
        registerSettingsSheet(new JIPipeTableViewerUIApplicationSettings());
        registerSettingsSheet(new JIPipeFileChooserApplicationSettings());

        // Data
        registerSettingsSheet(new JIPipeDefaultResultImporterApplicationSettings());
        registerSettingsSheet(new JIPipeDefaultCacheDisplayApplicationSettings());
        registerSettingsSheet(new JIPipeGeneralDataApplicationSettings());
        registerSettingsSheet(new JIPipeDataExporterApplicationSettings());


    }

    @Override
    public boolean isCorePlugin() {
        return true;
    }

}
