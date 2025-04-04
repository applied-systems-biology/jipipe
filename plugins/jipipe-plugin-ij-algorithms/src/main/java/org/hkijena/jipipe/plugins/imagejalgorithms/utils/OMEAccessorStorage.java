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

package org.hkijena.jipipe.plugins.imagejalgorithms.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import ome.xml.meta.MetadataRetrieve;
import org.apache.commons.text.WordUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.ImageJAlgorithmsPlugin;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class OMEAccessorStorage {

    private final BiMap<String, OMEAccessorTemplate> templateMap = HashBiMap.create();

    public OMEAccessorStorage() {

    }

    public void initialize(JIPipeProgressInfo progressInfo) {
        templateMap.clear();

        Map<String, Class<?>> typeNameMap = new HashMap<>();
        typeNameMap.put("int", int.class);

        try {
            JsonNode rootNode = JsonUtils.getObjectMapper().readerFor(JsonNode.class).readValue(ImageJAlgorithmsPlugin.RESOURCES.getResourceAsStream("ome/metadata-retrieve.json"));
            outer:
            for (JsonNode entry : ImmutableList.copyOf(rootNode.elements())) {
                String methodName = entry.get("name").textValue();
                String methodDescription = entry.get("description").textValue() + "\n" + entry.get("return_description").textValue();
                String methodLabel = WordUtils.capitalize(String.join(" ",
                        org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase(methodName.substring(3))));

                JIPipeProgressInfo methodProgress = progressInfo.resolveAndLog(methodName);

                ImmutableList<Map.Entry<String, JsonNode>> parameterDefinitionNodes = ImmutableList.copyOf(entry.get("params").fields());

                String[] parameterIds = new String[parameterDefinitionNodes.size()];
                Class<?>[] parameterClasses = new Class[parameterDefinitionNodes.size()];
                JIPipeDynamicParameterCollection dynamicParameterCollection = new JIPipeDynamicParameterCollection(false);

                for (Map.Entry<String, JsonNode> nodeEntry : parameterDefinitionNodes) {
                    String parameterName = nodeEntry.getValue().get("name").textValue();
                    String parameterDescription = nodeEntry.getValue().get("description").textValue();
                    int parameterIndex = nodeEntry.getValue().get("index").intValue();
                    String parameterType = nodeEntry.getValue().get("type").textValue();

                    Class<?> parameterClass = typeNameMap.getOrDefault(parameterType, null);
                    if (parameterClass == null) {
                        methodProgress.log("Skipped, as parameter type " + parameterType + " is not supported!");
                        continue outer;
                    }
                    JIPipeParameterTypeInfo parameterTypeInfo = JIPipe.getParameterTypes().getInfoByFieldClass(parameterClass);
                    if (parameterTypeInfo == null) {
                        methodProgress.log("Skipped, as parameter type " + parameterType + " is not supported!");
                        continue outer;
                    }
                    String parameterId = "param-" + parameterIndex + "-" + String.join("-",
                            org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase(parameterName)).toLowerCase(Locale.ROOT);
                    parameterIds[parameterIndex] = parameterId;
                    parameterClasses[parameterIndex] = parameterClass;

                    dynamicParameterCollection.addParameter(parameterId, parameterClass,
                            WordUtils.capitalize(String.join(" ", org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase(parameterName))),
                            parameterDescription);
                }

                try {
                    Method method = MetadataRetrieve.class.getMethod(methodName, parameterClasses);

                    methodProgress.log("Registered as '" + methodLabel + "' with " + dynamicParameterCollection.getParameters().size() + " parameters");
                    OMEAccessorTemplate template = new OMEAccessorTemplate(methodLabel, methodDescription, method, dynamicParameterCollection, Arrays.asList(parameterIds));
                    templateMap.put(methodName, template);
                } catch (NoSuchMethodException e) {
                    methodProgress.log("Skipped, as the method does not exist!");
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, OMEAccessorTemplate> getTemplateMap() {
        return templateMap;
    }
}
