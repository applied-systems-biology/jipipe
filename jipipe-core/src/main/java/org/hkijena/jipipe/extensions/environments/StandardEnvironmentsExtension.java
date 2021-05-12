package org.hkijena.jipipe.extensions.environments;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.environments.installers.*;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.scijava.plugin.Plugin;

@Plugin(type = JIPipeJavaExtension.class)
public class StandardEnvironmentsExtension extends JIPipePrepackagedDefaultJavaExtension {
    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Environment installers";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides utilities to set up Python and other environments");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:environment-installers";
    }

    @Override
    public String getDependencyVersion() {
        return "2021.5";
    }

    @Override
    public void register() {
        registerParameterEditor(ExternalEnvironment.class, ExternalEnvironmentParameterEditorUI.class);
        registerParameterType("python-environment",
                PythonEnvironment.class,
                null,
                null,
                null,
                "Python environment",
                "Describes a Python environment",
                ExternalEnvironmentParameterEditorUI.class);
        registerParameterType("optional-python-environment",
                OptionalPythonEnvironment.class,
                null,
                null,
                "Optional Python environment",
                "An optional Python environment",
                null);
        registerEnumParameterType("python-environment-type",
                PythonEnvironmentType.class,
                "Python environment type",
                "A Python environment type");
        registerParameterType("r-environment",
                REnvironment.class,
                null,
                null,
                null,
                "R environment",
                "Describes an R environment",
                ExternalEnvironmentParameterEditorUI.class);
        registerParameterType("optional-r-environment",
                OptionalREnvironment.class,
                null,
                null,
                "Optional R environment",
                "An optional R environment",
                null);

        // Register installers
        registerPythonEnvironmentInstaller(MinicondaEnvPythonInstaller.class);
        registerPythonEnvironmentInstaller(SelectCondaEnvPythonInstaller.class);
        registerPythonEnvironmentInstaller(SelectSystemPythonInstaller.class);
        registerPythonEnvironmentInstaller(SelectVirtualEnvPythonInstaller.class);
        registerREnvironmentInstaller(REnvInstaller.class);
    }
}
