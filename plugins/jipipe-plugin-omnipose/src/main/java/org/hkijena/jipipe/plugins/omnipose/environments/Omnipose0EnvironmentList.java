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

package org.hkijena.jipipe.plugins.omnipose.environments;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

public class Omnipose0EnvironmentList extends JIPipeListParameter<Omnipose0Environment> {
    public Omnipose0EnvironmentList() {
        super(Omnipose0Environment.class);
    }

    public Omnipose0EnvironmentList(Omnipose0EnvironmentList other) {
        super(Omnipose0Environment.class);
        for (Omnipose0Environment environment : other) {
            add(new Omnipose0Environment(environment));
        }
    }
}
