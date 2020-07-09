/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.pipelinej.api.grouping.parameters;

import com.google.common.eventbus.EventBus;
import org.hkijena.pipelinej.api.parameters.ACAQCustomParameterCollection;
import org.hkijena.pipelinej.api.parameters.ACAQNamedParameterCollection;
import org.hkijena.pipelinej.api.parameters.ACAQParameterAccess;
import org.hkijena.pipelinej.api.parameters.ACAQParameterCollection;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains {@link GraphNodeParameterReferenceAccess}
 */
public class GraphNodeParameterReferenceAccessGroup implements ACAQParameterCollection, ACAQCustomParameterCollection, ACAQNamedParameterCollection {

    private final EventBus eventBus = new EventBus();
    private final GraphNodeParameterReferenceGroup group;
    private Map<String, ACAQParameterAccess> parameters = new HashMap<>();

    /**
     * Creates a new instance
     *
     * @param group the group
     */
    public GraphNodeParameterReferenceAccessGroup(GraphNodeParameterReferenceGroup group) {
        this.group = group;
    }

    @Override
    public Map<String, ACAQParameterAccess> getParameters() {
        return parameters;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public String getDefaultParameterCollectionName() {
        return group.getName();
    }

    @Override
    public String getDefaultParameterCollectionDescription() {
        return group.getDescription();
    }
}
