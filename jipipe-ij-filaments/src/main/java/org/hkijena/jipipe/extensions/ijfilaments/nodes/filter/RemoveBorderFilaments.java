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

package org.hkijena.jipipe.extensions.ijfilaments.nodes.filter;

import ij.ImagePlus;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

@SetJIPipeDocumentation(name = "Remove border filaments", description = "Removes filament components that are within a specified distance of the image borders.")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", create = true)
@AddJIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", create = true)
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
        Filaments3DData inputData = iterationStep.getInputData("Input", Filaments3DData.class, progressInfo);
        ImagePlus reference = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo).getImage();
        Filaments3DData outputData = new Filaments3DData(inputData);
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
