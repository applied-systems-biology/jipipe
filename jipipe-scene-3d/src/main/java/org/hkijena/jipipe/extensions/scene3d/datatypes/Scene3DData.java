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

package org.hkijena.jipipe.extensions.scene3d.datatypes;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.LabelAsJIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.utils.JIPipeSerializedJsonObjectData;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.extensions.scene3d.model.Scene3DNode;
import org.hkijena.jipipe.extensions.scene3d.utils.Scene3DToColladaExporter;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.file.Path;
import java.util.*;

@SetJIPipeDocumentation(name = "3D scene", description = "3D objects arranged in a scene")
@LabelAsJIPipeHeavyData
public class Scene3DData extends JIPipeSerializedJsonObjectData implements List<Scene3DNode> {

    private final List<Scene3DNode> nodes = new ArrayList<>();

    public Scene3DData() {

    }

    public Scene3DData(Scene3DData other) {
        for (Scene3DNode node : other) {
            add(node.duplicate());
        }
    }

    public static Scene3DData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return JIPipeSerializedJsonObjectData.importData(storage, Scene3DData.class);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        Path outputFile = FileChooserSettings.saveFile(workbench.getWindow(), FileChooserSettings.LastDirectoryKey.Data, "Export Collada (*.dae)", new FileNameExtensionFilter("Collada 1.4 (*.dae)", "dae"));
        if (outputFile != null) {
            JIPipeRunnerQueue queue = new JIPipeRunnerQueue("Collada export");
            Scene3DToColladaExporter exporter = new Scene3DToColladaExporter(this, outputFile);
            JIPipeRunExecuterUI.runInDialog(workbench.getWindow(), exporter, queue);
        }
    }

    @Override
    public String toString() {
        return "3D scene (" + nodes.size() + " nodes)";
    }

    @Override
    public int size() {
        return nodes.size();
    }

    @Override
    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return nodes.contains(o);
    }

    @Override
    public Iterator<Scene3DNode> iterator() {
        return nodes.iterator();
    }

    @Override
    public Object[] toArray() {
        return nodes.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return nodes.toArray(a);
    }

    @Override
    public boolean add(Scene3DNode scene3DNode) {
        return nodes.add(scene3DNode);
    }

    @Override
    public boolean remove(Object o) {
        return nodes.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return nodes.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends Scene3DNode> c) {
        return nodes.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends Scene3DNode> c) {
        return nodes.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return nodes.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return nodes.retainAll(c);
    }

    @Override
    public void clear() {
        nodes.clear();
    }

    @Override
    public Scene3DNode get(int index) {
        return nodes.get(index);
    }

    @Override
    public Scene3DNode set(int index, Scene3DNode element) {
        return nodes.set(index, element);
    }

    @Override
    public void add(int index, Scene3DNode element) {
        nodes.add(index, element);
    }

    @Override
    public Scene3DNode remove(int index) {
        return nodes.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return nodes.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return nodes.lastIndexOf(o);
    }


    @Override
    public ListIterator<Scene3DNode> listIterator() {
        return nodes.listIterator();
    }


    @Override
    public ListIterator<Scene3DNode> listIterator(int index) {
        return nodes.listIterator(index);
    }


    @Override
    public List<Scene3DNode> subList(int fromIndex, int toIndex) {
        return nodes.subList(fromIndex, toIndex);
    }
}
