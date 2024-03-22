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

package org.hkijena.jipipe.api.data.context;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

import java.util.Set;

/**
 * Context annotation attached to data in a data table.
 * Contains information about the unique ID of the data and list of predecessor data.
 */
@JsonDeserialize(as = JIPipeMutableDataContext.class)
public interface JIPipeDataContext {

    /**
     * Creates a new data context
     *
     * @param source       the source
     * @param predecessors the predecessors
     * @return the new context
     */
    static JIPipeDataContext create(String source, JIPipeDataContext... predecessors) {
        JIPipeMutableDataContext context = new JIPipeMutableDataContext(source);
        for (JIPipeDataContext predecessor : predecessors) {
            context.addPredecessor(predecessor);
        }
        return context;
    }

    /**
     * Creates a new data context
     *
     * @param graphNode    the source
     * @param predecessors the predecessors
     * @return the new context
     */
    static JIPipeDataContext create(JIPipeGraphNode graphNode, JIPipeDataContext... predecessors) {
        JIPipeMutableDataContext context = new JIPipeMutableDataContext(graphNode);
        for (JIPipeDataContext predecessor : predecessors) {
            context.addPredecessor(predecessor);
        }
        return context;
    }

    /**
     * Gets the ID of the context
     *
     * @return the ID
     */
    @JsonGetter("id")
    String getId();

    /**
     * The ID of the creator of this context
     *
     * @return the source
     */
    @JsonSetter("source")
    String getSource();

    /**
     * Gets the known predecessors
     *
     * @return the predecessors
     */
    @JsonGetter("predecessors")
    Set<String> getPredecessors();

    /**
     * Returns a new context that contains the current one as predecessor
     *
     * @param source the unique source ID
     * @return the new context
     */
    default JIPipeDataContext branch(String source) {
        JIPipeMutableDataContext context = new JIPipeMutableDataContext(source);
        context.addPredecessor(this);
        return context;
    }

    /**
     * Returns a new context that contains the current one as predecessor
     *
     * @param source the node as source
     * @return the new context
     */
    default JIPipeDataContext branch(JIPipeGraphNode source) {
        return branch(source.getUUIDInParentGraph().toString());
    }

}
