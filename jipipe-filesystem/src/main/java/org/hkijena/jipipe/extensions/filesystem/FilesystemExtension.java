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

package org.hkijena.jipipe.extensions.filesystem;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.filesystem.algorithms.*;
import org.hkijena.jipipe.extensions.filesystem.compat.PathDataImageJAdapter;
import org.hkijena.jipipe.extensions.filesystem.compat.PathDataImporterUI;
import org.hkijena.jipipe.extensions.filesystem.datasources.*;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.filesystem.resultanalysis.CopyPathDataOperation;
import org.hkijena.jipipe.extensions.filesystem.resultanalysis.FilesystemDataSlotPreview;
import org.hkijena.jipipe.extensions.filesystem.resultanalysis.OpenPathDataOperation;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

/**
 * Extension that provides filesystem data types
 */
@Plugin(type = JIPipeJavaExtension.class)
public class FilesystemExtension extends JIPipePrepackagedDefaultJavaExtension {

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Filesystem types and algorithms";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Data types and algorithms for interacting with files and folders");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:filesystem";
    }

    @Override
    public String getDependencyVersion() {
        return "1.52.2";
    }

    @Override
    public void register() {
        // Register main data types
        registerDatatype("path", PathData.class, ResourceUtils.getPluginResource("icons/data-types/path.png"),
                null, FilesystemDataSlotPreview.class, new OpenPathDataOperation(), new CopyPathDataOperation());
        registerDatatype("file", FileData.class, ResourceUtils.getPluginResource("icons/data-types/file.png"),
                null, FilesystemDataSlotPreview.class, new OpenPathDataOperation(), new CopyPathDataOperation());
        registerDatatype("folder", FolderData.class, ResourceUtils.getPluginResource("icons/data-types/folder.png"),
                null, FilesystemDataSlotPreview.class, new OpenPathDataOperation(), new CopyPathDataOperation());

        // Register conversion between them
        registerDatatypeConversion(new ImplicitPathTypeConverter(PathData.class, FileData.class));
        registerDatatypeConversion(new ImplicitPathTypeConverter(PathData.class, FolderData.class));
        registerDatatypeConversion(new ImplicitPathTypeConverter(FileData.class, FolderData.class));
        registerDatatypeConversion(new ImplicitPathTypeConverter(FolderData.class, FileData.class));

        // Register ImageJ compat
        registerImageJDataAdapter(new PathDataImageJAdapter(PathData.class), PathDataImporterUI.class);
        registerImageJDataAdapter(new PathDataImageJAdapter(FileData.class), PathDataImporterUI.class);
        registerImageJDataAdapter(new PathDataImageJAdapter(FolderData.class), PathDataImporterUI.class);

        registerAlgorithms();

        registerSettingsSheet(FilesystemExtensionSettings.ID,
                "Filesystem",
                UIUtils.getIconFromResources("actions/document-open-folder.png"),
                "Extensions",
                UIUtils.getIconFromResources("actions/plugins.png"),
                new FilesystemExtensionSettings());
    }

    private void registerAlgorithms() {
        registerNodeType("import-file", FileDataSource.class);
        registerNodeType("import-file-list", FileListDataSource.class);
        registerNodeType("import-folder", FolderDataSource.class);
        registerNodeType("import-folder-list", FolderListDataSource.class);
        registerNodeType("import-path", PathDataSource.class);
        registerNodeType("import-path-list", PathListDataSource.class);
        registerNodeType("download-files", DownloadFilesDataSource.class);
        registerNodeType("file-temporary", TemporaryFileDataSource.class);
        registerNodeType("folder-temporary", TemporaryFolderDataSource.class);
        registerNodeType("folder-run-output", OutputFolderDataSource.class);
        registerNodeType("select-path-interactive", PathFromUserDataSource.class);

        registerNodeType("path-extract-filename", ExtractFileName.class, UIUtils.getIconURLFromResources("data-types/files.png"));
        registerNodeType("path-extract-parent", ExtractParent.class, UIUtils.getIconURLFromResources("actions/go-parent-folder.png"));
        registerNodeType("path-relativize", RelativizePaths.class, UIUtils.getIconURLFromResources("data-types/path.png"));
        registerNodeType("path-relativize-by-parameter", RelativizeByParameter.class, UIUtils.getIconURLFromResources("data-types/path.png"));
        registerNodeType("path-concatenate", ConcatenatePaths.class, UIUtils.getIconURLFromResources("actions/list-add.png"));
        registerNodeType("path-concatenate-by-parameter", ConcatenateByParameter.class, UIUtils.getIconURLFromResources("actions/list-add.png"));
        registerNodeType("folder-mkdir", CreateDirectory.class, UIUtils.getIconURLFromResources("actions/folder-new.png"));
        registerNodeType("path-rename-string", RenameByString.class, UIUtils.getIconURLFromResources("actions/tag.png"));
        registerNodeType("path-copy", CopyPath.class, UIUtils.getIconURLFromResources("actions/edit-copy.png"));
        registerNodeType("path-filter", FilterPaths.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("folder-list-files", ListFiles.class, UIUtils.getIconURLFromResources("actions/view-list-details.png"));
        registerNodeType("folder-list-subfolders", ListSubfolders.class, UIUtils.getIconURLFromResources("actions/view-list-details.png"));
        registerNodeType("data-to-output-path", ConvertDataToOutputPath.class, UIUtils.getIconURLFromResources("actions/folder-new.png"));
        registerNodeType("modify-path", ModifyPath.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));

        registerNodeType("export-data-by-parameter", ExportDataByParameter.class, UIUtils.getIconURLFromResources("actions/document-export.png"));
        registerNodeType("export-data", ExportData.class, UIUtils.getIconURLFromResources("actions/document-export.png"));
        registerNodeType("import-data-from-slot-folder", ImportData.class, UIUtils.getIconURLFromResources("actions/document-import.png"));
        registerNodeType("import-data-from-row-folder", ImportDataRowFolder.class, UIUtils.getIconURLFromResources("actions/document-import.png"));

        registerNodeType("annotation-to-path", AnnotationToPath.class, UIUtils.getIconURLFromResources("data-types/path.png"));

        registerNodeType("folder-annotate-by-name", SimpleFolderAnnotationGenerator.class, UIUtils.getIconURLFromResources("actions/image-auto-adjust.png"));
        registerNodeType("file-annotate-by-name", SimpleFileAnnotationGenerator.class, UIUtils.getIconURLFromResources("actions/image-auto-adjust.png"));
        registerNodeType("path-to-annotation-simple", SimplePathAnnotationGenerator.class, UIUtils.getIconURLFromResources("actions/image-auto-adjust.png"));

        registerNodeType("annotation-table-to-paths", AnnotationTableToPaths.class, UIUtils.getIconURLFromResources("data-types/path.png"));
    }

}
