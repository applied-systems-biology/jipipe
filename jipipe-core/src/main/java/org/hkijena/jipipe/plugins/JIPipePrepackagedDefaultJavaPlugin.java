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

package org.hkijena.jipipe.plugins;

import org.hkijena.jipipe.JIPipeDefaultJavaPlugin;
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.metadata.JIPipeOrganizationMetadata;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.VersionUtils;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@link JIPipeDefaultJavaPlugin} for internal usage
 */
public abstract class JIPipePrepackagedDefaultJavaPlugin extends JIPipeDefaultJavaPlugin {

    public JIPipePrepackagedDefaultJavaPlugin() {

    }

    @Override
    public List<JIPipeAuthorMetadata> getAuthors() {
        final JIPipeOrganizationMetadata HKI = new JIPipeOrganizationMetadata.Builder()
                .name("Applied Systems Biology, Leibniz Institute for Natural Product Research and Infection Biology – Hans-Knöll-Institute, Jena, Germany")
                .ror("https://ror.org/055s37c97")
                .website("https://www.leibniz-hki.de")
                .build();

        final JIPipeOrganizationMetadata uniJena = new JIPipeOrganizationMetadata.Builder()
                .name("Faculty of Biological Sciences, Friedrich Schiller University Jena, Germany")
                .ror("https://ror.org/05qpz1x62")
                .website("https://www.uni-jena.de")
                .build();

        final JIPipeOrganizationMetadata uniJena2 = new JIPipeOrganizationMetadata.Builder()
                .name("Institute of Microbiology, Faculty of Biological Sciences, Friedrich Schiller University Jena, Germany")
                .ror("https://ror.org/05qpz1x62")
                .website("https://www.uni-jena.de/mikrobiologie")
                .build();

        return Arrays.asList(
                new JIPipeAuthorMetadata.Builder()
                        .title("Dr.")
                        .firstName("Zoltán")
                        .lastName("Cseresnyés")
                        .website("https://www.leibniz-hki.de/en/staff-details.html?member=144")
                        .email("zoltan.cseresnyes@leibniz-hki.de")
                        .orcid("https://orcid.org/0000-0002-6574-2588")
                        .firstAuthor(true)
                        .correspondingAuthor(false)
                        .affiliations(Collections.singletonList(HKI))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .title("")
                        .firstName("Ruman")
                        .lastName("Gerst")
                        .website("https://www.leibniz-hki.de/en/staff-details.html?member=1027")
                        .email("ruman.gerst@leibniz-hki.de")
                        .orcid("https://orcid.org/0000-0002-0723-6038")
                        .firstAuthor(true)
                        .correspondingAuthor(false)
                        .affiliations(Arrays.asList(HKI, uniJena))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .title("Prof. Dr.")
                        .firstName("Marc Thilo")
                        .lastName("Figge")
                        .website("https://www.leibniz-hki.de/en/staff-details.html?member=81")
                        .email("thilo.figge@leibniz-hki.de")
                        .orcid("https://orcid.org/0000-0002-4044-9166")
                        .firstAuthor(false)
                        .correspondingAuthor(true)
                        .affiliations(Arrays.asList(HKI, uniJena2))
                        .build()
        );

    }

    @Override
    public String getDependencyVersion() {
        return VersionUtils.getJIPipeVersion();
    }

    @Override
    public String getWebsite() {
        return "https://www.jipipe.org/";
    }

    @Override
    public String getLicense() {
        return "MIT";
    }

    @Override
    public URL getLogo() {
        return ResourceUtils.getPluginResource("logo-400.png");
    }

    @Override
    public String getCitation() {
        return "Gerst, R., Cseresnyés, Z. & Figge, M.T. JIPipe: visual batch processing for ImageJ. Nat Methods (2023). https://doi.org/10.1038/s41592-022-01744-4";
    }
}
