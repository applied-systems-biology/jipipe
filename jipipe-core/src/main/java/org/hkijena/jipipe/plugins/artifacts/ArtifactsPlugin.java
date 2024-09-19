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

package org.hkijena.jipipe.plugins.artifacts;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryReference;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryReferenceList;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryType;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.util.Collections;
import java.util.Set;

/**
 * The core extension
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class ArtifactsPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:artifacts", JIPipe.getJIPipeVersion(), "Artifacts");

    @Override
    public String getName() {
        return "Artifacts";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides functionality associated to artifact download and management");
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerEnumParameterType("artifact-repository-type",
                JIPipeArtifactRepositoryType.class,
                "Artifact repository type",
                "An artifact repository type");
        registerParameterType("artifact-repository",
                JIPipeArtifactRepositoryReference.class,
                JIPipeArtifactRepositoryReferenceList.class,
                null, null,
                "Artifact repository",
                "An artifact repository",
                null);
        registerEnumParameterType("artifact-acceleration-preference",
                JIPipeArtifactAccelerationPreference.class,
                "Artifact acceleration preference",
                "Select artifacts that match the specified acceleration profile");
        registerApplicationSettingsSheet(new JIPipeArtifactApplicationSettings());
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:artifacts";
    }

    @Override
    public boolean isCorePlugin() {
        return true;
    }

}
