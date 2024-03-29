/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.grouping.parameters;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterSerializationMode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A parameter access that references to another one, but hides the source
 */
public class GraphNodeParameterReferenceAccess implements JIPipeParameterAccess, JIPipeParameterCollection.ParameterChangedEventListener {

    private final GraphNodeParameterReference reference;
    private final JIPipeParameterTree tree;
    private final JIPipeParameterCollection alternativeSource;
    private final JIPipeParameterAccess target;
    private final JIPipeParameterSerializationMode persistence;


    /**
     * Creates a new instance
     *
     * @param reference         the reference
     * @param tree              the tree that is referenced
     * @param alternativeSource the source they are attached to
     * @param persistent        if the values are persistent (saved by the algorithm)
     */
    public GraphNodeParameterReferenceAccess(GraphNodeParameterReference reference, JIPipeParameterTree tree, JIPipeParameterCollection alternativeSource, boolean persistent) {
        this.reference = reference;
        this.tree = tree;
        this.persistence = persistent ? JIPipeParameterSerializationMode.Default : JIPipeParameterSerializationMode.None;
        this.target = reference.resolve(tree);
        this.alternativeSource = alternativeSource;
        this.target.getSource().getParameterChangedEventEmitter().subscribeWeak(this);
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
    public boolean isHidden() {
        return target.isHidden();
    }

    @Override
    public boolean isImportant() {
        return false;
    }

    @Override
    public <T extends Annotation> T getAnnotationOfType(Class<T> klass) {
        return target.getAnnotationOfType(klass);
    }

    @Override
    public <T extends Annotation> List<T> getAnnotationsOfType(Class<T> klass) {
        return target.getAnnotationsOfType(klass);
    }

    @Override
    public Collection<Annotation> getAnnotations() {
        return target.getAnnotations();
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
    public JIPipeParameterCollection getSource() {
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
    public JIPipeParameterSerializationMode getPersistence() {
        return persistence;
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (Objects.equals(target.getKey(), event.getKey())) {
            alternativeSource.getParameterChangedEventEmitter().emit(new JIPipeParameterCollection.ParameterChangedEvent(alternativeSource, getKey()));
        }
    }
}
