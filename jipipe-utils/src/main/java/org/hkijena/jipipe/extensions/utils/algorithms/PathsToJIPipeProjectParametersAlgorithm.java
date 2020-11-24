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

package org.hkijena.jipipe.extensions.utils.algorithms;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;
import ij.IJ;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.extensions.parameters.collections.InputSlotMapParameterCollection;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.ParameterTreeUI;
import org.hkijena.jipipe.ui.settings.JIPipeProjectInfoParameters;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JIPipeDocumentation(name = "Paths to JIPipe project parameters", description = "Stores the incoming paths into parameters. This node supports path and string parameters.")
@JIPipeInputSlot(value = PathData.class, slotName = "Input")
@JIPipeOutputSlot(value = ParametersData.class, slotName = "Parameters")
@JIPipeOrganization(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Meta run")
public class PathsToJIPipeProjectParametersAlgorithm extends JIPipeIteratingAlgorithm {

    private InputSlotMapParameterCollection parameterKeyAssignments;

    public PathsToJIPipeProjectParametersAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
        .restrictInputTo(PathData.class)
        .addOutputSlot("Parameters", ParametersData.class, null)
        .sealOutput()
        .build());
        this.parameterKeyAssignments = new InputSlotMapParameterCollection(String.class, this, () -> "", false);
        parameterKeyAssignments.getEventBus().register(new ParameterWatcher());
        registerSubParameter(parameterKeyAssignments);
    }

    public PathsToJIPipeProjectParametersAlgorithm(PathsToJIPipeProjectParametersAlgorithm other) {
        super(other);
        this.parameterKeyAssignments = new InputSlotMapParameterCollection(String.class, this, () -> "", false);
        parameterKeyAssignments.getEventBus().register(new ParameterWatcher());
        other.parameterKeyAssignments.copyTo(parameterKeyAssignments);
        registerSubParameter(parameterKeyAssignments);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ParametersData data = new ParametersData();
        for (Map.Entry<String, JIPipeParameterAccess> entry : parameterKeyAssignments.getParameters().entrySet()) {
            PathData pathData = dataBatch.getInputData(entry.getKey(), PathData.class, progressInfo);
            JIPipeParameterAccess access = entry.getValue();
            String targetKey = access.get(String.class);
            data.getParameterData().put(targetKey, pathData.getPath());
        }
        dataBatch.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @JIPipeDocumentation(name = "Parameter assignments", description = "The value from each input slot is stored into a parameter with given unique ID. You can look up this unique ID inside the project parameters or " +
            "set it to [node id]/[parameter key] (see node/parameter documentation to look up those keys)")
    @JIPipeParameter(value = "parameter-key-assignments", uiOrder = -100)
    public InputSlotMapParameterCollection getParameterKeyAssignments() {
        return parameterKeyAssignments;
    }

    @JIPipeContextAction(iconURL =  ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/jipipe.png")
    @JIPipeDocumentation(name = "Load parameters from project", description = "Loads parameters from a project file")
    public void importParametersFromProject(JIPipeWorkbench workbench) {
        Path projectFile = FileChooserSettings.openFile(workbench.getWindow(), FileChooserSettings.KEY_PROJECT, "Import JIPipe project", UIUtils.EXTENSION_FILTER_JIP);
        if(projectFile != null) {
            try {
                JIPipeProject project = JIPipeProject.loadProject(projectFile, new JIPipeValidityReport());
                JIPipeProjectInfoParameters infoParameters = project.getPipelineParameters();
                JIPipeParameterTree tree = new JIPipeParameterTree(infoParameters);
                for (Map.Entry<String, JIPipeParameterAccess> entry : ImmutableList.copyOf(tree.getParameters().entrySet())) {
                    Class<?> fieldClass = entry.getValue().getFieldClass();
                    if(!fieldClass.isAssignableFrom(Path.class)) {
                        tree.removeParameterByKey(entry.getKey());
                    }
                }
                tree.removeParameterByKey("exported-parameters");
                if(tree.getParameters().isEmpty()) {
                    JOptionPane.showMessageDialog(workbench.getWindow(), "No compatible parameters found. Please add string or path parameters to the list of project-wide parameters.", "Import parameters", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                List<Object> objects = ParameterTreeUI.showPickerDialog(workbench.getWindow(), tree, "Select parameters to import");
                if(objects.isEmpty())
                    return;
                JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
                slotConfiguration.clearInputSlots(true);
                Set<String> existing = new HashSet<>();
                List<JIPipeParameterAccess> toAdd = new ArrayList<>();
                for (Object object : objects) {
                    toAdd.addAll(tree.getAllChildParameters(object));
                }
                for (JIPipeParameterAccess access : toAdd) {
                    String key = tree.getUniqueKey(access);
                    String slotName = access.getName();
                    if(StringUtils.isNullOrEmpty(slotName))
                        slotName = key;
                    slotName = StringUtils.makeUniqueString(StringUtils.makeFilesystemCompatible(slotName), " ", existing);
                    slotConfiguration.addSlot(slotName, new JIPipeDataSlotInfo(PathData.class, JIPipeSlotType.Input, null), true);
                    parameterKeyAssignments.get(slotName).set(key);
                }
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
    }

    private static class ParameterWatcher {

        private static final StringParameterSettings SETTINGS = new StringParameterSettings() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return StringParameterSettings.class;
            }

            @Override
            public boolean multiline() {
                return false;
            }

            @Override
            public boolean monospace() {
                return true;
            }

            @Override
            public String icon() {
                return ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/parameters.png";
            }

            @Override
            public String prompt() {
                return null;
            }
        };

        @Subscribe
        public void onParameterAdding(JIPipeDynamicParameterCollection.ParameterAddingEvent event) {
            event.getAccess().getAnnotationMap().put(StringParameterSettings.class, SETTINGS);
        }
    }
}
