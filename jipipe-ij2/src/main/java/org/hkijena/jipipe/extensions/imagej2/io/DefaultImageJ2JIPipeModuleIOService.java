package org.hkijena.jipipe.extensions.imagej2.io;

import org.apache.commons.lang3.ClassUtils;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.scijava.InstantiableException;
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
                if (instance.handlesInput()) {
                    knownInputHandlers.put(instance.getAcceptedModuleFieldClass(), instance);
                }
                if (instance.handlesOutput()) {
                    knownOutputHandlers.put(instance.getAcceptedModuleFieldClass(), instance);
                }
            } catch (InstantiableException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ImageJ2ModuleIO findModuleIO(ModuleItem<?> moduleItem, JIPipeSlotType ioType) {
        if (knownInputHandlers == null || knownOutputHandlers == null) {
            reload();
        }
        if (ioType == JIPipeSlotType.Input && moduleItem.isInput()) {
            return findModuleIO(moduleItem, knownInputHandlers);
        }
        if (ioType == JIPipeSlotType.Output && moduleItem.isOutput()) {
            return findModuleIO(moduleItem, knownOutputHandlers);
        }
        return null;
    }

    private ImageJ2ModuleIO findModuleIO(ModuleItem<?> moduleItem, Map<Class<?>, ImageJ2ModuleIO> knownIOHandlers) {
        Class<?> moduleType = moduleItem.getType();
        // Greatly simplifies the primitive handling
        moduleType = ClassUtils.primitiveToWrapper(moduleType);
        ImageJ2ModuleIO result = knownIOHandlers.getOrDefault(moduleType, null);
        if (result == null) {
            Queue<Class<?>> queue = new ArrayDeque<>();
            if (moduleType.getSuperclass() != null)
                queue.add(moduleType.getSuperclass());
            queue.addAll(Arrays.asList(moduleType.getInterfaces()));
            while (!queue.isEmpty()) {
                Class<?> item = queue.remove();
                result = knownIOHandlers.getOrDefault(item, null);
                if (result != null) {
                    knownIOHandlers.put(item, result);
                    return result;
                }
                if (item == Object.class)
                    continue;
                if (item.getSuperclass() != null)
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
