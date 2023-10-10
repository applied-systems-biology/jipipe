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
import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.ijfilaments.util.Point3d;
import org.hkijena.jipipe.extensions.imagejalgorithms.nodes.analyze.AnalyzeSkeleton2D3DAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;
import sc.fiji.analyzeSkeleton.*;

import java.util.IdentityHashMap;
import java.util.Map;

@JIPipeDocumentation(name = "Binary skeleton to simplified filaments", description = "Converts a binary skeleton into filaments by utilizing the Analyze Skeleton (2D/3D) plugin from Fiji. " +
        "Please note that the generated filaments are were automatically simplified by the method and might not fully represent anymore all structural information contained within the skeleton.")
@JIPipeCitation("Arganda-Carreras, I., Fernández-González, R., Muñoz-Barrutia, A., & Ortiz-De-Solorzano, C. (2010). 3D reconstruction of histological sections: Application to mammary gland tissue. Microscopy Research and Technique, 73(11), 1019–1029. doi:10.1002/jemt.20829")
@JIPipeCitation("G. Polder, H.L.E Hovens and A.J Zweers, Measuring shoot length of submerged aquatic plants using graph analysis (2010), In: Proceedings of the ImageJ User and Developer Conference, Centre de Recherche Public Henri Tudor, Luxembourg, 27-29 October, pp 172-177.")
@JIPipeNode(menuPath = "Convert", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlus3DGreyscaleMaskData.class, slotName = "Skeleton", autoCreate = true)
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", description = "ROI to exclude on pruning ends")
@JIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Reference", description = "Original grayscale input image (for lowest pixel intensity pruning mode)")
@JIPipeOutputSlot(value = Filaments3DData.class, slotName = "Filaments", description = "The filaments as extracted by the algorithm", autoCreate = true)
public class SkeletonToSimplifiedFilamentsFijiAlgorithm extends JIPipeIteratingAlgorithm {

    public static final JIPipeDataSlotInfo ROI_INPUT_SLOT = new JIPipeDataSlotInfo(ROIListData.class, JIPipeSlotType.Input, "ROI", "ROI to exclude on pruning ends", true);
    public static final JIPipeDataSlotInfo REFERENCE_INPUT_SLOT = new JIPipeDataSlotInfo(ImagePlus3DGreyscale8UData.class, JIPipeSlotType.Input, "Reference", "Original grayscale input image (for lowest pixel intensity pruning mode)", true);
    private AnalyzeSkeleton2D3DAlgorithm.CycleRemovalMethod pruneCyclesMethod = AnalyzeSkeleton2D3DAlgorithm.CycleRemovalMethod.None;
    private AnalyzeSkeleton2D3DAlgorithm.EndRemovalMethod pruneEndsMethod = AnalyzeSkeleton2D3DAlgorithm.EndRemovalMethod.None;

    public SkeletonToSimplifiedFilamentsFijiAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SkeletonToSimplifiedFilamentsFijiAlgorithm(SkeletonToSimplifiedFilamentsFijiAlgorithm other) {
        super(other);
        setPruneCyclesMethod(other.pruneCyclesMethod);
        setPruneEndsMethod(other.pruneEndsMethod);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus skeleton = dataBatch.getInputData("Skeleton", ImagePlus3DGreyscaleMaskData.class, progressInfo).getImage();
        Roi excludeRoi = null;
        ImagePlus referenceImage = null;

        // Get the excluded ROI
        if (pruneEndsMethod == AnalyzeSkeleton2D3DAlgorithm.EndRemovalMethod.ExcludeROI) {
            ROIListData roi = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
            if (roi != null && !roi.isEmpty()) {
                if (roi.size() == 1)
                    excludeRoi = roi.get(0);
                else {
                    roi = new ROIListData(roi);
                    roi.logicalOr();
                    excludeRoi = roi.get(0);
                }
            }
        }

        // Get reference image
        if (pruneCyclesMethod == AnalyzeSkeleton2D3DAlgorithm.CycleRemovalMethod.LowestIntensityBranch || pruneCyclesMethod == AnalyzeSkeleton2D3DAlgorithm.CycleRemovalMethod.LowestIntensityVoxel) {
            ImagePlus3DGreyscale8UData imageData = dataBatch.getInputData("Reference", ImagePlus3DGreyscale8UData.class, progressInfo);
            if (imageData != null) {
                referenceImage = imageData.getImage();
            }
        }

        AnalyzeSkeleton_ analyzeSkeleton = new AnalyzeSkeleton_();
        analyzeSkeleton.setup("", skeleton);
        SkeletonResult result = analyzeSkeleton.run(pruneCyclesMethod.getNativeValue(), pruneEndsMethod != AnalyzeSkeleton2D3DAlgorithm.EndRemovalMethod.None, true, referenceImage, true, true, excludeRoi);

        // Generate filaments
        Filaments3DData filamentsData = new Filaments3DData();
        for (Graph graph : result.getGraph()) {
            Map<Vertex, FilamentVertex> vertexMapping = new IdentityHashMap<>();
            for (Vertex vertex : graph.getVertices()) {
                int numPoints = vertex.getPoints().size();
                double x = 0;
                double y = 0;
                double z = 0;
                for (Point point : vertex.getPoints()) {
                    x += point.x;
                    y += point.y;
                    z += point.z;
                }
                x /= numPoints;
                y /= numPoints;
                z /= numPoints;

                FilamentVertex filamentVertex = new FilamentVertex();
                filamentVertex.setSpatialLocation(new Point3d((int) Math.round(x), (int) Math.round(y), (int) Math.round(z)));
                vertexMapping.put(vertex, filamentVertex);
                filamentsData.addVertex(filamentVertex);
            }
            for (Edge edge : graph.getEdges()) {
                Vertex v1 = edge.getV1();
                Vertex v2 = edge.getV2();
                FilamentEdge filamentEdge = new FilamentEdge();
                FilamentVertex filamentVertex1 = vertexMapping.get(v1);
                FilamentVertex filamentVertex2 = vertexMapping.get(v2);
                filamentsData.addEdgeIgnoreLoops(filamentVertex1, filamentVertex2, filamentEdge);
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), filamentsData, progressInfo);
    }

    @JIPipeDocumentation(name = "Prune cycles method", description = "Allows the selection of a method to prune possible loops in the skeleton")
    @JIPipeParameter("prune-cycles-method")
    public AnalyzeSkeleton2D3DAlgorithm.CycleRemovalMethod getPruneCyclesMethod() {
        return pruneCyclesMethod;
    }

    @JIPipeParameter("prune-cycles-method")
    public void setPruneCyclesMethod(AnalyzeSkeleton2D3DAlgorithm.CycleRemovalMethod pruneCyclesMethod) {
        this.pruneCyclesMethod = pruneCyclesMethod;
        toggleSlot(REFERENCE_INPUT_SLOT, pruneCyclesMethod == AnalyzeSkeleton2D3DAlgorithm.CycleRemovalMethod.LowestIntensityBranch || pruneCyclesMethod == AnalyzeSkeleton2D3DAlgorithm.CycleRemovalMethod.LowestIntensityVoxel);
    }

    @JIPipeDocumentation(name = "Prune ends methods", description = "Allows the selection of a method to prune any branch that ends in an end-point")
    @JIPipeParameter("prune-ends-method")
    public AnalyzeSkeleton2D3DAlgorithm.EndRemovalMethod getPruneEndsMethod() {
        return pruneEndsMethod;
    }

    @JIPipeParameter("prune-ends-method")
    public void setPruneEndsMethod(AnalyzeSkeleton2D3DAlgorithm.EndRemovalMethod pruneEndsMethod) {
        this.pruneEndsMethod = pruneEndsMethod;
        toggleSlot(ROI_INPUT_SLOT, pruneEndsMethod == AnalyzeSkeleton2D3DAlgorithm.EndRemovalMethod.ExcludeROI);
    }
}
