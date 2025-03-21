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

package org.hkijena.jipipe.plugins.utils.algorithms.meta;

import com.google.common.collect.ImmutableList;
import ij.IJ;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.JIPipeProjectInfoParameters;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterAccessTreeUI;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;
import org.hkijena.jipipe.plugins.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.plugins.parameters.library.graph.InputSlotMapParameterCollection;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

@SetJIPipeDocumentation(name = "Paths to JIPipe project parameters", description = "Stores the incoming paths into parameters. This node supports path and string parameters.")
@AddJIPipeInputSlot(value = PathData.class, name = "Input")
@AddJIPipeOutputSlot(value = ParametersData.class, name = "Parameters")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Meta run")
public class PathsToJIPipeProjectParametersAlgorithm extends JIPipeIteratingAlgorithm {

    private InputSlotMapParameterCollection parameterKeyAssignments;

    public PathsToJIPipeProjectParametersAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .restrictInputTo(PathData.class)
                .addOutputSlot("Parameters", "The project parameters", ParametersData.class)
                .sealOutput()
                .build());
        this.parameterKeyAssignments = new InputSlotMapParameterCollection(String.class, this, (slotInfo) -> "", false);
        parameterKeyAssignments.getBeforeAddParameterEventEmitter().subscribe(new ParameterWatcher());
        registerSubParameter(parameterKeyAssignments);
    }

    public PathsToJIPipeProjectParametersAlgorithm(PathsToJIPipeProjectParametersAlgorithm other) {
        super(other);
        this.parameterKeyAssignments = new InputSlotMapParameterCollection(String.class, this, (slotInfo) -> "", false);
        parameterKeyAssignments.getBeforeAddParameterEventEmitter().subscribe(new ParameterWatcher());
        other.parameterKeyAssignments.copyTo(parameterKeyAssignments);
        registerSubParameter(parameterKeyAssignments);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ParametersData data = new ParametersData();
        for (Map.Entry<String, JIPipeParameterAccess> entry : parameterKeyAssignments.getParameters().entrySet()) {
            PathData pathData = iterationStep.getInputData(entry.getKey(), PathData.class, progressInfo);
            JIPipeParameterAccess access = entry.getValue();
            String targetKey = access.get(String.class);
            data.getParameterData().put(targetKey, pathData.getPath());
        }
        iterationStep.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Parameter assignments", description = "The value from each input slot is stored into a parameter with given unique ID. You can look up this unique ID inside the project parameters or " +
            "set it to [node id]/[parameter key] (see node/parameter documentation to look up those keys)")
    @JIPipeParameter(value = "parameter-key-assignments", uiOrder = -100)
    public InputSlotMapParameterCollection getParameterKeyAssignments() {
        return parameterKeyAssignments;
    }

    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/apps/jipipe.png")
    @SetJIPipeDocumentation(name = "Load parameters from project", description = "Loads parameters from a project file")
    public void importParametersFromProject(JIPipeWorkbench workbench) {
        Window window = ((JIPipeDesktopWorkbench) workbench).getWindow();
        Path projectFile = JIPipeDesktop.openFile(window, workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Import JIPipe project", HTMLText.EMPTY, UIUtils.EXTENSION_FILTER_JIP);
        if (projectFile != null) {
            try {
                JIPipeProject project = JIPipeProject.loadProject(projectFile, new UnspecifiedValidationReportContext(), new JIPipeValidationReport(), new JIPipeNotificationInbox());
                JIPipeProjectInfoParameters infoParameters = project.getPipelineParameters();
                JIPipeParameterTree tree = new JIPipeParameterTree(infoParameters);
                for (Map.Entry<String, JIPipeParameterAccess> entry : ImmutableList.copyOf(tree.getParameters().entrySet())) {
                    Class<?> fieldClass = entry.getValue().getFieldClass();
                    if (!fieldClass.isAssignableFrom(Path.class)) {
                        tree.removeParameterByKey(entry.getKey());
                    }
                }
                tree.removeParameterByKey("exported-parameters");
                if (tree.getParameters().isEmpty()) {
                    JOptionPane.showMessageDialog(window, "No compatible parameters found. Please add string or path parameters to the list of project-wide parameters.", "Import parameters", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                List<Object> objects = JIPipeDesktopParameterAccessTreeUI.showPickerDialog(window, tree, "Select parameters to import");
                if (objects.isEmpty())
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
                    if (StringUtils.isNullOrEmpty(slotName))
                        slotName = key;
                    slotName = StringUtils.makeUniqueString(StringUtils.makeFilesystemCompatible(slotName), " ", existing);
                    slotConfiguration.addSlot(slotName, new JIPipeDataSlotInfo(PathData.class, JIPipeSlotType.Input), true);
                    parameterKeyAssignments.get(slotName).set(key);
                }
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
    }

    private static class ParameterWatcher implements JIPipeDynamicParameterCollection.BeforeAddParameterEventListener {

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

            @Override
            public boolean visible() {
                return true;
            }
        };

        @Override
        public void onDynamicParameterCollectionBeforeAddParameter(JIPipeDynamicParameterCollection.BeforeAddParameterEvent event) {
            event.getAccess().getAnnotationMap().put(StringParameterSettings.class, SETTINGS);
        }
    }
}
