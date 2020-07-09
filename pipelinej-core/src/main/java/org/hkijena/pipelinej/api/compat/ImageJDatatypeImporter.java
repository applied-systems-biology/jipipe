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

package org.hkijena.pipelinej.api.compat;

import org.hkijena.pipelinej.api.data.ACAQData;

import java.util.function.Supplier;

/**
 * Runs a {@link ImageJDatatypeAdapter} import operation.
 */
public class ImageJDatatypeImporter implements Supplier<ACAQData> {
    private ImageJDatatypeAdapter adapter;
    private String parameters;

    /**
     * @param adapter the adapter
     */
    public ImageJDatatypeImporter(ImageJDatatypeAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * @return arbitrary string data that can be used by the ImageJ adapter for the conversion
     */
    public String getParameters() {
        return parameters;
    }

    /**
     * Sets arbitrary string data that can be used by the adapter for the conversion
     *
     * @param parameters conversion parameters
     */
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public ImageJDatatypeAdapter getAdapter() {
        return adapter;
    }

    @Override
    public ACAQData get() {
        return adapter.importFromImageJ(parameters);
    }
}
