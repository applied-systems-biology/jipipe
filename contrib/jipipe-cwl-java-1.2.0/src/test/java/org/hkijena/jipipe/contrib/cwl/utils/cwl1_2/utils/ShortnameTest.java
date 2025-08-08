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

package org.hkijena.jipipe.contrib.cwl.utils.cwl1_2.utils;

import org.hkijena.jipipe.contrib.cwl.cwl1_2.utils.Uris;
import org.junit.Assert;
import org.junit.Test;

public class ShortnameTest {
	@Test
	public void testShortname() {
		Assert.assertEquals(Uris.shortname(
				"file:/Users/jdidion/projects/cwlScala/target/test-classes/CommandLineTools/conformance/#anon_enum_inside_array_inside_schemadef.cwl/first/user_type_2/species/homo_sapiens"),
				"homo_sapiens");
		Assert.assertEquals(Uris.shortname(
				"file:///home/michael/cwljava/src/test/resources/org/w3id/cwl/cwl1_2/utils/valid_anon_enum_inside_array_inside_schemadef.cwl#vcf2maf_params/ncbi_build/GRCh37"),
				"GRCh37");
		// Below are from https://w3id.org/cwl/v1.2/SchemaSalad.html#Short_names
		Assert.assertEquals(Uris.shortname("http://example.com/foo"), "foo");
		Assert.assertEquals(Uris.shortname("http://example.com/#bar"), "bar");
		Assert.assertEquals(Uris.shortname("http://example.com/foo/bar"), "bar");
		Assert.assertEquals(Uris.shortname("http://example.com/foo#bar"), "bar");
		Assert.assertEquals(Uris.shortname("http://example.com/#foo/bar"), "bar");
		Assert.assertEquals(Uris.shortname("http://example.com/foo#bar/baz"), "baz");
	}

}
