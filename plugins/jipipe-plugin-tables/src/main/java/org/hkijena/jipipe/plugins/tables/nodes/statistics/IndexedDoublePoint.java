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

import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.DoublePoint;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class IndexedDoublePoint implements Clusterable, Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 3946024775784901369L;

    /**
     * The source row
     */
    private final int sourceRow;

    /** Point coordinates. */
    private final double[] point;

    /**
     * Build an instance wrapping a double array.
     * <p>
     * The wrapped array is referenced, it is <em>not</em> copied.
     *
     * @param point the n-dimensional point in double space
     */
    public IndexedDoublePoint(int sourceRow, final double[] point) {
        this.sourceRow = sourceRow;
        this.point = point;
    }

    /**
     * Build an instance wrapping an integer array.
     * The wrapped array is copied to an internal double array.
     *
     * @param point the n-dimensional point in integer space
     */
    public IndexedDoublePoint(int sourceRow, final int[] point) {
        this.sourceRow = sourceRow;
        this.point = new double[point.length];
        for ( int i = 0; i < point.length; i++) {
            this.point[i] = point[i];
        }
    }

    public static double[] calculateCenter(List<IndexedDoublePoint> clusterPoints) {
        int numPoints = clusterPoints.size();

        int dimension = clusterPoints.get(0).getPoint().length;
        double[] centroid = new double[dimension];

        for (IndexedDoublePoint point : clusterPoints) {
            double[] coords = point.getPoint();
            for (int i = 0; i < dimension; i++) {
                centroid[i] += coords[i];
            }
        }

        // Compute the mean for each dimension
        for (int i = 0; i < dimension; i++) {
            centroid[i] /= numPoints;
        }

        return centroid;
    }

    public int getSourceRow() {
        return sourceRow;
    }

    /** {@inheritDoc} */
    public double[] getPoint() {
        return point;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof IndexedDoublePoint)) {
            return false;
        }
        return Arrays.equals(point, ((IndexedDoublePoint) other).point);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Arrays.hashCode(point);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Arrays.toString(point);
    }

}