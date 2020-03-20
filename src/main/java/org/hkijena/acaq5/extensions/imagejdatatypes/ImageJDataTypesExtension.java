package org.hkijena.acaq5.extensions.imagejdatatypes;

import org.hkijena.acaq5.ACAQExtensionService;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datasources.BioformatsImporter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datasources.ImagePlusFromFileAlgorithmDeclaration;
import org.hkijena.acaq5.extensions.imagejdatatypes.datasources.ROIDataFromFile;
import org.hkijena.acaq5.extensions.imagejdatatypes.datasources.ResultsTableFromFile;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ACAQROIData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ACAQResultsTableData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColor8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorRGBData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.*;
import org.hkijena.acaq5.extensions.imagejdatatypes.resultanalysis.ImageDataSlotRowUI;
import org.hkijena.acaq5.extensions.imagejdatatypes.resultanalysis.ROIDataSlotRowUI;
import org.hkijena.acaq5.extensions.imagejdatatypes.resultanalysis.ResultsTableDataSlotRowUI;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.macro.MacroWrapperAlgorithm.IMAGEJ_DATA_CLASSES;

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
        registerImageDataType(registryService, "imagej-imgplus", ImagePlusData.class, "icons/data-types/imgplus.png");
        registerImageDataType(registryService, "imagej-imgplus-2d", ImagePlus2DData.class, "icons/data-types/imgplus-2d.png");
        registerImageDataType(registryService, "imagej-imgplus-2d-greyscale", ImagePlus2DGreyscaleData.class, "icons/data-types/imgplus-2d-greyscale.png");
        registerImageDataType(registryService, "imagej-imgplus-2d-greyscale-8u", ImagePlus2DGreyscale8UData.class, "icons/data-types/imgplus-2d-greyscale-8u.png");
        registerImageDataType(registryService, "imagej-imgplus-2d-greyscale-16u", ImagePlus2DGreyscale16UData.class, "icons/data-types/imgplus-2d-greyscale-16u.png");
        registerImageDataType(registryService, "imagej-imgplus-2d-greyscale-32f", ImagePlus2DGreyscale32FData.class, "icons/data-types/imgplus-2d-greyscale-32f.png");
        registerImageDataType(registryService, "imagej-imgplus-2d-greyscale-mask", ImagePlus2DGreyscaleMaskData.class, "icons/data-types/imgplus-2d-greyscale-mask.png");
        registerImageDataType(registryService, "imagej-imgplus-2d-color", ImagePlus2DColorData.class, "icons/data-types/imgplus-2d-color.png");
        registerImageDataType(registryService, "imagej-imgplus-2d-color-rgb", ImagePlus2DColorRGBData.class, "icons/data-types/imgplus-2d-color-rgb.png");
        registerImageDataType(registryService, "imagej-imgplus-2d-color-8u", ImagePlus2DColor8UData.class, "icons/data-types/imgplus-2d-color-8u.png");

        registryService.getDatatypeRegistry().register("imagej-roi", ACAQROIData.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQROIData.class, ResourceUtils.getPluginResource("icons/data-types/roi.png"));
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQROIData.class, ROIDataSlotRowUI.class);

        registryService.getDatatypeRegistry().register("imagej-results-table", ACAQResultsTableData.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQResultsTableData.class, ResourceUtils.getPluginResource("icons/data-types/results-table.png"));
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQResultsTableData.class, ResultsTableDataSlotRowUI.class);

        // Register data sources
        registryService.getAlgorithmRegistry().register(ROIDataFromFile.class);
        registryService.getAlgorithmRegistry().register(ResultsTableFromFile.class);
        registryService.getAlgorithmRegistry().register(BioformatsImporter.class);

        // Register result data slot UIs
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQROIData.class, ROIDataSlotRowUI.class);
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQResultsTableData.class, ResultsTableDataSlotRowUI.class);
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQResultsTableData.class, ResultsTableDataSlotRowUI.class);

    }

    private void registerImageDataType(ACAQRegistryService registryService, String id, Class<? extends ACAQData> dataClass, String iconResource) {
        IMAGEJ_DATA_CLASSES.add(dataClass);
        registryService.getDatatypeRegistry().register(id, dataClass);
        registryService.getUIDatatypeRegistry().registerIcon(dataClass,
                ResourceUtils.getPluginResource(iconResource));
        registryService.getUIDatatypeRegistry().registerResultSlotUI(dataClass,
                ImageDataSlotRowUI.class);
        ImagePlusFromFileAlgorithmDeclaration importerDeclaration = new ImagePlusFromFileAlgorithmDeclaration(id, dataClass);
        registryService.getAlgorithmRegistry().register(importerDeclaration);
    }
}


