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

package org.hkijena.jipipe.extensions;

import org.hkijena.jipipe.JIPipeDefaultJavaPlugin;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.VersionUtils;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * {@link JIPipeDefaultJavaPlugin} for internal usage
 */
public abstract class JIPipePrepackagedDefaultJavaPlugin extends JIPipeDefaultJavaPlugin {

    public JIPipePrepackagedDefaultJavaPlugin() {

    }

    @Override
    public List<JIPipeAuthorMetadata> getAuthors() {
        final String HKI = "Applied Systems Biology, Leibniz Institute for Natural Product Research and Infection Biology – Hans-Knöll-Institute, Jena, Germany";
        final String uniJena = "Faculty of Biological Sciences, Friedrich Schiller University Jena, Germany";
        final String uniJena2 = "Institute of Microbiology, Faculty of Biological Sciences, Friedrich Schiller University Jena, Germany";
        return Arrays.asList(new JIPipeAuthorMetadata("Dr.",
                        "Zoltán",
                        "Cseresnyés",
                        new StringList(HKI),
                        "https://www.leibniz-hki.de/en/staff-details.html?member=144",
                        "zoltan.cseresnyes@leibniz-hki.de",
                        true,
                        false),
                new JIPipeAuthorMetadata("",
                        "Ruman",
                        "Gerst",
                        new StringList(HKI, uniJena),
                        "https://www.leibniz-hki.de/en/staff-details.html?member=1027",
                        "ruman.gerst@leibniz-hki.de",
                        true,
                        false),
                new JIPipeAuthorMetadata("Prof. Dr.",
                        "Marc Thilo",
                        "Figge",
                        new StringList(HKI, uniJena2),
                        "https://www.leibniz-hki.de/en/staff-details.html?member=81",
                        "thilo.figge@leibniz-hki.de",
                        false,
                        true));
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
