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

package org.hkijena.jipipe.api.grouping;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterEditorUI;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameters;
import org.hkijena.jipipe.api.grouping.parameters.NodeGroupContents;
import org.hkijena.jipipe.api.grouping.parameters.NodeGroupContentsParameterEditorUI;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

/**
 * Extension for anything that is related to {@link GraphWrapperAlgorithm}
 */
@Plugin(type = JIPipeJavaExtension.class)
public class GroupingExtension extends JIPipePrepackagedDefaultJavaExtension {

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Node grouping";
    }

    @Override
    public String getDescription() {
        return "Provides algorithms that wrap around algorithm graphs";
    }

    @Override
    public void register() {
        registerNodeType("graph-wrapper:input", GraphWrapperAlgorithmInput.class, UIUtils.getIconURLFromResources("actions/plug.png"));
        registerNodeType("graph-wrapper:output", GraphWrapperAlgorithmOutput.class, UIUtils.getIconURLFromResources("actions/plug.png"));

        registerParameterType("node-group:content",
                NodeGroupContents.class,
                null,
                null,
                "Group content",
                "Node group contents",
                NodeGroupContentsParameterEditorUI.class);
        registerParameterType("graph-node-parameters",
                GraphNodeParameters.class,
                null,
                null,
                "Exported parameters",
                "Organizes parameters sourced from another graph",
                GraphNodeParameterEditorUI.class);
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:grouping";
    }

    @Override
    public String getDependencyVersion() {
        return "2020.10";
    }
}
