package edu.kit.datamanager.ro_crate.writer;

import edu.kit.datamanager.ro_crate.Crate;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProvenanceManagerTest {
    public final String OLD_VERSION = "1.0.0";
    private final ProvenanceManager OLD_PROV_MANAGER = new ProvenanceManager(() -> OLD_VERSION);
    private final String OLD_LIBRARY_ID = OLD_PROV_MANAGER.getLibraryId();

    public final String NEW_VERSION = "2.5.3";
    private final ProvenanceManager NEW_PROV_MANAGER = new ProvenanceManager(() -> NEW_VERSION);
    private final String NEW_LIBRARY_ID = NEW_PROV_MANAGER.getLibraryId();

    @Test
    void should_CreateInitialEntities_WithCorrectVersion() {
        // Given
        Crate crate = new RoCrate.RoCrateBuilder().build();

        // When
        OLD_PROV_MANAGER.addProvenanceInformation(crate);

        // Then
        var entities = crate.getAllContextualEntities();
        assertEquals(2, entities.size(), "Should have created two entities");

        // Find ro-crate-java entity
        var roCrateJavaEntity = entities.stream()
                .filter(e -> e.getId().equals(OLD_LIBRARY_ID))
                .findFirst()
                .orElseThrow();

        assertEquals(OLD_VERSION, roCrateJavaEntity.getProperty("version").asText());
        assertEquals(OLD_VERSION, roCrateJavaEntity.getProperty("softwareVersion").asText());

        // Find CreateAction and verify it points to correct version
        var createAction = entities.stream()
                .filter(e -> e.getTypes().contains("CreateAction"))
                .findFirst()
                .orElseThrow();

        assertEquals(OLD_LIBRARY_ID, createAction.getIdProperty("agent"));
    }

    @Test
    void should_CreateDifferentEntities_WhenDifferentVersionsModifyCrate() {
        // Given
        Crate crate = new RoCrate.RoCrateBuilder().build();

        // When creating with old version
        OLD_PROV_MANAGER.addProvenanceInformation(crate);

        // And modifying with new version
        NEW_PROV_MANAGER.addProvenanceInformation(crate);

        // Then
        var entities = crate.getAllContextualEntities();
        assertEquals(4, entities.size(), "Should have four entities (2 ro-crate-java + CreateAction + UpdateAction)");

        // Verify both version entities exist
        var oldVersionEntity = entities.stream()
                .filter(e -> e.getId().equals(OLD_LIBRARY_ID))
                .findFirst()
                .orElseThrow();
        var newVersionEntity = entities.stream()
                .filter(e -> e.getId().equals(NEW_LIBRARY_ID))
                .findFirst()
                .orElseThrow();

        assertEquals(OLD_VERSION, oldVersionEntity.getProperty("version").asText());
        assertEquals(NEW_VERSION, newVersionEntity.getProperty("version").asText());

        // Verify actions point to correct versions
        var createAction = entities.stream()
                .filter(e -> e.getTypes().contains("CreateAction"))
                .findFirst()
                .orElseThrow();
        var updateAction = entities.stream()
                .filter(e -> e.getTypes().contains("UpdateAction"))
                .findFirst()
                .orElseThrow();

        assertEquals(OLD_LIBRARY_ID, createAction.getIdProperty("agent"),
                "CreateAction should point to old version");
        assertEquals(NEW_LIBRARY_ID, updateAction.getIdProperty("agent"),
                "UpdateAction should point to new version");
    }

    @Test
    void should_ReuseExistingVersionEntity_WhenSameVersionModifiesCrateMultipleTimes() {
        // Given
        Crate crate = new RoCrate.RoCrateBuilder().build();

        // When modifying multiple times with same version
        OLD_PROV_MANAGER.addProvenanceInformation(crate);
        OLD_PROV_MANAGER.addProvenanceInformation(crate);
        OLD_PROV_MANAGER.addProvenanceInformation(crate);

        // Then
        var entities = crate.getAllContextualEntities();

        // Should have one ro-crate-java entity and three actions
        long roCrateJavaCount = entities.stream()
                .filter(e -> e.getId().startsWith(ProvenanceManager.RO_CRATE_JAVA_ID_PREFIX.toString()))
                .count();
        assertEquals(1, roCrateJavaCount, "Should have only one ro-crate-java entity");

        var actions = entities.stream()
                .filter(e -> e.getTypes().contains("CreateAction") || e.getTypes().contains("UpdateAction"))
                .toList();
        assertEquals(3, actions.size(), "Should have three actions");

        // All actions should point to the same version entity
        for (ContextualEntity action : actions) {
            assertEquals(OLD_LIBRARY_ID, action.getIdProperty("agent"),
                    "All actions should point to the same version entity");
        }
    }

    @Test
    void should_PreserveVersionSpecificMetadata_WhenModifying() {
        // Given
        Crate crate = new RoCrate.RoCrateBuilder().build();

        // When creating with old version
        new ProvenanceManager(() -> OLD_VERSION).addProvenanceInformation(crate);

        // And modifying with new version
        new ProvenanceManager(() -> NEW_VERSION).addProvenanceInformation(crate);

        // And modifying again with old version
        new ProvenanceManager(() -> OLD_VERSION).addProvenanceInformation(crate);

        // Then
        var entities = crate.getAllContextualEntities();

        // Should have exactly two ro-crate-java entities
        var roCrateJavaEntities = entities.stream()
                .filter(e -> e.getId().startsWith(ProvenanceManager.RO_CRATE_JAVA_ID_PREFIX.toString()))
                .toList();
        assertEquals(2, roCrateJavaEntities.size(), "Should have exactly two ro-crate-java entities");

        // Each entity should maintain its complete metadata
        for (ContextualEntity entity : roCrateJavaEntities) {
            assertNotNull(entity.getProperty("name"), "Should have name");
            assertNotNull(entity.getProperty("url"), "Should have url");
            assertNotNull(entity.getProperty("license"), "Should have license");
            assertEquals(entity.getProperty("version"),
                    entity.getProperty("softwareVersion"),
                    "version and softwareVersion should match");
        }

        // Actions should point to appropriate versions
        var actions = entities.stream()
                .filter(e -> e.getTypes().contains("CreateAction") || e.getTypes().contains("UpdateAction"))
                .toList();
        assertEquals(3, actions.size(), "Should have three actions");

        // First action (CreateAction) should point to old version
        var createAction = actions.stream()
                .filter(e -> e.getTypes().contains("CreateAction"))
                .findFirst()
                .orElseThrow();
        assertEquals(OLD_LIBRARY_ID, createAction.getIdProperty("agent"),
                "CreateAction should point to old version");

        // Update actions should point to respective versions
        var updateActions = actions.stream()
                .filter(e -> e.getTypes().contains("UpdateAction"))
                .toList();
        assertEquals(2, updateActions.size(), "Should have two update actions");

        assertTrue(updateActions.stream()
                .map(e -> e.getIdProperty("agent"))
                .allMatch(id -> id.equals(OLD_LIBRARY_ID) || id.equals(NEW_LIBRARY_ID)),
                "Update actions should point to either old or new version");
    }
}