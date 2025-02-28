package org.hkijena.jipipe.api.run;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.graph.GraphNodeReferenceParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;

import java.util.List;
import java.util.stream.Collectors;

public class JIPipeProjectRunSet extends AbstractJIPipeParameterCollection {
    private String name = "";
    private HTMLText description = new HTMLText();
    private GraphNodeReferenceParameter.List nodes = new GraphNodeReferenceParameter.List();
    private OptionalColorParameter color = new OptionalColorParameter();

    public JIPipeProjectRunSet() {
    }

    public JIPipeProjectRunSet(JIPipeProjectRunSet other) {
        setTo(other);
    }

    public void setTo(JIPipeProjectRunSet other) {
        this.name = other.name;
        this.description = new HTMLText(other.description);
        this.nodes = new GraphNodeReferenceParameter.List(other.nodes);
        this.color = new OptionalColorParameter(other.color);
    }

    @SetJIPipeDocumentation(name = "Color", description = "Custom color displayed in the user interface")
    @JIPipeParameter("color")
    @JsonGetter("color")
    public OptionalColorParameter getColor() {
        return color;
    }

    @JIPipeParameter("color")
    @JsonSetter("color")
    public void setColor(OptionalColorParameter color) {
        this.color = color;
    }

    @SetJIPipeDocumentation(name = "Description", description = "Custom description")
    @JIPipeParameter(value = "description", uiOrder = -90)
    @JsonGetter("description")
    public HTMLText getDescription() {
        return description;
    }

    @JIPipeParameter("description")
    @JsonSetter("description")
    public void setDescription(HTMLText description) {
        this.description = description;
    }

    @SetJIPipeDocumentation(name = "Name", description = "The name")
    @JIPipeParameter(value = "name", uiOrder = -100)
    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JIPipeParameter("name")
    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @SetJIPipeDocumentation(name = "Nodes", description = "List of nodes that are part of this run set")
    @JIPipeParameter("nodes")
    @JsonGetter("nodes")
    public GraphNodeReferenceParameter.List getNodes() {
        return nodes;
    }

    @JIPipeParameter("nodes")
    @JsonSetter("nodes")
    public void setNodes(GraphNodeReferenceParameter.List nodes) {
        this.nodes = nodes;
    }

    public boolean canResolveAllNodes(JIPipeProject project) {
        for (GraphNodeReferenceParameter node : nodes) {
            if(node.resolve(project) == null) {
                return false;
            }
        }
        return true;
    }

    public List<JIPipeGraphNode> resolveNodes(JIPipeProject project) {
        return nodes.stream().map(reference -> reference.resolve(project)).collect(Collectors.toList());
    }

}
