package org.hkijena.jipipe.extensions.ilastik;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.processes.ProcessEnvironment;

public class IlastikSettings extends AbstractJIPipeParameterCollection {

    public static String ID = "org.hkijena.jipipe:ilastik";
    private ProcessEnvironment environment = new ProcessEnvironment();
    private StringList easyInstallerRepositories = new StringList();
    private int maxThreads = -1;
    private int maxMemory = 4096;

    public IlastikSettings() {
        easyInstallerRepositories.add("https://github.com/applied-systems-biology/JIPipe-Repositories/raw/main/easyinstall/easyinstall-ilastik.json");
        environment.setArguments(new JIPipeExpressionParameter("cli_parameters"));
    }

    public static IlastikSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, IlastikSettings.class);
    }

    /**
     * Checks the Python settings
     *
     * @return if the settings are correct
     */
    public static boolean environmentSettingsAreValid() {
        if (JIPipe.getInstance() != null) {
            IlastikSettings instance = getInstance();
            JIPipeValidationReport report = new JIPipeValidationReport();
            instance.getEnvironment().reportValidity(new UnspecifiedValidationReportContext(), report);
            return report.isValid();
        }
        return false;
    }

    /**
     * Checks if the Python settings are valid or reports an invalid state
     *
     * @param context the context
     * @param report  the report
     */
    public static void checkIlastikSettings(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        if (!environmentSettingsAreValid()) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, context, "Ilastik is not configured!",
                    "This node requires an installation of Ilastik. You have to point JIPipe to a Ilastik installation.",
                    "Please install Ilastik from https://www.ilastik.org\n" +
                            "Then go to Project > Application settings > Extensions > Ilastik and setup the environment. " +
                            "Alternatively, the settings page will provide you with means to install Ilastik automatically."));
        }
    }

    @JIPipeDocumentation(name = "Ilastik environment", description = "Contains information about the location of the Ilastik installation.")
    @JIPipeParameter("environment")
    @ExternalEnvironmentParameterSettings(showCategory = "Ilastik")
    public ProcessEnvironment getEnvironment() {
        return environment;
    }

    @JIPipeParameter("environment")
    public void setEnvironment(ProcessEnvironment environment) {
        this.environment = environment;
    }

    @JIPipeDocumentation(name = "Easy installer repositories", description = "Allows to change the repositories for the EasyInstaller")
    @JIPipeParameter("easy-installer-repositories")
    public StringList getEasyInstallerRepositories() {
        return easyInstallerRepositories;
    }

    @JIPipeParameter("easy-installer-repositories")
    public void setEasyInstallerRepositories(StringList easyInstallerRepositories) {
        this.easyInstallerRepositories = easyInstallerRepositories;
    }

    @JIPipeDocumentation(name = "Maximum number of threads", description = "The maximum number of threads Ilastik will utilize. Negative or zero values indicate no limitation.")
    @JIPipeParameter("max-threads")
    public int getMaxThreads() {
        return maxThreads;
    }

    @JIPipeParameter("max-threads")
    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    @JIPipeDocumentation(name = "Maximum RAM allocation (MB)", description = "The maximum RAM that Ilastik will utilize. Must be at least 256 (values below that limit will be automatically increased)")
    @JIPipeParameter("max-memory")
    public int getMaxMemory() {
        return maxMemory;
    }

    @JIPipeParameter("max-memory")
    public void setMaxMemory(int maxMemory) {
        this.maxMemory = maxMemory;
    }
}
