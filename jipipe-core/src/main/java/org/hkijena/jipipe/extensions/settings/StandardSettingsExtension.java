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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
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
    public HTMLText getDescription() {
        return new HTMLText("Provides a collection of settings sheets");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:settings";
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerEnumParameterType("jipipe:settings:downloads:download-tool", DownloadSettings.DownloadTool.class, "Download tool", "The tool to use for downloading files");
        registerParameterType("jipipe:settings:downloads:downloader-environment", DownloadSettings.DownloadEnvironment.class, "Downloader process", "A downloader process");

        registerSettingsSheet(DownloadSettings.ID,
                "Downloads",
                UIUtils.getIconFromResources("actions/browser-download.png"),
                null,
                null,
                new DownloadSettings());
        registerSettingsSheet(RuntimeSettings.ID,
                "Runtime",
                UIUtils.getIconFromResources("actions/player_start.png"),
                null,
                null,
                new RuntimeSettings());
        registerSettingsSheet(ProjectsSettings.ID,
                "Projects",
                UIUtils.getIconFromResources("actions/project-development.png"),
                null,
                null,
                new ProjectsSettings());
        registerSettingsSheet(AutoSaveSettings.ID,
                "Auto-save",
                UIUtils.getIconFromResources("actions/save.png"),
                null,
                null,
                new AutoSaveSettings());
        registerSettingsSheet(HistoryJournalSettings.ID,
                "Journal",
                UIUtils.getIconFromResources("actions/edit-undo-history.png"),
                null,
                null,
                new HistoryJournalSettings());
        registerSettingsSheet(GeneralUISettings.ID,
                "General",
                UIUtils.getIconFromResources("actions/settings.png"),
                "UI",
                null,
                new GeneralUISettings());
        registerSettingsSheet(NotificationUISettings.ID,
                "Notifications",
                UIUtils.getIconFromResources("actions/dialog-messages.png"),
                "UI",
                null,
                new NotificationUISettings());
        registerSettingsSheet(GraphEditorUISettings.ID,
                "Graph editor",
                UIUtils.getIconFromResources("actions/distribute-graph.png"),
                "UI",
                null,
                new GraphEditorUISettings());
        registerSettingsSheet(ImageViewerUISettings.ID,
                "Image viewer",
                UIUtils.getIconFromResources("actions/viewimage.png"),
                "UI",
                null,
                new ImageViewerUISettings());
        registerSettingsSheet(TableViewerUISettings.ID,
                "Table viewer",
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
                UIUtils.getIconFromResources("actions/quickopen-file.png"),
                "UI",
                null,
                new FileChooserSettings());
        registerSettingsSheet(ExtensionSettings.ID,
                "Extensions",
                UIUtils.getIconFromResources("actions/plugins.png"),
                "General",
                null,
                new ExtensionSettings());
        registerSettingsSheet(DefaultResultImporterSettings.ID,
                "Default result importers",
                UIUtils.getIconFromResources("actions/rabbitvcs-import.png"),
                "Data",
                UIUtils.getIconFromResources("actions/database.png"),
                new DefaultResultImporterSettings());
        registerSettingsSheet(DefaultCacheDisplaySettings.ID,
                "Default cache displays",
                UIUtils.getIconFromResources("actions/zoom.png"),
                "Data",
                UIUtils.getIconFromResources("actions/database.png"),
                new DefaultCacheDisplaySettings());
        registerSettingsSheet(GeneralDataSettings.ID,
                "General",
                UIUtils.getIconFromResources("actions/settings.png"),
                "Data",
                null,
                new GeneralDataSettings());
        registerSettingsSheet(VirtualDataSettings.ID,
                "Reduced memory",
                UIUtils.getIconFromResources("actions/rabbitvcs-drive.png"),
                "Data",
                null,
                new VirtualDataSettings());
        registerSettingsSheet(DataExporterSettings.ID,
                "Data export",
                UIUtils.getIconFromResources("actions/document-export.png"),
                "Data",
                null,
                new DataExporterSettings());
        registerSettingsSheet(NodeTemplateSettings.ID,
                "Node templates",
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
