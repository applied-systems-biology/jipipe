package org.hkijena.jipipe.plugins.imagejdatatypes.util;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

public class ImageJHistogram {
    private final double min;
    private final double max;
    private final long[] histogram;
    private final long maxCount;

    public ImageJHistogram(double min, double max, long[] histogram) {
        this.min = min;
        this.max = max;
        this.histogram = histogram;
        this.maxCount = Longs.max(histogram);
    }

    public boolean isEmpty() {
        return histogram == null || histogram.length == 0;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public long getMaxCount() {
        return maxCount;
    }

    /**
     * Returns the sum of counts in the histogram between the specified percentage range (0.0 to 1.0).
     * For example, 0.0 to 0.05 returns the count in the first 5% of bins.
     */
    public long countInPercentageRange(double fromPercent, double toPercent) {
        fromPercent = Math.max(0, fromPercent);
        toPercent = Math.min(1, toPercent);

        int len = histogram.length;
        int fromIndex = (int) Math.floor(fromPercent * len);
        int toIndex = (int) Math.ceil(toPercent * len);

        fromIndex = Math.max(0, fromIndex);
        toIndex = Math.min(len, toIndex);

        long count = 0;
        for (int i = fromIndex; i < toIndex; i++) {
            count += histogram[i];
        }
        return count;
    }

    /**
     * Returns the maximum of counts in the histogram between the specified percentage range (0.0 to 1.0).
     * For example, 0.0 to 0.05 returns the count in the first 5% of bins.
     */
    public long maxInPercentageRange(double fromPercent, double toPercent) {
        fromPercent = Math.max(0, fromPercent);
        toPercent = Math.min(1, toPercent);

        int len = histogram.length;
        int fromIndex = (int) Math.floor(fromPercent * len);
        int toIndex = (int) Math.ceil(toPercent * len);

        fromIndex = Math.max(0, fromIndex);
        toIndex = Math.min(len, toIndex);

        long max = 0;
        for (int i = fromIndex; i < toIndex; i++) {
            max = Math.max(max, histogram[i]);
        }
        return max;
    }

    public int size() {
        return histogram.length;
    }
}
