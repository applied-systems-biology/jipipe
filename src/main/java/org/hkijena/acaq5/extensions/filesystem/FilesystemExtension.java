package org.hkijena.acaq5.extensions.filesystem;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.acaq5.ACAQDefaultRegistry;
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
    public void register(ACAQDefaultRegistry registryService) {
        registryService.getDatatypeRegistry().register("file", ACAQFileData.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQFileData.class,
                ResourceUtils.getPluginResource("icons/data-types/file.png"));
        registryService.getDatatypeRegistry().register("folder", ACAQFolderData.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQFolderData.class,
                ResourceUtils.getPluginResource("icons/data-types/folder.png"));

        registryService.getAlgorithmRegistry().register(ACAQFileDataSource.class);
        registryService.getAlgorithmRegistry().register(ACAQFileListDataSource.class);
        registryService.getAlgorithmRegistry().register(ACAQFolderDataSource.class);
        registryService.getAlgorithmRegistry().register(ACAQFolderListDataSource.class);

        registryService.getAlgorithmRegistry().register(ACAQFilterFiles.class);
        registryService.getAlgorithmRegistry().register(ACAQFilterFolders.class);
        registryService.getAlgorithmRegistry().register(ACAQListFiles.class);
        registryService.getAlgorithmRegistry().register(ACAQListSubfolders.class);
        registryService.getAlgorithmRegistry().register(ACAQSubFolder.class);

        registryService.getAlgorithmRegistry().register(ACAQFolderAnnotationGenerator.class);
        registryService.getAlgorithmRegistry().register(ACAQFileAnnotationGenerator.class);

        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQFileData.class, FilesystemDataSlotRowUI.class);
        registryService.getUIDatatypeRegistry().registerResultTableCellUI(ACAQFileData.class, new FilesystemDataSlotCellUI());
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQFolderData.class, FilesystemDataSlotRowUI.class);
        registryService.getUIDatatypeRegistry().registerResultTableCellUI(ACAQFolderData.class, new FilesystemDataSlotCellUI());

        registerAlgorithmResources(registryService);
    }

    private void registerAlgorithmResources(ACAQDefaultRegistry registryService) {
        Set<String> algorithmFiles = ResourceUtils.walkInternalResourceFolder("extensions/filesystem/api/algorithms");
        for (String resourceFile : algorithmFiles) {
            try {
                JsonNode node = JsonUtils.getObjectMapper().readValue(ResourceUtils.class.getResource(resourceFile), JsonNode.class);
                GraphWrapperAlgorithmRegistrationTask task = new GraphWrapperAlgorithmRegistrationTask(node);
                registryService.getAlgorithmRegistry().scheduleRegister(task);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
