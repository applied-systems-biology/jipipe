package org.hkijena.jipipe.extensions.parameters.library.primitives;

import java.awt.*;

public enum FontStyleParameter {
    Plain(Font.PLAIN),
    Bold(Font.BOLD),
    Italic(Font.ITALIC),
    BoldItalic(Font.BOLD + Font.ITALIC);

    private final int nativeValue;

    FontStyleParameter(int nativeValue) {

        this.nativeValue = nativeValue;
    }

    @Override
    public String toString() {
        if(this == BoldItalic) {
            return "Bold + Italic";
        }
        return super.toString();
    }

    public Font toFont(FontFamilyParameter family, int size) {
       return family.toFont(nativeValue, size);
    }

    public int getNativeValue() {
        return nativeValue;
    }
}
