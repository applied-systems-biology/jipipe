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

package org.hkijena.jipipe.api.compat;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Runs a {@link ImageJDataImporter} import operation.
 */
public class ImageJDataImportOperation extends ImageJImportParameters implements Function<List<Object>, JIPipeDataTable> {
    private String importerId;
    private ImageJDataImporter importer;

    public ImageJDataImportOperation() {
    }

    /**
     * @param importer the adapter
     */
    public ImageJDataImportOperation(ImageJDataImporter importer) {
        this.importer = importer;
        this.importerId = Objects.requireNonNull(JIPipe.getImageJAdapters().getIdOf(importer));
    }


    public ImageJDataImporter getImporter() {
        if(importer == null) {
            importer = JIPipe.getImageJAdapters().getImporterById(getImporterId());
        }
        return importer;
    }

    @JIPipeDocumentation(name = "Importer ID", description = "The unique ID of the importer")
    @JIPipeParameter("id")
    @StringParameterSettings(monospace = true)
    @JsonGetter("id")
    public String getImporterId() {
        return importerId;
    }

    @JIPipeParameter("id")
    @JsonSetter("id")
    public void setImporterId(String importerId) {
        this.importer = null;
        this.importerId = importerId;
    }

    @Override
    public JIPipeDataTable apply(List<Object> objects) {
        return getImporter().importData(objects, this);
    }
}
