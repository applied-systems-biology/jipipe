/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejdatatypes;

import ome.xml.model.enums.DimensionOrder;
import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.DefaultImageJDataExporterUI;
import org.hkijena.jipipe.api.compat.DefaultImageJDataImporterUI;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.extensions.core.CorePlugin;
import org.hkijena.jipipe.extensions.core.data.OpenInNativeApplicationDataImportOperation;
import org.hkijena.jipipe.extensions.filesystem.FilesystemPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.color.ToHSBColorSpaceConverterAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.color.ToLABColorSpaceConverterAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.color.ToRGBColorSpaceConverterAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.datasources.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.compat.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorHSBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorLABData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorHSBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorLABData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorHSBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorLABData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.ImagePlus4DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColorHSBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColorLABData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.greyscale.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.ImagePlus5DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColorData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColorHSBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColorLABData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.greyscale.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.fft.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.display.AddROIToActiveJIPipeImageViewerDataDisplay;
import org.hkijena.jipipe.extensions.imagejdatatypes.display.AddROIToJIPipeImageViewerDataDisplay;
import org.hkijena.jipipe.extensions.imagejdatatypes.display.AddToROIManagerDataDisplay;
import org.hkijena.jipipe.extensions.imagejdatatypes.display.OpenInImageJDataDisplay;
import org.hkijena.jipipe.extensions.imagejdatatypes.parameters.OMEColorMode;
import org.hkijena.jipipe.extensions.imagejdatatypes.parameters.OMETIFFCompression;
import org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis.ImageDataPreview;
import org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis.ImportImageJPathDataOperation;
import org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis.OMEImageDataPreview;
import org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis.ROIDataPreview;
import org.hkijena.jipipe.extensions.imagejdatatypes.settings.ImageViewerUIROI2DDisplaySettings;
import org.hkijena.jipipe.extensions.imagejdatatypes.tools.BioFormatsConfigTool;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.AVICompression;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.BitDepth;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.OptionalBitDepth;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ROIElementDrawingMode;
import org.hkijena.jipipe.extensions.imageviewer.settings.ImageViewer2DUISettings;
import org.hkijena.jipipe.extensions.imageviewer.settings.ImageViewer3DUISettings;
import org.hkijena.jipipe.extensions.imageviewer.settings.ImageViewerGeneralUISettings;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.strings.StringsPlugin;
import org.hkijena.jipipe.extensions.tables.ResultsTableDataPreview;
import org.hkijena.jipipe.extensions.tables.TablesPlugin;
import org.hkijena.jipipe.extensions.tables.compat.ResultsTableDataImageJExporter;
import org.hkijena.jipipe.extensions.tables.compat.ResultsTableDataImageJImporter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.display.CacheAwareOpenResultsTableInJIPipeDataOperation;
import org.hkijena.jipipe.extensions.tables.display.OpenResultsTableInImageJDataOperation;
import org.hkijena.jipipe.extensions.tables.display.OpenResultsTableInJIPipeTabDataOperation;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
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
@Plugin(type = JIPipeJavaPlugin.class)
public class ImageJDataTypesPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:imagej-integration",
            JIPipe.getJIPipeVersion(),
            "ImageJ integration");

    public ImageJDataTypesPlugin() {
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CorePlugin.AS_DEPENDENCY, TablesPlugin.AS_DEPENDENCY, StringsPlugin.AS_DEPENDENCY, FilesystemPlugin.AS_DEPENDENCY);
    }

    @Override
    public PluginCategoriesEnumParameter.List getCategories() {
        return new PluginCategoriesEnumParameter.List(PluginCategoriesEnumParameter.CATEGORY_IMPORT_EXPORT, PluginCategoriesEnumParameter.CATEGORY_IMAGE_ANNOTATION, PluginCategoriesEnumParameter.CATEGORY_OME);
    }

    @Override
    public ImageParameter getThumbnail() {
        return new ImageParameter(ResourceUtils.getPluginResource("thumbnails/fiji.png"));
    }

    @Override
    public JIPipeAuthorMetadata.List getAcknowledgements() {
        return new JIPipeAuthorMetadata.List(new JIPipeAuthorMetadata("",
                "Curtis T.",
                "Rueden",
                new StringList("Laboratory for Optical and Computational Instrumentation, University of Wisconsin at Madison, Madison, Wisconsin, USA"),
                "",
                "",
                true,
                false),
                new JIPipeAuthorMetadata("",
                        "Johannes",
                        "Schindelin",
                        new StringList("Laboratory for Optical and Computational Instrumentation, University of Wisconsin at Madison, Madison, Wisconsin, USA",
                                "Morgridge Institute for Research, Madison, Wisconsin, USA"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Mark C.",
                        "Hiner",
                        new StringList("Laboratory for Optical and Computational Instrumentation, University of Wisconsin at Madison, Madison, Wisconsin, USA"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Barry E.",
                        "DeZonia",
                        new StringList("Laboratory for Optical and Computational Instrumentation, University of Wisconsin at Madison, Madison, Wisconsin, USA"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Alison E.",
                        "Walter",
                        new StringList("Laboratory for Optical and Computational Instrumentation, University of Wisconsin at Madison, Madison, Wisconsin, USA",
                                "Morgridge Institute for Research, Madison, Wisconsin, USA"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Ellen T.",
                        "Arena",
                        new StringList("Laboratory for Optical and Computational Instrumentation, University of Wisconsin at Madison, Madison, Wisconsin, USA",
                                "Morgridge Institute for Research, Madison, Wisconsin, USA"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Kevin W.",
                        "Eliceiri",
                        new StringList("Laboratory for Optical and Computational Instrumentation, University of Wisconsin at Madison, Madison, Wisconsin, USA",
                                "Morgridge Institute for Research, Madison, Wisconsin, USA"),
                        "",
                        "",
                        false,
                        true),
                new JIPipeAuthorMetadata("",
                        "Caroline A.",
                        "Schneider",
                        new StringList("Laboratory for Optical and Computational Instrumentation, University of Wisconsin at Madison, Madison, Wisconsin, USA"),
                        "",
                        "",
                        true,
                        false),
                new JIPipeAuthorMetadata("",
                        "Wayne S.",
                        "Rasband",
                        new StringList("Section on Instrumentation, US National Institutes of Health, Bethesda, Maryland, USA"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Melissa",
                        "Linkert",
                        new StringList("Laboratory for Optical and Computational Instrumentation, Department of Molecular Biology",
                                "Department of Biomedical Engineering, Graduate School, University of Wisconsin at Madison, Madison, WI 53711",
                                "Glencoe Software, Inc., Seattle, WA 98101"),
                        "",
                        "",
                        true,
                        false),
                new JIPipeAuthorMetadata("",
                        "Chris",
                        "Allan",
                        new StringList("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee DD1 5EH, Scotland, UK",
                                "Glencoe Software, Inc., Seattle, WA 98101"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Jean-Marie",
                        "Burel",
                        new StringList("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee DD1 5EH, Scotland, UK"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Will",
                        "Moore",
                        new StringList("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee DD1 5EH, Scotland, UK"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Andrew",
                        "Patterson",
                        new StringList("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee DD1 5EH, Scotland, UK"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Brian",
                        "Loranger",
                        new StringList("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee DD1 5EH, Scotland, UK"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Josh",
                        "Moore",
                        new StringList("Glencoe Software, Inc., Seattle, WA 98101"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Carlos",
                        "Neves",
                        new StringList("Glencoe Software, Inc., Seattle, WA 98101"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Donald",
                        "MacDonald",
                        new StringList("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee DD1 5EH, Scotland, UK"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Aleksandra",
                        "Tarkowska",
                        new StringList("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee DD1 5EH, Scotland, UK"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Caitlin",
                        "Sticco",
                        new StringList("Laboratory for Optical and Computational Instrumentation, Department of Molecular Biology",
                                "Department of Biomedical Engineering, Graduate School, University of Wisconsin at Madison, Madison, WI 53711"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Emma",
                        "Hill",
                        new StringList("The Rockefeller University Press, New York, NY 10065"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Mike",
                        "Rossner",
                        new StringList("The Rockefeller University Press, New York, NY 10065"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Jason R.",
                        "Swedlow",
                        new StringList("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee DD1 5EH, Scotland, UK",
                                "Glencoe Software, Inc., Seattle, WA 98101"),
                        "",
                        "",
                        false,
                        true));

    }

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
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(UIUtils.getIcon32FromResources("apps/bio-formats.png"));
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerSettingsSheet(ImageJDataTypesSettings.ID,
                "ImageJ data types",
                "Settings on how ImageJ data types are imported/exported",
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
        registerEnumParameterType("roi-element-drawing-mode",
                ROIElementDrawingMode.class,
                "ROI element drawing mode",
                "Determines how ROI elements are drawn");

        // Register settings
        registerSettingsSheet(ImageViewer2DUISettings.ID,
                "2D image viewer",
                "Settings for the JIPipe 2D image viewer",
                UIUtils.getIconFromResources("data-types/imgplus-2d.png"),
                "Image viewer",
                UIUtils.getIconFromResources("actions/viewimage.png"),
                new ImageViewer2DUISettings());
        registerSettingsSheet(ImageViewer3DUISettings.ID,
                "3D image viewer",
                "Settings for the JIPipe 2D image viewer",
                UIUtils.getIconFromResources("data-types/imgplus-3d.png"),
                "Image viewer",
                UIUtils.getIconFromResources("actions/viewimage.png"),
                new ImageViewer3DUISettings());

        // Register data types
        registerDatatype("imagej-ome",
                OMEImageData.class,
                UIUtils.getIconURLFromResources("data-types/bioformats.png"),
                null,
                OMEImageDataPreview.class,
                new OpenInImageJDataDisplay());
        registerImageJDataImporter("ome-image-from-image-window", new OMEImageFromImageWindowImageJImporter(), ImagePlusWindowImageJImporterUI.class);
        registerImageJDataExporter("ome-image-to-image-window", new OMEImageToImageWindowImageJExporter(), DefaultImageJDataExporterUI.class);

        ImagePlusDataFromImageWindowImageJImporter imageImporter = new ImagePlusDataFromImageWindowImageJImporter(ImagePlusData.class);
        ImagePlusDataToImageWindowImageJExporter imageExporter = new ImagePlusDataToImageWindowImageJExporter();
        registerImageJDataImporter("imagej-window-to-imgplus", imageImporter, ImagePlusWindowImageJImporterUI.class);
        registerImageJDataExporter("image-to-imagej-window", imageExporter, DefaultImageJDataExporterUI.class);
        registerImageDataType("imagej-imgplus", ImagePlusData.class, imageImporter, imageExporter, "icons/data-types/imgplus.png");

        // Other image data types
        registerImageDataType("imagej-imgplus-greyscale", ImagePlusGreyscaleData.class, imageImporter, imageExporter, "icons/data-types/imgplus-greyscale.png");
        registerImageDataType("imagej-imgplus-greyscale-8u", ImagePlusGreyscale8UData.class, imageImporter, imageExporter, "icons/data-types/imgplus-greyscale-8u.png");
        registerImageDataType("imagej-imgplus-greyscale-16u", ImagePlusGreyscale16UData.class, imageImporter, imageExporter, "icons/data-types/imgplus-greyscale-16u.png");
        registerImageDataType("imagej-imgplus-greyscale-32f", ImagePlusGreyscale32FData.class, imageImporter, imageExporter, "icons/data-types/imgplus-greyscale-32f.png");
        registerImageDataType("imagej-imgplus-greyscale-mask", ImagePlusGreyscaleMaskData.class, imageImporter, imageExporter, "icons/data-types/imgplus-greyscale-mask.png");
        registerImageDataType("imagej-imgplus-color", ImagePlusColorData.class, imageImporter, imageExporter, "icons/data-types/imgplus-color.png");
        registerImageDataType("imagej-imgplus-color-rgb", ImagePlusColorRGBData.class, imageImporter, imageExporter, "icons/data-types/imgplus-color-rgb.png");
        registerImageDataType("imagej-imgplus-color-hsb", ImagePlusColorHSBData.class, imageImporter, imageExporter, "icons/data-types/imgplus-color-hsb.png");
        registerImageDataType("imagej-imgplus-color-lab", ImagePlusColorLABData.class, imageImporter, imageExporter, "icons/data-types/imgplus-color-lab.png");
        registerImageDataType("imagej-imgplus-2d", ImagePlus2DData.class, imageImporter, imageExporter, "icons/data-types/imgplus-2d.png");
        registerImageDataType("imagej-imgplus-2d-greyscale", ImagePlus2DGreyscaleData.class, imageImporter, imageExporter, "icons/data-types/imgplus-2d-greyscale.png");
        registerImageDataType("imagej-imgplus-2d-greyscale-8u", ImagePlus2DGreyscale8UData.class, imageImporter, imageExporter, "icons/data-types/imgplus-2d-greyscale-8u.png");
        registerImageDataType("imagej-imgplus-2d-greyscale-16u", ImagePlus2DGreyscale16UData.class, imageImporter, imageExporter, "icons/data-types/imgplus-2d-greyscale-16u.png");
        registerImageDataType("imagej-imgplus-2d-greyscale-32f", ImagePlus2DGreyscale32FData.class, imageImporter, imageExporter, "icons/data-types/imgplus-2d-greyscale-32f.png");
        registerImageDataType("imagej-imgplus-2d-greyscale-mask", ImagePlus2DGreyscaleMaskData.class, imageImporter, imageExporter, "icons/data-types/imgplus-2d-greyscale-mask.png");
        registerImageDataType("imagej-imgplus-2d-color", ImagePlus2DColorData.class, imageImporter, imageExporter, "icons/data-types/imgplus-2d-color.png");
        registerImageDataType("imagej-imgplus-2d-color-rgb", ImagePlus2DColorRGBData.class, imageImporter, imageExporter, "icons/data-types/imgplus-2d-color-rgb.png");
        registerImageDataType("imagej-imgplus-2d-color-hsb", ImagePlus2DColorHSBData.class, imageImporter, imageExporter, "icons/data-types/imgplus-2d-color-hsb.png");
        registerImageDataType("imagej-imgplus-2d-color-lab", ImagePlus2DColorLABData.class, imageImporter, imageExporter, "icons/data-types/imgplus-2d-color-lab.png");
        registerImageDataType("imagej-imgplus-3d", ImagePlus3DData.class, imageImporter, imageExporter, "icons/data-types/imgplus-3d.png");
        registerImageDataType("imagej-imgplus-3d-greyscale", ImagePlus3DGreyscaleData.class, imageImporter, imageExporter, "icons/data-types/imgplus-3d-greyscale.png");
        registerImageDataType("imagej-imgplus-3d-greyscale-8u", ImagePlus3DGreyscale8UData.class, imageImporter, imageExporter, "icons/data-types/imgplus-3d-greyscale-8u.png");
        registerImageDataType("imagej-imgplus-3d-greyscale-16u", ImagePlus3DGreyscale16UData.class, imageImporter, imageExporter, "icons/data-types/imgplus-3d-greyscale-16u.png");
        registerImageDataType("imagej-imgplus-3d-greyscale-32f", ImagePlus3DGreyscale32FData.class, imageImporter, imageExporter, "icons/data-types/imgplus-3d-greyscale-32f.png");
        registerImageDataType("imagej-imgplus-3d-greyscale-mask", ImagePlus3DGreyscaleMaskData.class, imageImporter, imageExporter, "icons/data-types/imgplus-3d-greyscale-mask.png");
        registerImageDataType("imagej-imgplus-3d-color", ImagePlus3DColorData.class, imageImporter, imageExporter, "icons/data-types/imgplus-3d-color.png");
        registerImageDataType("imagej-imgplus-3d-color-rgb", ImagePlus3DColorRGBData.class, imageImporter, imageExporter, "icons/data-types/imgplus-3d-color-rgb.png");
        registerImageDataType("imagej-imgplus-3d-color-hsb", ImagePlus3DColorHSBData.class, imageImporter, imageExporter, "icons/data-types/imgplus-3d-color-hsb.png");
        registerImageDataType("imagej-imgplus-3d-color-lab", ImagePlus3DColorLABData.class, imageImporter, imageExporter, "icons/data-types/imgplus-3d-color-lab.png");
        registerImageDataType("imagej-imgplus-4d", ImagePlus4DData.class, imageImporter, imageExporter, "icons/data-types/imgplus-4d.png");
        registerImageDataType("imagej-imgplus-4d-greyscale", ImagePlus4DGreyscaleData.class, imageImporter, imageExporter, "icons/data-types/imgplus-4d-greyscale.png");
        registerImageDataType("imagej-imgplus-4d-greyscale-8u", ImagePlus4DGreyscale8UData.class, imageImporter, imageExporter, "icons/data-types/imgplus-4d-greyscale-8u.png");
        registerImageDataType("imagej-imgplus-4d-greyscale-16u", ImagePlus4DGreyscale16UData.class, imageImporter, imageExporter, "icons/data-types/imgplus-4d-greyscale-16u.png");
        registerImageDataType("imagej-imgplus-4d-greyscale-32f", ImagePlus4DGreyscale32FData.class, imageImporter, imageExporter, "icons/data-types/imgplus-4d-greyscale-32f.png");
        registerImageDataType("imagej-imgplus-4d-greyscale-mask", ImagePlus4DGreyscaleMaskData.class, imageImporter, imageExporter, "icons/data-types/imgplus-4d-greyscale-mask.png");
        registerImageDataType("imagej-imgplus-4d-color", ImagePlus4DColorData.class, imageImporter, imageExporter, "icons/data-types/imgplus-4d-color.png");
        registerImageDataType("imagej-imgplus-4d-color-rgb", ImagePlus4DColorRGBData.class, imageImporter, imageExporter, "icons/data-types/imgplus-4d-color-rgb.png");
        registerImageDataType("imagej-imgplus-4d-color-hsb", ImagePlus4DColorHSBData.class, imageImporter, imageExporter, "icons/data-types/imgplus-4d-color-hsb.png");
        registerImageDataType("imagej-imgplus-4d-color-lab", ImagePlus4DColorLABData.class, imageImporter, imageExporter, "icons/data-types/imgplus-4d-color-lab.png");
        registerImageDataType("imagej-imgplus-5d", ImagePlus5DData.class, imageImporter, imageExporter, "icons/data-types/imgplus-5d.png");
        registerImageDataType("imagej-imgplus-5d-greyscale", ImagePlus5DGreyscaleData.class, imageImporter, imageExporter, "icons/data-types/imgplus-5d-greyscale.png");
        registerImageDataType("imagej-imgplus-5d-greyscale-8u", ImagePlus5DGreyscale8UData.class, imageImporter, imageExporter, "icons/data-types/imgplus-5d-greyscale-8u.png");
        registerImageDataType("imagej-imgplus-5d-greyscale-16u", ImagePlus5DGreyscale16UData.class, imageImporter, imageExporter, "icons/data-types/imgplus-5d-greyscale-16u.png");
        registerImageDataType("imagej-imgplus-5d-greyscale-32f", ImagePlus5DGreyscale32FData.class, imageImporter, imageExporter, "icons/data-types/imgplus-5d-greyscale-32f.png");
        registerImageDataType("imagej-imgplus-5d-greyscale-mask", ImagePlus5DGreyscaleMaskData.class, imageImporter, imageExporter, "icons/data-types/imgplus-5d-greyscale-mask.png");
        registerImageDataType("imagej-imgplus-5d-color", ImagePlus5DColorData.class, imageImporter, imageExporter, "icons/data-types/imgplus-5d-color.png");
        registerImageDataType("imagej-imgplus-5d-color-rgb", ImagePlus5DColorRGBData.class, imageImporter, imageExporter, "icons/data-types/imgplus-5d-color-rgb.png");
        registerImageDataType("imagej-imgplus-5d-color-hsb", ImagePlus5DColorHSBData.class, imageImporter, imageExporter, "icons/data-types/imgplus-5d-color-hsb.png");
        registerImageDataType("imagej-imgplus-5d-color-lab", ImagePlus5DColorLABData.class, imageImporter, imageExporter, "icons/data-types/imgplus-5d-color-lab.png");
        registerConverters();

        registerDatatype("imagej-roi", ROIListData.class, ResourceUtils.getPluginResource("icons/data-types/roi.png"),
                null, ROIDataPreview.class,
                new AddToROIManagerDataDisplay(),
                new AddROIToActiveJIPipeImageViewerDataDisplay(),
                new AddROIToJIPipeImageViewerDataDisplay());
        registerDatatype("imagej-lut", LUTData.class, ResourceUtils.getPluginResource("icons/data-types/lut.png"));
        registerImageJDataImporter("roi-from-roi-manager", new RoiManagerImageJImporter(), RoiManagerImageJImporterUI.class);
        registerImageJDataExporter("roi-to-roi-manager", new RoiManagerImageJExporter(), DefaultImageJDataExporterUI.class);
        registerDatatype("imagej-results-table",
                ResultsTableData.class,
                ResourceUtils.getPluginResource("icons/data-types/results-table.png"),
                null,
                ResultsTableDataPreview.class,
                new OpenResultsTableInImageJDataOperation(),
                new OpenResultsTableInJIPipeTabDataOperation(),
                new CacheAwareOpenResultsTableInJIPipeDataOperation(),
                new OpenInNativeApplicationDataImportOperation(".csv"));
        registerImageJDataImporter("import-results-table", new ResultsTableDataImageJImporter(), DefaultImageJDataImporterUI.class);
        registerImageJDataExporter("export-results-table", new ResultsTableDataImageJExporter(), DefaultImageJDataExporterUI.class);

        // Register FFT data types
        registerImageDataType("imagej-imgplus-fft", ImagePlusFFTData.class, imageImporter, imageExporter, "icons/data-types/imgplus-fft.png");
        registerImageDataType("imagej-imgplus-fft-2d", ImagePlusFFT2DData.class, imageImporter, imageExporter, "icons/data-types/imgplus-fft-2d.png");
        registerImageDataType("imagej-imgplus-fft-3d", ImagePlusFFT3DData.class, imageImporter, imageExporter, "icons/data-types/imgplus-fft-3d.png");
        registerImageDataType("imagej-imgplus-fft-4d", ImagePlusFFT4DData.class, imageImporter, imageExporter, "icons/data-types/imgplus-fft-4d.png");
        registerImageDataType("imagej-imgplus-fft-5d", ImagePlusFFT5DData.class, imageImporter, imageExporter, "icons/data-types/imgplus-fft-5d.png");

        // Register data sources
        registerNodeType("import-imagej-roi-from-file", ROIDataFromFile.class);
        registerNodeType("extract-imagej-roi-from-ome-image", ROIDataFromOMEImage.class);
        registerEnumParameterType("import-imagej-results-table-from-file:file-format", ResultsTableFromFile.FileFormat.class, "File format", "A file format");
        registerNodeType("import-imagej-results-table-from-file", ResultsTableFromFile.class);
        registerNodeType("import-imagej-imgplus-from-file", ImagePlusFromFile.class);
        registerNodeType("import-imagej-bioformats", BioFormatsImporter.class, UIUtils.getIconURLFromResources("apps/bioformats.png"));
        registerNodeType("import-imagej-lut-from-file-as-rgb", ImportLUTAsRGBFromFile.class, ResourceUtils.getPluginResource("icons/data-types/lut.png"));

        // Register algorithms
        registerNodeType("convert-imagej-image", ImageTypeConverter.class, UIUtils.getIconURLFromResources("actions/viewimage.png"));
        registerNodeType("export-imagej-bioformats", BioFormatsExporter.class, UIUtils.getIconURLFromResources("apps/bioformats.png"));
        registerNodeType("export-imagej-bioformats-v2", BioFormatsExporter2.class, UIUtils.getIconURLFromResources("apps/bioformats.png"));
        registerNodeType("set-imagej-bioformats-settings", SetBioFormatsExporterSettings.class, UIUtils.getIconURLFromResources("apps/bioformats.png"));
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

        registerMenuExtension(BioFormatsConfigTool.class);

        registerSettingsSheet(ImageViewerUIROI2DDisplaySettings.ID,
                "2D ROI display",
                "Settings for the ROI manager component of the JIPipe image viewer",
                UIUtils.getIconFromResources("actions/roi.png"),
                "Image viewer",
                UIUtils.getIconFromResources("actions/viewimage.png"),
                new ImageViewerUIROI2DDisplaySettings());
        registerSettingsSheet(ImageViewerGeneralUISettings.ID,
                "General",
                "General settings for the image viewer",
                UIUtils.getIconFromResources("actions/configure.png"),
                "Image viewer",
                UIUtils.getIconFromResources("actions/viewimage.png"),
                new ImageViewerGeneralUISettings());

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

    private void registerImageDataType(String id, Class<? extends ImagePlusData> dataClass, ImagePlusDataFromImageWindowImageJImporter imageImporter, ImagePlusDataToImageWindowImageJExporter imageExporter, String iconResource) {
        if (dataClass.getAnnotation(ImageTypeInfo.class) == null) {
            throw new IllegalArgumentException("Cannot register image data type '" + id + "' (" + dataClass + ") without ImageTypeInfo annotation!");
        }
        registerDatatype(id,
                dataClass,
                ResourceUtils.getPluginResource(iconResource),
                null,
                ImageDataPreview.class,
                new OpenInImageJDataDisplay());
        configureDefaultImageJAdapters(dataClass, imageImporter, imageExporter);
//        registerImageJDataImporter("import-" + id, new ImagePlusDataFromImageWindowImageJImporter(dataClass), ImagePlusWindowImageJImporterUI.class);
    }
}


