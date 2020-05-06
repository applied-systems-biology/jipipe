package org.hkijena.acaq5.extensions.plots.datasources;

import org.hkijena.acaq5.ui.plotbuilder.PlotDataSource;

/**
 * Generates numbers from 0 to row count
 */
public class ZeroPlotDataSource implements PlotDataSource {
    @Override
    public String getName() {
        return "Generate: Zeros";
    }

    @Override
    public String[] getDataAsString(int rows) {
        String[] data = new String[rows];
        for (int i = 0; i < rows; i++) {
            data[i] = "0";
        }
        return data;
    }

    @Override
    public double[] getDataAsDouble(int rows) {
        return new double[rows];
    }

    @Override
    public int getRows() {
        return 0;
    }

    @Override
    public boolean isNumeric() {
        return true;
    }

    @Override
    public boolean isUserRemovable() {
        return false;
    }
}
