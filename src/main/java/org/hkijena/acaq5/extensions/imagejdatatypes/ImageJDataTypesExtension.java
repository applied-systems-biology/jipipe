package org.hkijena.acaq5.extensions.imagejdatatypes;

import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
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

import static org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.macro.MacroWrapperAlgorithm.IMAGEJ_DATA_CLASSES;

@Plugin(type = ACAQJavaExtension.class)
public class ImageJDataTypesExtension extends ACAQPrepackagedDefaultJavaExtension {

    @Override
    public String getName() {
        return "ImageJ integration";
    }

    @Override
    public String getDescription() {
        return "Adds support for commonly used ImageJ data types";
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.acaq5:imagej-integration";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }

    @Override
    public void register() {
        // Register data types
        registerImageDataType("imagej-imgplus", ImagePlusData.class, "icons/data-types/imgplus.png");
        registerImageDataType("imagej-imgplus-2d", ImagePlus2DData.class, "icons/data-types/imgplus-2d.png");
        registerImageDataType("imagej-imgplus-2d-greyscale", ImagePlus2DGreyscaleData.class, "icons/data-types/imgplus-2d-greyscale.png");
        registerImageDataType("imagej-imgplus-2d-greyscale-8u", ImagePlus2DGreyscale8UData.class, "icons/data-types/imgplus-2d-greyscale-8u.png");
        registerImageDataType("imagej-imgplus-2d-greyscale-16u", ImagePlus2DGreyscale16UData.class, "icons/data-types/imgplus-2d-greyscale-16u.png");
        registerImageDataType("imagej-imgplus-2d-greyscale-32f", ImagePlus2DGreyscale32FData.class, "icons/data-types/imgplus-2d-greyscale-32f.png");
        registerImageDataType("imagej-imgplus-2d-greyscale-mask", ImagePlus2DGreyscaleMaskData.class, "icons/data-types/imgplus-2d-greyscale-mask.png");
        registerImageDataType("imagej-imgplus-2d-color", ImagePlus2DColorData.class, "icons/data-types/imgplus-2d-color.png");
        registerImageDataType("imagej-imgplus-2d-color-rgb", ImagePlus2DColorRGBData.class, "icons/data-types/imgplus-2d-color-rgb.png");
        registerImageDataType("imagej-imgplus-2d-color-8u", ImagePlus2DColor8UData.class, "icons/data-types/imgplus-2d-color-8u.png");

        registerDatatype("imagej-roi", ACAQROIData.class, ResourceUtils.getPluginResource("icons/data-types/roi.png"),
                ROIDataSlotRowUI.class, null);
        registerDatatype("imagej-results-table", ACAQResultsTableData.class, ResourceUtils.getPluginResource("icons/data-types/results-table.png"),
                ResultsTableDataSlotRowUI.class, null);

        // Register data sources
        registerAlgorithm("import-imagej-roi-from-file", ROIDataFromFile.class);
        registerAlgorithm("import-imagej-results-table-from-file", ResultsTableFromFile.class);
        registerAlgorithm("import-imagej-bioformats", BioformatsImporter.class);
    }

    private void registerImageDataType(String id, Class<? extends ACAQData> dataClass, String iconResource) {
        IMAGEJ_DATA_CLASSES.add(dataClass);
        registerDatatype(id, dataClass, ResourceUtils.getPluginResource(iconResource), ImageDataSlotRowUI.class, null);
        ImagePlusFromFileAlgorithmDeclaration importerDeclaration = new ImagePlusFromFileAlgorithmDeclaration(id, dataClass);
        registerAlgorithm(importerDeclaration);
    }
}


