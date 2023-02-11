package org.hkijena.jipipe.extensions.ijfilaments.nodes.process;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.FilamentsData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.scijava.vecmath.Vector3d;

import java.awt.*;
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
        Set<FilamentVertex> unconnected = filamentsData.vertexSet().stream().filter(vertex -> filamentsData.degreeOf(vertex) == 1).collect(Collectors.toSet());


        // For each candidate: search for best matching vertex within radius that is within the same original component
        // The other candidate should also not be connected to the current candidate
        // Score: abs(scalar product of normalized vectors)
        Set<EdgeCandidate> candidates = new TreeSet<>();
        ConnectivityInspector<FilamentVertex, FilamentEdge> inspector = new ConnectivityInspector<>(filamentsData);
        Random random = new Random();
        for (FilamentVertex current : unconnected) {

            Vector3d currentV1 = current.getCentroid().toVector3d();
            Vector3d currentV2 = Graphs.neighborSetOf(filamentsData, current).iterator().next().getCentroid().toVector3d();
            Vector3d currentDirection = new Vector3d(currentV2.x - currentV1.x, currentV2.y - currentV1.y, currentV2.z - currentV1.z);
            currentDirection.normalize();

            for (FilamentVertex other : unconnected) {
                if(other != current &&
                        Objects.equals(components.get(current), components.get(other)) &&
                        other.getCentroid().distanceTo(current.getCentroid()) < 50  &&
                        !inspector.pathExists(current, other)) {
                    // TODO: visibility (Z/C/T)
                    // TODO: allow to restrict angle

                    // TODO: Prefer shorter connections
                    // TODO: maybe do a raytrace to check for collisions?

                    // Calculate score
                    Vector3d otherV1 = other.getCentroid().toVector3d();
                    Vector3d otherV2 = Graphs.neighborSetOf(filamentsData, other).iterator().next().getCentroid().toVector3d();
                    Vector3d otherDirection = new Vector3d(otherV2.x - otherV1.x, otherV2.y - otherV1.y, otherV2.z - otherV1.z);
                    otherDirection.normalize();

                    // Idea is: opposite directions -> scalar product is -1.
                    double score = -currentDirection.dot(otherDirection);

                    // Add as candidate
                    EdgeCandidate candidate = new EdgeCandidate(current, other, score);
                    candidates.add(candidate);

                }
            }
        }

        // Apply connect
        int successes = 0;
        Set<FilamentVertex> processedCandidateVertices = new HashSet<>();
        for (EdgeCandidate candidate : candidates) {
            if(!processedCandidateVertices.contains(candidate.source) && !processedCandidateVertices.contains(candidate.target)) {
                // Connect
                FilamentEdge edge = filamentsData.addEdge(candidate.source, candidate.target);
                edge.setColor(Color.getHSBColor(random.nextFloat(), 1, 1));

                // Mark as used
                processedCandidateVertices.add(candidate.source);
                processedCandidateVertices.add(candidate.target);
                ++successes;
            }
        }

        progressInfo.log("Successfully created " + successes + " edges.");

        dataBatch.addOutputData(getFirstOutputSlot(), filamentsData, progressInfo);
    }

    public static class EdgeCandidate implements Comparable<EdgeCandidate> {
        private final FilamentVertex source;
        private final FilamentVertex target;
        private final double score;

        public EdgeCandidate(FilamentVertex source, FilamentVertex target, double score) {
            this.source = source;
            this.target = target;
            this.score = score;
        }

        public FilamentVertex getSource() {
            return source;
        }

        public FilamentVertex getTarget() {
            return target;
        }

        public double getScore() {
            return score;
        }

        @Override
        public int compareTo(@NotNull FixOverlapsNonBranchingAlgorithm.EdgeCandidate o) {
            return -Double.compare(score, o.score);
        }
    }

}
