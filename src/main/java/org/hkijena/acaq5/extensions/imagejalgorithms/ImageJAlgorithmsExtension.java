package org.hkijena.acaq5.extensions.imagejalgorithms;

import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleException;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Extension that adds ImageJ2 algorithms
 */
@Plugin(type = ACAQJavaExtension.class)
public class ImageJAlgorithmsExtension extends ACAQPrepackagedDefaultJavaExtension {

    @Parameter
    private CommandService commandService;

    @Override
    public String getName() {
        return "ImageJ2 algorithms";
    }

    @Override
    public String getDescription() {
        return "Integrates ImageJ2 algorithms into ACAQ5";
    }

    @Override
    public void register() {
        for (CommandInfo command : commandService.getCommands()) {
            if (ImageJ2AlgorithmWrapper.isCompatible(command, getContext())) {
                try {
                    ImageJ2AlgorithmWrapperDeclaration declaration = new ImageJ2AlgorithmWrapperDeclaration(command, getContext());
                    registerAlgorithm(new ImageJ2AlgorithmWrapperRegistrationTask(this, declaration));
                } catch (ModuleException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.acaq5:imagej-algorithms";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }
}
