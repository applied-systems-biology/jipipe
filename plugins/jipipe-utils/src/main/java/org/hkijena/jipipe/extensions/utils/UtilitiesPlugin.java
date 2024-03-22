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

package org.hkijena.jipipe.extensions.utils;

import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.grouping.JIPipeNodeGroup;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.extensions.core.CorePlugin;
import org.hkijena.jipipe.extensions.core.nodes.JIPipeCommentNode;
import org.hkijena.jipipe.extensions.filesystem.FilesystemPlugin;
import org.hkijena.jipipe.extensions.filesystem.resultanalysis.CopyPathDataOperation;
import org.hkijena.jipipe.extensions.filesystem.resultanalysis.OpenPathDataOperation;
import org.hkijena.jipipe.extensions.multiparameters.MultiParameterAlgorithmsPlugin;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.utils.algorithms.*;
import org.hkijena.jipipe.extensions.utils.algorithms.datatable.*;
import org.hkijena.jipipe.extensions.utils.algorithms.distribute.DistributeDataRandomlyByCountAlgorithm;
import org.hkijena.jipipe.extensions.utils.algorithms.distribute.DistributeDataRandomlyByPercentageAlgorithm;
import org.hkijena.jipipe.extensions.utils.algorithms.iterationsteps.CrasherAlgorithm;
import org.hkijena.jipipe.extensions.utils.algorithms.iterationsteps.MultiIterationStepCheckerAlgorithm;
import org.hkijena.jipipe.extensions.utils.algorithms.iterationsteps.SingleIterationStepCheckerAlgorithm;
import org.hkijena.jipipe.extensions.utils.algorithms.meta.GetJIPipeSlotFolderAlgorithm;
import org.hkijena.jipipe.extensions.utils.algorithms.meta.JIPipeProjectParameterDefinition;
import org.hkijena.jipipe.extensions.utils.algorithms.meta.PathsToJIPipeProjectParametersAlgorithm;
import org.hkijena.jipipe.extensions.utils.algorithms.meta.RunJIPipeProjectAlgorithm;
import org.hkijena.jipipe.extensions.utils.algorithms.processes.RunProcessIteratingAlgorithm;
import org.hkijena.jipipe.extensions.utils.algorithms.processes.RunProcessMergingAlgorithm;
import org.hkijena.jipipe.extensions.utils.contextmenu.ParameterExplorerContextMenuAction;
import org.hkijena.jipipe.extensions.utils.datatypes.JIPipeOutputData;
import org.hkijena.jipipe.extensions.utils.datatypes.PathDataToJIPipeOutputConverter;
import org.hkijena.jipipe.extensions.utils.display.ImportJIPipeProjectDataOperation;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.util.Set;

@Plugin(type = JIPipeJavaPlugin.class)
public class UtilitiesPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:utils",
            JIPipe.getJIPipeVersion(),
            "Utilities");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(UtilitiesPlugin.class, "org/hkijena/jipipe/extensions/utils");

    public UtilitiesPlugin() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_DATA_PROCESSING, PluginCategoriesEnumParameter.CATEGORY_SCRIPTING);
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CorePlugin.AS_DEPENDENCY, FilesystemPlugin.AS_DEPENDENCY, MultiParameterAlgorithmsPlugin.AS_DEPENDENCY);
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Utilities";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Utility algorithms");
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerDatatype("jipipe-run-output", JIPipeOutputData.class, UIUtils.getIconURLFromResources("apps/jipipe.png"), new OpenPathDataOperation(), new CopyPathDataOperation(), new ImportJIPipeProjectDataOperation());
        registerDatatypeConversion(new PathDataToJIPipeOutputConverter());

        registerNodeType("merge-data-to-table", MergeDataToTableAlgorithm.class, UIUtils.getIconURLFromResources("data-types/data-table.png"));
        registerNodeType("extract-table-to-data", ExtractTableAlgorithm.class, UIUtils.getIconURLFromResources("data-types/data.png"));
        registerNodeType("data-table-annotate-with-properties", AnnotateWithDataTableProperties.class, UIUtils.getIconURLFromResources("data-types/data-table.png"));
        registerNodeType("data-table-pull-annotations", PullDataTableAnnotations.class, UIUtils.getIconURLFromResources("actions/document-export.png"));
        registerNodeType("data-table-push-annotations", PushDataTableAnnotations.class, UIUtils.getIconURLFromResources("actions/document-import.png"));

        registerNodeType("io-interface", IOInterfaceAlgorithm.class, UIUtils.getIconURLFromResources("devices/knemo-wireless-transmit-receive.png"));
        registerNodeType("node-group", JIPipeNodeGroup.class, UIUtils.getIconURLFromResources("actions/object-group.png"));
        registerNodeType("converter", ConverterAlgorithm.class, UIUtils.getIconURLFromResources("actions/view-refresh.png"));
        registerNodeType("sort-rows-by-annotation", SortRowsByAnnotationsAlgorithm.class, UIUtils.getIconURLFromResources("actions/sort-name.png"));
        registerNodeType("sort-rows-by-expression", SortRowsByExpressionAlgorithm.class, UIUtils.getIconURLFromResources("actions/sort-name.png"));
        registerNodeType("jipipe-run-project", RunJIPipeProjectAlgorithm.class, UIUtils.getIconURLFromResources("actions/run-build.png"));
        registerNodeType("jipipe-project-parameters", JIPipeProjectParameterDefinition.class, UIUtils.getIconURLFromResources("apps/jipipe.png"));
        registerNodeType("jipipe-project-parameters-from-paths", PathsToJIPipeProjectParametersAlgorithm.class, UIUtils.getIconURLFromResources("apps/jipipe.png"));
        registerNodeType("jipipe-output-get-slot-folder", GetJIPipeSlotFolderAlgorithm.class, UIUtils.getIconURLFromResources("actions/find.png"));
        registerNodeType("jipipe-output-import-slot-folder", ImportJIPipeSlotFolderAlgorithm.class, UIUtils.getIconURLFromResources("actions/document-import.png"));
        registerNodeType("select-data-table-rows", SelectDataTableRowsAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("run-expression", RunExpressionAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("generate-placeholder-for-missing-data", UsePlaceholderForMissingDataAlgorithm.class, UIUtils.getIconURLFromResources("actions/glob.png"));
        registerNodeType("data-batch-slicer", IterationStepSlicer.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("data-slicer", DataSlicer.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("distribute-data", DistributeDataRandomlyByPercentageAlgorithm.class, UIUtils.getIconURLFromResources("actions/distribute-randomize.png"));
        registerNodeType("distribute-data-by-count", DistributeDataRandomlyByCountAlgorithm.class, UIUtils.getIconURLFromResources("actions/distribute-randomize.png"));
        registerNodeType("data-to-string-data", ToDataStringAlgorithm.class, UIUtils.getIconURLFromResources("actions/edit-select-text.png"));

        registerNodeType("run-process-iterating", RunProcessIteratingAlgorithm.class, UIUtils.getIconURLFromResources("actions/cm_runterm.png"));
        registerNodeType("run-process-merging", RunProcessMergingAlgorithm.class, UIUtils.getIconURLFromResources("actions/cm_runterm.png"));

        registerNodeType("sleep", SleepAlgorithm.class, UIUtils.getIconURLFromResources("actions/clock.png"));
        registerNodeType("duplicate", DuplicateDataAlgorithm.class, UIUtils.getIconURLFromResources("actions/edit-duplicate.png"));

        registerNodeType("check-iteration-step-single", SingleIterationStepCheckerAlgorithm.class, UIUtils.getIconURLFromResources("actions/package.png"));
        registerNodeType("check-iteration-step-multi", MultiIterationStepCheckerAlgorithm.class, UIUtils.getIconURLFromResources("actions/package.png"));

        registerNodeType("crasher-on-input", CrasherAlgorithm.class, UIUtils.getIconURLFromResources("actions/error.png"));

        // Comment node
        JIPipeJavaNodeInfo commentNodeInfo = new JIPipeJavaNodeInfo("jipipe:comment", JIPipeCommentNode.class);
        commentNodeInfo.setRunnable(false);
        registerNodeType(commentNodeInfo, UIUtils.getIconURLFromResources("actions/edit-comment.png"));

        // Parameter explorer
        registerContextMenuAction(new ParameterExplorerContextMenuAction());

        registerNodeExamplesFromResources(RESOURCES, "examples");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:utils";
    }

}
