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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.convert;

import ij.gui.Roi;
import ij.process.FloatPolygon;
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
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.ijfilaments.util.NonSpatialPoint3d;
import org.hkijena.jipipe.plugins.ijfilaments.util.Point3d;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;

@SetJIPipeDocumentation(name = "Convert 2D ROI to filaments", description = "Converts 2D ROI into equivalent filaments")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DGraphData.class, name = "Output", create = true)
public class ConvertROIToFilamentsAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public ConvertROIToFilamentsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertROIToFilamentsAlgorithm(ConvertROIToFilamentsAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI2DListData rois = iterationStep.getInputData(getFirstInputSlot(), ROI2DListData.class, progressInfo);
        Filaments3DGraphData filaments = new Filaments3DGraphData();

        for (Roi roi : rois) {
            FloatPolygon polygon = roi.getFloatPolygon();

            if (polygon.npoints <= 1) {
                continue;
            }

            FilamentVertex firstVertex = null;
            FilamentVertex lastVertex = null;
            for (int i = 0; i < polygon.npoints; i++) {
                FilamentVertex currentVertex = new FilamentVertex();
                double z = Math.max(roi.getZPosition() - 1, 0);
                currentVertex.setSpatialLocation(new Point3d(polygon.xpoints[i], polygon.ypoints[i], z));
                currentVertex.setNonSpatialLocation(new NonSpatialPoint3d(Math.max(roi.getCPosition() - 1, 0), Math.max(roi.getTPosition() - 1, 0)));
                filaments.addVertex(currentVertex);

                if (lastVertex != null) {
                    filaments.addEdge(lastVertex, currentVertex);
                }
                if (firstVertex == null) {
                    firstVertex = currentVertex;
                }

                lastVertex = currentVertex;

                // Connect the last
                if (i == polygon.npoints - 1) {
                    filaments.addEdge(lastVertex, firstVertex);
                }
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), filaments, progressInfo);
    }
}
