package org.hkijena.acaq5.api.batchimporter;

import org.hkijena.acaq5.ACAQExtensionService;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQBatchImporterFileDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQBatchImporterFolderDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQBatchImporterFileData;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQBatchImporterFolderData;
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
        registryService.getDatatypeRegistry().register(ACAQBatchImporterFileData.class, ACAQBatchImporterFileDataSlot.class);
        registryService.getDatatypeRegistry().register(ACAQBatchImporterFolderData.class, ACAQBatchImporterFolderDataSlot.class);
    }
}
