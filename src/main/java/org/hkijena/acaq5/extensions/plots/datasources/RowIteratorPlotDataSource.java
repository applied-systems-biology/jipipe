package org.hkijena.acaq5.extensions.plots.datasources;

import org.hkijena.acaq5.ui.plotbuilder.PlotDataSource;

/**
 * Generates numbers from 0 to row count
 */
public class RowIteratorPlotDataSource implements PlotDataSource {
    @Override
    public String getName() {
        return "Generate: Row index";
    }

    @Override
    public String[] getDataAsString(int rows) {
        String[] data = new String[rows];
        for (int i = 0; i < rows; i++) {
            data[i] = "" + i;
        }
        return data;
    }

    @Override
    public double[] getDataAsDouble(int rows) {
        double[] data = new double[rows];
        for (int i = 0; i < rows; i++) {
            data[i] = i;
        }
        return data;
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
