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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.layout;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.awt.*;
import java.util.*;

/**
 * Applies auto-layout according to Sugiyama et al.
 * Sugiyama, Kozo; Tagawa, Shôjirô; Toda, Mitsuhiko (1981), "Methods for visual understanding of hierarchical system structures",
 * IEEE Transactions on Systems, Man, and Cybernetics, SMC-11 (2): 109–125
 * <p>
 * Code was adapted from <a href="https://blog.disy.net/sugiyama-method/">...</a>
 */
public class SugiyamaGraphAutoLayoutImplementation implements JIPipeGraphAutoLayoutImplementation {

    @Override
    public void accept(JIPipeDesktopGraphCanvasUI canvasUI) {

        JIPipeGraph graph = canvasUI.getGraph();

        // Create an algorithm UI graph
        Set<JIPipeDesktopGraphNodeUI> freeFloating = new HashSet<>();
        DefaultDirectedGraph<SugiyamaVertex, DefaultEdge> sugiyamaGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
        Map<JIPipeDesktopGraphNodeUI, SugiyamaVertex> vertexMap = new HashMap<>();
        for (JIPipeDesktopGraphNodeUI ui : canvasUI.getNodeUIs().values()) {
            // Ignore all free-floating nodes (no inputs, no outputs within this compartment)
            boolean isFreeFloating = true;
            outer:
            for (JIPipeDataSlot inputSlot : ui.getNode().getInputSlots()) {
                Set<JIPipeDataSlot> sourceSlots = graph.getInputIncomingSourceSlots(inputSlot);
                for (JIPipeDataSlot sourceSlot : sourceSlots) {
                    if (Objects.equals(sourceSlot.getNode().getCompartmentUUIDInParentGraph(), inputSlot.getNode().getCompartmentUUIDInParentGraph())) {
                        isFreeFloating = false;
                        break outer;
                    }
                }
            }
            if (isFreeFloating) {
                outer:
                for (JIPipeDataSlot outputSlot : ui.getNode().getOutputSlots()) {
                    for (JIPipeDataSlot targetSlot : graph.getOutputOutgoingTargetSlots(outputSlot)) {
                        if (Objects.equals(outputSlot.getNode().getCompartmentUUIDInParentGraph(), targetSlot.getNode().getCompartmentUUIDInParentGraph())) {
                            isFreeFloating = false;
                            break outer;
                        }
                    }
                }
            }

            if (!isFreeFloating) {
                SugiyamaVertex sugiyamaVertex = new SugiyamaVertex(ui);
                sugiyamaGraph.addVertex(sugiyamaVertex);
                vertexMap.put(ui, sugiyamaVertex);
            } else {
                freeFloating.add(ui);
            }
        }
        for (JIPipeGraphEdge edge : graph.getGraph().edgeSet()) {
            JIPipeDesktopGraphNodeUI sourceUI = canvasUI.getNodeUIs().getOrDefault(graph.getGraph().getEdgeSource(edge).getNode(), null);
            JIPipeDesktopGraphNodeUI targetUI = canvasUI.getNodeUIs().getOrDefault(graph.getGraph().getEdgeTarget(edge).getNode(), null);
            if (sourceUI == null || targetUI == null)
                continue;
            if (sourceUI.getNode() == targetUI.getNode())
                continue;
            SugiyamaVertex sourceVertex = vertexMap.getOrDefault(sourceUI, null);
            SugiyamaVertex targetVertex = vertexMap.getOrDefault(targetUI, null);
            if (sourceVertex == null || targetVertex == null)
                continue;
            if (!sugiyamaGraph.containsEdge(sourceVertex, targetVertex))
                sugiyamaGraph.addEdge(sourceVertex, targetVertex);
        }


        // Skip: Remove cycles. JIPipeAlgorithmGraph ensures that there are none

        // Assign layerIndices
        int maxLayer = 0;
        for (SugiyamaVertex vertex : ImmutableList.copyOf(new TopologicalOrderIterator<>(sugiyamaGraph))) {
            for (DefaultEdge edge : sugiyamaGraph.incomingEdgesOf(vertex)) {
                SugiyamaVertex source = sugiyamaGraph.getEdgeSource(edge);
                vertex.layer = Math.max(vertex.layer, source.layer) + 1;
                maxLayer = Math.max(maxLayer, vertex.layer);
            }
        }

        // Create virtual vertices
        for (DefaultEdge edge : ImmutableList.copyOf(sugiyamaGraph.edgeSet())) {
            SugiyamaVertex source = sugiyamaGraph.getEdgeSource(edge);
            SugiyamaVertex target = sugiyamaGraph.getEdgeTarget(edge);
            int layerDifference = target.layer - source.layer;
            assert layerDifference >= 1;
            if (layerDifference > 1) {
                sugiyamaGraph.removeEdge(source, target);
                SugiyamaVertex lastLayer = source;
                for (int layer = source.layer + 1; layer < target.layer; ++layer) {
                    SugiyamaVertex virtual = new SugiyamaVertex();
                    virtual.layer = layer;
                    sugiyamaGraph.addVertex(virtual);
                    sugiyamaGraph.addEdge(lastLayer, virtual);
                    lastLayer = virtual;
                }
                sugiyamaGraph.addEdge(lastLayer, target);
            }
        }

        // Assign the row for each layer
        int maxIndex = 0;
        for (int layer = 0; layer <= maxLayer; ++layer) {
            int row = 0;
            for (SugiyamaVertex vertex : sugiyamaGraph.vertexSet()) {
                if (vertex.layer == layer) {
                    vertex.index = row++;
                    maxIndex = Math.max(maxIndex, vertex.index);
                }
            }
        }

        rearrangeSugiyamaVertical(canvasUI, sugiyamaGraph, maxLayer, maxIndex);

        // Add free-floating algorithms back into the graph
        if (!freeFloating.isEmpty()) {
            int minX = canvasUI.getViewMode().getGridWidth() * 4;
            for (JIPipeDesktopGraphNodeUI ui : canvasUI.getNodeUIs().values()) {
                if (!freeFloating.contains(ui)) {
                    minX = Math.max(ui.getRightX(), minX);
                }
            }
            int y = canvasUI.getViewMode().getGridHeight();
            for (JIPipeDesktopGraphNodeUI ui : freeFloating) {
                ui.moveToClosestGridPoint(new Point(minX, y), true, true);
                y += ui.getHeight() + canvasUI.getViewMode().getGridHeight();
            }
        }
    }


    private void rearrangeSugiyamaHorizontal(JIPipeDesktopGraphCanvasUI canvasUI, DefaultDirectedGraph<SugiyamaVertex, DefaultEdge> sugiyamaGraph, int maxLayer, int maxIndex) {

        if (sugiyamaGraph.vertexSet().isEmpty())
            return;

        // Create a table of column -> row -> vertex
        int maxRow = maxIndex;
        int maxColumn = maxLayer;
        Map<Integer, Map<Integer, SugiyamaVertex>> vertexTable = new HashMap<>();
        for (SugiyamaVertex vertex : sugiyamaGraph.vertexSet()) {
            Map<Integer, SugiyamaVertex> column = vertexTable.getOrDefault(vertex.layer, null);
            if (column == null) {
                column = new HashMap<>();
                vertexTable.put(vertex.layer, column);
            }
            column.put(vertex.index, vertex);
        }

        // Calculate widths and heights
        Map<Integer, Integer> columnWidths = new HashMap<>();
        Map<Integer, Integer> rowHeights = new HashMap<>();
        for (SugiyamaVertex vertex : sugiyamaGraph.vertexSet()) {
            if (!vertex.virtual) {
                int column = vertex.layer;
                int row = vertex.index;
                int columnWidth = Math.max(vertex.algorithmUI.getWidth(), columnWidths.getOrDefault(column, 0));
                int rowHeight = Math.max(vertex.algorithmUI.getHeight(), rowHeights.getOrDefault(row, 0));
                columnWidths.put(column, columnWidth);
                rowHeights.put(row, rowHeight);
            }
        }
        for (int column : columnWidths.keySet()) {
            columnWidths.put(column, (int) Math.round(columnWidths.get(column) + 2 * canvasUI.getViewMode().getGridWidth() * canvasUI.getZoom()));
        }
        for (int row : rowHeights.keySet()) {
            rowHeights.put(row, (int) Math.round(rowHeights.get(row) + canvasUI.getViewMode().getGridHeight() * canvasUI.getZoom()));
        }

        // Rearrange algorithms
        int x = canvasUI.getViewMode().getGridWidth();
        for (int column = 0; column <= maxColumn; ++column) {
            Map<Integer, SugiyamaVertex> columnMap = vertexTable.get(column);
            int y = canvasUI.getViewMode().getGridHeight();
            for (int row = 0; row <= maxRow; ++row) {
                SugiyamaVertex vertex = columnMap.getOrDefault(row, null);
                if (vertex != null && !vertex.virtual) {
                    JIPipeDesktopGraphNodeUI ui = vertex.algorithmUI;
                    ui.moveToClosestGridPoint(new Point(x, y), true, true);
                }
                y += rowHeights.getOrDefault(row, 0);
            }
            x += columnWidths.getOrDefault(column, 0);
        }


    }

    private void rearrangeSugiyamaVertical(JIPipeDesktopGraphCanvasUI canvasUI, DefaultDirectedGraph<SugiyamaVertex, DefaultEdge> sugiyamaGraph, int maxLayer, int maxIndex) {

        if (sugiyamaGraph.vertexSet().isEmpty())
            return;

        // Create a table of column -> row -> vertex
        int maxRow = maxLayer;
        int maxColumn = maxIndex;
        Map<Integer, Map<Integer, SugiyamaVertex>> vertexTable = new HashMap<>();
        for (SugiyamaVertex vertex : sugiyamaGraph.vertexSet()) {
            Map<Integer, SugiyamaVertex> column = vertexTable.getOrDefault(vertex.index, null);
            if (column == null) {
                column = new HashMap<>();
                vertexTable.put(vertex.index, column);
            }
            column.put(vertex.layer, vertex);
        }

        // Calculate widths and heights
        Map<Integer, Integer> columnWidths = new HashMap<>();
        Map<Integer, Integer> rowHeights = new HashMap<>();
        for (SugiyamaVertex vertex : sugiyamaGraph.vertexSet()) {
            if (!vertex.virtual) {
                int column = vertex.index;
                int row = vertex.layer;
                int columnWidth = Math.max(vertex.algorithmUI.getWidth(), columnWidths.getOrDefault(column, 0));
                int rowHeight = Math.max(vertex.algorithmUI.getHeight(), rowHeights.getOrDefault(row, 0));
                columnWidths.put(column, columnWidth);
                rowHeights.put(row, rowHeight);
            }
        }
        for (int column : columnWidths.keySet()) {
            columnWidths.put(column, (int) Math.round(columnWidths.get(column) + 2 * canvasUI.getViewMode().getGridWidth() * canvasUI.getZoom()));
        }
        for (int row : rowHeights.keySet()) {
            rowHeights.put(row, (int) Math.round(rowHeights.get(row) + canvasUI.getViewMode().getGridHeight() * canvasUI.getZoom()));
        }

        // Rearrange algorithms
        int x = canvasUI.getViewMode().getGridWidth();
        for (int column = 0; column <= maxColumn; ++column) {
            Map<Integer, SugiyamaVertex> columnMap = vertexTable.get(column);
            int y = canvasUI.getViewMode().getGridHeight();
            for (int row = 0; row <= maxRow; ++row) {
                SugiyamaVertex vertex = columnMap.getOrDefault(row, null);
                if (vertex != null && !vertex.virtual) {
                    JIPipeDesktopGraphNodeUI ui = vertex.algorithmUI;
                    ui.moveToClosestGridPoint(new Point(x, y), true, true);
                }
                y += rowHeights.getOrDefault(row, 0);
            }
            x += columnWidths.getOrDefault(column, 0);
        }


    }

    /**
     * Model for graph layout
     */
    private static class SugiyamaVertex {
        private JIPipeDesktopGraphNodeUI algorithmUI;
        private int layer = 0;
        private int index = 0;
        private boolean virtual = false;

        private SugiyamaVertex(JIPipeDesktopGraphNodeUI algorithmUI) {
            this.algorithmUI = algorithmUI;
        }

        public SugiyamaVertex() {
            this.virtual = true;
        }
    }

}
