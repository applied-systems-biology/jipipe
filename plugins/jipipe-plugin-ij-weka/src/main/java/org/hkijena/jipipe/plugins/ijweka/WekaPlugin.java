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

package org.hkijena.jipipe.plugins.ijweka;

import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.metadata.JIPipeOrganizationMetadata;
import org.hkijena.jipipe.api.parameters.JIPipeParameterArchetype;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.core.CorePlugin;
import org.hkijena.jipipe.plugins.filesystem.FilesystemPlugin;
import org.hkijena.jipipe.plugins.ijweka.datatypes.WekaModelData;
import org.hkijena.jipipe.plugins.ijweka.nodes.*;
import org.hkijena.jipipe.plugins.ijweka.parameters.WekaClassifierDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.ijweka.parameters.WekaClassifierParameter;
import org.hkijena.jipipe.plugins.ijweka.parameters.features.WekaFeature2D;
import org.hkijena.jipipe.plugins.ijweka.parameters.features.WekaFeature3D;
import org.hkijena.jipipe.plugins.ijweka.parameters.features.WekaFeatureSet2D;
import org.hkijena.jipipe.plugins.ijweka.parameters.features.WekaFeatureSet3D;
import org.hkijena.jipipe.plugins.imagejalgorithms.ImageJAlgorithmsPlugin;
import org.hkijena.jipipe.plugins.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameterList;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Plugin(type = JIPipeJavaPlugin.class)
public class WekaPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:ij-weka",
            JIPipe.getJIPipeVersion(),
            "IJ Trainable Weka Filter integration");
    public static final String RESOURCE_BASE_PATH = "/org/hkijena/jipipe/plugins/ijweka";

    public WekaPlugin() {
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CorePlugin.AS_DEPENDENCY, FilesystemPlugin.AS_DEPENDENCY, ImageJDataTypesPlugin.AS_DEPENDENCY, ImageJAlgorithmsPlugin.AS_DEPENDENCY);
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public PluginCategoriesEnumParameterList getCategories() {
        return new PluginCategoriesEnumParameterList(PluginCategoriesEnumParameter.CATEGORY_SEGMENTATION, PluginCategoriesEnumParameter.CATEGORY_MACHINE_LEARNING);
    }

    @Override
    public JIPipeAuthorMetadata.List getAcknowledgements() {

        // Shared affiliations
        final JIPipeOrganizationMetadata ikerbasque = new JIPipeOrganizationMetadata.Builder()
                .name("Ikerbasque, Basque Foundation for Science, Bilbao, Spain")
                .build();
        final JIPipeOrganizationMetadata upv = new JIPipeOrganizationMetadata.Builder()
                .name("Computer Science and Artificial Intelligence Department, Basque Country University, San Sebastian, Spain")
                .build();
        final JIPipeOrganizationMetadata dipc = new JIPipeOrganizationMetadata.Builder()
                .name("Donostia International Physics Center, San Sebastian, Spain")
                .build();
        final JIPipeOrganizationMetadata harvardSeas = new JIPipeOrganizationMetadata.Builder()
                .name("Harvard John A. Paulson School of Engineering and Applied Sciences, Harvard University, Cambridge, MA, USA")
                .build();
        final JIPipeOrganizationMetadata loci = new JIPipeOrganizationMetadata.Builder()
                .name("Laboratory for Optical and Computational Instrumentation, University of Wisconsin, Madison, WI, USA")
                .build();
        final JIPipeOrganizationMetadata janelia = new JIPipeOrganizationMetadata.Builder()
                .name("Howard Hughes Medical Institute, Janelia Research Campus, Ashburn, VA, USA")
                .build();
        final JIPipeOrganizationMetadata princeton = new JIPipeOrganizationMetadata.Builder()
                .name("Neuroscience Institute and Computer Science Department, Princeton University, NJ, USA")
                .build();

        return new JIPipeAuthorMetadata.List(
                new JIPipeAuthorMetadata.Builder()
                        .firstName("Ignacio")
                        .lastName("Arganda-Carreras")
                        .affiliations(List.of(ikerbasque, upv, dipc))
                        .firstAuthor(true)
                        .correspondingAuthor(true)
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Verena")
                        .lastName("Kaynig")
                        .affiliations(List.of(harvardSeas))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Curtis")
                        .lastName("Rueden")
                        .affiliations(List.of(loci))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Kevin W.")
                        .lastName("Eliceiri")
                        .affiliations(List.of(loci))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Johannes")
                        .lastName("Schindelin")
                        .affiliations(List.of(loci))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Albert")
                        .lastName("Cardona")
                        .affiliations(List.of(janelia))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("H Sebastian")
                        .lastName("Seung")
                        .affiliations(List.of(princeton))
                        .build()
        );
    }

    @Override
    public StringList getDependencyCitations() {
        StringList strings = new StringList();
        strings.add("Arganda-Carreras, I., Kaynig, V., Rueden, C., Eliceiri, K. W., Schindelin, J., Cardona, A., & Seung, H. S. (2017). " +
                "Trainable Weka Segmentation: a machine learning tool for microscopy pixel classification. " +
                "Bioinformatics (Oxford Univ Press) 33 (15), doi:10.1093/bioinformatics/btx180");
        return strings;
    }

    @Override
    public String getName() {
        return "IJ Trainable Weka Filter integration";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates the Trainable Weka Filter into JIPipe");
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Collections.singletonList(new ImageIcon(getClass().getResource(RESOURCE_BASE_PATH + "/weka-32.png")));
    }

    @Override
    public void register(JIPipeService service, Context context, JIPipeProgressInfo progressInfo) {
        URL wekaModelIcon = getClass().getResource(RESOURCE_BASE_PATH + "/weka-model-data.png");
        URL wekaIcon = getClass().getResource(RESOURCE_BASE_PATH + "/weka.png");

        // Register new parameters
        registerEnumParameterType("weka-feature-2d", WekaFeature2D.class, "Weka Feature 2D", "A 2D Weka feature");
        registerParameterType("weka-feature-set-2d", WekaFeatureSet2D.class, JIPipeParameterArchetype.MultiSelect, "Weka Feature set 2D", "A collection of Weka 2D features");
        registerEnumParameterType("weka-feature-3d", WekaFeature3D.class, "Weka Feature 3D", "A 2D Weka feature");
        registerParameterType("weka-feature-set-3d", WekaFeatureSet3D.class, JIPipeParameterArchetype.MultiSelect, "Weka Feature set 3D", "A collection of Weka 3D features");
        registerParameterType("weka-classifier", WekaClassifierParameter.class, JIPipeParameterArchetype.Value, "Weka classifier", "Settings for a Weka classifier", WekaClassifierDesktopParameterEditorUI.class);

        // Register data types
        registerDatatype("weka-model", WekaModelData.class, wekaModelIcon);

        // Register nodes
        registerNodeType("import-weka-model-from-file", ImportWekaModelFromFileAlgorithm.class, wekaIcon);
        registerNodeType("weka-training-roi-2d", WekaTrainingROI2DAlgorithm.class, wekaIcon);
        registerNodeType("weka-training-roi-3d", WekaTrainingROI3DAlgorithm.class, wekaIcon);
        registerNodeType("weka-training-mask-2d", WekaTrainingMask2DAlgorithm.class, wekaIcon);
        registerNodeType("weka-training-label-2d", WekaTrainingLabels2DAlgorithm.class, wekaIcon);
        registerNodeType("weka-training-mask-2d-v2", WekaTrainingMask2DAlgorithm2.class, wekaIcon);
        registerNodeType("weka-training-label-2d-v2", WekaTrainingLabels2DAlgorithm2.class, wekaIcon);
        registerNodeType("weka-classification-2d", WekaClassification2DAlgorithm.class, wekaIcon);
        registerNodeType("weka-classification-3d", WekaClassification3DAlgorithm.class, wekaIcon);
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:ij-weka";
    }

}
