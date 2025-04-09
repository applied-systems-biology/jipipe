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

package org.hkijena.jipipe.plugins.ijfilaments.datatypes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.EllipseRoi;
import ij.gui.Line;
import ij.gui.Roi;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import mcib3d.geom.Vector3D;
import mcib3d.image3d.ImageHandler;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeImageThumbnailData;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.plugins.ij3d.utils.ExtendedObjectCreator3D;
import org.hkijena.jipipe.plugins.ijfilaments.display.FilamentsManagerPlugin2D;
import org.hkijena.jipipe.plugins.ijfilaments.util.*;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.BitDepth;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imageviewer.legacy.api.JIPipeDesktopLegacyImageViewerOverlay;
import org.hkijena.jipipe.plugins.imageviewer.legacy.api.JIPipeDesktopLegacyImageViewerPlugin;
import org.hkijena.jipipe.plugins.napari.NapariOverlay;
import org.hkijena.jipipe.plugins.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.plugins.scene3d.model.Scene3DGroupNode;
import org.hkijena.jipipe.plugins.scene3d.model.geometries.Scene3DLineGeometry;
import org.hkijena.jipipe.plugins.scene3d.model.geometries.Scene3DSphereGeometry;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.nio.json.JSONImporter;
import org.scijava.vecmath.Vector3d;

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

@SetJIPipeDocumentation(name = "Filaments 3D", description = "Stores 3D filaments as graph")
@JIPipeDataStorageDocumentation(humanReadableDescription = "A *.json file containing a JGraphT graph in its serialized form",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
@JsonSerialize(using = FilamentsDataSerializer.class)
@JsonDeserialize(using = FilamentsDataDeserializer.class)
public class Filaments3DGraphData extends SimpleGraph<FilamentVertex, FilamentEdge> implements JIPipeData, JIPipeDesktopLegacyImageViewerOverlay, NapariOverlay {

    public Filaments3DGraphData() {
        super(FilamentEdge.class);
    }

    public Filaments3DGraphData(Filaments3DGraphData other) {
        super(FilamentEdge.class);
        mergeWithCopy(other);
    }

    public static Filaments3DGraphData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Filaments3DGraphData graph;
        JSONImporter<FilamentVertex, FilamentEdge> importer = new JSONImporter<>();
        Path jsonPath = storage.findFileByExtension(".json").get();
        try (InputStream stream = storage.open(jsonPath)) {
            graph = JsonUtils.getObjectMapper().readValue(stream, Filaments3DGraphData.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return graph;
    }

    private static void calculateNewVertexLocation(double factorX, double factorY, double factorZ, JIPipeExpressionParameter locationMergingFunction, Map<FilamentVertex, Point3d> locationMap, JIPipeExpressionVariablesMap variables, FilamentVertex vertex, Collection<FilamentVertex> group) {
        // Apply average where applicable
        double x = vertex.getSpatialLocation().getX();
        double y = vertex.getSpatialLocation().getY();
        double z = vertex.getSpatialLocation().getZ();
        if (factorX > 0) {
            variables.set("values", group.stream().map(v -> locationMap.get(v).getX()).collect(Collectors.toList()));
            x = locationMergingFunction.evaluateToInteger(variables);
        }
        if (factorY > 0) {
            variables.set("values", group.stream().map(v -> locationMap.get(v).getY()).collect(Collectors.toList()));
            y = locationMergingFunction.evaluateToInteger(variables);
        }
        if (factorZ > 0) {
            variables.set("values", group.stream().map(v -> locationMap.get(v).getZ()).collect(Collectors.toList()));
            z = locationMergingFunction.evaluateToInteger(variables);
        }
        vertex.setSpatialLocation(new Point3d(x, y, z));
    }

    private static Vector3d getFinalVertexLocationFor3DExport(boolean physicalSizes, Quantity.LengthUnit meshLengthUnit, boolean forceMeshLengthUnit, String consensusUnit, FilamentVertex vertex) {
        Vector3d finalLocation;
        if (physicalSizes) {
            if (forceMeshLengthUnit) {
                finalLocation = vertex.getSpatialLocationInUnit(meshLengthUnit.name());
            } else {
                finalLocation = vertex.getSpatialLocationInUnit(consensusUnit);
            }
        } else {
            finalLocation = vertex.getSpatialLocation().toSciJavaVector3d();
        }
        return finalLocation;
    }

    public static void drawLabelLineOnProcessor(double x0, double y0, double z0, double x1, double y1, double z1, int label, double rad0, double rad1, ImageProcessor processor, int imageZ) {
        Vector3D V = new Vector3D(x1 - x0, y1 - y0, z1 - z0);
        double len = V.getLength();
        V.normalize();
        double vx = V.getX();
        double vy = V.getY();
        double vz = V.getZ();
        for (int i = 0; i < (int) len; i++) {
            double perc = i / len;
            int rad = (int) (rad0 + perc * (rad1 - rad0));
            drawLabelBallOnProcessor((int) (x0 + i * vx), (int) (y0 + i * vy), (int) (z0 + i * vz), label, rad, false, processor, imageZ);
        }
    }

    public static void drawLabelBallOnProcessor(int targetX, int targetY, int targetZ, int label, int radius, boolean hollow, ImageProcessor processor, int imageZ) {
        int imageWidth = processor.getWidth();
        int imageHeight = processor.getHeight();
        if (radius <= 0) {
            if (targetZ == imageZ) {
                processor.setf(targetX, targetY, label);
            }
        } else if (Math.abs(imageZ - targetZ) <= radius) {
            float[] pixels = (float[]) processor.getPixels();
            for (int y = targetY - radius; y < targetY + radius; y++) {
                if (y < 0 || y >= imageHeight)
                    continue;
                for (int x = targetX - radius; x < targetX + radius; x++) {
                    if (x < 0 || x >= imageWidth)
                        continue;
                    double k = Math.pow(x - targetX, 2) + Math.pow(y - targetY, 2) + Math.pow(imageZ - targetZ, 2);
                    if (k > radius * radius) {
                        continue;
                    }
                    if (hollow && k < Math.pow(radius - 1, 2)) {
                        continue;
                    }
                    pixels[x + y * imageWidth] = label;
                }
            }
        }
    }

    public void mergeWithCopy(Filaments3DGraphData other) {
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

    public void mergeWith(Filaments3DGraphData other) {
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
        try (OutputStream stream = storage.write("graph.json")) {
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(stream, this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new Filaments3DGraphData(this);
    }

    @Override
    public JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        Rectangle boundsXY = getBoundsXY();
        if (boundsXY.width == 0)
            boundsXY.width = width;
        if (boundsXY.height == 0)
            boundsXY.height = height;
        double scale = Math.min(1.0 * width / boundsXY.width, 1.0 * height / boundsXY.height);
        BufferedImage image = new BufferedImage((int) Math.max(1, boundsXY.width * scale), (int) Math.max(1, boundsXY.height * scale), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        if (edgeSet().isEmpty()) {
            for (FilamentVertex vertex : vertexSet()) {
                int x1 = (int) Math.round((vertex.getSpatialLocation().getX() - boundsXY.x) * scale);
                int y1 = (int) Math.round((vertex.getSpatialLocation().getY() - boundsXY.y) * scale);
                graphics.setPaint(vertex.getColor());
                graphics.drawRect(x1, y1, 1, 1);
            }
        } else {
            for (FilamentEdge edge : edgeSet()) {
                FilamentVertex edgeSource = getEdgeSource(edge);
                FilamentVertex edgeTarget = getEdgeTarget(edge);
                int x1 = (int) Math.round((edgeSource.getSpatialLocation().getX() - boundsXY.x) * scale);
                int y1 = (int) Math.round((edgeSource.getSpatialLocation().getY() - boundsXY.y) * scale);
                int x2 = (int) Math.round((edgeTarget.getSpatialLocation().getX() - boundsXY.x) * scale);
                int y2 = (int) Math.round((edgeTarget.getSpatialLocation().getY() - boundsXY.y) * scale);
                graphics.setPaint(edge.getColor());
                graphics.drawLine(x1, y1, x2, y2);
            }
        }


        graphics.dispose();
        return new JIPipeImageThumbnailData(image);
    }

    @Override
    public String toString() {
        return String.format("Filaments [%d vertices, %d edges]", vertexSet().size(), edgeSet().size());
    }

    public ROI2DListData toRoi(boolean ignoreNon2DEdges, boolean withEdges, boolean withVertices, int forcedLineThickness, int forcedVertexRadius) {
        ROI2DListData outputData = new ROI2DListData();

        if (withEdges) {
            for (FilamentEdge edge : edgeSet()) {
                FilamentVertex edgeSource = getEdgeSource(edge);
                FilamentVertex edgeTarget = getEdgeTarget(edge);

                if (edgeSource.getSpatialLocation().getZ() != edgeTarget.getSpatialLocation().getZ() ||
                        edgeSource.getNonSpatialLocation().getChannel() != edgeTarget.getNonSpatialLocation().getChannel() ||
                        edgeSource.getNonSpatialLocation().getFrame() != edgeTarget.getNonSpatialLocation().getFrame()) {
                    if (ignoreNon2DEdges)
                        continue;
                    outputData.add(edgeToRoiLine(edge, (int) edgeSource.getSpatialLocation().getZ(), edgeSource.getNonSpatialLocation().getChannel(), edgeSource.getNonSpatialLocation().getFrame(), forcedLineThickness));
                    outputData.add(edgeToRoiLine(edge, (int) edgeTarget.getSpatialLocation().getZ(), edgeTarget.getNonSpatialLocation().getChannel(), edgeTarget.getNonSpatialLocation().getFrame(), forcedLineThickness));
                } else {
                    outputData.add(edgeToRoiLine(edge, (int) edgeSource.getSpatialLocation().getZ(), edgeSource.getNonSpatialLocation().getChannel(), edgeSource.getNonSpatialLocation().getFrame(), forcedLineThickness));
                }
            }
        }
        if (withVertices) {
            for (FilamentVertex vertex : vertexSet()) {
                outputData.add(vertexToRoi(vertex, forcedVertexRadius));
            }
        }

        return outputData;
    }

    private Roi vertexToRoi(FilamentVertex vertex, int forcedVertexRadius) {
        Point3d centroid = vertex.getSpatialLocation();
        NonSpatialPoint3d nonSpatialLocation = vertex.getNonSpatialLocation();
        double radius = vertex.getRadius();
        if (forcedVertexRadius > 0) {
            radius = forcedVertexRadius;
        }
        EllipseRoi roi = new EllipseRoi(centroid.getX() - radius / 2.0,
                centroid.getY() - radius / 2.0,
                centroid.getX() + radius / 2.0,
                centroid.getY() + radius / 2.0,
                1);
        roi.setName(vertex.getUuid().toString());
        roi.setStrokeColor(vertex.getColor());
//        roi.setFillColor(vertex.getColor());
        int c = Math.max(-1, nonSpatialLocation.getChannel());
        int z = (int) Math.max(-1, centroid.getZ());
        int t = Math.max(-1, nonSpatialLocation.getFrame());
        roi.setPosition(c + 1, z + 1, t + 1);
        return roi;
    }

    public Line edgeToRoiLine(FilamentEdge edge, int z, int c, int t, int forcedLineThickness) {
        FilamentVertex edgeSource = getEdgeSource(edge);
        FilamentVertex edgeTarget = getEdgeTarget(edge);
        Line roi = new Line(edgeSource.getSpatialLocation().getX(), edgeSource.getSpatialLocation().getY(), edgeTarget.getSpatialLocation().getX(), edgeTarget.getSpatialLocation().getY());
        roi.setStrokeColor(edge.getColor());
//        roi.setFillColor(edge.getColor());
        double thickness = Math.min(edgeSource.getRadius(), edgeTarget.getRadius());
        if (forcedLineThickness == 0) {
            thickness = 1;
        } else if (forcedLineThickness > 0) {
            thickness = forcedLineThickness;
        }
        roi.setStrokeWidth(thickness);
        roi.setName(edge.getUuid().toString());
        roi.setPosition(c + 1, z + 1, t + 1);
        return roi;
    }

    public void removeDuplicateVertices(boolean onlySameComponent) {
        Multimap<Point3d, FilamentVertex> multimap = groupVerticesByLocation();
        Map<FilamentVertex, Integer> componentIds;
        if (onlySameComponent) {
            componentIds = findComponentIds();
        } else {
            componentIds = null;
        }

        for (Point3d location : multimap.keySet()) {
            Collection<FilamentVertex> vertices = multimap.get(location);
            if (onlySameComponent) {
                Map<Integer, List<FilamentVertex>> groups = vertices.stream().collect(Collectors.groupingBy(componentIds::get));
                for (List<FilamentVertex> vertexList : groups.values()) {
                    mergeVertices(vertexList);
                }
            } else {
                mergeVertices(vertices);
            }
        }
    }

    public void mergeVertices(Collection<FilamentVertex> vertices) {
        if (vertices.size() > 1) {
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
        if (!Objects.equals(source, target)) {
            addEdge(source, target);
        }
    }

    public void addEdgeIgnoreLoops(FilamentVertex source, FilamentVertex target, FilamentEdge edge) {
        if (!Objects.equals(source, target)) {
            addEdge(source, target, edge);
        }
    }

    public void removeSelfEdges() {
        for (FilamentEdge edge : ImmutableList.copyOf(edgeSet())) {
            FilamentVertex edgeSource = getEdgeSource(edge);
            FilamentVertex edgeTarget = getEdgeTarget(edge);
            if (edgeSource == edgeTarget) {
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
        tableData.addNumericColumn("vsx");
        tableData.addNumericColumn("vsy");
        tableData.addNumericColumn("vsz");
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
        target.put(prefix + "vsx", vertex.getPhysicalVoxelSizeX().toString());
        target.put(prefix + "vsy", vertex.getPhysicalVoxelSizeY().toString());
        target.put(prefix + "vsz", vertex.getPhysicalVoxelSizeZ().toString());
        for (Map.Entry<String, String> entry : vertex.getMetadata().entrySet()) {
            target.put(prefix + entry.getKey(), StringUtils.tryParseDoubleOrReturnString(entry.getValue()));
        }
        for (Map.Entry<String, Double> entry : vertex.getValueBackups().entrySet()) {
            target.put(prefix + ".value." + entry.getKey(), entry.getValue());
        }
    }

    public ResultsTableData measureEdges() {
        String unit = getConsensusPhysicalSizeUnit();
        ResultsTableData tableData = new ResultsTableData();
        tableData.addStringColumn("uuid");
        tableData.addStringColumn("color");
        tableData.addNumericColumn("length");
        tableData.addNumericColumn("ulength");
        Map<String, Object> rowData = new LinkedHashMap<>();
        for (FilamentEdge edge : edgeSet()) {
            rowData.clear();
            measureEdge(edge, rowData, "", unit);
            tableData.addRow(rowData);
        }
        return tableData;
    }

    /**
     * Measures an edge
     *
     * @param edge   the edge
     * @param target the map where results will be stored
     * @param prefix the prefix for the map keys
     * @param unit   the unit for physical sizes
     */
    public void measureEdge(FilamentEdge edge, Map<String, Object> target, String prefix, String unit) {
        target.put(prefix + "uuid", edge.getUuid());
        target.put(prefix + "color", ColorUtils.colorToHexString(edge.getColor()));
        target.put(prefix + "length", getEdgeLength(edge, false, Quantity.UNIT_PIXELS));
        target.put(prefix + "ulength", getEdgeLength(edge, true, unit));
        target.put(prefix + "unit", unit);
        for (Map.Entry<String, String> entry : edge.getMetadata().entrySet()) {
            target.put(prefix + entry.getKey(), StringUtils.tryParseDoubleOrReturnString(entry.getValue()));
        }
        measureVertex(getEdgeSource(edge), target, prefix + "source.");
        measureVertex(getEdgeTarget(edge), target, prefix + "target.");
    }

    /**
     * Measures an edge that might not exist
     *
     * @param sourceVertex the source vertex
     * @param targetVertex the target vertex
     * @param target       the map where results will be stored
     * @param prefix       the prefix for the map keys
     * @param unit         the unit for physical sizes
     */
    public void measureNewEdge(FilamentVertex sourceVertex, FilamentVertex targetVertex, Map<String, Object> target, String prefix, String unit) {
        target.put(prefix + "uuid", "");
        target.put(prefix + "color", "");
        target.put(prefix + "length", getNewEdgeLength(sourceVertex, targetVertex, false, Quantity.UNIT_PIXELS));
        target.put(prefix + "ulength", getNewEdgeLength(sourceVertex, targetVertex, true, unit));
        target.put(prefix + "unit", unit);
        measureVertex(sourceVertex, target, prefix + "source.");
        measureVertex(targetVertex, target, prefix + "target.");
    }

    /**
     * Gets the length of an edge in the specified unit
     *
     * @param edge             the edge
     * @param usePhysicalSizes if the length should be returned in pixels or in the physical size
     * @param unit             the unit (any supported by {@link org.hkijena.jipipe.plugins.parameters.library.quantities.Quantity})
     * @return the length of the edge
     */
    public double getEdgeLength(FilamentEdge edge, boolean usePhysicalSizes, String unit) {
        FilamentVertex edgeSource = getEdgeSource(edge);
        FilamentVertex edgeTarget = getEdgeTarget(edge);
        if (!usePhysicalSizes || Quantity.isPixelsUnit(unit)) {
            return edgeSource.getSpatialLocation().distanceTo(edgeTarget.getSpatialLocation());
        } else {
            Vector3d sourceLocation = edgeSource.getSpatialLocationInUnit(unit);
            Vector3d targetLocation = edgeTarget.getSpatialLocationInUnit(unit);
            targetLocation.sub(sourceLocation);
            return targetLocation.length();
        }
    }

    /**
     * Gets the length of an edge that might not exist yet in the specified unit
     *
     * @param edgeSource       the source vertex
     * @param edgeTarget       the target vertex
     * @param usePhysicalSizes if the length should be returned in pixels or in the physical size
     * @param unit             the unit (any supported by {@link org.hkijena.jipipe.plugins.parameters.library.quantities.Quantity})
     * @return the length of the edge
     */
    public double getNewEdgeLength(FilamentVertex edgeSource, FilamentVertex edgeTarget, boolean usePhysicalSizes, String unit) {
        if (!usePhysicalSizes || Quantity.isPixelsUnit(unit)) {
            return edgeSource.getSpatialLocation().distanceTo(edgeTarget.getSpatialLocation());
        } else {
            Vector3d sourceLocation = edgeSource.getSpatialLocationInUnit(unit);
            Vector3d targetLocation = edgeTarget.getSpatialLocationInUnit(unit);
            targetLocation.sub(sourceLocation);
            return targetLocation.length();
        }
    }

    /**
     * Finds the common unit within the whole vertex set.
     * Returns 'pixels' if units are inconsistent or other error conditions happen
     *
     * @return the consensus unit
     */
    public String getConsensusPhysicalSizeUnit() {
        if (isEmpty()) {
            return Quantity.UNIT_PIXELS;
        }
        String unit = null;
        for (FilamentVertex vertex : vertexSet()) {
            String vertexUnit = vertex.getConsensusPhysicalSizeUnit();
            if (unit == null) {
                unit = vertexUnit;
            } else if (!Objects.equals(unit, vertexUnit)) {
                return Quantity.UNIT_PIXELS;
            }
        }
        return unit;
    }

    public Multimap<Point3d, FilamentVertex> groupVerticesByLocation() {
        Multimap<Point3d, FilamentVertex> multimap = HashMultimap.create();
        for (FilamentVertex vertex : vertexSet()) {
            multimap.put(vertex.getSpatialLocation(), vertex);
        }
        return multimap;
    }

    public void smooth(double factorX, double factorY, double factorZ, boolean enforceSameComponent, JIPipeExpressionParameter locationMergingFunction) {
        // Backup
        Map<FilamentVertex, Point3d> locationMap = new IdentityHashMap<>();
        for (FilamentVertex vertex : vertexSet()) {
            locationMap.put(vertex, new Point3d(vertex.getSpatialLocation()));
        }
        Map<FilamentVertex, Integer> componentIds;
        if (enforceSameComponent) {
            componentIds = findComponentIds();
        } else {
            componentIds = null;
        }

        // Downscale
        for (FilamentVertex vertex : vertexSet()) {
            Point3d centroid = vertex.getSpatialLocation();
            if (factorX > 0) {
                centroid.setX((int) Math.round(centroid.getX() / factorX));
            }
            if (factorY > 0) {
                centroid.setY((int) Math.round(centroid.getY() / factorY));
            }
            if (factorZ > 0) {
                centroid.setZ((int) Math.round(centroid.getZ() / factorZ));
            }
        }

        // Remove duplicates
        removeDuplicateVertices(enforceSameComponent);

        // Group vertices by location and calculate a new centroid
        Multimap<Point3d, FilamentVertex> multimap = groupVerticesByLocation();
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        for (FilamentVertex vertex : vertexSet()) {
            // Restore original
            vertex.setSpatialLocation(locationMap.get(vertex));
            Collection<FilamentVertex> group = multimap.get(vertex.getSpatialLocation());

            if (group.size() > 1) {
                if (enforceSameComponent) {
                    int component = componentIds.get(vertex);
                    Set<FilamentVertex> group2 = new HashSet<>();
                    for (FilamentVertex item : group) {
                        if (componentIds.get(item) == component) {
                            group2.add(item);
                        }
                    }

                    // Workaround for IDK why
                    group2.add(vertex);

                    calculateNewVertexLocation(factorX, factorY, factorZ, locationMergingFunction, locationMap, variables, vertex, group2);
                } else {
                    calculateNewVertexLocation(factorX, factorY, factorZ, locationMergingFunction, locationMap, variables, vertex, group);
                }
            }
        }

        // Cleanup
        removeSelfEdges();
    }

    public boolean isEmpty() {
        return vertexSet().isEmpty();
    }

    public Rectangle getBoundsXY() {
        double minX = Integer.MAX_VALUE;
        double maxX = Integer.MIN_VALUE;
        double minY = Integer.MAX_VALUE;
        double maxY = Integer.MIN_VALUE;
        for (FilamentVertex vertex : vertexSet()) {
            minX = Math.min(minX, vertex.getSpatialLocation().getX());
            maxX = Math.max(maxX, vertex.getSpatialLocation().getX());
            minY = Math.min(minY, vertex.getSpatialLocation().getY());
            maxY = Math.max(maxY, vertex.getSpatialLocation().getY());
        }
        return new Rectangle((int) minX, (int) minY, (int) (maxX - minX), (int) (maxY - minY));
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
            for (FilamentVertex vertex : connectedSet) {
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
                if (removeInX) {
                    double xMin = vertex.getXMin(useThickness);
                    double xMax = vertex.getXMax(useThickness);
                    if (xMin <= borderDistance) {
                        found = true;
                        break;
                    }
                    if (xMax >= reference.getWidth() - borderDistance - 1) {
                        found = true;
                        break;
                    }
                }
                if (removeInY) {
                    double yMin = vertex.getYMin(useThickness);
                    double yMax = vertex.getYMax(useThickness);
                    if (yMin <= borderDistance) {
                        found = true;
                        break;
                    }
                    if (yMax >= reference.getHeight() - borderDistance - 1) {
                        found = true;
                        break;
                    }
                }
                if (removeInZ) {
                    double zMin = vertex.getZMin(useThickness);
                    double zMax = vertex.getZMax(useThickness);
                    if (zMin <= borderDistance) {
                        found = true;
                        break;
                    }
                    if (zMax >= reference.getNSlices() - borderDistance - 1) {
                        found = true;
                        break;
                    }
                }
            }
            if (found) {
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
                if (degreeOf(vertex) == 2) {
                    List<FilamentVertex> neighbors = Graphs.neighborListOf(this, vertex);
                    addEdge(neighbors.get(0), neighbors.get(1));
                    removeVertex(vertex);
                    updated = true;
                }
            }
        }
        while (updated);
    }

    /**
     * Returns a shallow copy (the vertices and edges are not copied)
     *
     * @return the shallow copy
     */
    public Filaments3DGraphData shallowCopy() {
        Filaments3DGraphData copy = new Filaments3DGraphData();
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
     *
     * @param vertices the vertices
     * @return the shallow copy
     */
    public Filaments3DGraphData extractShallowCopy(Set<FilamentVertex> vertices) {
        Filaments3DGraphData copy = new Filaments3DGraphData();
        for (FilamentVertex vertex : vertexSet()) {
            if (vertices.contains(vertex)) {
                copy.addVertex(vertex);
            }
        }
        for (FilamentEdge edge : edgeSet()) {
            FilamentVertex edgeSource = getEdgeSource(edge);
            FilamentVertex edgeTarget = getEdgeTarget(edge);
            if (vertices.contains(edgeSource) && vertices.contains(edgeTarget)) {
                copy.addEdge(edgeSource, edgeTarget, edge);
            }
        }
        return copy;
    }

    /**
     * Extracts a deep copy of the nodes end edges that only contains the selected vertices
     *
     * @param vertices the vertices
     * @return the shallow copy
     */
    public Filaments3DGraphData extractDeepCopy(Set<FilamentVertex> vertices) {
        Filaments3DGraphData copy = new Filaments3DGraphData();
        Map<FilamentVertex, FilamentVertex> vertexMap = new IdentityHashMap<>();
        for (FilamentVertex vertex : vertexSet()) {
            if (vertices.contains(vertex)) {
                FilamentVertex cv = new FilamentVertex(vertex);
                copy.addVertex(cv);
                vertexMap.put(vertex, cv);
            }
        }
        for (FilamentEdge edge : edgeSet()) {
            FilamentVertex edgeSource = vertexMap.get(getEdgeSource(edge));
            FilamentVertex edgeTarget = vertexMap.get(getEdgeTarget(edge));
            if (edgeSource != null && edgeTarget != null) {
                copy.addEdge(edgeSource, edgeTarget, edge);
            }
        }
        return copy;
    }

    public ConnectivityInspector<FilamentVertex, FilamentEdge> getConnectivityInspector() {
        return new ConnectivityInspector<>(this);
    }

    public ResultsTableData measureComponents() {
        ResultsTableData measurements = new ResultsTableData();
        ConnectivityInspector<FilamentVertex, FilamentEdge> connectivityInspector = getConnectivityInspector();
        List<Set<FilamentVertex>> connectedSets = connectivityInspector.connectedSets();
        String consensusPhysicalSizeUnit = getConsensusPhysicalSizeUnit();
        for (int i = 0; i < connectedSets.size(); i++) {
            Set<FilamentVertex> vertices = connectedSets.get(i);
            measureComponent(measurements, consensusPhysicalSizeUnit, vertices);
        }
        return measurements;
    }

    public void measureComponent(ResultsTableData measurements, String consensusPhysicalSizeUnit, Set<FilamentVertex> vertices) {
        Set<FilamentEdge> edges = edgesOf(vertices);

        // Vertex stats
        double minVertexRadiusPixels = Double.POSITIVE_INFINITY;
        double maxVertexRadiusPixels = Double.NEGATIVE_INFINITY;
        double sumVertexRadiusPixels = 0;
        double minVertexRadiusUnit = Double.POSITIVE_INFINITY;
        double maxVertexRadiusUnit = Double.NEGATIVE_INFINITY;
        double sumVertexRadiusUnit = 0;
        double minVertexIntensity = Double.POSITIVE_INFINITY;
        double maxVertexIntensity = Double.NEGATIVE_INFINITY;
        double sumVertexIntensity = 0;
        for (FilamentVertex vertex : vertices) {
            minVertexRadiusPixels = Math.min(vertex.getRadius(), minVertexRadiusPixels);
            maxVertexRadiusPixels = Math.max(vertex.getRadius(), maxVertexRadiusPixels);
            sumVertexRadiusPixels += vertex.getRadius();
            double radiusInUnit = vertex.getMaxRadiusInUnit(consensusPhysicalSizeUnit);
            minVertexRadiusUnit = Math.min(radiusInUnit, minVertexRadiusPixels);
            maxVertexRadiusUnit = Math.max(radiusInUnit, maxVertexRadiusPixels);
            sumVertexRadiusUnit += radiusInUnit;
            minVertexIntensity = Math.min(vertex.getValue(), minVertexIntensity);
            maxVertexIntensity = Math.max(vertex.getValue(), maxVertexIntensity);
            sumVertexIntensity += vertex.getValue();
        }

        // Edge stats (pixels)
        double minEdgeLengthPixels = Double.POSITIVE_INFINITY;
        double maxEdgeLengthPixels = Double.NEGATIVE_INFINITY;
        double sumEdgeLengthPixels = 0;

        for (FilamentEdge edge : edges) {
            double length = getEdgeLength(edge, false, Quantity.UNIT_PIXELS);
            minEdgeLengthPixels = Math.min(minEdgeLengthPixels, length);
            maxEdgeLengthPixels = Math.max(maxEdgeLengthPixels, length);
            sumEdgeLengthPixels += length;
        }

        double sumEdgeLengthCorrectedPixels = sumEdgeLengthPixels;
        for (FilamentVertex vertex : vertices) {
            int degree = degreeOf(vertex);
            if (degree == 0) {
                // Count radius 2 times
                sumEdgeLengthCorrectedPixels += vertex.getRadius() * 2;
            } else if (degree == 1) {
                // Count radius 1 time
                sumEdgeLengthCorrectedPixels += vertex.getRadius();
            }
        }

        // Edge stats (physical)
        double minEdgeLengthUnit = Double.POSITIVE_INFINITY;
        double maxEdgeLengthUnit = Double.NEGATIVE_INFINITY;
        double sumEdgeLengthUnit = 0;

        for (FilamentEdge edge : edges) {
            double length = getEdgeLength(edge, true, consensusPhysicalSizeUnit);
            minEdgeLengthUnit = Math.min(minEdgeLengthUnit, length);
            maxEdgeLengthUnit = Math.max(maxEdgeLengthUnit, length);
            sumEdgeLengthUnit += length;
        }

        double sumEdgeLengthCorrectedUnit = sumEdgeLengthUnit;
        for (FilamentVertex vertex : vertices) {
            int degree = degreeOf(vertex);
            if (degree == 0) {
                // Count radius 2 times
                sumEdgeLengthCorrectedUnit += vertex.getMaxRadiusInUnit(consensusPhysicalSizeUnit) * 2;
            } else if (degree == 1) {
                // Count radius 1 time
                sumEdgeLengthCorrectedUnit += vertex.getMaxRadiusInUnit(consensusPhysicalSizeUnit);
            }
        }

        // Make a simplified copy and calculate the simplified edge lengths
        Filaments3DGraphData simplified = extractShallowCopy(vertices);
        simplified.simplify();
        double simplifiedSumEdgeLengthPixels = 0;
        double simplifiedSumEdgeLengthUnit = 0;
        for (FilamentEdge edge : simplified.edgeSet()) {
            simplifiedSumEdgeLengthPixels += simplified.getEdgeLength(edge, false, Quantity.UNIT_PIXELS);
            simplifiedSumEdgeLengthUnit += simplified.getEdgeLength(edge, true, consensusPhysicalSizeUnit);
        }

        double simplifiedSumEdgeLengthCorrectedPixels = simplifiedSumEdgeLengthPixels;
        double simplifiedSumEdgeLengthCorrectedUnit = simplifiedSumEdgeLengthUnit;
        for (FilamentVertex vertex : simplified.vertexSet()) {
            int degree = simplified.degreeOf(vertex);
            if (degree == 0) {
                // Count radius 2 times
                simplifiedSumEdgeLengthCorrectedPixels += vertex.getRadius() * 2;
                simplifiedSumEdgeLengthCorrectedUnit += vertex.getMaxRadiusInUnit(consensusPhysicalSizeUnit) * 2;
            } else if (degree == 1) {
                // Count radius 1 time
                simplifiedSumEdgeLengthCorrectedPixels += vertex.getRadius();
                simplifiedSumEdgeLengthCorrectedUnit += vertex.getMaxRadiusInUnit(consensusPhysicalSizeUnit);
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

        // Sum X, Y, Z, C, T
        double sumX = 0;
        double sumY = 0;
        double sumZ = 0;
        double sumC = 0;
        double sumT = 0;

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
            sumX += vertex.getSpatialLocation().getX();
            sumY += vertex.getSpatialLocation().getY();
            sumZ += vertex.getSpatialLocation().getZ();
            sumC += vertex.getNonSpatialLocation().getChannel();
            sumT += vertex.getNonSpatialLocation().getFrame();
        }

        double centroidX = sumX / vertices.size();
        double centroidY = sumY / vertices.size();
        double centroidZ = sumZ / vertices.size();
        double centroidC = sumC / vertices.size();
        double centroidT = sumT / vertices.size();

        int row = measurements.addRow();
        measurements.setValueAt(row, row, "Component");
        measurements.setValueAt(vertices.size(), row, "numVertices");
        measurements.setValueAt(edges.size(), row, "numEdges");

        measurements.setValueAt(sumEdgeLengthPixels, row, "lengthPixels");
        measurements.setValueAt(sumEdgeLengthUnit, row, "lengthUnit");
        measurements.setValueAt(sumEdgeLengthCorrectedPixels, row, "lengthPixelsRadiusCorrected");
        measurements.setValueAt(sumEdgeLengthCorrectedUnit, row, "lengthUnitRadiusCorrected");
        measurements.setValueAt(simplifiedSumEdgeLengthPixels, row, "simplifiedLengthPixels");
        measurements.setValueAt(simplifiedSumEdgeLengthUnit, row, "simplifiedLengthUnit");
        measurements.setValueAt(simplifiedSumEdgeLengthCorrectedPixels, row, "simplifiedLengthPixelsRadiusCorrected");
        measurements.setValueAt(simplifiedSumEdgeLengthCorrectedUnit, row, "simplifiedLengthUnitRadiusCorrected");

        measurements.setValueAt(simplifiedSumEdgeLengthPixels / sumEdgeLengthPixels, row, "confinementRatio");
        measurements.setValueAt(simplifiedSumEdgeLengthCorrectedPixels / sumEdgeLengthCorrectedPixels, row, "confinementRatioRadiusCorrected");
        measurements.setValueAt(vertices.stream().filter(vertex -> degreeOf(vertex) == 0).count(), row, "numVerticesWithDegree0");
        measurements.setValueAt(vertices.stream().filter(vertex -> degreeOf(vertex) == 1).count(), row, "numVerticesWithDegree1");
        measurements.setValueAt(vertices.stream().filter(vertex -> degreeOf(vertex) == 2).count(), row, "numVerticesWithDegree2");
        measurements.setValueAt(vertices.stream().filter(vertex -> degreeOf(vertex) == 3).count(), row, "numVerticesWithDegree3");
        measurements.setValueAt(vertices.stream().filter(vertex -> degreeOf(vertex) == 4).count(), row, "numVerticesWithDegree4");
        measurements.setValueAt(vertices.stream().filter(vertex -> degreeOf(vertex) == 5).count(), row, "numVerticesWithDegree5");
        measurements.setValueAt(vertices.stream().filter(vertex -> degreeOf(vertex) > 5).count(), row, "numVerticesWithDegreeMoreThan5");
        measurements.setValueAt(vertices.stream().map(vertex -> degreeOf(vertex)).max(Comparator.naturalOrder()).orElse(0), row, "maxDegree");
        measurements.setValueAt(vertices.stream().map(vertex -> degreeOf(vertex)).min(Comparator.naturalOrder()).orElse(0), row, "minDegree");
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

        measurements.setValueAt(centroidX, row, "centroidX");
        measurements.setValueAt(centroidY, row, "centroidY");
        measurements.setValueAt(centroidZ, row, "centroidZ");
        measurements.setValueAt(centroidC, row, "centroidC");
        measurements.setValueAt(centroidT, row, "centroidT");

        measurements.setValueAt(minEdgeLengthPixels, row, "minEdgeLengthPixels");
        measurements.setValueAt(minEdgeLengthUnit, row, "minEdgeLengthUnit");
        measurements.setValueAt(maxEdgeLengthPixels, row, "maxEdgeLengthPixels");
        measurements.setValueAt(maxEdgeLengthUnit, row, "maxEdgeLengthUnit");
        measurements.setValueAt(sumEdgeLengthPixels / edges.size(), row, "avgEdgeLengthPixels");
        measurements.setValueAt(sumEdgeLengthUnit / edges.size(), row, "avgEdgeLengthUnit");

        measurements.setValueAt(minVertexRadiusPixels, row, "minVertexRadiusPixels");
        measurements.setValueAt(minVertexRadiusUnit, row, "minVertexRadiusUnit");
        measurements.setValueAt(maxVertexRadiusPixels, row, "maxVertexRadiusPixels");
        measurements.setValueAt(maxVertexRadiusUnit, row, "maxVertexRadiusUnit");
        measurements.setValueAt(sumVertexRadiusPixels / vertices.size(), row, "avgVertexRadiusPixels");
        measurements.setValueAt(sumVertexRadiusUnit / vertices.size(), row, "avgVertexRadiusUnit");

        measurements.setValueAt(minVertexIntensity, row, "minVertexValue");
        measurements.setValueAt(maxVertexIntensity, row, "maxVertexValue");
        measurements.setValueAt(sumVertexIntensity / vertices.size(), row, "avgVertexValue");

        measurements.setValueAt(consensusPhysicalSizeUnit, row, "physicalSizeUnit");
    }

    private Set<FilamentEdge> edgesOf(Set<FilamentVertex> vertices) {
        Set<FilamentEdge> edges = new HashSet<>();
        for (FilamentVertex vertex : vertices) {
            edges.addAll(edgesOf(vertex));
        }
        return edges;
    }

    public ImagePlus createBlankCanvas(String title, BitDepth bitDepth) {
        return createBlankCanvas(title, bitDepth.getBitDepth());
    }

    public boolean is2D() {
        for (FilamentVertex vertex : vertexSet()) {
            if (vertex.getPhysicalVoxelSizeZ().getValue() != 0) {
                return false;
            }
        }
        return true;
    }

    public ImagePlus createBlankCanvas(String title, int bitDepth) {
        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;
        int maxC = 0;
        int maxT = 0;

        if (is2D()) {
            for (FilamentVertex vertex : vertexSet()) {
                maxX = (int) Math.max(maxX, vertex.getXMax(true));
                maxY = (int) Math.max(maxY, vertex.getYMax(true));
                maxZ = (int) Math.max(maxZ, vertex.getZMax(true));
                maxC = Math.max(maxC, vertex.getNonSpatialLocation().getChannel());
                maxT = Math.max(maxT, vertex.getNonSpatialLocation().getFrame());
            }
        } else {
            for (FilamentVertex vertex : vertexSet()) {
                maxX = (int) Math.max(maxX, vertex.getXMax(true));
                maxY = (int) Math.max(maxY, vertex.getYMax(true));
                maxZ = (int) Math.max(maxZ, vertex.getSpatialLocation().getZ());
                maxC = Math.max(maxC, vertex.getNonSpatialLocation().getChannel());
                maxT = Math.max(maxT, vertex.getNonSpatialLocation().getFrame());
            }
        }

        return IJ.createHyperStack(title, maxX + 1, maxY + 1, maxC + 1, maxZ + 1, maxT + 1, bitDepth);
    }

    public ImagePlus toLabels(ImagePlus referenceImage, boolean withEdges, boolean withVertices, int forcedLineThickness, int forcedVertexRadius, JIPipeProgressInfo progressInfo) {
        ROI3DListData roi3D = toRoi3D(withEdges, withVertices, forcedLineThickness, forcedVertexRadius, progressInfo);
        return roi3D.toLabels(referenceImage, progressInfo);
    }

    public ImagePlus toMask(ImagePlus referenceImage, boolean withEdges, boolean withVertices, int forcedLineThickness, int forcedVertexRadius, JIPipeProgressInfo progressInfo) {
        ROI3DListData roi3D = toRoi3D(withEdges, withVertices, forcedLineThickness, forcedVertexRadius, progressInfo);
        return roi3D.toMask(referenceImage, progressInfo);
    }

    public ROI3DListData toRoi3D(boolean withEdges, boolean withVertices, int forcedLineThickness, int forcedVertexRadius, JIPipeProgressInfo progressInfo) {
        ConnectivityInspector<FilamentVertex, FilamentEdge> connectivityInspector = getConnectivityInspector();
        List<Set<FilamentVertex>> connectedSets = connectivityInspector.connectedSets();

        ROI3DListData result = new ROI3DListData();

        ImagePlus blankCanvas = createBlankCanvas("", 8);
        ImageJIterationUtils.forEachIndexedCTStack(blankCanvas, (imp, index, ctProgress) -> {
            ExtendedObjectCreator3D objectCreator3D = new ExtendedObjectCreator3D(ImageHandler.wrap(imp));
            for (int i = 0; i < connectedSets.size(); i++) {
                if (ctProgress.isCancelled())
                    return;
                Set<FilamentVertex> connectedSet = connectedSets.get(i);
                ctProgress.resolveAndLog("Component", i, connectedSets.size());

                boolean found = false;
                for (FilamentVertex vertex : connectedSet) {
                    if (vertex.getNonSpatialLocation().getFrame() == index.getT() || vertex.getNonSpatialLocation().getChannel() == index.getC()) {
                        found = true;
                        break;
                    }
                }

                if (!found)
                    continue;

                if (withEdges) {
                    objectCreator3D.reset();
                    Filaments3DGraphData extracted = extractShallowCopy(connectedSet);
                    for (FilamentEdge edge : extracted.edgeSet()) {
                        if (ctProgress.isCancelled())
                            return;
                        FilamentVertex source = extracted.getEdgeSource(edge);
                        FilamentVertex target = extracted.getEdgeTarget(edge);
                        if (forcedLineThickness >= 0) {
                            objectCreator3D.createLine((int) source.getSpatialLocation().getX(), (int) source.getSpatialLocation().getY(), (int) source.getSpatialLocation().getZ(),
                                    (int) target.getSpatialLocation().getX(), (int) target.getSpatialLocation().getY(), (int) target.getSpatialLocation().getZ(),
                                    1,
                                    forcedLineThickness);
                        } else {
                            int sourceRadius = (int) source.getRadius();
                            int targetRadius = (int) target.getRadius();
                            if (forcedVertexRadius > 0) {
                                sourceRadius = forcedVertexRadius;
                                targetRadius = forcedVertexRadius;
                            }
                            objectCreator3D.createLine((int) source.getSpatialLocation().getX(), (int) source.getSpatialLocation().getY(), (int) source.getSpatialLocation().getZ(),
                                    (int) target.getSpatialLocation().getX(), (int) target.getSpatialLocation().getY(), (int) target.getSpatialLocation().getZ(),
                                    1,
                                    sourceRadius,
                                    targetRadius);
                        }
                    }

                    ROI3D roi3D = new ROI3D(objectCreator3D.getObject3DVoxels(1));
                    roi3D.setFrame(index.getT() + 1);
                    roi3D.setChannel(index.getC() + 1);
                    result.add(roi3D);
                }
                if (withVertices) {
                    objectCreator3D.reset();
                    for (FilamentVertex vertex : connectedSet) {
                        if (ctProgress.isCancelled())
                            return;
                        int radius = (int) vertex.getRadius();
                        if (forcedVertexRadius > 0) {
                            radius = forcedVertexRadius;
                        }
                        objectCreator3D.createSphere(vertex.getSpatialLocation().getX(),
                                vertex.getSpatialLocation().getY(),
                                vertex.getSpatialLocation().getZ(),
                                vertex.getRadius(),
                                radius,
                                false);
                    }

                    ROI3D roi3D = new ROI3D(objectCreator3D.getObject3DVoxels(1));
                    roi3D.setFrame(index.getT() + 1);
                    roi3D.setChannel(index.getC() + 1);
                    result.add(roi3D);
                }
            }
        }, progressInfo);

        return result;
    }

    public Color getAverageVertexColor() {
        double r = 0;
        double g = 0;
        double b = 0;
        for (FilamentVertex vertex : vertexSet()) {
            r += vertex.getColor().getRed();
            g += vertex.getColor().getGreen();
            b += vertex.getColor().getBlue();
        }
        return new Color((int) (r / vertexSet().size()),
                (int) (g / vertexSet().size()),
                (int) (b / vertexSet().size()));
    }

    public Color getAverageEdgeColor() {
        double r = 0;
        double g = 0;
        double b = 0;
        for (FilamentEdge edge : edgeSet()) {
            r += edge.getColor().getRed();
            g += edge.getColor().getGreen();
            b += edge.getColor().getBlue();
        }
        return new Color((int) (r / edgeSet().size()),
                (int) (g / edgeSet().size()),
                (int) (b / edgeSet().size()));
    }

    public Scene3DGroupNode toScene3D(boolean withVertices, boolean withEdges, boolean physicalSizes, Quantity.LengthUnit meshLengthUnit, boolean forceMeshLengthUnit, float overrideVertexRadius, float overrideEdgeRadius, Color overrideVertexColor, Color overrideEdgeColor, String name) {
        Scene3DGroupNode componentGroup = new Scene3DGroupNode();
        componentGroup.setName(name);

        String consensusUnit = getConsensusPhysicalSizeUnit();

        if (withVertices) {
            Scene3DGroupNode verticesGroup = new Scene3DGroupNode();
            verticesGroup.setName("Vertices");
            componentGroup.addChild(verticesGroup);

            for (FilamentVertex vertex : vertexSet()) {
                Vector3d location = getFinalVertexLocationFor3DExport(physicalSizes, meshLengthUnit, forceMeshLengthUnit, consensusUnit, vertex);

                Scene3DSphereGeometry geometry = new Scene3DSphereGeometry();
                geometry.setColor(overrideVertexColor != null ? overrideVertexColor : vertex.getColor());
                geometry.setRadiusX(getFinalVertexRadiusFor3DExport(physicalSizes, meshLengthUnit, forceMeshLengthUnit, consensusUnit, (float) vertex.getRadius(), vertex.getPhysicalVoxelSizeX()));
                geometry.setRadiusY(getFinalVertexRadiusFor3DExport(physicalSizes, meshLengthUnit, forceMeshLengthUnit, consensusUnit, (float) vertex.getRadius(), vertex.getPhysicalVoxelSizeY()));
                geometry.setRadiusZ(getFinalVertexRadiusFor3DExport(physicalSizes, meshLengthUnit, forceMeshLengthUnit, consensusUnit, (float) vertex.getRadius(), vertex.getPhysicalVoxelSizeZ()));
                geometry.setCenterX((float) location.x);
                geometry.setCenterY((float) location.y);
                geometry.setCenterZ((float) location.z);
                verticesGroup.addChild(geometry);
            }

        }
        if (withEdges) {
            Scene3DGroupNode edgesGroup = new Scene3DGroupNode();
            edgesGroup.setName("Edges");

            for (FilamentEdge edge : edgeSet()) {
                FilamentVertex source = getEdgeSource(edge);
                FilamentVertex target = getEdgeTarget(edge);

                float sourceRadius = (getFinalVertexRadiusFor3DExport(physicalSizes, meshLengthUnit, forceMeshLengthUnit, consensusUnit, (float) source.getRadius(), source.getPhysicalVoxelSizeX()) +
                        getFinalVertexRadiusFor3DExport(physicalSizes, meshLengthUnit, forceMeshLengthUnit, consensusUnit, (float) source.getRadius(), source.getPhysicalVoxelSizeY()) +
                        getFinalVertexRadiusFor3DExport(physicalSizes, meshLengthUnit, forceMeshLengthUnit, consensusUnit, (float) source.getRadius(), source.getPhysicalVoxelSizeY())) / 3;
                float targetRadius = (getFinalVertexRadiusFor3DExport(physicalSizes, meshLengthUnit, forceMeshLengthUnit, consensusUnit, (float) target.getRadius(), target.getPhysicalVoxelSizeX()) +
                        getFinalVertexRadiusFor3DExport(physicalSizes, meshLengthUnit, forceMeshLengthUnit, consensusUnit, (float) target.getRadius(), target.getPhysicalVoxelSizeY()) +
                        getFinalVertexRadiusFor3DExport(physicalSizes, meshLengthUnit, forceMeshLengthUnit, consensusUnit, (float) target.getRadius(), target.getPhysicalVoxelSizeY())) / 3;

                Vector3d sourceLocation = getFinalVertexLocationFor3DExport(physicalSizes, meshLengthUnit, forceMeshLengthUnit, consensusUnit, source);
                Vector3d targetLocation = getFinalVertexLocationFor3DExport(physicalSizes, meshLengthUnit, forceMeshLengthUnit, consensusUnit, target);

                Scene3DLineGeometry lineGeometry = new Scene3DLineGeometry();
                lineGeometry.getStart().setX((float) sourceLocation.x);
                lineGeometry.getStart().setY((float) sourceLocation.y);
                lineGeometry.getStart().setZ((float) sourceLocation.z);
                lineGeometry.getStart().setRadius(sourceRadius);
                lineGeometry.getEnd().setX((float) targetLocation.x);
                lineGeometry.getEnd().setY((float) targetLocation.y);
                lineGeometry.getEnd().setZ((float) targetLocation.z);
                lineGeometry.getEnd().setRadius(targetRadius);
                lineGeometry.setColor(edge.getColor());

                edgesGroup.addChild(lineGeometry);
            }


            componentGroup.addChild(edgesGroup);
        }

        return componentGroup;
    }

    private float getFinalVertexRadiusFor3DExport(boolean physicalSizes, Quantity.LengthUnit meshLengthUnit, boolean forceMeshLengthUnit, String consensusUnit, float radius, Quantity voxelSize) {
        float finalRadius;
        if (physicalSizes) {
            if (forceMeshLengthUnit) {
                finalRadius = (float) (voxelSize.convertTo(consensusUnit).convertTo(meshLengthUnit.name()).getValue() * radius);
            } else {
                finalRadius = (float) (voxelSize.convertTo(consensusUnit).getValue() * radius);
            }
        } else {
            finalRadius = radius;
        }
        return finalRadius;
    }

    public ImagePlus toLabels2(ImagePlus reference, boolean withEdges, boolean withVertices, int forcedLineThickness, int forcedVertexRadius, boolean ignoreC, boolean ignoreZ, boolean ignoreT, boolean hollowVertices, JIPipeProgressInfo progressInfo) {
        if (reference == null) {
            reference = createBlankCanvas("Image", BitDepth.Grayscale32f);
        } else {
            reference = ImageJUtils.newBlankOf(reference, BitDepth.Grayscale32f);
        }

        // Assign color map
        Map<FilamentVertex, Integer> vertexToLabelMapping = new HashMap<>();

        ConnectivityInspector<FilamentVertex, FilamentEdge> connectivityInspector = getConnectivityInspector();
        List<Set<FilamentVertex>> connectedSets = connectivityInspector.connectedSets();
        for (int i = 0; i < connectedSets.size(); i++) {
            Set<FilamentVertex> connectedSet = connectedSets.get(i);
            for (FilamentVertex vertex : connectedSet) {
                vertexToLabelMapping.put(vertex, i + 1);
            }
        }

        // Draw
        ImageJIterationUtils.forEachIndexedZCTSlice(reference, (ip, index) -> {

            final int z = index.getZ();
            final int c = index.getC();
            final int t = index.getT();

            if (withEdges) {
                for (FilamentEdge edge : edgeSet()) {
                    FilamentVertex source = getEdgeSource(edge);
                    FilamentVertex target = getEdgeTarget(edge);
                    int label = vertexToLabelMapping.get(source); // Sufficient due to connected set

                    if (source.getNonSpatialLocation().getChannel() != c && !ignoreC)
                        continue;
                    if (source.getNonSpatialLocation().getFrame() != t && !ignoreT)
                        continue;
                    if (target.getNonSpatialLocation().getChannel() != c && !ignoreC)
                        continue;
                    if (target.getNonSpatialLocation().getFrame() != t && !ignoreT)
                        continue;

                    double sourceRadius = source.getRadius();
                    double targetRadius = target.getRadius();
                    if (forcedVertexRadius >= 0) {
                        sourceRadius = forcedVertexRadius;
                        targetRadius = forcedVertexRadius;
                    }
                    if (forcedLineThickness >= 0) {
                        sourceRadius = forcedLineThickness;
                        targetRadius = forcedLineThickness;
                    }

                    drawLabelLineOnProcessor(source.getSpatialLocation().getX(), source.getSpatialLocation().getY(), source.getSpatialLocation().getZ(),
                            target.getSpatialLocation().getX(), target.getSpatialLocation().getY(), target.getSpatialLocation().getZ(),
                            label,
                            sourceRadius,
                            targetRadius,
                            ip,
                            z);
                }
            }
            if (withVertices) {
                for (FilamentVertex vertex : vertexSet()) {
//            if(vertex.getSpatialLocation().getZ() != z && !ignoreZ)
//                continue;
                    if (vertex.getNonSpatialLocation().getChannel() != c && !ignoreC)
                        continue;
                    if (vertex.getNonSpatialLocation().getFrame() != t && !ignoreT)
                        continue;
                    int label = vertexToLabelMapping.get(vertex);
                    int radius = (int) vertex.getRadius();

                    if (forcedVertexRadius >= 0)
                        radius = forcedVertexRadius;

                    drawLabelBallOnProcessor((int) vertex.getSpatialLocation().getX(), (int) vertex.getSpatialLocation().getY(), (int) vertex.getSpatialLocation().getZ(), label, radius, hollowVertices, ip, z);
                }
            }
        }, progressInfo);

        return reference;
    }

    @Override
    public Set<Class<? extends JIPipeDesktopLegacyImageViewerPlugin>> getRequiredLegacyImageViewerPlugins() {
        return Collections.singleton(FilamentsManagerPlugin2D.class);
    }

    @Override
    public List<Path> exportOverlayToNapari(ImagePlus imp, Path outputDirectory, String prefix, JIPipeProgressInfo progressInfo) {
        if (!isEmpty()) {
            Path outputFile = outputDirectory.resolve(prefix + "_filaments.tif");

            progressInfo.log("Exporting " + this);
            FilamentsDrawer filamentsDrawer = new FilamentsDrawer();
            ImagePlus rendered = ImageJUtils.newBlankOf(imp, BitDepth.ColorRGB);
            ImageJIterationUtils.forEachIndexedZCTSlice(rendered, (ip, index) -> {
                filamentsDrawer.drawFilamentsOnProcessor(this, (ColorProcessor) ip, index.getZ(), index.getC(), index.getT());
            }, progressInfo);
            IJ.saveAsTiff(rendered, outputFile.toString());

            return Collections.singletonList(outputFile);
        } else {
            return Collections.emptyList();
        }
    }
}
