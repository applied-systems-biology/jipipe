package org.hkijena.jipipe.api.data.context;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Set;

/**
 * Context annotation attached to data in a data table.
 * Contains information about the unique ID of the data and list of predecessor data.
 */
@JsonDeserialize(as = JIPipeMutableDataContext.class)
public interface JIPipeDataContext {

    /**
     * Gets the ID of the context
     * @return the ID
     */
    @JsonGetter("id")
    String getId();

    /**
     * Gets the known predecessors
     * @return the predecessors
     */
    @JsonGetter("predecessors")
    Set<String> getPredecessors();
}
