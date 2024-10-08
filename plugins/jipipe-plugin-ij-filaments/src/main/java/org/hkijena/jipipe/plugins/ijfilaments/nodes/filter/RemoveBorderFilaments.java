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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.filter;

import ij.ImagePlus;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;

@SetJIPipeDocumentation(name = "Remove border filaments", description = "Removes filament components that are within a specified distance of the image borders.")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = Filaments3DGraphData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", create = true)
@AddJIPipeOutputSlot(value = Filaments3DGraphData.class, name = "Output", create = true)
public class RemoveBorderFilaments extends JIPipeIteratingAlgorithm {

    private boolean removeInX = true;

    private boolean removeInY = true;
    private boolean removeInZ = true;

    private boolean useThickness = true;
    private double borderDistance = 0;

    public RemoveBorderFilaments(JIPipeNodeInfo info) {
        super(info);
    }

    public RemoveBorderFilaments(RemoveBorderFilaments other) {
        super(other);
        this.removeInX = other.removeInX;
        this.removeInY = other.removeInY;
        this.removeInZ = other.removeInZ;
        this.borderDistance = other.borderDistance;
        this.useThickness = other.useThickness;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DGraphData inputData = iterationStep.getInputData("Input", Filaments3DGraphData.class, progressInfo);
        ImagePlus reference = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo).getImage();
        Filaments3DGraphData outputData = new Filaments3DGraphData(inputData);
        outputData.removeComponentsAtBorder(reference, removeInX, removeInY, removeInZ, useThickness, borderDistance);
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Check X coordinate", description = "If enabled, check if the object's X coordinate")
    @JIPipeParameter("remove-in-x")
    public boolean isRemoveInX() {
        return removeInX;
    }

    @JIPipeParameter("remove-in-x")
    public void setRemoveInX(boolean removeInX) {
        this.removeInX = removeInX;
    }

    @SetJIPipeDocumentation(name = "Check Y coordinate", description = "If enabled, check if the object's Y coordinate")
    @JIPipeParameter("remove-in-y")
    public boolean isRemoveInY() {
        return removeInY;
    }

    @JIPipeParameter("remove-in-y")
    public void setRemoveInY(boolean removeInY) {
        this.removeInY = removeInY;
    }

    @SetJIPipeDocumentation(name = "Check Z coordinate", description = "If enabled, check if the object's Z coordinate")
    @JIPipeParameter("remove-in-z")
    public boolean isRemoveInZ() {
        return removeInZ;
    }

    @JIPipeParameter("remove-in-z")
    public void setRemoveInZ(boolean removeInZ) {
        this.removeInZ = removeInZ;
    }

    @SetJIPipeDocumentation(name = "Border distance", description = "The maximum distance to the border (defaults to zero)")
    @JIPipeParameter("border-distance")
    public double getBorderDistance() {
        return borderDistance;
    }

    @JIPipeParameter("border-distance")
    public void setBorderDistance(double borderDistance) {
        this.borderDistance = borderDistance;
    }

    @SetJIPipeDocumentation(name = "Use thickness", description = "If enabled, the thickness of the vertices is considered while determining if a vertex crosses the border. Otherwise, only the center point is used (thickness zero)")
    @JIPipeParameter("use-thickness")
    public boolean isUseThickness() {
        return useThickness;
    }

    @JIPipeParameter("use-thickness")
    public void setUseThickness(boolean useThickness) {
        this.useThickness = useThickness;
    }
}
