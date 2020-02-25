package org.hkijena.acaq5.api.batchimporter;

import org.hkijena.acaq5.ACAQExtensionService;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.batchimporter.algorithms.*;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFileDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFilesDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFolderDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFoldersDataSlot;
import org.hkijena.acaq5.api.batchimporter.datasources.ACAQFileDataSource;
import org.hkijena.acaq5.api.batchimporter.datasources.ACAQFolderDataSource;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFileData;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFilesData;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFolderData;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFoldersData;
import org.hkijena.acaq5.api.batchimporter.traits.ProjectSampleTrait;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Plugin(type = ACAQExtensionService.class)
public class ACAQBatchImporterExtensionService extends AbstractService implements ACAQExtensionService {
    @Override
    public String getName() {
        return "Batch importer";
    }

    @Override
    public String getDescription() {
        return "Extension data types and algorithms for the batch importer";
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
        registryService.getDatatypeRegistry().register(ACAQFilesData.class, ACAQFilesDataSlot.class);
        registryService.getDatatypeRegistry().register(ACAQFolderData.class, ACAQFolderDataSlot.class);
        registryService.getDatatypeRegistry().register(ACAQFoldersData.class, ACAQFoldersDataSlot.class);

        registryService.getAlgorithmRegistry().register(ACAQFileDataSource.class);
        registryService.getAlgorithmRegistry().register(ACAQFolderDataSource.class);

        registryService.getAlgorithmRegistry().register(ACAQFilterFiles.class);
        registryService.getAlgorithmRegistry().register(ACAQFilterFolders.class);
        registryService.getAlgorithmRegistry().register(ACAQSelectFile.class);
        registryService.getAlgorithmRegistry().register(ACAQSelectFolder.class);
        registryService.getAlgorithmRegistry().register(ACAQListFiles.class);
        registryService.getAlgorithmRegistry().register(ACAQListSubfolders.class);
        registryService.getAlgorithmRegistry().register(ACAQSubFolder.class);

        registryService.getTraitRegistry().register(ProjectSampleTrait.class);
    }
}
