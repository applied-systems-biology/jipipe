package edu.kit.datamanager.ro_crate.reader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.context.CrateMetadataContext;
import edu.kit.datamanager.ro_crate.context.RoCrateMetadataContext;
import edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity;
import edu.kit.datamanager.ro_crate.entities.data.DataEntity;
import edu.kit.datamanager.ro_crate.entities.data.RootDataEntity;
import edu.kit.datamanager.ro_crate.special.IdentifierUtils;
import edu.kit.datamanager.ro_crate.special.JsonUtilFunctions;
import edu.kit.datamanager.ro_crate.validation.JsonSchemaValidation;
import edu.kit.datamanager.ro_crate.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This class allows reading crates from the outside into the library in order
 * to inspect or modify it.
 * <p>
 * The constructor takes a strategy to support different ways of importing the
 * crates. (from zip, folder, etc.).
 * <p>
 * The reader considers "hasPart" and "isPartOf" properties and considers all
 * entities (in-)directly connected to the root entity ("./") as DataEntities.
 *
 * @param <T> the type of the location parameter
 */
public class CrateReader<T> {

    private static final Logger logger = LoggerFactory.getLogger(CrateReader.class);

    /**
     * This is a private inner class that shall not be exposed. **Do not make it
     * public or protected.** It serves only the purpose of unsafe operations
     * while reading a crate and may be specific to this implementation.
     */
    private static class RoCrateUnsafe extends RoCrate {

        public void addDataEntityWithoutRootHasPart(DataEntity entity) {
            this.metadataContext.checkEntity(entity);
            this.roCratePayload.addDataEntity(entity);
        }
    }

    /**
     * If the number of JSON entities in the crate is larger than this number,
     * parallelization will be used.
     */
    private static final int PARALLELIZATION_THRESHOLD = 100;

    private static final String FILE_PREVIEW_FILES = "ro-crate-preview_files";
    private static final String FILE_PREVIEW_HTML = "ro-crate-preview.html";
    private static final String FILE_METADATA_JSON = "ro-crate-metadata.json";

    protected static final String SPECIFICATION_PREFIX = "https://w3id.org/ro/crate/";

    protected static final String PROP_ABOUT = "about";
    protected static final String PROP_CONTEXT = "@context";
    protected static final String PROP_CONFORMS_TO = "conformsTo";
    protected static final String PROP_GRAPH = "@graph";
    protected static final String PROP_HAS_PART = "hasPart";
    protected static final String PROP_ID = "@id";

    private final GenericReaderStrategy<T> strategy;

    public CrateReader(GenericReaderStrategy<T> strategy) {
        this.strategy = strategy;
    }

    /**
     * This function will read the location (using one of the specified
     * strategies) and then build the relation between the entities.
     *
     * @param location the location of the ro-crate to be read
     * @return the read RO-crate
     *
     * @throws IOException if the crate cannot be read
     */
    public RoCrate readCrate(T location) throws IOException {
        // get the ro-crate-metadata.json
        ObjectNode metadataJson = strategy.readMetadataJson(location);
        // get the content of the crate
        File files = strategy.readContent(location);

        // this set will contain the files that are associated with entities
        HashSet<String> usedFiles = new HashSet<>();
        usedFiles.add(files.toPath().resolve(FILE_METADATA_JSON).toFile().getPath());
        usedFiles.add(files.toPath().resolve(FILE_PREVIEW_HTML).toFile().getPath());
        usedFiles.add(files.toPath().resolve(FILE_PREVIEW_FILES).toFile().getPath());
        return rebuildCrate(metadataJson, files, usedFiles).markAsImported();
    }

    private RoCrate rebuildCrate(ObjectNode metadataJson, File files, HashSet<String> usedFiles) {
        Objects.requireNonNull(metadataJson,
                "metadataJson must not be null – did the strategy fail to locate 'ro-crate-metadata.json'?");
        Objects.requireNonNull(files,
                "files directory must not be null – check GenericReaderStrategy.readContent()");
        JsonNode context = metadataJson.get(PROP_CONTEXT);

        CrateMetadataContext crateContext = new RoCrateMetadataContext(context);
        RoCrateUnsafe crate = new RoCrateUnsafe();
        crate.setMetadataContext(crateContext);
        JsonNode graph = metadataJson.get(PROP_GRAPH);

        if (graph.isArray()) {
            moveRootEntitiesFromGraphToCrate(crate, (ArrayNode) graph);
            RootDataEntity root = crate.getRootDataEntity();
            if (root != null) {
                Set<String> dataEntityIds = getDataEntityIds(root, graph);
                for (JsonNode entityJson : graph) {
                    String eId = unpackId(entityJson);
                    if (dataEntityIds.contains(eId)) {
                        // data entity
                        DataEntity.DataEntityBuilder dataEntity = new DataEntity.DataEntityBuilder()
                                .setAllUnsafe(entityJson.deepCopy());

                        // Handle data entities with corresponding file
                        checkFolderHasFile(entityJson.get(PROP_ID).asText(), files).ifPresent(file -> {
                            usedFiles.add(file.getPath());
                            dataEntity.setLocationWithExceptions(file.toPath())
                                    .setId(file.getName());
                        });

                        crate.addDataEntityWithoutRootHasPart(dataEntity.build());
                    } else {
                        // contextual entity
                        crate.addContextualEntity(
                                new ContextualEntity.ContextualEntityBuilder()
                                        .setAllUnsafe(entityJson.deepCopy())
                                        .build());
                    }
                }
            }
        }

        Collection<File> untrackedFiles = Arrays.stream(
                Optional.ofNullable(files.listFiles()).orElse(new File[0]))
                .filter(f -> !usedFiles.contains(f.getPath()))
                .collect(Collectors.toSet());

        crate.setUntrackedFiles(untrackedFiles);
        Validator defaultValidation = new Validator(new JsonSchemaValidation());
        defaultValidation.validate(crate);
        return crate;
    }

    /**
     * Extracts graph connections from top to bottom.
     * <p>
     * Example: (connections.get(parent) -> children)
     *
     * @param graph the ArrayNode with all Entities.
     * @return the graph connections.
     */
    protected Map<String, Set<String>> makeEntityGraph(JsonNode graph) {
        Map<String, Set<String>> connections = new HashMap<>();

        Map<String, JsonNode> idToNodes = new HashMap<>();
        StreamSupport.stream(graph.spliterator(), false)
                .forEach(jsonNode -> idToNodes.put(unpackId(jsonNode), jsonNode));

        for (JsonNode entityNode : graph) {
            String currentId = unpackId(entityNode);
            StreamSupport.stream(entityNode.path("hasPart").spliterator(), false)
                    .map(this::unpackId)
                    .map(s -> idToNodes.getOrDefault(s, null))
                    .filter(Objects::nonNull)
                    .forEach(child -> connections.computeIfAbsent(currentId, key -> new HashSet<>())
                    .add(unpackId(child)));
            StreamSupport.stream(entityNode.path("isPartOf").spliterator(), false)
                    .map(this::unpackId)
                    .map(s -> idToNodes.getOrDefault(s, null))
                    .filter(Objects::nonNull)
                    .forEach(parent -> connections.computeIfAbsent(unpackId(parent), key -> new HashSet<>())
                    .add(currentId));
        }
        return connections;
    }

    protected Set<String> getDataEntityIds(RootDataEntity root, JsonNode graph) {
        if (root == null) {
            return Set.of();
        }
        Map<String, Set<String>> network = makeEntityGraph(graph);
        Set<String> directDataEntities = new HashSet<>(root.hasPart);

        Stack<String> processingQueue = new Stack<>();
        processingQueue.addAll(directDataEntities);
        Set<String> result = new HashSet<>();

        while (!processingQueue.empty()) {
            String currentId = processingQueue.pop();
            result.add(currentId);
            network.getOrDefault(currentId, new HashSet<>()).stream()
                    .filter(subId -> !result.contains(subId)) // avoid loops!
                    .forEach(subId -> {
                        result.add(subId);
                        processingQueue.add(subId);
                    });
        }
        return result;
    }

    protected String unpackId(JsonNode node) {
        if (node.isTextual()) {
            return node.asText();
        } else /*if (node.isObject())*/ {
            return node.path(PROP_ID).asText();
        }
    }

    protected Optional<File> checkFolderHasFile(String filepathOrId, File folder) {
        if (IdentifierUtils.isUrl(filepathOrId)) {
            return Optional.empty();
        }
        return IdentifierUtils.decode(filepathOrId)
                .map(decoded -> folder.toPath().resolve(decoded).normalize())
                // defence-in-depth: ensure we are still inside the crate folder
                .filter(resolved -> resolved.startsWith(folder.toPath()))
                .map(Path::toFile)
                .filter(File::exists);
    }

    /**
     * Moves the descriptor and the root entity from the graph to the crate.
     * <p>
     * Extracts the root data entity and the Metadata File Descriptor from the
     * graph and inserts them into the crate object. It also deletes it from the
     * graph. We will need the root dataset to distinguish between data entities
     * and contextual entities.
     *
     * @param crate the crate, which will receive the entities, if available in
     * the graph.
     * @param graph the graph of the Metadata JSON file, where the entities are
     * extracted and removed from.
     */
    protected void moveRootEntitiesFromGraphToCrate(RoCrate crate, ArrayNode graph) {
        Optional<JsonNode> maybeDescriptor = getMetadataDescriptor(graph);

        maybeDescriptor.ifPresent(descriptor -> {
            setCrateDescriptor(crate, descriptor);
            JsonUtilFunctions.removeJsonNodeFromArrayNode(graph, descriptor);

            Optional<ObjectNode> maybeRoot = extractRoot(graph, descriptor);

            maybeRoot.ifPresent(root -> {
                Set<String> hasPartIds = extractHasPartIds(root);

                crate.setRootDataEntity(
                        new RootDataEntity.RootDataEntityBuilder()
                                .setAllUnsafe(root.deepCopy())
                                .setHasPart(hasPartIds)
                                .build());

                JsonUtilFunctions.removeJsonNodeFromArrayNode(graph, root);
            });
        });
    }

    /**
     * Find the metadata descriptor.
     * <p>
     * Currently prefers algorithm of version 1.1 over the one of 1.2-DRAFT.
     *
     * @param graph the graph to search the descriptor in.
     * @return the metadata descriptor of the crate.
     */
    protected Optional<JsonNode> getMetadataDescriptor(ArrayNode graph) {
        boolean isParallel = graph.size() > PARALLELIZATION_THRESHOLD;
        // use the algorithm described here:
        // https://www.researchobject.org/ro-crate/1.1/root-data-entity.html#finding-the-root-data-entity
        Optional<JsonNode> maybeDescriptor = StreamSupport.stream(graph.spliterator(), isParallel)
                // "2. if the conformsTo property is a URI that starts with
                // https://w3id.org/ro/crate/"
                .filter(node -> node.path(PROP_CONFORMS_TO).path(PROP_ID).asText().startsWith(SPECIFICATION_PREFIX))
                // "3. from this entity’s about object keep the @id URI as variable root"
                .filter(node -> node.path(PROP_ABOUT).path(PROP_ID).isTextual())
                // There should be only one descriptor. If multiple exist, we take the first
                // one.
                .findFirst();
        return maybeDescriptor.or(()
                -> // from https://www.researchobject.org/ro-crate/1.2-DRAFT/root-data-entity.html#finding-the-root-data-entity
                StreamSupport.stream(graph.spliterator(), isParallel)
                        .filter(node -> node.path(PROP_ID).asText().equals(FILE_METADATA_JSON))
                        .findFirst()
        );
    }

    /**
     * Extracts the root entity from the graph, using the information from the
     * descriptor.
     * <p>
     * Basically implements step 5 of the algorithm described here:
     * <a href="https://www.researchobject.org/ro-crate/1.1/root-data-entity.html#finding-the-root-data-entity">
     * https://www.researchobject.org/ro-crate/1.1/root-data-entity.html#finding-the-root-data-entity
     * </a>
     *
     * @param graph the graph from the metadata JSON-LD file
     * @param descriptor the RO-Crate descriptor
     * @return the root entity, if found
     */
    private Optional<ObjectNode> extractRoot(ArrayNode graph, JsonNode descriptor) {
        String rootId = descriptor.get(PROP_ABOUT).get(PROP_ID).asText();
        boolean isParallel = graph.size() > PARALLELIZATION_THRESHOLD;
        return StreamSupport.stream(graph.spliterator(), isParallel)
                // root is an object (filter + conversion)
                .filter(JsonNode::isObject)
                .map(JsonNode::<ObjectNode>deepCopy)
                // "5. if the entity has an @id URI that matches root return it"
                .filter(node -> node.path(PROP_ID).asText().equals(rootId))
                .findFirst();
    }

    private Set<String> extractHasPartIds(ObjectNode root) {
        JsonNode hasPartNode = root.path(PROP_HAS_PART);
        boolean isParallel = hasPartNode.isArray() && hasPartNode.size() > PARALLELIZATION_THRESHOLD;
        Set<String> hasPartIds = StreamSupport.stream(hasPartNode.spliterator(), isParallel)
                .map(hasPart -> hasPart.path(PROP_ID).asText())
                .filter(text -> !text.isBlank())
                .collect(Collectors.toSet());
        if (hasPartIds.isEmpty() && hasPartNode.path(PROP_ID).isTextual()) {
            hasPartIds.add(hasPartNode.path(PROP_ID).asText());
        }
        return hasPartIds;
    }

    private void setCrateDescriptor(RoCrate crate, JsonNode descriptor) {
        ContextualEntity descriptorEntity = new ContextualEntity.ContextualEntityBuilder()
                .setAllUnsafe(descriptor.deepCopy())
                .build();
        crate.setJsonDescriptor(descriptorEntity);
    }
}
