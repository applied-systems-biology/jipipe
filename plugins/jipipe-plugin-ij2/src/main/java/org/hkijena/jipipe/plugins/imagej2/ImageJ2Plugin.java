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

package org.hkijena.jipipe.plugins.imagej2;

import net.imagej.ops.OpInfo;
import net.imagej.ops.OpService;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory;
import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.*;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.DefaultImageJDataExporterUI;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.core.CorePlugin;
import org.hkijena.jipipe.plugins.imagej2.algorithms.CreateIJ2OutOfBoundsFactoryAlgorithm;
import org.hkijena.jipipe.plugins.imagej2.algorithms.CreateIJ2ShapeAlgorithm;
import org.hkijena.jipipe.plugins.imagej2.compat.IJ2DataFromImageWindowImageJImporter;
import org.hkijena.jipipe.plugins.imagej2.compat.IJ2DataToImageWindowImageJExporter;
import org.hkijena.jipipe.plugins.imagej2.converters.ImageJ1ToImageJ2Converter;
import org.hkijena.jipipe.plugins.imagej2.converters.ImageJ2ToImageJ1Converter;
import org.hkijena.jipipe.plugins.imagej2.datatypes.ImageJ2DatasetData;
import org.hkijena.jipipe.plugins.imagej2.datatypes.outofbounds.*;
import org.hkijena.jipipe.plugins.imagej2.datatypes.outofbounds.constant.*;
import org.hkijena.jipipe.plugins.imagej2.datatypes.shapes.*;
import org.hkijena.jipipe.plugins.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.plugins.imagejdatatypes.compat.ImagePlusWindowImageJImporterUI;
import org.hkijena.jipipe.plugins.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Extension that adds ImageJ2 algorithms
 */
@Plugin(type = JIPipeJavaPlugin.class, priority = Priority.LOW)
public class ImageJ2Plugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:imagej2",
            JIPipe.getJIPipeVersion(),
            "ImageJ2 algorithms");

    public ImageJ2Plugin() {
    }

    @Override
    public PluginCategoriesEnumParameter.List getCategories() {
        return new PluginCategoriesEnumParameter.List(PluginCategoriesEnumParameter.CATEGORY_IMAGEJ2, PluginCategoriesEnumParameter.CATEGORY_IMGLIB);
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CorePlugin.AS_DEPENDENCY, ImageJDataTypesPlugin.AS_DEPENDENCY);
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
                        true));
    }

    @Override
    public StringList getDependencyCitations() {
        StringList result = new StringList();
        result.add("Rueden, C. T.; Schindelin, J. & Hiner, M. C. et al. (2017), \"ImageJ2: ImageJ for the next generation of scientific image data\", " +
                "BMC Bioinformatics 18:529");
        return result;
    }

    @Override
    public List<JIPipeImageJUpdateSiteDependency> getImageJUpdateSiteDependencies() {
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "ImageJ2 algorithms";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates ImageJ2 algorithms into JIPipe");
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        // Images
        registerDatatype("ij2-dataset", ImageJ2DatasetData.class, UIUtils.getIconURLFromResources("data-types/ij2-image.png"));
        registerDatatypeConversion(new ImageJ1ToImageJ2Converter());
        registerDatatypeConversion(new ImageJ2ToImageJ1Converter());
        registerImageJDataImporter("ij2-dataset-from-window", new IJ2DataFromImageWindowImageJImporter(), ImagePlusWindowImageJImporterUI.class);
        registerImageJDataExporter("ij2-dataset-to-window", new IJ2DataToImageWindowImageJExporter(), DefaultImageJDataExporterUI.class);

        // Shapes
        registerDatatype("ij2-shape", ImageJ2ShapeData.class, UIUtils.getIconURLFromResources("data-types/ij2-shape.png"));
        registerDatatype("ij2-shape-empty", EmptyImageJ2ShapeData.class, UIUtils.getIconURLFromResources("data-types/ij2-shape.png"));
        registerDatatype("ij2-shape-rectangle", RectangleImageJ2ShapeData.class, UIUtils.getIconURLFromResources("data-types/ij2-shape.png"));
        registerDatatype("ij2-shape-centered-rectangle", CenteredRectangleImageJ2ShapeData.class, UIUtils.getIconURLFromResources("data-types/ij2-shape.png"));
        registerDatatype("ij2-shape-periodic-line", PeriodicLineImageJ2ShapeData.class, UIUtils.getIconURLFromResources("data-types/ij2-shape.png"));
        registerDatatype("ij2-shape-pair-of-points", PairOfPointsImageJ2ShapeData.class, UIUtils.getIconURLFromResources("data-types/ij2-shape.png"));
        registerDatatype("ij2-shape-hypersphere", HyperSphereImageJ2ShapeData.class, UIUtils.getIconURLFromResources("data-types/ij2-shape.png"));
        registerDatatype("ij2-shape-horizontal-line", HorizontalLineImageJ2ShapeData.class, UIUtils.getIconURLFromResources("data-types/ij2-shape.png"));
        registerDatatype("ij2-shape-diamond-tips", DiamondTipsImageJ2ShapeData.class, UIUtils.getIconURLFromResources("data-types/ij2-shape.png"));
        registerDatatype("ij2-shape-diamond", DiamondImageJ2ShapeData.class, UIUtils.getIconURLFromResources("data-types/ij2-shape.png"));

        registerNodeType("ij2-create-shape", CreateIJ2ShapeAlgorithm.class);

        // Out of bounds factory
        registerDatatype("ij2-out-of-bounds-factory", ImageJ2OutOfBoundsFactoryData.class, UIUtils.getIconURLFromResources("data-types/ij2-out-of-bounds-factory.png"));
        registerDatatype("ij2-out-of-bounds-factory-empty", EmptyImageJ2OutOfBoundsFactory.class, UIUtils.getIconURLFromResources("data-types/ij2-out-of-bounds-factory.png"));
        registerDatatype("ij2-out-of-bounds-factory-mirror", MirrorImageJ2OutOfBoundsFactoryData.class, UIUtils.getIconURLFromResources("data-types/ij2-out-of-bounds-factory.png"));
        registerDatatype("ij2-out-of-bounds-factory-periodic", PeriodicImageJ2OutOfBoundsFactoryData.class, UIUtils.getIconURLFromResources("data-types/ij2-out-of-bounds-factory.png"));
        registerDatatype("ij2-out-of-bounds-factory-border", BorderImageJ2OutOfBoundsFactoryData.class, UIUtils.getIconURLFromResources("data-types/ij2-out-of-bounds-factory.png"));
        registerDatatype("ij2-out-of-bounds-factory-constant-byte", ByteConstantValueImageJ2OutOfBoundsFactoryData.class, UIUtils.getIconURLFromResources("data-types/ij2-out-of-bounds-factory.png"));
        registerDatatype("ij2-out-of-bounds-factory-constant-short", ShortConstantValueImageJ2OutOfBoundsFactoryData.class, UIUtils.getIconURLFromResources("data-types/ij2-out-of-bounds-factory.png"));
        registerDatatype("ij2-out-of-bounds-factory-constant-int", IntegerConstantValueImageJ2OutOfBoundsFactoryData.class, UIUtils.getIconURLFromResources("data-types/ij2-out-of-bounds-factory.png"));
        registerDatatype("ij2-out-of-bounds-factory-constant-long", LongConstantValueImageJ2OutOfBoundsFactoryData.class, UIUtils.getIconURLFromResources("data-types/ij2-out-of-bounds-factory.png"));
        registerDatatype("ij2-out-of-bounds-factory-constant-float", FloatConstantValueImageJ2OutOfBoundsFactoryData.class, UIUtils.getIconURLFromResources("data-types/ij2-out-of-bounds-factory.png"));
        registerDatatype("ij2-out-of-bounds-factory-constant-double", DoubleConstantValueImageJ2OutOfBoundsFactoryData.class, UIUtils.getIconURLFromResources("data-types/ij2-out-of-bounds-factory.png"));
        registerEnumParameterType("ij2-out-of-bounds-mirror-factory:boundary",
                OutOfBoundsMirrorFactory.Boundary.class,
                "Mirror boundary",
                "Boundary pixels are either duplicated or not");

        registerNodeType("ij2-create-out-of-bounds-factory", CreateIJ2OutOfBoundsFactoryAlgorithm.class);

        // ImageJ2 ops
        OpService opService = context.getService(OpService.class);
        for (OpInfo info : opService.infos()) {
            JIPipeProgressInfo moduleProgress = progressInfo.resolve(info.cInfo().getTitle() + " @ " + info.cInfo().getDelegateClassName());
            try {
                ImageJ2OpNodeInfo nodeInfo = new ImageJ2OpNodeInfo(context, info, moduleProgress);
                if (nodeInfo.getInputSlots().isEmpty() && nodeInfo.getOutputSlots().isEmpty()) {
                    progressInfo.log("Node has no data slots. Skipping.");
                    continue;
                }
                registerNodeType(nodeInfo, UIUtils.getIconURLFromResources("apps/imagej2.png"));
            } catch (Exception e) {
                moduleProgress.log("Unable to register module:");
                moduleProgress.log(e.toString());
            }
        }
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Collections.singletonList(UIUtils.getIcon32FromResources("apps/imglib2.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:imagej2";
    }

}



