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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.generate;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.desktop.api.nodes.AddJIPipeDesktopNodeQuickAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsPlugin;
import org.hkijena.jipipe.plugins.ijfilaments.environments.OptionalTSOAXEnvironment;
import org.hkijena.jipipe.plugins.ijfilaments.environments.TSOAXEnvironment;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class TSOAXAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final TSOAXConvergenceParameters convergenceParameters;
    private final TSOAXEvolutionParameters evolutionParameters;
    private final TSOAXInitializationParameters initializationParameters;
    private boolean cleanUpAfterwards = true;
    private boolean splitByTrack = true;
    private OptionalTSOAXEnvironment overrideEnvironment = new OptionalTSOAXEnvironment();
    private OptionalTextAnnotationNameParameter trackAnnotationName = new OptionalTextAnnotationNameParameter("Track", true);


    public TSOAXAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.convergenceParameters = new TSOAXConvergenceParameters();
        this.evolutionParameters = new TSOAXEvolutionParameters();
        this.initializationParameters = new TSOAXInitializationParameters();
        registerSubParameters(convergenceParameters, evolutionParameters, initializationParameters);
    }

    public TSOAXAlgorithm(TSOAXAlgorithm other) {
        super(other);
        this.convergenceParameters = new TSOAXConvergenceParameters(other.convergenceParameters);
        this.evolutionParameters = new TSOAXEvolutionParameters(other.evolutionParameters);
        this.initializationParameters = new TSOAXInitializationParameters(other.initializationParameters);
        this.cleanUpAfterwards = other.cleanUpAfterwards;
        this.overrideEnvironment = new OptionalTSOAXEnvironment(other.overrideEnvironment);
        this.trackAnnotationName = new OptionalTextAnnotationNameParameter(trackAnnotationName);
        this.splitByTrack = other.splitByTrack;
        registerSubParameters(convergenceParameters, evolutionParameters, initializationParameters);
    }

    public void saveParameterFile(Path outputFile) {
        Map<String, String> data = new HashMap<>();
        data.put("intensity-scaling", String.valueOf(initializationParameters.getIntensityScaling()));
        data.put("gaussian-std", String.valueOf(initializationParameters.getGaussianStd()));
        data.put("ridge-threshold", String.valueOf(initializationParameters.getRidgeThreshold()));
        data.put("maximum-foreground", String.valueOf(initializationParameters.getMaximumForeground()));
        data.put("minimum-foreground", String.valueOf(initializationParameters.getMinimumForeground()));
        data.put("init-z", String.valueOf(initializationParameters.isInitZ()));
        data.put("snake-point-spacing", String.valueOf(initializationParameters.getSnakePointSpacing()));
        data.put("minimum-snake-length", String.valueOf(convergenceParameters.getMinimumSnakeLength()));
        data.put("maximum-iterations", String.valueOf(convergenceParameters.getMaximumIterations()));
        data.put("change-threshold", String.valueOf(convergenceParameters.getChangeThreshold()));
        data.put("check-period", String.valueOf(convergenceParameters.getCheckPeriod()));
        data.put("alpha", String.valueOf(evolutionParameters.getAlpha()));
        data.put("beta", String.valueOf(evolutionParameters.getBeta()));
        data.put("gamma", String.valueOf(evolutionParameters.getGamma()));
        data.put("external-factor", String.valueOf(evolutionParameters.getExternalFactor()));
        data.put("stretch-factor", String.valueOf(evolutionParameters.getStretchFactor()));
        data.put("number-of-background-radial-sectors", String.valueOf(evolutionParameters.getNumberOfBackgroundRadialSectors()));
        data.put("background-z-xy-ratio", String.valueOf(evolutionParameters.getBackgroundZXYRatio()));
        data.put("radial-near", String.valueOf(evolutionParameters.getRadialNear()));
        data.put("radial-far", String.valueOf(evolutionParameters.getRadialFar()));
        data.put("delta", String.valueOf(evolutionParameters.getDelta()));
        data.put("overlap-threshold", String.valueOf(evolutionParameters.getOverlapThreshold()));
        data.put("grouping-distance-threshold", String.valueOf(evolutionParameters.getGroupingDistanceThreshold()));
        data.put("grouping-delta", String.valueOf(evolutionParameters.getGroupingDelta()));
        data.put("minimum-angle-for-soac-linking", String.valueOf(evolutionParameters.getMinimumAngleForSOACLinking()));
        data.put("damp-z", String.valueOf(evolutionParameters.isDampZ()));
        data.put("association-threshold", String.valueOf(evolutionParameters.getAssociationThreshold()));
        data.put("c", String.valueOf(evolutionParameters.getC()));
        data.put("grouping", String.valueOf(evolutionParameters.isGrouping()));
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile.toFile()))) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                writer.write(entry.getKey() + "\t" + entry.getValue());
                writer.newLine();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @SetJIPipeDocumentation(name = "Split by track", description = "If enabled, split the filaments by their track ID (if available)")
    @JIPipeParameter("split-by-track")
    public boolean isSplitByTrack() {
        return splitByTrack;
    }

    @JIPipeParameter("split-by-track")
    public void setSplitByTrack(boolean splitByTrack) {
        this.splitByTrack = splitByTrack;
    }

    @SetJIPipeDocumentation(name = "Annotate with track ID", description = "If enabled, add the track ID as annotation to the generated filaments")
    @JIPipeParameter("track-annotation-name")
    public OptionalTextAnnotationNameParameter getTrackAnnotationName() {
        return trackAnnotationName;
    }

    @JIPipeParameter("track-annotation-name")
    public void setTrackAnnotationName(OptionalTextAnnotationNameParameter trackAnnotationName) {
        this.trackAnnotationName = trackAnnotationName;
    }

    @SetJIPipeDocumentation(name = "Override environment", description = "If enabled, override the TSOAX environment for this node")
    @JIPipeParameter("override-environment")
    public OptionalTSOAXEnvironment getOverrideEnvironment() {
        return overrideEnvironment;
    }

    @JIPipeParameter("override-environment")
    public void setOverrideEnvironment(OptionalTSOAXEnvironment overrideEnvironment) {
        this.overrideEnvironment = overrideEnvironment;
    }

    @SetJIPipeDocumentation(name = "Clean up data after processing", description = "If enabled, data is deleted from temporary directories after " +
            "the processing was finished. Disable this to make it possible to debug your scripts. The directories are accessible via the logs (Tools &gt; Logs).")
    @JIPipeParameter("cleanup-afterwards")
    public boolean isCleanUpAfterwards() {
        return cleanUpAfterwards;
    }

    @JIPipeParameter("cleanup-afterwards")
    public void setCleanUpAfterwards(boolean cleanUpAfterwards) {
        this.cleanUpAfterwards = cleanUpAfterwards;
    }

    @SetJIPipeDocumentation(name = "Convergence", description = "Controls how junctions are resolved to minimize sharp bends.")
    @JIPipeParameter(value = "convergence", uiOrder = -30, collapsed = true)
    public TSOAXConvergenceParameters getConvergenceParameters() {
        return convergenceParameters;
    }

    @SetJIPipeDocumentation(name = "Evolution", description = "Controls how the initial stretching active contours are stretched according to the local intensity contrast. Use the stretch factor to determine the overall elongation.")
    @JIPipeParameter(value = "evolution", uiOrder = -40, collapsed = true)
    public TSOAXEvolutionParameters getEvolutionParameters() {
        return evolutionParameters;
    }

    @SetJIPipeDocumentation(name = "Initialization", description = "Determines at which intensity ridges the the initial stretching active contours are placed. Use the ridge threshold to determine the minimum intensity steepness.")
    @JIPipeParameter(value = "initialization", uiOrder = -50)
    public TSOAXInitializationParameters getInitializationParameters() {
        return initializationParameters;
    }

    @Override
    public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {
        super.getEnvironmentDependencies(target);
        target.add(getConfiguredTSOAXEnvironment());
    }

    /**
     * Gets the correct OMERO environment.
     * Adheres to the chain of overrides.
     *
     * @return the environment
     */
    public TSOAXEnvironment getConfiguredTSOAXEnvironment() {
        JIPipeGraphNode node = this;
        JIPipeProject project = node.getRuntimeProject();
        if (project == null) {
            project = node.getParentGraph().getProject();
        }
        return FilamentsPlugin.getTSOAXEnvironment(project, getOverrideEnvironment());
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        if (!getConfiguredTSOAXEnvironment().generateValidityReport(reportContext).isValid()) {
            report.report(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    reportContext,
                    "TSOAX not configured",
                    "The TSOAX integration is not configured correctly.",
                    "Go to the Project > Project settings/overview > Settings > Plugins > Filaments and setup an appropriate default TSOAX environment."));
        }
    }

    @AddJIPipeDesktopNodeQuickAction(name = "Save TSOAX parameters *.txt", description = "Saves parameters into the TSOAX parameters file format",
            icon = "actions/document-export.png", buttonIcon = "actions/filesave.png", buttonText = "Save")
    public void saveParametersDesktopQuickAction(JIPipeDesktopGraphCanvasUI canvasUI) {
        Path filePath = JIPipeFileChooserApplicationSettings.saveFile(canvasUI.getDesktopWorkbench().getWindow(),
                JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data,
                "Save TSOAX parameters",
                UIUtils.EXTENSION_FILTER_TXT);
        if (filePath != null) {
            saveParameterFile(filePath);
            canvasUI.getDesktopWorkbench().sendStatusBarText("Saved parameters to " + filePath);
        }
    }

    @AddJIPipeDesktopNodeQuickAction(name = "Load TSOAX parameters *.txt", description = "Loads parameters from a file in the TSOAX parameters format",
            icon = "actions/document-export.png", buttonIcon = "actions/fileopen.png", buttonText = "Open")
    public void loadParametersDesktopQuickAction(JIPipeDesktopGraphCanvasUI canvasUI) {
        Path filePath = JIPipeFileChooserApplicationSettings.openFile(canvasUI.getDesktopWorkbench().getWindow(),
                JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data,
                "Open TSOAX parameters",
                UIUtils.EXTENSION_FILTER_TXT);
        if (filePath != null) {
            openParameterFile(filePath);
            emitParameterUIChangedEvent();
            JOptionPane.showMessageDialog(canvasUI.getDesktopWorkbench().getWindow(),
                    "TSOAX parameters were successfully imported",
                    "Load TSOAX parameters",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void openParameterFile(Path filePath) {
        Map<String, String> data = new HashMap<>();
        try {
            for (String line : Files.readAllLines(filePath)) {
                if (!StringUtils.isNullOrEmpty(line) && line.contains("\t")) {
                    String[] items = line.split("\t");
                    if (items.length == 2) {
                        data.put(items[0].trim(), items[1].trim());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Read data
        if (data.containsKey("intensity-scaling")) {
            initializationParameters.setIntensityScaling(Double.parseDouble(data.get("intensity-scaling")));
        }
        if (data.containsKey("gaussian-std")) {
            initializationParameters.setGaussianStd(Double.parseDouble(data.get("gaussian-std")));
        }
        if (data.containsKey("ridge-threshold")) {
            initializationParameters.setRidgeThreshold(Double.parseDouble(data.get("ridge-threshold")));
        }
        if (data.containsKey("maximum-foreground")) {
            initializationParameters.setMaximumForeground(Double.parseDouble(data.get("maximum-foreground")));
        }
        if (data.containsKey("minimum-foreground")) {
            initializationParameters.setMinimumForeground(Double.parseDouble(data.get("minimum-foreground")));
        }
        if (data.containsKey("init-z")) {
            initializationParameters.setInitZ(Boolean.parseBoolean(data.get("init-z")));
        }

        if (data.containsKey("snake-point-spacing")) {
            initializationParameters.setSnakePointSpacing(Double.parseDouble(data.get("snake-point-spacing")));
        }
        if (data.containsKey("minimum-snake-length")) {
            convergenceParameters.setMinimumSnakeLength(Integer.parseInt(data.get("minimum-snake-length")));
        }
        if (data.containsKey("maximum-iterations")) {
            convergenceParameters.setMaximumIterations(Integer.parseInt(data.get("maximum-iterations")));
        }
        if (data.containsKey("change-threshold")) {
            convergenceParameters.setChangeThreshold(Double.parseDouble(data.get("change-threshold")));
        }
        if (data.containsKey("check-period")) {
            convergenceParameters.setCheckPeriod(Integer.parseInt(data.get("check-period")));
        }

        if (data.containsKey("alpha")) {
            evolutionParameters.setAlpha(Double.parseDouble(data.get("alpha")));
        }
        if (data.containsKey("beta")) {
            evolutionParameters.setBeta(Double.parseDouble(data.get("beta")));
        }
        if (data.containsKey("gamma")) {
            evolutionParameters.setGamma(Double.parseDouble(data.get("gamma")));
        }
        if (data.containsKey("external-factor")) {
            evolutionParameters.setExternalFactor(Double.parseDouble(data.get("external-factor")));
        }
        if (data.containsKey("stretch-factor")) {
            evolutionParameters.setStretchFactor(Double.parseDouble(data.get("stretch-factor")));
        }
        if (data.containsKey("number-of-background-radial-sectors")) {
            evolutionParameters.setNumberOfBackgroundRadialSectors(Integer.parseInt(data.get("number-of-background-radial-sectors")));
        }
        if (data.containsKey("background-z-xy-ratio")) {
            evolutionParameters.setBackgroundZXYRatio(Double.parseDouble(data.get("background-z-xy-ratio")));
        }
        if (data.containsKey("radial-near")) {
            evolutionParameters.setRadialNear(Integer.parseInt(data.get("radial-near")));
        }
        if (data.containsKey("radial-far")) {
            evolutionParameters.setRadialFar(Integer.parseInt(data.get("radial-far")));
        }
        if (data.containsKey("delta")) {
            evolutionParameters.setDelta(Double.parseDouble(data.get("delta")));
        }
        if (data.containsKey("overlap-threshold")) {
            evolutionParameters.setOverlapThreshold(Double.parseDouble(data.get("overlap-threshold")));
        }
        if (data.containsKey("grouping-distance-threshold")) {
            evolutionParameters.setGroupingDistanceThreshold(Double.parseDouble(data.get("grouping-distance-threshold")));
        }
        if (data.containsKey("grouping-delta")) {
            evolutionParameters.setGroupingDelta(Integer.parseInt(data.get("grouping-delta")));
        }
        if (data.containsKey("minimum-angle-for-soac-linking")) {
            evolutionParameters.setMinimumAngleForSOACLinking(Double.parseDouble(data.get("minimum-angle-for-soac-linking")));
        }
        if (data.containsKey("damp-z")) {
            evolutionParameters.setDampZ(Boolean.parseBoolean(data.get("damp-z")));
        }
        if (data.containsKey("association-threshold")) {
            evolutionParameters.setAssociationThreshold(Double.parseDouble(data.get("association-threshold")));
        }
        if (data.containsKey("c")) {
            evolutionParameters.setC(Double.parseDouble(data.get("c")));
        }
        if (data.containsKey("grouping")) {
            evolutionParameters.setGrouping(Boolean.parseBoolean(data.get("grouping")));
        }
    }

}
