package org.hkijena.jipipe.plugins.cellpose.utils;

import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.plugins.cellpose.environments.cp4.Cellpose4Environment;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Set;

public class CellposeVersionUtils {

    private static final Set<String> MODELS_REQUIRING_42 = Set.of("cpsam_v2", "cpdino", "cpdino-vitb");

    public static String getInstalledVersion(Cellpose4Environment env) {
        if (env == null || !env.isLoadFromArtifact()) {
            return null;
        }
        String query = env.getArtifactQuery().getQuery();
        if (StringUtils.isNullOrEmpty(query)) {
            return null;
        }
        JIPipeArtifact artifact = env.getArtifactQuery().toArtifact();
        return artifact.getVersion();
    }

    public static boolean requiresCellpose42(String modelId) {
        return modelId != null && MODELS_REQUIRING_42.contains(modelId);
    }

    public static boolean isModelSupported(String modelId, String version) {
        if (version == null) {
            return true;
        }
        if (!requiresCellpose42(modelId)) {
            return true;
        }
        return StringUtils.compareVersions(version, "4.2") >= 0;
    }
}
