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

package org.hkijena.jipipe.ui.grapheditor;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * JSON-serializable class that stores node hotkeys
 */
public class NodeHotKeyStorage {

    private Map<String, Map<Hotkey, String>> hotkeys = new HashMap<>();

    public NodeHotKeyStorage() {
    }

    public static NodeHotKeyStorage getInstance(JIPipeGraph graph) {
        Object instance = graph.getAdditionalMetadata().getOrDefault("node-hotkeys", null);
        if (instance instanceof NodeHotKeyStorage) {
            return (NodeHotKeyStorage) instance;
        }
        instance = new NodeHotKeyStorage();
        graph.getAdditionalMetadata().put("node-hotkeys", instance);
        return (NodeHotKeyStorage) instance;
    }

    /**
     * Renames a node to another Id
     *
     * @param renamingMap map from old to new value
     */
    public void renameNodeIds(Map<String, String> renamingMap) {
        for (Map.Entry<String, Map<Hotkey, String>> entry : hotkeys.entrySet()) {
            for (Map.Entry<Hotkey, String> hotkeyEntry : ImmutableList.copyOf(entry.getValue().entrySet())) {
                String newName = renamingMap.getOrDefault(hotkeyEntry.getValue(), hotkeyEntry.getValue());
                entry.getValue().put(hotkeyEntry.getKey(), newName);
            }
        }
    }

    public void setHotkey(UUID compartment, UUID nodeId, Hotkey hotkey) {
        Map<Hotkey, String> compartmentMap = hotkeys.getOrDefault(StringUtils.nullToEmpty(compartment), null);
        if (compartmentMap == null) {
            compartmentMap = new HashMap<>();
            hotkeys.put(StringUtils.nullToEmpty(compartment), compartmentMap);
        }
        for (Map.Entry<Hotkey, String> entry : ImmutableList.copyOf(compartmentMap.entrySet())) {
            if (nodeId.toString().equals(entry.getValue())) {
                compartmentMap.remove(entry.getKey());
            }
        }
        if (hotkey != Hotkey.None) {
            compartmentMap.put(hotkey, nodeId.toString());
        }
    }

    /**
     * Renames the internal compartment mappings
     *
     * @param renamingMap map from old to new value
     */
    public void renameCompartments(Map<String, String> renamingMap) {
        Map<String, Map<Hotkey, String>> copy = new HashMap<>();
        for (Map.Entry<String, Map<Hotkey, String>> entry : hotkeys.entrySet()) {
            copy.put(renamingMap.getOrDefault(entry.getKey(), entry.getKey()), entry.getValue());
        }
        this.hotkeys = copy;
    }

    public String getNodeForHotkey(Hotkey hotkey, UUID compartment) {
        Map<Hotkey, String> hotkeyMap = hotkeys.getOrDefault(StringUtils.nullToEmpty(compartment), null);
        if (hotkeyMap == null)
            return null;
        return hotkeyMap.getOrDefault(hotkey, null);
    }

    @JsonGetter("hotkeys")
    public Map<String, Map<Hotkey, String>> getHotkeys() {
        return hotkeys;
    }

    @JsonSetter("hotkeys")
    public void setHotkeys(Map<String, Map<Hotkey, String>> hotkeys) {
        this.hotkeys = hotkeys;
    }

    public Hotkey getHotkeyFor(UUID compartment, UUID id) {
        Map<Hotkey, String> hotkeyMap = hotkeys.getOrDefault(StringUtils.nullToEmpty(compartment), null);
        if (hotkeyMap == null)
            return Hotkey.None;
        for (Map.Entry<Hotkey, String> entry : hotkeyMap.entrySet()) {
            if (Objects.equals(entry.getValue(), id.toString()))
                return entry.getKey();
        }
        return Hotkey.None;
    }

    public enum Hotkey {
        None,
        Slot1,
        Slot2,
        Slot3,
        Slot4,
        Slot5,
        Slot6,
        Slot7,
        Slot8,
        Slot9,
        Slot0;

        public static Hotkey fromIndex(int index) {
            switch (index) {
                case 0:
                    return Slot0;
                case 1:
                    return Slot1;
                case 2:
                    return Slot2;
                case 3:
                    return Slot3;
                case 4:
                    return Slot4;
                case 5:
                    return Slot5;
                case 6:
                    return Slot6;
                case 7:
                    return Slot7;
                case 8:
                    return Slot8;
                case 9:
                    return Slot9;
                default:
                    return None;
            }
        }

        public static Hotkey fromKeyCode(int key) {
            switch (key) {
                case KeyEvent.VK_0:
                case KeyEvent.VK_NUMPAD0:
                    return Slot0;
                case KeyEvent.VK_1:
                case KeyEvent.VK_NUMPAD1:
                    return Slot1;
                case KeyEvent.VK_2:
                case KeyEvent.VK_NUMPAD2:
                    return Slot2;
                case KeyEvent.VK_3:
                case KeyEvent.VK_NUMPAD3:
                    return Slot3;
                case KeyEvent.VK_4:
                case KeyEvent.VK_NUMPAD4:
                    return Slot4;
                case KeyEvent.VK_5:
                case KeyEvent.VK_NUMPAD5:
                    return Slot5;
                case KeyEvent.VK_6:
                case KeyEvent.VK_NUMPAD6:
                    return Slot6;
                case KeyEvent.VK_7:
                case KeyEvent.VK_NUMPAD7:
                    return Slot7;
                case KeyEvent.VK_8:
                case KeyEvent.VK_NUMPAD8:
                    return Slot8;
                case KeyEvent.VK_9:
                case KeyEvent.VK_NUMPAD9:
                    return Slot9;
            }
            return None;
        }
    }

}
