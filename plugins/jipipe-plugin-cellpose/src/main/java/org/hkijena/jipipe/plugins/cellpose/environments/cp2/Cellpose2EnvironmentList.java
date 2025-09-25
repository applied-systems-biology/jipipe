/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.cellpose.environments.cp2;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

public class Cellpose2EnvironmentList extends JIPipeListParameter<Cellpose2Environment> {
    public Cellpose2EnvironmentList() {
        super(Cellpose2Environment.class);
    }

    public Cellpose2EnvironmentList(Cellpose2EnvironmentList other) {
        super(Cellpose2Environment.class);
        for (Cellpose2Environment environment : other) {
            add(new Cellpose2Environment(environment));
        }
    }
}
