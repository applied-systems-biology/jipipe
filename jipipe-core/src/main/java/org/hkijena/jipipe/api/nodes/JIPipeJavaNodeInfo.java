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

package org.hkijena.jipipe.api.nodes;

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
import org.hkijena.jipipe.api.nodes.categories.InternalNodeTypeCategory;
import org.hkijena.jipipe.utils.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Set;

/**
 * {@link JIPipeNodeInfo} for an algorithm that is defined in Java code
 * All necessary properties are extracted from class attributes
 */
@JsonSerialize(using = JIPipeJavaNodeInfo.Serializer.class)
public class JIPipeJavaNodeInfo extends JIPipeMutableNodeInfo {

    /**
     * Creates a new node type info
     *
     * @param id        Algorithm ID
     * @param nodeClass Algorithm class
     */
    public JIPipeJavaNodeInfo(String id, Class<? extends JIPipeGraphNode> nodeClass) {
        setnodeClass(nodeClass);
        setId(id);
        setName(getNameOf(nodeClass));
        setDescription(getDescriptionOf(nodeClass));
        setCategory(getCategoryOf(nodeClass));
        setMenuPath(getMenuPathOf(nodeClass));
        if (nodeClass.getAnnotation(JIPipeHidden.class) != null) {
            setHidden(true);
        }
        initializeSlots();
    }

    private void initializeSlots() {
        for (JIPipeInputSlot slot : getInstanceClass().getAnnotationsByType(JIPipeInputSlot.class)) {
            getInputSlots().add(slot);
        }
        for (JIPipeOutputSlot slot : getInstanceClass().getAnnotationsByType(JIPipeOutputSlot.class)) {
            getOutputSlots().add(slot);
        }
    }

    @Override
    public JIPipeGraphNode duplicate(JIPipeGraphNode algorithm) {
        try {
            return ConstructorUtils.getMatchingAccessibleConstructor(getInstanceClass(), algorithm.getClass()).newInstance(algorithm);
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
            return getInstanceClass().getConstructor(JIPipeNodeInfo.class).newInstance(this);
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
    public static JIPipeNodeTypeCategory getCategoryOf(Class<? extends JIPipeGraphNode> klass) {
        JIPipeOrganization[] annotations = klass.getAnnotationsByType(JIPipeOrganization.class);
        if (annotations.length > 0) {
            Class<? extends JIPipeNodeTypeCategory> categoryClass = annotations[0].nodeTypeCategory();
            return (JIPipeNodeTypeCategory) ReflectionUtils.newInstance(categoryClass);
        } else {
            return new InternalNodeTypeCategory();
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
     * Serializes the info
     */
    public static class Serializer extends JsonSerializer<JIPipeJavaNodeInfo> {
        @Override
        public void serialize(JIPipeJavaNodeInfo info, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("jipipe:algorithm-class", info.getInstanceClass().getCanonicalName());
            jsonGenerator.writeEndObject();
        }
    }
}
