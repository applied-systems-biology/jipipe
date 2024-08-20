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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.modify;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.ijfilaments.util.NonSpatialPoint3d;
import org.hkijena.jipipe.plugins.ijfilaments.util.Point3d;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.plugins.parameters.library.quantities.OptionalQuantity;
import org.hkijena.jipipe.plugins.parameters.library.quantities.Quantity;

import java.util.HashSet;
import java.util.Set;

@SetJIPipeDocumentation(name = "Subdivide filament edges", description = "Inserts a vertex at the center of edges. The connectivity and shape of the filament is preserved.")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = Filaments3DGraphData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DGraphData.class, name = "Output", create = true)
public class SubdivideFilamentEdges extends JIPipeSimpleIteratingAlgorithm {

    private OptionalQuantity maximumLength = new OptionalQuantity(new Quantity(2, "px"), true);
    private OptionalIntegerParameter maximumNumIterations = new OptionalIntegerParameter(true, 2);

    public SubdivideFilamentEdges(JIPipeNodeInfo info) {
        super(info);
    }

    public SubdivideFilamentEdges(SubdivideFilamentEdges other) {
        super(other);
        this.maximumLength = new OptionalQuantity(other.maximumLength);
        this.maximumNumIterations = new OptionalIntegerParameter(other.maximumNumIterations);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DGraphData graph = new Filaments3DGraphData(iterationStep.getInputData(getFirstInputSlot(), Filaments3DGraphData.class, progressInfo));
        int numIterations = 0;
        final int maxIterations = maximumNumIterations.orElse(Integer.MAX_VALUE);
        while (true) {
            ++numIterations;

            // Find candidate edges
            Set<FilamentEdge> edges = new HashSet<>();
            for (FilamentEdge edge : graph.edgeSet()) {
                if (maximumLength.isEnabled()) {
                    double edgeLength = graph.getEdgeLength(edge, true, maximumLength.getContent().getUnit());
                    if (edgeLength > maximumLength.getContent().getValue()) {
                        edges.add(edge);
                    }
                } else {
                    edges.add(edge);
                }
            }
            progressInfo.log("Iteration " + numIterations + ": Detected " + edges.size() + " edges to split");

            // Nothing found -> cancel
            if (edges.isEmpty()) {
                break;
            }

            // Apply splitting
            for (FilamentEdge edge : edges) {
                FilamentVertex edgeSource = graph.getEdgeSource(edge);
                FilamentVertex edgeTarget = graph.getEdgeTarget(edge);
                FilamentVertex vertex = new FilamentVertex();
                graph.addVertex(vertex);
                vertex.setNonSpatialLocation(new NonSpatialPoint3d(edgeSource.getNonSpatialLocation()));
                vertex.setSpatialLocation(new Point3d((edgeSource.getSpatialLocation().getX() + edgeTarget.getSpatialLocation().getX()) / 2.0,
                        (edgeSource.getSpatialLocation().getY() + edgeTarget.getSpatialLocation().getY()) / 2.0,
                        (edgeSource.getSpatialLocation().getZ() + edgeTarget.getSpatialLocation().getZ()) / 2.0));
                vertex.setValue((edgeSource.getValue() + edgeTarget.getValue()) / 2.0);
                vertex.setRadius((edgeSource.getRadius() + edgeTarget.getRadius()) / 2.0);

                graph.removeEdge(edge);
                graph.addEdge(edgeSource, vertex);
                graph.addEdge(vertex, edgeTarget);
            }

            // Iteration constraint
            if (numIterations >= maxIterations) {
                break;
            }
        }
        iterationStep.addOutputData(getFirstOutputSlot(), graph, progressInfo);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        if (!maximumLength.isEnabled() && !maximumNumIterations.isEnabled()) {
            report.report(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, reportContext, "Subdivide has no critera",
                    "The filament subdivision operator has no stopping criteria and will run infinitely",
                    "Use either the maximum length and/or the maximum number of iterations"));
        }
    }

    @SetJIPipeDocumentation(name = "Split if longer than ...", description = "If enabled, only edges longer than the specified length are split")
    @JIPipeParameter("maximum-length")
    public OptionalQuantity getMaximumLength() {
        return maximumLength;
    }

    @JIPipeParameter("maximum-length")
    public void setMaximumLength(OptionalQuantity maximumLength) {
        this.maximumLength = maximumLength;
    }

    @SetJIPipeDocumentation(name = "Maximum iterations", description = "The maximum number of split iterations")
    @JIPipeParameter("max-num-iterations")
    public OptionalIntegerParameter getMaximumNumIterations() {
        return maximumNumIterations;
    }

    @JIPipeParameter("max-num-iterations")
    public void setMaximumNumIterations(OptionalIntegerParameter maximumNumIterations) {
        this.maximumNumIterations = maximumNumIterations;
    }
}
