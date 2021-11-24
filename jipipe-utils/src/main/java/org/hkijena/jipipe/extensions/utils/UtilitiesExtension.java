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

package org.hkijena.jipipe.extensions.utils;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.grouping.NodeGroup;
import org.hkijena.jipipe.api.nodes.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.core.nodes.JIPipeCommentNode;
import org.hkijena.jipipe.extensions.datatables.algorithms.ExtractTableAlgorithm;
import org.hkijena.jipipe.extensions.datatables.algorithms.MergeDataToTableAlgorithm;
import org.hkijena.jipipe.extensions.filesystem.resultanalysis.CopyPathDataOperation;
import org.hkijena.jipipe.extensions.filesystem.resultanalysis.OpenPathDataOperation;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.utils.algorithms.*;
import org.hkijena.jipipe.extensions.utils.contextmenu.ParameterExplorerContextMenuAction;
import org.hkijena.jipipe.extensions.utils.datatypes.JIPipeOutputData;
import org.hkijena.jipipe.extensions.utils.datatypes.PathDataToJIPipeOutputConverter;
import org.hkijena.jipipe.extensions.utils.display.ImportJIPipeProjectDataOperation;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

@Plugin(type = JIPipeJavaExtension.class)
public class UtilitiesExtension extends JIPipePrepackagedDefaultJavaExtension {
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
    public void register() {
        registerDatatype("jipipe-run-output", JIPipeOutputData.class, UIUtils.getIconURLFromResources("apps/jipipe.png"), null, null, new OpenPathDataOperation(), new CopyPathDataOperation(), new ImportJIPipeProjectDataOperation());
        registerDatatypeConversion(new PathDataToJIPipeOutputConverter());

        registerNodeType("merge-data-to-table", MergeDataToTableAlgorithm.class, UIUtils.getIconURLFromResources("data-types/data-table.png"));
        registerNodeType("extract-table-to-data", ExtractTableAlgorithm.class, UIUtils.getIconURLFromResources("data-types/data.png"));

        registerNodeType("io-interface", IOInterfaceAlgorithm.class, UIUtils.getIconURLFromResources("devices/knemo-wireless-transmit-receive.png"));
        registerNodeType("node-group", NodeGroup.class, UIUtils.getIconURLFromResources("actions/object-group.png"));
        registerNodeType("converter", ConverterAlgorithm.class, UIUtils.getIconURLFromResources("actions/view-refresh.png"));
        registerNodeType("sort-rows-by-annotation", SortRowsAlgorithm.class, UIUtils.getIconURLFromResources("actions/sort-name.png"));
        registerNodeType("jipipe-run-project", RunJIPipeProjectAlgorithm.class, UIUtils.getIconURLFromResources("actions/run-build.png"));
        registerNodeType("jipipe-project-parameters", JIPipeProjectParameterDefinition.class, UIUtils.getIconURLFromResources("apps/jipipe.png"));
        registerNodeType("jipipe-project-parameters-from-paths", PathsToJIPipeProjectParametersAlgorithm.class, UIUtils.getIconURLFromResources("apps/jipipe.png"));
        registerNodeType("jipipe-output-get-slot-folder", GetJIPipeSlotFolderAlgorithm.class, UIUtils.getIconURLFromResources("actions/find.png"));
        registerNodeType("jipipe-output-import-slot-folder", ImportJIPipeSlotFolderAlgorithm.class, UIUtils.getIconURLFromResources("actions/document-import.png"));
        registerNodeType("select-data-table-rows", SelectDataTableRowsAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("run-expression", RunExpressionAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("generate-placeholder-for-missing-data", UsePlaceholderForMissingDataAlgorithm.class, UIUtils.getIconURLFromResources("actions/glob.png"));
        registerNodeType("data-batch-slicer", DataBatchSlicer.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("data-slicer", DataSlicer.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("distribute-data", DistributeDataRandomlyByPercentageAlgorithm.class, UIUtils.getIconURLFromResources("actions/distribute-randomize.png"));
        registerNodeType("distribute-data-by-count", DistributeDataRandomlyByCountAlgorithm.class, UIUtils.getIconURLFromResources("actions/distribute-randomize.png"));
        registerNodeType("data-to-string-data", ToDataStringAlgorithm.class, UIUtils.getIconURLFromResources("actions/edit-select-text.png"));

        // Comment node
        JIPipeJavaNodeInfo commentNodeInfo = new JIPipeJavaNodeInfo("jipipe:comment", JIPipeCommentNode.class);
        commentNodeInfo.setRunnable(false);
        registerNodeType(commentNodeInfo, UIUtils.getIconURLFromResources("actions/edit-comment.png"));

        // Parameter explorer
        registerContextMenuAction(new ParameterExplorerContextMenuAction());
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:utils";
    }

    @Override
    public String getDependencyVersion() {
        return "1.52.0";
    }
}
