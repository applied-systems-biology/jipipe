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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.outline;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import org.hkijena.jipipe.api.AddJIPipeCitation;
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
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.ConcaveHullMoreiraSantos;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;


@SetJIPipeDocumentation(name = "Outline 2D ROI (Concave Hull Moreira/Santos)", description = "Uses the algorithm by Moreira and Santos to calculate the concave hull of the provided rois")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Outline")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Output", create = true)
@AddJIPipeCitation("Moreira, A., & Santos, M. Y. (2007, March). Concave hull: A k-nearest neighbours approach for the computation of the region occupied by a set of points. In International Conference on Computer Graphics Theory and Applications (Vol. 2, pp. 61-68). SciTePress.")
public class OutlineRoiConcaveHullMoreiraSantosAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int kMeansK = 3;
    private boolean skipInvalidInputs = false;

    public OutlineRoiConcaveHullMoreiraSantosAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public OutlineRoiConcaveHullMoreiraSantosAlgorithm(OutlineRoiConcaveHullMoreiraSantosAlgorithm other) {
        super(other);
        this.kMeansK = other.kMeansK;
        this.skipInvalidInputs = other.skipInvalidInputs;
    }

    public static PolygonRoi convertToRoi(List<Point2D> coordinates) {
        if (coordinates == null || coordinates.size() < 3) {
            throw new IllegalArgumentException("At least 3 coordinates are required to form a polygon.");
        }

        // Extract X and Y coordinates with subpixel precision
        float[] xPoints = new float[coordinates.size()];
        float[] yPoints = new float[coordinates.size()];

        for (int i = 0; i < coordinates.size(); i++) {
            xPoints[i] = (float) coordinates.get(i).getX();
            yPoints[i] = (float) coordinates.get(i).getY();
        }

        // Create and return the PolygonRoi with subpixel precision
        return new PolygonRoi(xPoints, yPoints, coordinates.size(), Roi.POLYGON);
    }

    @SetJIPipeDocumentation(name = "K-means k", description = "The k parameter of the underlying k-means algorithm. If this value is < 3, it is set to 3. " +
            "Determines the number of closes neighbors are selected from the current point.")
    @JIPipeParameter("kmeans-k")
    public int getkMeansK() {
        return kMeansK;
    }

    @JIPipeParameter("kmeans-k")
    public void setkMeansK(int kMeansK) {
        this.kMeansK = kMeansK;
    }

    @SetJIPipeDocumentation(name = "Delete invalid ROIs", description = "If a ROI has less than 3 points, delete it from the output. " +
            "Otherwise, the ROI is not processed and be stored in the output as-is.")
    @JIPipeParameter("skip-invalid-inputs")
    public boolean isSkipInvalidInputs() {
        return skipInvalidInputs;
    }

    @JIPipeParameter("skip-invalid-inputs")
    public void setSkipInvalidInputs(boolean skipInvalidInputs) {
        this.skipInvalidInputs = skipInvalidInputs;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI2DListData input = iterationStep.getInputData(getFirstInputSlot(), ROI2DListData.class, progressInfo);
        ROI2DListData output = new ROI2DListData();

        for (int i = 0; i < input.size(); i++) {
            Roi roi = input.get(i);
            FloatPolygon polygon = roi.getFloatPolygon();
            if (polygon.npoints >= 3) {
                List<Point2D> points = new ArrayList<>();
                for (int j = 0; j < polygon.npoints; j++) {
                    points.add(new Point2D.Double(polygon.xpoints[j], polygon.ypoints[j]));
                }
                List<Point2D> hull = ConcaveHullMoreiraSantos.calculateConcaveHull(points, kMeansK);
                Roi hullRoi = convertToRoi(hull);
                hullRoi.copyAttributes(roi);
                output.add(hullRoi);

            } else if (skipInvalidInputs) {
                progressInfo.log("Skipping ROI at index " + i + " (NPoints < 3)");
            } else {
                progressInfo.log("Refusing to process ROI at index " + i + " (NPoints < 3)");
                output.add(roi);
            }

        }

        iterationStep.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

}
