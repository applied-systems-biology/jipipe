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
import org.hkijena.jipipe.utils.UIUtils;
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
        registerParameterType("jipipe:settings:downloads:downloader-environment", DownloadSettings.DownloadEnvironment.class, "Downloader process", "A downloader process");

        registerSettingsSheet(DownloadSettings.ID,
                "Downloads",
                "Configure how JIPipe downloads resources from the web",
                UIUtils.getIconFromResources("actions/download.png"),
                null,
                null,
                new DownloadSettings());
        registerSettingsSheet(RuntimeSettings.ID,
                "Runtime",
                "General properties of JIPipe runs (number of threads, etc.)",
                UIUtils.getIconFromResources("actions/play.png"),
                null,
                null,
                new RuntimeSettings());
        registerSettingsSheet(ProjectsSettings.ID,
                "Projects",
                "Project-related settings, including the default template and list of recent projects",
                UIUtils.getIconFromResources("actions/project-development.png"),
                null,
                null,
                new ProjectsSettings());
        registerSettingsSheet(BackupSettings.ID,
                "Backup",
                "Determine the behavior of the automated backup functionality",
                UIUtils.getIconFromResources("actions/save.png"),
                null,
                null,
                new BackupSettings());
        registerSettingsSheet(HistoryJournalSettings.ID,
                "Journal",
                "Configure the undo/redo behavior (e.g., how many steps are saved)",
                UIUtils.getIconFromResources("actions/edit-undo-history.png"),
                null,
                null,
                new HistoryJournalSettings());
        registerSettingsSheet(GeneralUISettings.ID,
                "General",
                "General UI settings",
                UIUtils.getIconFromResources("actions/settings.png"),
                "UI",
                null,
                new GeneralUISettings());
        registerSettingsSheet(NotificationUISettings.ID,
                "Notifications",
                "Determines when and how you are notified by JIPipe",
                UIUtils.getIconFromResources("actions/dialog-messages.png"),
                "UI",
                null,
                new NotificationUISettings());
        registerSettingsSheet(GraphEditorUISettings.ID,
                "Graph editor",
                "Configures the pipeline editor UI (e.g., the default view mode)",
                UIUtils.getIconFromResources("actions/distribute-graph.png"),
                "UI",
                null,
                new GraphEditorUISettings());
        registerSettingsSheet(TableViewerUISettings.ID,
                "Table viewer",
                "Settings for the JIPipe table viewer",
                UIUtils.getIconFromResources("actions/table.png"),
                "UI",
                null,
                new TableViewerUISettings());
        registerEnumParameterType("settings:" + FileChooserSettings.ID + ":file-chooser-type",
                FileChooserSettings.FileChooserType.class,
                "File chooser type",
                "Type of file chooser");
        registerSettingsSheet(FileChooserSettings.ID,
                "File chooser",
                "Allows to change the open/save dialogs",
                UIUtils.getIconFromResources("actions/quickopen-file.png"),
                "UI",
                null,
                new FileChooserSettings());
        registerSettingsSheet(ExtensionSettings.ID,
                "Extensions",
                "Settings regarding the validation of extensions",
                UIUtils.getIconFromResources("actions/plugins.png"),
                "General",
                null,
                new ExtensionSettings());
        registerSettingsSheet(DefaultResultImporterSettings.ID,
                "Default result importers",
                "Determines how the JIPipe result viewer imports data from the file system",
                UIUtils.getIconFromResources("actions/rabbitvcs-import.png"),
                "Data",
                UIUtils.getIconFromResources("actions/database.png"),
                new DefaultResultImporterSettings());
        registerSettingsSheet(DefaultCacheDisplaySettings.ID,
                "Default cache displays",
                "Determines how the JIPipe cache browser displays data by default (double click)",
                UIUtils.getIconFromResources("actions/zoom.png"),
                "Data",
                UIUtils.getIconFromResources("actions/database.png"),
                new DefaultCacheDisplaySettings());
        registerSettingsSheet(GeneralDataSettings.ID,
                "General",
                "Allows to change general data display settings (e.g., whether previews are displayed and their size)",
                UIUtils.getIconFromResources("actions/settings.png"),
                "Data",
                null,
                new GeneralDataSettings());
        registerSettingsSheet(DataExporterSettings.ID,
                "Data export",
                "Default settings for data exporters",
                UIUtils.getIconFromResources("actions/document-export.png"),
                "Data",
                null,
                new DataExporterSettings());
        registerSettingsSheet(NodeTemplateSettings.ID,
                "Node templates",
                "Contains the global list of node templates",
                UIUtils.getIconFromResources("actions/plugins.png"),
                "Extensions",
                UIUtils.getIconFromResources("actions/plugins.png"),
                new NodeTemplateSettings());
        registerParameterType("jipipe:settings:projects:new-project-template",
                ProjectsSettings.ProjectTemplateEnum.class,
                null,
                null,
                "New project template",
                "Template for new projects",
                null);
    }

    @Override
    public boolean isCoreExtension() {
        return true;
    }

}
