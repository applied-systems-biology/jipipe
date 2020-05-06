package org.hkijena.acaq5.ui.plotbuilder.datasources;

import org.hkijena.acaq5.ui.plotbuilder.PlotDataSource;

import java.util.Arrays;

/**
 * {@link PlotDataSource} that contains doubles.
 */
public class DoubleArrayPlotDataSource implements PlotDataSource {

    private double[] data;
    private String name;

    /**
     * Creates a new instance
     *
     * @param data the data. Can have any size
     * @param name non-empty name
     */
    public DoubleArrayPlotDataSource(double[] data, String name) {
        this.data = data;
        this.name = name;
    }

    @Override
    public String[] getDataAsString(int rows) {
        double[] arr = getDataAsDouble(rows);
        String[] result = new String[rows];
        for (int i = 0; i < arr.length; ++i) {
            result[i] = "" + arr[i];
        }
        return result;
    }

    @Override
    public double[] getDataAsDouble(int rows) {
        return Arrays.copyOf(data, rows);
    }

    @Override
    public String getName() {
        return name;
    }

    public double[] getData() {
        return data;
    }

    @Override
    public int getRows() {
        return data.length;
    }

    @Override
    public boolean isNumeric() {
        return true;
    }

    @Override
    public boolean isUserRemovable() {
        return true;
    }
}
