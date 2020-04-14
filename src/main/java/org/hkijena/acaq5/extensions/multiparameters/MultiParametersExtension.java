package org.hkijena.acaq5.extensions.multiparameters;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.events.AlgorithmRegisteredEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterHolder;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQJsonTraitDeclaration;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.multiparameters.algorithms.MultiParameterAlgorithmDeclaration;
import org.hkijena.acaq5.extensions.multiparameters.datasources.ParametersDataDefinition;
import org.hkijena.acaq5.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.StringUtils;
import org.scijava.plugin.Plugin;

import java.util.Map;

/**
 * Extension that provides capabilities to run multiple parameters
 */
@Plugin(type = ACAQJavaExtension.class)
public class MultiParametersExtension extends ACAQPrepackagedDefaultJavaExtension {

    @Override
    public String getName() {
        return "Multi parameter algorithms";
    }

    @Override
    public String getDescription() {
        return "Extension that provides capabilities to run multiple parameters";
    }

    @Override
    public void register() {
        // Register annotations for each available parameter
        for (ACAQAlgorithmDeclaration declaration : ACAQAlgorithmRegistry.getInstance().getRegisteredAlgorithms().values()) {
            registerParameterTraits(declaration);
        }
        ACAQAlgorithmRegistry.getInstance().getEventBus().register(this);

        // Register data types
        registerDatatype("parameters", ParametersData.class,
                ResourceUtils.getPluginResource("icons/data-types/data-type-parameters.png"),
                null,
                null);

        // Register algorithms
        registerAlgorithm("parameters-define", ParametersDataDefinition.class);
        registerAlgorithm(new MultiParameterAlgorithmDeclaration());
    }

    /**
     * Triggered when an algorithm is registered
     *
     * @param event generated event
     */
    @Subscribe
    public void onAlgorithmRegistered(AlgorithmRegisteredEvent event) {
        registerParameterTraits(event.getAlgorithmDeclaration());
    }

    private void registerParameterTraits(ACAQAlgorithmDeclaration declaration) {
        if (declaration.getCategory() == ACAQAlgorithmCategory.Internal)
            return;
        ACAQAlgorithm instance = declaration.newInstance();
        Map<String, ACAQParameterAccess> parameters = ACAQParameterHolder.getParameters(instance);

        for (ACAQParameterAccess parameterAccess : parameters.values()) {
            if (!StringUtils.isNullOrEmpty(parameterAccess.getName())) {
                String traitId = "parameter-" + StringUtils.jsonify(parameterAccess.getName());
                if (!ACAQTraitRegistry.getInstance().hasTraitWithId(traitId)) {
                    ACAQJsonTraitDeclaration traitDeclaration = new ACAQJsonTraitDeclaration();
                    traitDeclaration.setId(traitId);
                    traitDeclaration.setName(parameterAccess.getName());
                    traitDeclaration.setDescription("Represents a parameter value");
                    traitDeclaration.setDiscriminator(true);
                    traitDeclaration.setHidden(true);
                    traitDeclaration.getTraitIcon().setIconName("wrench-blue.png");
                    registerTrait(traitDeclaration, ResourceUtils.getPluginResource("icons/traits/wrench-blue.png"));
                }
            }
        }
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.acaq5:multi-parameters";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }
}
