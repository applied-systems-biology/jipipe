package org.hkijena.jipipe.utils.xml;

import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class XmlUtils {
    public static Document readFromString(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xml));
            return builder.parse(is);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String prettyPrint(String xmlString, int indent, boolean ignoreDeclaration) {
        try {
            InputSource src = new InputSource(new StringReader(xmlString));
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src);

            TransformerFactory transformerFactory = new TransformerFactoryImpl();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, ignoreDeclaration ? "yes" : "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            Writer out = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(out));
            return out.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error occurs when pretty-printing xml:\n" + xmlString, e);
        }
    }

    public static String nodeToString(Node node, boolean omitXmlDeclaration, boolean prettyPrint) {
        if (node == null) {
            throw new IllegalArgumentException("node is null.");
        }

        try {
            // Remove unwanted whitespaces
            node.normalize();
            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile("//text()[normalize-space()='']");
            NodeList nodeList = (NodeList) expr.evaluate(node, XPathConstants.NODESET);

            for (int i = 0; i < nodeList.getLength(); ++i) {
                Node nd = nodeList.item(i);
                nd.getParentNode().removeChild(nd);
            }

            // Create and setup transformer
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            if (omitXmlDeclaration) {
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            }

            if (prettyPrint) {
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            }

            // Turn the node into a string
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            return writer.toString();
        } catch (TransformerException | XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> extractStringListFromXPath(Document document, String xPath, Map<String, String> namespaceMap) {
        XPath xPathInstance = XPathFactory.newInstance().newXPath();
        xPathInstance.setNamespaceContext(new NamespaceContext() {
            @Override
            public Iterator<?> getPrefixes(String arg0) {
                return null;
            }

            @Override
            public String getPrefix(String arg0) {
                return null;
            }

            @Override
            public String getNamespaceURI(String arg0) {
                if (arg0 != null) {
                    return namespaceMap.getOrDefault(arg0, null);
                }
                return null;
            }
        });
        try {
            Object evaluationResult = xPathInstance.compile(xPath).evaluate(document, XPathConstants.NODESET);
            List<String> result = new ArrayList<>();
            if (evaluationResult instanceof NodeList) {
                NodeList nodeList = (NodeList) evaluationResult;
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node item = nodeList.item(i);
                    result.add(nodeToString(item, true, false));
                }
            }
            return result;
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    public static String extractStringFromXPath(Document document, String xPath, Map<String, String> namespaceMap) {
        XPath xPathInstance = XPathFactory.newInstance().newXPath();
        xPathInstance.setNamespaceContext(new NamespaceContext() {
            @Override
            public Iterator<?> getPrefixes(String arg0) {
                return null;
            }

            @Override
            public String getPrefix(String arg0) {
                return null;
            }

            @Override
            public String getNamespaceURI(String arg0) {
                if (arg0 != null) {
                    return namespaceMap.getOrDefault(arg0, null);
                }
                return null;
            }
        });
        try {
            Object evaluationResult = xPathInstance.compile(xPath).evaluate(document, XPathConstants.NODESET);
            StringBuilder result = new StringBuilder();
            if (evaluationResult instanceof NodeList) {
                NodeList nodeList = (NodeList) evaluationResult;
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node item = nodeList.item(i);
                    result.append(nodeToString(item, true, false));
                }
            }
            return result.toString();
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }
}
