/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.plotbuilder_old;

import com.google.common.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @param <T> Stored data type
 */
public abstract class ACAQLegacyPlotSeriesColumn<T> {
    private List<ACAQLegacyPlotSeriesData> seriesDataList;
    private List<ACAQLegacyPlotSeriesGenerator> generators;
    private int seriesDataIndex = -1;
    private EventBus eventBus = new EventBus();

    /**
     * @param seriesDataList       Containing data
     * @param defaultGenerator     Generates default values
     * @param additionalGenerators Additional generators
     */
    @SafeVarargs
    public ACAQLegacyPlotSeriesColumn(List<ACAQLegacyPlotSeriesData> seriesDataList, ACAQLegacyPlotSeriesGenerator defaultGenerator, ACAQLegacyPlotSeriesGenerator... additionalGenerators) {
        this.seriesDataList = seriesDataList;
        this.generators = new ArrayList<>();
        this.generators.add(defaultGenerator);
        this.generators.addAll(Arrays.asList(additionalGenerators));
    }

    /**
     * Gets the first n rows.
     * Generates data if not available
     *
     * @param rowCount the number of rows to return
     * @return Row values
     */
    public List<T> getValues(int rowCount) {
        if (seriesDataIndex < 0) {
            ACAQLegacyPlotSeriesGenerator generator = generators.get(-seriesDataIndex - 1);
            List<T> result = new ArrayList<>(rowCount);
            for (int row = 0; row < rowCount; ++row) {
                result.add((T) generator.getGeneratorFunction().apply(row));
            }
            return result;
        } else {
            return getValuesFromTable();
        }
    }

    /**
     * @return Data extracted from table
     */
    protected abstract List<T> getValuesFromTable();

    /**
     * @return The current series data
     */
    public ACAQLegacyPlotSeriesData getSeriesData() {
        if (seriesDataIndex >= 0)
            return seriesDataList.get(seriesDataIndex);
        else
            return null;
    }

    /**
     * @return The current series data index
     */
    public int getSeriesDataIndex() {
        return seriesDataIndex;
    }

    /**
     * Sets the current series data index
     *
     * @param seriesDataIndex index
     */
    public void setSeriesDataIndex(int seriesDataIndex) {
        this.seriesDataIndex = seriesDataIndex;
        eventBus.post(new DataChangedEvent(this));
    }

    /**
     * Gets the number of rows that is required to hold this data
     * If data is generated, it returns 0
     *
     * @return rows that is required to hold this data. If data is generated, it returns 0
     */
    public int getRequiredRowCount() {
        if (seriesDataIndex >= 0) {
            return seriesDataList.get(seriesDataIndex).getSize();
        } else {
            return 0;
        }
    }

    /**
     * @return The event bus
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * @return Available generators
     */
    public List<ACAQLegacyPlotSeriesGenerator> getGenerators() {
        return Collections.unmodifiableList(generators);
    }

    /**
     * Triggered when data is changed
     */
    public static class DataChangedEvent {
        private ACAQLegacyPlotSeriesColumn seriesColumn;

        /**
         * @param seriesColumn Event source
         */
        public DataChangedEvent(ACAQLegacyPlotSeriesColumn seriesColumn) {
            this.seriesColumn = seriesColumn;
        }

        /**
         * @return Event source
         */
        public ACAQLegacyPlotSeriesColumn getSeriesColumn() {
            return seriesColumn;
        }
    }
}
