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

package org.hkijena.jipipe.plugins.filesystem;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.DefaultImageJDataExporterUI;
import org.hkijena.jipipe.api.compat.DefaultImageJDataImporterUI;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.filesystem.algorithms.local.*;
import org.hkijena.jipipe.plugins.filesystem.algorithms.zarr.ListZARRDirectoryZIPDatasetsAlgorithm;
import org.hkijena.jipipe.plugins.filesystem.algorithms.zarr.ListZARRURIDatasetsAlgorithm;
import org.hkijena.jipipe.plugins.filesystem.compat.PathDataFromTableImageJImporter;
import org.hkijena.jipipe.plugins.filesystem.compat.PathDataToTableImageJExporter;
import org.hkijena.jipipe.plugins.filesystem.datasources.*;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FileData;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;
import org.hkijena.jipipe.plugins.filesystem.desktop.PathDataViewer;
import org.hkijena.jipipe.plugins.filesystem.resultanalysis.CopyPathDataDisplayOperation;
import org.hkijena.jipipe.plugins.filesystem.resultanalysis.FilesystemDataSlotPreview;
import org.hkijena.jipipe.plugins.filesystem.resultanalysis.OpenPathDataDisplayOperation;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

/**
 * Extension that provides filesystem data types
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class FilesystemPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:filesystem",
            JIPipe.getJIPipeVersion(),
            "Filesystem types and algorithms");

    public static JIPipeResourceManager RESOURCES = new JIPipeResourceManager(FilesystemPlugin.class, "org/hkijena/jipipe/plugins/filesystem");

    public FilesystemPlugin() {
    }

    @Override
    public PluginCategoriesEnumParameter.List getCategories() {
        return new PluginCategoriesEnumParameter.List(PluginCategoriesEnumParameter.CATEGORY_AUTOMATION, PluginCategoriesEnumParameter.CATEGORY_ANNOTATION, PluginCategoriesEnumParameter.CATEGORY_FILTERING);
    }

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
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        // Register main data types
        registerDatatype("path", PathData.class, ResourceUtils.getPluginResource("icons/data-types/path.png"),
                null, FilesystemDataSlotPreview.class, new OpenPathDataDisplayOperation(), new CopyPathDataDisplayOperation());
        registerDatatype("file", FileData.class, ResourceUtils.getPluginResource("icons/data-types/file.png"),
                null, FilesystemDataSlotPreview.class, new OpenPathDataDisplayOperation(), new CopyPathDataDisplayOperation());
        registerDatatype("folder", FolderData.class, ResourceUtils.getPluginResource("icons/data-types/folder.png"),
                null, FilesystemDataSlotPreview.class, new OpenPathDataDisplayOperation(), new CopyPathDataDisplayOperation());

        // Register viewer
        registerDefaultDataTypeViewer(PathData.class, PathDataViewer.class);

        // Register conversion between them
        registerDatatypeConversion(new ImplicitPathTypeConverter(PathData.class, FileData.class));
        registerDatatypeConversion(new ImplicitPathTypeConverter(PathData.class, FolderData.class));
        registerDatatypeConversion(new ImplicitPathTypeConverter(FileData.class, FolderData.class));
        registerDatatypeConversion(new ImplicitPathTypeConverter(FolderData.class, FileData.class));

        // Register ImageJ compat
        registerImageJDataImporter("path-from-results-table", new PathDataFromTableImageJImporter(PathData.class), DefaultImageJDataImporterUI.class);
        registerImageJDataExporter("path-to-results-table", new PathDataToTableImageJExporter(), DefaultImageJDataExporterUI.class);

        registerAlgorithms();

        registerApplicationSettingsSheet(new JIPipeFilesystemPluginApplicationSettings());
    }

    private void registerAlgorithms() {
        registerNodeType("import-file", FileDataSource.class);
        registerNodeType("import-file-list", FileListDataSource.class);
        registerNodeType("import-folder", FolderDataSource.class);
        registerNodeType("import-folder-list", FolderListDataSource.class);
        registerNodeType("import-path", PathDataSource.class);
        registerNodeType("import-path-list", PathListDataSource.class);
        registerNodeType("import-uri-list", URIListDataSource.class);
        registerNodeType("download-files", DownloadFilesDataSource.class);
        registerNodeType("file-temporary", TemporaryFileDataSource.class);
        registerNodeType("folder-temporary", TemporaryFolderDataSource.class);
        registerNodeType("folder-run-output", OutputFolderDataSource.class);
        registerNodeType("select-path-interactive", PathFromUserDataSource.class);
        registerNodeType("project-user-directory", ProjectUserFolderDataSource.class);

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
        registerNodeType("export-data-by-parameter-v2", ExportDataByParameter2.class, UIUtils.getIconURLFromResources("actions/document-export.png"));
        registerNodeType("export-data", ExportData.class, UIUtils.getIconURLFromResources("actions/document-export.png"));
        registerNodeType("export-data-table-by-parameter", ExportDataTableByParameter.class, UIUtils.getIconURLFromResources("actions/document-export.png"));
        registerNodeType("export-data-table", ExportDataTable.class, UIUtils.getIconURLFromResources("actions/document-export.png"));
        registerNodeType("import-data-from-slot-folder", ImportDataTableDirectory.class, UIUtils.getIconURLFromResources("actions/document-import.png"));
        registerNodeType("import-data-table-from-archive", ImportDataTableArchive.class, UIUtils.getIconURLFromResources("actions/document-import.png"));
        registerNodeType("import-data-from-row-folder", ImportDataRowFolder.class, UIUtils.getIconURLFromResources("actions/document-import.png"));

        registerNodeType("annotation-to-path", AnnotationToPath.class, UIUtils.getIconURLFromResources("data-types/path.png"));

        registerNodeType("folder-annotate-by-name", SimpleFolderAnnotationGenerator.class, UIUtils.getIconURLFromResources("actions/image-auto-adjust.png"));
        registerNodeType("file-annotate-by-name", SimpleFileAnnotationGenerator.class, UIUtils.getIconURLFromResources("actions/image-auto-adjust.png"));
        registerNodeType("path-to-annotation-simple", SimplePathAnnotationGenerator.class, UIUtils.getIconURLFromResources("actions/image-auto-adjust.png"));

        registerNodeType("annotation-table-to-paths", AnnotationTableToPaths.class, UIUtils.getIconURLFromResources("data-types/path.png"));
        registerNodeType("annotate-with-path-properties", AnnotateWithPathProperties.class, UIUtils.getIconURLFromResources("data-types/path.png"));

        registerNodeType("path-modify-with-expression", ModifyPathWithExpression.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("get-exported-path", ConvertToExportedPath.class, UIUtils.getIconURLFromResources("actions/reload.png"));

        // ZARR
        registerNodeType("list-zarr-directory-zip-datasets", ListZARRDirectoryZIPDatasetsAlgorithm.class, UIUtils.getIconURLFromResources("actions/zarr.png"));
        registerNodeType("list-zarr-uri-datasets", ListZARRURIDatasetsAlgorithm.class, UIUtils.getIconURLFromResources("actions/zarr.png"));

        registerNodeExamplesFromResources(RESOURCES, "examples");
    }

}
