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

package org.hkijena.jipipe.plugins.forms.algorithms.generators;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.plugins.forms.algorithms.SimpleFormGeneratorAlgorithm;
import org.hkijena.jipipe.plugins.forms.datatypes.BooleanFormData;
import org.hkijena.jipipe.plugins.forms.datatypes.FormData;

@SetJIPipeDocumentation(name = "Boolean input form", description = "Creates a boolean input form (a checkbox). ")
@AddJIPipeInputSlot(value = FormData.class, name = "Existing")
@AddJIPipeOutputSlot(value = FormData.class, name = "Combined")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class BooleanFormGeneratorAlgorithm extends SimpleFormGeneratorAlgorithm {

    public BooleanFormGeneratorAlgorithm(JIPipeNodeInfo info) {
        super(info, new BooleanFormData());
    }

    public BooleanFormGeneratorAlgorithm(BooleanFormGeneratorAlgorithm other) {
        super(other);
    }

}
