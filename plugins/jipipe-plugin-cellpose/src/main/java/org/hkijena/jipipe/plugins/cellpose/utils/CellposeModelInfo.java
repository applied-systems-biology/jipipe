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

package org.hkijena.jipipe.plugins.cellpose.utils;

import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;

import java.util.List;

public class CellposeModelInfo {
    private String modelNameOrPath;
    //    private String sizeModelNameOrPath;
    private boolean modelPretrained;
    private List<JIPipeTextAnnotation> annotationList;

    public String getModelNameOrPath() {
        return modelNameOrPath;
    }

    public void setModelNameOrPath(String modelNameOrPath) {
        this.modelNameOrPath = modelNameOrPath;
    }

//    public String getSizeModelNameOrPath() {
//        return sizeModelNameOrPath;
//    }
//
//    public void setSizeModelNameOrPath(String sizeModelNameOrPath) {
//        this.sizeModelNameOrPath = sizeModelNameOrPath;
//    }

    public boolean isModelPretrained() {
        return modelPretrained;
    }

    public void setModelPretrained(boolean modelPretrained) {
        this.modelPretrained = modelPretrained;
    }

    public List<JIPipeTextAnnotation> getAnnotationList() {
        return annotationList;
    }

    public void setAnnotationList(List<JIPipeTextAnnotation> annotationList) {
        this.annotationList = annotationList;
    }
}
