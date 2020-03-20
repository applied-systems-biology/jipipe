package org.hkijena.acaq5.extensions.filesystem;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.acaq5.ACAQExtensionService;
import org.hkijena.acaq5.ACAQRegistryService;
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
import org.scijava.service.AbstractService;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Plugin(type = ACAQExtensionService.class)
public class FilesystemExtension extends AbstractService implements ACAQExtensionService {
    @Override
    public String getName() {
        return "Filesystem";
    }

    @Override
    public String getDescription() {
        return "Basic types for filesystem handling";
    }

    @Override
    public List<String> getAuthors() {
        return Arrays.asList("Zoltán Cseresnyés", "Ruman Gerst");
    }

    @Override
    public String getURL() {
        return "https://applied-systems-biology.github.io/acaq5/";
    }

    @Override
    public String getLicense() {
        return "BSD-2";
    }

    @Override
    public URL getIconURL() {
        return ResourceUtils.getPluginResource("logo-400.png");
    }

    @Override
    public void register(ACAQRegistryService registryService) {
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

    private void registerAlgorithmResources(ACAQRegistryService registryService) {
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
