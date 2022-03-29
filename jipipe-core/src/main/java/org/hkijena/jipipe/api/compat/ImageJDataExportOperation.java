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
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Combines a {@link ImageJDataExporter} with its appropriate parameters.
 */
public class ImageJDataExportOperation extends ImageJExportParameters implements BiFunction<JIPipeDataTable, JIPipeProgressInfo, List<Object>> {
    private ImageJDataExporter exporter;
    private ImageJExportParameters parameters;
    private String exporterId;

    public ImageJDataExportOperation() {

    }

    /**
     * @param exporter the adapter
     */
    public ImageJDataExportOperation(ImageJDataExporter exporter) {
        this.exporter = exporter;
        this.exporterId = Objects.requireNonNull(JIPipe.getImageJAdapters().getIdOf(exporter));
    }

    @JIPipeDocumentation(name = "Exporter ID", description = "The unique ID of the exporter")
    @JIPipeParameter(value = "id", hidden = true)
    @StringParameterSettings(monospace = true)
    @JsonGetter("id")
    public String getExporterId() {
        return exporterId;
    }

    @JIPipeParameter("id")
    @JsonSetter("id")
    public void setExporterId(String exporterId) {
        this.exporterId = exporterId;
        this.exporter = null;
    }

    /**
     * @return arbitrary string data that can be used by the ImageJ adapter for the conversion
     */
    public ImageJExportParameters getParameters() {
        return parameters;
    }

    /**
     * Sets arbitrary string data that can be used by the adapter for the conversion
     *
     * @param parameters conversion parameters
     */
    public void setParameters(ImageJExportParameters parameters) {
        this.parameters = parameters;
    }

    public ImageJDataExporter getExporter() {
        if (exporter == null) {
            return JIPipe.getImageJAdapters().getExporterById(getExporterId());
        }
        return exporter;
    }

    @Override
    public List<Object> apply(JIPipeDataTable dataTable, JIPipeProgressInfo progressInfo) {
        return getExporter().exportData(dataTable, this, progressInfo);
    }
}
