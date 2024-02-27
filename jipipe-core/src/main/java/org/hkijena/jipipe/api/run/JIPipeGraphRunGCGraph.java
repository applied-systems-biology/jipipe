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

package org.hkijena.jipipe.api.run;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A graph that allows to track which data is currently being used
 * Contains {@link JIPipeInputDataSlot}, {@link JIPipeOutputDataSlot}, and {@link JIPipeGraphNode}
 * Unlike the {@link JIPipeGraphRunDataFlowGraph}, this graph works on edges
 */
public class JIPipeGraphRunGCGraph extends DefaultDirectedGraph<Object, DefaultEdge> {

    private final GCEventEmitter gcEventEmitter = new GCEventEmitter();
    public JIPipeGraphRunGCGraph() {
        super(DefaultEdge.class);
    }

    public JIPipeGraphRunGCGraph(JIPipeGraph graph, Set<JIPipeGraphNode> nodeFilter) {
        super(DefaultEdge.class);
        addGraph(graph, nodeFilter);
    }

    public void addGraph(JIPipeGraph graph, Set<JIPipeGraphNode> nodeFilter) {
        for (JIPipeGraphNode graphNode : nodeFilter) {
            addVertex(graphNode);
            for (JIPipeInputDataSlot inputSlot : graphNode.getInputSlots()) {
                addVertex(inputSlot);
            }
            for (JIPipeOutputDataSlot outputSlot : graphNode.getOutputSlots()) {
                addVertex(outputSlot);
            }
        }
        for (JIPipeGraphNode graphNode : nodeFilter) {
            for (JIPipeInputDataSlot inputSlot : graphNode.getInputSlots()) {
                addEdge(inputSlot, graphNode);

                // Inputs
                for (JIPipeGraphEdge graphEdge : graph.getGraph().incomingEdgesOf(inputSlot)) {
                    JIPipeDataSlot edgeSource = graph.getGraph().getEdgeSource(graphEdge);
                    if(containsVertex(edgeSource)) {
                        addEdge(edgeSource, inputSlot);
                    }
                }
            }
            for (JIPipeOutputDataSlot outputSlot : graphNode.getOutputSlots()) {
                addEdge(graphNode, outputSlot);
            }
        }
    }

    @Override
    public String toString() {
        return "[GC] " + vertexSet().size() + " vertices, " + edgeSet().size() + " edges";
    }

    /**
     * Removes a connection from the targeted output slot to the input, which will free up the output slot at some point
     * @param outputSlot the output slot
     * @param inputSlot the input slot
     * @param progressInfo the progress info
     */
    public void removeOutputToInputEdge(JIPipeDataSlot outputSlot, JIPipeInputDataSlot inputSlot, JIPipeProgressInfo progressInfo) {
        if(containsEdge(outputSlot, inputSlot)) {
            removeEdge(outputSlot, inputSlot);
            progressInfo.log("Removed O-I edge " + outputSlot.getDisplayName() + " >>> " + inputSlot.getDisplayName());
            gc(progressInfo);
        }
        else {
            progressInfo.log("[!] Unable to remove non-existing O-I edge " + outputSlot.getDisplayName() + " >>> " + inputSlot.getDisplayName());
        }
    }

    public void removeNodeToOutputEdge(JIPipeOutputDataSlot outputDataSlot, JIPipeProgressInfo progressInfo) {
        if(containsEdge(outputDataSlot.getNode(), outputDataSlot)) {
            removeEdge(outputDataSlot.getNode(), outputDataSlot);
            progressInfo.log("Removed N-O edge " + outputDataSlot.getNode().getDisplayName() + " >>> " + outputDataSlot.getDisplayName());
            gc(progressInfo);
        }
        else {
            progressInfo.log("[!] Unable to remove non-existing N-O edge " + outputDataSlot.getNode().getDisplayName() + " >>> " + outputDataSlot.getDisplayName());
        }
    }

    public void removeInputToNodeEdge(JIPipeAlgorithm algorithm, JIPipeProgressInfo progressInfo) {
        if(containsVertex(algorithm)) {
            for (JIPipeInputDataSlot inputSlot : algorithm.getInputSlots()) {
                if (containsEdge(inputSlot, algorithm)) {
                    removeEdge(inputSlot, algorithm);
                    progressInfo.log("Removed I-N edge " + inputSlot.getDisplayName() + " >>> " + algorithm.getDisplayName());
                } else {
                    progressInfo.log("[!] Unable to remove non-existing I-N edge " + inputSlot.getDisplayName() + " >>> " + algorithm.getDisplayName());
                }
            }
            gc(progressInfo);
        }
        else {
            progressInfo.log("[!] Unable to remove input edges from algorithm " + algorithm.getDisplayName());
        }

    }

    public void gc(JIPipeProgressInfo progressInfo) {
        List<Object> toCleanup = new ArrayList<>();
        for (Object vertex : vertexSet()) {
            if(degreeOf(vertex) == 0) {
                toCleanup.add(vertex);
            }
        }
        for (Object vertex : toCleanup) {
            if(vertex instanceof JIPipeGraphNode) {
                progressInfo.log("-N Unregistering " + ((JIPipeGraphNode) vertex).getDisplayName());
                removeVertex(vertex);
            }
            else if(vertex instanceof JIPipeInputDataSlot) {
                progressInfo.log("-I Clearing " + ((JIPipeInputDataSlot) vertex).getDisplayName());
                ((JIPipeInputDataSlot) vertex).clear();
                removeVertex(vertex);
            }
            else if(vertex instanceof JIPipeOutputDataSlot) {
                progressInfo.log("-O Clearing " + ((JIPipeOutputDataSlot) vertex).getDisplayName());
                removeVertex(vertex);
                gcEventEmitter.emit(new GCEvent(this, (JIPipeOutputDataSlot) vertex));
                if(!((JIPipeOutputDataSlot) vertex).isSkipGC()) {
                    ((JIPipeOutputDataSlot) vertex).clear();
                }
                else {
                    progressInfo.log("--> Clearing was prevented [skip GC]");
                }
            }
            else {
                progressInfo.log("-? [!] Unregistering UNKNOWN " + vertex);
                removeVertex(vertex);
            }
        }
    }

    public GCEventEmitter getGcEventEmitter() {
        return gcEventEmitter;
    }

    public interface GCEventListener {
        void onGCEvent(GCEvent event);
    }

    public static class GCEvent extends AbstractJIPipeEvent {
        private final JIPipeOutputDataSlot slot;

        public GCEvent(JIPipeGraphRunGCGraph source, JIPipeOutputDataSlot slot) {
            super(source);
            this.slot = slot;
        }

        public JIPipeOutputDataSlot getSlot() {
            return slot;
        }
    }

    public static class GCEventEmitter extends JIPipeEventEmitter<GCEvent, GCEventListener> {

        @Override
        protected void call(GCEventListener GCEventListener, GCEvent event) {
            GCEventListener.onGCEvent(event);
        }
    }
}
