package org.hkijena.acaq5.ui.plotbuilder;

/**
 * {@link PlotDataSource} that contains doubles.
 */
public class StringArrayPlotDataSource implements PlotDataSource {

    private String[] data;
    private String name;

    /**
     * Creates a new instance
     *
     * @param data the data. Can have any size
     * @param name non-empty name
     */
    public StringArrayPlotDataSource(String[] data, String name) {
        this.data = data;
        this.name = name;
    }

    @Override
    public String[] getDataAsString(int rows) {
        if (rows == data.length)
            return data;
        String[] result = new String[rows];
        for (int i = 0; i < rows; ++i) {
            result[i] = i < data.length ? data[i] : "";
        }
        return result;
    }

    @Override
    public double[] getDataAsDouble(int rows) {
        String[] arr = getDataAsString(rows);
        double[] result = new double[rows];
        for (int i = 0; i < arr.length; ++i) {
            try {
                result[i] = Double.parseDouble(arr[i]);
            } catch (NumberFormatException e) {
                result[i] = 0;
            }
        }
        return result;
    }

    @Override
    public int getRows() {
        return data.length;
    }

    @Override
    public String getName() {
        return name;
    }

    public String[] getData() {
        return data;
    }
}
