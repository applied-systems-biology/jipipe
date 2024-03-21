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

package org.hkijena.jipipe.extensions.python.installers;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

@SetJIPipeDocumentation(name = "Install Miniconda 3", description = "Installs Miniconda 3")
public class MinicondaEnvPythonInstaller extends BasicMinicondaEnvPythonInstaller {
    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public MinicondaEnvPythonInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        setConfiguration(new Configuration());
    }

    @Override
    protected void postprocessInstall() {
        super.postprocessInstall();

        Configuration configuration = (Configuration) getConfiguration();
        if (configuration.isInstallNumpy()) {
            runConda("install", "--yes", "numpy");
        }
        if (configuration.isInstallScikitImage()) {
            runConda("install", "--yes", "scikit-image");
        }
        if (configuration.isInstallScikitLearn()) {
            runConda("install", "--yes", "-c", "conda-forge", "scikit-learn");
        }
        if (configuration.isInstallTiffFile()) {
            runConda("install", "--yes", "-c", "conda-forge", "tifffile");
        }
        if (configuration.isInstallPandas()) {
            runConda("install", "--yes", "-c", "anaconda", "pandas");
        }
    }

    public static class Configuration extends BasicMinicondaEnvPythonInstaller.Configuration {
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
