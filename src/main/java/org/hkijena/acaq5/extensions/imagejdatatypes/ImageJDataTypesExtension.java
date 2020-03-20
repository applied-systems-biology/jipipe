package org.hkijena.acaq5.extensions.imagejdatatypes;

import org.hkijena.acaq5.ACAQExtensionService;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.extensions.imagejdatatypes.datasources.*;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.*;
import org.hkijena.acaq5.extensions.imagejdatatypes.resultanalysis.ImageDataSlotRowUI;
import org.hkijena.acaq5.extensions.imagejdatatypes.resultanalysis.ROIDataSlotRowUI;
import org.hkijena.acaq5.extensions.imagejdatatypes.resultanalysis.ResultsTableDataSlotRowUI;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Plugin(type = ACAQExtensionService.class)
public class ImageJDataTypesExtension extends AbstractService implements ACAQExtensionService {
    @Override
    public String getName() {
        return "ImageJ data types";
    }

    @Override
    public String getDescription() {
        return "Provides support for ImageJ data types";
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
        // Register data types
        registryService.getDatatypeRegistry().register("imagej-imgplus-greyscale", ACAQGreyscaleImageData.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQGreyscaleImageData.class,
                ResourceUtils.getPluginResource("icons/data-types/greyscale.png"));
        registryService.getDatatypeRegistry().register("imagej-imgplus-mask", ACAQMaskData.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQMaskData.class,
                ResourceUtils.getPluginResource("icons/data-types/binary.png"));
        registryService.getDatatypeRegistry().register("imagej-roi", ACAQROIData.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQROIData.class,
                ResourceUtils.getPluginResource("icons/data-types/roi.png"));
        registryService.getDatatypeRegistry().register("imagej-imgplus", ACAQMultichannelImageData.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQMultichannelImageData.class,
                ResourceUtils.getPluginResource("icons/data-types/multichannel.png"));
        registryService.getDatatypeRegistry().register("imagej-resultstable", ACAQResultsTableData.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQResultsTableData.class,
                ResourceUtils.getPluginResource("icons/data-types/results-table.png"));

        // Register data sources
        registryService.getAlgorithmRegistry().register(ACAQGreyscaleImageDataFromFile.class);
        registryService.getAlgorithmRegistry().register(ACAQMaskImageDataFromFile.class);
        registryService.getAlgorithmRegistry().register(ACAQROIDataFromFile.class);
        registryService.getAlgorithmRegistry().register(ACAQMultichannelImageDataFromFile.class);
        registryService.getAlgorithmRegistry().register(ACAQResultsTableFromFile.class);
        registryService.getAlgorithmRegistry().register(ACAQBioformatsImporter.class);

        // Register result data slot UIs
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQMultichannelImageData.class, ImageDataSlotRowUI.class);
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQGreyscaleImageData.class, ImageDataSlotRowUI.class);
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQMaskData.class, ImageDataSlotRowUI.class);
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQROIData.class, ROIDataSlotRowUI.class);
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQResultsTableData.class, ResultsTableDataSlotRowUI.class);
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQResultsTableData.class, ResultsTableDataSlotRowUI.class);

    }
}
