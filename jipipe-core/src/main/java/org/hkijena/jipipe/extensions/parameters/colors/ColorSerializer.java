/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.parameters.colors;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.Color;
import java.io.IOException;

/**
 * Serializer for {@link Color}
 */
public class ColorSerializer extends JsonSerializer<Color> {
    @Override
    public void serialize(Color value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
        gen.writeString(StringUtils.colorToHexString(value));
    }
}
