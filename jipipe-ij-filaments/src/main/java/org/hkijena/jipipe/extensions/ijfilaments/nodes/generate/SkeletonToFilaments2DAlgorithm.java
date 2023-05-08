/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 *
 */

package org.hkijena.jipipe.extensions.ijfilaments.nodes.generate;

import ij.ImagePlus;
import ij.measure.Calibration;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.ijfilaments.util.NonSpatialPoint3d;
import org.hkijena.jipipe.extensions.ijfilaments.util.Point3d;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@JIPipeDocumentation(name = "Binary skeleton to 2D filaments", description = "Applies a simple algorithm that converts a binary skeleton into a filament. This algorithm only supports 2D data and will apply the processing per Z/C/T slice.")
@JIPipeNode(menuPath = "Convert", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Skeleton", autoCreate = true)
@JIPipeOutputSlot(value = Filaments3DData.class, slotName = "Filaments", description = "The filaments as extracted by the algorithm", autoCreate = true)
public class SkeletonToFilaments2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean force2D = false;

    public SkeletonToFilaments2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SkeletonToFilaments2DAlgorithm(SkeletonToFilaments2DAlgorithm other) {
        super(other);
        this.force2D = other.force2D;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus skeleton = dataBatch.getInputData("Skeleton", ImagePlusData.class, progressInfo).getImage();
        Filaments3DData filamentsData = new Filaments3DData();

        Calibration calibration = skeleton.getCalibration();

        ImageJUtils.forEachIndexedZCTSlice(skeleton, (ip, index) -> {
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

        dataBatch.addOutputData(getFirstOutputSlot(), filamentsData, progressInfo);
    }

    @JIPipeDocumentation(name = "Force 2D", description = "Sets the calibration parameters so that the filament is only present in two dimensions.")
    @JIPipeParameter("force-2d")
    public boolean isForce2D() {
        return force2D;
    }

    @JIPipeParameter("force-2d")
    public void setForce2D(boolean force2D) {
        this.force2D = force2D;
    }
}
