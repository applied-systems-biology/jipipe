package org.hkijena.acaq5.api.traits;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.extensions.parametereditors.editors.StringParameterSettings;

import java.util.HashSet;
import java.util.Set;

/**
 * A mutable implementation of {@link ACAQTraitDeclaration}
 */
public abstract class ACAQMutableTraitDeclaration implements ACAQTraitDeclaration, ACAQParameterCollection {
    private EventBus eventBus = new EventBus();
    private String id;
    private Class<? extends ACAQTrait> traitClass;
    private boolean discriminator;
    private boolean hidden;
    private String name;
    private String description;
    private Set<ACAQTraitDeclaration> inherited = new HashSet<>();

    @Override
    @JsonGetter("id")
    @ACAQParameter("id")
    @ACAQDocumentation(name = "ID", description = "A unique ID")
    @StringParameterSettings(monospace = true)
    public String getId() {
        return id;
    }

    /**
     * Sets the ID
     *
     * @param id The ID
     */
    @JsonSetter("id")
    @ACAQParameter("id")
    public void setId(String id) {
        this.id = id;
        eventBus.post(new ParameterChangedEvent(this, "id"));
    }

    @Override
    public Class<? extends ACAQTrait> getTraitClass() {
        return traitClass;
    }

    /**
     * Sets the trait class
     *
     * @param traitClass The trait class
     */
    public void setTraitClass(Class<? extends ACAQTrait> traitClass) {
        this.traitClass = traitClass;
    }

    @Override
    public boolean isDiscriminator() {
        return discriminator;
    }

    /**
     * Sets if the declaration is a discriminator
     *
     * @param discriminator Sets if the declaration is a discriminator
     */
    public void setDiscriminator(boolean discriminator) {
        this.discriminator = discriminator;
    }

    @Override
    @JsonGetter("name")
    @ACAQParameter("name")
    @ACAQDocumentation(name = "Name", description = "The name of this annotation type")
    public String getName() {
        return name;
    }

    /**
     * Sets the name
     *
     * @param name The name
     */
    @JsonSetter("name")
    @ACAQParameter("name")
    public void setName(String name) {
        this.name = name;
        eventBus.post(new ParameterChangedEvent(this, "name"));
    }

    @Override
    @JsonGetter("description")
    @ACAQParameter("description")
    @ACAQDocumentation(name = "Description", description = "The description of this annotation type")
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description
     *
     * @param description The description
     */
    @JsonSetter("description")
    @ACAQParameter("description")
    public void setDescription(String description) {
        this.description = description;
        eventBus.post(new ParameterChangedEvent(this, "description"));
    }

    @Override
    public Set<ACAQTraitDeclaration> getInherited() {
        return inherited;
    }

    /**
     * Sets the inherited
     *
     * @param inherited The inherited traits
     */
    public void setInherited(Set<ACAQTraitDeclaration> inherited) {
        this.inherited = inherited;
    }

    @Override
    @JsonGetter("hidden")
    @ACAQParameter("hidden")
    @ACAQDocumentation(name = "Is hidden", description = "If true, users will not be able to assign this annotation manually")
    public boolean isHidden() {
        return hidden;
    }

    /**
     * Sets if the trait should be hidden from the user
     *
     * @param hidden Sets if the trait should be hidden from the user
     */
    @JsonSetter("hidden")
    @ACAQParameter("hidden")
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
        eventBus.post(new ParameterChangedEvent(this, "hidden"));
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public String toString() {
        return "" + id;
    }
}
