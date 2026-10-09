package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.nodes.JIPipeNodeClassification;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyInputProfile;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension;
import org.hkijena.jipipe.api.nodes.taxonomy.NoFramework;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;

public abstract class DefaultJIPipeNodeDatabaseEntry implements JIPipeNodeDatabaseEntry {
    private final Map<Class<?>, Object> attachments = new HashMap<>();
    private final StampedLock attachmentsLock = new StampedLock();

    // --- Node taxonomy facets (Phase 1b) ---
    private Set<String> functionIds = Collections.emptySet();
    private JIPipeNodeTaxonomyFramework framework = new NoFramework();
    private Set<JIPipeNodeTaxonomySpatialDimension> spatialDimensions = Collections.emptySet();
    private JIPipeNodeTaxonomyInputProfile inputProfile = JIPipeNodeTaxonomyInputProfile.NA;
    private Set<String> tags = Collections.emptySet();

    @Override
    public Set<String> getFunctionIds() {
        return functionIds;
    }

    /**
     * Sets the taxonomy function ids this entry's node appears under.
     *
     * @param functionIds the taxonomy function ids, possibly empty
     */
    public void setFunctionIds(Set<String> functionIds) {
        this.functionIds = functionIds == null ? Collections.emptySet() : new LinkedHashSet<>(functionIds);
    }

    @Override
    public JIPipeNodeTaxonomyFramework getFramework() {
        return framework;
    }

    /**
     * Sets the framework this entry's node implements.
     *
     * @param framework the framework, or {@link NoFramework} if the node declares none
     */
    public void setFramework(JIPipeNodeTaxonomyFramework framework) {
        this.framework = framework == null ? new NoFramework() : framework;
    }

    @Override
    public Set<JIPipeNodeTaxonomySpatialDimension> getSpatialDimensions() {
        return spatialDimensions;
    }

    /**
     * Sets the spatial dimensions this entry's node operates in.
     *
     * @param spatialDimensions the declared spatial dimensions, possibly empty
     */
    public void setSpatialDimensions(Set<JIPipeNodeTaxonomySpatialDimension> spatialDimensions) {
        this.spatialDimensions = spatialDimensions == null ? Collections.emptySet() : new LinkedHashSet<>(spatialDimensions);
    }

    @Override
    public JIPipeNodeTaxonomyInputProfile getInputProfile() {
        return inputProfile;
    }

    /**
     * Sets the derived input profile of this entry's node.
     *
     * @param inputProfile the input profile
     */
    public void setInputProfile(JIPipeNodeTaxonomyInputProfile inputProfile) {
        this.inputProfile = inputProfile == null ? JIPipeNodeTaxonomyInputProfile.NA : inputProfile;
    }

    @Override
    public Set<String> getTags() {
        return tags;
    }

    /**
     * Sets the entry's merged taxonomy tag set.
     *
     * @param tags the merged tags, possibly empty
     */
    public void setTags(Set<String> tags) {
        this.tags = tags == null ? Collections.emptySet() : new LinkedHashSet<>(tags);
    }

    @Override
    public <T> T getAttachment(Class<T> attachmentClass) {
        final long stamp = attachmentsLock.readLock();
        try {
            return (T) attachments.get(attachmentClass);
        } finally {
            attachmentsLock.unlock(stamp);
        }
    }

    @Override
    public <T> void attach(T attachment) {
        final long stamp = attachmentsLock.writeLock();
        try {
            attachments.put(attachment.getClass(), attachment);
        } finally {
            attachmentsLock.unlock(stamp);
        }
    }

    @Override
    public JIPipeNodeClassification getNodeClassification() {
        return JIPipeNodeClassification.Standard;
    }
}
