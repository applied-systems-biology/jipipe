package org.hkijena.acaq5.extensions.filesystem;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.filesystem.algorithms.*;
import org.hkijena.acaq5.extensions.filesystem.annotation.FileAnnotationGenerator;
import org.hkijena.acaq5.extensions.filesystem.annotation.FolderAnnotationGenerator;
import org.hkijena.acaq5.extensions.filesystem.compat.PathDataImageJAdapter;
import org.hkijena.acaq5.extensions.filesystem.compat.PathDataImporterUI;
import org.hkijena.acaq5.extensions.filesystem.datasources.FileDataSource;
import org.hkijena.acaq5.extensions.filesystem.datasources.FileListDataSource;
import org.hkijena.acaq5.extensions.filesystem.datasources.FolderDataSource;
import org.hkijena.acaq5.extensions.filesystem.datasources.FolderListDataSource;
import org.hkijena.acaq5.extensions.filesystem.dataypes.FileData;
import org.hkijena.acaq5.extensions.filesystem.dataypes.FolderData;
import org.hkijena.acaq5.extensions.filesystem.dataypes.PathData;
import org.hkijena.acaq5.extensions.filesystem.resultanalysis.FilesystemDataSlotCellUI;
import org.hkijena.acaq5.extensions.filesystem.resultanalysis.FilesystemDataSlotRowUI;
import org.hkijena.acaq5.extensions.standardalgorithms.api.registries.GraphWrapperAlgorithmRegistrationTask;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.util.Set;

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
        registerAlgorithmResources();
    }

    private void registerAlgorithms() {
        registerAlgorithm("import-file", FileDataSource.class);
        registerAlgorithm("import-file-list", FileListDataSource.class);
        registerAlgorithm("import-folder", FolderDataSource.class);
        registerAlgorithm("import-folder-list", FolderListDataSource.class);

        registerAlgorithm("file-filter", FilterFiles.class);
        registerAlgorithm("folder-filter", FilterFolders.class);
        registerAlgorithm("folder-list-files", ListFiles.class);
        registerAlgorithm("folder-list-subfolders", ListSubfolders.class);
        registerAlgorithm("folder-navigate-subfolders", NavigateSubFolder.class);

        registerAlgorithm("folder-annotate-by-name", FolderAnnotationGenerator.class);
        registerAlgorithm("file-annotate-by-name", FileAnnotationGenerator.class);
    }

    private void registerAlgorithmResources() {
        Set<String> algorithmFiles = ResourceUtils.walkInternalResourceFolder("extensions/filesystem/api/algorithms");
        for (String resourceFile : algorithmFiles) {
            try {
                JsonNode node = JsonUtils.getObjectMapper().readValue(ResourceUtils.class.getResource(resourceFile), JsonNode.class);
                GraphWrapperAlgorithmRegistrationTask task = new GraphWrapperAlgorithmRegistrationTask(node, this);
                getRegistry().getAlgorithmRegistry().scheduleRegister(task);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
