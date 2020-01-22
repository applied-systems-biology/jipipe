package org.hkijena.acaq5.utils;

import bham.sc.uk.landini.morphology_collection.GreyscaleErode_;

public class GreyscaleErode extends GreyscaleErode_ {
    public GreyscaleErode(int iterations, boolean whiteForeground) {
        this.iterations = iterations;
        this.doIwhite = whiteForeground;
    }
}
