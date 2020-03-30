package org.hkijena.acaq5.api.traits;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.HashSet;
import java.util.Set;

public class ACAQJsonTraitDeclaration extends ACAQMutableTraitDeclaration implements ACAQValidatable {

    private Set<String> inheritedIds = new HashSet<>();
    private ACAQTraitIconRef traitIcon = new ACAQTraitIconRef();

    @Override
    public ACAQTrait newInstance() {
        if (isDiscriminator())
            return new ACAQDefaultDiscriminator(this, "");
        else
            return new ACAQDefaultTrait(this);
    }

    @Override
    public ACAQTrait newInstance(String value) {
        if (isDiscriminator())
            return new ACAQDefaultDiscriminator(this, value);
        else
            return new ACAQDefaultTrait(this);
    }

    @Override
    public Set<ACAQDependency> getDependencies() {
        updatedInheritedDeclarations();
        Set<ACAQDependency> dependencies = new HashSet<>();
        for (ACAQTraitDeclaration declaration : getInherited()) {
            ACAQDependency source = ACAQTraitRegistry.getInstance().getSourceOf(declaration.getId());
            if (source != null) {
                dependencies.add(source);
            }
        }
        return dependencies;
    }

    @Override
    @JsonGetter("is-discriminator")
    @ACAQParameter("is-discriminator")
    @ACAQDocumentation(name = "Contains value", description = "If true, this annotation can store a string value.")
    public boolean isDiscriminator() {
        return super.isDiscriminator();
    }

    @Override
    @JsonSetter("is-discriminator")
    @ACAQParameter("is-discriminator")
    public void setDiscriminator(boolean discriminator) {
        super.setDiscriminator(discriminator);
        if (discriminator) {
            setTraitClass(ACAQDefaultDiscriminator.class);
        } else {
            setTraitClass(ACAQDefaultTrait.class);
        }
        getEventBus().post(new ParameterChangedEvent(this, "is-discriminator"));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (StringUtils.isNullOrEmpty(getName()))
            report.forCategory("Name").reportIsInvalid("The name is empty! Please provide a meaningful name.");
        if (StringUtils.isNullOrEmpty(getId()))
            report.forCategory("ID").reportIsInvalid("The ID is empty! Please provide a unique ID.");
        if (ACAQTraitRegistry.getInstance().hasTraitWithId(getId()) && isDiscriminator() != ACAQTraitRegistry.getInstance().getDeclarationById(getId()).isDiscriminator()) {
            report.forCategory("Contains value").reportIsInvalid("There is already an existing annotation type with a different value behavior!" +
                    " Please make sure that same IDs have same behaviors.");
        }
    }

    @JsonGetter("inherited-annotation-ids")
    public Set<String> getInheritedIds() {
        return inheritedIds;
    }

    @JsonSetter("inherited-annotation-ids")
    public void setInheritedIds(Set<String> inheritedIds) {
        this.inheritedIds = inheritedIds;
    }

    public void updatedInheritedDeclarations() {
        getInherited().clear();
        for (String id : inheritedIds) {
            if (ACAQTraitRegistry.getInstance().hasTraitWithId(id)) {
                getInherited().add(ACAQTraitRegistry.getInstance().getDeclarationById(id));
            }
        }
    }

    @JsonGetter("icon")
    @ACAQDocumentation(name="Icon", description = "Icon of this annotation type")
    @ACAQParameter("icon")
    public ACAQTraitIconRef getTraitIcon() {
        return traitIcon;
    }

    @JsonSetter("icon")
    @ACAQParameter("icon")
    public void setTraitIcon(ACAQTraitIconRef traitIcon) {
        this.traitIcon = traitIcon;
    }
}
