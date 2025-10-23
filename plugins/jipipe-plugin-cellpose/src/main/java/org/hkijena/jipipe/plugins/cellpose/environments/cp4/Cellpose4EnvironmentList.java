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

package org.hkijena.jipipe.plugins.cellpose.environments.cp4;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

public class Cellpose4EnvironmentList extends JIPipeListParameter<Cellpose4Environment> {
    public Cellpose4EnvironmentList() {
        super(Cellpose4Environment.class);
    }

    public Cellpose4EnvironmentList(Cellpose4EnvironmentList other) {
        super(Cellpose4Environment.class);
        for (Cellpose4Environment environment : other) {
            add(new Cellpose4Environment(environment));
        }
    }
}
