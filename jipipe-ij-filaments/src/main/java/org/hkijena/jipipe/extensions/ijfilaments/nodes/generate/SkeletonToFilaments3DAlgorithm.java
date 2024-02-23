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
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.ijfilaments.util.NonSpatialPoint3d;
import org.hkijena.jipipe.extensions.ijfilaments.util.Point3d;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "Binary skeleton to 3D filaments", description = "Applies a simple algorithm that converts a binary skeleton into a filament. " +
        "This algorithm 3D data and will apply the processing per C/T stack.")
@ConfigureJIPipeNode(menuPath = "Convert", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Skeleton", create = true)
@AddJIPipeOutputSlot(value = Filaments3DData.class, slotName = "Filaments", description = "The filaments as extracted by the algorithm", create = true)
public class SkeletonToFilaments3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public SkeletonToFilaments3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SkeletonToFilaments3DAlgorithm(SkeletonToFilaments3DAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus skeleton = iterationStep.getInputData("Skeleton", ImagePlusData.class, progressInfo).getImage();
        Filaments3DData filamentsData = new Filaments3DData();

        Calibration calibration = skeleton.getCalibration();

        ImageJUtils.forEachIndexedCTStack(skeleton, (imp, index, ctProgress) -> {
            Map<Point3d, FilamentVertex> vertexMap = new HashMap<>();

            // percentage tracking
            int lastPercentage = -1;

            // Collect vertices
            for (int z = 0; z < imp.getNSlices(); z++) {
                ImageProcessor ip = imp.getImageStack().getProcessor(z + 1);
                int width = ip.getWidth();
                int height = ip.getHeight();

                int newPercentage = (int) (0.5 * z / imp.getNSlices() * 100);
                if (newPercentage != lastPercentage) {
                    ctProgress.log(newPercentage + "%");
                    lastPercentage = newPercentage;
                }

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        if (ip.get(x, y) > 0) {
                            FilamentVertex vertex = new FilamentVertex();
                            vertex.setSpatialLocation(new Point3d(x, y, z));
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
                                if (!StringUtils.isNullOrEmpty(calibration.getZUnit())) {
                                    vertex.setPhysicalVoxelSizeZ(new Quantity(calibration.pixelDepth, calibration.getZUnit()));
                                }
                            }
                            filamentsData.addVertex(vertex);
                            vertexMap.put(new Point3d(x, y, z), vertex);
                        }
                    }
                }
            }

            ctProgress.log("Detected " + vertexMap.size() + " vertices");

            // Collect edges
            for (int z = 0; z < imp.getNSlices(); z++) {
                ImageProcessor lastIp = z > 0 ? imp.getImageStack().getProcessor(z) : null;
                ImageProcessor nextIp = z < imp.getNSlices() - 1 ? imp.getImageStack().getProcessor(z + 2) : null;
                ImageProcessor ip = imp.getImageStack().getProcessor(z + 1);
                int width = ip.getWidth();
                int height = ip.getHeight();

                int newPercentage = (int) ((0.5 + (0.5 * z / imp.getNSlices())) * 100);
                if (newPercentage != lastPercentage) {
                    ctProgress.log(newPercentage + "%");
                    lastPercentage = newPercentage;
                }

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        if (ip.get(x, y) > 0) {
                            // Connections to the current layer
                            FilamentVertex source = vertexMap.get(new Point3d(x, y, z));

                            // In last layer
                            if (lastIp != null && lastIp.get(x, y) > 0) {
                                for (int sy = y - 1; sy <= y + 1; sy++) {
                                    for (int sx = x - 1; sx <= x + 1; sx++) {
                                        if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
                                            if (lastIp.get(sx, sy) > 0) {
                                                FilamentVertex target = vertexMap.get(new Point3d(sx, sy, z - 1));
                                                filamentsData.addEdgeIgnoreLoops(source, target);
                                            }
                                        }
                                    }
                                }
                            }

                            // In current layer
                            for (int sy = y - 1; sy <= y + 1; sy++) {
                                for (int sx = x - 1; sx <= x + 1; sx++) {
                                    if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
                                        if (sx == x && sy == y)
                                            continue;
                                        if (ip.get(sx, sy) > 0) {
                                            FilamentVertex target = vertexMap.get(new Point3d(sx, sy, z));
                                            filamentsData.addEdgeIgnoreLoops(source, target);
                                        }
                                    }
                                }
                            }

                            // In next layer
                            if (nextIp != null && nextIp.get(x, y) > 0) {
                                for (int sy = y - 1; sy <= y + 1; sy++) {
                                    for (int sx = x - 1; sx <= x + 1; sx++) {
                                        if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
                                            if (nextIp.get(sx, sy) > 0) {
                                                FilamentVertex target = vertexMap.get(new Point3d(sx, sy, z + 1));
                                                filamentsData.addEdgeIgnoreLoops(source, target);
                                            }
                                        }
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
}
