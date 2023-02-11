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

@JIPipeDocumentation(name = "Filaments", description = "Stores filaments as graph")
@JIPipeDataStorageDocumentation(humanReadableDescription = "A *.json file containing a JGraphT graph in its serialized form",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
@JsonSerialize(using = FilamentsDataSerializer.class)
@JsonDeserialize(using = FilamentsDataDeserializer.class)
public class FilamentsData  extends SimpleGraph<FilamentVertex, FilamentEdge> implements JIPipeData {

    public FilamentsData() {
        super(FilamentEdge.class);
    }

    public FilamentsData(FilamentsData other) {
        super(FilamentEdge.class);
        mergeWithCopy(other);
    }

    public void mergeWithCopy(FilamentsData other) {
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

    public void mergeWith(FilamentsData other) {
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
        return new FilamentsData(this);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        CachedFilamentsDataViewerWindow window = new CachedFilamentsDataViewerWindow(workbench, JIPipeDataTableDataSource.wrap(this, source), displayName, false);
        window.setVisible(true);
    }

    @Override
    public Component preview(int width, int height) {
        Rectangle boundsXY = getBoundsXY();
        double scale = Math.min(1.0 * width / boundsXY.width, 1.0 * height / boundsXY.height);
        BufferedImage image = new BufferedImage((int)(boundsXY.width * scale), (int)(boundsXY.height * scale), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        for (FilamentEdge edge : edgeSet()) {
            FilamentVertex edgeSource = getEdgeSource(edge);
            FilamentVertex edgeTarget = getEdgeTarget(edge);
            int x1 = (int)Math.round((edgeSource.getCentroid().getX() - boundsXY.x) * scale);
            int y1 = (int)Math.round((edgeSource.getCentroid().getY() - boundsXY.y) * scale);
            int x2 = (int)Math.round((edgeTarget.getCentroid().getX() - boundsXY.x) * scale);
            int y2 = (int)Math.round((edgeTarget.getCentroid().getY() - boundsXY.y) * scale);
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


    public static FilamentsData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        FilamentsData graph;
        JSONImporter<FilamentVertex, FilamentEdge> importer = new JSONImporter<>();
        Path jsonPath = storage.findFileByExtension(".json").get();
        try(InputStream stream = storage.open(jsonPath)) {
            graph = JsonUtils.getObjectMapper().readValue(stream, FilamentsData.class);
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

                if (edgeSource.getCentroid().getZ() != edgeTarget.getCentroid().getZ() ||
                        edgeSource.getCentroid().getC() != edgeTarget.getCentroid().getC() ||
                        edgeSource.getCentroid().getT() != edgeTarget.getCentroid().getT()) {
                    if (ignoreNon2DEdges)
                        continue;
                    outputData.add(edgeToRoiLine(edge, edgeSource.getCentroid().getZ(), edgeSource.getCentroid().getC(), edgeSource.getCentroid().getT(), thinEdgeLines));
                    outputData.add(edgeToRoiLine(edge, edgeTarget.getCentroid().getZ(), edgeTarget.getCentroid().getC(), edgeTarget.getCentroid().getT(), thinEdgeLines));
                } else {
                    outputData.add(edgeToRoiLine(edge, edgeSource.getCentroid().getZ(), edgeSource.getCentroid().getC(), edgeSource.getCentroid().getT(), thinEdgeLines));
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
        FilamentLocation centroid = vertex.getCentroid();
        EllipseRoi roi = new EllipseRoi(centroid.getX() - vertex.getThickness() / 2.0,
                centroid.getY() - vertex.getThickness() / 2.0,
                centroid.getX() + vertex.getThickness() / 2.0,
                centroid.getY() + vertex.getThickness() / 2.0,
                1);
        roi.setName(vertex.getUuid().toString());
        roi.setStrokeColor(vertex.getColor());
//        roi.setFillColor(vertex.getColor());
        int c = Math.max(-1, centroid.getC());
        int z = Math.max(-1, centroid.getZ());
        int t = Math.max(-1, centroid.getT());
        roi.setPosition(c + 1, z + 1, t + 1);
        return roi;
    }

    public Line edgeToRoiLine(FilamentEdge edge, int z, int c, int t, boolean thinLines) {
        FilamentVertex edgeSource = getEdgeSource(edge);
        FilamentVertex edgeTarget = getEdgeTarget(edge);
        Line roi = new Line(edgeSource.getCentroid().getX(), edgeSource.getCentroid().getY(), edgeTarget.getCentroid().getX(), edgeTarget.getCentroid().getY());
        roi.setStrokeColor(edge.getColor());
//        roi.setFillColor(edge.getColor());
        if(!thinLines) {
            roi.setStrokeWidth((edgeSource.getThickness() + edgeTarget.getThickness()) / 2);
        }
        roi.setName(edge.getUuid().toString());
        roi.setPosition(c + 1, z + 1, t + 1);
        return roi;
    }

    public void removeDuplicateVertices() {
        Multimap<FilamentLocation, FilamentVertex> multimap = groupVerticesByLocation();

        for (FilamentLocation location : multimap.keySet()) {
            Collection<FilamentVertex> vertices = multimap.get(location);
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
                    if(vertex != referenceVertex) {
                        for (FilamentEdge edge : edgesOf(vertex)) {
                            FilamentVertex edgeSource = getEdgeSource(edge);
                            FilamentVertex edgeTarget = getEdgeTarget(edge);
                            if(edgeSource == vertex) {
                                addEdgeIgnoreLoops(referenceVertex, edgeTarget);
                            }
                            else {
                                addEdgeIgnoreLoops(edgeSource, referenceVertex);
                            }
                        }
                        removeVertex(vertex);
                    }
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
        target.put(prefix + "x", vertex.getCentroid().getX());
        target.put(prefix + "y", vertex.getCentroid().getY());
        target.put(prefix + "z", vertex.getCentroid().getZ());
        target.put(prefix + "c", vertex.getCentroid().getC());
        target.put(prefix + "t", vertex.getCentroid().getT());
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
        return edgeSource.getCentroid().distanceTo(edgeTarget.getCentroid());
    }

    public Multimap<FilamentLocation, FilamentVertex> groupVerticesByLocation() {
        Multimap<FilamentLocation, FilamentVertex> multimap = HashMultimap.create();
        for (FilamentVertex vertex : vertexSet()) {
            multimap.put(vertex.getCentroid(), vertex);
        }
        return multimap;
    }

    public void smooth(double factorXY, double factorZ, double factorC, double factorT, DefaultExpressionParameter locationMergingFunction) {
        // Backup
        Map<FilamentVertex, FilamentLocation> locationMap = new IdentityHashMap<>();
        for (FilamentVertex vertex : vertexSet()) {
            locationMap.put(vertex, new FilamentLocation(vertex.getCentroid()));
        }

        // Downscale
        for (FilamentVertex vertex : vertexSet()) {
            FilamentLocation centroid = vertex.getCentroid();
            if(factorXY > 0) {
                centroid.setX((int)Math.round(centroid.getX() / factorXY));
                centroid.setY((int)Math.round(centroid.getY() / factorXY));
            }
            if(factorZ > 0 && centroid.getZ() > 0) {
                centroid.setZ((int)Math.round(centroid.getZ() / factorZ));
            }
            if(factorC > 0 && centroid.getC() > 0) {
                centroid.setC((int)Math.round(centroid.getC() / factorC));
            }
            if(factorT > 0 && centroid.getT() > 0) {
                centroid.setT((int)Math.round(centroid.getT() / factorT));
            }
        }

        // Remove duplicates
       removeDuplicateVertices();

        // Group vertices by location and calculate a new centroid
        Multimap<FilamentLocation, FilamentVertex> multimap = groupVerticesByLocation();
        ExpressionVariables variables = new ExpressionVariables();
        for (FilamentVertex vertex : vertexSet()) {
            Collection<FilamentVertex> group = multimap.get(vertex.getCentroid());

            // Restore original
            vertex.setCentroid(locationMap.get(vertex));

            // Apply average where applicable
            if(factorXY > 0) {
                variables.set("values", group.stream().map(v -> v.getCentroid().getX()).collect(Collectors.toList()));
                vertex.getCentroid().setX(locationMergingFunction.evaluateToInteger(variables));
            }
            if(factorXY > 0) {
                variables.set("values", group.stream().map(v -> v.getCentroid().getY()).collect(Collectors.toList()));
                vertex.getCentroid().setY(locationMergingFunction.evaluateToInteger(variables));
            }
            if(factorC > 0) {
                variables.set("values", group.stream().map(v -> v.getCentroid().getC()).collect(Collectors.toList()));
                vertex.getCentroid().setC(locationMergingFunction.evaluateToInteger(variables));
            }
            if(factorZ > 0) {
                variables.set("values", group.stream().map(v -> v.getCentroid().getX()).collect(Collectors.toList()));
                vertex.getCentroid().setZ(locationMergingFunction.evaluateToInteger(variables));
            }
            if(factorT > 0) {
                variables.set("values", group.stream().map(v -> v.getCentroid().getT()).collect(Collectors.toList()));
                vertex.getCentroid().setT(locationMergingFunction.evaluateToInteger(variables));
            }
        }

        // Cleanup
        removeSelfEdges();
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
            minX = Math.min(minX, vertex.getCentroid().getX());
            maxX = Math.max(maxX, vertex.getCentroid().getX());
            minY = Math.min(minY, vertex.getCentroid().getY());
            maxY = Math.max(maxY, vertex.getCentroid().getY());
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
