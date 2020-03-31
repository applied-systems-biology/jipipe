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

package org.hkijena.acaq5.ui.plotbuilder;

import java.util.function.Function;

/**
 * Generates data
 * @param <T> Generated data type
 */
public class ACAQPlotSeriesGenerator<T> {
    private String name;
    private Function<Integer, T> generatorFunction;

    /**
     * @param name Generator name
     * @param generatorFunction Function that generates data based on row index
     */
    public ACAQPlotSeriesGenerator(String name, Function<Integer, T> generatorFunction) {
        this.name = name;
        this.generatorFunction = generatorFunction;
    }

    /**
     * @return Generator name
     */
    public String getName() {
        return name;
    }

    /**
     * @return Function that generates data based on row index
     */
    public Function<Integer, T> getGeneratorFunction() {
        return generatorFunction;
    }
}
