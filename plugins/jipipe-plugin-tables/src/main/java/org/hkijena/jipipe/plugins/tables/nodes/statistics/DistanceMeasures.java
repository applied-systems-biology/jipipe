/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.tables.nodes.statistics;

import org.apache.commons.math3.ml.distance.*;

public enum DistanceMeasures {
    Euclidean(new EuclideanDistance()),
    Chebyshev(new ChebyshevDistance()),
    EarthMovers(new EarthMoversDistance()),
    Manhattan(new ManhattanDistance()),
    Canberra(new CanberraDistance());

    private final DistanceMeasure distanceMeasure;

    DistanceMeasures(DistanceMeasure distanceMeasure) {
        this.distanceMeasure = distanceMeasure;
    }

    public DistanceMeasure getDistanceMeasure() {
        return distanceMeasure;
    }
}
