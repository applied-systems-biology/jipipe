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

package org.hkijena.jipipe.extensions;

import org.hkijena.jipipe.JIPipeDefaultJavaExtension;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * {@link JIPipeDefaultJavaExtension} for internal usage
 */
public abstract class JIPipePrepackagedDefaultJavaExtension extends JIPipeDefaultJavaExtension {

    @Override
    public List<JIPipeAuthorMetadata> getAuthors() {
        final String HKI = "Applied Systems Biology, Leibniz Institute for Natural Product Research and Infection Biology – Hans-Knöll-Institute, Jena, Germany";
        final String uniJena = "Faculty of Biological Sciences, Friedrich-Schiller-University Jena, Germany";
        return Arrays.asList(new JIPipeAuthorMetadata("Zoltán", "Cseresnyés", HKI),
                new JIPipeAuthorMetadata("Ruman", "Gerst", HKI + "\n" + uniJena));
    }

    @Override
    public String getWebsite() {
        return "https://www.jipipe.org/";
    }

    @Override
    public String getLicense() {
        return "BSD 2-Clause";
    }

    @Override
    public URL getLogo() {
        return ResourceUtils.getPluginResource("logo-400.png");
    }

    @Override
    public String getCitation() {
        return "";
    }
}
