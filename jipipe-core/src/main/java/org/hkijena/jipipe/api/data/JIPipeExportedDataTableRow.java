package org.hkijena.jipipe.api.data;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.ArrayList;
import java.util.List;

/**
 * A row in the table
 */
public class JIPipeExportedDataTableRow {
    private int index;
    private List<JIPipeAnnotation> annotations = new ArrayList<>();
    private List<JIPipeExportedDataAnnotation> dataAnnotations = new ArrayList<>();
    private String trueDataType;

    /**
     * Creates new instance
     */
    public JIPipeExportedDataTableRow() {
    }

    /**
     * @return Internal location relative to the output folder
     */
    @JsonGetter("index")
    public int getIndex() {
        return index;
    }

    /**
     * Sets the location
     *
     * @param index Internal location relative to the output folder
     */
    @JsonSetter("index")
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * @return Annotations
     */
    @JsonGetter("annotations")
    public List<JIPipeAnnotation> getAnnotations() {
        return annotations;
    }

    /**
     * Sets annotations
     *
     * @param annotations List of annotations
     */
    @JsonSetter("annotations")
    public void setAnnotations(List<JIPipeAnnotation> annotations) {
        this.annotations = annotations;
    }

    @JsonGetter("data-annotations")
    public List<JIPipeExportedDataAnnotation> getDataAnnotations() {
        return dataAnnotations;
    }

    @JsonSetter("data-annotations")
    public void setDataAnnotations(List<JIPipeExportedDataAnnotation> dataAnnotations) {
        this.dataAnnotations = dataAnnotations;
    }

    /**
     * Compatibility function to allow reading tables in an older format
     *
     * @param annotations List of annotations
     */
    @JsonSetter("traits")
    public void setTraits(List<JIPipeAnnotation> annotations) {
        this.annotations = annotations;
    }

    @JsonGetter("true-data-type")
    public String getTrueDataType() {
        return trueDataType;
    }

    @JsonSetter("true-data-type")
    public void setTrueDataType(String trueDataType) {
        this.trueDataType = trueDataType;
    }
}
