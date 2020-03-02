package org.hkijena.acaq5.filesystem;

import org.hkijena.acaq5.ACAQExtensionService;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.filesystem.api.algorithms.*;
import org.hkijena.acaq5.filesystem.api.annotation.ACAQFolderAnnotationGenerator;
import org.hkijena.acaq5.filesystem.api.datasources.ACAQFileDataSource;
import org.hkijena.acaq5.filesystem.api.datasources.ACAQFolderDataSource;
import org.hkijena.acaq5.filesystem.api.dataypes.ACAQFileData;
import org.hkijena.acaq5.filesystem.api.dataypes.ACAQFolderData;
import org.hkijena.acaq5.filesystem.ui.resultanalysis.FilesystemDataSlotCellUI;
import org.hkijena.acaq5.filesystem.ui.resultanalysis.FilesystemDataSlotRowUI;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Plugin(type = ACAQExtensionService.class)
public class ACAQFilesystemExtensionService extends AbstractService implements ACAQExtensionService {
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
        registryService.getDatatypeRegistry().register(ACAQFileData.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQFileData.class,
                ResourceUtils.getPluginResource("icons/data-types/file.png"));
        registryService.getDatatypeRegistry().register(ACAQFolderData.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQFolderData.class,
                ResourceUtils.getPluginResource("icons/data-types/folder.png"));

        registryService.getAlgorithmRegistry().register(ACAQFileDataSource.class);
        registryService.getAlgorithmRegistry().register(ACAQFolderDataSource.class);

        registryService.getAlgorithmRegistry().register(ACAQFilterFiles.class);
        registryService.getAlgorithmRegistry().register(ACAQFilterFolders.class);
        registryService.getAlgorithmRegistry().register(ACAQListFiles.class);
        registryService.getAlgorithmRegistry().register(ACAQListSubfolders.class);
        registryService.getAlgorithmRegistry().register(ACAQSubFolder.class);

        registryService.getAlgorithmRegistry().register(ACAQFolderAnnotationGenerator.class);

        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQFileData.class, FilesystemDataSlotRowUI.class);
        registryService.getUIDatatypeRegistry().registerResultTableCellUI(ACAQFileData.class, new FilesystemDataSlotCellUI());
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQFolderData.class, FilesystemDataSlotRowUI.class);
        registryService.getUIDatatypeRegistry().registerResultTableCellUI(ACAQFolderData.class, new FilesystemDataSlotCellUI());
    }
}
