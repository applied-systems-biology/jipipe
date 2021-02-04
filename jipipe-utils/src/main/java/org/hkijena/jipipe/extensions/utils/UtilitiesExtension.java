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
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.filesystem.resultanalysis.CopyPathDataOperation;
import org.hkijena.jipipe.extensions.filesystem.resultanalysis.OpenPathDataOperation;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.utils.algorithms.ConverterAlgorithm;
import org.hkijena.jipipe.extensions.utils.algorithms.GetJIPipeSlotFolderAlgorithm;
import org.hkijena.jipipe.extensions.utils.algorithms.ImportJIPipeSlotFolderAlgorithm;
import org.hkijena.jipipe.extensions.utils.algorithms.JIPipeProjectParameterDefinition;
import org.hkijena.jipipe.extensions.utils.algorithms.PathsToJIPipeProjectParametersAlgorithm;
import org.hkijena.jipipe.extensions.utils.algorithms.RunJIPipeProjectAlgorithm;
import org.hkijena.jipipe.extensions.utils.algorithms.SortRowsAlgorithm;
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
    public String getDescription() {
        return "Utility algorithms";
    }

    @Override
    public void register() {
        registerDatatype("jipipe-run-output", JIPipeOutputData.class, UIUtils.getIconURLFromResources("apps/jipipe.png"), null, null, new OpenPathDataOperation(), new CopyPathDataOperation(), new ImportJIPipeProjectDataOperation());
        registerDatatypeConversion(new PathDataToJIPipeOutputConverter());

        registerNodeType("io-interface", IOInterfaceAlgorithm.class, UIUtils.getIconURLFromResources("devices/knemo-wireless-transmit-receive.png"));
        registerNodeType("node-group", NodeGroup.class, UIUtils.getIconURLFromResources("actions/object-group.png"));
        registerNodeType("converter", ConverterAlgorithm.class, UIUtils.getIconURLFromResources("actions/view-refresh.png"));
        registerNodeType("sort-rows-by-annotation", SortRowsAlgorithm.class, UIUtils.getIconURLFromResources("actions/sort-name.png"));
        registerNodeType("jipipe-run-project", RunJIPipeProjectAlgorithm.class, UIUtils.getIconURLFromResources("actions/run-build.png"));
        registerNodeType("jipipe-project-parameters", JIPipeProjectParameterDefinition.class, UIUtils.getIconURLFromResources("apps/jipipe.png"));
        registerNodeType("jipipe-project-parameters-from-paths", PathsToJIPipeProjectParametersAlgorithm.class, UIUtils.getIconURLFromResources("apps/jipipe.png"));
        registerNodeType("jipipe-output-get-slot-folder", GetJIPipeSlotFolderAlgorithm.class, UIUtils.getIconURLFromResources("actions/find.png"));
        registerNodeType("jipipe-output-import-slot-folder", ImportJIPipeSlotFolderAlgorithm.class, UIUtils.getIconURLFromResources("actions/document-import.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:utils";
    }

    @Override
    public String getDependencyVersion() {
        return "2021.2";
    }
}
