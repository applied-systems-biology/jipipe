package org.hkijena.jipipe.extensions.scene3d.datatypes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeSerializedJsonObjectData;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.extensions.scene3d.model.Scene3DNode;
import org.hkijena.jipipe.extensions.strings.JsonData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.util.*;

@JIPipeDocumentation(name = "3D scene", description = "3D objects arranged in a scene")
@JIPipeHeavyData
public class Scene3DData extends JIPipeSerializedJsonObjectData implements List<Scene3DNode> {

    private final List<Scene3DNode> nodes = new ArrayList<>();
    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }

    public static Scene3DData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return JIPipeSerializedJsonObjectData.importData(storage, Scene3DData.class);
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
    public <T> T[] toArray( T[] a) {
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
    public boolean containsAll( Collection<?> c) {
        return nodes.containsAll(c);
    }

    @Override
    public boolean addAll( Collection<? extends Scene3DNode> c) {
        return nodes.addAll(c);
    }

    @Override
    public boolean addAll(int index,  Collection<? extends Scene3DNode> c) {
        return nodes.addAll(index, c);
    }

    @Override
    public boolean removeAll( Collection<?> c) {
        return nodes.removeAll(c);
    }

    @Override
    public boolean retainAll( Collection<?> c) {
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