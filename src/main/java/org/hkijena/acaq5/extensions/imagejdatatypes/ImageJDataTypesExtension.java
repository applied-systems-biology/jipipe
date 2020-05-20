package org.hkijena.acaq5.extensions.imagejdatatypes;

import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.imagejdatatypes.algorithms.ImageTypeConverter;
import org.hkijena.acaq5.extensions.imagejdatatypes.algorithms.ImplicitImageTypeConverter;
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
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color.ImagePlusColor8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color.ImagePlusColorData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
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
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.fft.*;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.*;
import org.hkijena.acaq5.extensions.imagejdatatypes.resultanalysis.ImageDataSlotRowUI;
import org.hkijena.acaq5.extensions.imagejdatatypes.resultanalysis.ROIDataSlotRowUI;
import org.hkijena.acaq5.extensions.imagejdatatypes.resultanalysis.ResultsTableDataSlotRowUI;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides ImageJ data types
 */
@Plugin(type = ACAQJavaExtension.class)
public class ImageJDataTypesExtension extends ACAQPrepackagedDefaultJavaExtension {

    public static final List<Class<? extends ACAQData>> IMAGE_TYPES_GREYSCALE = Arrays.asList(
            ImagePlusGreyscaleData.class, ImagePlusGreyscale32FData.class, ImagePlusGreyscaleMaskData.class, ImagePlusGreyscale8UData.class, ImagePlusGreyscale8UData.class, ImagePlusGreyscale16UData.class,
            ImagePlus2DGreyscaleData.class, ImagePlus2DGreyscale32FData.class, ImagePlus2DGreyscaleMaskData.class, ImagePlus2DGreyscale8UData.class, ImagePlus2DGreyscale8UData.class, ImagePlus2DGreyscale16UData.class,
            ImagePlus3DGreyscaleData.class, ImagePlus3DGreyscale32FData.class, ImagePlus3DGreyscaleMaskData.class, ImagePlus3DGreyscale8UData.class, ImagePlus3DGreyscale8UData.class, ImagePlus3DGreyscale16UData.class,
            ImagePlus4DGreyscaleData.class, ImagePlus4DGreyscale32FData.class, ImagePlus4DGreyscaleMaskData.class, ImagePlus4DGreyscale8UData.class, ImagePlus4DGreyscale8UData.class, ImagePlus4DGreyscale16UData.class,
            ImagePlus5DGreyscaleData.class, ImagePlus5DGreyscale32FData.class, ImagePlus5DGreyscaleMaskData.class, ImagePlus5DGreyscale8UData.class, ImagePlus5DGreyscale8UData.class, ImagePlus5DGreyscale16UData.class);

    public static final List<Class<? extends ACAQData>> IMAGE_TYPES_GREYSCALE_8U = Arrays.asList(
            ImagePlusGreyscale8UData.class,
            ImagePlus2DGreyscale8UData.class,
            ImagePlus3DGreyscale8UData.class,
            ImagePlus4DGreyscale8UData.class,
            ImagePlus5DGreyscale8UData.class);

    public static final List<Class<? extends ACAQData>> IMAGE_TYPES_GREYSCALE_16U = Arrays.asList(
            ImagePlusGreyscale16UData.class,
            ImagePlus2DGreyscale16UData.class,
            ImagePlus3DGreyscale16UData.class,
            ImagePlus4DGreyscale16UData.class,
            ImagePlus5DGreyscale16UData.class);

    public static final List<Class<? extends ACAQData>> IMAGE_TYPES_GREYSCALE_MASK = Arrays.asList(
            ImagePlusGreyscaleMaskData.class,
            ImagePlus2DGreyscaleMaskData.class,
            ImagePlus3DGreyscaleMaskData.class,
            ImagePlus4DGreyscaleMaskData.class,
            ImagePlus5DGreyscaleMaskData.class);

    public static final List<Class<? extends ACAQData>> IMAGE_TYPES_GREYSCALE_32F = Arrays.asList(
            ImagePlusGreyscale32FData.class,
            ImagePlus2DGreyscale32FData.class,
            ImagePlus3DGreyscale32FData.class,
            ImagePlus4DGreyscale32FData.class,
            ImagePlus5DGreyscale32FData.class);

    public static final List<Class<? extends ACAQData>> IMAGE_TYPES_COLOR = Arrays.asList(
            ImagePlusColorData.class, ImagePlusColor8UData.class, ImagePlusColorRGBData.class,
            ImagePlus2DColorData.class, ImagePlus2DColor8UData.class, ImagePlus2DColorRGBData.class,
            ImagePlus3DColorData.class, ImagePlus3DColor8UData.class, ImagePlus3DColorRGBData.class,
            ImagePlus4DColorData.class, ImagePlus4DColor8UData.class, ImagePlus4DColorRGBData.class,
            ImagePlus5DColorData.class, ImagePlus5DColor8UData.class, ImagePlus5DColorRGBData.class);

    public static final List<Class<? extends ACAQData>> IMAGE_TYPES_COLOR_8U = Arrays.asList(
            ImagePlusColor8UData.class,
            ImagePlus2DColor8UData.class,
            ImagePlus3DColor8UData.class,
            ImagePlus4DColor8UData.class,
            ImagePlus5DColor8UData.class);

    public static final List<Class<? extends ACAQData>> IMAGE_TYPES_COLOR_RGB = Arrays.asList(
            ImagePlusColorRGBData.class,
            ImagePlus2DColorRGBData.class,
            ImagePlus3DColorRGBData.class,
            ImagePlus4DColorRGBData.class,
            ImagePlus5DColorRGBData.class);

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
        registerImageDataType("imagej-imgplus-greyscale", ImagePlusGreyscaleData.class, "icons/data-types/imgplus-greyscale.png");
        registerImageDataType("imagej-imgplus-greyscale-8u", ImagePlusGreyscale8UData.class, "icons/data-types/imgplus-greyscale-8u.png");
        registerImageDataType("imagej-imgplus-greyscale-16u", ImagePlusGreyscale16UData.class, "icons/data-types/imgplus-greyscale-16u.png");
        registerImageDataType("imagej-imgplus-greyscale-32f", ImagePlusGreyscale32FData.class, "icons/data-types/imgplus-greyscale-32f.png");
        registerImageDataType("imagej-imgplus-greyscale-mask", ImagePlusGreyscaleMaskData.class, "icons/data-types/imgplus-greyscale-mask.png");
        registerImageDataType("imagej-imgplus-color", ImagePlusColorData.class, "icons/data-types/imgplus-color.png");
        registerImageDataType("imagej-imgplus-color-rgb", ImagePlusColorRGBData.class, "icons/data-types/imgplus-color-rgb.png");
        registerImageDataType("imagej-imgplus-color-8u", ImagePlusColor8UData.class, "icons/data-types/imgplus-color-8u.png");
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
        registerConverters();

        registerDatatype("imagej-roi", ROIData.class, ResourceUtils.getPluginResource("icons/data-types/roi.png"),
                ROIDataSlotRowUI.class, null);
        registerImageJDataAdapter(new ROIDataImageJAdapter(), ROIDataImporterUI.class);
        registerDatatype("imagej-results-table", ResultsTableData.class, ResourceUtils.getPluginResource("icons/data-types/results-table.png"),
                ResultsTableDataSlotRowUI.class, null);
        registerImageJDataAdapter(new ResultsTableDataImageJAdapter(), ResultsTableDataImporterUI.class);

        // Register FFT data types
        registerImageDataType("imagej-imgplus-fft", ImagePlusFFTData.class, "icons/data-types/imgplus-fft.png");
        registerImageDataType("imagej-imgplus-fft-2d", ImagePlusFFT2DData.class, "icons/data-types/imgplus-fft-2d.png");
        registerImageDataType("imagej-imgplus-fft-3d", ImagePlusFFT3DData.class, "icons/data-types/imgplus-fft-3d.png");
        registerImageDataType("imagej-imgplus-fft-4d", ImagePlusFFT4DData.class, "icons/data-types/imgplus-fft-4d.png");
        registerImageDataType("imagej-imgplus-fft-5d", ImagePlusFFT5DData.class, "icons/data-types/imgplus-fft-5d.png");

        // Register data sources
        registerAlgorithm("import-imagej-roi-from-file", ROIDataFromFile.class);
        registerAlgorithm("import-imagej-results-table-from-file", ResultsTableFromFile.class);
        registerAlgorithm("import-imagej-bioformats", BioformatsImporter.class);

        // Register algorithms
        registerAlgorithm("external-imagej-macro", MacroWrapperAlgorithm.class);
        registerAlgorithm("convert-imagej-image", ImageTypeConverter.class);

        // Register parameter editors
        registerEnumParameterType("import-imagej-bioformats:color-mode", BioformatsImporter.ColorMode.class,
                "Color mode", "Available modes");
        registerEnumParameterType("import-imagej-bioformats:order", BioformatsImporter.Order.class,
                "Order", "Available orders");
        registerParameterType("ij-macro-code",
                MacroCode.class,
                MacroCode::new,
                m -> new MacroCode((MacroCode) m),
                "ImageJ macro",
                "An ImageJ macro code",
                MacroParameterEditorUI.class);
    }

    /**
     * Creates following converters:
     * Lower dimensionality to higher dimensionality (e.g. 2D is also 3D data)
     * Between same dimensionality
     */
    private void registerConverters() {
        Set<Class<? extends ACAQData>> dataTypes = getRegistry().getDatatypeRegistry().getRegisteredDataTypes().values()
                .stream().filter(ImagePlusData.class::isAssignableFrom).collect(Collectors.toSet());
        Map<Integer, List<Class<? extends ACAQData>>> groupedByDimensionality =
                dataTypes.stream().collect(Collectors.groupingBy(d -> (Integer) ImagePlusData.getDimensionalityOf((Class<? extends ImagePlusData>) d)));
        // Create converters within the same dimension
        for (Map.Entry<Integer, List<Class<? extends ACAQData>>> entry : groupedByDimensionality.entrySet()) {
            int dimensionalityHere = entry.getKey();
            List<Class<? extends ACAQData>> typesHere = entry.getValue();

            for (Map.Entry<Integer, List<Class<? extends ACAQData>>> otherEntry : groupedByDimensionality.entrySet()) {
                if (otherEntry.getKey() >= dimensionalityHere || otherEntry.getKey() == -1) {
                    for (Class<? extends ACAQData> inputClass : typesHere) {
                        for (Class<? extends ACAQData> outputClass : otherEntry.getValue()) {
                            if (!ACAQDatatypeRegistry.isTriviallyConvertible(inputClass, outputClass)) {
                                ImplicitImageTypeConverter converter = new ImplicitImageTypeConverter(inputClass, outputClass);
                                registerDatatypeConversion(converter);
                            }
                        }
                    }
                }
            }
        }
    }

    private void registerImageDataType(String id, Class<? extends ImagePlusData> dataClass, String iconResource) {
        registerDatatype(id, dataClass, ResourceUtils.getPluginResource(iconResource), ImageDataSlotRowUI.class, null);
        registerImageJDataAdapter(new ImgPlusDataImageJAdapter(dataClass), ImagePlusDataImporterUI.class);
        ImagePlusFromFileAlgorithmDeclaration importerDeclaration = new ImagePlusFromFileAlgorithmDeclaration(id, dataClass);
        registerAlgorithm(importerDeclaration);
    }
}


