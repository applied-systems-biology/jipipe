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
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.DefaultImageJDataExporterUI;
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.metadata.JIPipeOrganizationMetadata;
import org.hkijena.jipipe.api.service.JIPipeService;
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
import org.hkijena.jipipe.plugins.imagej2.viewers.ImageJ2DatasetDataViewer;
import org.hkijena.jipipe.plugins.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.plugins.imagejdatatypes.compat.ImagePlusWindowImageJImporterUI;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameterList;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
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
    public PluginCategoriesEnumParameterList getCategories() {
        return new PluginCategoriesEnumParameterList(PluginCategoriesEnumParameter.CATEGORY_IMAGEJ2, PluginCategoriesEnumParameter.CATEGORY_IMGLIB);
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
        // Shared affiliations
        final JIPipeOrganizationMetadata loci = new JIPipeOrganizationMetadata.Builder()
                .name("Laboratory for Optical and Computational Instrumentation, University of Wisconsin at Madison, Madison, Wisconsin, USA")
                .ror("https://ror.org/01y2jtd41")
                .website("https://loci.wisc.edu")
                .build();

        final JIPipeOrganizationMetadata morgridge = new JIPipeOrganizationMetadata.Builder()
                .name("Morgridge Institute for Research, Madison, Wisconsin, USA")
                .ror("https://ror.org/05cb4rb43")
                .website("https://morgridge.org")
                .build();

        // Author list
        return new JIPipeAuthorMetadata.List(
                new JIPipeAuthorMetadata.Builder()
                        .firstName("Curtis T.")
                        .lastName("Rueden")
                        .affiliations(List.of(loci))
                        .firstAuthor(true)
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Johannes")
                        .lastName("Schindelin")
                        .affiliations(List.of(loci, morgridge))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Mark C.")
                        .lastName("Hiner")
                        .affiliations(List.of(loci))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Barry E.")
                        .lastName("DeZonia")
                        .affiliations(List.of(loci))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Alison E.")
                        .lastName("Walter")
                        .affiliations(List.of(loci, morgridge))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Ellen T.")
                        .lastName("Arena")
                        .affiliations(List.of(loci, morgridge))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Kevin W.")
                        .lastName("Eliceiri")
                        .affiliations(List.of(loci, morgridge))
                        .correspondingAuthor(true)
                        .build()
        );

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
    public void register(JIPipeService service, Context context, JIPipeProgressInfo progressInfo) {
        // Images
        registerDatatype("ij2-dataset", ImageJ2DatasetData.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-image.png"));
        registerDatatypeConversion(new ImageJ1ToImageJ2Converter());
        registerDatatypeConversion(new ImageJ2ToImageJ1Converter());
        registerImageJDataImporter("ij2-dataset-from-window", new IJ2DataFromImageWindowImageJImporter(), ImagePlusWindowImageJImporterUI.class);
        registerImageJDataExporter("ij2-dataset-to-window", new IJ2DataToImageWindowImageJExporter(), DefaultImageJDataExporterUI.class);

        registerDefaultDataTypeViewer(ImageJ2DatasetData.class, ImageJ2DatasetDataViewer.class);

        // Shapes
        registerDatatype("ij2-shape", ImageJ2ShapeData.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-shape.png"));
        registerDatatype("ij2-shape-empty", EmptyImageJ2ShapeData.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-shape.png"));
        registerDatatype("ij2-shape-rectangle", RectangleImageJ2ShapeData.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-shape.png"));
        registerDatatype("ij2-shape-centered-rectangle", CenteredRectangleImageJ2ShapeData.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-shape.png"));
        registerDatatype("ij2-shape-periodic-line", PeriodicLineImageJ2ShapeData.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-shape.png"));
        registerDatatype("ij2-shape-pair-of-points", PairOfPointsImageJ2ShapeData.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-shape.png"));
        registerDatatype("ij2-shape-hypersphere", HyperSphereImageJ2ShapeData.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-shape.png"));
        registerDatatype("ij2-shape-horizontal-line", HorizontalLineImageJ2ShapeData.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-shape.png"));
        registerDatatype("ij2-shape-diamond-tips", DiamondTipsImageJ2ShapeData.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-shape.png"));
        registerDatatype("ij2-shape-diamond", DiamondImageJ2ShapeData.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-shape.png"));

        registerNodeType("ij2-create-shape", CreateIJ2ShapeAlgorithm.class);

        // Out of bounds factory
        registerDatatype("ij2-out-of-bounds-factory", ImageJ2OutOfBoundsFactoryData.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-out-of-bounds-factory.png"));
        registerDatatype("ij2-out-of-bounds-factory-empty", EmptyImageJ2OutOfBoundsFactory.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-out-of-bounds-factory.png"));
        registerDatatype("ij2-out-of-bounds-factory-mirror", MirrorImageJ2OutOfBoundsFactoryData.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-out-of-bounds-factory.png"));
        registerDatatype("ij2-out-of-bounds-factory-periodic", PeriodicImageJ2OutOfBoundsFactoryData.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-out-of-bounds-factory.png"));
        registerDatatype("ij2-out-of-bounds-factory-border", BorderImageJ2OutOfBoundsFactoryData.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-out-of-bounds-factory.png"));
        registerDatatype("ij2-out-of-bounds-factory-constant-byte", ByteConstantValueImageJ2OutOfBoundsFactoryData.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-out-of-bounds-factory.png"));
        registerDatatype("ij2-out-of-bounds-factory-constant-short", ShortConstantValueImageJ2OutOfBoundsFactoryData.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-out-of-bounds-factory.png"));
        registerDatatype("ij2-out-of-bounds-factory-constant-int", IntegerConstantValueImageJ2OutOfBoundsFactoryData.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-out-of-bounds-factory.png"));
        registerDatatype("ij2-out-of-bounds-factory-constant-long", LongConstantValueImageJ2OutOfBoundsFactoryData.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-out-of-bounds-factory.png"));
        registerDatatype("ij2-out-of-bounds-factory-constant-float", FloatConstantValueImageJ2OutOfBoundsFactoryData.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-out-of-bounds-factory.png"));
        registerDatatype("ij2-out-of-bounds-factory-constant-double", DoubleConstantValueImageJ2OutOfBoundsFactoryData.class, JIPipe.RESOURCES.getIcon16URL("data-types/ij2-out-of-bounds-factory.png"));
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
                registerNodeType(nodeInfo, JIPipe.RESOURCES.getIcon16URL("apps/imagej2.png"));
            } catch (Exception e) {
                moduleProgress.log("Unable to register module:");
                moduleProgress.log(e.toString());
            }
        }
    }

    @Override
    public List<JIPipeJavaPluginSplashIcon> getSplashIcons() {
        return List.of(JIPipeJavaPluginSplashIcon.builder().id("imglib2").name("ImgLib2").icon(JIPipe.RESOURCES.getIcon32("apps/imglib2.png")).url("https://github.com/imglib/imglib2").build());
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:imagej2";
    }

}



