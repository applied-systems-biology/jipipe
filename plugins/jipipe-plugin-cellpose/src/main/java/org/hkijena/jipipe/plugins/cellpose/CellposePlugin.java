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
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ui.FileImageJDataImporterUI;
import org.hkijena.jipipe.api.compat.ui.FolderImageJDataExporterUI;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.cellpose.algorithms.*;
import org.hkijena.jipipe.plugins.cellpose.datatypes.CellposeModelData;
import org.hkijena.jipipe.plugins.cellpose.datatypes.CellposeSizeModelData;
import org.hkijena.jipipe.plugins.cellpose.legacy.PretrainedLegacyCellpose2InferenceModel;
import org.hkijena.jipipe.plugins.cellpose.legacy.PretrainedLegacyCellpose2TrainingModel;
import org.hkijena.jipipe.plugins.cellpose.legacy.algorithms.*;
import org.hkijena.jipipe.plugins.cellpose.legacy.compat.LegacyCellposeModelImageJExporter;
import org.hkijena.jipipe.plugins.cellpose.legacy.compat.LegacyCellposeModelImageJImporter;
import org.hkijena.jipipe.plugins.cellpose.legacy.compat.LegacyCellposeSizeModelImageJExporter;
import org.hkijena.jipipe.plugins.cellpose.legacy.compat.LegacyCellposeSizeModelImageJImporter;
import org.hkijena.jipipe.plugins.cellpose.legacy.datatypes.LegacyCellposeModelData;
import org.hkijena.jipipe.plugins.cellpose.legacy.datatypes.LegacyCellposeSizeModelData;
import org.hkijena.jipipe.plugins.cellpose.parameters.PretrainedCellpose2Model;
import org.hkijena.jipipe.plugins.cellpose.parameters.PretrainedCellpose2ModelList;
import org.hkijena.jipipe.plugins.core.CorePlugin;
import org.hkijena.jipipe.plugins.imagejalgorithms.ImageJAlgorithmsPlugin;
import org.hkijena.jipipe.plugins.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonPlugin;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.UIUtils;
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

    public static PythonEnvironment getEnvironment(JIPipeProject project, OptionalPythonEnvironment nodeEnvironment) {
        if (nodeEnvironment.isEnabled()) {
            return nodeEnvironment.getContent();
        }
        if (project != null && project.getSettingsSheet(CellposePluginProjectSettings.class).getProjectDefaultEnvironment().isEnabled()) {
            return project.getSettingsSheet(CellposePluginProjectSettings.class).getProjectDefaultEnvironment().getContent();
        }
        return Cellpose2PluginApplicationSettings.getInstance().getReadOnlyDefaultEnvironment();
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
        return new JIPipeAuthorMetadata.List(new JIPipeAuthorMetadata("",
                "Carsen",
                "Stringer",
                new StringList("HHMI Janelia Research Campus, Ashburn, VA, USA"),
                "",
                "",
                true,
                false),
                new JIPipeAuthorMetadata("",
                        "Tim",
                        "Wang",
                        new StringList("HHMI Janelia Research Campus, Ashburn, VA, USA"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Michalis",
                        "Michaelos",
                        new StringList("HHMI Janelia Research Campus, Ashburn, VA, USA"),
                        "",
                        "",
                        false,
                        false),
                new JIPipeAuthorMetadata("",
                        "Marius",
                        "Pachitariu",
                        new StringList("HHMI Janelia Research Campus, Ashburn, VA, USA"),
                        "",
                        "",
                        false,
                        true));
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
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(UIUtils.getIcon32FromResources("apps/cellpose.png"));
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerApplicationSettingsSheet(new Cellpose2PluginApplicationSettings());
        registerProjectSettingsSheet(CellposePluginProjectSettings.class);

        // Modern nodes and data types
        registerDatatype("cellpose-model-v2", CellposeModelData.class, UIUtils.getIconURLFromResources("data-types/cellpose-model.png"));
        registerDatatype("cellpose-size-model-v2", CellposeSizeModelData.class, UIUtils.getIconURLFromResources("data-types/cellpose-size-model.png"));

        registerEnumParameterType("cellpose-2.x-pretrained-model", PretrainedCellpose2Model.class, "Cellpose 2.x pretrained model", "A pretrained model provided with Cellpose 2.x");
        registerParameterType("cellpose-2.x-pretrained-model-list", PretrainedCellpose2ModelList.class, "Cellpose 2.x pretrained model list", "A list of pretrained Cellpose 2.x models");

        registerNodeType("import-cellpose-model-v2", ImportCellposeModelFromFileAlgorithm.class);
        registerNodeType("import-cellpose-size-model-v2", ImportCellposeSizeModelFromFileAlgorithm.class);
        registerNodeType("import-cellpose-2.x-pretrained-model", ImportPretrainedCellpose2ModelAlgorithm.class);

        registerNodeType("cellpose-inference-2.x", Cellpose2InferenceAlgorithm.class, UIUtils.getIconURLFromResources("apps/cellpose.png"));
        registerNodeType("cellpose-training-2.x", Cellpose2TrainingAlgorithm.class, UIUtils.getIconURLFromResources("apps/cellpose.png"));

        // Legacy nodes and data types
        registerEnumParameterType("cellpose-model", PretrainedLegacyCellpose2InferenceModel.class, "Cellpose model (deprecated)", "A Cellpose model");
        registerEnumParameterType("cellpose-pretrained-model", PretrainedLegacyCellpose2TrainingModel.class, "Cellpose pre-trained model (deprecated)", "A pretrained model for Cellpose");

        registerDatatype("cellpose-model", LegacyCellposeModelData.class, UIUtils.getIconURLFromResources("data-types/cellpose-model.png"));
        registerImageJDataImporter("cellpose-model-from-file", new LegacyCellposeModelImageJImporter(), FileImageJDataImporterUI.class);
        registerImageJDataExporter("cellpose-model-to-directory", new LegacyCellposeModelImageJExporter(), FolderImageJDataExporterUI.class);
        registerDatatype("cellpose-size-model", LegacyCellposeSizeModelData.class, UIUtils.getIconURLFromResources("data-types/cellpose-size-model.png"));
        registerImageJDataImporter("cellpose-size-model-from-file", new LegacyCellposeSizeModelImageJImporter(), FileImageJDataImporterUI.class);
        registerImageJDataExporter("cellpose-size-model-to-directory", new LegacyCellposeSizeModelImageJExporter(), FolderImageJDataExporterUI.class);

        registerNodeType("cellpose", Cellpose1InferenceAlgorithm.class, UIUtils.getIconURLFromResources("emblems/vcs-conflicting.png"));
        registerNodeType("cellpose-2", LegacyCellpose2InferenceAlgorithm.class, UIUtils.getIconURLFromResources("apps/cellpose.png"));
        registerNodeType("cellpose-training", Cellpose1TrainingAlgorithm.class, UIUtils.getIconURLFromResources("emblems/vcs-conflicting.png"));
        registerNodeType("cellpose-training-2", LegacyCellpose2TrainingAlgorithm.class, UIUtils.getIconURLFromResources("apps/cellpose.png"));
        registerNodeType("import-cellpose-model", ImportLegacyCellposeModelAlgorithm.class);
        registerNodeType("import-cellpose-size-model", ImportLegacyCellposeSizeModelAlgorithm.class);

        registerProjectTemplatesFromResources(RESOURCES, "templates");
    }
}
