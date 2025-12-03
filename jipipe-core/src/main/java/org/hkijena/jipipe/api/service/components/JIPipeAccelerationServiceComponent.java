package org.hkijena.jipipe.api.service.components;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.acceleration.JIPipeHardwareAccelerationMode;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.Vector2iParameter;
import org.hkijena.jipipe.plugins.settings.application.JIPipeHardwareAccelerationApplicationSettings;
import org.hkijena.jipipe.utils.CUDAUtils;

/**
 * Service component responsible for handling acceleration-related tasks
 */
public final class JIPipeAccelerationServiceComponent extends JIPipeServiceComponent {
    public JIPipeAccelerationServiceComponent(JIPipeService service) {
        super(service);
    }

    public void autoDetect(JIPipeProgressInfo progressInfo) {
        JIPipeHardwareAccelerationApplicationSettings hardwareAccelerationApplicationSettings = JIPipeHardwareAccelerationApplicationSettings.getInstance();
        progressInfo.log("Determining acceleration profile ...");
        try {

            if (CUDAUtils.hasCudaSupport()) {
                progressInfo.log("Determining acceleration profile ... CUDA support detected");
                hardwareAccelerationApplicationSettings.setAccelerationPreference(JIPipeHardwareAccelerationMode.CUDA);

                try {
                    hardwareAccelerationApplicationSettings.setAccelerationPreferenceVersions(new Vector2iParameter(
                            CUDAUtils.getMinimumCudaVersion(),
                            CUDAUtils.getMaximumCudaVersion()
                    ));
                    progressInfo.log("Determined CUDA version limits as " + hardwareAccelerationApplicationSettings.getAccelerationPreferenceVersions());
                } catch (Exception e) {
                    progressInfo.log(e);
                }
            }

            hardwareAccelerationApplicationSettings.setAutoConfigureAccelerationOnNextStartup(false);
            getService().getApplicationSettings().save();
        } catch (Exception e) {
            progressInfo.log(e);
        }
    }
}
