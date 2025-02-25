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

package org.hkijena.jipipe.desktop.app.settings.project;

import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.JIPipeProjectDirectories;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.StringUtils;

import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JIPipeDesktopMergedProjectSettingsUserDirectories extends AbstractJIPipeParameterCollection implements JIPipeCustomParameterCollection {
    private final JIPipeProject project;

    public JIPipeDesktopMergedProjectSettingsUserDirectories(JIPipeProject project) {
        this.project = project;
    }

    @Override
    public Map<String, JIPipeParameterAccess> getParameters() {
        Map<String, JIPipeParameterAccess> result = new HashMap<>();
        List<JIPipeProjectDirectories.DirectoryEntry> directoriesAsInstance = project.getMetadata().getDirectories().getDirectoriesAsInstance();
        for (int i = 0; i < directoriesAsInstance.size(); i++) {
            JIPipeProjectDirectories.DirectoryEntry directoryEntry = directoriesAsInstance.get(i);
            int finalI = i;
            result.put("jipipe:project:directory/" + i, JIPipeManualParameterAccess.builder()
                    .setFieldClass(Path.class)
                    .setGetter(directoryEntry::getPath)
                    .setSetter((newValue) -> {
                        // Write it into the original entry
                        JIPipeDynamicParameterCollection target = project.getMetadata().getDirectories().getDirectories().get(finalI);
                        target.get("path").set(newValue);
                    })
                    .setName(StringUtils.orElse(directoryEntry.getName(), StringUtils.orElse(directoryEntry.getKey(), "Unnamed (index " + i + ")")))
                    .setDescription(directoryEntry.getDescription())
                    .setKey("jipipe:project:directory/" + i)
                    .addAnnotation(new PathParameterSettings() {

                        @Override
                        public Class<? extends Annotation> annotationType() {
                            return PathParameterSettings.class;
                        }

                        @Override
                        public PathIOMode ioMode() {
                            return PathIOMode.Open;
                        }

                        @Override
                        public PathType pathMode() {
                            return PathType.FilesAndDirectories;
                        }

                        @Override
                        public String[] extensions() {
                            return new String[0];
                        }

                        @Override
                        public JIPipeFileChooserApplicationSettings.LastDirectoryKey key() {
                            return JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data;
                        }
                    })
                    .setSource(this)
                    .setImportant(directoryEntry.isMustExist())
                    .build());
        }
        return result;
    }
}
