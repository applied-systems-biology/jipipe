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

import ome.xml.model.enums.DimensionOrder;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.core.data.OpenInNativeApplicationDataImportOperation;
import org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.color.ToHSBColorSpaceConverterAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.color.ToLABColorSpaceConverterAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.color.ToRGBColorSpaceConverterAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.compat.ImagePlusDataImporterUI;
import org.hkijena.jipipe.extensions.imagejdatatypes.compat.ImgPlusDataImageJAdapter;
import org.hkijena.jipipe.extensions.imagejdatatypes.compat.OMEImageDataImageJAdapter;
import org.hkijena.jipipe.extensions.imagejdatatypes.compat.ROIDataImageJAdapter;
import org.hkijena.jipipe.extensions.imagejdatatypes.compat.ROIDataImporterUI;
import org.hkijena.jipipe.extensions.imagejdatatypes.compat.ResultsTableDataImageJAdapter;
import org.hkijena.jipipe.extensions.imagejdatatypes.compat.ResultsTableDataImporterUI;
import org.hkijena.jipipe.extensions.imagejdatatypes.datasources.BioFormatsImporter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datasources.ImagePlusFromFile;
import org.hkijena.jipipe.extensions.imagejdatatypes.datasources.ROIDataFromFile;
import org.hkijena.jipipe.extensions.imagejdatatypes.datasources.ResultsTableFromFile;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.LUTData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorHSBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorLABData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorHSBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorLABData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorHSBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorLABData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.ImagePlus4DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColorHSBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColorLABData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.greyscale.ImagePlus4DGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.greyscale.ImagePlus4DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.greyscale.ImagePlus4DGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.greyscale.ImagePlus4DGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.greyscale.ImagePlus4DGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.ImagePlus5DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColorHSBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColorLABData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.greyscale.ImagePlus5DGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.greyscale.ImagePlus5DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.greyscale.ImagePlus5DGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.greyscale.ImagePlus5DGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.greyscale.ImagePlus5DGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.fft.ImagePlusFFT2DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.fft.ImagePlusFFT3DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.fft.ImagePlusFFT4DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.fft.ImagePlusFFT5DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.fft.ImagePlusFFTData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.display.AddROIToActiveJIPipeImageViewerDataDisplay;
import org.hkijena.jipipe.extensions.imagejdatatypes.display.AddROIToJIPipeImageViewerDataDisplay;
import org.hkijena.jipipe.extensions.imagejdatatypes.display.AddToROIManagerDataDisplay;
import org.hkijena.jipipe.extensions.imagejdatatypes.display.OpenInImageJDataDisplay;
import org.hkijena.jipipe.extensions.imagejdatatypes.parameters.OMEColorMode;
import org.hkijena.jipipe.extensions.imagejdatatypes.parameters.OMETIFFCompression;
import org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.AVICompression;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.BitDepth;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.OptionalBitDepth;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.tables.ResultsTableDataPreview;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.display.CacheAwareOpenResultsTableInJIPipeDataOperation;
import org.hkijena.jipipe.extensions.tables.display.OpenResultsTableInImageJDataOperation;
import org.hkijena.jipipe.extensions.tables.display.OpenResultsTableInJIPipeTabDataOperation;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

import javax.swing.*;
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

    /**
     * All image data types known to this library
     */
    public static final List<Class<? extends JIPipeData>> IMAGE_TYPES = Arrays.asList(
            ImagePlusData.class, ImagePlus2DData.class, ImagePlus3DData.class, ImagePlus4DData.class, ImagePlus5DData.class,
            ImagePlusColorHSBData.class, ImagePlusColorRGBData.class, ImagePlusColorData.class, ImagePlusColorLABData.class,
            ImagePlus2DColorHSBData.class, ImagePlus2DColorRGBData.class, ImagePlus2DColorData.class, ImagePlus2DColorLABData.class,
            ImagePlus3DColorHSBData.class, ImagePlus3DColorRGBData.class, ImagePlus3DColorData.class, ImagePlus3DColorLABData.class,
            ImagePlus4DColorHSBData.class, ImagePlus4DColorRGBData.class, ImagePlus4DColorData.class, ImagePlus4DColorLABData.class,
            ImagePlus5DColorHSBData.class, ImagePlus5DColorRGBData.class, ImagePlus5DColorData.class, ImagePlus5DColorLABData.class,
            ImagePlusGreyscaleData.class, ImagePlusGreyscale32FData.class, ImagePlusGreyscaleMaskData.class, ImagePlusGreyscale8UData.class, ImagePlusGreyscale8UData.class, ImagePlusGreyscale16UData.class,
            ImagePlus2DGreyscaleData.class, ImagePlus2DGreyscale32FData.class, ImagePlus2DGreyscaleMaskData.class, ImagePlus2DGreyscale8UData.class, ImagePlus2DGreyscale8UData.class, ImagePlus2DGreyscale16UData.class,
            ImagePlus3DGreyscaleData.class, ImagePlus3DGreyscale32FData.class, ImagePlus3DGreyscaleMaskData.class, ImagePlus3DGreyscale8UData.class, ImagePlus3DGreyscale8UData.class, ImagePlus3DGreyscale16UData.class,
            ImagePlus4DGreyscaleData.class, ImagePlus4DGreyscale32FData.class, ImagePlus4DGreyscaleMaskData.class, ImagePlus4DGreyscale8UData.class, ImagePlus4DGreyscale8UData.class, ImagePlus4DGreyscale16UData.class,
            ImagePlus5DGreyscaleData.class, ImagePlus5DGreyscale32FData.class, ImagePlus5DGreyscaleMaskData.class, ImagePlus5DGreyscale8UData.class, ImagePlus5DGreyscale8UData.class, ImagePlus5DGreyscale16UData.class);

    /**
     * All dimension-less image data types
     */
    public static final List<Class<? extends JIPipeData>> IMAGE_TYPES_DIMENSIONLESS = Arrays.asList(
            ImagePlusData.class,
            ImagePlusColorHSBData.class, ImagePlusColorRGBData.class,
            ImagePlusColorData.class, ImagePlusColorLABData.class,
            ImagePlusGreyscaleData.class, ImagePlusGreyscale32FData.class,
            ImagePlusGreyscaleMaskData.class, ImagePlusGreyscale8UData.class,
            ImagePlusGreyscale8UData.class, ImagePlusGreyscale16UData.class);

    /**
     * All greyscale image data types
     */
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
            ImagePlusColorHSBData.class, ImagePlusColorRGBData.class, ImagePlusColorData.class, ImagePlusColorLABData.class,
            ImagePlus2DColorHSBData.class, ImagePlus2DColorRGBData.class, ImagePlus2DColorData.class, ImagePlus2DColorLABData.class,
            ImagePlus3DColorHSBData.class, ImagePlus3DColorRGBData.class, ImagePlus3DColorData.class, ImagePlus3DColorLABData.class,
            ImagePlus4DColorHSBData.class, ImagePlus4DColorRGBData.class, ImagePlus4DColorData.class, ImagePlus4DColorLABData.class,
            ImagePlus5DColorHSBData.class, ImagePlus5DColorRGBData.class, ImagePlus5DColorData.class, ImagePlus5DColorLABData.class);

    public static final List<Class<? extends JIPipeData>> IMAGE_TYPES_COLOR_HSB = Arrays.asList(
            ImagePlusColorHSBData.class,
            ImagePlus2DColorHSBData.class,
            ImagePlus3DColorHSBData.class,
            ImagePlus4DColorHSBData.class,
            ImagePlus5DColorHSBData.class);

    public static final List<Class<? extends JIPipeData>> IMAGE_TYPES_COLOR_LAB = Arrays.asList(
            ImagePlusColorLABData.class,
            ImagePlus2DColorLABData.class,
            ImagePlus3DColorLABData.class,
            ImagePlus4DColorLABData.class,
            ImagePlus5DColorLABData.class);

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
    public HTMLText getDescription() {
        return new HTMLText("Adds support for commonly used ImageJ data types");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:imagej-integration";
    }

    @Override
    public String getDependencyVersion() {
        return "1.42.1";
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(UIUtils.getIcon32FromResources("apps/bio-formats.png"));
    }

    @Override
    public void register() {
        registerSettingsSheet(ImageJDataTypesSettings.ID,
                "ImageJ data types",
                UIUtils.getIconFromResources("apps/imagej.png"),
                "Extensions",
                UIUtils.getIconFromResources("actions/plugins.png"),
                new ImageJDataTypesSettings());
        registerEnumParameterType("ome-tiff-compression",
                OMETIFFCompression.class,
                "OME TIFF Compression",
                "Available compression algorithms");
        registerEnumParameterType("ij-bit-depth",
                BitDepth.class,
                "Bit depth",
                "Image bit depth");
        registerEnumParameterType("optional-ij-bit-depth",
                OptionalBitDepth.class,
                "Optional Bit depth",
                "Image bit depth");
        registerEnumParameterType("avi-compression",
                AVICompression.class,
                "AVI compression",
                "Determines how AVI movies are compressed");

        // Register data types
        registerDatatype("imagej-ome",
                OMEImageData.class,
                UIUtils.getIconURLFromResources("data-types/bioformats.png"),
                null,
                OMEImageDataPreview.class,
                new OMEImageDataImportIntoImageJOperation(),
                new OpenInImageJDataDisplay(),
                new OMEImageDataImportViaBioFormatsOperation(),
                new OMEImageDataImportIntoJIPipeOperation());
        registerImageJDataAdapter(new OMEImageDataImageJAdapter(), ImagePlusDataImporterUI.class);
        registerImageDataType("imagej-imgplus", ImagePlusData.class, "icons/data-types/imgplus.png");
        registerImageDataType("imagej-imgplus-greyscale", ImagePlusGreyscaleData.class, "icons/data-types/imgplus-greyscale.png");
        registerImageDataType("imagej-imgplus-greyscale-8u", ImagePlusGreyscale8UData.class, "icons/data-types/imgplus-greyscale-8u.png");
        registerImageDataType("imagej-imgplus-greyscale-16u", ImagePlusGreyscale16UData.class, "icons/data-types/imgplus-greyscale-16u.png");
        registerImageDataType("imagej-imgplus-greyscale-32f", ImagePlusGreyscale32FData.class, "icons/data-types/imgplus-greyscale-32f.png");
        registerImageDataType("imagej-imgplus-greyscale-mask", ImagePlusGreyscaleMaskData.class, "icons/data-types/imgplus-greyscale-mask.png");
        registerImageDataType("imagej-imgplus-color", ImagePlusColorData.class, "icons/data-types/imgplus-color.png");
        registerImageDataType("imagej-imgplus-color-rgb", ImagePlusColorRGBData.class, "icons/data-types/imgplus-color-rgb.png");
        registerImageDataType("imagej-imgplus-color-hsb", ImagePlusColorHSBData.class, "icons/data-types/imgplus-color-hsb.png");
        registerImageDataType("imagej-imgplus-color-lab", ImagePlusColorLABData.class, "icons/data-types/imgplus-color-lab.png");
        registerImageDataType("imagej-imgplus-2d", ImagePlus2DData.class, "icons/data-types/imgplus-2d.png");
        registerImageDataType("imagej-imgplus-2d-greyscale", ImagePlus2DGreyscaleData.class, "icons/data-types/imgplus-2d-greyscale.png");
        registerImageDataType("imagej-imgplus-2d-greyscale-8u", ImagePlus2DGreyscale8UData.class, "icons/data-types/imgplus-2d-greyscale-8u.png");
        registerImageDataType("imagej-imgplus-2d-greyscale-16u", ImagePlus2DGreyscale16UData.class, "icons/data-types/imgplus-2d-greyscale-16u.png");
        registerImageDataType("imagej-imgplus-2d-greyscale-32f", ImagePlus2DGreyscale32FData.class, "icons/data-types/imgplus-2d-greyscale-32f.png");
        registerImageDataType("imagej-imgplus-2d-greyscale-mask", ImagePlus2DGreyscaleMaskData.class, "icons/data-types/imgplus-2d-greyscale-mask.png");
        registerImageDataType("imagej-imgplus-2d-color", ImagePlus2DColorData.class, "icons/data-types/imgplus-2d-color.png");
        registerImageDataType("imagej-imgplus-2d-color-rgb", ImagePlus2DColorRGBData.class, "icons/data-types/imgplus-2d-color-rgb.png");
        registerImageDataType("imagej-imgplus-2d-color-hsb", ImagePlus2DColorHSBData.class, "icons/data-types/imgplus-2d-color-hsb.png");
        registerImageDataType("imagej-imgplus-2d-color-lab", ImagePlus2DColorLABData.class, "icons/data-types/imgplus-2d-color-lab.png");
        registerImageDataType("imagej-imgplus-3d", ImagePlus3DData.class, "icons/data-types/imgplus-3d.png");
        registerImageDataType("imagej-imgplus-3d-greyscale", ImagePlus3DGreyscaleData.class, "icons/data-types/imgplus-3d-greyscale.png");
        registerImageDataType("imagej-imgplus-3d-greyscale-8u", ImagePlus3DGreyscale8UData.class, "icons/data-types/imgplus-3d-greyscale-8u.png");
        registerImageDataType("imagej-imgplus-3d-greyscale-16u", ImagePlus3DGreyscale16UData.class, "icons/data-types/imgplus-3d-greyscale-16u.png");
        registerImageDataType("imagej-imgplus-3d-greyscale-32f", ImagePlus3DGreyscale32FData.class, "icons/data-types/imgplus-3d-greyscale-32f.png");
        registerImageDataType("imagej-imgplus-3d-greyscale-mask", ImagePlus3DGreyscaleMaskData.class, "icons/data-types/imgplus-3d-greyscale-mask.png");
        registerImageDataType("imagej-imgplus-3d-color", ImagePlus3DColorData.class, "icons/data-types/imgplus-3d-color.png");
        registerImageDataType("imagej-imgplus-3d-color-rgb", ImagePlus3DColorRGBData.class, "icons/data-types/imgplus-3d-color-rgb.png");
        registerImageDataType("imagej-imgplus-3d-color-hsb", ImagePlus3DColorHSBData.class, "icons/data-types/imgplus-3d-color-hsb.png");
        registerImageDataType("imagej-imgplus-3d-color-lab", ImagePlus3DColorLABData.class, "icons/data-types/imgplus-3d-color-lab.png");
        registerImageDataType("imagej-imgplus-4d", ImagePlus4DData.class, "icons/data-types/imgplus-4d.png");
        registerImageDataType("imagej-imgplus-4d-greyscale", ImagePlus4DGreyscaleData.class, "icons/data-types/imgplus-4d-greyscale.png");
        registerImageDataType("imagej-imgplus-4d-greyscale-8u", ImagePlus4DGreyscale8UData.class, "icons/data-types/imgplus-4d-greyscale-8u.png");
        registerImageDataType("imagej-imgplus-4d-greyscale-16u", ImagePlus4DGreyscale16UData.class, "icons/data-types/imgplus-4d-greyscale-16u.png");
        registerImageDataType("imagej-imgplus-4d-greyscale-32f", ImagePlus4DGreyscale32FData.class, "icons/data-types/imgplus-4d-greyscale-32f.png");
        registerImageDataType("imagej-imgplus-4d-greyscale-mask", ImagePlus4DGreyscaleMaskData.class, "icons/data-types/imgplus-4d-greyscale-mask.png");
        registerImageDataType("imagej-imgplus-4d-color", ImagePlus4DColorData.class, "icons/data-types/imgplus-4d-color.png");
        registerImageDataType("imagej-imgplus-4d-color-rgb", ImagePlus4DColorRGBData.class, "icons/data-types/imgplus-4d-color-rgb.png");
        registerImageDataType("imagej-imgplus-4d-color-hsb", ImagePlus4DColorHSBData.class, "icons/data-types/imgplus-4d-color-hsb.png");
        registerImageDataType("imagej-imgplus-4d-color-lab", ImagePlus4DColorLABData.class, "icons/data-types/imgplus-4d-color-lab.png");
        registerImageDataType("imagej-imgplus-5d", ImagePlus5DData.class, "icons/data-types/imgplus-5d.png");
        registerImageDataType("imagej-imgplus-5d-greyscale", ImagePlus5DGreyscaleData.class, "icons/data-types/imgplus-5d-greyscale.png");
        registerImageDataType("imagej-imgplus-5d-greyscale-8u", ImagePlus5DGreyscale8UData.class, "icons/data-types/imgplus-5d-greyscale-8u.png");
        registerImageDataType("imagej-imgplus-5d-greyscale-16u", ImagePlus5DGreyscale16UData.class, "icons/data-types/imgplus-5d-greyscale-16u.png");
        registerImageDataType("imagej-imgplus-5d-greyscale-32f", ImagePlus5DGreyscale32FData.class, "icons/data-types/imgplus-5d-greyscale-32f.png");
        registerImageDataType("imagej-imgplus-5d-greyscale-mask", ImagePlus5DGreyscaleMaskData.class, "icons/data-types/imgplus-5d-greyscale-mask.png");
        registerImageDataType("imagej-imgplus-5d-color", ImagePlus5DColorData.class, "icons/data-types/imgplus-5d-color.png");
        registerImageDataType("imagej-imgplus-5d-color-rgb", ImagePlus5DColorRGBData.class, "icons/data-types/imgplus-5d-color-rgb.png");
        registerImageDataType("imagej-imgplus-5d-color-hsb", ImagePlus5DColorHSBData.class, "icons/data-types/imgplus-5d-color-hsb.png");
        registerImageDataType("imagej-imgplus-5d-color-lab", ImagePlus5DColorLABData.class, "icons/data-types/imgplus-5d-color-lab.png");
        registerConverters();

        registerDatatype("imagej-roi", ROIListData.class, ResourceUtils.getPluginResource("icons/data-types/roi.png"),
                null, ROIDataPreview.class,
                new ROIDataImportIntoImageOperation(),
                new AddToROIManagerDataDisplay(),
                new AddROIToActiveJIPipeImageViewerDataDisplay(),
                new AddROIToJIPipeImageViewerDataDisplay());
        registerDatatype("imagej-lut", LUTData.class, ResourceUtils.getPluginResource("icons/data-types/lut.png"), null, null);
        registerImageJDataAdapter(new ROIDataImageJAdapter(), ROIDataImporterUI.class);
        registerDatatype("imagej-results-table",
                ResultsTableData.class,
                ResourceUtils.getPluginResource("icons/data-types/results-table.png"),
                null,
                ResultsTableDataPreview.class,
                new OpenResultsTableInImageJDataOperation(),
                new OpenResultsTableInJIPipeTabDataOperation(),
                new CacheAwareOpenResultsTableInJIPipeDataOperation(),
                new OpenInNativeApplicationDataImportOperation(".csv"));
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
        registerNodeType("import-imagej-imgplus-from-file", ImagePlusFromFile.class);
        registerNodeType("import-imagej-bioformats", BioFormatsImporter.class, UIUtils.getIconURLFromResources("apps/bioformats.png"));

        // Register algorithms
        registerNodeType("convert-imagej-image", ImageTypeConverter.class, UIUtils.getIconURLFromResources("actions/viewimage.png"));
        registerNodeType("export-imagej-bioformats", BioFormatsExporter.class, UIUtils.getIconURLFromResources("apps/bioformats.png"));
        registerNodeType("image-properties-to-annotation", ImagePropertiesToAnnotationAlgorithm.class, UIUtils.getIconURLFromResources("data-types/annotation-table.png"));

        registerNodeType("ij1-color-convert-to-rgb", ToRGBColorSpaceConverterAlgorithm.class, UIUtils.getIconURLFromResources("data-types/imgplus-color-rgb.png"));
        registerNodeType("ij1-color-convert-to-hsb", ToHSBColorSpaceConverterAlgorithm.class, UIUtils.getIconURLFromResources("data-types/imgplus-color-hsb.png"));
        registerNodeType("ij1-color-convert-to-lab", ToLABColorSpaceConverterAlgorithm.class, UIUtils.getIconURLFromResources("data-types/imgplus-color-lab.png"));

        // Register parameter editors
        registerEnumParameterType("import-imagej-bioformats:color-mode", OMEColorMode.class,
                "Color mode", "Available modes");
        registerEnumParameterType("import-imagej-bioformats:order", DimensionOrder.class,
                "Order", "Available orders");

        // Register additional file importers
        registerDatatypeOperation("path", new ImportImageJPathDataOperation());
        registerDatatypeOperation("file", new ImportImageJPathDataOperation());

    }

    /**
     * Creates following converters:
     * Lower dimensionality to higher dimensionality (e.g. 2D is also 3D data)
     * Between same dimensionality
     */
    private void registerConverters() {
        registerDatatypeConversion(new ImagePlusToOMEImageTypeConverter());
        registerDatatypeConversion(new OMEImageToImagePlusTypeConverter());
        registerDatatypeConversion(new OMEImageToROITypeConverter());
        registerDatatypeConversion(new OmeImageToOMEXMLTypeConverter());
        registerDatatypeConversion(new PlotToImageTypeConverter());
        registerDatatypeConversion(new ImageToLUTTypeConverter());
        registerDatatypeConversion(new LUTToImageTypeConverter());

        Set<Class<? extends JIPipeData>> dataTypes = getRegistry().getDatatypeRegistry().getRegisteredDataTypes().values()
                .stream().filter(ImagePlusData.class::isAssignableFrom).collect(Collectors.toSet());
        Map<Integer, List<Class<? extends JIPipeData>>> groupedByDimensionality =
                dataTypes.stream().collect(Collectors.groupingBy(d -> ImagePlusData.getDimensionalityOf((Class<? extends ImagePlusData>) d)));
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
        registerDatatype(id,
                dataClass,
                ResourceUtils.getPluginResource(iconResource),
                null,
                ImageDataPreview.class,
                new ImagePlusDataImportIntoJIPipeOperation(),
                new ImagePlusDataImportIntoImageJOperation(),
                new OpenInImageJDataDisplay(),
                new OMEImageDataImportViaBioFormatsOperation());
        registerImageJDataAdapter(new ImgPlusDataImageJAdapter(dataClass), ImagePlusDataImporterUI.class);
    }
}


