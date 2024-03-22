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

package org.hkijena.jipipe.api.grouping;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReferenceGroupCollection;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReferenceGroupCollectionDesktopParameterEditorUI;
import org.hkijena.jipipe.api.grouping.parameters.NodeGroupContents;
import org.hkijena.jipipe.api.grouping.parameters.NodeGroupContentsDesktopParameterEditorUI;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

/**
 * Extension for anything that is related to {@link JIPipeGraphWrapperAlgorithm}
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class GroupingPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Node grouping";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides algorithms that wrap around algorithm graphs");
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerNodeType("graph-wrapper:input", GraphWrapperAlgorithmInput.class, UIUtils.getIconURLFromResources("actions/plug.png"));
        registerNodeType("graph-wrapper:output", GraphWrapperAlgorithmOutput.class, UIUtils.getIconURLFromResources("actions/plug.png"));

        registerParameterType("node-group:content",
                NodeGroupContents.class,
                null,
                null,
                "Group content",
                "Node group contents",
                NodeGroupContentsDesktopParameterEditorUI.class);
        registerParameterType("graph-node-parameters",
                GraphNodeParameterReferenceGroupCollection.class,
                null,
                null,
                "Exported parameters",
                "Organizes parameters sourced from another graph",
                GraphNodeParameterReferenceGroupCollectionDesktopParameterEditorUI.class);
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:grouping";
    }

    @Override
    public boolean isCoreExtension() {
        return true;
    }

}
