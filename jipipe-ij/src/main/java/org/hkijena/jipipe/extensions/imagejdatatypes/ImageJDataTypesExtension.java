/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejdatatypes;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.ImagePropertiesToAnnotationAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.ImageTypeConverter;
import org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.ImplicitImageTypeConverter;
import org.hkijena.jipipe.extensions.imagejdatatypes.compat.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datasources.BioFormatsImporter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datasources.ImagePlusFromFileNodeInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.datasources.ROIDataFromFile;
import org.hkijena.jipipe.extensions.imagejdatatypes.datasources.ResultsTableFromFile;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColor8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColor8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColor8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.ImagePlus4DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColor8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.greyscale.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.ImagePlus5DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColor8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.greyscale.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.fft.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis.ImageDataSlotRowUI;
import org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis.ROIDataSlotRowUI;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.tables.ResultsTableDataSlotRowUI;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides ImageJ data types
 */
@Plugin(type = JIPipeJavaExtension.class)
public class ImageJDataTypesExtension extends JIPipePrepackagedDefaultJavaExtension {

    public static final List<Class<? extends JIPipeData>> IMAGE_TYPES = Arrays.asList(
            ImagePlusData.class, ImagePlus2DData.class, ImagePlus3DData.class, ImagePlus4DData.class, ImagePlus5DData.class,
            ImagePlusColorData.class, ImagePlusColor8UData.class, ImagePlusColorRGBData.class,
            ImagePlus2DColorData.class, ImagePlus2DColor8UData.class, ImagePlus2DColorRGBData.class,
            ImagePlus3DColorData.class, ImagePlus3DColor8UData.class, ImagePlus3DColorRGBData.class,
            ImagePlus4DColorData.class, ImagePlus4DColor8UData.class, ImagePlus4DColorRGBData.class,
            ImagePlus5DColorData.class, ImagePlus5DColor8UData.class, ImagePlus5DColorRGBData.class,
            ImagePlusGreyscaleData.class, ImagePlusGreyscale32FData.class, ImagePlusGreyscaleMaskData.class, ImagePlusGreyscale8UData.class, ImagePlusGreyscale8UData.class, ImagePlusGreyscale16UData.class,
            ImagePlus2DGreyscaleData.class, ImagePlus2DGreyscale32FData.class, ImagePlus2DGreyscaleMaskData.class, ImagePlus2DGreyscale8UData.class, ImagePlus2DGreyscale8UData.class, ImagePlus2DGreyscale16UData.class,
            ImagePlus3DGreyscaleData.class, ImagePlus3DGreyscale32FData.class, ImagePlus3DGreyscaleMaskData.class, ImagePlus3DGreyscale8UData.class, ImagePlus3DGreyscale8UData.class, ImagePlus3DGreyscale16UData.class,
            ImagePlus4DGreyscaleData.class, ImagePlus4DGreyscale32FData.class, ImagePlus4DGreyscaleMaskData.class, ImagePlus4DGreyscale8UData.class, ImagePlus4DGreyscale8UData.class, ImagePlus4DGreyscale16UData.class,
            ImagePlus5DGreyscaleData.class, ImagePlus5DGreyscale32FData.class, ImagePlus5DGreyscaleMaskData.class, ImagePlus5DGreyscale8UData.class, ImagePlus5DGreyscale8UData.class, ImagePlus5DGreyscale16UData.class);

    public static final List<Class<? extends JIPipeData>> IMAGE_TYPES_GREYSCALE = Arrays.asList(
            ImagePlusGreyscaleData.class, ImagePlusGreyscale32FData.class, ImagePlusGreyscaleMaskData.class, ImagePlusGreyscale8UData.class, ImagePlusGreyscale8UData.class, ImagePlusGreyscale16UData.class,
            ImagePlus2DGreyscaleData.class, ImagePlus2DGreyscale32FData.class, ImagePlus2DGreyscaleMaskData.class, ImagePlus2DGreyscale8UData.class, ImagePlus2DGreyscale8UData.class, ImagePlus2DGreyscale16UData.class,
            ImagePlus3DGreyscaleData.class, ImagePlus3DGreyscale32FData.class, ImagePlus3DGreyscaleMaskData.class, ImagePlus3DGreyscale8UData.class, ImagePlus3DGreyscale8UData.class, ImagePlus3DGreyscale16UData.class,
            ImagePlus4DGreyscaleData.class, ImagePlus4DGreyscale32FData.class, ImagePlus4DGreyscaleMaskData.class, ImagePlus4DGreyscale8UData.class, ImagePlus4DGreyscale8UData.class, ImagePlus4DGreyscale16UData.class,
            ImagePlus5DGreyscaleData.class, ImagePlus5DGreyscale32FData.class, ImagePlus5DGreyscaleMaskData.class, ImagePlus5DGreyscale8UData.class, ImagePlus5DGreyscale8UData.class, ImagePlus5DGreyscale16UData.class);

    public static final List<Class<? extends JIPipeData>> IMAGE_TYPES_GREYSCALE_8U = Arrays.asList(
            ImagePlusGreyscale8UData.class,
            ImagePlus2DGreyscale8UData.class,
            ImagePlus3DGreyscale8UData.class,
            ImagePlus4DGreyscale8UData.class,
            ImagePlus5DGreyscale8UData.class);

    public static final List<Class<? extends JIPipeData>> IMAGE_TYPES_GREYSCALE_16U = Arrays.asList(
            ImagePlusGreyscale16UData.class,
            ImagePlus2DGreyscale16UData.class,
            ImagePlus3DGreyscale16UData.class,
            ImagePlus4DGreyscale16UData.class,
            ImagePlus5DGreyscale16UData.class);

    public static final List<Class<? extends JIPipeData>> IMAGE_TYPES_GREYSCALE_MASK = Arrays.asList(
            ImagePlusGreyscaleMaskData.class,
            ImagePlus2DGreyscaleMaskData.class,
            ImagePlus3DGreyscaleMaskData.class,
            ImagePlus4DGreyscaleMaskData.class,
            ImagePlus5DGreyscaleMaskData.class);

    public static final List<Class<? extends JIPipeData>> IMAGE_TYPES_GREYSCALE_32F = Arrays.asList(
            ImagePlusGreyscale32FData.class,
            ImagePlus2DGreyscale32FData.class,
            ImagePlus3DGreyscale32FData.class,
            ImagePlus4DGreyscale32FData.class,
            ImagePlus5DGreyscale32FData.class);

    public static final List<Class<? extends JIPipeData>> IMAGE_TYPES_COLOR = Arrays.asList(
            ImagePlusColorData.class, ImagePlusColor8UData.class, ImagePlusColorRGBData.class,
            ImagePlus2DColorData.class, ImagePlus2DColor8UData.class, ImagePlus2DColorRGBData.class,
            ImagePlus3DColorData.class, ImagePlus3DColor8UData.class, ImagePlus3DColorRGBData.class,
            ImagePlus4DColorData.class, ImagePlus4DColor8UData.class, ImagePlus4DColorRGBData.class,
            ImagePlus5DColorData.class, ImagePlus5DColor8UData.class, ImagePlus5DColorRGBData.class);

    public static final List<Class<? extends JIPipeData>> IMAGE_TYPES_COLOR_8U = Arrays.asList(
            ImagePlusColor8UData.class,
            ImagePlus2DColor8UData.class,
            ImagePlus3DColor8UData.class,
            ImagePlus4DColor8UData.class,
            ImagePlus5DColor8UData.class);

    public static final List<Class<? extends JIPipeData>> IMAGE_TYPES_COLOR_RGB = Arrays.asList(
            ImagePlusColorRGBData.class,
            ImagePlus2DColorRGBData.class,
            ImagePlus3DColorRGBData.class,
            ImagePlus4DColorRGBData.class,
            ImagePlus5DColorRGBData.class);

    @Override
    public StringList getDependencyCitations() {
        StringList result = new StringList();
        result.add("Rueden, C. T.; Schindelin, J. & Hiner, M. C. et al. (2017), \"ImageJ2: ImageJ for the next generation of scientific image data\", " +
                "BMC Bioinformatics 18:529");
        result.add("Schneider, C. A.; Rasband, W. S. & Eliceiri, K. W. (2012), \"NIH Image to ImageJ: 25 years of image analysis\", " +
                "Nature methods 9(7): 671-675");
        result.add("Melissa Linkert, Curtis T. Rueden, Chris Allan, Jean-Marie Burel, Will Moore, Andrew Patterson, Brian Loranger, Josh Moore, " +
                "Carlos Neves, Donald MacDonald, Aleksandra Tarkowska, Caitlin Sticco, Emma Hill, Mike Rossner, Kevin W. Eliceiri, " +
                "and Jason R. Swedlow (2010) Metadata matters: access to image data in the real world. The Journal of Cell Biology 189(5), 777-782");
        return result;
    }

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
        return "org.hkijena.jipipe:imagej-integration";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }

    @Override
    public void register() {
        registerSettingsSheet(ImageJDataTypesSettings.ID,
                "ImageJ data types",
                "Extensions",
                UIUtils.getIconFromResources("actions/plugins.png"),
                new ImageJDataTypesSettings());
        registerEnumParameterType("ome-tiff-compression",
                OMETIFFCompression.class,
                "OME TIFF Compression",
                "Available compression algorithms");

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

        registerDatatype("imagej-roi", ROIListData.class, ResourceUtils.getPluginResource("icons/data-types/roi.png"),
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
        registerNodeType("import-imagej-roi-from-file", ROIDataFromFile.class);
        registerNodeType("import-imagej-results-table-from-file", ResultsTableFromFile.class);
        registerNodeType("import-imagej-bioformats", BioFormatsImporter.class, UIUtils.getIconURLFromResources("apps/bioformats.png"));

        // Register algorithms
        registerNodeType("convert-imagej-image", ImageTypeConverter.class, UIUtils.getIconURLFromResources("actions/viewimage.png"));
        registerNodeType("image-properties-to-annotation", ImagePropertiesToAnnotationAlgorithm.class, UIUtils.getIconURLFromResources("data-types/annotation-table.png"));

        // Register parameter editors
        registerEnumParameterType("import-imagej-bioformats:color-mode", BioFormatsImporter.ColorMode.class,
                "Color mode", "Available modes");
        registerEnumParameterType("import-imagej-bioformats:order", BioFormatsImporter.Order.class,
                "Order", "Available orders");

    }

    /**
     * Creates following converters:
     * Lower dimensionality to higher dimensionality (e.g. 2D is also 3D data)
     * Between same dimensionality
     */
    private void registerConverters() {
        Set<Class<? extends JIPipeData>> dataTypes = getRegistry().getDatatypeRegistry().getRegisteredDataTypes().values()
                .stream().filter(ImagePlusData.class::isAssignableFrom).collect(Collectors.toSet());
        Map<Integer, List<Class<? extends JIPipeData>>> groupedByDimensionality =
                dataTypes.stream().collect(Collectors.groupingBy(d -> (Integer) ImagePlusData.getDimensionalityOf((Class<? extends ImagePlusData>) d)));
        // Create converters within the same dimension
        for (Map.Entry<Integer, List<Class<? extends JIPipeData>>> entry : groupedByDimensionality.entrySet()) {
            int dimensionalityHere = entry.getKey();
            List<Class<? extends JIPipeData>> typesHere = entry.getValue();

            for (Map.Entry<Integer, List<Class<? extends JIPipeData>>> otherEntry : groupedByDimensionality.entrySet()) {
                if (otherEntry.getKey() >= dimensionalityHere || otherEntry.getKey() == -1) {
                    for (Class<? extends JIPipeData> inputClass : typesHere) {
                        for (Class<? extends JIPipeData> outputClass : otherEntry.getValue()) {
                            if (!JIPipeDatatypeRegistry.isTriviallyConvertible(inputClass, outputClass)) {
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
        ImagePlusFromFileNodeInfo info = new ImagePlusFromFileNodeInfo(id, dataClass);
        registerNodeType(info);
    }
}


