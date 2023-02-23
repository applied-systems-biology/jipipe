/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.registries.*;
import org.hkijena.jipipe.ui.registries.JIPipeCustomMenuRegistry;
import org.scijava.service.Service;

import java.util.List;
import java.util.Set;

/**
 * Contains all JIPipe resources
 */
public interface JIPipeService extends Service, JIPipeValidatable {

    EventBus getEventBus();

    JIPipeImageJAdapterRegistry getImageJDataAdapterRegistry();

    List<JIPipeDependency> getRegisteredExtensions();

    JIPipeNodeRegistry getNodeRegistry();

    JIPipeDatatypeRegistry getDatatypeRegistry();

    JIPipeParameterTypeRegistry getParameterTypeRegistry();

    JIPipeCustomMenuRegistry getCustomMenuRegistry();

    JIPipeSettingsRegistry getSettingsRegistry();

    JIPipeExpressionRegistry getExpressionRegistry();

    JIPipeUtilityRegistry getUtilityRegistry();

    JIPipeExternalEnvironmentRegistry getExternalEnvironmentRegistry();

    JIPipeExtensionRegistry getExtensionRegistry();

    JIPipeProjectTemplateRegistry getProjectTemplateRegistry();

    JIPipeGraphEditorToolRegistry getGraphEditorToolRegistry();

    JIPipeExpressionRegistry getTableOperationRegistry();

    Set<String> getRegisteredExtensionIds();

    JIPipeDependency findExtensionById(String dependencyId);
}
