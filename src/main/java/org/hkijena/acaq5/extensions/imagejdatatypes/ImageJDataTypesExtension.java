package org.hkijena.acaq5.extensions.imagejdatatypes;

import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.imagejdatatypes.compat.ImgPlusDataImageJAdapter;
import org.hkijena.acaq5.extensions.imagejdatatypes.compat.ROIDataImageJAdapter;
import org.hkijena.acaq5.extensions.imagejdatatypes.compat.ResultsTableDataImageJAdapter;
import org.hkijena.acaq5.extensions.imagejdatatypes.compat.importers.ImagePlusDataImporterUI;
import org.hkijena.acaq5.extensions.imagejdatatypes.compat.importers.ROIDataImporterUI;
import org.hkijena.acaq5.extensions.imagejdatatypes.compat.importers.ResultsTableDataImporterUI;
import org.hkijena.acaq5.extensions.imagejdatatypes.datasources.BioformatsImporter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datasources.ImagePlusFromFileAlgorithmDeclaration;
import org.hkijena.acaq5.extensions.imagejdatatypes.datasources.ROIDataFromFile;
import org.hkijena.acaq5.extensions.imagejdatatypes.datasources.ResultsTableFromFile;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColor8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorRGBData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.*;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColor8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorRGBData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.greyscale.*;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.ImagePlus4DData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColor8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColorData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColorRGBData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.greyscale.*;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.ImagePlus5DData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColor8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColorData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColorRGBData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.greyscale.*;
import org.hkijena.acaq5.extensions.imagejdatatypes.resultanalysis.ImageDataSlotRowUI;
import org.hkijena.acaq5.extensions.imagejdatatypes.resultanalysis.ROIDataSlotRowUI;
import org.hkijena.acaq5.extensions.imagejdatatypes.resultanalysis.ResultsTableDataSlotRowUI;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.plugin.Plugin;

/**
 * Provides ImageJ data types
 */
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
        registerImageDataType("imagej-imgplus-3d", ImagePlus3DData.class, "icons/data-types/imgplus-3d.png");
        registerImageDataType("imagej-imgplus-3d-greyscale", ImagePlus3DGreyscaleData.class, "icons/data-types/imgplus-3d-greyscale.png");
        registerImageDataType("imagej-imgplus-3d-greyscale-8u", ImagePlus3DGreyscale8UData.class, "icons/data-types/imgplus-3d-greyscale-8u.png");
        registerImageDataType("imagej-imgplus-3d-greyscale-16u", ImagePlus3DGreyscale16UData.class, "icons/data-types/imgplus-3d-greyscale-16u.png");
        registerImageDataType("imagej-imgplus-3d-greyscale-32f", ImagePlus3DGreyscale32FData.class, "icons/data-types/imgplus-3d-greyscale-32f.png");
        registerImageDataType("imagej-imgplus-3d-greyscale-mask", ImagePlus3DGreyscaleMaskData.class, "icons/data-types/imgplus-3d-greyscale-mask.png");
        registerImageDataType("imagej-imgplus-3d-color", ImagePlus3DColorData.class, "icons/data-types/imgplus-3d-color.png");
        registerImageDataType("imagej-imgplus-3d-color-rgb", ImagePlus3DColorRGBData.class, "icons/data-types/imgplus-3d-color-rgb.png");
        registerImageDataType("imagej-imgplus-3d-color-8u", ImagePlus3DColor8UData.class, "icons/data-types/imgplus-3d-color-8u.png");
        registerImageDataType("imagej-imgplus-4d", ImagePlus4DData.class, "icons/data-types/imgplus-4d.png");
        registerImageDataType("imagej-imgplus-4d-greyscale", ImagePlus4DGreyscaleData.class, "icons/data-types/imgplus-4d-greyscale.png");
        registerImageDataType("imagej-imgplus-4d-greyscale-8u", ImagePlus4DGreyscale8UData.class, "icons/data-types/imgplus-4d-greyscale-8u.png");
        registerImageDataType("imagej-imgplus-4d-greyscale-16u", ImagePlus4DGreyscale16UData.class, "icons/data-types/imgplus-4d-greyscale-16u.png");
        registerImageDataType("imagej-imgplus-4d-greyscale-32f", ImagePlus4DGreyscale32FData.class, "icons/data-types/imgplus-4d-greyscale-32f.png");
        registerImageDataType("imagej-imgplus-4d-greyscale-mask", ImagePlus4DGreyscaleMaskData.class, "icons/data-types/imgplus-4d-greyscale-mask.png");
        registerImageDataType("imagej-imgplus-4d-color", ImagePlus4DColorData.class, "icons/data-types/imgplus-4d-color.png");
        registerImageDataType("imagej-imgplus-4d-color-rgb", ImagePlus4DColorRGBData.class, "icons/data-types/imgplus-4d-color-rgb.png");
        registerImageDataType("imagej-imgplus-4d-color-8u", ImagePlus4DColor8UData.class, "icons/data-types/imgplus-4d-color-8u.png");
        registerImageDataType("imagej-imgplus-5d", ImagePlus5DData.class, "icons/data-types/imgplus-5d.png");
        registerImageDataType("imagej-imgplus-5d-greyscale", ImagePlus5DGreyscaleData.class, "icons/data-types/imgplus-5d-greyscale.png");
        registerImageDataType("imagej-imgplus-5d-greyscale-8u", ImagePlus5DGreyscale8UData.class, "icons/data-types/imgplus-5d-greyscale-8u.png");
        registerImageDataType("imagej-imgplus-5d-greyscale-16u", ImagePlus5DGreyscale16UData.class, "icons/data-types/imgplus-5d-greyscale-16u.png");
        registerImageDataType("imagej-imgplus-5d-greyscale-32f", ImagePlus5DGreyscale32FData.class, "icons/data-types/imgplus-5d-greyscale-32f.png");
        registerImageDataType("imagej-imgplus-5d-greyscale-mask", ImagePlus5DGreyscaleMaskData.class, "icons/data-types/imgplus-5d-greyscale-mask.png");
        registerImageDataType("imagej-imgplus-5d-color", ImagePlus5DColorData.class, "icons/data-types/imgplus-5d-color.png");
        registerImageDataType("imagej-imgplus-5d-color-rgb", ImagePlus5DColorRGBData.class, "icons/data-types/imgplus-5d-color-rgb.png");
        registerImageDataType("imagej-imgplus-5d-color-8u", ImagePlus5DColor8UData.class, "icons/data-types/imgplus-5d-color-8u.png");

        registerDatatype("imagej-roi", ROIData.class, ResourceUtils.getPluginResource("icons/data-types/roi.png"),
                ROIDataSlotRowUI.class, null);
        registerImageJDataAdapter(new ROIDataImageJAdapter(), ROIDataImporterUI.class);
        registerDatatype("imagej-results-table", ResultsTableData.class, ResourceUtils.getPluginResource("icons/data-types/results-table.png"),
                ResultsTableDataSlotRowUI.class, null);
        registerImageJDataAdapter(new ResultsTableDataImageJAdapter(), ResultsTableDataImporterUI.class);

        // Register data sources
        registerAlgorithm("import-imagej-roi-from-file", ROIDataFromFile.class);
        registerAlgorithm("import-imagej-results-table-from-file", ResultsTableFromFile.class);
        registerAlgorithm("import-imagej-bioformats", BioformatsImporter.class);

        // Register algorithms
        registerAlgorithm("external-imagej-macro", MacroWrapperAlgorithm.class);

        // Register parameter editors
        registerParameterType(MacroCode.class, MacroParameterEditorUI.class, "ImageJ macro", "An ImageJ macro code");
    }

    private void registerImageDataType(String id, Class<? extends ImagePlusData> dataClass, String iconResource) {
        registerDatatype(id, dataClass, ResourceUtils.getPluginResource(iconResource), ImageDataSlotRowUI.class, null);
        registerImageJDataAdapter(new ImgPlusDataImageJAdapter(dataClass), ImagePlusDataImporterUI.class);
        ImagePlusFromFileAlgorithmDeclaration importerDeclaration = new ImagePlusFromFileAlgorithmDeclaration(id, dataClass);
        registerAlgorithm(importerDeclaration);
    }
}


