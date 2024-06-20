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

package org.hkijena.jipipe.plugins.scene3d.nodes;


import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.scene3d.datatypes.Scene3DData;
import org.hkijena.jipipe.plugins.scene3d.model.Scene3DGroupNode;

@SetJIPipeDocumentation(name = "Group 3D scene objects", description = "Puts the objects in the incoming 3D scene into a group")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "3D Scenes")
@AddJIPipeInputSlot(value = Scene3DData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Scene3DData.class, name = "Output", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Scene3DData inputData = iterationStep.getInputData(getFirstInputSlot(), Scene3DData.class, progressInfo);
        Scene3DData outputData = new Scene3DData();
        Scene3DGroupNode groupNode = new Scene3DGroupNode();
        groupNode.setName(groupName);
        groupNode.getChildren().addAll(inputData);
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Group name", description = "The optional name of the group")
    @JIPipeParameter("group-name")
    public String getGroupName() {
        return groupName;
    }

    @JIPipeParameter("group-name")
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
