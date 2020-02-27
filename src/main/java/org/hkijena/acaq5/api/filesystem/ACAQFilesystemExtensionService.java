package org.hkijena.acaq5.api.filesystem;

import org.hkijena.acaq5.ACAQExtensionService;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.filesystem.algorithms.*;
import org.hkijena.acaq5.api.filesystem.dataslots.ACAQFileDataSlot;
import org.hkijena.acaq5.api.filesystem.dataslots.ACAQFolderDataSlot;
import org.hkijena.acaq5.api.filesystem.datasources.ACAQFileDataSource;
import org.hkijena.acaq5.api.filesystem.datasources.ACAQFolderDataSource;
import org.hkijena.acaq5.api.filesystem.dataypes.*;
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
        registryService.getDatatypeRegistry().register(ACAQFileData.class, ACAQFileDataSlot.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQFileData.class,
                ResourceUtils.getPluginResource("icons/data-types/file.png"));
        registryService.getDatatypeRegistry().register(ACAQFolderData.class, ACAQFolderDataSlot.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQFolderData.class,
                ResourceUtils.getPluginResource("icons/data-types/folder.png"));

        registryService.getAlgorithmRegistry().register(ACAQFileDataSource.class);
        registryService.getAlgorithmRegistry().register(ACAQFolderDataSource.class);

        registryService.getAlgorithmRegistry().register(ACAQFilterFiles.class);
        registryService.getAlgorithmRegistry().register(ACAQFilterFolders.class);
        registryService.getAlgorithmRegistry().register(ACAQListFiles.class);
        registryService.getAlgorithmRegistry().register(ACAQListSubfolders.class);
        registryService.getAlgorithmRegistry().register(ACAQSubFolder.class);
    }
}
