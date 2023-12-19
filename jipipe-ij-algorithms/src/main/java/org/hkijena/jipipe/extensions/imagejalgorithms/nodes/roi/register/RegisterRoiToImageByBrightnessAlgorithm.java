package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.register;

import com.google.common.collect.MinMaxPriorityQueue;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Register ROI 2D (by intensity)", description = "Tests multiple locations of the specified ROI within the image and finds the scale, rotation, and translation of the ROI where its components align to the maximum average intensity.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Register")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeInputSlot(value = ImagePlus2DData.class, slotName = "Image", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Registered ROI", autoCreate = true)
public class RegisterRoiToImageByBrightnessAlgorithm extends JIPipeIteratingAlgorithm {

    private JIPipeExpressionParameter rotationRange = new JIPipeExpressionParameter("MAKE_SEQUENCE(-180, 180, 1)");

    private JIPipeExpressionParameter scaleRange = new JIPipeExpressionParameter("MAKE_SEQUENCE(0.5, 1.5, 0.1)");

    private int xyStep = 5;


    public RegisterRoiToImageByBrightnessAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RegisterRoiToImageByBrightnessAlgorithm(RegisterRoiToImageByBrightnessAlgorithm other) {
        super(other);
        this.rotationRange = new JIPipeExpressionParameter(other.rotationRange);
        this.scaleRange = new JIPipeExpressionParameter(other.scaleRange);
        this.xyStep = other.xyStep;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ImageProcessor ip = iterationStep.getInputData("Image", ImagePlus2DData.class, progressInfo).getImage().getProcessor();
        ROIListData rois = new ROIListData(iterationStep.getInputData("ROI", ROIListData.class, progressInfo));
        rois.logicalOr();

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());

        List<Double> rotations = rotationRange.evaluateToDoubleList(variables);
        List<Double> scales = scaleRange.evaluateToDoubleList(variables);

        int maxTasks = rotations.size() * scales.size();
        int finishedTasks = 0;
        int oldPercentage = 0;
        MinMaxPriorityQueue<Candidate> candidates = MinMaxPriorityQueue.maximumSize(5).create();

        for (double scale : scales) {
            if (progressInfo.isCancelled())
                return;

            // Generate the scaled ROI
            ROIListData scaledRoi = rois.scale(scale, scale, false);
            Rectangle scaledBounds = scaledRoi.getBounds();

            // Find the bounds to test
            int xStart = scaledBounds.x;
            int yStart = scaledBounds.y;
            int xEnd = ip.getWidth() - scaledBounds.width;
            int yEnd = ip.getHeight() - scaledBounds.height;

            for (double rotation : rotations) {
                if (progressInfo.isCancelled())
                    return;

                // Generate new roi
                ROIListData scaledRotatedRoi = scaledRoi.rotate(rotation, new Point2D.Double(scaledBounds.x + scaledBounds.width / 2.0,
                        scaledBounds.y + scaledBounds.height / 2.0));

                // Measure
                Roi roi = scaledRotatedRoi.get(0);

                int xBase = xStart;

                while (xBase < xEnd) {
                    int yBase = yStart;
                    while (yBase < yEnd) {

                        roi.setLocation(xBase, yBase);
                        ip.setRoi(roi);
                        ImageStatistics statistics = ip.getStats();
                        candidates.add(new Candidate(roi, scale, rotation, xBase, yBase, statistics.mean));

                        yBase += xyStep;
                    }
                    xBase += xyStep;
                }

                // Track progress
                ++finishedTasks;
                int percentage = (int) (100.0 * finishedTasks / maxTasks);
                if (percentage != oldPercentage) {
                    progressInfo.log(percentage + "% (" + finishedTasks + " / " + maxTasks + " ROI)");
                    oldPercentage = percentage;
                }
            }
        }

        if (!candidates.isEmpty()) {
            List<JIPipeTextAnnotation> annotations = new ArrayList<>();
            Candidate candidate = candidates.peekFirst();
            annotations.add(new JIPipeTextAnnotation("Scale", candidate.scale + ""));
            annotations.add(new JIPipeTextAnnotation("Angle", candidate.angle + ""));
            annotations.add(new JIPipeTextAnnotation("X", candidate.translateX + ""));
            annotations.add(new JIPipeTextAnnotation("Y", candidate.translateY + ""));
            candidate.roi.setLocation(candidate.translateX, candidate.translateY);
            ROIListData output = new ROIListData();
            output.add(candidate.roi);
            iterationStep.addOutputData(getFirstOutputSlot(), output, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Step (X/Y)", description = "The step size in the X and Y direction")
    @JIPipeParameter("xy-step")
    public int getXyStep() {
        return xyStep;
    }

    @JIPipeParameter("xy-step")
    public void setXyStep(int xyStep) {
        this.xyStep = xyStep;
    }

    @JIPipeDocumentation(name = "Rotation range", description = "Expression that generates the rotations to be tested")
    @JIPipeParameter("rotation-range")
    public JIPipeExpressionParameter getRotationRange() {
        return rotationRange;
    }

    @JIPipeParameter("rotation-range")
    public void setRotationRange(JIPipeExpressionParameter rotationRange) {
        this.rotationRange = rotationRange;
    }

    @JIPipeDocumentation(name = "Scale range", description = "Expression that generates the scales to be tested")
    @JIPipeParameter("scale-range")
    public JIPipeExpressionParameter getScaleRange() {
        return scaleRange;
    }

    @JIPipeParameter("scale-range")
    public void setScaleRange(JIPipeExpressionParameter scaleRange) {
        this.scaleRange = scaleRange;
    }

    private static class Candidate implements Comparable<Candidate> {
        private final Roi roi;
        private final double scale;
        private final double angle;

        private final int translateX;

        private final int translateY;

        private final double score;

        private Candidate(Roi roi, double scale, double angle, int translateX, int translateY, double score) {
            this.roi = roi;
            this.scale = scale;
            this.angle = angle;
            this.translateX = translateX;
            this.translateY = translateY;
            this.score = score;
        }

        @Override
        public int compareTo(@NotNull RegisterRoiToImageByBrightnessAlgorithm.Candidate o) {
            return -Double.compare(score, o.score);
        }
    }
}
