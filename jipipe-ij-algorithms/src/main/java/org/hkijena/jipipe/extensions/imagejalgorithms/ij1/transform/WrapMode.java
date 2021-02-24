package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform;

public enum WrapMode {
    None,
    Wrap,
    Mirror;

    public int wrap(int value, int min, int max) {
        int width = Math.abs(max - min);
        switch (this) {
            case None:
                return value;
            case Wrap:
                value -= min;
                while(value < 0) {
                    value += width;
                }
                return min + (value % width);
            case Mirror:
                if(value < min)
                    return 2 * min - value;
                else if(value > max)
                    return 2 * max - value;
                else
                    return value;
            default:
                throw new UnsupportedOperationException();
        }
    }
}
