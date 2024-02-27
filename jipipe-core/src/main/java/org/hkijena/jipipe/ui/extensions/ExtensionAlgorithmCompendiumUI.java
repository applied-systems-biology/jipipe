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

package org.hkijena.jipipe.ui.extensions;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipePlugin;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.ui.documentation.JIPipeAlgorithmCompendiumUI;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ExtensionAlgorithmCompendiumUI extends JIPipeAlgorithmCompendiumUI {
    private final JIPipePlugin extension;

    public ExtensionAlgorithmCompendiumUI(JIPipePlugin extension) {
        this.extension = extension;
    }

    @Override
    protected List<JIPipeNodeInfo> getFilteredItems() {
        Predicate<JIPipeNodeInfo> filterFunction = info -> getSearchField().test(info.getName() + " " + info.getDescription() + " " + info.getMenuPath());

        return JIPipe.getNodes().getDeclaredBy(extension).stream().filter(filterFunction)
                .sorted(Comparator.comparing(JIPipeNodeInfo::getName)).collect(Collectors.toList());
    }
}
