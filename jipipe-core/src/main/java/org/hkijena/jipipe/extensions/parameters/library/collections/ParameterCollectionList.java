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

package org.hkijena.jipipe.extensions.parameters.library.collections;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.util.*;

/**
 * A list of multiple {@link org.hkijena.jipipe.api.parameters.JIPipeParameterCollection} items (internally {@link org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection}.
 * We suggest to use this parameter instead of defining custom pairs ({@link org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameter} or functions ({@link org.hkijena.jipipe.extensions.parameters.api.functions.FunctionParameter}
 * <p>
 * You should provide a template {@link JIPipeParameterCollection} class (that is instantiable) to automatically populate newly created {@link JIPipeDynamicParameterCollection} with values. Provide this class via the containingCollection() static method
 */
@JsonSerialize(using = ParameterCollectionList.Serializer.class)
@JsonDeserialize(using = ParameterCollectionList.Deserializer.class)
public class ParameterCollectionList extends ListParameter<JIPipeDynamicParameterCollection> {

    private JIPipeDynamicParameterCollection template = new JIPipeDynamicParameterCollection(false);

    public ParameterCollectionList() {
        super(JIPipeDynamicParameterCollection.class);
    }

    public ParameterCollectionList(ParameterCollectionList other) {
        super(JIPipeDynamicParameterCollection.class);
        this.template = new JIPipeDynamicParameterCollection(other.template);
        for (JIPipeDynamicParameterCollection collection : other) {
            add(new JIPipeDynamicParameterCollection(collection));
        }
    }

    /**
     * Creates an instance from a template collection class.
     * Each item contains the same parameters as the template.
     * Please note that nested parameters (groups) are flattened.
     *
     * @param klass the template collection. must be instantiable.
     * @return the list
     */
    public static ParameterCollectionList containingCollection(Class<? extends JIPipeParameterCollection> klass) {
        ParameterCollectionList list = new ParameterCollectionList();
        JIPipeParameterCollection templateCollection = (JIPipeParameterCollection) ReflectionUtils.newInstance(klass);
        JIPipeParameterTree tree = new JIPipeParameterTree(templateCollection);
        for (Map.Entry<String, JIPipeParameterAccess> entry : tree.getParameters().entrySet()) {
            JIPipeMutableParameterAccess parameterAccess = new JIPipeMutableParameterAccess(entry.getValue());
            list.getTemplate().addParameter(parameterAccess);
        }
        return list;
    }

    /**
     * Creates an instance that contains a parameter of a specific value
     *
     * @param klass the type of parameter that is contained
     * @return the list
     */
    public static ParameterCollectionList containingSingle(Class<?> klass) {
        ParameterCollectionList list = new ParameterCollectionList();
        list.getTemplate().addParameter("value", klass, "Value", "");
        return list;
    }

    /**
     * Returns the template object for new items
     *
     * @return the template
     */
    public JIPipeDynamicParameterCollection getTemplate() {
        return template;
    }

    /**
     * Sets the template for new items
     *
     * @param template the template
     */
    public void setTemplate(JIPipeDynamicParameterCollection template) {
        this.template = template;
    }

    /**
     * Maps the items to a {@link JIPipeParameterCollection} type. Useful if you created this list based on a template.
     *
     * @param klass the collection class
     * @param <T>   the collection class
     * @return list of mapped objects
     */
    public <T extends JIPipeParameterCollection> List<T> mapToCollection(Class<T> klass) {
        List<T> result = new ArrayList<>();
        for (JIPipeDynamicParameterCollection source : this) {
            JIPipeParameterCollection target = (JIPipeParameterCollection) ReflectionUtils.newInstance(klass);
            JIPipeParameterTree tree = new JIPipeParameterTree(target);
            for (Map.Entry<String, JIPipeParameterAccess> entry : source.getParameters().entrySet()) {
                JIPipeParameterAccess targetAccess = tree.getParameters().getOrDefault(entry.getKey(), null);
                if (targetAccess == null || !targetAccess.getFieldClass().isAssignableFrom(entry.getValue().getFieldClass()))
                    continue;
                targetAccess.set(entry.getValue().get(Object.class));
            }
            result.add((T) target);
        }
        return result;
    }

    @Override
    public JIPipeDynamicParameterCollection addNewInstance() {
        if (getCustomInstanceGenerator() != null) {
            return super.addNewInstance();
        } else {
            JIPipeDynamicParameterCollection result = new JIPipeDynamicParameterCollection(false);
            for (Map.Entry<String, JIPipeParameterAccess> entry : template.getParameters().entrySet()) {
                JIPipeMutableParameterAccess copy = new JIPipeMutableParameterAccess(entry.getValue());
                result.addParameter(copy);
            }
            add(result);
            return result;
        }
    }

    public void addFromTemplate(JIPipeParameterCollection source) {
        JIPipeDynamicParameterCollection target = addNewInstance();
        JIPipeParameterTree tree = new JIPipeParameterTree(source);
        for (Map.Entry<String, JIPipeParameterAccess> entry : target.getParameters().entrySet()) {
            Object o = tree.getParameters().get(entry.getKey()).get(Object.class);
            entry.getValue().set(o);
        }
    }

    /**
     * Applies the entries of the current template to all items (adding/removing/changing items)
     */
    public void applyTemplateToItems() {
        Set<String> toRemove = new HashSet<>();
        Set<String> toAdd = new HashSet<>();
        for (JIPipeDynamicParameterCollection collection : this) {
            for (Map.Entry<String, JIPipeParameterAccess> entry : collection.getParameters().entrySet()) {
                JIPipeMutableParameterAccess itemAccess = (JIPipeMutableParameterAccess) entry.getValue();
                if (!template.containsKey(entry.getKey())) {
                    toRemove.add(entry.getKey());
                    continue;
                }
                JIPipeParameterAccess templateAccess = template.get(entry.getKey());
                if (templateAccess.getFieldClass() != itemAccess.getFieldClass()) {
                    toRemove.add(entry.getKey());
                    toAdd.add(entry.getKey());
                }
                itemAccess.setUIOrder(templateAccess.getUIOrder());
                if (templateAccess instanceof JIPipeMutableParameterAccess) {
                    itemAccess.setAnnotationMap(((JIPipeMutableParameterAccess) templateAccess).getAnnotationMap());
                }
            }
            for (Map.Entry<String, JIPipeParameterAccess> entry : template.getParameters().entrySet()) {
                if (!collection.containsKey(entry.getKey())) {
                    toAdd.add(entry.getKey());
                }
            }
            for (String key : toRemove) {
                collection.removeParameter(key);
            }
            for (String key : toAdd) {
                JIPipeParameterAccess source = template.get(key);
                JIPipeMutableParameterAccess copy = new JIPipeMutableParameterAccess(source);
                collection.addParameter(copy);
            }
        }
    }

    public static class Serializer extends JsonSerializer<ParameterCollectionList> {
        @Override
        public void serialize(ParameterCollectionList value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeObjectField("template", value.getTemplate());
            gen.writeArrayFieldStart("items");
            for (JIPipeDynamicParameterCollection item : value) {
                gen.writeObject(item);
            }
            gen.writeEndArray();
            gen.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<ParameterCollectionList> {

        @Override
        public ParameterCollectionList deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = p.readValueAsTree();
            ParameterCollectionList result = new ParameterCollectionList();
            result.template = JsonUtils.getObjectMapper().readerFor(JIPipeDynamicParameterCollection.class).readValue(node.get("template"));
            for (JsonNode itemNode : ImmutableList.copyOf(node.get("items").elements())) {
                JIPipeDynamicParameterCollection item = JsonUtils.getObjectMapper().readerFor(JIPipeDynamicParameterCollection.class).readValue(itemNode);
                result.add(item);
            }
            result.applyTemplateToItems();
            return result;
        }
    }
}
