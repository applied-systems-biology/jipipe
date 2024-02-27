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

package org.hkijena.jipipe.extensions.forms.datatypes;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeDummyParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeManualParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.extensions.forms.utils.SingleAnnotationIOSettings;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameter;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.DynamicStringEnumParameter;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.Collections;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Selection input form", description = "A form element that allows the user to select one of multiple options")
public class EnumFormData extends ParameterFormData {

    private String value = "";
    private SingleAnnotationIOSettings annotationIOSettings = new SingleAnnotationIOSettings();
    private StringAndStringPairParameter.List items = new StringAndStringPairParameter.List();

    public EnumFormData() {
    }

    public EnumFormData(EnumFormData other) {
        super(other);
        this.value = other.value;
        this.annotationIOSettings = new SingleAnnotationIOSettings(other.annotationIOSettings);
        this.items = new StringAndStringPairParameter.List(other.items);
    }

    public static EnumFormData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return FormData.importData(storage, EnumFormData.class, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Initial value", description = "The initial value. Should be the annotation value of the item.")
    @JIPipeParameter("initial-value")
    public String getValue() {
        return value;
    }

    @JIPipeParameter("initial-value")
    public void setValue(String value) {
        this.value = value;
    }

    @SetJIPipeDocumentation(name = "Form element I/O", description = "Use following settings to determine how to extract initial values " +
            "from annotations and where to store the user-defined value.")
    @JIPipeParameter("form:io")
    public SingleAnnotationIOSettings getAnnotationIOSettings() {
        return annotationIOSettings;
    }

    @SetJIPipeDocumentation(name = "Items", description = "List of available items. For each entry, you " +
            "can define the value written into the annotations and the label visible to the user. Duplicate labels/values are not allowed.")
    @JIPipeParameter("items")
    @PairParameterSettings(singleRow = false, keyLabel = "Annotation value", valueLabel = "Label")
    public StringAndStringPairParameter.List getItems() {
        return items;
    }

    @JIPipeParameter("items")
    public void setItems(StringAndStringPairParameter.List items) {
        this.items = items;
    }

    @Override
    public Component getEditor(JIPipeWorkbench workbench) {
        BiMap<String, String> itemMap = HashBiMap.create();
        for (StringAndStringPairParameter item : items) {
            itemMap.put(item.getKey(), item.getValue());
        }
        DynamicStringEnumParameter instance = new DynamicStringEnumParameter() {
            @Override
            public String renderLabel(String value) {
                return itemMap.getOrDefault(value, "<Not found: " + value + ">");
            }
        };
        instance.setAllowedValues(items.stream().map(PairParameter::getKey).collect(Collectors.toList()));
        instance.setValue(this.value);
        JIPipeManualParameterAccess access = JIPipeManualParameterAccess.builder().setGetter(() -> instance)
                .setSetter((obj) -> {
                    this.value = instance.getValue();
                    return true;
                })
                .setName(getName())
                .setDescription(getDescription().getBody())
                .setSource(new JIPipeDummyParameterCollection())
                .setFieldClass(DynamicStringEnumParameter.class)
                .build();
        return JIPipe.getParameterTypes().createEditorFor(workbench, new JIPipeParameterTree(access), access);
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new EnumFormData(this);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
    }

    @Override
    public String toString() {
        return String.format("Selection form [name=%s, value=%s]", getName(), value);
    }

    @Override
    public void loadData(JIPipeMultiIterationStep iterationStep) {
        if (annotationIOSettings.getInputAnnotation().isEnabled()) {
            JIPipeTextAnnotation annotation =
                    iterationStep.getMergedTextAnnotations().getOrDefault(annotationIOSettings.getInputAnnotation().getContent(),
                            null);
            if (annotation != null) {
                this.value = StringUtils.nullToEmpty(annotation.getValue());
            }
        }
    }

    @Override
    public void writeData(JIPipeMultiIterationStep iterationStep) {
        if (annotationIOSettings.getOutputAnnotation().isEnabled()) {
            annotationIOSettings.getAnnotationMergeStrategy().mergeInto(iterationStep.getMergedTextAnnotations(),
                    Collections.singletonList(annotationIOSettings.getOutputAnnotation().createAnnotation(value)));
        }
    }
}
