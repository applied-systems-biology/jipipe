package org.hkijena.jipipe.extensions.imagej2.io;

import org.jetbrains.annotations.Nullable;
import org.scijava.InstantiableException;
import org.scijava.log.LogService;
import org.scijava.module.ModuleItem;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.service.AbstractService;

import java.util.*;

@Plugin(type = ImageJ2JIPipeModuleIOService.class)
public class DefaultImageJ2JIPipeModuleIOService extends AbstractService implements ImageJ2JIPipeModuleIOService {
    private Map<Class<?>, ImageJ2ModuleIO> knownInputHandlers;
    private Map<Class<?>, ImageJ2ModuleIO> knownOutputHandlers;
    public DefaultImageJ2JIPipeModuleIOService() {
    }

    public void reload() {
        knownInputHandlers = new HashMap<>();
        knownOutputHandlers = new HashMap<>();
        for (PluginInfo<ImageJ2ModuleIO> plugin : getPlugins()) {
            try {
                ImageJ2ModuleIO instance = plugin.createInstance();
                if(instance.handlesInput()) {
                    knownInputHandlers.put(instance.getAcceptedModuleFieldClass(), instance);
                }
                if(instance.handlesOutput()) {
                    knownOutputHandlers.put(instance.getAcceptedModuleFieldClass(), instance);
                }
            } catch (InstantiableException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ImageJ2ModuleIO findModuleIO(ModuleItem<?> moduleItem) {
        if(knownInputHandlers == null || knownOutputHandlers == null) {
            reload();
        }
        if(moduleItem.isInput()) {
            return findModuleIO(moduleItem, knownInputHandlers);
        }
        else {
            return findModuleIO(moduleItem, knownOutputHandlers);
        }
    }

    private ImageJ2ModuleIO findModuleIO(ModuleItem<?> moduleItem, Map<Class<?>, ImageJ2ModuleIO> knownIOHandlers) {
        ImageJ2ModuleIO result = knownIOHandlers.getOrDefault(moduleItem.getType(), null);
        if(result == null) {
            Queue<Class<?>> queue = new ArrayDeque<>();
            if(moduleItem.getType().getSuperclass() != null)
                queue.add(moduleItem.getType().getSuperclass());
            queue.addAll(Arrays.asList(moduleItem.getType().getInterfaces()));
            while(!queue.isEmpty()) {
                Class<?> item = queue.remove();
                if(item == Object.class)
                    continue;
                result = knownIOHandlers.getOrDefault(item, null);
                if(result != null) {
                    knownIOHandlers.put(item, result);
                    return result;
                }
                if(item.getSuperclass() != null)
                    queue.add(item.getSuperclass());
                queue.addAll(Arrays.asList(item.getInterfaces()));
            }
        }
        return result;
    }

    @Override
    public Class<ImageJ2ModuleIO> getPluginType() {
        return ImageJ2ModuleIO.class;
    }
}
