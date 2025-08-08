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

package org.hkijena.jipipe.contrib.cwl.utils.cwl1_1.utils;

import java.util.Map;

import org.hkijena.jipipe.contrib.cwl.cwl1_1.utils.YamlUtils;
import org.junit.Assert;
import org.junit.Test;

public class YamlUtilsTest {
  @Test
  public void testSimpleLoad() {
    final String yamlStr = "moo: cow\nbark: dog\n";
    final Map<String, Object> loaded = YamlUtils.mapFromString(yamlStr);
    Assert.assertEquals(loaded.get("moo"), "cow");
    Assert.assertEquals(loaded.get("bark"), "dog");
  }
}
