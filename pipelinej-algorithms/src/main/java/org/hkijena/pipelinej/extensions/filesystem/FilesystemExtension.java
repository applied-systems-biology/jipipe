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

package org.hkijena.pipelinej.extensions.filesystem;

import org.hkijena.pipelinej.ACAQJavaExtension;
import org.hkijena.pipelinej.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.pipelinej.extensions.filesystem.algorithms.*;
import org.hkijena.pipelinej.extensions.filesystem.compat.PathDataImageJAdapter;
import org.hkijena.pipelinej.extensions.filesystem.compat.PathDataImporterUI;
import org.hkijena.pipelinej.extensions.filesystem.datasources.FileDataSource;
import org.hkijena.pipelinej.extensions.filesystem.datasources.FileListDataSource;
import org.hkijena.pipelinej.extensions.filesystem.datasources.FolderDataSource;
import org.hkijena.pipelinej.extensions.filesystem.datasources.FolderListDataSource;
import org.hkijena.pipelinej.extensions.filesystem.dataypes.FileData;
import org.hkijena.pipelinej.extensions.filesystem.dataypes.FolderData;
import org.hkijena.pipelinej.extensions.filesystem.dataypes.PathData;
import org.hkijena.pipelinej.extensions.filesystem.resultanalysis.FilesystemDataSlotCellUI;
import org.hkijena.pipelinej.extensions.filesystem.resultanalysis.FilesystemDataSlotRowUI;
import org.hkijena.pipelinej.utils.ResourceUtils;
import org.hkijena.pipelinej.utils.UIUtils;
import org.scijava.plugin.Plugin;

/**
 * Extension that provides filesystem data types
 */
@Plugin(type = ACAQJavaExtension.class)
public class FilesystemExtension extends ACAQPrepackagedDefaultJavaExtension {

    @Override
    public String getName() {
        return "Filesystem types and algorithms";
    }

    @Override
    public String getDescription() {
        return "Data types and algorithms for interacting with files and folders";
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.acaq5:filesystem";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }

    @Override
    public void register() {
        // Register main data types
        registerDatatype("path", PathData.class, ResourceUtils.getPluginResource("icons/data-types/path.png"),
                FilesystemDataSlotRowUI.class, new FilesystemDataSlotCellUI());
        registerDatatype("file", FileData.class, ResourceUtils.getPluginResource("icons/data-types/file.png"),
                FilesystemDataSlotRowUI.class, new FilesystemDataSlotCellUI());
        registerDatatype("folder", FolderData.class, ResourceUtils.getPluginResource("icons/data-types/folder.png"),
                FilesystemDataSlotRowUI.class, new FilesystemDataSlotCellUI());

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
    }

    private void registerAlgorithms() {
        registerAlgorithm("import-file", FileDataSource.class);
        registerAlgorithm("import-file-list", FileListDataSource.class);
        registerAlgorithm("import-folder", FolderDataSource.class);
        registerAlgorithm("import-folder-list", FolderListDataSource.class);

        registerAlgorithm("path-filter", FilterPaths.class, UIUtils.getAlgorithmIconURL("filter.png"));
        registerAlgorithm("folder-list-files", ListFiles.class, UIUtils.getAlgorithmIconURL("list.png"));
        registerAlgorithm("folder-list-subfolders", ListSubfolders.class, UIUtils.getAlgorithmIconURL("list.png"));
        registerAlgorithm("folder-navigate-subfolders", NavigateSubFolder.class, UIUtils.getAlgorithmIconURL("go-parent-folder.png"));

        registerAlgorithm("folder-annotate-by-name", SimpleFolderAnnotationGenerator.class, UIUtils.getAlgorithmIconURL("tools-wizard.png"));
        registerAlgorithm("file-annotate-by-name", SimpleFileAnnotationGenerator.class, UIUtils.getAlgorithmIconURL("tools-wizard.png"));
        registerAlgorithm("path-to-annotation-simple", SimplePathAnnotationGenerator.class, UIUtils.getAlgorithmIconURL("tools-wizard.png"));

        registerAlgorithm("annotation-table-to-paths", AnnotationTableToPaths.class, UIUtils.getAlgorithmIconURL("path.png"));
    }

}
