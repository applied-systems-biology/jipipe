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

package org.hkijena.jipipe.plugins.ilastik.environments;

import org.hkijena.jipipe.plugins.parameters.api.optional.OptionalParameter;

public class OptionalIlastikEnvironment extends OptionalParameter<IlastikEnvironment> {
    public OptionalIlastikEnvironment() {
        super(IlastikEnvironment.class);
    }

    public OptionalIlastikEnvironment(OptionalIlastikEnvironment other) {
        super(IlastikEnvironment.class);
        setEnabled(other.isEnabled());
        if (other.getContent() != null) {
            setContent(new IlastikEnvironment(other.getContent()));
        }
    }
}
