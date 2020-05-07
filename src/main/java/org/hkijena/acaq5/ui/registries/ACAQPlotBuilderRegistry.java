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

import java.util.*;

/**
 * Registry for plots
 */
public class ACAQPlotBuilderRegistry {

    private Map<String, Class<? extends PlotDataSource>> registeredDataSources = new HashMap<>();

    /**
     * Registers a new data source type
     *
     * @param id The ID of this data source
     * @param klass Data source classRowIteratorPlotDataSource
     */
    public void registerDataSource(String id, Class<? extends PlotDataSource> klass) {
        registeredDataSources.put(id, klass);
    }

    public Map<String, Class<? extends PlotDataSource>> getRegisteredDataSources() {
        return Collections.unmodifiableMap(registeredDataSources);
    }

    public static ACAQPlotBuilderRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getPlotBuilderRegistry();
    }
}
