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

package org.hkijena.jipipe.api.registries;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.CustomValidationReportContext;

import java.util.HashSet;
import java.util.Set;

/**
 * Mutable implementation of {@link JIPipeNodeRegistrationTask}
 */
public abstract class JIPipeDefaultNodeRegistrationTask implements JIPipeNodeRegistrationTask {
    private Set<String> dependencyAlgorithmIds = new HashSet<>();
    private Set<String> dependencyDatatypeIds = new HashSet<>();
    private Set<Class<? extends JIPipeData>> dependencyDatatypeClasses = new HashSet<>();

    /**
     * Creates a new task
     */
    public JIPipeDefaultNodeRegistrationTask() {
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
            if (!JIPipe.getNodes().hasNodeInfoWithId(id))
                return false;
        }
        for (String id : dependencyDatatypeIds) {
            if (!JIPipe.getDataTypes().hasDatatypeWithId(id))
                return false;
        }
        for (Class<? extends JIPipeData> dataClass : dependencyDatatypeClasses) {
            if (!JIPipe.getDataTypes().hasDataType(dataClass))
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
    public Set<Class<? extends JIPipeData>> getDependencyDatatypeClasses() {
        return dependencyDatatypeClasses;
    }

    /**
     * Sets dependency data classes. The task will wait until the data class is registered.
     *
     * @param dependencyDatatypeClasses Dependency data classes
     */
    public void setDependencyDatatypeClasses(Set<Class<? extends JIPipeData>> dependencyDatatypeClasses) {
        this.dependencyDatatypeClasses = dependencyDatatypeClasses;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        for (String id : dependencyAlgorithmIds) {
            if (!JIPipe.getNodes().hasNodeInfoWithId(id))
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new CustomValidationReportContext(context, "Dependency algorithms"),
                        "A dependency is missing!",
                        "Dependency algorithm '" + id + "' is missing!",
                        "Please make sure to install dependency plugins."));
        }
        for (String id : dependencyDatatypeIds) {
            if (!JIPipe.getDataTypes().hasDatatypeWithId(id))
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new CustomValidationReportContext(context, "Dependency data types"),
                        "A dependency is missing!",
                        "Dependency data type '" + id + "' is missing!",
                        "Please make sure to install dependency plugins."));
        }
        for (Class<? extends JIPipeData> dataClass : dependencyDatatypeClasses) {
            if (!JIPipe.getDataTypes().hasDataType(dataClass))
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new CustomValidationReportContext(context, "Dependency data types"),
                        "A dependency is missing!",
                        "Dependency data type '" + dataClass.getCanonicalName() + "' is missing!",
                        "Please make sure to install dependency plugins."));
        }
    }
}
