package org.hkijena.acaq5.extensions.filesystem;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.filesystem.api.algorithms.*;
import org.hkijena.acaq5.extensions.filesystem.api.annotation.ACAQFileAnnotationGenerator;
import org.hkijena.acaq5.extensions.filesystem.api.annotation.ACAQFolderAnnotationGenerator;
import org.hkijena.acaq5.extensions.filesystem.api.datasources.ACAQFileDataSource;
import org.hkijena.acaq5.extensions.filesystem.api.datasources.ACAQFileListDataSource;
import org.hkijena.acaq5.extensions.filesystem.api.datasources.ACAQFolderDataSource;
import org.hkijena.acaq5.extensions.filesystem.api.datasources.ACAQFolderListDataSource;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.ACAQFileData;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.ACAQFolderData;
import org.hkijena.acaq5.extensions.filesystem.ui.resultanalysis.FilesystemDataSlotCellUI;
import org.hkijena.acaq5.extensions.filesystem.ui.resultanalysis.FilesystemDataSlotRowUI;
import org.hkijena.acaq5.extensions.standardalgorithms.api.registries.GraphWrapperAlgorithmRegistrationTask;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.util.Set;

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
        registerDatatype("file", ACAQFileData.class, ResourceUtils.getPluginResource("icons/data-types/file.png"),
                FilesystemDataSlotRowUI.class, new FilesystemDataSlotCellUI());
        registerDatatype("folder", ACAQFolderData.class, ResourceUtils.getPluginResource("icons/data-types/folder.png"),
                FilesystemDataSlotRowUI.class, new FilesystemDataSlotCellUI());

        registerAlgorithm("import-file", ACAQFileDataSource.class);
        registerAlgorithm("import-file-list", ACAQFileListDataSource.class);
        registerAlgorithm("import-folder", ACAQFolderDataSource.class);
        registerAlgorithm("import-folder-list", ACAQFolderListDataSource.class);

        registerAlgorithm("file-filter", ACAQFilterFiles.class);
        registerAlgorithm("folder-filter", ACAQFilterFolders.class);
        registerAlgorithm("folder-list-files", ACAQListFiles.class);
        registerAlgorithm("folder-list-subfolders", ACAQListSubfolders.class);
        registerAlgorithm("folder-navigate-subfolders", ACAQSubFolder.class);

        registerAlgorithm("folder-annotate-by-name", ACAQFolderAnnotationGenerator.class);
        registerAlgorithm("file-annotate-by-name", ACAQFileAnnotationGenerator.class);

        registerAlgorithmResources();
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
