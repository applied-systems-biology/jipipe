package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.transform;

public enum WrapMode {
    None,
    Wrap,
    Replicate,
    Mirror;

    public int wrap(int value, int min, int max) {
        int width = Math.abs(max - min);
        switch (this) {
            case None:
                return value;
            case Wrap:
                value -= min;
                while (value < 0) {
                    value += width;
                }
                return min + (value % width);
            case Replicate:
                return Math.max(min, Math.min(max, value));
            case Mirror:
                if (value < min)
                    return 2 * min - value;
                else if (value > max)
                    return 2 * max - value;
                else
                    return value;
            default:
                throw new UnsupportedOperationException();
        }
    }
}
