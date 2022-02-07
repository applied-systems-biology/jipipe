package org.hkijena.jipipe.extensions.forms.datatypes;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.parameters.JIPipeDummyParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeManualParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.forms.utils.SingleAnnotationIOSettings;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameter;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.DynamicStringEnumParameter;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.Component;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Selection input form", description = "A form element that allows the user to select one of multiple options")
public class EnumFormData extends ParameterFormData {

    private String value = "";
    private SingleAnnotationIOSettings annotationIOSettings = new SingleAnnotationIOSettings();
    private StringAndStringPairParameter.List items = new StringAndStringPairParameter.List();

    public EnumFormData() {
        annotationIOSettings.getEventBus().register(this);
    }

    public EnumFormData(EnumFormData other) {
        super(other);
        this.value = other.value;
        this.annotationIOSettings = new SingleAnnotationIOSettings(other.annotationIOSettings);
        this.items = new StringAndStringPairParameter.List(other.items);
        annotationIOSettings.getEventBus().register(this);
    }

    @JIPipeDocumentation(name = "Initial value", description = "The initial value. Should be the annotation value of the item.")
    @JIPipeParameter("initial-value")
    public String getValue() {
        return value;
    }

    @JIPipeParameter("initial-value")
    public void setValue(String value) {
        this.value = value;
    }

    @JIPipeDocumentation(name = "Form element I/O", description = "Use following settings to determine how to extract initial values " +
            "from annotations and where to store the user-defined value.")
    @JIPipeParameter("form:io")
    public SingleAnnotationIOSettings getAnnotationIOSettings() {
        return annotationIOSettings;
    }

    @JIPipeDocumentation(name = "Items", description = "List of available items. For each entry, you " +
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
        return JIPipe.getParameterTypes().createEditorFor(workbench, access);
    }

    @Override
    public JIPipeData duplicate() {
        return new EnumFormData(this);
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
    }

    @Override
    public String toString() {
        return String.format("Selection form [name=%s, value=%s]", getName(), value);
    }

    @Override
    public void loadData(JIPipeMergingDataBatch dataBatch) {
        if (annotationIOSettings.getInputAnnotation().isEnabled()) {
            JIPipeTextAnnotation annotation =
                    dataBatch.getMergedAnnotations().getOrDefault(annotationIOSettings.getInputAnnotation().getContent(),
                            null);
            if (annotation != null) {
                this.value = StringUtils.nullToEmpty(annotation.getValue());
            }
        }
    }

    @Override
    public void writeData(JIPipeMergingDataBatch dataBatch) {
        if (annotationIOSettings.getOutputAnnotation().isEnabled()) {
            annotationIOSettings.getAnnotationMergeStrategy().mergeInto(dataBatch.getMergedAnnotations(),
                    Collections.singletonList(annotationIOSettings.getOutputAnnotation().createAnnotation(value)));
        }
    }

    public static EnumFormData importFrom(Path rowStorage) {
        return FormData.importFrom(rowStorage, EnumFormData.class);
    }
}
