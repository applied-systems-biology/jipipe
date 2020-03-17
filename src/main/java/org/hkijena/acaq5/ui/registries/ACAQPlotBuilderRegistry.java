/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Insitute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.registries;

import org.hkijena.acaq5.ui.plotbuilder.ACAQPlot;
import org.hkijena.acaq5.ui.plotbuilder.ACAQPlotSeriesData;
import org.hkijena.acaq5.ui.plotbuilder.ACAQPlotSettingsUI;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class ACAQPlotBuilderRegistry {

    private Map<Class<? extends ACAQPlot>, Entry> entries = new HashMap<>();

    public void register(Class<? extends ACAQPlot> plotType, Class<? extends ACAQPlotSettingsUI> settingsType, String name, Icon icon) {
        entries.put(plotType, new Entry(plotType, settingsType, name, icon));
    }

    public Collection<Entry> getEntries() {
        return entries.values();
    }

    public String getNameOf(ACAQPlot plot) {
        return entries.get(plot.getClass()).getName();
    }

    public Icon getIconOf(ACAQPlot plot) {
        return entries.get(plot.getClass()).getIcon();
    }

    public List<ACAQPlot> createAllPlots(List<ACAQPlotSeriesData> seriesDataList) {
        List<ACAQPlot> plots = new ArrayList<>();
        for (Entry entry : entries.values()) {
            try {
                plots.add(entry.getPlotType().getConstructor(List.class).newInstance(seriesDataList));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        return plots;
    }

    public ACAQPlotSettingsUI createSettingsUIFor(ACAQPlot plot) {
        try {
            return entries.get(plot.getClass()).getSettingsType().getConstructor(ACAQPlot.class).newInstance(plot);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Entry {
        private Class<? extends ACAQPlot> plotType;
        private Class<? extends ACAQPlotSettingsUI> settingsType;
        private String name;
        private Icon icon;

        public Entry(Class<? extends ACAQPlot> plotType, Class<? extends ACAQPlotSettingsUI> settingsType, String name, Icon icon) {
            this.plotType = plotType;
            this.settingsType = settingsType;
            this.name = name;
            this.icon = icon;
        }

        public Class<? extends ACAQPlot> getPlotType() {
            return plotType;
        }

        public String getName() {
            return name;
        }

        public Icon getIcon() {
            return icon;
        }

        public Class<? extends ACAQPlotSettingsUI> getSettingsType() {
            return settingsType;
        }
    }
}
