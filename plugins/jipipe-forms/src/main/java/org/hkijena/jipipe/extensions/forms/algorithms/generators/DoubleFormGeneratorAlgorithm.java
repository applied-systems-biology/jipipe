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

package org.hkijena.jipipe.extensions.forms.algorithms.generators;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.forms.algorithms.SimpleFormGeneratorAlgorithm;
import org.hkijena.jipipe.extensions.forms.datatypes.DoubleFormData;
import org.hkijena.jipipe.extensions.forms.datatypes.FormData;

@SetJIPipeDocumentation(name = "Number input form", description = "Creates a real number input form. ")
@AddJIPipeInputSlot(value = FormData.class, slotName = "Existing")
@AddJIPipeOutputSlot(value = FormData.class, slotName = "Combined")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class DoubleFormGeneratorAlgorithm extends SimpleFormGeneratorAlgorithm {

    public DoubleFormGeneratorAlgorithm(JIPipeNodeInfo info) {
        super(info, new DoubleFormData());
    }

    public DoubleFormGeneratorAlgorithm(DoubleFormGeneratorAlgorithm other) {
        super(other);
    }

}
