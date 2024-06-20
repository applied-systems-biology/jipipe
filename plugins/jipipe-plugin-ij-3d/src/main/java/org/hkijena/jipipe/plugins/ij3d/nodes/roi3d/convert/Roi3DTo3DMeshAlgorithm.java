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

package org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.convert;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.plugins.scene3d.datatypes.Scene3DData;
import org.hkijena.jipipe.plugins.scene3d.model.geometries.Scene3DUnindexedMeshGeometry;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;

@SetJIPipeDocumentation(name = "3D ROI to 3D scene", description = "Converts 3D ROI into a 3D scene.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = ROI3DListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Scene3DData.class, name = "Output", create = true)
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

    @SetJIPipeDocumentation(name = "Smooth meshes", description = "If enabled, smooth the output meshes")
    @JIPipeParameter("smooth")
    public boolean isSmooth() {
        return smooth;
    }

    @JIPipeParameter("smooth")
    public void setSmooth(boolean smooth) {
        this.smooth = smooth;
    }

    @SetJIPipeDocumentation(name = "Mesh length", description = "If 'Force mesh length' and 'Physical dimensions' are enabled, scale the mesh to that 1 unit in its coordinate system is of the specified unit.")
    @JIPipeParameter("mesh-length-unit")
    public Quantity.LengthUnit getMeshLengthUnit() {
        return meshLengthUnit;
    }

    @JIPipeParameter("mesh-length-unit")
    public void setMeshLengthUnit(Quantity.LengthUnit meshLengthUnit) {
        this.meshLengthUnit = meshLengthUnit;
    }

    @SetJIPipeDocumentation(name = "Force mesh length", description = "If this option and 'Physical dimensions' are enabled, scale the mesh to that 1 unit in its coordinate system is of the unit specified in 'Mesh length'.")
    @JIPipeParameter("force-mesh-length-unit")
    public boolean isForceMeshLengthUnit() {
        return forceMeshLengthUnit;
    }

    @JIPipeParameter("force-mesh-length-unit")
    public void setForceMeshLengthUnit(boolean forceMeshLengthUnit) {
        this.forceMeshLengthUnit = forceMeshLengthUnit;
    }

    @SetJIPipeDocumentation(name = "Physical sizes", description = "If enabled, the physical voxel size is considered during the generation of the mesh")
    @JIPipeParameter("physical-sizes")
    public boolean isPhysicalSizes() {
        return physicalSizes;
    }

    @JIPipeParameter("physical-sizes")
    public void setPhysicalSizes(boolean physicalSizes) {
        this.physicalSizes = physicalSizes;
    }

    @SetJIPipeDocumentation(name = "Mesh name", description = "The prefix of the mesh")
    @JIPipeParameter("mesh-name-prefix")
    public String getMeshNamePrefix() {
        return meshNamePrefix;
    }

    @JIPipeParameter("mesh-name-prefix")
    public void setMeshNamePrefix(String meshNamePrefix) {
        this.meshNamePrefix = meshNamePrefix;
    }

    @SetJIPipeDocumentation(name = "Override mesh color", description = "Overrides the color (diffuse) of the mesh. Otherwise, the color is taken from the ROI")
    @JIPipeParameter("override-mesh-color")
    public OptionalColorParameter getOverrideMeshColor() {
        return overrideMeshColor;
    }

    @JIPipeParameter("override-mesh-color")
    public void setOverrideMeshColor(OptionalColorParameter overrideMeshColor) {
        this.overrideMeshColor = overrideMeshColor;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI3DListData rois = iterationStep.getInputData(getFirstInputSlot(), ROI3DListData.class, progressInfo);
        Scene3DData scene3DData = new Scene3DData();
        for (int i = 0; i < rois.size(); i++) {
            ROI3D roi3D = rois.get(i);
            JIPipeProgressInfo roiProgress = progressInfo.resolveAndLog("ROI", i, rois.size());
            Scene3DUnindexedMeshGeometry geometry = roi3D.toGeometry(overrideMeshColor.getContentOrDefault(null), physicalSizes, forceMeshLengthUnit, meshLengthUnit, smooth, roiProgress);
            geometry.setName(StringUtils.nullToEmpty(meshNamePrefix) + StringUtils.nullToEmpty(geometry.getName()));
            scene3DData.add(geometry);
        }
        iterationStep.addOutputData(getFirstOutputSlot(), scene3DData, progressInfo);
    }
}
