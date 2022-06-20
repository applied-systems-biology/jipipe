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

package org.hkijena.jipipe.extensions.ij3d;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.net.URL;
import java.util.Collections;
import java.util.List;

@Plugin(type = JIPipeJavaExtension.class)
public class IJ3DExtension extends JIPipePrepackagedDefaultJavaExtension {

    public static final String RESOURCE_BASE_PATH = "/org/hkijena/jipipe/extensions/ij3d";

    @Override
    public StringList getDependencyCitations() {
        StringList strings = new StringList();
        strings.add("J. Ollion, J. Cochennec, F. Loll, C. Escudé, T. Boudier. (2013) TANGO: A Generic Tool for High-throughput 3D Image Analysis for Studying Nuclear Organization. Bioinformatics 2013 Jul 15;29(14):1840-1.");
        return strings;
    }

    @Override
    public String getName() {
        return "IJ Trainable Weka Filter integration";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates the Trainable Weka Filter into JIPipe");
    }

//    @Override
//    public List<ImageIcon> getSplashIcons() {
//        return Collections.singletonList(new ImageIcon(getClass().getResource(RESOURCE_BASE_PATH + "/weka-32.png")));
//    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:ij-3d";
    }

}
