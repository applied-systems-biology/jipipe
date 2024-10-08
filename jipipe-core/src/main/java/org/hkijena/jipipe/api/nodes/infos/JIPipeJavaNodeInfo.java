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

package org.hkijena.jipipe.api.nodes.infos;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeEmptyData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.InternalNodeTypeCategory;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.DocumentationUtils;
import org.hkijena.jipipe.utils.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        setDataSourceMenuLocation(getDataSourceMenuLocationOf(nodeClass));
        setAliases(getAliasesOf(nodeClass));
        if (nodeClass.getAnnotation(LabelAsJIPipeHidden.class) != null) {
            setHidden(true);
        }
        if (nodeClass.getAnnotation(Deprecated.class) != null) {
            setDeprecated(true);
        }
        setRunnable(JIPipeAlgorithm.class.isAssignableFrom(nodeClass));
        // Load additional citations
        for (AddJIPipeCitation citation : nodeClass.getAnnotationsByType(AddJIPipeCitation.class)) {
            getAdditionalCitations().add(citation.value());
        }
        initializeSlots();
    }

    /**
     * Returns the name of an algorithm
     *
     * @param klass Algorithm class
     * @return The name
     */
    public static String getNameOf(Class<? extends JIPipeGraphNode> klass) {
        SetJIPipeDocumentation[] annotations = klass.getAnnotationsByType(SetJIPipeDocumentation.class);
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
    public static HTMLText getDescriptionOf(Class<? extends JIPipeGraphNode> klass) {
        String rawDescription = DocumentationUtils.getDocumentationDescription(klass);
        return new HTMLText(rawDescription.replace("\n", "<br/>"));
    }

    /**
     * Returns the category of an algorithm
     *
     * @param klass The algorithm class
     * @return The category
     */
    public static JIPipeNodeTypeCategory getCategoryOf(Class<? extends JIPipeGraphNode> klass) {
        ConfigureJIPipeNode[] annotations = klass.getAnnotationsByType(ConfigureJIPipeNode.class);
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
    public static String getMenuPathOf(Class<? extends JIPipeGraphNode> klass) {
        ConfigureJIPipeNode[] annotations = klass.getAnnotationsByType(ConfigureJIPipeNode.class);
        if (annotations.length > 0) {
            return annotations[0].menuPath();
        } else {
            return "";
        }
    }

    /**
     * Returns the alternative assignment to another data source type menu for a node
     * Only applicable if the node is of node category {@link org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory}
     * {@link JIPipeEmptyData} means that no re-assignment should be applied
     *
     * @param klass The data class
     * @return The menu path of the data class
     */
    static Class<? extends JIPipeData> getDataSourceMenuLocationOf(Class<? extends JIPipeGraphNode> klass) {
        ConfigureJIPipeNode[] annotations = klass.getAnnotationsByType(ConfigureJIPipeNode.class);
        if (annotations.length > 0) {
            return annotations[0].dataSourceMenuLocation();
        } else {
            return JIPipeEmptyData.class;
        }
    }

    /**
     * Gets alternative menu locations
     *
     * @param klass node class
     * @return locations
     */
    public static List<JIPipeNodeMenuLocation> getAliasesOf(Class<? extends JIPipeGraphNode> klass) {
        List<JIPipeNodeMenuLocation> result = new ArrayList<>();
        for (AddJIPipeNodeAlias location : klass.getAnnotationsByType(AddJIPipeNodeAlias.class)) {
            result.add(new JIPipeNodeMenuLocation((JIPipeNodeTypeCategory) ReflectionUtils.newInstance(location.nodeTypeCategory()), location.menuPath(), location.aliasName()));
        }
        return result;
    }

    private void initializeSlots() {
        for (AddJIPipeInputSlot slot : getInstanceClass().getAnnotationsByType(AddJIPipeInputSlot.class)) {
            getInputSlots().add(slot);
        }
        for (AddJIPipeOutputSlot slot : getInstanceClass().getAnnotationsByType(AddJIPipeOutputSlot.class)) {
            getOutputSlots().add(slot);
        }
    }

    @Override
    public JIPipeGraphNode duplicate(JIPipeGraphNode algorithm) {
        try {
            return ConstructorUtils.getMatchingAccessibleConstructor(getInstanceClass(), algorithm.getClass()).newInstance(algorithm);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new JIPipeValidationRuntimeException(e, "Unable to copy node '" + algorithm.getName() + "'!",
                    "There is a programming error in the node's code.",
                    "Please contact the developer of the plugin that created the node.");
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
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            throw new JIPipeValidationRuntimeException(e, "Unable to create a node instance!",
                    "There is a programming error in an node's code.",
                    "Please contact the developer of the plugin that created the node.");
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
