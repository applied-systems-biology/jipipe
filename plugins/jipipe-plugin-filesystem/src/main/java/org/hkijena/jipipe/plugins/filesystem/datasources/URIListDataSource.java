package org.hkijena.jipipe.plugins.filesystem.datasources;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.strings.URIData;

@SetJIPipeDocumentation(name = "URI list", description = "A list of Universal Resource Identifiers (e.g., URLs, file URIs)")
@AddJIPipeOutputSlot(value = URIData.class, name = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class URIListDataSource extends JIPipeSimpleIteratingAlgorithm {

    private StringList uriList = new StringList();

    public URIListDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    public URIListDataSource(URIListDataSource other) {
        super(other);
        this.uriList = new StringList(other.uriList);
    }

    @SetJIPipeDocumentation(name = "URI List", description = "The list of URI")
    @JIPipeParameter("uri-list")
    @StringParameterSettings(monospace = true)
    public StringList getUriList() {
        return uriList;
    }

    @JIPipeParameter("uri-list")
    public void setUriList(StringList uriList) {
        this.uriList = uriList;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        for (String s : uriList) {
            iterationStep.addOutputData(getFirstOutputSlot(), new URIData(s), progressInfo);
        }
    }
}
