package org.hkijena.jipipe.extensions.ijfilaments.nodes.filter;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.jgrapht.alg.connectivity.ConnectivityInspector;

import java.util.HashSet;
import java.util.Set;

@JIPipeDocumentation(name = "Remove border filaments", description = "Removes filament components that are within a specified distance of the image borders.")
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true)
@JIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", autoCreate = true)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Filaments3DData inputData = dataBatch.getInputData("Input", Filaments3DData.class, progressInfo);
        ImagePlus reference = dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo).getImage();
        Filaments3DData outputData = new Filaments3DData(inputData);
        outputData.removeComponentsAtBorder(reference, removeInX, removeInY, removeInZ, useThickness, borderDistance);
        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @JIPipeDocumentation(name = "Check X coordinate", description = "If enabled, check if the object's X coordinate")
    @JIPipeParameter("remove-in-x")
    public boolean isRemoveInX() {
        return removeInX;
    }

    @JIPipeParameter("remove-in-x")
    public void setRemoveInX(boolean removeInX) {
        this.removeInX = removeInX;
    }

    @JIPipeDocumentation(name = "Check Y coordinate", description = "If enabled, check if the object's Y coordinate")
    @JIPipeParameter("remove-in-y")
    public boolean isRemoveInY() {
        return removeInY;
    }

    @JIPipeParameter("remove-in-y")
    public void setRemoveInY(boolean removeInY) {
        this.removeInY = removeInY;
    }

    @JIPipeDocumentation(name = "Check Z coordinate", description = "If enabled, check if the object's Z coordinate")
    @JIPipeParameter("remove-in-z")
    public boolean isRemoveInZ() {
        return removeInZ;
    }

    @JIPipeParameter("remove-in-z")
    public void setRemoveInZ(boolean removeInZ) {
        this.removeInZ = removeInZ;
    }

    @JIPipeDocumentation(name = "Border distance", description = "The maximum distance to the border (defaults to zero)")
    @JIPipeParameter("border-distance")
    public double getBorderDistance() {
        return borderDistance;
    }

    @JIPipeParameter("border-distance")
    public void setBorderDistance(double borderDistance) {
        this.borderDistance = borderDistance;
    }

    @JIPipeDocumentation(name = "Use thickness", description = "If enabled, the thickness of the vertices is considered while determining if a vertex crosses the border. Otherwise, only the center point is used (thickness zero)")
    @JIPipeParameter("use-thickness")
    public boolean isUseThickness() {
        return useThickness;
    }

    @JIPipeParameter("use-thickness")
    public void setUseThickness(boolean useThickness) {
        this.useThickness = useThickness;
    }
}