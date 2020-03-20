package org.hkijena.acaq5.api.registries;

import java.util.HashSet;
import java.util.Set;

public abstract class DefaultACAQAlgorithmRegistrationTask implements ACAQAlgorithmRegistrationTask {
    private Set<String> dependencyAlgorithmIds = new HashSet<>();
    private Set<String> dependencyTraitIds = new HashSet<>();
    private Set<String> dependencyDatatypeIds = new HashSet<>();

    public DefaultACAQAlgorithmRegistrationTask() {

    }

    public Set<String> getDependencyAlgorithmIds() {
        return dependencyAlgorithmIds;
    }

    public void setDependencyAlgorithmIds(Set<String> dependencyAlgorithmIds) {
        this.dependencyAlgorithmIds = dependencyAlgorithmIds;
    }

    public Set<String> getDependencyTraitIds() {
        return dependencyTraitIds;
    }

    public void setDependencyTraitIds(Set<String> dependencyTraitIds) {
        this.dependencyTraitIds = dependencyTraitIds;
    }

    @Override
    public boolean canRegister() {
        for (String id : dependencyAlgorithmIds) {
            if (!ACAQAlgorithmRegistry.getInstance().hasAlgorithmWithId(id))
                return false;
        }
        for (String id : dependencyTraitIds) {
            if (!ACAQTraitRegistry.getInstance().hasTraitWithId(id))
                return false;
        }
        for (String id : dependencyDatatypeIds) {
            if (!ACAQDatatypeRegistry.getInstance().hasDatatypeWithId(id))
                return false;
        }

        return true;
    }

    public Set<String> getDependencyDatatypeIds() {
        return dependencyDatatypeIds;
    }

    public void setDependencyDatatypeIds(Set<String> dependencyDatatypeIds) {
        this.dependencyDatatypeIds = dependencyDatatypeIds;
    }
}
