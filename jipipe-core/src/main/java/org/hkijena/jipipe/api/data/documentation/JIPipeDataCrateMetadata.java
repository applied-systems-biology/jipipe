package org.hkijena.jipipe.api.data.documentation;

import org.hkijena.jipipe.api.data.JIPipeData;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface JIPipeDataCrateMetadata {

    /**
     * Creates the metadata entry by parsing The {@link ConfigureJIPipeDataCrate} annotation of the data class.
     * Also handles the inheritance. Ensures the same IDs are overwritten by ones higher up within the inheritance hierarchy.
     *
     * @param dataClass the data class
     * @return the crate metadata
     */
    static JIPipeDataCrateMetadata create(Class<? extends JIPipeData> dataClass) {
        JIPipeMutableDataCrateMetadata result = new JIPipeMutableDataCrateMetadata();
        final ConfigureJIPipeDataCrate rootConfig = dataClass.getAnnotation(ConfigureJIPipeDataCrate.class);
        if (rootConfig == null) {
            throw new IllegalArgumentException("The data class " + dataClass.getName() + " must have a @ConfigureJIPipeDataCrate annotation!");
        }

        Set<Class<? extends JIPipeData>> handledClasses = new HashSet<>();
        handledClasses.add(dataClass); // Prevent loop
        for (Class<? extends JIPipeData> inherited : rootConfig.inherits()) {
            if (inherited == dataClass) {
                throw new IllegalArgumentException("Data crate definition for " + dataClass + " inherits from itself!");
            }
            createEntities(inherited, handledClasses, result);
        }

        // Ensures that the current data class items overwrite anything inherited
        createEntities(rootConfig.entities(), result);

        return result;
    }

    private static void createEntities(Class<? extends JIPipeData> dataClass, Set<Class<? extends JIPipeData>> handledClasses, JIPipeMutableDataCrateMetadata result) {
        if (handledClasses.contains(dataClass)) {
            return;
        }
        handledClasses.add(dataClass);
        final ConfigureJIPipeDataCrate config = dataClass.getAnnotation(ConfigureJIPipeDataCrate.class);
        if (config == null) {
            throw new IllegalStateException("Tried to inherit crate metadata from " + dataClass + ", but has no @ConfigureJIPipeDataCrate annotation!");
        }
        for (Class<? extends JIPipeData> inherited : config.inherits()) {
            createEntities(inherited, handledClasses, result);
        }
        createEntities(config.entities(), result);
    }

    private static void createEntities(DefineJIPipeDataCrateEntity[] entities, JIPipeMutableDataCrateMetadata result) {
        for (DefineJIPipeDataCrateEntity entity : entities) {
            JIPipeDataCrateMetadataEntry entry = new JIPipeDataCrateMetadataEntry();
            entry.setId(entity.id());
            entry.setName(entity.name());
            entry.setDescription(entity.description());
            entry.setEncodingFormat(List.of(entity.encodingFormat()));
            entry.setType(entity.type());
            entry.setPresence(entity.presence());

            result.put(entry);
        }
    }

    /**
     * Returns a sorted list of entries
     *
     * @return the entries
     */
    List<JIPipeDataCrateMetadataEntry> getEntries();

    /**
     * Returns the entries
     *
     * @return the entries
     */
    Map<String, JIPipeDataCrateMetadataEntry> getEntriesMap();

    /**
     * Checks if the crate metadata is valid
     *
     * @return if the crate metadata is valid
     */
    boolean isValid();

    boolean isEmpty();
}
