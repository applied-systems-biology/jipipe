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

package org.hkijena.acaq5.ui.registries;

import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.ui.plotbuilder.PlotDataSource;
import org.hkijena.acaq5.ui.plotbuilder_old.ACAQLegacyPlot;
import org.hkijena.acaq5.ui.plotbuilder_old.ACAQLegacyPlotSeriesData;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Registry for plot
 */
public class ACAQPlotBuilderRegistry {

    private Set<Class<? extends PlotDataSource>> registeredDataSources = new HashSet<>();
    private Map<Class<? extends ACAQLegacyPlot>, Entry> entries = new HashMap<>();

    /**
     * Registers a plot type
     *
     * @param plotType Plot instance
     * @param name     Plot name
     * @param icon     Plot icon
     */
    public void register(Class<? extends ACAQLegacyPlot> plotType, String name, Icon icon) {
        entries.put(plotType, new Entry(plotType, name, icon));
    }

    /**
     * @return Registered entries
     */
    public Collection<Entry> getEntries() {
        return entries.values();
    }

    /**
     * @param plot The plot
     * @return Name of the plot type
     */
    public String getNameOf(ACAQLegacyPlot plot) {
        return entries.get(plot.getClass()).getName();
    }

    /**
     * @param plot The plot
     * @return Icon for the plot
     */
    public Icon getIconOf(ACAQLegacyPlot plot) {
        return entries.get(plot.getClass()).getIcon();
    }

    /**
     * Creates all plots for the data
     *
     * @param seriesDataList The data
     * @return List of plots
     */
    public List<ACAQLegacyPlot> createAllPlots(List<ACAQLegacyPlotSeriesData> seriesDataList) {
        List<ACAQLegacyPlot> plots = new ArrayList<>();
        for (Entry entry : entries.values()) {
            try {
                plots.add(entry.getPlotType().getConstructor(List.class).newInstance(seriesDataList));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        return plots;
    }

    /**
     * Registers a new data source type
     *
     * @param klass Data source classRowIteratorPlotDataSource
     */
    public void registerDataSource(Class<? extends PlotDataSource> klass) {
        registeredDataSources.add(klass);
    }

    public Set<Class<? extends PlotDataSource>> getRegisteredDataSources() {
        return Collections.unmodifiableSet(registeredDataSources);
    }

    public static ACAQPlotBuilderRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getPlotBuilderRegistry();
    }

    /**
     * Registry entry
     */
    public static class Entry {
        private Class<? extends ACAQLegacyPlot> plotType;
        private String name;
        private Icon icon;

        /**
         * @param plotType Plot type
         * @param name     Plot name
         * @param icon     Plot icon
         */
        public Entry(Class<? extends ACAQLegacyPlot> plotType, String name, Icon icon) {
            this.plotType = plotType;
            this.name = name;
            this.icon = icon;
        }

        /**
         * @return The plot type
         */
        public Class<? extends ACAQLegacyPlot> getPlotType() {
            return plotType;
        }

        /**
         * @return Plot type name
         */
        public String getName() {
            return name;
        }

        /**
         * @return Plot type icon
         */
        public Icon getIcon() {
            return icon;
        }

    }
}
