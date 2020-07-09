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

package org.hkijena.pipelinej.api.registries;

import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.data.ACAQData;

import java.util.HashSet;
import java.util.Set;

/**
 * Mutable implementation of {@link ACAQAlgorithmRegistrationTask}
 */
public abstract class ACAQDefaultAlgorithmRegistrationTask implements ACAQAlgorithmRegistrationTask {
    private Set<String> dependencyAlgorithmIds = new HashSet<>();
    private Set<String> dependencyDatatypeIds = new HashSet<>();
    private Set<Class<? extends ACAQData>> dependencyDatatypeClasses = new HashSet<>();

    /**
     * Creates a new task
     */
    public ACAQDefaultAlgorithmRegistrationTask() {
    }

    /**
     * @return Dependency algorithm IDs
     */
    public Set<String> getDependencyAlgorithmIds() {
        return dependencyAlgorithmIds;
    }

    /**
     * @param dependencyAlgorithmIds IDs of dependency algorithms
     */
    public void setDependencyAlgorithmIds(Set<String> dependencyAlgorithmIds) {
        this.dependencyAlgorithmIds = dependencyAlgorithmIds;
    }

    @Override
    public boolean canRegister() {
        for (String id : dependencyAlgorithmIds) {
            if (!ACAQAlgorithmRegistry.getInstance().hasAlgorithmWithId(id))
                return false;
        }
        for (String id : dependencyDatatypeIds) {
            if (!ACAQDatatypeRegistry.getInstance().hasDatatypeWithId(id))
                return false;
        }
        for (Class<? extends ACAQData> dataClass : dependencyDatatypeClasses) {
            if (!ACAQDatatypeRegistry.getInstance().hasDataType(dataClass))
                return false;
        }

        return true;
    }

    /**
     * @return Dependency data type IDs
     */
    public Set<String> getDependencyDatatypeIds() {
        return dependencyDatatypeIds;
    }

    /**
     * @param dependencyDatatypeIds IDs of dependency data types
     */
    public void setDependencyDatatypeIds(Set<String> dependencyDatatypeIds) {
        this.dependencyDatatypeIds = dependencyDatatypeIds;
    }

    /**
     * @return Dependency data classes
     */
    public Set<Class<? extends ACAQData>> getDependencyDatatypeClasses() {
        return dependencyDatatypeClasses;
    }

    /**
     * Sets dependency data classes. The task will wait until the data class is registered.
     *
     * @param dependencyDatatypeClasses Dependency data classes
     */
    public void setDependencyDatatypeClasses(Set<Class<? extends ACAQData>> dependencyDatatypeClasses) {
        this.dependencyDatatypeClasses = dependencyDatatypeClasses;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        for (String id : dependencyAlgorithmIds) {
            if (!ACAQAlgorithmRegistry.getInstance().hasAlgorithmWithId(id))
                report.forCategory("Dependency Algorithms").reportIsInvalid("A dependency is missing!",
                        "Dependency algorithm '" + id + "' is missing!",
                        "Please make sure to install dependency plugins.",
                        this);
        }
        for (String id : dependencyDatatypeIds) {
            if (!ACAQDatatypeRegistry.getInstance().hasDatatypeWithId(id))
                report.forCategory("Dependency Data types").reportIsInvalid("A dependency is missing!",
                        "Dependency data type '" + id + "' is missing!",
                        "Please make sure to install dependency plugins.",
                        this);
        }
        for (Class<? extends ACAQData> dataClass : dependencyDatatypeClasses) {
            if (!ACAQDatatypeRegistry.getInstance().hasDataType(dataClass))
                report.forCategory("Dependency Data types").reportIsInvalid("A dependency is missing!",
                        "Dependency data type '" + dataClass.getCanonicalName() + "' is missing!",
                        "Please make sure to install dependency plugins.",
                        this);
        }
    }
}
