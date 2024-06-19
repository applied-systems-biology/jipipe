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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.analyze;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale8UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import sc.fiji.analyzeSkeleton.AnalyzeSkeleton_;
import sc.fiji.analyzeSkeleton.Edge;
import sc.fiji.analyzeSkeleton.Graph;
import sc.fiji.analyzeSkeleton.SkeletonResult;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;

@SetJIPipeDocumentation(name = "Analyze skeleton 2D/3D", description = "Tags all pixel/voxels in a skeleton image and then counts all its junctions, triple and quadruple points and branches, and measures their average and maximum length.")
@AddJIPipeCitation("Arganda-Carreras, I., Fernández-González, R., Muñoz-Barrutia, A., & Ortiz-De-Solorzano, C. (2010). 3D reconstruction of histological sections: Application to mammary gland tissue. Microscopy Research and Technique, 73(11), 1019–1029. doi:10.1002/jemt.20829")
@AddJIPipeCitation("G. Polder, H.L.E Hovens and A.J Zweers, Measuring shoot length of submerged aquatic plants using graph analysis (2010), In: Proceedings of the ImageJ User and Developer Conference, Centre de Recherche Public Henri Tudor, Luxembourg, 27-29 October, pp 172-177.")
@ConfigureJIPipeNode(menuPath = "Analyze", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlus3DGreyscaleMaskData.class, name = "Skeleton", create = true)
@AddJIPipeInputSlot(value = ROIListData.class, name = "ROI", description = "ROI to exclude on pruning ends")
@AddJIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, name = "Reference", description = "Original grayscale input image (for lowest pixel intensity pruning mode)")
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Skeletons", description = "Table of all skeletons")
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Branches", description = "Table of all branches")
@AddJIPipeOutputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Tagged skeletons", description = "End-point voxels are displayed in blue, slab voxels in orange and junction voxels in purple")
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Labels", description = "Label image of the skeletons")
@AddJIPipeOutputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Largest shortest paths", description = "The largest shortest path (in magenta)")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Analyze\nSkeleton", aliasName = "Analyze Skeleton (2D/3D)")
public class AnalyzeSkeleton2D3DAlgorithm extends JIPipeIteratingAlgorithm {

    public static final JIPipeDataSlotInfo ROI_INPUT_SLOT = new JIPipeDataSlotInfo(ROIListData.class, JIPipeSlotType.Input, "ROI", "ROI to exclude on pruning ends", true);
    public static final JIPipeDataSlotInfo REFERENCE_INPUT_SLOT = new JIPipeDataSlotInfo(ImagePlus3DGreyscale8UData.class, JIPipeSlotType.Input, "Reference", "Original grayscale input image (for lowest pixel intensity pruning mode)", true);

    public static final JIPipeDataSlotInfo SKELETONS_TABLE_OUTPUT_SLOT = new JIPipeDataSlotInfo(ResultsTableData.class, JIPipeSlotType.Output, "Skeletons", "Table of all skeletons");

    public static final JIPipeDataSlotInfo BRANCHES_TABLE_OUTPUT_SLOT = new JIPipeDataSlotInfo(ResultsTableData.class, JIPipeSlotType.Output, "Branches", "Table of all branches");

    public static final JIPipeDataSlotInfo TAGGED_SKELETONS_IMAGE_OUTPUT_SLOT = new JIPipeDataSlotInfo(ImagePlusGreyscale8UData.class, JIPipeSlotType.Output, "Tagged skeletons", "End-point voxels are displayed in blue, slab voxels in orange and junction voxels in purple");

    public static final JIPipeDataSlotInfo LABELS_IMAGE_OUTPUT_SLOT = new JIPipeDataSlotInfo(ImagePlusGreyscale32FData.class, JIPipeSlotType.Output, "Labels", "Label image of the skeletons");

    public static final JIPipeDataSlotInfo LSP_IMAGE_OUTPUT_SLOT = new JIPipeDataSlotInfo(ImagePlusGreyscale8UData.class, JIPipeSlotType.Output, "Largest shortest paths", "The largest shortest path (in magenta)");
    private final OutputParameters outputParameters;
    private CycleRemovalMethod pruneCyclesMethod = CycleRemovalMethod.None;
    private EndRemovalMethod pruneEndsMethod = EndRemovalMethod.None;

    public AnalyzeSkeleton2D3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.outputParameters = new OutputParameters(this);
        this.outputParameters.setOutputSkeletonTable(true);
        this.outputParameters.setOutputTaggedSkeletons(true);
        registerSubParameter(outputParameters);
    }

    public AnalyzeSkeleton2D3DAlgorithm(AnalyzeSkeleton2D3DAlgorithm other) {
        super(other);
        this.outputParameters = new OutputParameters(other.outputParameters, this);
        setPruneCyclesMethod(other.pruneCyclesMethod);
        setPruneEndsMethod(other.pruneEndsMethod);
        registerSubParameter(outputParameters);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus skeleton = iterationStep.getInputData("Skeleton", ImagePlus3DGreyscaleMaskData.class, progressInfo).getImage();
        Roi excludeRoi = null;
        ImagePlus referenceImage = null;

        // Get the excluded ROI
        if (pruneEndsMethod == EndRemovalMethod.ExcludeROI) {
            ROIListData roi = iterationStep.getInputData("ROI", ROIListData.class, progressInfo);
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
        if (pruneCyclesMethod == CycleRemovalMethod.LowestIntensityBranch || pruneCyclesMethod == CycleRemovalMethod.LowestIntensityVoxel) {
            ImagePlus3DGreyscale8UData imageData = iterationStep.getInputData("Reference", ImagePlus3DGreyscale8UData.class, progressInfo);
            if (imageData != null) {
                referenceImage = imageData.getImage();
            }
        }

        AnalyzeSkeleton_ analyzeSkeleton = new AnalyzeSkeleton_();
        analyzeSkeleton.setup("", skeleton);
        SkeletonResult result = analyzeSkeleton.run(pruneCyclesMethod.getNativeValue(), pruneEndsMethod != EndRemovalMethod.None, true, referenceImage, true, true, excludeRoi);

        // Extract Skeletons
        if (outputParameters.isOutputSkeletonTable()) {
            ResultsTable skeletonsTable = calculateSkeletonTable(result);
            iterationStep.addOutputData("Skeletons", new ResultsTableData(skeletonsTable), progressInfo);
        }

        // Extract Branches
        if (outputParameters.isOutputBranchTable()) {
            ResultsTable branchesTable = calculateBranchesTable(skeleton, analyzeSkeleton, result);
            iterationStep.addOutputData("Branches", new ResultsTableData(branchesTable), progressInfo);
        }

        // Extract Tagged skeletons
        if (outputParameters.isOutputTaggedSkeletons()) {
            ImagePlus taggedSkeletons = generateTaggedSkeletons(skeleton, analyzeSkeleton);
            ImageJUtils.copyHyperstackDimensions(skeleton, taggedSkeletons);
            iterationStep.addOutputData("Tagged skeletons", new ImagePlusGreyscale8UData(taggedSkeletons), progressInfo);
        }

        // Extract Labels
        if (outputParameters.isOutputSkeletonLabels()) {
            ImagePlus labeledSkeletons = new ImagePlus("Labeled skeletons", analyzeSkeleton.getLabeledSkeletons());
            IJ.run(labeledSkeletons, "Fire", null);
            ImageJUtils.copyHyperstackDimensions(skeleton, labeledSkeletons);
            iterationStep.addOutputData("Labels", new ImagePlusGreyscale32FData(labeledSkeletons), progressInfo);
        }

        // Extract Largest shortest paths
        if (outputParameters.isOutputLargestShortestPaths()) {
            ImagePlus largestShortestPaths = generateShortPathImage(skeleton, analyzeSkeleton);
            IJ.run(largestShortestPaths, "Fire", null);
            ImageJUtils.copyHyperstackDimensions(skeleton, largestShortestPaths);
            iterationStep.addOutputData("Largest shortest paths", new ImagePlusGreyscale8UData(largestShortestPaths), progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Outputs", description = "Please select the entries to add/remove outputs")
    @JIPipeParameter("output-parameters")
    public OutputParameters getOutputParameters() {
        return outputParameters;
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
                "Branch length", "V1 x", "V1 y",
                "V1 z", "V2 x", "V2 y", "V2 z", "Euclidean distance", "running average length", "average intensity (inner 3rd)", "average intensity"};


        // Edge comparator (by branch length)
        Comparator<Edge> comp = new Comparator<Edge>() {
            public int compare(Edge o1, Edge o2) {
                final double diff = o1.getLength() - o2.getLength();
                if (diff < 0)
                    return 1;
                else if (diff == 0)
                    return 0;
                else
                    return -1;
            }

            public boolean equals(Object o) {
                return false;
            }
        };
        // Display branch information for each tree
        for (int i = 0; i < numOfTrees; i++) {
            final ArrayList<Edge> listEdges = graph[i].getEdges();
            // Sort branches by length
            listEdges.sort(comp);
            for (final Edge e : listEdges) {
                extra_rt.incrementCounter();
                extra_rt.addValue(extra_head[1], i + 1);
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

        final String[] head = {"Skeleton", "# Branches", "# Junctions", "# End-point voxels",
                "# Junction voxels", "# Slab voxels", "Average Branch Length",
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

        ResultsTableData rt = new ResultsTableData();
        for (String s : head) {
            rt.addStringColumn(s);
        }

        for (int i = 0; i < result.getNumOfTrees(); i++) {
            rt.addRow();
            rt.setValueAt(i + 1, i, "Skeleton");
            rt.setValueAt(numberOfBranches[i], i, "# Branches");
            rt.setValueAt(numberOfJunctions[i], i, "# Junctions");
            rt.setValueAt(numberOfEndPoints[i], i, "# End-point voxels");
            rt.setValueAt(numberOfJunctionVoxels[i], i, "# Junction voxels");
            rt.setValueAt(numberOfSlabs[i], i, "# Slab voxels");
            rt.setValueAt(averageBranchLength[i], i, "Average Branch Length");
            rt.setValueAt(numberOfTriplePoints[i], i, "# Triple points");
            rt.setValueAt(numberOfQuadruplePoints[i], i, "# Quadruple points");
            rt.setValueAt(maximumBranchLength[i], i, "Maximum Branch Length");
            if (null != shortestPathList) {
                rt.setValueAt(shortestPathList.get(i), i, "Longest Shortest Path");
                rt.setValueAt(spStartPosition[i][0], i, "spx");
                rt.setValueAt(spStartPosition[i][1], i, "spy");
                rt.setValueAt(spStartPosition[i][2], i, "spz");
            }
        }
        return rt.getTable();
    }

    @SetJIPipeDocumentation(name = "Prune cycles method", description = "Allows the selection of a method to prune possible loops in the skeleton")
    @JIPipeParameter("prune-cycles-method")
    public CycleRemovalMethod getPruneCyclesMethod() {
        return pruneCyclesMethod;
    }

    @JIPipeParameter("prune-cycles-method")
    public void setPruneCyclesMethod(CycleRemovalMethod pruneCyclesMethod) {
        this.pruneCyclesMethod = pruneCyclesMethod;
        toggleSlot(REFERENCE_INPUT_SLOT, pruneCyclesMethod == CycleRemovalMethod.LowestIntensityBranch || pruneCyclesMethod == CycleRemovalMethod.LowestIntensityVoxel);
    }

    @SetJIPipeDocumentation(name = "Prune ends methods", description = "Allows the selection of a method to prune any branch that ends in an end-point")
    @JIPipeParameter("prune-ends-method")
    public EndRemovalMethod getPruneEndsMethod() {
        return pruneEndsMethod;
    }

    @JIPipeParameter("prune-ends-method")
    public void setPruneEndsMethod(EndRemovalMethod pruneEndsMethod) {
        this.pruneEndsMethod = pruneEndsMethod;
        toggleSlot(ROI_INPUT_SLOT, pruneEndsMethod == EndRemovalMethod.ExcludeROI);
    }

    @AddJIPipeDocumentationDescription(description = "<ul>" +
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
            if (this == ExcludeROI)
                return "Exclude ROI";
            return name();
        }
    }

    @AddJIPipeDocumentationDescription(description = "<ul>" +
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

    public static class OutputParameters extends AbstractJIPipeParameterCollection {
        private AnalyzeSkeleton2D3DAlgorithm parent;
        private boolean outputSkeletonTable = false;
        private boolean outputBranchTable = false;
        private boolean outputTaggedSkeletons = false;
        private boolean outputSkeletonLabels = false;
        private boolean outputLargestShortestPaths = false;

        public OutputParameters(AnalyzeSkeleton2D3DAlgorithm parent) {
            this.parent = parent;
        }

        public OutputParameters(OutputParameters other, AnalyzeSkeleton2D3DAlgorithm newParent) {
            this.parent = newParent;
            this.setOutputSkeletonTable(other.outputSkeletonTable);
            this.setOutputBranchTable(other.outputBranchTable);
            this.setOutputTaggedSkeletons(other.outputTaggedSkeletons);
            this.setOutputSkeletonLabels(other.outputSkeletonLabels);
            this.setOutputLargestShortestPaths(other.outputLargestShortestPaths);
        }

        public AnalyzeSkeleton2D3DAlgorithm getParent() {
            return parent;
        }

        public void setParent(AnalyzeSkeleton2D3DAlgorithm parent) {
            this.parent = parent;
        }

        @SetJIPipeDocumentation(name = "Output skeleton table", description = "If enabled, output a table with all skeletons")
        @JIPipeParameter("output-skeleton-table")
        public boolean isOutputSkeletonTable() {
            return outputSkeletonTable;
        }

        @JIPipeParameter("output-skeleton-table")
        public void setOutputSkeletonTable(boolean outputSkeletonTable) {
            this.outputSkeletonTable = outputSkeletonTable;
            parent.toggleSlot(SKELETONS_TABLE_OUTPUT_SLOT, outputSkeletonTable);
        }

        @SetJIPipeDocumentation(name = "Output branch table", description = "If enabled, output a table with all detected branches")
        @JIPipeParameter("output-branch-table")
        public boolean isOutputBranchTable() {
            return outputBranchTable;
        }

        @JIPipeParameter("output-branch-table")
        public void setOutputBranchTable(boolean outputBranchTable) {
            this.outputBranchTable = outputBranchTable;
            parent.toggleSlot(BRANCHES_TABLE_OUTPUT_SLOT, outputBranchTable);
        }

        @SetJIPipeDocumentation(name = "Output tagged skeletons", description = "If enabled, output an image where end-point voxels are displayed in blue, slab voxels in orange and junction voxels in purple")
        @JIPipeParameter("output-tagged-skeletons")
        public boolean isOutputTaggedSkeletons() {
            return outputTaggedSkeletons;
        }

        @JIPipeParameter("output-tagged-skeletons")
        public void setOutputTaggedSkeletons(boolean outputTaggedSkeletons) {
            this.outputTaggedSkeletons = outputTaggedSkeletons;
            parent.toggleSlot(TAGGED_SKELETONS_IMAGE_OUTPUT_SLOT, outputTaggedSkeletons);
        }

        @SetJIPipeDocumentation(name = "Output skeleton labels", description = "If enabled, output a label image of all skeletons")
        @JIPipeParameter("output-skeleton-labels")
        public boolean isOutputSkeletonLabels() {
            return outputSkeletonLabels;
        }

        @JIPipeParameter("output-skeleton-labels")
        public void setOutputSkeletonLabels(boolean outputSkeletonLabels) {
            this.outputSkeletonLabels = outputSkeletonLabels;
            parent.toggleSlot(LABELS_IMAGE_OUTPUT_SLOT, outputSkeletonLabels);
        }

        @SetJIPipeDocumentation(name = "Output largest shortest paths", description = "If enabled, output an image that highlights the largest shortest paths")
        @JIPipeParameter("output-largest-shortest-paths")
        public boolean isOutputLargestShortestPaths() {
            return outputLargestShortestPaths;
        }

        @JIPipeParameter("output-largest-shortest-paths")
        public void setOutputLargestShortestPaths(boolean outputLargestShortestPaths) {
            this.outputLargestShortestPaths = outputLargestShortestPaths;
            parent.toggleSlot(LSP_IMAGE_OUTPUT_SLOT, outputLargestShortestPaths);
        }
    }
}
