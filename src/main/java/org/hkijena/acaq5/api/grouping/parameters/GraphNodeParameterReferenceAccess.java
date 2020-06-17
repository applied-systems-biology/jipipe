package org.hkijena.acaq5.api.grouping.parameters;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.api.parameters.ACAQParameterVisibility;

import java.lang.annotation.Annotation;

/**
 * A parameter access that references to another one, but hides the source
 * Note: This is marked as non-persistent!
 */
public class GraphNodeParameterReferenceAccess implements ACAQParameterAccess {

    private final GraphNodeParameterReference reference;
    private final ACAQParameterTree tree;
    private final ACAQParameterCollection alternativeSource;
    private final ACAQParameterAccess target;
    private final boolean persistent;


    /**
     * Creates a new instance
     *
     * @param reference         the reference
     * @param tree              the tree that is referenced
     * @param alternativeSource the source they are attached to
     * @param persistent        if the values are persistent (saved by the algorithm)
     */
    public GraphNodeParameterReferenceAccess(GraphNodeParameterReference reference, ACAQParameterTree tree, ACAQParameterCollection alternativeSource, boolean persistent) {
        this.reference = reference;
        this.tree = tree;
        this.persistent = persistent;
        this.target = reference.resolve(tree);
        this.alternativeSource = alternativeSource;
    }

    @Override
    public String getKey() {
        return target.getKey();
    }

    @Override
    public String getName() {
        return reference.getName(tree);
    }

    @Override
    public String getDescription() {
        return reference.getDescription(tree);
    }

    @Override
    public ACAQParameterVisibility getVisibility() {
        return target.getVisibility();
    }

    @Override
    public <T extends Annotation> T getAnnotationOfType(Class<T> klass) {
        return target.getAnnotationOfType(klass);
    }

    @Override
    public Class<?> getFieldClass() {
        return target.getFieldClass();
    }

    @Override
    public <T> T get(Class<T> klass) {
        return target.get(klass);
    }

    @Override
    public <T> boolean set(T value) {
        return target.set(value);
    }

    @Override
    public ACAQParameterCollection getSource() {
        return alternativeSource;
    }

    @Override
    public double getPriority() {
        return 0;
    }

    @Override
    public String getShortKey() {
        return null;
    }

    @Override
    public int getUIOrder() {
        return 0;
    }

    @Override
    public boolean isPersistent() {
        return persistent;
    }
}
