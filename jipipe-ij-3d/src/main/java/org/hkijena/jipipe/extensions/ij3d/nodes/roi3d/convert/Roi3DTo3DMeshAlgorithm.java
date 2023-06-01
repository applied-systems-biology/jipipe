package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.convert;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.extensions.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.extensions.scene3d.datatypes.Scene3DData;
import org.hkijena.jipipe.extensions.scene3d.model.geometries.Scene3DUnindexedMeshGeometry;
import org.hkijena.jipipe.extensions.scene3d.utils.MarchingCubes;
import org.hkijena.jipipe.extensions.scene3d.utils.Scene3DUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;

@JIPipeDocumentation(name = "3D ROI to 3D scene", description = "Converts 3D ROI into a 3D scene.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = ROI3DListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = Scene3DData.class, slotName = "Output", autoCreate = true)
public class Roi3DTo3DMeshAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private String meshNamePrefix;
    private OptionalColorParameter overrideMeshColor = new OptionalColorParameter(Color.RED, false);
    private Quantity.LengthUnit meshLengthUnit = Quantity.LengthUnit.mm;
    private boolean forceMeshLengthUnit = true;
    private boolean physicalSizes = true;

    private boolean smooth = false;

    public Roi3DTo3DMeshAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public Roi3DTo3DMeshAlgorithm(Roi3DTo3DMeshAlgorithm other) {
        super(other);
        this.meshNamePrefix = other.meshNamePrefix;
        this.overrideMeshColor = other.overrideMeshColor;
        this.meshLengthUnit = other.meshLengthUnit;
        this.forceMeshLengthUnit = other.forceMeshLengthUnit;
        this.physicalSizes = other.physicalSizes;
        this.smooth = other.smooth;
    }

    @JIPipeDocumentation(name = "Smooth meshes", description = "If enabled, smooth the output meshes")
    @JIPipeParameter("smooth")
    public boolean isSmooth() {
        return smooth;
    }

    @JIPipeParameter("smooth")
    public void setSmooth(boolean smooth) {
        this.smooth = smooth;
    }

    @JIPipeDocumentation(name = "Mesh length", description = "If 'Force mesh length' and 'Physical dimensions' are enabled, scale the mesh to that 1 unit in its coordinate system is of the specified unit.")
    @JIPipeParameter("mesh-length-unit")
    public Quantity.LengthUnit getMeshLengthUnit() {
        return meshLengthUnit;
    }

    @JIPipeParameter("mesh-length-unit")
    public void setMeshLengthUnit(Quantity.LengthUnit meshLengthUnit) {
        this.meshLengthUnit = meshLengthUnit;
    }

    @JIPipeDocumentation(name = "Force mesh length", description = "If this option and 'Physical dimensions' are enabled, scale the mesh to that 1 unit in its coordinate system is of the unit specified in 'Mesh length'.")
    @JIPipeParameter("force-mesh-length-unit")
    public boolean isForceMeshLengthUnit() {
        return forceMeshLengthUnit;
    }

    @JIPipeParameter("force-mesh-length-unit")
    public void setForceMeshLengthUnit(boolean forceMeshLengthUnit) {
        this.forceMeshLengthUnit = forceMeshLengthUnit;
    }

    @JIPipeDocumentation(name = "Physical sizes", description = "If enabled, the physical voxel size is considered during the generation of the mesh")
    @JIPipeParameter("physical-sizes")
    public boolean isPhysicalSizes() {
        return physicalSizes;
    }

    @JIPipeParameter("physical-sizes")
    public void setPhysicalSizes(boolean physicalSizes) {
        this.physicalSizes = physicalSizes;
    }

    @JIPipeDocumentation(name = "Mesh name", description = "The prefix of the mesh")
    @JIPipeParameter("mesh-name-prefix")
    public String getMeshNamePrefix() {
        return meshNamePrefix;
    }

    @JIPipeParameter("mesh-name-prefix")
    public void setMeshNamePrefix(String meshNamePrefix) {
        this.meshNamePrefix = meshNamePrefix;
    }

    @JIPipeDocumentation(name = "Override mesh color", description = "Overrides the color (diffuse) of the mesh. Otherwise, the color is taken from the ROI")
    @JIPipeParameter("override-mesh-color")
    public OptionalColorParameter getOverrideMeshColor() {
        return overrideMeshColor;
    }

    @JIPipeParameter("override-mesh-color")
    public void setOverrideMeshColor(OptionalColorParameter overrideMeshColor) {
        this.overrideMeshColor = overrideMeshColor;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROI3DListData rois = dataBatch.getInputData(getFirstInputSlot(), ROI3DListData.class, progressInfo);
        Scene3DData scene3DData = new Scene3DData();
        for (int i = 0; i < rois.size(); i++) {
            ROI3D roi3D = rois.get(i);
            JIPipeProgressInfo roiProgress = progressInfo.resolveAndLog("ROI", i, rois.size());
            Scene3DUnindexedMeshGeometry geometry = roi3D.toGeometry(overrideMeshColor.getContentOrDefault(null), physicalSizes, forceMeshLengthUnit, meshLengthUnit, smooth, roiProgress);
            geometry.setName(StringUtils.nullToEmpty(meshNamePrefix) + StringUtils.nullToEmpty(geometry.getName()));
            scene3DData.add(geometry);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), scene3DData, progressInfo);
    }
}
