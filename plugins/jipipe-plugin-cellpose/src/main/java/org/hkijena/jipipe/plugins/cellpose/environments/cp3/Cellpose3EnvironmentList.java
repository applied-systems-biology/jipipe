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

package org.hkijena.jipipe.plugins.cellpose.environments.cp3;

import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;

public class Cellpose3EnvironmentList extends ListParameter<Cellpose3Environment> {
    public Cellpose3EnvironmentList() {
        super(Cellpose3Environment.class);
    }

    public Cellpose3EnvironmentList(Cellpose3EnvironmentList other) {
        super(Cellpose3Environment.class);
        for (Cellpose3Environment environment : other) {
            add(new Cellpose3Environment(environment));
        }
    }
}
