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

package org.hkijena.jipipe.plugins.ijocr.environments;

import org.hkijena.jipipe.plugins.parameters.api.optional.OptionalParameter;

public class OptionalTesseractOCREnvironment extends OptionalParameter<TesseractOCREnvironment> {
    public OptionalTesseractOCREnvironment() {
        super(TesseractOCREnvironment.class);
    }

    public OptionalTesseractOCREnvironment(OptionalTesseractOCREnvironment other) {
        super(TesseractOCREnvironment.class);
        setEnabled(other.isEnabled());
        if (other.getContent() != null) {
            setContent(new TesseractOCREnvironment(other.getContent()));
        }
    }
}
