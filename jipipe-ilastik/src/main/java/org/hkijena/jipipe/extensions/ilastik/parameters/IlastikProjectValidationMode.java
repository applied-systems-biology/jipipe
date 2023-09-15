package org.hkijena.jipipe.extensions.ilastik.parameters;

public enum IlastikProjectValidationMode {
    CrashOnError("Crash on error"),
    SkipOnError("Skip on error"),
    Ignore("Do not validate");

    private final String text;

    IlastikProjectValidationMode(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
