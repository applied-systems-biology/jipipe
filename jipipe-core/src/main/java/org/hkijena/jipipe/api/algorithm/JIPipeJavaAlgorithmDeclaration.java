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

package org.hkijena.jipipe.api.algorithm;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHidden;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Set;

/**
 * {@link JIPipeAlgorithmDeclaration} for an algorithm that is defined in Java code
 * All necessary properties are extracted from class attributes
 */
@JsonSerialize(using = JIPipeJavaAlgorithmDeclaration.Serializer.class)
public class JIPipeJavaAlgorithmDeclaration extends JIPipeMutableAlgorithmDeclaration {

    /**
     * Creates a new algorithm declaration
     *
     * @param id             Algorithm ID
     * @param algorithmClass Algorithm class
     */
    public JIPipeJavaAlgorithmDeclaration(String id, Class<? extends JIPipeGraphNode> algorithmClass) {
        setAlgorithmClass(algorithmClass);
        setId(id);
        setName(getNameOf(algorithmClass));
        setDescription(getDescriptionOf(algorithmClass));
        setCategory(getCategoryOf(algorithmClass));
        setMenuPath(getMenuPathOf(algorithmClass));
        if (algorithmClass.getAnnotation(JIPipeHidden.class) != null) {
            setHidden(true);
        }
        initializeSlots();
    }

    private void initializeSlots() {
        for (AlgorithmInputSlot slot : getAlgorithmClass().getAnnotationsByType(AlgorithmInputSlot.class)) {
            getInputSlots().add(slot);
        }
        for (AlgorithmOutputSlot slot : getAlgorithmClass().getAnnotationsByType(AlgorithmOutputSlot.class)) {
            getOutputSlots().add(slot);
        }
    }

    @Override
    public JIPipeGraphNode clone(JIPipeGraphNode algorithm) {
        try {
            return ConstructorUtils.getMatchingAccessibleConstructor(getAlgorithmClass(), algorithm.getClass()).newInstance(algorithm);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new UserFriendlyRuntimeException(e, "Unable to copy algorithm '" + algorithm.getName() + "'!",
                    "Undefined", "There is a programming error in the algorithm's code.",
                    "Please contact the developer of the plugin that created the algorithm.");
        }
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public JIPipeGraphNode newInstance() {
        try {
            return getAlgorithmClass().getConstructor(JIPipeAlgorithmDeclaration.class).newInstance(this);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new UserFriendlyRuntimeException(e, "Unable to create an algorithm instance!",
                    "Undefined", "There is a programming error in an algorithm's code.",
                    "Please contact the developer of the plugin that created the algorithm.");
        }
    }

    /**
     * Returns the name of an algorithm
     *
     * @param klass Algorithm class
     * @return The name
     */
    public static String getNameOf(Class<? extends JIPipeGraphNode> klass) {
        JIPipeDocumentation[] annotations = klass.getAnnotationsByType(JIPipeDocumentation.class);
        if (annotations.length > 0) {
            return annotations[0].name();
        } else {
            return klass.getSimpleName();
        }
    }

    /**
     * Returns the description of an algorithm
     *
     * @param klass The algorithm class
     * @return The name
     */
    public static String getDescriptionOf(Class<? extends JIPipeGraphNode> klass) {
        JIPipeDocumentation[] annotations = klass.getAnnotationsByType(JIPipeDocumentation.class);
        if (annotations.length > 0) {
            return annotations[0].description();
        } else {
            return null;
        }
    }

    /**
     * Returns the category of an algorithm
     *
     * @param klass The algorithm class
     * @return The category
     */
    public static JIPipeAlgorithmCategory getCategoryOf(Class<? extends JIPipeGraphNode> klass) {
        JIPipeOrganization[] annotations = klass.getAnnotationsByType(JIPipeOrganization.class);
        if (annotations.length > 0) {
            return annotations[0].algorithmCategory();
        } else {
            return JIPipeAlgorithmCategory.Internal;
        }
    }

    /**
     * Returns the menu path of the algorithm
     *
     * @param klass The algorithm class
     * @return The menu path
     */
    static String getMenuPathOf(Class<? extends JIPipeGraphNode> klass) {
        JIPipeOrganization[] annotations = klass.getAnnotationsByType(JIPipeOrganization.class);
        if (annotations.length > 0) {
            return annotations[0].menuPath();
        } else {
            return "";
        }
    }

    /**
     * Serializes the declaration
     */
    public static class Serializer extends JsonSerializer<JIPipeJavaAlgorithmDeclaration> {
        @Override
        public void serialize(JIPipeJavaAlgorithmDeclaration declaration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("jipipe:algorithm-class", declaration.getAlgorithmClass().getCanonicalName());
            jsonGenerator.writeEndObject();
        }
    }
}
