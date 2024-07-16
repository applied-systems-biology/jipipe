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
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.plugins.ijfilaments.parameters.VertexMaskParameter;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalStringParameter;

@SetJIPipeDocumentation(name = "Restore filament vertex value backups", description = "Restore value backups from vertices")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = Filaments3DData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DData.class, name = "Output", create = true)
public class RestoreVertexValueBackupAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final VertexMaskParameter vertexMask;
    private StringQueryExpression backupFilter = new StringQueryExpression("\"old_value\"");
    private OptionalStringParameter backupOldValue = new OptionalStringParameter("old_value", false);
    private JIPipeExpressionParameter missingValue = new JIPipeExpressionParameter("current_value");

    public RestoreVertexValueBackupAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.vertexMask = new VertexMaskParameter();
        registerSubParameter(vertexMask);
    }

    public RestoreVertexValueBackupAlgorithm(RestoreVertexValueBackupAlgorithm other) {
        super(other);
        this.backupOldValue = other.backupOldValue;
        this.backupFilter = new StringQueryExpression(other.backupFilter);
        this.vertexMask = new VertexMaskParameter(other.vertexMask);
        this.missingValue = new JIPipeExpressionParameter(other.missingValue);
        registerSubParameter(vertexMask);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData filaments = new Filaments3DData(iterationStep.getInputData("Filaments", Filaments3DData.class, progressInfo));
        JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap();
        variablesMap.putAnnotations(iterationStep.getMergedTextAnnotations());
        for (FilamentVertex vertex : vertexMask.filter(filaments, filaments.vertexSet(), variablesMap)) {
            String key = backupFilter.queryFirst(vertex.getValueBackups().keySet(), variablesMap);
            Double value = vertex.getValueBackups().getOrDefault(key, null);
            if(value == null) {
                variablesMap.put("current_value", vertex.getValue());
                value = missingValue.evaluateToDouble(variablesMap);
            }

            if(backupOldValue.isEnabled()) {
                vertex.getValueBackups().put(backupOldValue.getContent(), value);
            }
            vertex.setValue(value);
        }
    }

    @SetJIPipeDocumentation(name = "Backup", description = "Selects the backup by its key")
    @JIPipeParameter("backup-filter")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public StringQueryExpression getBackupFilter() {
        return backupFilter;
    }

    @JIPipeParameter("backup-filter")
    public void setBackupFilter(StringQueryExpression backupFilter) {
        this.backupFilter = backupFilter;
    }

    @SetJIPipeDocumentation(name = "Vertex mask", description = "Used to filter vertices")
    @JIPipeParameter("vertex-filter")
    public VertexMaskParameter getVertexMask() {
        return vertexMask;
    }

    @SetJIPipeDocumentation(name = "Backup old value", description = "If enabled, backup the value to the specified storage")
    @JIPipeParameter("backup-old-value")
    @StringParameterSettings(monospace = true)
    public OptionalStringParameter getBackupOldValue() {
        return backupOldValue;
    }

    @JIPipeParameter("backup-old-value")
    public void setBackupOldValue(OptionalStringParameter backupOldValue) {
        this.backupOldValue = backupOldValue;
    }

    @SetJIPipeDocumentation(name = "Missing value", description = "Used if the backup is missing")
    @JIPipeParameter("missing-value")
    @AddJIPipeExpressionParameterVariable(key = "current_value", name = "Current value", description = "The current vertex value")
    public JIPipeExpressionParameter getMissingValue() {
        return missingValue;
    }

    @JIPipeParameter("missing-value")
    public void setMissingValue(JIPipeExpressionParameter missingValue) {
        this.missingValue = missingValue;
    }
}
