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

package org.hkijena.jipipe.contrib.cwl.cwl1_1.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.resolver.ScalarResolver;
import org.snakeyaml.engine.v2.schema.CoreSchema;

public class YamlUtils {

	public static Map<String, Object> mapFromString(final String text) {
		LoadSettings settings = LoadSettings.builder().setSchema(new CoreSchema()).build();
		Load load = new Load(settings);
		final Map<String, Object> result = (Map<String, Object>) load.loadFromString(text);
		return result;
	}

	public static List<Object> listFromString(final String text) {
		LoadSettings settings = LoadSettings.builder().setSchema(new CoreSchema()).build();
		Load load = new Load(settings);
		final List<Object> result = (List<Object>) load.loadFromString(text);
		return result;
	}
}
