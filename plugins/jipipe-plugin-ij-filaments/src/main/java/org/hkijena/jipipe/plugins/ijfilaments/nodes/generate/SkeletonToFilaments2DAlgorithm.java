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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.generate;

import ij.ImagePlus;
import ij.measure.Calibration;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.ijfilaments.util.NonSpatialPoint3d;
import org.hkijena.jipipe.plugins.ijfilaments.util.Point3d;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "Binary skeleton to 2D filaments", description = "Applies a simple algorithm that converts a binary skeleton into a filament. This algorithm only supports 2D data and will apply the processing per Z/C/T slice. Please note that by default " +
        "the Z voxel size is set to zero.")
@ConfigureJIPipeNode(menuPath = "Convert", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Skeleton", create = true)
@AddJIPipeOutputSlot(value = Filaments3DGraphData.class, name = "Filaments", description = "The filaments as extracted by the algorithm", create = true)
public class SkeletonToFilaments2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean force2D = true;

    public SkeletonToFilaments2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SkeletonToFilaments2DAlgorithm(SkeletonToFilaments2DAlgorithm other) {
        super(other);
        this.force2D = other.force2D;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus skeleton = iterationStep.getInputData("Skeleton", ImagePlusData.class, progressInfo).getImage();
        Filaments3DGraphData filamentsData = new Filaments3DGraphData();

        Calibration calibration = skeleton.getCalibration();

        ImageJIterationUtils.forEachIndexedZCTSlice(skeleton, (ip, index) -> {
            Map<Point, FilamentVertex> vertexMap = new HashMap<>();

            // Collect vertices
            int width = ip.getWidth();
            int height = ip.getHeight();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (ip.get(x, y) > 0) {
                        FilamentVertex vertex = new FilamentVertex();
                        vertex.setSpatialLocation(new Point3d(x, y, index.getZ()));
                        vertex.setNonSpatialLocation(new NonSpatialPoint3d(index.getC(), index.getT()));
                        if (calibration != null) {
                            if (!StringUtils.isNullOrEmpty(calibration.getXUnit())) {
                                vertex.setPhysicalVoxelSizeX(new Quantity(calibration.pixelWidth, calibration.getXUnit()));
                                vertex.setPhysicalVoxelSizeY(new Quantity(calibration.pixelWidth, calibration.getXUnit())); // X = Y condition
                                vertex.setPhysicalVoxelSizeZ(new Quantity(0, calibration.getZUnit())); // X = Y = Z condition
                            }
                            if (!StringUtils.isNullOrEmpty(calibration.getYUnit())) {
                                vertex.setPhysicalVoxelSizeY(new Quantity(calibration.pixelHeight, calibration.getYUnit()));
                            }
                            if (!force2D && !StringUtils.isNullOrEmpty(calibration.getZUnit())) {
                                vertex.setPhysicalVoxelSizeZ(new Quantity(calibration.pixelDepth, calibration.getZUnit()));
                            }
                        }
                        filamentsData.addVertex(vertex);
                        vertexMap.put(new Point(x, y), vertex);
                    }
                }
            }

            // Collect edges
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (ip.get(x, y) > 0) {
                        FilamentVertex source = vertexMap.get(new Point(x, y));
                        for (int sy = y - 1; sy <= y + 1; sy++) {
                            for (int sx = x - 1; sx <= x + 1; sx++) {
                                if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
                                    if (sx == x && sy == y)
                                        continue;
                                    if (ip.get(sx, sy) > 0) {
                                        FilamentVertex target = vertexMap.get(new Point(sx, sy));
                                        filamentsData.addEdgeIgnoreLoops(source, target);
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }, progressInfo);

        filamentsData.removeSelfEdges();

        iterationStep.addOutputData(getFirstOutputSlot(), filamentsData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Zero Z voxel size (2D)", description = "Sets the calibration parameters so that the filament is only present in two dimensions by setting the Z voxel size to zero. If not set and filaments are present in 2D only, issues regarding calibrated lengths might arise.")
    @JIPipeParameter(value = "force-2d", important = true)
    public boolean isForce2D() {
        return force2D;
    }

    @JIPipeParameter("force-2d")
    public void setForce2D(boolean force2D) {
        this.force2D = force2D;
    }
}
