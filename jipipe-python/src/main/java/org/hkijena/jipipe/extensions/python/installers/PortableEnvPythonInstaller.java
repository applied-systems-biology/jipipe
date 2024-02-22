package org.hkijena.jipipe.extensions.python.installers;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

@SetJIPipeDocumentation(name = "Install Python 3", description = "Installs Python 3")
public class PortableEnvPythonInstaller extends BasicPortableEnvPythonInstaller {
    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public PortableEnvPythonInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        this.setConfiguration(new Configuration());
    }

    @Override
    protected void postprocessInstall() {
        super.postprocessInstall();

        Configuration configuration = (Configuration) getConfiguration();
        if (configuration.isInstallNumpy()) {
            runPip("install", "numpy");
        }
        if (configuration.isInstallScikitImage()) {
            runPip("install", "scikit-image");
        }
        if (configuration.isInstallScikitLearn()) {
            runPip("install", "scikit-learn");
        }
        if (configuration.isInstallTiffFile()) {
            runPip("install", "tifffile");
        }
        if (configuration.isInstallPandas()) {
            runPip("install", "pandas");
        }
    }

    public static class Configuration extends BasicPortableEnvPythonInstaller.Configuration {
        private boolean installNumpy = true;
        private boolean installScikitImage = true;
        private boolean installScikitLearn = true;
        private boolean installTiffFile = true;
        private boolean installPandas = true;

        @SetJIPipeDocumentation(name = "Install Numpy", description = "Install the numpy library")
        @JIPipeParameter("install-numpy")
        public boolean isInstallNumpy() {
            return installNumpy;
        }

        @JIPipeParameter("install-numpy")
        public void setInstallNumpy(boolean installNumpy) {
            this.installNumpy = installNumpy;
        }

        @SetJIPipeDocumentation(name = "Install Scikit Image", description = "Install the scikit-image library")
        @JIPipeParameter("install-scikit-image")
        public boolean isInstallScikitImage() {
            return installScikitImage;
        }

        @JIPipeParameter("install-scikit-image")
        public void setInstallScikitImage(boolean installScikitImage) {
            this.installScikitImage = installScikitImage;
        }

        @SetJIPipeDocumentation(name = "Install Scikit Learn", description = "Install the scikit-learn library")
        @JIPipeParameter("install-scikit-learn")
        public boolean isInstallScikitLearn() {
            return installScikitLearn;
        }

        @JIPipeParameter("install-scikit-learn")
        public void setInstallScikitLearn(boolean installScikitLearn) {
            this.installScikitLearn = installScikitLearn;
        }

        @SetJIPipeDocumentation(name = "Install TIFFFile", description = "Install the tifffile library")
        @JIPipeParameter("install-tifffile")
        public boolean isInstallTiffFile() {
            return installTiffFile;
        }

        @JIPipeParameter("install-tifffile")
        public void setInstallTiffFile(boolean installTiffFile) {
            this.installTiffFile = installTiffFile;
        }

        @SetJIPipeDocumentation(name = "Install Pandas", description = "Installs the pandas library")
        @JIPipeParameter("install-pandas")
        public boolean isInstallPandas() {
            return installPandas;
        }

        @JIPipeParameter("install-pandas")
        public void setInstallPandas(boolean installPandas) {
            this.installPandas = installPandas;
        }
    }
}
