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

package org.hkijena.jipipe.api.data.serialization;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * A row in the table
 */
public class JIPipeDataTableRowInfo {
    private int index;
    private List<JIPipeTextAnnotation> textAnnotations = new ArrayList<>();
    private List<JIPipeDataAnnotationInfo> dataAnnotations = new ArrayList<>();
    private String trueDataType;
    private JIPipeDataContext dataContext;

    /**
     * Creates new instance
     */
    public JIPipeDataTableRowInfo() {
    }

    /**
     * Creates a new instance from an existing row
     *
     * @param dataTable the data table
     * @param row       the row
     */
    public JIPipeDataTableRowInfo(JIPipeDataTable dataTable, int row) {
        this.index = row;
        this.trueDataType = dataTable.getDataInfo(row).getId();
        this.dataContext = dataTable.getDataContext(row);
        this.textAnnotations = dataTable.getTextAnnotations(row);
        for (JIPipeDataAnnotation dataAnnotation : dataTable.getDataAnnotations(row)) {
            this.dataAnnotations.add(new JIPipeDataAnnotationInfo(dataAnnotation.getName(),
                    Paths.get("data-annotations").resolve("" + row).resolve(dataAnnotation.getName()),
                    dataAnnotation.getDataInfo().getId(),
                    this));
        }
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
    @JsonGetter("text-annotations")
    public List<JIPipeTextAnnotation> getTextAnnotations() {
        return textAnnotations;
    }

    /**
     * Sets annotations
     *
     * @param textAnnotations List of annotations
     */
    @JsonSetter("text-annotations")
    public void setTextAnnotations(List<JIPipeTextAnnotation> textAnnotations) {
        this.textAnnotations = textAnnotations;
    }

    @JsonGetter("data-annotations")
    public List<JIPipeDataAnnotationInfo> getDataAnnotations() {
        return dataAnnotations;
    }

    @JsonSetter("data-annotations")
    public void setDataAnnotations(List<JIPipeDataAnnotationInfo> dataAnnotations) {
        this.dataAnnotations = dataAnnotations;
    }

    /**
     * Compatibility function to allow reading tables in an older format
     *
     * @param annotations List of annotations
     */
    @JsonSetter("traits")
    public void setTraits(List<JIPipeTextAnnotation> annotations) {
        this.textAnnotations = annotations;
    }

    /**
     * Compatibility function to allow reading tables in an older format
     *
     * @param annotations List of annotations
     */
    @JsonSetter("annotations")
    public void setAnnotations(List<JIPipeTextAnnotation> annotations) {
        this.textAnnotations = annotations;
    }

    @JsonGetter("true-data-type")
    public String getTrueDataType() {
        return trueDataType;
    }

    @JsonSetter("true-data-type")
    public void setTrueDataType(String trueDataType) {
        this.trueDataType = trueDataType;
    }

    @JsonGetter("data-context")
    public JIPipeDataContext getDataContext() {
        return dataContext;
    }

    @JsonSetter("data-context")
    public void setDataContext(JIPipeDataContext dataContext) {
        this.dataContext = dataContext;
    }
}
