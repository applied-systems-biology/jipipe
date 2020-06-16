package org.hkijena.acaq5.api.grouping;

import com.fasterxml.jackson.annotation.JsonGetter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.grouping.parameters.NodeGroupContents;
import org.hkijena.acaq5.api.parameters.ACAQParameter;

/**
 * A sub-graph algorithm that can be defined by a user
 */
@ACAQDocumentation(name = "Group", description = "A sub-graph that contains its own pipeline.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Miscellaneous)
public class NodeGroup extends GraphWrapperAlgorithm {

    private NodeGroupContents contents;

    public NodeGroup(ACAQAlgorithmDeclaration declaration) {
        super(declaration, new ACAQAlgorithmGraph());
        contents = new NodeGroupContents();
        contents.setWrappedGraph(getWrappedGraph());
        contents.setParent(this);
    }

    public NodeGroup(GraphWrapperAlgorithm other) {
        super(other);
    }

    @ACAQDocumentation(name = "Wrapped graph", description = "The graph that is wrapped inside this node")
    @ACAQParameter("contents")
    public NodeGroupContents getContents() {
        return contents;
    }

    @ACAQParameter("contents")
    public void setContents(NodeGroupContents contents) {
        this.contents = contents;
        setWrappedGraph(contents.getWrappedGraph());
        contents.setParent(this);
    }

}
