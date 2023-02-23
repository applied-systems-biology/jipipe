package org.hkijena.jipipe.extensions.ijfilaments.datatypes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
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
        CachedFilamentsDataViewerWindow window = new CachedFilamentsDataViewerWindow(workbench, JIPipeDataTableDataSource.wrap(this, source), displayName, false);
        window.setVisible(true);
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
        EllipseRoi roi = new EllipseRoi(centroid.getX() - vertex.getThickness() / 2.0,
                centroid.getY() - vertex.getThickness() / 2.0,
                centroid.getX() + vertex.getThickness() / 2.0,
                centroid.getY() + vertex.getThickness() / 2.0,
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
            roi.setStrokeWidth((edgeSource.getThickness() + edgeTarget.getThickness()) / 2);
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
                thickness += vertex.getThickness();
            }
            thickness /= vertices.size();
            referenceVertex.setThickness(thickness);

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
        tableData.addNumericColumn("thickness");
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
        target.put(prefix + "thickness", vertex.getThickness());
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
        ConnectivityInspector<FilamentVertex, FilamentEdge> connectivityInspector = new ConnectivityInspector<>(this);
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
}
