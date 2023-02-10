package org.hkijena.jipipe.extensions.ijfilaments.datatypes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.ijfilaments.util.*;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.nio.json.JSONImporter;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Filaments", description = "Stores filaments as graph")
@JIPipeDataStorageDocumentation(humanReadableDescription = "A *.json file containing a JGraphT graph in its serialized form",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
@JsonSerialize(using = FilamentsDataSerializer.class)
@JsonDeserialize(using = FilamentsDataDeserializer.class)
public class FilamentsData  extends DefaultDirectedGraph<FilamentVertex, FilamentEdge> implements JIPipeData {

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

    }

    @Override
    public Component preview(int width, int height) {
        return JIPipeData.super.preview(width, height);
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
                                addEdge(referenceVertex, edgeTarget);
                            }
                            else {
                                addEdge(edgeSource, referenceVertex);
                            }
                        }
                        removeVertex(vertex);
                    }
                }
            }
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
}
