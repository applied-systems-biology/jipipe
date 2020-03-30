package org.hkijena.acaq5.api.compat;

import org.hkijena.acaq5.api.data.ACAQData;

import java.util.function.Supplier;

/**
 * Runs a {@link ImageJDatatypeAdapter} import operation.
 */
public class ImageJDatatypeImporter implements Supplier<ACAQData> {
    private ImageJDatatypeAdapter adapter;
    private String windowName;

    public ImageJDatatypeImporter(ImageJDatatypeAdapter adapter) {
        this.adapter = adapter;
    }

    public String getWindowName() {
        return windowName;
    }

    public void setWindowName(String windowName) {
        this.windowName = windowName;
    }

    public ImageJDatatypeAdapter getAdapter() {
        return adapter;
    }

    @Override
    public ACAQData get() {
        return adapter.importFromImageJ(windowName);
    }
}
