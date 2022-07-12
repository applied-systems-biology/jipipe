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
 *
 */

package org.hkijena.jipipe.ui.extensions;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeExtension;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.ui.documentation.JIPipeDataTypeCompendiumUI;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ExtensionDataTypeCompendiumUI extends JIPipeDataTypeCompendiumUI {
    private final JIPipeExtension extension;

    public ExtensionDataTypeCompendiumUI(JIPipeExtension extension) {
        this.extension = extension;
    }

    @Override
    protected List<JIPipeDataInfo> getFilteredItems() {
        if(extension == null)
            return Collections.emptyList();
        if (dataInfos == null)
            dataInfos = JIPipe.getDataTypes().getDeclaredBy(extension).stream().sorted(Comparator.comparing(JIPipeDataInfo::getName)).collect(Collectors.toList());
        Predicate<JIPipeDataInfo> filterFunction = info -> getSearchField().test(info.getName() + " " + info.getDescription() + " " + info.getMenuPath());
        return dataInfos.stream().filter(filterFunction).collect(Collectors.toList());
    }
}
