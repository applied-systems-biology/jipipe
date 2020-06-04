package org.hkijena.acaq5.api.registries;

import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.algorithm.ACAQJavaAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.traits.AddsTrait;
import org.hkijena.acaq5.api.data.traits.BadForTrait;
import org.hkijena.acaq5.api.data.traits.GoodForTrait;
import org.hkijena.acaq5.api.data.traits.RemovesTrait;

/**
 * Registers a Java algorithm
 */
public class ACAQJavaAlgorithmRegistrationTask extends ACAQDefaultAlgorithmRegistrationTask {

    private ACAQDependency source;
    private String id;
    private Class<? extends ACAQGraphNode> algorithmClass;
    private boolean alreadyRegistered = false;

    /**
     * Creates a new registration task
     *
     * @param id             The id
     * @param algorithmClass The algorithm class
     * @param source         The dependency the registers the algorithm
     */
    public ACAQJavaAlgorithmRegistrationTask(String id, Class<? extends ACAQGraphNode> algorithmClass, ACAQDependency source) {
        this.source = source;
        this.id = id;
        this.algorithmClass = algorithmClass;

        for (AlgorithmInputSlot slot : algorithmClass.getAnnotationsByType(AlgorithmInputSlot.class)) {
            getDependencyDatatypeClasses().add(slot.value());
        }
        for (AlgorithmOutputSlot slot : algorithmClass.getAnnotationsByType(AlgorithmOutputSlot.class)) {
            getDependencyDatatypeClasses().add(slot.value());
        }
        for (GoodForTrait trait : algorithmClass.getAnnotationsByType(GoodForTrait.class)) {
            getDependencyTraitIds().add(trait.value());
        }
        for (BadForTrait trait : algorithmClass.getAnnotationsByType(BadForTrait.class)) {
            getDependencyTraitIds().add(trait.value());
        }
        for (AddsTrait trait : algorithmClass.getAnnotationsByType(AddsTrait.class)) {
            getDependencyTraitIds().add(trait.value());
        }
        for (RemovesTrait trait : algorithmClass.getAnnotationsByType(RemovesTrait.class)) {
            getDependencyTraitIds().add(trait.value());
        }
    }

    @Override
    public void register() {
        if (alreadyRegistered)
            return;
        alreadyRegistered = true;
        ACAQAlgorithmRegistry.getInstance().register(new ACAQJavaAlgorithmDeclaration(id, algorithmClass), source);
    }

    @Override
    public String toString() {
        return id + " @ " + algorithmClass;
    }
}
