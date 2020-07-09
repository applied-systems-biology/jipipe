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

package org.hkijena.jipipe.api.grouping.parameters;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.parameters.JIPipeCustomParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeNamedParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains {@link GraphNodeParameterReferenceAccess}
 */
public class GraphNodeParameterReferenceAccessGroup implements JIPipeParameterCollection, JIPipeCustomParameterCollection, JIPipeNamedParameterCollection {

    private final EventBus eventBus = new EventBus();
    private final GraphNodeParameterReferenceGroup group;
    private Map<String, JIPipeParameterAccess> parameters = new HashMap<>();

    /**
     * Creates a new instance
     *
     * @param group the group
     */
    public GraphNodeParameterReferenceAccessGroup(GraphNodeParameterReferenceGroup group) {
        this.group = group;
    }

    @Override
    public Map<String, JIPipeParameterAccess> getParameters() {
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
