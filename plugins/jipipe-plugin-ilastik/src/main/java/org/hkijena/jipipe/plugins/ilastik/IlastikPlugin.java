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

package org.hkijena.jipipe.plugins.ilastik;

import com.google.common.collect.Sets;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryInstallArtifactRun;
import org.hkijena.jipipe.api.artifacts.JIPipeLocalArtifact;
import org.hkijena.jipipe.api.artifacts.JIPipeRemoteArtifact;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentReference;
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.metadata.JIPipeOrganizationMetadata;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterArchetype;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.core.CorePlugin;
import org.hkijena.jipipe.plugins.ilastik.datatypes.IlastikModelData;
import org.hkijena.jipipe.plugins.ilastik.environments.IlastikEnvironment;
import org.hkijena.jipipe.plugins.ilastik.environments.OptionalIlastikEnvironment;
import org.hkijena.jipipe.plugins.ilastik.nodes.ExportIlastikHDF5ImageAlgorithm;
import org.hkijena.jipipe.plugins.ilastik.nodes.IlastikPixelClassificationAlgorithm;
import org.hkijena.jipipe.plugins.ilastik.nodes.ImportIlastikHDF5ImageAlgorithm;
import org.hkijena.jipipe.plugins.ilastik.nodes.ImportIlastikModel;
import org.hkijena.jipipe.plugins.ilastik.parameters.IlastikProjectValidationMode;
import org.hkijena.jipipe.plugins.ilastik.settings.IlastikPluginApplicationSettings;
import org.hkijena.jipipe.plugins.ilastik.settings.IlastikPluginProjectSettings;
import org.hkijena.jipipe.plugins.imagejalgorithms.ImageJAlgorithmsPlugin;
import org.hkijena.jipipe.plugins.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.*;

@Plugin(type = JIPipeJavaPlugin.class)
public class IlastikPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:ilastik",
            JIPipe.getJIPipeVersion(),
            "Ilastik integration");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(IlastikPlugin.class, "org/hkijena/jipipe/plugins/ilastik");

    public IlastikPlugin() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_DEEP_LEARNING,
                PluginCategoriesEnumParameter.CATEGORY_SEGMENTATION,
                PluginCategoriesEnumParameter.CATEGORY_CLASSIFICATION,
                PluginCategoriesEnumParameter.CATEGORY_MACHINE_LEARNING);
    }

    /**
     * Runs Ilastik
     *
     * @param environment  the environment. can be null (then the {@link IlastikPluginApplicationSettings} environment is taken)
     * @param parameters   the cli parameters
     * @param detached     if the process is launched detached
     * @param progressInfo the progress info
     */
    public static void runIlastik(IlastikEnvironment environment, List<String> parameters, boolean detached, JIPipeProgressInfo progressInfo) {

        // Environment variables
        Map<String, String> environmentVariables = new HashMap<>();
        if (IlastikPluginApplicationSettings.getInstance().getMaxThreads() > 0) {
            environmentVariables.put("LAZYFLOW_THREADS", String.valueOf(IlastikPluginApplicationSettings.getInstance().getMaxThreads()));
        }
        environmentVariables.put("LAZYFLOW_TOTAL_RAM_MB", String.valueOf(Math.max(256, IlastikPluginApplicationSettings.getInstance().getMaxMemory())));
        environmentVariables.put("LANG", "en_US.UTF-8");
        environmentVariables.put("LC_ALL", "en_US.UTF-8");
        environmentVariables.put("LC_CTYPE", "en_US.UTF-8");

        environment.runExecutable(parameters, environmentVariables, detached, progressInfo);
    }

    public static JIPipeEnvironmentReference<IlastikEnvironment> getEnvironment(JIPipeProject project, OptionalIlastikEnvironment nodeEnvironment, JIPipeGraphNode node) {
        var selector = JIPipeEnvironmentReference.defaultOptions(IlastikEnvironment.class)
                .application(IlastikPluginApplicationSettings.getInstance().getReadOnlyDefaultEnvironment());
        if (nodeEnvironment != null) {
            selector.node(nodeEnvironment, node);
        }
        if (project != null) {
            selector.project(project.getSettingsSheet(IlastikPluginProjectSettings.class).getProjectDefaultEnvironment(), project);
        }
        return selector.select();
    }

    public static void launchIlastik(JIPipeDesktopWorkbench workbench, List<String> arguments) {
        IlastikEnvironment environment = IlastikPlugin.getEnvironment(workbench.getProject(), null, null).getEnvironment();
        if (!environment.generateValidityReport(new UnspecifiedValidationReportContext(), JIPipeValidationReportSettings.DEFAULT).isValid()) {
            JOptionPane.showMessageDialog(workbench.getWindow(),
                    "Ilastik is currently not correctly installed. Please check the project/application settings and ensure that Ilastik is setup correctly.",
                    "Launch Ilastik",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (environment.isLoadFromArtifact()) {
            JIPipeArtifact artifact = JIPipe.getArtifacts().searchClosestCompatibleArtifactFromQuery(environment.getArtifactQuery().getQuery());
            if (artifact instanceof JIPipeLocalArtifact) {
                environment.applyConfigurationFromArtifact((JIPipeLocalArtifact) artifact, new JIPipeProgressInfo());
            } else if (artifact instanceof JIPipeRemoteArtifact) {
                if (JOptionPane.showConfirmDialog(workbench.getWindow(), "The Ilastik version " + artifact.getVersion() + " is currently not downloaded. " +
                        "Download it now?", "Run Ilastik", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    JIPipeArtifactRepositoryInstallArtifactRun run = new JIPipeArtifactRepositoryInstallArtifactRun((JIPipeRemoteArtifact) artifact);
                    JIPipeDesktopRunExecuteUI.runInDialog(workbench, workbench.getWindow(), run);
                    artifact = JIPipe.getArtifacts().queryCachedArtifact(artifact.getFullId(JIPipeArtifact.ResolutionStatus.GroupNameVersion));
                    if (artifact instanceof JIPipeLocalArtifact) {
                        environment.applyConfigurationFromArtifact((JIPipeLocalArtifact) artifact, new JIPipeProgressInfo());
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }
        }
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        progressInfo.setLogToStdOut(true);
        workbench.sendStatusBarText("Launching Ilastik ...");
        IlastikPlugin.runIlastik(environment, arguments, true, progressInfo);
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CorePlugin.AS_DEPENDENCY, ImageJDataTypesPlugin.AS_DEPENDENCY, ImageJAlgorithmsPlugin.AS_DEPENDENCY);
    }

    @Override
    public boolean isBeta() {
        return true;
    }

    @Override
    public JIPipeAuthorMetadata.List getAcknowledgements() {
        // Shared affiliations
        final JIPipeOrganizationMetadata janelia = new JIPipeOrganizationMetadata.Builder()
                .name("HHMI Janelia Research Campus, Ashburn, Virginia, USA")
                .ror("https://ror.org/013sk6x84")
                .website("https://www.janelia.org")
                .build();

        final JIPipeOrganizationMetadata heidelbergHCI = new JIPipeOrganizationMetadata.Builder()
                .name("HCI/IWR, Heidelberg University, Heidelberg, Germany")
                .ror("https://ror.org/04rcqnp59")
                .website("https://hci.iwr.uni-heidelberg.de")
                .build();

        final JIPipeOrganizationMetadata emblHeidelberg = new JIPipeOrganizationMetadata.Builder()
                .name("European Molecular Biology Laboratory, Heidelberg, Germany")
                .ror("https://ror.org/03mstc592")
                .website("https://www.embl.org")
                .build();

        return new JIPipeAuthorMetadata.List(
                new JIPipeAuthorMetadata.Builder()
                        .firstName("Stuart")
                        .lastName("Berg")
                        .affiliations(List.of(janelia))
                        .firstAuthor(true)
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Dominik")
                        .lastName("Kutra")
                        .affiliations(List.of(heidelbergHCI, emblHeidelberg))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Thorben")
                        .lastName("Kroeger")
                        .affiliations(List.of(heidelbergHCI))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Christoph N.")
                        .lastName("Straehle")
                        .affiliations(List.of(heidelbergHCI))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Bernhard X.")
                        .lastName("Kausler")
                        .affiliations(List.of(heidelbergHCI))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Carsten")
                        .lastName("Haubold")
                        .affiliations(List.of(heidelbergHCI))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Martin")
                        .lastName("Schiegg")
                        .affiliations(List.of(heidelbergHCI))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Janez")
                        .lastName("Ales")
                        .affiliations(List.of(heidelbergHCI))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Thorsten")
                        .lastName("Beier")
                        .affiliations(List.of(heidelbergHCI))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Markus")
                        .lastName("Rudy")
                        .affiliations(List.of(heidelbergHCI))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Kemal")
                        .lastName("Eren")
                        .affiliations(List.of(heidelbergHCI))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Jaime I")
                        .lastName("Cervantes")
                        .affiliations(List.of(heidelbergHCI))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Buote")
                        .lastName("Xu")
                        .affiliations(List.of(heidelbergHCI))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Fynn")
                        .lastName("Beuttenmueller")
                        .affiliations(List.of(heidelbergHCI, emblHeidelberg))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Adrian")
                        .lastName("Wolny")
                        .affiliations(List.of(heidelbergHCI))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Chong")
                        .lastName("Zhang")
                        .affiliations(List.of(heidelbergHCI))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Ullrich")
                        .lastName("Koethe")
                        .affiliations(List.of(heidelbergHCI))
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Fred A.")
                        .lastName("Hamprecht")
                        .affiliations(List.of(heidelbergHCI))
                        .email("fred.hamprecht@iwr.uni-heidelberg.de")
                        .correspondingAuthor(true)
                        .build(),

                new JIPipeAuthorMetadata.Builder()
                        .firstName("Anna")
                        .lastName("Kreshuk")
                        .affiliations(List.of(heidelbergHCI, emblHeidelberg))
                        .email("anna.kreshuk@embl.de")
                        .correspondingAuthor(true)
                        .build()
        );

    }

    @Override
    public StringList getDependencyCitations() {
        StringList strings = new StringList();
        strings.add("Berg, S., Kutra, D., Kroeger, T., Straehle, C. N., Kausler, B. X., Haubold, C., ... & Kreshuk, A. (2019). Ilastik: interactive machine learning for (bio) image analysis. Nature methods, 16(12), 1226-1232.");
        return strings;
    }

    @Override
    public String getName() {
        return "Ilastik integration";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates Ilastik");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:ilastik";
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(RESOURCES.getIcon32("ilastik.png"));
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        IlastikPluginApplicationSettings settings = new IlastikPluginApplicationSettings();
        registerApplicationSettingsSheet(settings);
        registerProjectSettingsSheet(IlastikPluginProjectSettings.class);
        registerMenuExtension(RunIlastikDesktopMenuExtension.class);
        registerDatatype("ilastik-model", IlastikModelData.class, RESOURCES.getIcon16URL("ilastik-model.png"));

        registerEnvironment(IlastikEnvironment.class,
                IlastikEnvironment.List.class,
                settings,
                "ilastik-environment",
                "Ilastik environment",
                "An Ilastik environment",
                RESOURCES.getIcon16("ilastik.png"));
        registerParameterType("optional-ilastik-environment",
                OptionalIlastikEnvironment.class,
                JIPipeParameterArchetype.Value, null,
                null,
                "Optional Ilastik environment",
                "An optional Ilastik environment",
                null);
        registerEnumParameterType("ilastik-project-validation-mode",
                IlastikProjectValidationMode.class,
                "Ilastik project validation mode",
                "Determines how Ilastik projects are checked");


        registerNodeType("import-ilastik-model", ImportIlastikModel.class);
        registerNodeType("import-ilastik-hdf5-image", ImportIlastikHDF5ImageAlgorithm.class);
        registerNodeType("export-ilastik-hdf5-image", ExportIlastikHDF5ImageAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/document-export.png"));
        registerNodeType("ilastik-pixel-classification", IlastikPixelClassificationAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
//        registerNodeType("ilastik-autocontext", IlastikAutoContextAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
//        registerNodeType("ilastik-object-classification", IlastikObjectClassificationAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-math-expression.png"));
    }
}
