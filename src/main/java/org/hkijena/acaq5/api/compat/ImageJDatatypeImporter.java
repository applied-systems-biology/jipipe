package org.hkijena.acaq5.api.compat;

import org.hkijena.acaq5.api.data.ACAQData;

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
