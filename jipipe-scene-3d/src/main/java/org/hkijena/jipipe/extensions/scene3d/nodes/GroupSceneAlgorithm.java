package org.hkijena.jipipe.extensions.scene3d.nodes;


import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.scene3d.datatypes.Scene3DData;
import org.hkijena.jipipe.extensions.scene3d.model.Scene3DGroupNode;

@JIPipeDocumentation(name = "Group 3D scene objects", description = "Puts the objects in the incoming 3D scene into a group")
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "3D Scenes")
@JIPipeInputSlot(value = Scene3DData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = Scene3DData.class, slotName = "Output", autoCreate = true)
public class GroupSceneAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private String groupName;

    public GroupSceneAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public GroupSceneAlgorithm(GroupSceneAlgorithm other) {
        super(other);
        this.groupName = other.groupName;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        Scene3DData inputData = iterationStep.getInputData(getFirstInputSlot(), Scene3DData.class, progressInfo);
        Scene3DData outputData = new Scene3DData();
        Scene3DGroupNode groupNode = new Scene3DGroupNode();
        groupNode.setName(groupName);
        groupNode.getChildren().addAll(inputData);
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @JIPipeDocumentation(name = "Group name", description = "The optional name of the group")
    @JIPipeParameter("group-name")
    public String getGroupName() {
        return groupName;
    }

    @JIPipeParameter("group-name")
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
