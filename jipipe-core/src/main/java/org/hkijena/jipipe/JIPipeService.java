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

package org.hkijena.jipipe;

import org.hkijena.jipipe.api.initialization.events.*;
import org.hkijena.jipipe.api.registries.*;
import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.desktop.api.registries.JIPipeCustomMenuRegistry;
import org.scijava.service.Service;

import java.util.List;
import java.util.Set;

/**
 * Contains all JIPipe resources
 */
public interface JIPipeService extends Service, JIPipeValidatable {

    JIPipeRecentProjectsRegistry getRecentProjectsRegistry();

    JIPipeImageJAdapterRegistry getImageJDataAdapterRegistry();

    List<JIPipeDependency> getRegisteredExtensions();

    JIPipeNodeRegistry getNodeRegistry();

    JIPipeNodeTemplateRegistry getNodeTemplateRegistry();

    JIPipeDatatypeRegistry getDatatypeRegistry();

    JIPipeParameterTypeRegistry getParameterTypeRegistry();

    JIPipeCustomMenuRegistry getCustomMenuRegistry();

    JIPipeApplicationSettingsRegistry getApplicationSettingsRegistry();

    JIPipeProjectSettingsRegistry getProjectSettingsRegistry();

    JIPipeMetadataRegistry getMetadataRegistry();

    JIPipeExpressionRegistry getExpressionRegistry();

    JIPipeUtilityRegistry getUtilityRegistry();

    JIPipeExternalEnvironmentRegistry getExternalEnvironmentRegistry();

    JIPipePluginRegistry getPluginRegistry();

    JIPipeProjectTemplateRegistry getProjectTemplateRegistry();

    JIPipeGraphEditorToolRegistry getGraphEditorToolRegistry();

    JIPipeExpressionRegistry getTableOperationRegistry();

    JIPipeArtifactsRegistry getArtifactsRegistry();

    Set<String> getRegisteredExtensionIds();

    JIPipeDependency findExtensionById(String dependencyId);

    JIPipeDatatypeRegisteredEventEmitter getDatatypeRegisteredEventEmitter();

    JIPipePluginDiscoveredEventEmitter getExtensionDiscoveredEventEmitter();

    JIPipePluginRegisteredEventEmitter getExtensionRegisteredEventEmitter();

    JIPipeNodeInfoRegisteredEventEmitter getNodeInfoRegisteredEventEmitter();

}
