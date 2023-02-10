package org.hkijena.jipipe.extensions.ijfilaments.nodes.process;

import com.google.common.collect.ImmutableList;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.FilamentsData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.scijava.vecmath.Vector3d;

import java.util.*;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Fix overlapping filaments (non-branching)", description = "Algorithm that attempts to fix filaments that are merged together by junctions. " +
        "Please note that this operation assumes that all filaments are non-branching.")
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Process")
@JIPipeInputSlot(value = FilamentsData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = FilamentsData.class, slotName = "Output", autoCreate = true)
public class FixOverlapsNonBranchingAlgorithm extends JIPipeSimpleIteratingAlgorithm {


    public FixOverlapsNonBranchingAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FixOverlapsNonBranchingAlgorithm(FixOverlapsNonBranchingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FilamentsData filamentsData = new FilamentsData(dataBatch.getInputData(getFirstInputSlot(), FilamentsData.class, progressInfo));
        Map<FilamentVertex, Integer> components = filamentsData.findComponentIds();

        // Remove all junction nodes (degree > 2)
        filamentsData.removeVertexIf(vertex -> filamentsData.degreeOf(vertex) > 2);

        // Detect candidates to be connected (degree == 1)
        Set<FilamentVertex> toConnect = filamentsData.vertexSet().stream().filter(vertex -> filamentsData.degreeOf(vertex) == 1).collect(Collectors.toSet());

        // For each candidate: search for best matching vertex within radius that is within the same original component
        // The other candidate should also not be connected to the current candidate
        // Score: abs(scalar product)
        Set<FilamentVertex> candidates = new HashSet<>();
        Map<FilamentVertex, Double> candidateScores = new HashMap<>();
        int successes = 0;
        while(!toConnect.isEmpty()) {
            candidates.clear();
            FilamentVertex current = toConnect.iterator().next();
            toConnect.remove(current);
            Vector3d currentCentroid = current.getCentroid().toVector3d();

            for (FilamentVertex other : toConnect) {
                // TODO: visibility (Z/C/T)
                if(other != current && Objects.equals(components.get(other), components.get(current)) && other.getCentroid().distanceTo(current.getCentroid()) < 50) { // TODO: configurable formula
                    // TODO: allow to restrict angle
                    candidates.add(other);
                }
            }

            // Calculate score
            for (FilamentVertex candidate : candidates) {
                Vector3d candidateCentroid = candidate.getCentroid().toVector3d(); // TODO: custom formula
                double score = Math.abs( currentCentroid.dot(candidateCentroid) );
                candidateScores.put(candidate, score);
            }

            // Choose best score
            if(!candidateScores.isEmpty()) {
                FilamentVertex bestCandidate = candidateScores.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();

                // Connect
                filamentsData.addEdge(current, bestCandidate);
                toConnect.remove(bestCandidate);
                ++successes;
            }
            else {
                progressInfo.log("Unable to find candidate for " + current.getUuid());
            }
        }

        progressInfo.log("Successfully created " + successes + " edges.");

        dataBatch.addOutputData(getFirstOutputSlot(), filamentsData, progressInfo);
    }

}
