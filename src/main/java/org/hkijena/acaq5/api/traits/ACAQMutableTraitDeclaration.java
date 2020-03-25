package org.hkijena.acaq5.api.traits;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.HashSet;
import java.util.Set;

public abstract class ACAQMutableTraitDeclaration implements ACAQTraitDeclaration {
    private String id;
    private Class<? extends ACAQTrait> traitClass;
    private boolean discriminator;
    private boolean hidden;
    private String name;
    private String description;
    private Set<ACAQTraitDeclaration> inherited = new HashSet<>();

    @Override
    @JsonGetter("id")
    public String getId() {
        return id;
    }

    @JsonSetter("id")
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Class<? extends ACAQTrait> getTraitClass() {
        return traitClass;
    }

    public void setTraitClass(Class<? extends ACAQTrait> traitClass) {
        this.traitClass = traitClass;
    }

    @Override
    public boolean isDiscriminator() {
        return discriminator;
    }

    public void setDiscriminator(boolean discriminator) {
        this.discriminator = discriminator;
    }

    @Override
    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @Override
    @JsonGetter("description")
    public String getDescription() {
        return description;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Set<ACAQTraitDeclaration> getInherited() {
        return inherited;
    }

    public void setInherited(Set<ACAQTraitDeclaration> inherited) {
        this.inherited = inherited;
    }

    @Override
    @JsonGetter("hidden")
    public boolean isHidden() {
        return hidden;
    }

    @JsonSetter("hidden")
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}
