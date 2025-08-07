package edu.kit.datamanager.ro_crate.writer;

import edu.kit.datamanager.ro_crate.Crate;
import edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity;
import edu.kit.datamanager.ro_crate.util.ClasspathPropertiesVersionProvider;
import edu.kit.datamanager.ro_crate.util.VersionProvider;

import java.time.Instant;

import static edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity.ContextualEntityBuilder;

/**
 * Manages provenance information for RO-Crates.
 * Handles the creation and updating of ro-crate-java entity and its actions.
 */
public class ProvenanceManager {
    /**
     * A record to hold the prefix for the ro-crate-java ID.
     * This is used to ensure that the ID is consistent across different versions of the library.
     * Having a type for the prefix avoids using it by accident as a full ID.
     */
    protected record IdPrefix(String prefix) {
        /**
         * Constructs a String with the given suffix.
         *
         * @param suffix The suffix to append to the prefix.
         * @return A String combining the prefix and suffix, separated by a hyphen.
         *         Like this: "$prefix-$suffix".
         */
        public String withSuffix(String suffix) {
            return prefix + "-" + suffix;
        }

        @Override
        public String toString() {
            return prefix;
        }
    }

    /**
     * The prefix for the ro-crate-java ID.
     * This is used to identify the ro-crate-java entity in the crate.
     */
    protected static final IdPrefix RO_CRATE_JAVA_ID_PREFIX = new IdPrefix("#ro-crate-java");

    /**
     * The VersionProvider used to retrieve the version of ro-crate-java.
     * This allows for flexibility in how the version is determined, e.g., from a properties file.
     */
    protected VersionProvider versionProvider;

    /**
     * Constructs a ProvenanceManager with the default ClasspathPropertiesVersionProvider.
     */
    public ProvenanceManager() {
        this(new ClasspathPropertiesVersionProvider());
    }

    /**
     * Constructs a ProvenanceManager with a specified VersionProvider.
     *
     * @param versionProvider The VersionProvider to use for retrieving the version of ro-crate-java.
     */
    public ProvenanceManager(VersionProvider versionProvider) {
        this.versionProvider = versionProvider;
    }

    /**
     * Returns the full ID for the ro-crate-java entity of this library version
     * to be used for an entity describing it.
     * <p>
     * The ID is constructed using the RO_CRATE_JAVA_ID_PREFIX and the version from the VersionProvider.
     *
     * @return The ID for the ro-crate-java entity.
     */
    public String getLibraryId() {
        return RO_CRATE_JAVA_ID_PREFIX.withSuffix(versionProvider.getVersion().toLowerCase());
    }

    /**
     * Adds provenance information to the given crate.
     * This includes creating or updating the ro-crate-java entity and its associated action entity.
     *
     * @param crate The crate to which provenance information will be added.
     */
    public void addProvenanceInformation(Crate crate) {
        // Determine if this is the first write
        boolean isFirstWrite = crate.getAllContextualEntities().stream().noneMatch(
                entity -> entity.getId().startsWith(RO_CRATE_JAVA_ID_PREFIX.toString()))
                && !crate.isImported();

        String libraryId = this.getLibraryId();

        // Create action entity first
        ContextualEntity actionEntity = buildNewActionEntity(isFirstWrite, libraryId);

        // Create or update ro-crate-java entity
        ContextualEntity roCrateJavaEntity = buildRoCrateJavaEntity(crate, actionEntity.getId(), libraryId);

        // Add entities to crate
        crate.addContextualEntity(roCrateJavaEntity);
        crate.addContextualEntity(actionEntity);
    }

    protected ContextualEntity buildNewActionEntity(boolean isFirstWrite, String libraryId) {
        return new ContextualEntityBuilder()
                .addType(isFirstWrite ? "CreateAction" : "UpdateAction")
                .addIdProperty("result", "./")
                .addProperty("startTime", Instant.now().toString())
                .addIdProperty("agent", libraryId)
                .build();
    }

    protected ContextualEntity buildRoCrateJavaEntity(
            Crate crate,
            String newActionId,
            String libraryId
    ) {
        String version = this.versionProvider.getVersion();
        ContextualEntity self = crate.getAllContextualEntities().stream()
                .filter(contextualEntity -> libraryId.equals(contextualEntity.getId()))
                .findFirst()
                .orElseGet(() -> new ContextualEntityBuilder()
                        .setId(libraryId)
                        .addType("SoftwareApplication")
                        .addProperty("name", "ro-crate-java")
                        .addProperty("url", "https://github.com/kit-data-manager/ro-crate-java")
                        .addProperty("version", version)
                        .addProperty("softwareVersion", version)
                        .addProperty("license", "Apache-2.0")
                        .addProperty("description", "A Java library for creating and manipulating RO-Crates")
                        .addIdProperty("Action", newActionId)
                        .build()
                );
        self.addIdProperty("Action", newActionId);
        return self;
    }
}
