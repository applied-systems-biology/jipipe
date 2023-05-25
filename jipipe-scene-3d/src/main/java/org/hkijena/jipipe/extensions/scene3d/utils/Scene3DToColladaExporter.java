package org.hkijena.jipipe.extensions.scene3d.utils;

import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.extensions.clij2.Scene3DExtension;
import org.hkijena.jipipe.extensions.scene3d.datatypes.Scene3DData;
import org.hkijena.jipipe.extensions.scene3d.model.Scene3DMeshObject;
import org.hkijena.jipipe.extensions.scene3d.model.Scene3DNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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

    private void createGeometryLibrary(Document doc, Element rootElement, Set<String> geometryIds) {
        Element geometryListElement = doc.createElement("library_geometries");
        rootElement.appendChild(geometryListElement);

        // Converting to mesh
        List<Scene3DMeshObject> meshObjectList = new ArrayList<>();
        getProgressInfo().setProgress(0, scene3DNodes.size());
        for (int i = 0; i < scene3DNodes.size(); i++) {
            getProgressInfo().resolveAndLog("Generating meshes", i, scene3DNodes.size());
            Scene3DNode scene3DNode = scene3DNodes.get(i);
            scene3DNode.toMesh(meshObjectList);
        }

        // Generate XML data
        for (Scene3DMeshObject meshObject : meshObjectList) {
            String id = UUID.randomUUID().toString();

            Element geometryElement = doc.createElement("geometry");
            geometryListElement.appendChild(geometryElement);
            geometryElement.setAttribute("id", id);
            geometryElement.setAttribute("name", id);

            Element meshElement = doc.createElement("mesh");
            geometryElement.appendChild(meshElement);

            Element
        }
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
