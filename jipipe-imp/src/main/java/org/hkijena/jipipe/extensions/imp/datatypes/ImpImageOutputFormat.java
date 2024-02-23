package org.hkijena.jipipe.extensions.imp.datatypes;

public enum ImpImageOutputFormat {
    PNG("PNG", ".png"),
    BMP("BMP", ".bmp"),
    JPG("JPG", ".jpg", ".jpeg"),
    GIF("GIF", ".gif");

    private final String nativeValue;
    private final String[] extensions;

    ImpImageOutputFormat(String nativeValue, String... extensions) {

        this.nativeValue = nativeValue;
        this.extensions = extensions;
    }

    public String getNativeValue() {
        return nativeValue;
    }

    public String[] getExtensions() {
        return extensions;
    }

    public String getExtension() {
        return extensions[0];
    }
}
