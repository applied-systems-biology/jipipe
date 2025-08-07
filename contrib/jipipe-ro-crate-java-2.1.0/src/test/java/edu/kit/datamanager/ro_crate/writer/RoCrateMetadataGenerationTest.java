package edu.kit.datamanager.ro_crate.writer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.HelpFunctions;
import edu.kit.datamanager.ro_crate.reader.Readers;
import edu.kit.datamanager.ro_crate.validation.JsonSchemaValidation;
import edu.kit.datamanager.ro_crate.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static edu.kit.datamanager.ro_crate.util.Graph.*;
import static org.junit.jupiter.api.Assertions.*;

class RoCrateMetadataGenerationTest {

    private final String currentVersionId = new ProvenanceManager().getLibraryId();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = new Validator(new JsonSchemaValidation());
    }

    private void validateCrate(RoCrate crate) {
        assertTrue(validator.validate(crate),
            "Crate should validate against the JSON schema");
    }

    @Test
    void should_ContainRoCrateJavaEntities_When_WritingEmptyCrate(@TempDir Path tempDir) throws IOException {
        // Create and write crate
        RoCrate crate = new RoCrate.RoCrateBuilder().build();
        validateCrate(crate);

        Path outputPath = tempDir.resolve("test-crate");
        Writers.newFolderWriter().save(crate, outputPath.toString());

        // Re-read the crate to verify it's still valid after writing
        RoCrate readCrate = Readers.newFolderReader().readCrate(outputPath.toString());
        validateCrate(readCrate);

        // Read and print metadata for debugging
        String metadata = Files.readString(outputPath.resolve("ro-crate-metadata.json"));
        HelpFunctions.prettyPrintJsonString(metadata);

        // Parse metadata file
        JsonNode rootNode = objectMapper.readTree(metadata);
        JsonNode graph = rootNode.get("@graph");

        // Find ro-crate-java entity
        JsonNode roCrateJavaEntity = findEntityById(graph, this.currentVersionId);
        assertNotNull(roCrateJavaEntity, "ro-crate-java entity should exist");
        assertEquals("SoftwareApplication", roCrateJavaEntity.get("@type").asText(),
            "ro-crate-java should be of type SoftwareApplication");

        // Find CreateAction entity
        JsonNode createActionEntity = findEntityByType(graph, "CreateAction");
        assertNotNull(createActionEntity, "CreateAction entity should exist");
        assertNotNull(createActionEntity.get("startTime"), "CreateAction should have startTime");
        assertEquals(this.currentVersionId, createActionEntity.get("agent").get("@id").asText(),
            "CreateAction should reference ro-crate-java as agent");
    }

    @Test
    void should_HaveRequiredPropertiesInRoCrateJavaEntity_When_WritingCrate(@TempDir Path tempDir) throws IOException {
        // Create and write crate
        RoCrate crate = new RoCrate.RoCrateBuilder().build();
        validateCrate(crate);

        Path outputPath = tempDir.resolve("test-crate");
        Writers.newFolderWriter().save(crate, outputPath.toString());

        RoCrate readCrate = Readers.newFolderReader().readCrate(outputPath.toString());
        validateCrate(readCrate);

        // Read and print metadata for debugging
        String metadata = Files.readString(outputPath.resolve("ro-crate-metadata.json"));
        HelpFunctions.prettyPrintJsonString(metadata);

        // Parse metadata file
        JsonNode rootNode = objectMapper.readTree(metadata);
        JsonNode roCrateJavaEntity = findEntityById(rootNode.get("@graph"), this.currentVersionId);

        assertNotNull(roCrateJavaEntity, "ro-crate-java entity should exist");
        assertEquals("ro-crate-java", roCrateJavaEntity.get("name").asText(),
            "should have correct name");
        assertEquals("https://github.com/kit-data-manager/ro-crate-java",
            roCrateJavaEntity.get("url").asText(),
            "should have correct repository URL");
        assertNotNull(roCrateJavaEntity.get("version"),
            "should have version property");
    }

    @Test
    void should_HaveBidirectionalRelation_Between_RoCrateJavaAndItsAction(@TempDir Path tempDir) throws IOException {
        // Create and write crate
        RoCrate crate = new RoCrate.RoCrateBuilder().build();
        validateCrate(crate);

        Path outputPath = tempDir.resolve("test-crate");
        Writers.newFolderWriter().save(crate, outputPath.toString());

        RoCrate readCrate = Readers.newFolderReader().readCrate(outputPath.toString());
        validateCrate(readCrate);

        // Read and print metadata for debugging
        String metadata = Files.readString(outputPath.resolve("ro-crate-metadata.json"));
        HelpFunctions.prettyPrintJsonString(metadata);

        // Parse metadata file
        JsonNode rootNode = objectMapper.readTree(metadata);
        JsonNode graph = rootNode.get("@graph");

        // Get both entities
        JsonNode roCrateJavaEntity = findEntityById(graph, this.currentVersionId);
        JsonNode createActionEntity = findEntityByType(graph, "CreateAction");

        assertNotNull(roCrateJavaEntity, "ro-crate-java entity should exist");
        assertNotNull(createActionEntity, "CreateAction entity should exist");

        // Test CreateAction -> ro-crate-java reference
        JsonNode agentRef = createActionEntity.get("agent");
        assertNotNull(agentRef, "CreateAction should have agent property");
        assertEquals(this.currentVersionId, agentRef.get("@id").asText(),
            "CreateAction's agent should reference ro-crate-java");

        // Test ro-crate-java -> CreateAction reference
        JsonNode actionRef = roCrateJavaEntity.get("Action");
        assertNotNull(actionRef, "ro-crate-java should have action property");
        assertEquals(createActionEntity.get("@id").asText(), actionRef.get("@id").asText(),
            "ro-crate-java's action should reference the CreateAction");
    }

    @Test
    void should_AccumulateActions_When_WritingMultipleTimes(@TempDir Path tempDir) throws IOException, InterruptedException {
        // Create and write crate first time
        RoCrate crate = new RoCrate.RoCrateBuilder().build();
        validateCrate(crate);

        Path outputPath = tempDir.resolve("test-crate");
        Writers.newFolderWriter().save(crate, outputPath.toString());
        validateCrate(Readers.newFolderReader().readCrate(outputPath.toString()));
        Thread.sleep(10);

        // Write same crate two more times to simulate updates
        Writers.newFolderWriter().save(crate, outputPath.toString());
        Thread.sleep(10);
        Writers.newFolderWriter().save(crate, outputPath.toString());
        validateCrate(Readers.newFolderReader().readCrate(outputPath.toString()));
        Thread.sleep(10);

        // Read and print metadata for debugging
        String metadata = Files.readString(outputPath.resolve("ro-crate-metadata.json"));
        HelpFunctions.prettyPrintJsonString(metadata);

        // Parse metadata file
        JsonNode rootNode = objectMapper.readTree(metadata);
        JsonNode graph = rootNode.get("@graph");

        // Get ro-crate-java entity
        JsonNode roCrateJavaEntity = findEntityById(graph, this.currentVersionId);
        assertNotNull(roCrateJavaEntity, "ro-crate-java entity should exist");

        // Verify actions array exists and has three entries
        assertTrue(roCrateJavaEntity.get("Action").isArray(),
            "ro-crate-java should have an array of actions");
        assertEquals(3, roCrateJavaEntity.get("Action").size(),
            "should have three actions after three writes");

        // Find all action entities
        JsonNode createAction = findEntityByType(graph, "CreateAction");
        assertNotNull(createAction, "should have one CreateAction");

        JsonNode[] createActions = findEntitiesByType(graph, "CreateAction");
        assertEquals(1, createActions.length, "should have exactly one CreateAction");

        JsonNode[] updateActions = findEntitiesByType(graph, "UpdateAction");
        assertEquals(2, updateActions.length, "should have two UpdateActions");

        // Verify CreateAction properties
        assertNotNull(createAction.get("startTime"), "CreateAction should have startTime");
        assertEquals(this.currentVersionId, createAction.get("agent").get("@id").asText(),
            "CreateAction should reference ro-crate-java as agent");

        // Verify UpdateAction properties
        for (JsonNode updateAction : updateActions) {
            assertNotNull(updateAction.get("startTime"),
                "UpdateAction should have startTime");
            assertEquals(this.currentVersionId, updateAction.get("agent").get("@id").asText(),
                "UpdateAction should reference ro-crate-java as agent");
        }

        // Verify chronological order of timestamps
        String createTime = createAction.get("startTime").asText();
        String updateTime1 = updateActions[0].get("startTime").asText();
        String updateTime2 = updateActions[1].get("startTime").asText();
        // The order of updates is not the order in the graph.
        // But we can check that creation happened before the updates:
        assertTrue(createTime.compareTo(updateTime1) < 0,
            "First update should be after creation");
        assertTrue(createTime.compareTo(updateTime2) < 0,
            "Second update should be after creation");
    }

    @Test
    void should_HaveValidVersionFormat_When_WritingCrate(@TempDir Path tempDir) throws IOException {
        // Create and write crate
        RoCrate crate = new RoCrate.RoCrateBuilder().build();
        validateCrate(crate);

        Path outputPath = tempDir.resolve("test-crate");
        Writers.newFolderWriter().save(crate, outputPath.toString());
        validateCrate(Readers.newFolderReader().readCrate(outputPath.toString()));

        // Read and print metadata for debugging
        String metadata = Files.readString(outputPath.resolve("ro-crate-metadata.json"));
        HelpFunctions.prettyPrintJsonString(metadata);

        // Parse metadata file
        JsonNode rootNode = objectMapper.readTree(metadata);
        JsonNode roCrateJavaEntity = findEntityById(rootNode.get("@graph"), this.currentVersionId);

        // Version format validation
        @SuppressWarnings("DataFlowIssue")
        String version = roCrateJavaEntity.get("version").asText();

        // Semantic versioning regex pattern that allows:
        // - Required: major.minor.patch (e.g., 1.2.3)
        // - Optional: pre-release identifier (e.g., -rc1, -RC1, -beta.1, -SNAPSHOT)
        // - Optional: build metadata (e.g., +build.123)
        String semverPattern = "(?i)^\\d+\\.\\d+\\.\\d+(?:-(?:rc\\d+|alpha|beta|snapshot)(?:\\.\\d+)?)?(?:\\+[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*)?$";

        assertTrue(version.matches(semverPattern),
            String.format("Version '%s' should match semantic versioning format: major.minor.patch[-prerelease][+build]%n" +
                        "Examples: 1.2.3, 1.2.3-rc1, 1.2.3-SNAPSHOT, 1.2.3-beta.1, 1.2.3+build.123", version));
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void should_HaveCompleteMetadata_When_WritingCrate(@TempDir Path tempDir) throws IOException {
        // Create and write crate
        RoCrate crate = new RoCrate.RoCrateBuilder().build();
        validateCrate(crate);

        Path outputPath = tempDir.resolve("test-crate");
        Writers.newFolderWriter().save(crate, outputPath.toString());
        validateCrate(Readers.newFolderReader().readCrate(outputPath.toString()));

        // Read and print metadata for debugging
        String metadata = Files.readString(outputPath.resolve("ro-crate-metadata.json"));
        HelpFunctions.prettyPrintJsonString(metadata);

        // Parse metadata file
        JsonNode rootNode = objectMapper.readTree(metadata);
        JsonNode roCrateJavaEntity = findEntityById(rootNode.get("@graph"), this.currentVersionId);

        // Required properties with specific values
        assertEquals("ro-crate-java", roCrateJavaEntity.get("name").asText(),
            "should have correct name");
        assertEquals("https://github.com/kit-data-manager/ro-crate-java",
            roCrateJavaEntity.get("url").asText(),
            "should have correct repository URL");
        assertEquals("SoftwareApplication", roCrateJavaEntity.get("@type").asText(),
            "should have correct type");

        // Optional but recommended properties
        assertNotNull(roCrateJavaEntity.get("description"),
            "should have a description");
        assertFalse(roCrateJavaEntity.get("description").asText().isEmpty(),
                "description should not be empty");

        assertNotNull(roCrateJavaEntity.get("license"),
            "should have a license");
        assertTrue(roCrateJavaEntity.has("softwareVersion"),
            "should have softwareVersion as an alias for version");
        assertEquals(roCrateJavaEntity.get("version").asText(),
            roCrateJavaEntity.get("softwareVersion").asText(),
            "version and softwareVersion should match");
    }

    @Test
    void should_AddProvenanceInfo_When_ModifyingExistingCrateWithoutProvenance(@TempDir Path tempDir) throws IOException {
        // First create a crate without provenance information
        RoCrate originalCrate = new RoCrate.RoCrateBuilder().build();
        validateCrate(originalCrate);

        Path outputPath = tempDir.resolve("test-crate");

        // Use writer with disabled provenance (not implemented yet)
        Writers.newFolderWriter()
                .withAutomaticProvenance(null)
                .save(originalCrate, outputPath.toString());

        // Verify the original crate has no provenance information
        String originalMetadata = Files.readString(outputPath.resolve("ro-crate-metadata.json"));
        HelpFunctions.prettyPrintJsonString(originalMetadata);

        JsonNode originalRoot = objectMapper.readTree(originalMetadata);
        JsonNode originalGraph = originalRoot.get("@graph");
        assertNull(findEntityById(originalGraph, this.currentVersionId),
            "Original crate should not have ro-crate-java entity");
        assertNull(findEntityByType(originalGraph, "CreateAction"),
            "Original crate should not have CreateAction");
        assertNull(findEntityByType(originalGraph, "UpdateAction"),
            "Original crate should not have UpdateAction");

        // Now read and modify the crate
        RoCrate modifiedCrate = Readers.newFolderReader().readCrate(outputPath.toString());
        validateCrate(modifiedCrate);
        modifiedCrate.getRootDataEntity().addProperty("description", "Modified crate");
        validateCrate(modifiedCrate);

        // Write the modified crate with provenance enabled (default)
        Path modifiedPath = tempDir.resolve("modified-crate");
        Writers.newFolderWriter().save(modifiedCrate, modifiedPath.toString());

        // Read and verify the modified crate's metadata
        String modifiedMetadata = Files.readString(modifiedPath.resolve("ro-crate-metadata.json"));
        HelpFunctions.prettyPrintJsonString(modifiedMetadata);

        JsonNode modifiedRoot = objectMapper.readTree(modifiedMetadata);
        JsonNode modifiedGraph = modifiedRoot.get("@graph");

        // Verify ro-crate-java entity was added
        JsonNode roCrateJavaEntity = findEntityById(modifiedGraph, this.currentVersionId);
        assertNotNull(roCrateJavaEntity, "ro-crate-java entity should be added");

        // Should only have UpdateAction, no CreateAction
        assertNull(findEntityByType(modifiedGraph, "CreateAction"),
            "Modified crate should not have CreateAction");

        JsonNode updateAction = findEntityByType(modifiedGraph, "UpdateAction");
        assertNotNull(updateAction, "Should have UpdateAction");

        // Verify update action properties
        assertNotNull(updateAction.get("startTime"),
            "UpdateAction should have startTime");
        assertEquals(this.currentVersionId,
            updateAction.get("agent").get("@id").asText(),
            "UpdateAction should reference ro-crate-java as agent");

        // Verify ro-crate-java references the action
        assertTrue(roCrateJavaEntity.get("Action").isObject(),
            "ro-crate-java should have a single reference to an UpdateAction");
        assertEquals(updateAction.get("@id").asText(),
            roCrateJavaEntity.get("Action").get("@id").asText(),
            "ro-crate-java should reference the UpdateAction");
    }

    @Test
    void should_PreserveExistingProvenance_When_ModifyingCrate(@TempDir Path tempDir) throws IOException, InterruptedException {
        // First create a crate with normal provenance
        RoCrate originalCrate = new RoCrate.RoCrateBuilder().build();
        validateCrate(originalCrate);

        Path outputPath = tempDir.resolve("test-crate");
        Writers.newFolderWriter().save(originalCrate, outputPath.toString());
        Thread.sleep(10);

        // Now read and modify the crate
        RoCrate modifiedCrate = Readers.newFolderReader().readCrate(outputPath.toString());
        validateCrate(modifiedCrate);
        modifiedCrate.getRootDataEntity().addProperty("description", "Modified crate");
        validateCrate(modifiedCrate);

        // Write the modified crate
        Writers.newFolderWriter().save(modifiedCrate, outputPath.toString());

        // Read and verify the metadata
        String metadata = Files.readString(outputPath.resolve("ro-crate-metadata.json"));
        HelpFunctions.prettyPrintJsonString(metadata);

        JsonNode root = objectMapper.readTree(metadata);
        JsonNode graph = root.get("@graph");

        // Should have both CreateAction and UpdateAction
        JsonNode createAction = findEntityByType(graph, "CreateAction");
        assertNotNull(createAction, "Original CreateAction should be preserved");

        JsonNode[] updateActions = findEntitiesByType(graph, "UpdateAction");
        assertEquals(1, updateActions.length, "Should have exactly one UpdateAction");

        // Verify chronological order
        String createTime = createAction.get("startTime").asText();
        String updateTime = updateActions[0].get("startTime").asText();
        assertTrue(createTime.compareTo(updateTime) < 0,
            "Update should be after creation");

        // Verify ro-crate-java entity references both actions
        JsonNode roCrateJavaEntity = findEntityById(graph, this.currentVersionId);
        //noinspection DataFlowIssue
        assertTrue(roCrateJavaEntity.get("Action").isArray(),
            "ro-crate-java should have an array of actions");
        assertEquals(2, roCrateJavaEntity.get("Action").size(),
            "should have both actions");
    }
}
