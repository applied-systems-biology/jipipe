package org.hkijena.jipipe.extensions.ijfilaments.datatypes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import ij.ImagePlus;
import ij.gui.EllipseRoi;
import ij.gui.Line;
import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataTableDataSource;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.ijfilaments.display.CachedFilamentsDataViewerWindow;
import org.hkijena.jipipe.extensions.ijfilaments.util.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.nio.json.JSONImporter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Filaments 3D", description = "Stores 3D filaments as graph")
@JIPipeDataStorageDocumentation(humanReadableDescription = "A *.json file containing a JGraphT graph in its serialized form",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
@JsonSerialize(using = FilamentsDataSerializer.class)
@JsonDeserialize(using = FilamentsDataDeserializer.class)
public class Filaments3DData extends SimpleGraph<FilamentVertex, FilamentEdge> implements JIPipeData {

    public Filaments3DData() {
        super(FilamentEdge.class);
    }

    public Filaments3DData(Filaments3DData other) {
        super(FilamentEdge.class);
        mergeWithCopy(other);
    }

    public void mergeWithCopy(Filaments3DData other) {
        Map<FilamentVertex, FilamentVertex> copyMap = new IdentityHashMap<>();
        for (FilamentVertex vertex : other.vertexSet()) {
            FilamentVertex copy = new FilamentVertex(vertex);
            addVertex(copy);
            copyMap.put(vertex, copy);
        }
        for (FilamentEdge edge : other.edgeSet()) {
            FilamentVertex edgeSource = other.getEdgeSource(edge);
            FilamentVertex edgeTarget = other.getEdgeTarget(edge);
            FilamentVertex copyEdgeSource = copyMap.get(edgeSource);
            FilamentVertex copyEdgeTarget = copyMap.get(edgeTarget);
            FilamentEdge edgeCopy = new FilamentEdge(edge);
            addEdge(copyEdgeSource, copyEdgeTarget, edgeCopy);
        }
    }

    public void mergeWith(Filaments3DData other) {
        for (FilamentVertex vertex : other.vertexSet()) {
            addVertex(vertex);
        }
        for (FilamentEdge edge : other.edgeSet()) {
            FilamentVertex edgeSource = other.getEdgeSource(edge);
            FilamentVertex edgeTarget = other.getEdgeTarget(edge);
            addEdge(edgeSource, edgeTarget, edge);
        }
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        try(OutputStream stream = storage.write("graph.json")) {
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(stream, this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new Filaments3DData(this);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        CachedFilamentsDataViewerWindow window = new CachedFilamentsDataViewerWindow(workbench, JIPipeDataTableDataSource.wrap(this, source), displayName);
        window.setVisible(true);
        SwingUtilities.invokeLater(window::reloadDisplayedData);
    }

    @Override
    public Component preview(int width, int height) {
        Rectangle boundsXY = getBoundsXY();
        if(boundsXY.width == 0)
            boundsXY.width = width;
        if(boundsXY.height == 0)
            boundsXY.height = height;
        double scale = Math.min(1.0 * width / boundsXY.width, 1.0 * height / boundsXY.height);
        BufferedImage image = new BufferedImage((int)(boundsXY.width * scale), (int)(boundsXY.height * scale), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        for (FilamentEdge edge : edgeSet()) {
            FilamentVertex edgeSource = getEdgeSource(edge);
            FilamentVertex edgeTarget = getEdgeTarget(edge);
            int x1 = (int)Math.round((edgeSource.getSpatialLocation().getX() - boundsXY.x) * scale);
            int y1 = (int)Math.round((edgeSource.getSpatialLocation().getY() - boundsXY.y) * scale);
            int x2 = (int)Math.round((edgeTarget.getSpatialLocation().getX() - boundsXY.x) * scale);
            int y2 = (int)Math.round((edgeTarget.getSpatialLocation().getY() - boundsXY.y) * scale);
            graphics.setPaint(edge.getColor());
            graphics.drawLine(x1, y1, x2, y2);
        }
//        for (FilamentVertex vertex : vertexSet()) {
//            int x1 = (int)Math.round((vertex.getCentroid().getX() - boundsXY.x) * scale + dx);
//            int y1 = (int)Math.round((vertex.getCentroid().getY() - boundsXY.y) * scale + dy);
//            graphics.setPaint(vertex.getColor());
//            graphics.drawRect(x1,y1,1,1);
//        }
        graphics.dispose();
        return new JLabel(new ImageIcon(image));
    }

    @Override
    public String toString() {
        return String.format("Filaments [%d vertices, %d edges]", vertexSet().size(), edgeSet().size());
    }


    public static Filaments3DData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Filaments3DData graph;
        JSONImporter<FilamentVertex, FilamentEdge> importer = new JSONImporter<>();
        Path jsonPath = storage.findFileByExtension(".json").get();
        try(InputStream stream = storage.open(jsonPath)) {
            graph = JsonUtils.getObjectMapper().readValue(stream, Filaments3DData.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return graph;
    }

    public ROIListData toRoi(boolean ignoreNon2DEdges, boolean withEdges, boolean withVertices, boolean thinEdgeLines) {
        ROIListData outputData = new ROIListData();

        if(withEdges) {
            for (FilamentEdge edge : edgeSet()) {
                FilamentVertex edgeSource = getEdgeSource(edge);
                FilamentVertex edgeTarget = getEdgeTarget(edge);

                if (edgeSource.getSpatialLocation().getZ() != edgeTarget.getSpatialLocation().getZ() ||
                        edgeSource.getNonSpatialLocation().getChannel() != edgeTarget.getNonSpatialLocation().getChannel() ||
                        edgeSource.getNonSpatialLocation().getFrame() != edgeTarget.getNonSpatialLocation().getFrame()) {
                    if (ignoreNon2DEdges)
                        continue;
                    outputData.add(edgeToRoiLine(edge, edgeSource.getSpatialLocation().getZ(), edgeSource.getNonSpatialLocation().getChannel(), edgeSource.getNonSpatialLocation().getFrame(), thinEdgeLines));
                    outputData.add(edgeToRoiLine(edge, edgeTarget.getSpatialLocation().getZ(), edgeTarget.getNonSpatialLocation().getChannel(), edgeTarget.getNonSpatialLocation().getFrame(), thinEdgeLines));
                } else {
                    outputData.add(edgeToRoiLine(edge, edgeSource.getSpatialLocation().getZ(), edgeSource.getNonSpatialLocation().getChannel(), edgeSource.getNonSpatialLocation().getFrame(), thinEdgeLines));
                }
            }
        }
        if(withVertices) {
            for (FilamentVertex vertex : vertexSet()) {
                outputData.add(vertexToRoi(vertex));
            }
        }

        return outputData;
    }

    private Roi vertexToRoi(FilamentVertex vertex) {
        Point3d centroid = vertex.getSpatialLocation();
        NonSpatialPoint3d nonSpatialLocation = vertex.getNonSpatialLocation();
        EllipseRoi roi = new EllipseRoi(centroid.getX() - vertex.getRadius() / 2.0,
                centroid.getY() - vertex.getRadius() / 2.0,
                centroid.getX() + vertex.getRadius() / 2.0,
                centroid.getY() + vertex.getRadius() / 2.0,
                1);
        roi.setName(vertex.getUuid().toString());
        roi.setStrokeColor(vertex.getColor());
//        roi.setFillColor(vertex.getColor());
        int c = Math.max(-1, nonSpatialLocation.getChannel());
        int z = Math.max(-1, centroid.getZ());
        int t = Math.max(-1, nonSpatialLocation.getFrame());
        roi.setPosition(c + 1, z + 1, t + 1);
        return roi;
    }

    public Line edgeToRoiLine(FilamentEdge edge, int z, int c, int t, boolean thinLines) {
        FilamentVertex edgeSource = getEdgeSource(edge);
        FilamentVertex edgeTarget = getEdgeTarget(edge);
        Line roi = new Line(edgeSource.getSpatialLocation().getX(), edgeSource.getSpatialLocation().getY(), edgeTarget.getSpatialLocation().getX(), edgeTarget.getSpatialLocation().getY());
        roi.setStrokeColor(edge.getColor());
//        roi.setFillColor(edge.getColor());
        if(!thinLines) {
            roi.setStrokeWidth((edgeSource.getRadius() + edgeTarget.getRadius()) / 2);
        }
        roi.setName(edge.getUuid().toString());
        roi.setPosition(c + 1, z + 1, t + 1);
        return roi;
    }

    public void removeDuplicateVertices(boolean onlySameComponent) {
        Multimap<Point3d, FilamentVertex> multimap = groupVerticesByLocation();
        Map<FilamentVertex, Integer> componentIds;
        if(onlySameComponent) {
            componentIds = findComponentIds();
        } else {
            componentIds = null;
        }

        for (Point3d location : multimap.keySet()) {
            Collection<FilamentVertex> vertices = multimap.get(location);
            if(onlySameComponent) {
                Map<Integer, List<FilamentVertex>> groups = vertices.stream().collect(Collectors.groupingBy(componentIds::get));
                for (List<FilamentVertex> vertexList : groups.values()) {
                    mergeVertices(vertexList);
                }
            }
            else {
                mergeVertices(vertices);
            }
        }
    }

    public void mergeVertices(Collection<FilamentVertex> vertices) {
        if(vertices.size() > 1) {
            // Apply merge
            FilamentVertex referenceVertex = vertices.iterator().next();

            // Calculate new thickness
            double thickness = 0;
            for (FilamentVertex vertex : vertices) {
                thickness += vertex.getRadius();
            }
            thickness /= vertices.size();
            referenceVertex.setRadius(thickness);

            // Merge
            for (FilamentVertex vertex : vertices) {
                if (vertex != referenceVertex) {
                    for (FilamentEdge edge : edgesOf(vertex)) {
                        FilamentVertex edgeSource = getEdgeSource(edge);
                        FilamentVertex edgeTarget = getEdgeTarget(edge);
                        if (edgeSource == vertex) {
                            addEdgeIgnoreLoops(referenceVertex, edgeTarget);
                        } else {
                            addEdgeIgnoreLoops(edgeSource, referenceVertex);
                        }
                    }
                    removeVertex(vertex);
                }
            }
        }
    }

    public void addEdgeIgnoreLoops(FilamentVertex source, FilamentVertex target) {
        if(!Objects.equals(source, target)) {
            addEdge(source, target);
        }
    }

    public void addEdgeIgnoreLoops(FilamentVertex source, FilamentVertex target, FilamentEdge edge) {
        if(!Objects.equals(source, target)) {
            addEdge(source, target, edge);
        }
    }

    public void removeSelfEdges() {
        for (FilamentEdge edge : ImmutableList.copyOf(edgeSet())) {
            FilamentVertex edgeSource = getEdgeSource(edge);
            FilamentVertex edgeTarget = getEdgeTarget(edge);
            if(edgeSource == edgeTarget) {
                removeEdge(edge);
            }
        }
    }

    public ResultsTableData measureVertices() {
        ResultsTableData tableData = new ResultsTableData();
        tableData.addStringColumn("uuid");
        tableData.addNumericColumn("x");
        tableData.addNumericColumn("y");
        tableData.addNumericColumn("z");
        tableData.addNumericColumn("c");
        tableData.addNumericColumn("t");
        tableData.addNumericColumn("radius");
        tableData.addNumericColumn("value");
        tableData.addStringColumn("color");
        tableData.addNumericColumn("degree");
        Map<String, Object> rowData = new LinkedHashMap<>();
        for (FilamentVertex vertex : vertexSet()) {
            rowData.clear();
            measureVertex(vertex, rowData, "");
            tableData.addRow(rowData);
        }
        return tableData;
    }

    public void measureVertex(FilamentVertex vertex, Map<String, Object> target, String prefix) {
        target.put(prefix + "uuid", vertex.getUuid().toString());
        target.put(prefix + "x", vertex.getSpatialLocation().getX());
        target.put(prefix + "y", vertex.getSpatialLocation().getY());
        target.put(prefix + "z", vertex.getSpatialLocation().getZ());
        target.put(prefix + "c", vertex.getNonSpatialLocation().getChannel());
        target.put(prefix + "t", vertex.getNonSpatialLocation().getFrame());
        target.put(prefix + "radius", vertex.getRadius());
        target.put(prefix + "value", vertex.getValue());
        target.put(prefix + "color", ColorUtils.colorToHexString(vertex.getColor()));
        target.put(prefix + "degree", degreeOf(vertex));
        for (Map.Entry<String, String> entry : vertex.getMetadata().entrySet()) {
            target.put(prefix + entry.getKey(), StringUtils.tryParseDoubleOrReturnString(entry.getValue()));
        }
    }

    public ResultsTableData measureEdges() {
        ResultsTableData tableData = new ResultsTableData();
        tableData.addStringColumn("uuid");
        tableData.addStringColumn("color");
        tableData.addNumericColumn("length");
        Map<String, Object> rowData = new LinkedHashMap<>();
        for (FilamentEdge edge : edgeSet()) {
            rowData.clear();
            measureEdge(edge, rowData, "");
            tableData.addRow(rowData);
        }
        return tableData;
    }

    public void measureEdge(FilamentEdge edge, Map<String, Object> target, String prefix) {
        target.put(prefix + "uuid", edge.getUuid());
        target.put(prefix + "color", ColorUtils.colorToHexString(edge.getColor()));
        target.put(prefix + "length", getEdgeLength(edge));
        for (Map.Entry<String, String> entry : edge.getMetadata().entrySet()) {
            target.put(prefix + entry.getKey(), StringUtils.tryParseDoubleOrReturnString(entry.getValue()));
        }
        measureVertex(getEdgeSource(edge), target, prefix + "source.");
        measureVertex(getEdgeTarget(edge), target, prefix + "target.");
    }

    private double getEdgeLength(FilamentEdge edge) {
        FilamentVertex edgeSource = getEdgeSource(edge);
        FilamentVertex edgeTarget = getEdgeTarget(edge);
        return edgeSource.getSpatialLocation().distanceTo(edgeTarget.getSpatialLocation());
    }

    public Multimap<Point3d, FilamentVertex> groupVerticesByLocation() {
        Multimap<Point3d, FilamentVertex> multimap = HashMultimap.create();
        for (FilamentVertex vertex : vertexSet()) {
            multimap.put(vertex.getSpatialLocation(), vertex);
        }
        return multimap;
    }

    public void smooth(double factorX, double factorY, double factorZ, boolean enforceSameComponent, DefaultExpressionParameter locationMergingFunction) {
        // Backup
        Map<FilamentVertex, Point3d> locationMap = new IdentityHashMap<>();
        for (FilamentVertex vertex : vertexSet()) {
            locationMap.put(vertex, new Point3d(vertex.getSpatialLocation()));
        }
        Map<FilamentVertex, Integer> componentIds;
        if(enforceSameComponent) {
            componentIds = findComponentIds();
        } else {
            componentIds = null;
        }

        // Downscale
        for (FilamentVertex vertex : vertexSet()) {
            Point3d centroid = vertex.getSpatialLocation();
            if(factorX > 0) {
                centroid.setX((int)Math.round(centroid.getX() / factorX));
            }
            if(factorY > 0) {
                centroid.setY((int)Math.round(centroid.getY() / factorY));
            }
            if(factorZ > 0) {
                centroid.setZ((int)Math.round(centroid.getZ() / factorZ));
            }
        }

        // Remove duplicates
       removeDuplicateVertices(enforceSameComponent);

        // Group vertices by location and calculate a new centroid
        Multimap<Point3d, FilamentVertex> multimap = groupVerticesByLocation();
        ExpressionVariables variables = new ExpressionVariables();
        for (FilamentVertex vertex : vertexSet()) {
            // Restore original
            vertex.setSpatialLocation(locationMap.get(vertex));
            Collection<FilamentVertex> group = multimap.get(vertex.getSpatialLocation());

            if(group.size() > 1) {
                if (enforceSameComponent) {
                    int component = componentIds.get(vertex);
                    Set<FilamentVertex> group2 = new HashSet<>();
                    for (FilamentVertex item : group) {
                        if (componentIds.get(item) == component) {
                            group2.add(item);
                        }
                    }
                    calculateNewVertexLocation(factorX, factorY, factorZ, locationMergingFunction, locationMap, variables, vertex, group2);
                } else {
                    calculateNewVertexLocation(factorX, factorY, factorZ, locationMergingFunction, locationMap, variables, vertex, group);
                }
            }
        }

        // Cleanup
        removeSelfEdges();
    }

    private static void calculateNewVertexLocation(double factorX, double factorY, double factorZ, DefaultExpressionParameter locationMergingFunction, Map<FilamentVertex, Point3d> locationMap, ExpressionVariables variables, FilamentVertex vertex, Collection<FilamentVertex> group) {
        // Apply average where applicable
        if(factorX > 0) {
            variables.set("values", group.stream().map(v -> locationMap.get(v).getX()).collect(Collectors.toList()));
            vertex.getSpatialLocation().setX(locationMergingFunction.evaluateToInteger(variables));
        }
        if(factorY > 0) {
            variables.set("values", group.stream().map(v -> locationMap.get(v).getY()).collect(Collectors.toList()));
            vertex.getSpatialLocation().setY(locationMergingFunction.evaluateToInteger(variables));
        }
        if(factorZ > 0) {
            variables.set("values", group.stream().map(v -> locationMap.get(v).getZ()).collect(Collectors.toList()));
            vertex.getSpatialLocation().setZ(locationMergingFunction.evaluateToInteger(variables));
        }
    }

    public boolean isEmpty() {
        return vertexSet().isEmpty();
    }

    public Rectangle getBoundsXY() {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (FilamentVertex vertex : vertexSet()) {
            minX = Math.min(minX, vertex.getSpatialLocation().getX());
            maxX = Math.max(maxX, vertex.getSpatialLocation().getX());
            minY = Math.min(minY, vertex.getSpatialLocation().getY());
            maxY = Math.max(maxY, vertex.getSpatialLocation().getY());
        }
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    public void removeVertexIf(Predicate<FilamentVertex> predicate) {
        List<FilamentVertex> toDelete = vertexSet().stream().filter(predicate).collect(Collectors.toList());
        for (FilamentVertex vertex : toDelete) {
            removeVertex(vertex);
        }
    }

    public Map<FilamentVertex, Integer> findComponentIds() {
        ConnectivityInspector<FilamentVertex, FilamentEdge> connectivityInspector = getConnectivityInspector();
        Map<FilamentVertex, Integer> result = new HashMap<>();
        int component = 0;
        for (Set<FilamentVertex> connectedSet : connectivityInspector.connectedSets()) {
            for(FilamentVertex vertex : connectedSet) {
                result.put(vertex, component);
            }
            ++component;
        }
        return result;
    }

    public void removeComponentsAtBorder(ImagePlus reference, boolean removeInX, boolean removeInY, boolean removeInZ, boolean useThickness, double borderDistance) {
        ConnectivityInspector<FilamentVertex, FilamentEdge> connectivityInspector = getConnectivityInspector();
        Set<FilamentVertex> toDelete = new HashSet<>();

        for (Set<FilamentVertex> connectedSet : connectivityInspector.connectedSets()) {
            boolean found = false;
            for (FilamentVertex vertex : connectedSet) {
                if(removeInX) {
                    double xMin = vertex.getXMin(useThickness);
                    double xMax = vertex.getXMax(useThickness);
                    if(xMin <= borderDistance) {
                        found = true;
                        break;
                    }
                    if(xMax >= reference.getWidth() - borderDistance - 1) {
                        found = true;
                        break;
                    }
                }
                if(removeInY) {
                    double yMin = vertex.getYMin(useThickness);
                    double yMax = vertex.getYMax(useThickness);
                    if(yMin <= borderDistance) {
                        found = true;
                        break;
                    }
                    if(yMax >= reference.getHeight() - borderDistance - 1) {
                        found = true;
                        break;
                    }
                }
                if(removeInZ) {
                    double zMin = vertex.getZMin(useThickness);
                    double zMax = vertex.getZMax(useThickness);
                    if(zMin <= borderDistance) {
                        found = true;
                        break;
                    }
                    if(zMax >= reference.getNSlices() - borderDistance - 1) {
                        found = true;
                        break;
                    }
                }
            }
            if(found) {
                toDelete.addAll(connectedSet);
            }
        }

        removeAllVertices(toDelete);
    }

    /**
     * Simplifies the graph by only keeping (degree 0 or 1) and branching points (degree more than 2).
     * Keeps the connections.
     */
    public void simplify() {
        boolean updated;
        do {
            updated = false;
            for (FilamentVertex vertex : ImmutableList.copyOf(vertexSet())) {
                if(degreeOf(vertex) == 2) {
                    List<FilamentVertex> neighbors = Graphs.neighborListOf(this, vertex);
                    addEdge(neighbors.get(0), neighbors.get(1));
                    removeVertex(vertex);
                    updated = true;
                }
            }
        }
        while(updated);
    }

    /**
     * Returns a shallow copy (the vertices and edges are not copied)
     * @return the shallow copy
     */
    public Filaments3DData shallowCopy() {
        Filaments3DData copy = new Filaments3DData();
        for (FilamentVertex vertex : vertexSet()) {
            copy.addVertex(vertex);
        }
        for (FilamentEdge edge : edgeSet()) {
            copy.addEdge(getEdgeSource(edge), getEdgeTarget(edge), edge);
        }
        return copy;
    }

    /**
     * Extracts a shallow copy of the nodes end edges that only contains the selected vertices
     * @param vertices the vertices
     * @return the shallow copy
     */
    public Filaments3DData extractShallowCopy(Set<FilamentVertex> vertices) {
        Filaments3DData copy = new Filaments3DData();
        for (FilamentVertex vertex : vertexSet()) {
            if(vertices.contains(vertex)) {
                copy.addVertex(vertex);
            }
        }
        for (FilamentEdge edge : edgeSet()) {
            FilamentVertex edgeSource = getEdgeSource(edge);
            FilamentVertex edgeTarget = getEdgeTarget(edge);
            if(vertices.contains(edgeSource) || vertices.contains(edgeTarget)) {
                copy.addEdge(edgeSource, edgeTarget, edge);
            }
        }
        return copy;
    }

    public ConnectivityInspector<FilamentVertex, FilamentEdge> getConnectivityInspector() {
        return new ConnectivityInspector<>(this);
    }

    public ResultsTableData measure() {
        ResultsTableData measurements = new ResultsTableData();
        ConnectivityInspector<FilamentVertex, FilamentEdge> connectivityInspector = getConnectivityInspector();
        List<Set<FilamentVertex>> connectedSets = connectivityInspector.connectedSets();
        for (int i = 0; i < connectedSets.size(); i++) {
            Set<FilamentVertex> vertices = connectedSets.get(i);
            Set<FilamentEdge> edges = edgesOf(vertices);

            // Vertex stats
            double minVertexRadius = Double.POSITIVE_INFINITY;
            double maxVertexRadius = Double.NEGATIVE_INFINITY;
            double sumVertexRadius = 0;
            double minVertexIntensity = Double.POSITIVE_INFINITY;
            double maxVertexIntensity = Double.NEGATIVE_INFINITY;
            double sumVertexIntensity = 0;
            for (FilamentVertex vertex : vertices) {
                minVertexRadius = Math.min(vertex.getRadius(), minVertexRadius);
                maxVertexRadius = Math.max(vertex.getRadius(), maxVertexRadius);
                sumVertexRadius += vertex.getRadius();
                minVertexIntensity = Math.min(vertex.getValue(), minVertexIntensity);
                maxVertexIntensity = Math.max(vertex.getValue(), maxVertexIntensity);
                sumVertexIntensity += vertex.getValue();
            }

            // Edge stats
            double minEdgeLength = Double.POSITIVE_INFINITY;
            double maxEdgeLength = Double.NEGATIVE_INFINITY;
            double sumEdgeLength = 0;

            for (FilamentEdge edge : edges) {
                double length = getEdgeLength(edge);
                minEdgeLength = Math.min(minEdgeLength, length);
                maxEdgeLength = Math.max(maxEdgeLength, length);
                sumEdgeLength += length;
            }

            double sumEdgeLengthCorrected = sumEdgeLength;
            for (FilamentVertex vertex : vertices) {
                int degree = degreeOf(vertex);
                if(degree == 0) {
                    // Count radius 2 times
                    sumEdgeLengthCorrected += vertex.getRadius() * 2;
                }
                else if(degree == 1) {
                    // Count radius 1 time
                    sumEdgeLengthCorrected += vertex.getRadius();
                }
            }

            // Make a simplified copy and calculate the simplified edge lengths
            Filaments3DData simplified = extractShallowCopy(vertices);
            simplified.simplify();
            double simplifiedSumEdgeLength = 0;
            for (FilamentEdge edge : simplified.edgeSet()) {
                simplifiedSumEdgeLength += simplified.getEdgeLength(edge);
            }

            double simplifiedSumEdgeLengthCorrected = simplifiedSumEdgeLength;
            for (FilamentVertex vertex : simplified.vertexSet()) {
                int degree = simplified.degreeOf(vertex);
                if(degree == 0) {
                    // Count radius 2 times
                    simplifiedSumEdgeLengthCorrected += vertex.getRadius() * 2;
                }
                else if(degree == 1) {
                    // Count radius 1 time
                    simplifiedSumEdgeLengthCorrected += vertex.getRadius();
                }
            }

            // Min/max location
            double minXCenter = Double.POSITIVE_INFINITY;
            double maxXCenter = Double.NEGATIVE_INFINITY;
            double minYCenter = Double.POSITIVE_INFINITY;
            double maxYCenter = Double.NEGATIVE_INFINITY;
            double minZCenter = Double.POSITIVE_INFINITY;
            double maxZCenter = Double.NEGATIVE_INFINITY;
            double minXRadius = Double.POSITIVE_INFINITY;
            double maxXRadius = Double.NEGATIVE_INFINITY;
            double minYRadius = Double.POSITIVE_INFINITY;
            double maxYRadius = Double.NEGATIVE_INFINITY;
            double minZRadius = Double.POSITIVE_INFINITY;
            double maxZRadius = Double.NEGATIVE_INFINITY;

            for (FilamentVertex vertex : vertices) {
                minXCenter = Math.min(vertex.getXMin(false), minXCenter);
                minYCenter = Math.min(vertex.getYMin(false), minYCenter);
                minZCenter = Math.min(vertex.getZMin(false), minZCenter);
                maxXCenter = Math.max(vertex.getXMax(false), maxXCenter);
                maxYCenter = Math.max(vertex.getYMax(false), maxYCenter);
                maxZCenter = Math.max(vertex.getZMax(false), maxZCenter);
                minXRadius = Math.min(vertex.getXMin(true), minXRadius);
                minYRadius = Math.min(vertex.getYMin(true), minYRadius);
                minZRadius = Math.min(vertex.getZMin(true), minZRadius);
                maxXRadius = Math.max(vertex.getXMax(true), maxXRadius);
                maxYRadius = Math.max(vertex.getYMax(true), maxYRadius);
                maxZRadius = Math.max(vertex.getZMax(true), maxZRadius);
            }

            int row = measurements.addRow();
            measurements.setValueAt(row, row, "Component");
            measurements.setValueAt(vertices.size(), row, "numVertices");
            measurements.setValueAt(edges.size(), row, "numEdges");
            measurements.setValueAt(sumEdgeLength, row, "lengthPixels");
            measurements.setValueAt(sumEdgeLengthCorrected, row, "lengthPixelsRadiusCorrected");
            measurements.setValueAt(simplifiedSumEdgeLength, row, "simplifiedLengthPixels");
            measurements.setValueAt(simplifiedSumEdgeLengthCorrected, row, "simplifiedLengthPixelsRadiusCorrected");
            measurements.setValueAt(simplifiedSumEdgeLength / sumEdgeLength, row, "confinementRatio");
            measurements.setValueAt(simplifiedSumEdgeLengthCorrected / sumEdgeLengthCorrected, row, "confinementRatioRadiusCorrected");
            measurements.setValueAt(vertices.stream().filter(vertex -> degreeOf(vertex) == 0).count(), row, "numVerticesWithDegree0");
            measurements.setValueAt(vertices.stream().filter(vertex -> degreeOf(vertex) == 1).count(), row, "numVerticesWithDegree1");
            measurements.setValueAt(vertices.stream().filter(vertex -> degreeOf(vertex) == 2).count(), row, "numVerticesWithDegree2");
            measurements.setValueAt(vertices.stream().filter(vertex -> degreeOf(vertex) == 3).count(), row, "numVerticesWithDegree3");
            measurements.setValueAt(vertices.stream().filter(vertex -> degreeOf(vertex) == 4).count(), row, "numVerticesWithDegree4");
            measurements.setValueAt(vertices.stream().filter(vertex -> degreeOf(vertex) == 5).count(), row, "numVerticesWithDegree5");
            measurements.setValueAt(vertices.stream().filter(vertex -> degreeOf(vertex) > 5).count(), row, "numVerticesWithDegreeMoreThan5");
            measurements.setValueAt(minXCenter, row, "centerMinX");
            measurements.setValueAt(minYCenter, row, "centerMinY");
            measurements.setValueAt(minZCenter, row, "centerMinZ");
            measurements.setValueAt(maxXCenter, row, "centerMaxX");
            measurements.setValueAt(maxYCenter, row, "centerMaxY");
            measurements.setValueAt(maxZCenter, row, "centerMaxZ");
            measurements.setValueAt(minXRadius, row, "sphereMinX");
            measurements.setValueAt(minYRadius, row, "sphereMinY");
            measurements.setValueAt(minZRadius, row, "sphereMinZ");
            measurements.setValueAt(maxXRadius, row, "sphereMaxX");
            measurements.setValueAt(maxYRadius, row, "sphereMaxY");
            measurements.setValueAt(maxZRadius, row, "sphereMaxZ");
            measurements.setValueAt(minEdgeLength, row, "minEdgeLength");
            measurements.setValueAt(maxEdgeLength, row, "maxEdgeLength");
            measurements.setValueAt(sumEdgeLength / edges.size(), row, "avgEdgeLength");
            measurements.setValueAt(minVertexRadius, row, "minVertexRadius");
            measurements.setValueAt(maxVertexRadius, row, "maxVertexRadius");
            measurements.setValueAt(sumVertexRadius / vertices.size(), row, "avgVertexRadius");
            measurements.setValueAt(minVertexIntensity, row, "minVertexValue");
            measurements.setValueAt(maxVertexIntensity, row, "maxVertexValue");
            measurements.setValueAt(sumVertexIntensity / vertices.size(), row, "avgVertexValue");
        }
        return measurements;
    }

    private Set<FilamentEdge> edgesOf(Set<FilamentVertex> vertices) {
        Set<FilamentEdge> edges = new HashSet<>();
        for (FilamentVertex vertex : vertices) {
            edges.addAll(edgesOf(vertex));
        }
        return edges;
    }
}
