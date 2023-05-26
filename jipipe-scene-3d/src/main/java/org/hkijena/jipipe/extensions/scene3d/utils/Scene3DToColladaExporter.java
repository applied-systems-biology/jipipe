package org.hkijena.jipipe.extensions.scene3d.utils;

import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.clij2.Scene3DExtension;
import org.hkijena.jipipe.extensions.scene3d.datatypes.Scene3DData;
import org.hkijena.jipipe.extensions.scene3d.model.geometries.Scene3DMeshGeometry;
import org.hkijena.jipipe.extensions.scene3d.model.geometries.Scene3DUnindexedMeshGeometry;
import org.hkijena.jipipe.extensions.scene3d.model.Scene3DNode;
import org.hkijena.jipipe.utils.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Scene3DToColladaExporter extends AbstractJIPipeRunnable {

    private final Scene3DData scene3DNodes;

    private final Path outputFile;

    public Scene3DToColladaExporter(Scene3DData scene3DNodes, Path outputFile) {
        this.scene3DNodes = scene3DNodes;
        this.outputFile = outputFile;
    }

    @Override
    public String getTaskLabel() {
        return "Export Collada";
    }

    @Override
    public void run() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("COLLADA");
            rootElement.setAttribute("xmlns", "http://www.collada.org/2005/11/COLLADASchema");
            rootElement.setAttribute("version", "1.4.1");
            doc.appendChild(rootElement);

            Set<String> geometryIds = new HashSet<>();

            // Create content
            createAssetMetadata(doc, rootElement);
            createGeometryLibrary(doc, rootElement, geometryIds);
            createSceneLibrary(doc, rootElement, geometryIds);

            // Write XML file
            try(FileOutputStream outputStream = new FileOutputStream(outputFile.toString())) {
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(outputStream);

                transformer.transform(source, result);
            }

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void createSceneLibrary(Document doc, Element rootElement, Set<String> geometryIds) {
        Element visualScenesListElement = doc.createElement("library_visual_scenes");
        rootElement.appendChild(visualScenesListElement);

        Element sceneElement = doc.createElement("visual_scene");
        visualScenesListElement.appendChild(sceneElement);
        sceneElement.setAttribute("id", "Scene");
        sceneElement.setAttribute("name", "Scene");

        for (String geometryId : geometryIds) {
            Element nodeElement = doc.createElement("node");
            sceneElement.appendChild(nodeElement);

            nodeElement.setAttribute("id", "node-" + geometryId);
            nodeElement.setAttribute("name", "node-" + geometryId);
            nodeElement.setAttribute("type", "NODE");

            Element matrixElement = doc.createElement("matrix");
            nodeElement.appendChild(matrixElement);
            matrixElement.setAttribute("sid", "transform");
            matrixElement.setTextContent("1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 1");

            Element instanceGeometryElement = doc.createElement("instance_geometry");
            nodeElement.appendChild(instanceGeometryElement);
            instanceGeometryElement.setAttribute("url", "#" + geometryId);
            instanceGeometryElement.setAttribute("name", geometryId);
        }

        Element rootSceneElement = doc.createElement("scene");
        rootElement.appendChild(rootSceneElement);
        Element instanceVisualSceneElement = doc.createElement("instance_visual_scene");
        rootSceneElement.appendChild(instanceVisualSceneElement);
        instanceVisualSceneElement.setAttribute("url", "#Scene");
    }

    private void createGeometryLibrary(Document doc, Element rootElement, Set<String> geometryIds) {
        Element geometryListElement = doc.createElement("library_geometries");
        rootElement.appendChild(geometryListElement);

        // Converting to mesh
        List<Scene3DMeshGeometry> meshObjectList = new ArrayList<>();
        getProgressInfo().setProgress(0, scene3DNodes.size());
        for (int i = 0; i < scene3DNodes.size(); i++) {
            JIPipeProgressInfo nodeProgress = getProgressInfo().resolveAndLog("Generating meshes", i, scene3DNodes.size());
            Scene3DNode scene3DNode = scene3DNodes.get(i);
            scene3DNode.toMesh(meshObjectList, nodeProgress);
        }

        // Generate XML data
        getProgressInfo().setProgress(0, meshObjectList.size());
        for (int i = 0; i < meshObjectList.size(); i++) {
            JIPipeProgressInfo meshProgress = getProgressInfo().resolveAndLog("Indexing meshes", i, scene3DNodes.size());

            Scene3DMeshGeometry meshObject = meshObjectList.get(i);
            if(meshObject instanceof Scene3DUnindexedMeshGeometry) {
                meshObject = ((Scene3DUnindexedMeshGeometry) meshObject).toIndexedMeshGeometry(meshProgress);
            }

            String id = StringUtils.makeUniqueString(StringUtils.orElse(meshObject.getName(), "unnamed"), "-", geometryIds);
            geometryIds.add(id);

            Element geometryElement = doc.createElement("geometry");
            geometryListElement.appendChild(geometryElement);
            geometryElement.setAttribute("id", id);
            geometryElement.setAttribute("name", id);

            createMesh(doc, meshObject, geometryElement, id);
        }
    }

    private static void createMesh(Document doc, Scene3DMeshGeometry meshObject, Element geometryElement, String id) {
        Element meshElement = doc.createElement("mesh");
        geometryElement.appendChild(meshElement);

        meshElement.appendChild(createSource(doc, meshObject.getVertices(), id + "-verts-array", 3));
        meshElement.appendChild(createSource(doc, meshObject.getNormals(), id + "-normals-array", 3));

        Element verticesElement = doc.createElement("vertices");
        meshElement.appendChild(verticesElement);
        verticesElement.setAttribute("id", id + "-array-verts");
        verticesElement.appendChild(createInput(doc, "#" + id + "-verts-array", "POSITION", 0));

        Element trianglesElement = doc.createElement("triangles");
        meshElement.appendChild(trianglesElement);
        trianglesElement.setAttribute("count", String.valueOf(meshObject.getNumVertices()));
        trianglesElement.appendChild(createInput(doc, "#" + id + "-array-verts", "VERTEX", 0));
        trianglesElement.appendChild(createInput(doc, "#" + id + "-normals-array", "NORMAL", 1));
        trianglesElement.appendChild(createIndex(doc, meshObject));
    }

    private static Element createIndex(Document doc, Scene3DMeshGeometry meshObject) {
        Element element = doc.createElement("p");

        int[] verticesIndex = meshObject.getVerticesIndex();
        int[] normalsIndex = meshObject.getNormalsIndex();

        if(verticesIndex.length != normalsIndex.length) {
            throw new IllegalArgumentException("Index arrays have different lengths!");
        }

        int[] index = new int[verticesIndex.length + normalsIndex.length];

        // Pairs of indices <vertex index> <normal index> ...
        for (int i = 0; i < verticesIndex.length; i++) {
            index[2 * i] = verticesIndex[i];
            index[2 * i + 1] = normalsIndex[i];
        }

        element.setTextContent(Ints.join(" ", index));

        return element;
    }

    private static Element createInput(Document doc, String source, String semantic, int offset) {
        Element element = doc.createElement("input");
        element.setAttribute("source", source);
        element.setAttribute("semantic", semantic);
        element.setAttribute("offset", String.valueOf(offset));
        return element;
    }

    private static Element createSource(Document doc, float[] values, String id, int stride) {
        Element sourceElement = doc.createElement("source");
        sourceElement.setAttribute("id", id);

        Element floatArrayElement = doc.createElement("float_array");
        sourceElement.appendChild(floatArrayElement);
        floatArrayElement.setAttribute("id", id + "-array");
        floatArrayElement.setAttribute("count", String.valueOf(values.length));
        floatArrayElement.setTextContent(Floats.join(" ", values));

        Element techniqueCommonElement = doc.createElement("technique_common");
        sourceElement.appendChild(techniqueCommonElement);
        Element accessorElement = doc.createElement("accessor");
        techniqueCommonElement.appendChild(accessorElement);
        accessorElement.setAttribute("count", String.valueOf(values.length / stride));
        accessorElement.setAttribute("source", "#" + id + "-array");
        accessorElement.setAttribute("stride", String.valueOf(stride));

        accessorElement.appendChild(createParam(doc, "float", "X"));
        accessorElement.appendChild(createParam(doc, "float", "Y"));
        accessorElement.appendChild(createParam(doc, "float", "Z"));

        return sourceElement;
    }

    private static Element createParam(Document doc, String type, String name) {
        Element element = doc.createElement("param");
        element.setAttribute("type", type);
        element.setAttribute("name", name);
        return element;
    }

    private static Element createTextElement(Document document, String elementName, String elementContent) {
        Element element = document.createElement(elementName);
        element.setTextContent(elementContent);
        return element;
    }

    private void createAssetMetadata(Document doc, Element rootElement) {
        Element assetElement = doc.createElement("asset");
        rootElement.appendChild(assetElement);

        Element contributorElement = doc.createElement("contributor");
        assetElement.appendChild(contributorElement);

        contributorElement.appendChild(createTextElement(doc, "author", "JIPipe user"));
        contributorElement.appendChild(createTextElement(doc, "authoring_tool", "JIPipe DAE exporter version " + Scene3DExtension.AS_DEPENDENCY.getDependencyVersion()));

        assetElement.appendChild(createTextElement(doc, "created", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        assetElement.appendChild(createTextElement(doc, "modified", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        assetElement.appendChild(createTextElement(doc, "up_axis", "Z_UP"));
    }
}
