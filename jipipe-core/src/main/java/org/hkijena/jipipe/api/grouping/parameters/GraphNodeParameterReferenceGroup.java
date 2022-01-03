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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.grouping.events.ParameterReferencesChangedEvent;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A list of {@link GraphNodeParameterReference}.
 */
public class GraphNodeParameterReferenceGroup {
    private final EventBus eventBus = new EventBus();
    private List<GraphNodeParameterReference> content = new ArrayList<>();
    private String name;
    private HTMLText description = new HTMLText();

    /**
     * Creates a new instance
     */
    public GraphNodeParameterReferenceGroup() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public GraphNodeParameterReferenceGroup(GraphNodeParameterReferenceGroup other) {
        this.name = other.name;
        this.description = other.description;
        for (GraphNodeParameterReference reference : other.content) {
            this.content.add(new GraphNodeParameterReference(reference));
        }
    }

    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonGetter("description")
    public HTMLText getDescription() {
        return description;
    }

    @JsonSetter("description")
    public void setDescription(HTMLText description) {
        this.description = description;
    }

    @JsonGetter("content")
    public List<GraphNodeParameterReference> getContent() {
        return Collections.unmodifiableList(content);
    }

    @JsonSetter("content")
    public void setContent(List<GraphNodeParameterReference> content) {
        this.content = content;
        triggerChangedEvent();
    }

    public void triggerChangedEvent() {
        eventBus.post(new ParameterReferencesChangedEvent());
    }

    /**
     * Adds a new element into the group. Will not add duplicate elements.
     *
     * @param reference the reference
     */
    public void addContent(GraphNodeParameterReference reference) {
        if (!content.contains(reference))
            this.content.add(reference);
        triggerChangedEvent();
    }

    /**
     * Adds multiple elements. Will not add duplicate elements.
     *
     * @param references the references
     */
    public void addContent(Collection<GraphNodeParameterReference> references) {
        if (references.isEmpty())
            return;
        for (GraphNodeParameterReference reference : references) {
            if (!content.contains(reference)) {
                this.content.add(reference);
            }
        }
        triggerChangedEvent();
    }

    /**
     * Adds a new element into the group
     *
     * @param reference the reference
     */
    public void removeContent(GraphNodeParameterReference reference) {
        this.content.remove(reference);
        triggerChangedEvent();
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}
