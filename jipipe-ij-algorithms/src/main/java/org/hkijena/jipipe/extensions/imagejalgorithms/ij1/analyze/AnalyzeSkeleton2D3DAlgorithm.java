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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.analyze;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.process.LUT;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeDocumentationDescription;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ReflectionUtils;
import sc.fiji.analyzeSkeleton.AnalyzeSkeleton_;
import sc.fiji.analyzeSkeleton.Edge;
import sc.fiji.analyzeSkeleton.Graph;
import sc.fiji.analyzeSkeleton.SkeletonResult;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

@JIPipeDocumentation(name = "Analyze skeleton 2D/3D", description = "Tags all pixel/voxels in a skeleton image and then counts all its junctions, triple and quadruple points and branches, and measures their average and maximum length.")
@JIPipeCitation("Arganda-Carreras, I., Fernández-González, R., Muñoz-Barrutia, A., & Ortiz-De-Solorzano, C. (2010). 3D reconstruction of histological sections: Application to mammary gland tissue. Microscopy Research and Technique, 73(11), 1019–1029. doi:10.1002/jemt.20829")
@JIPipeCitation("G. Polder, H.L.E Hovens and A.J Zweers, Measuring shoot length of submerged aquatic plants using graph analysis (2010), In: Proceedings of the ImageJ User and Developer Conference, Centre de Recherche Public Henri Tudor, Luxembourg, 27-29 October, pp 172-177.")
@JIPipeNode(menuPath = "Analyze", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlus3DGreyscaleMaskData.class, slotName = "Skeleton", autoCreate = true)
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", description = "ROI to exclude on pruning ends")
@JIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Reference", description = "Original grayscale input image (for lowest pixel intensity pruning mode)")
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Skeletons", autoCreate = true, description = "Table of all skeletons")
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Branches", autoCreate = true, description = "Table of all branches")
@JIPipeOutputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Tagged skeletons", autoCreate = true, description = "End-point voxels are displayed in blue, slab voxels in orange and junction voxels in purple")
@JIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Labels", autoCreate = true, description = "Label image of the skeletons")
@JIPipeOutputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Largest shortest paths", autoCreate = true, description = "The largest shortest path (in magenta)")
public class AnalyzeSkeleton2D3DAlgorithm extends JIPipeIteratingAlgorithm {

    public static final JIPipeDataSlotInfo ROI_INPUT_SLOT = new JIPipeDataSlotInfo(ROIListData.class, JIPipeSlotType.Input, "ROI", "ROI to exclude on pruning ends", null, true);
    public static final JIPipeDataSlotInfo REFERENCE_INPUT_SLOT = new JIPipeDataSlotInfo(ImagePlus3DGreyscale8UData.class, JIPipeSlotType.Input, "Reference", "Original grayscale input image (for lowest pixel intensity pruning mode)", null, true);

    private CycleRemovalMethod pruneCyclesMethod = CycleRemovalMethod.None;
    private EndRemovalMethod pruneEndsMethod = EndRemovalMethod.None;

    public AnalyzeSkeleton2D3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public AnalyzeSkeleton2D3DAlgorithm(AnalyzeSkeleton2D3DAlgorithm other) {
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
        if(pruneEndsMethod == EndRemovalMethod.ExcludeROI) {
            ROIListData roi = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
            if(roi != null && !roi.isEmpty()) {
                if(roi.size() == 1)
                    excludeRoi = roi.get(0);
                else {
                    roi = new ROIListData(roi);
                    roi.logicalOr();
                    excludeRoi = roi.get(0);
                }
            }
        }

        // Get reference image
        if(pruneCyclesMethod == CycleRemovalMethod.LowestIntensityBranch || pruneCyclesMethod == CycleRemovalMethod.LowestIntensityVoxel) {
            ImagePlus3DGreyscale8UData imageData = dataBatch.getInputData("Reference", ImagePlus3DGreyscale8UData.class, progressInfo);
            if(imageData != null) {
                referenceImage = imageData.getImage();
            }
        }

        AnalyzeSkeleton_ analyzeSkeleton = new AnalyzeSkeleton_();
        analyzeSkeleton.setup("", skeleton);
        SkeletonResult result = analyzeSkeleton.run(pruneCyclesMethod.getNativeValue(), pruneEndsMethod != EndRemovalMethod.None, true, referenceImage, true, true, excludeRoi);

        // Extract Skeletons
        ResultsTable skeletonsTable = calculateSkeletonTable(result);
        dataBatch.addOutputData("Skeletons", new ResultsTableData(skeletonsTable), progressInfo);

        // Extract Branches
        ResultsTable branchesTable = calculateBranchesTable(skeleton, analyzeSkeleton, result);
        dataBatch.addOutputData("Branches", new ResultsTableData(branchesTable), progressInfo);

        // Extract Tagged skeletons
        ImagePlus taggedSkeletons = generateTaggedSkeletons(skeleton, analyzeSkeleton);
        ImageJUtils.copyHyperstackDimensions(skeleton, taggedSkeletons);
        dataBatch.addOutputData("Tagged skeletons", new ImagePlusGreyscale8UData(taggedSkeletons), progressInfo);

        // Extract Labels
        ImagePlus labeledSkeletons = new ImagePlus("Labeled skeletons", analyzeSkeleton.getLabeledSkeletons());
        IJ.run(labeledSkeletons, "Fire", null);
        ImageJUtils.copyHyperstackDimensions(skeleton, labeledSkeletons);
        dataBatch.addOutputData("Labels", new ImagePlusGreyscale32FData(labeledSkeletons), progressInfo);

        // Extract Largest shortest paths
        ImagePlus largestShortestPaths = generateShortPathImage(skeleton, analyzeSkeleton);
        IJ.run(largestShortestPaths, "Fire", null);
        ImageJUtils.copyHyperstackDimensions(skeleton, largestShortestPaths);
        dataBatch.addOutputData("Largest shortest paths", new ImagePlusGreyscale8UData(largestShortestPaths), progressInfo);
    }

    private ImagePlus generateShortPathImage(ImagePlus imRef, AnalyzeSkeleton_ analyzeSkeleton) {
        ImageStack shortPathImage;
        try {
            Field field = AnalyzeSkeleton_.class.getDeclaredField("shortPathImage");
            field.setAccessible(true);
            shortPathImage = (ImageStack) field.get(analyzeSkeleton);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        ImagePlus shortIP = new ImagePlus("Longest shortest paths", shortPathImage.duplicate());
        shortIP.setCalibration(imRef.getCalibration());
        IJ.run(shortIP, "Fire", null);
        shortIP.resetDisplayRange();
        return shortIP;
    }

    private ImagePlus generateTaggedSkeletons(ImagePlus imRef, AnalyzeSkeleton_ analyzeSkeleton) {
        final int slices = imRef.getNSlices();
        final int frames = imRef.getNFrames();
        final int channels = imRef.getNChannels();
        ImageStack taggedImage;
        try {
            taggedImage = (ImageStack) MethodUtils.invokeMethod(analyzeSkeleton, true, "tagImage", imRef.getStack());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        ImagePlus tagIP = IJ.createHyperStack("Tagged skeleton", imRef.getWidth(), imRef.getHeight(), channels, slices, frames,
                imRef.getBitDepth());
        tagIP.setStack(taggedImage.duplicate(), channels, slices, frames);
        tagIP.setCalibration(imRef.getCalibration());
        IJ.run(tagIP, "Fire", null);
        tagIP.resetDisplayRange();
        return tagIP;
    }

    private ResultsTable calculateBranchesTable(ImagePlus imRef, AnalyzeSkeleton_ analyzeSkeleton, SkeletonResult result) {
        // New results table
        final ResultsTable extra_rt = new ResultsTable();
        int numOfTrees = result.getNumOfTrees();
        Graph[] graph = result.getGraph();

        final String[] extra_head = {"Branch", "Skeleton ID",
                "Branch length","V1 x", "V1 y",
                "V1 z","V2 x","V2 y", "V2 z", "Euclidean distance","running average length", "average intensity (inner 3rd)", "average intensity"};


        // Edge comparator (by branch length)
        Comparator<Edge> comp = new Comparator<Edge>(){
            public int compare(Edge o1, Edge o2)
            {
                final double diff = o1.getLength() - o2.getLength();
                if(diff < 0)
                    return 1;
                else if(diff == 0)
                    return 0;
                else
                    return -1;
            }
            public boolean equals(Object o)
            {
                return false;
            }
        };
        // Display branch information for each tree
        for(int i = 0 ; i < numOfTrees; i++)
        {
            final ArrayList<Edge> listEdges = graph[i].getEdges();
            // Sort branches by length
            listEdges.sort(comp);
            for(final Edge e : listEdges)
            {
                extra_rt.incrementCounter();
                extra_rt.addValue(extra_head[1], i+1);
                extra_rt.addValue(extra_head[2], e.getLength());
                extra_rt.addValue(extra_head[3], e.getV1().getPoints().get(0).x * imRef.getCalibration().pixelWidth);
                extra_rt.addValue(extra_head[4], e.getV1().getPoints().get(0).y * imRef.getCalibration().pixelHeight);
                extra_rt.addValue(extra_head[5], e.getV1().getPoints().get(0).z * imRef.getCalibration().pixelDepth);
                extra_rt.addValue(extra_head[6], e.getV2().getPoints().get(0).x * imRef.getCalibration().pixelWidth);
                extra_rt.addValue(extra_head[7], e.getV2().getPoints().get(0).y * imRef.getCalibration().pixelHeight);
                extra_rt.addValue(extra_head[8], e.getV2().getPoints().get(0).z * imRef.getCalibration().pixelDepth);
                extra_rt.addValue(extra_head[9], analyzeSkeleton.calculateDistance(e.getV1().getPoints().get(0), e.getV2().getPoints().get(0)));
                extra_rt.addValue(extra_head[10], e.getLength_ra());
                extra_rt.addValue(extra_head[11], e.getColor3rd());
                extra_rt.addValue(extra_head[12], e.getColor());
            }

        }
        return extra_rt;
    }

    private ResultsTable calculateSkeletonTable(SkeletonResult result) {
        final ResultsTable rt = new ResultsTable();
        rt.showRowNumbers( true );

        final String[] head = {"Skeleton", "# Branches","# Junctions", "# End-point voxels",
                "# Junction voxels","# Slab voxels","Average Branch Length",
                "# Triple points", "# Quadruple points", "Maximum Branch Length",
                "Longest Shortest Path", "spx", "spy", "spz"};

        int[] numberOfBranches = result.getBranches();
        int[] numberOfJunctions = result.getJunctions();
        int[] numberOfEndPoints = result.getEndPoints();
        int[] numberOfJunctionVoxels = result.getJunctionVoxels();
        int[] numberOfSlabs = result.getSlabs();
        double[] averageBranchLength = result.getAverageBranchLength();
        int[] numberOfTriplePoints = result.getTriples();
        int[] numberOfQuadruplePoints = result.getQuadruples();
        double[] maximumBranchLength = result.getMaximumBranchLength();
        ArrayList<Double> shortestPathList = result.getShortestPathList();
        double[][] spStartPosition = result.getSpStartPosition();

        for(int i = 0 ; i < result.getNumOfTrees(); i++)
        {
            rt.incrementCounter();

            rt.addValue(head[ 1], numberOfBranches[i]);
            rt.addValue(head[ 2], numberOfJunctions[i]);
            rt.addValue(head[ 3], numberOfEndPoints[i]);
            rt.addValue(head[ 4], numberOfJunctionVoxels[i]);
            rt.addValue(head[ 5], numberOfSlabs[i]);
            rt.addValue(head[ 6], averageBranchLength[i]);
            rt.addValue(head[ 7], numberOfTriplePoints[i]);
            rt.addValue(head[ 8], numberOfQuadruplePoints[i]);
            rt.addValue(head[ 9], maximumBranchLength[i]);
            if(null != shortestPathList)
            {
                rt.addValue(head[10],shortestPathList.get(i));
                rt.addValue(head[11],spStartPosition[i][0]);
                rt.addValue(head[12],spStartPosition[i][1]);
                rt.addValue(head[13],spStartPosition[i][2]);
            }
        }
        return rt;
    }

    @JIPipeDocumentation(name = "Prune cycles method", description = "Allows the selection of a method to prune possible loops in the skeleton")
    @JIPipeParameter("prune-cycles-method")
    public CycleRemovalMethod getPruneCyclesMethod() {
        return pruneCyclesMethod;
    }

    @JIPipeParameter("prune-cycles-method")
    public void setPruneCyclesMethod(CycleRemovalMethod pruneCyclesMethod) {
        this.pruneCyclesMethod = pruneCyclesMethod;
        toggleSlot(REFERENCE_INPUT_SLOT, pruneCyclesMethod == CycleRemovalMethod.LowestIntensityBranch || pruneCyclesMethod == CycleRemovalMethod.LowestIntensityVoxel);
    }

    @JIPipeDocumentation(name = "Prune ends methods", description = "Allows the selection of a method to prune any branch that ends in an end-point")
    @JIPipeParameter("prune-ends-method")
    public EndRemovalMethod getPruneEndsMethod() {
        return pruneEndsMethod;
    }

    @JIPipeParameter("prune-ends-method")
    public void setPruneEndsMethod(EndRemovalMethod pruneEndsMethod) {
        this.pruneEndsMethod = pruneEndsMethod;
        toggleSlot(ROI_INPUT_SLOT, pruneEndsMethod == EndRemovalMethod.ExcludeROI);
    }

    @JIPipeDocumentationDescription(description = "<ul>" +
            "<li>None: branches that end in an end point are not removed.</li>" +
            "<li>All:branches that end in an end point are removed.</li>" +
            "<li>Exclude ROI: branches that end in an end point are removed; regions within the ROI are excluded</li>" +
            "</ul>")
    public enum EndRemovalMethod {
        None,
        All,
        ExcludeROI;

        @Override
        public String toString() {
            if(this == ExcludeROI)
                return "Exclude ROI";
            return name();
        }
    }

    @JIPipeDocumentationDescription(description = "<ul>" +
            "<li>None: no cycle detection nor pruning is performed.</li>" +
            "<li>Shortest branch: the shortest branch among the loop branches will be cut in its middle point.</li>" +
            "<li>Lowest intensity voxel: the darkest voxel among the loop voxels will be cut (set to 0) in the input image.</li>" +
            "<li>Lowest intensity branch: the darkest (in average) branch among the loop branches will be cut in its darkest voxel.</li>" +
            "</ul>")
    public enum CycleRemovalMethod {
        None(0),
        ShortestBranch(1),
        LowestIntensityVoxel(2),
        LowestIntensityBranch(3);

        private final int nativeValue;
        CycleRemovalMethod(int nativeValue) {

            this.nativeValue = nativeValue;
        }

        public int getNativeValue() {
            return nativeValue;
        }


        @Override
        public String toString() {
            switch (this) {
                case ShortestBranch:
                    return "Shortest branch";
                case LowestIntensityVoxel:
                    return "Lowest intensity voxel";
                case LowestIntensityBranch:
                    return "Lowest intensity branch";
                default:
                    return name();
            }
        }
    }
}
