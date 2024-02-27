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

package org.hkijena.jipipe.api.compat;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

/**
 * Describes how data should be imported into ImageJ
 */
public class ImageJImportParameters extends AbstractJIPipeParameterCollection {
    private String name;
    private boolean duplicate;

    public ImageJImportParameters() {
    }

    public ImageJImportParameters(String name) {
        this.name = name;
    }

    public ImageJImportParameters(ImageJImportParameters other) {
        this.name = other.name;
        this.duplicate = other.duplicate;
    }

    @SetJIPipeDocumentation(name = "Name", description = "The name associated to the imported data")
    @JIPipeParameter("name")
    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    @JIPipeParameter("name")
    public void setName(String name) {
        this.name = name;
    }

    @SetJIPipeDocumentation(name = "Duplicate data", description = "If enabled, a duplicate is imported if possible")
    @JIPipeParameter("duplicate")
    public boolean isDuplicate() {
        return duplicate;
    }

    @JIPipeParameter("duplicate")
    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public void copyTo(ImageJImportParameters other) {
        other.name = this.name;
        other.duplicate = this.duplicate;
    }

    @Override
    public String toString() {
        return "ImageJImportParameters{" +
                "name='" + name + '\'' +
                ", duplicate=" + duplicate +
                '}';
    }
}
