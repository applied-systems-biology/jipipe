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

package org.hkijena.jipipe.extensions.ijweka;

import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.core.CoreExtension;
import org.hkijena.jipipe.extensions.filesystem.FilesystemExtension;
import org.hkijena.jipipe.extensions.ijweka.datatypes.WekaModelData;
import org.hkijena.jipipe.extensions.ijweka.nodes.*;
import org.hkijena.jipipe.extensions.ijweka.parameters.WekaClassifierParameter;
import org.hkijena.jipipe.extensions.ijweka.parameters.WekaClassifierParameterEditorUI;
import org.hkijena.jipipe.extensions.ijweka.parameters.features.WekaFeature2D;
import org.hkijena.jipipe.extensions.ijweka.parameters.features.WekaFeature3D;
import org.hkijena.jipipe.extensions.ijweka.parameters.features.WekaFeatureSet2D;
import org.hkijena.jipipe.extensions.ijweka.parameters.features.WekaFeatureSet3D;
import org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesExtension;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Plugin(type = JIPipeJavaExtension.class)
public class WekaExtension extends JIPipePrepackagedDefaultJavaExtension {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:ij-weka",
            JIPipe.getJIPipeVersion(),
            "IJ Trainable Weka Filter integration");
    public static final String RESOURCE_BASE_PATH = "/org/hkijena/jipipe/extensions/ijweka";

    public WekaExtension() {
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CoreExtension.AS_DEPENDENCY, FilesystemExtension.AS_DEPENDENCY, ImageJDataTypesExtension.AS_DEPENDENCY, ImageJAlgorithmsExtension.AS_DEPENDENCY);
    }

    @Override
    public PluginCategoriesEnumParameter.List getCategories() {
        return new PluginCategoriesEnumParameter.List(PluginCategoriesEnumParameter.CATEGORY_SEGMENTATION, PluginCategoriesEnumParameter.CATEGORY_MACHINE_LEARNING);
    }

    @Override
    public ImageParameter getThumbnail() {
        return new ImageParameter(ResourceUtils.getPluginResource("thumbnails/weka.png"));
    }

    @Override
    public JIPipeAuthorMetadata.List getAcknowledgements() {
        return new JIPipeAuthorMetadata.List(new JIPipeAuthorMetadata("", "Ignacio", "Arganda-Carreras", new StringList(
                "Ikerbasque, Basque Foundation for Science, Bilbao, Spain",
                "Computer Science and Artificial Intelligence Department, Basque Country University, San Sebastian, Spain",
                "Donostia International Physics Center, San Sebastian, Spain"
        ), "", "", true, true),
                new JIPipeAuthorMetadata("", "Verena", "Kaynig", new StringList(
                        "Harvard John A. Paulson School of Engineering and Applied Sciences, Harvard University, Cambridge, MA, USA"
                ), "", "", false, false),
                new JIPipeAuthorMetadata("", "Curtis", "Rueden", new StringList(
                        "Laboratory for Optical and Computational Instrumentation, University of Wisconsin, Madison, WI, USA"
                ), "", "", false, false),
                new JIPipeAuthorMetadata("", "Kevin W.", "Eliceiri", new StringList(
                        "Laboratory for Optical and Computational Instrumentation, University of Wisconsin, Madison, WI, USA"
                ), "", "", false, false),
                new JIPipeAuthorMetadata("", "Johannes", "Schindelin", new StringList(
                        "Laboratory for Optical and Computational Instrumentation, University of Wisconsin, Madison, WI, USA"
                ), "", "", false, false),
                new JIPipeAuthorMetadata("", "Albert", "Cardona", new StringList(
                        "Howard Hughes Medical Institute, Janelia Research Campus, Ashburn, VA, USA"
                ), "", "", false, false),
                new JIPipeAuthorMetadata("", "H Sebastian", "Seung", new StringList(
                        "Neuroscience Institute and Computer Science Department, Princeton University, NJ, USA"
                ), "", "", false, false));
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
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        URL wekaModelIcon = getClass().getResource(RESOURCE_BASE_PATH + "/weka-model-data.png");
        URL wekaIcon = getClass().getResource(RESOURCE_BASE_PATH + "/weka.png");

        // Register new parameters
        registerEnumParameterType("weka-feature-2d", WekaFeature2D.class, "Weka Feature 2D", "A 2D Weka feature");
        registerParameterType("weka-feature-set-2d", WekaFeatureSet2D.class, "Weka Feature set 2D", "A collection of Weka 2D features");
        registerEnumParameterType("weka-feature-3d", WekaFeature3D.class, "Weka Feature 3D", "A 2D Weka feature");
        registerParameterType("weka-feature-set-3d", WekaFeatureSet3D.class, "Weka Feature set 3D", "A collection of Weka 3D features");
        registerParameterType("weka-classifier", WekaClassifierParameter.class, "Weka classifier", "Settings for a Weka classifier", WekaClassifierParameterEditorUI.class);

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
