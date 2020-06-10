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
import org.hkijena.acaq5.ui.registries.ACAQUIAlgorithmRegistry;

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
     *  @param id             The id
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
        ACAQJavaAlgorithmDeclaration declaration = new ACAQJavaAlgorithmDeclaration(id, algorithmClass);
        ACAQAlgorithmRegistry.getInstance().register(declaration, source);
        if(icon != null)
            ACAQUIAlgorithmRegistry.getInstance().registerIcon(declaration, icon);
    }

    @Override
    public String toString() {
        return id + " @ " + algorithmClass;
    }
}
