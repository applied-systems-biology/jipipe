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

package org.hkijena.jipipe.plugins.cellpose;

import com.google.common.collect.Sets;
import org.hkijena.jipipe.*;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ui.FileImageJDataImporterUI;
import org.hkijena.jipipe.api.compat.ui.FolderImageJDataExporterUI;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentArchetype;
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.metadata.JIPipeOrganizationMetadata;
import org.hkijena.jipipe.api.parameters.JIPipeParameterArchetype;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.cellpose.algorithms.ImportCellposeModelFromFileAlgorithm;
import org.hkijena.jipipe.plugins.cellpose.algorithms.ImportCellposeSizeModelFromFileAlgorithm;
import org.hkijena.jipipe.plugins.cellpose.algorithms.cp2.Cellpose2SegmentationInferenceAlgorithm;
import org.hkijena.jipipe.plugins.cellpose.algorithms.cp2.Cellpose2TrainingAlgorithm;
import org.hkijena.jipipe.plugins.cellpose.algorithms.cp2.ImportPretrainedCellpose2ModelAlgorithm;
import org.hkijena.jipipe.plugins.cellpose.algorithms.cp3.*;
import org.hkijena.jipipe.plugins.cellpose.datatypes.CellposeModelData;
import org.hkijena.jipipe.plugins.cellpose.datatypes.CellposeSizeModelData;
import org.hkijena.jipipe.plugins.cellpose.environments.cp2.Cellpose2Environment;
import org.hkijena.jipipe.plugins.cellpose.environments.cp2.Cellpose2EnvironmentList;
import org.hkijena.jipipe.plugins.cellpose.environments.cp2.OptionalCellpose2Environment;
import org.hkijena.jipipe.plugins.cellpose.environments.cp3.Cellpose3Environment;
import org.hkijena.jipipe.plugins.cellpose.environments.cp3.Cellpose3EnvironmentList;
import org.hkijena.jipipe.plugins.cellpose.environments.cp3.OptionalCellpose3Environment;
import org.hkijena.jipipe.plugins.cellpose.legacy.PretrainedLegacyCellpose2InferenceModel;
import org.hkijena.jipipe.plugins.cellpose.legacy.PretrainedLegacyCellpose2TrainingModel;
import org.hkijena.jipipe.plugins.cellpose.legacy.algorithms.*;
import org.hkijena.jipipe.plugins.cellpose.legacy.compat.LegacyCellposeModelImageJExporter;
import org.hkijena.jipipe.plugins.cellpose.legacy.compat.LegacyCellposeModelImageJImporter;
import org.hkijena.jipipe.plugins.cellpose.legacy.compat.LegacyCellposeSizeModelImageJExporter;
import org.hkijena.jipipe.plugins.cellpose.legacy.compat.LegacyCellposeSizeModelImageJImporter;
import org.hkijena.jipipe.plugins.cellpose.legacy.datatypes.LegacyCellposeModelData;
import org.hkijena.jipipe.plugins.cellpose.legacy.datatypes.LegacyCellposeSizeModelData;
import org.hkijena.jipipe.plugins.cellpose.parameters.cp2.PretrainedCellpose2SegmentationModel;
import org.hkijena.jipipe.plugins.cellpose.parameters.cp2.PretrainedCellpose2SegmentationModelList;
import org.hkijena.jipipe.plugins.cellpose.parameters.cp3.*;
import org.hkijena.jipipe.plugins.core.CorePlugin;
import org.hkijena.jipipe.plugins.imagejalgorithms.ImageJAlgorithmsPlugin;
import org.hkijena.jipipe.plugins.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.python.PythonPlugin;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Plugin(type = JIPipeJavaPlugin.class)
public class CellposePlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:cellpose",
            JIPipe.getJIPipeVersion(),
            "Cellpose integration");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(CellposePlugin.class, "org/hkijena/jipipe/plugins/cellpose");

    public CellposePlugin() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_DEEP_LEARNING, PluginCategoriesEnumParameter.CATEGORY_SEGMENTATION, PluginCategoriesEnumParameter.CATEGORY_MACHINE_LEARNING);
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CorePlugin.AS_DEPENDENCY, ImageJDataTypesPlugin.AS_DEPENDENCY, PythonPlugin.AS_DEPENDENCY, ImageJAlgorithmsPlugin.AS_DEPENDENCY);
    }

    @Override
    public JIPipeAuthorMetadata.List getAcknowledgements() {
        // Shared affiliation
        final JIPipeOrganizationMetadata janelia = new JIPipeOrganizationMetadata.Builder()
                .name("HHMI Janelia Research Campus, Ashburn, VA, USA")
                .ror("https://ror.org/013sk6x84")
                .website("https://www.janelia.org")
                .build();

        return new JIPipeAuthorMetadata.List(
                new JIPipeAuthorMetadata.Builder()
                        .firstName("Carsen")
                        .lastName("Stringer")
                        .affiliations(List.of(janelia))
                        .firstAuthor(true)
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Tim")
                        .lastName("Wang")
                        .affiliations(List.of(janelia))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Michalis")
                        .lastName("Michaelos")
                        .affiliations(List.of(janelia))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Marius")
                        .lastName("Pachitariu")
                        .affiliations(List.of(janelia))
                        .correspondingAuthor(true)
                        .build()
        );

    }

    @Override
    public StringList getDependencyCitations() {
        StringList strings = new StringList();
        strings.add("Stringer, C., Wang, T., Michaelos, M., & Pachitariu, M. (2021). Cellpose: a generalist algorithm for cellular segmentation. Nature Methods, 18(1), 100-106.");
        return strings;
    }

    @Override
    public String getName() {
        return "Cellpose integration";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates Cellpose");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:cellpose";
    }

    @Override
    public List<JIPipeJavaPluginSplashIcon> getSplashIcons() {
        return List.of(JIPipeJavaPluginSplashIcon.builder().id("cellpose").name("Cellpose").icon(JIPipe.RESOURCES.getIcon32("apps/cellpose.png")).url("https://www.cellpose.org/").build());
    }

    @Override
    public void register(JIPipeService service, Context context, JIPipeProgressInfo progressInfo) {

        registerArtifactEnvironment("cellpose2",
                "com.github.mouseland.cellpose:*",
                JIPipeEnvironmentArchetype.Managed, Cellpose2Environment.class,
                OptionalCellpose2Environment.class,
                Cellpose2EnvironmentList.class,
                "Cellpose 2.x",
                "A Python environment with Cellpose 2.x",
                JIPipe.RESOURCES.getIcon16("apps/cellpose.png"));
        registerArtifactEnvironment("cellpose3",
                "com.github.mouseland.cellpose3:*",
                JIPipeEnvironmentArchetype.Managed, Cellpose3Environment.class,
                OptionalCellpose3Environment.class,
                Cellpose3EnvironmentList.class,
                "Cellpose 3.x",
                "A Python environment with Cellpose 3.x",
                JIPipe.RESOURCES.getIcon16("apps/cellpose.png"));

        // Modern nodes and data types
        registerDatatype("cellpose-model-v2", CellposeModelData.class, JIPipe.RESOURCES.getIcon16URL("data-types/cellpose-model.png"));
        registerDatatype("cellpose-size-model-v2", CellposeSizeModelData.class, JIPipe.RESOURCES.getIcon16URL("data-types/cellpose-size-model.png"));

        registerEnumParameterType("cellpose-2.x-pretrained-model", PretrainedCellpose2SegmentationModel.class, "Cellpose 2.x pretrained model", "A pretrained model provided with Cellpose 2.x");
        registerParameterType("cellpose-2.x-pretrained-model-list", PretrainedCellpose2SegmentationModelList.class, JIPipeParameterArchetype.List, "Cellpose 2.x pretrained model list", "A list of pretrained Cellpose 2.x models");

        registerEnumParameterType("cellpose-3.x-pretrained-segmentation-model", PretrainedCellpose3SegmentationModel.class, "Cellpose 3.x pretrained segmentation model", "A pretrained segmentation model provided with Cellpose 3.x");
        registerParameterType("cellpose-3.x-pretrained-segmentation-model-list", PretrainedCellpose3SegmentationModelList.class, JIPipeParameterArchetype.List, "Cellpose 3.x pretrained segmentation model list", "A list of pretrained segmentation Cellpose 3.x models");
        registerEnumParameterType("cellpose-3.x-pretrained-denoise-model", PretrainedCellpose3DenoiseModel.class, "Cellpose 3.x pretrained segmentation model", "A pretrained segmentation model provided with Cellpose 3.x");
        registerParameterType("cellpose-3.x-pretrained-denoise-model-list", PretrainedCellpose3DenoiseModelList.class, JIPipeParameterArchetype.List, "Cellpose 3.x pretrained denoise model list", "A list of pretrained denoise Cellpose 3.x models");
        registerEnumParameterType("cellpose-3.x-denoise-noise-type", Cellpose3DenoiseTrainingNoiseType.class, "Cellpose 3.x noise type", "Available noise types for denoising");

        registerNodeType("import-cellpose-model-v2", ImportCellposeModelFromFileAlgorithm.class);
        registerNodeType("import-cellpose-size-model-v2", ImportCellposeSizeModelFromFileAlgorithm.class);

        // CP2 nodes
        registerNodeType("import-cellpose-2.x-pretrained-model", ImportPretrainedCellpose2ModelAlgorithm.class);
        registerNodeType("cellpose-inference-2.x", Cellpose2SegmentationInferenceAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/cellpose.png"));
        registerNodeType("cellpose-training-2.x", Cellpose2TrainingAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/cellpose.png"));

        // CP3 nodes
        registerNodeType("import-cellpose-3.x-pretrained-segmentation-model", ImportPretrainedCellpose3SegmentationModelAlgorithm.class);
        registerNodeType("import-cellpose-3.x-pretrained-denoise-model", ImportPretrainedCellpose3DenoiseModelAlgorithm.class);
        registerNodeType("cellpose-segmentation-inference-3.x", Cellpose3SegmentationInferenceAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/cellpose.png"));
        registerNodeType("cellpose-denoise-inference-3.x", Cellpose3DenoiseInferenceAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/cellpose.png"));
        registerNodeType("cellpose-segmentation-training-3.x", Cellpose3SegmentationTrainingAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/cellpose.png"));
        registerNodeType("cellpose-denoise-training-3.x", Cellpose3DenoiseTrainingAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/cellpose.png"));

        // Legacy nodes and data types
        registerEnumParameterType("cellpose-model", PretrainedLegacyCellpose2InferenceModel.class, "Cellpose model (deprecated)", "A Cellpose model");
        registerEnumParameterType("cellpose-pretrained-model", PretrainedLegacyCellpose2TrainingModel.class, "Cellpose pre-trained model (deprecated)", "A pretrained model for Cellpose");

        registerDatatype("cellpose-model", LegacyCellposeModelData.class, JIPipe.RESOURCES.getIcon16URL("data-types/cellpose-model.png"));
        registerImageJDataImporter("cellpose-model-from-file", new LegacyCellposeModelImageJImporter(), FileImageJDataImporterUI.class);
        registerImageJDataExporter("cellpose-model-to-directory", new LegacyCellposeModelImageJExporter(), FolderImageJDataExporterUI.class);
        registerDatatype("cellpose-size-model", LegacyCellposeSizeModelData.class, JIPipe.RESOURCES.getIcon16URL("data-types/cellpose-size-model.png"));
        registerImageJDataImporter("cellpose-size-model-from-file", new LegacyCellposeSizeModelImageJImporter(), FileImageJDataImporterUI.class);
        registerImageJDataExporter("cellpose-size-model-to-directory", new LegacyCellposeSizeModelImageJExporter(), FolderImageJDataExporterUI.class);

        registerNodeType("cellpose", Cellpose1InferenceAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("emblems/vcs-conflicting.png"));
        registerNodeType("cellpose-2", LegacyCellpose2InferenceAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/cellpose.png"));
        registerNodeType("cellpose-training", Cellpose1TrainingAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("emblems/vcs-conflicting.png"));
        registerNodeType("cellpose-training-2", LegacyCellpose2TrainingAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/cellpose.png"));
        registerNodeType("import-cellpose-model", ImportLegacyCellposeModelAlgorithm.class);
        registerNodeType("import-cellpose-size-model", ImportLegacyCellposeSizeModelAlgorithm.class);

        registerProjectTemplatesFromResources(RESOURCES, "templates");
    }
}
