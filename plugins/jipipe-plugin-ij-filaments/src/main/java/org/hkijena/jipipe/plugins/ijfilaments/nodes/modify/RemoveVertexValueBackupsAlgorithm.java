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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.modify;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.plugins.ijfilaments.parameters.VertexMaskParameter;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;

@SetJIPipeDocumentation(name = "Remove filament vertex value backups", description = "Removes value backups from vertices")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = Filaments3DData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DData.class, name = "Output", create = true)
public class RemoveVertexValueBackupsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final VertexMaskParameter vertexMask;
    private JIPipeExpressionParameter backupFilter = new JIPipeExpressionParameter("name == \"old_value\"");

    public RemoveVertexValueBackupsAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.vertexMask = new VertexMaskParameter();
        registerSubParameter(vertexMask);
    }

    public RemoveVertexValueBackupsAlgorithm(RemoveVertexValueBackupsAlgorithm other) {
        super(other);
        this.backupFilter = new JIPipeExpressionParameter(other.backupFilter);
        this.vertexMask = new VertexMaskParameter(other.vertexMask);
        registerSubParameter(vertexMask);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData filaments = new Filaments3DData(iterationStep.getInputData("Filaments", Filaments3DData.class, progressInfo));
        JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap();
        variablesMap.putAnnotations(iterationStep.getMergedTextAnnotations());
        for (FilamentVertex vertex : vertexMask.filter(filaments, filaments.vertexSet(), variablesMap)) {
            for (String key : ImmutableList.copyOf(vertex.getValueBackups().keySet())) {
                variablesMap.put("name", key);
                variablesMap.put("value", vertex.getValueBackups().get(key));
                if(backupFilter.test(variablesMap)) {
                    vertex.getValueBackups().remove(key);
                }
            }
        }
    }

    @SetJIPipeDocumentation(name = "Remove if ...", description = "Removes all vertex value backups that match the filter")
    @JIPipeParameter("backup-filter")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "name", name = "Name", description = "The name of the backup")
    @AddJIPipeExpressionParameterVariable(key = "value", name = "Value", description = "The value of the backup")
    public JIPipeExpressionParameter getBackupFilter() {
        return backupFilter;
    }

    @JIPipeParameter("backup-filter")
    public void setBackupFilter(JIPipeExpressionParameter backupFilter) {
        this.backupFilter = backupFilter;
    }

    @SetJIPipeDocumentation(name = "Vertex mask", description = "Used to filter vertices")
    @JIPipeParameter("vertex-filter")
    public VertexMaskParameter getVertexMask() {
        return vertexMask;
    }
}
