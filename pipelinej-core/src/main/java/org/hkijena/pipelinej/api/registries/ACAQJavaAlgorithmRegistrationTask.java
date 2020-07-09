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

import org.hkijena.pipelinej.ACAQDependency;
import org.hkijena.pipelinej.api.algorithm.ACAQGraphNode;
import org.hkijena.pipelinej.api.algorithm.ACAQJavaAlgorithmDeclaration;
import org.hkijena.pipelinej.api.algorithm.AlgorithmInputSlot;
import org.hkijena.pipelinej.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.pipelinej.ui.registries.ACAQUIAlgorithmRegistry;

import java.net.URL;

/**
 * Registers a Java algorithm
 */
public class ACAQJavaAlgorithmRegistrationTask extends ACAQDefaultAlgorithmRegistrationTask {

    private ACAQDependency source;
    private String id;
    private Class<? extends ACAQGraphNode> algorithmClass;
    private URL icon;
    private boolean alreadyRegistered = false;

    /**
     * Creates a new registration task
     *
     * @param id             The id
     * @param algorithmClass The algorithm class
     * @param source         The dependency the registers the algorithm
     * @param icon
     */
    public ACAQJavaAlgorithmRegistrationTask(String id, Class<? extends ACAQGraphNode> algorithmClass, ACAQDependency source, URL icon) {
        this.source = source;
        this.id = id;
        this.algorithmClass = algorithmClass;
        this.icon = icon;

        for (AlgorithmInputSlot slot : algorithmClass.getAnnotationsByType(AlgorithmInputSlot.class)) {
            getDependencyDatatypeClasses().add(slot.value());
        }
        for (AlgorithmOutputSlot slot : algorithmClass.getAnnotationsByType(AlgorithmOutputSlot.class)) {
            getDependencyDatatypeClasses().add(slot.value());
        }
    }

    @Override
    public void register() {
        if (alreadyRegistered)
            return;
        alreadyRegistered = true;
        ACAQJavaAlgorithmDeclaration declaration = new ACAQJavaAlgorithmDeclaration(id, algorithmClass);
        ACAQAlgorithmRegistry.getInstance().register(declaration, source);
        if (icon != null)
            ACAQUIAlgorithmRegistry.getInstance().registerIcon(declaration, icon);
    }

    @Override
    public String toString() {
        return id + " @ " + algorithmClass;
    }
}
