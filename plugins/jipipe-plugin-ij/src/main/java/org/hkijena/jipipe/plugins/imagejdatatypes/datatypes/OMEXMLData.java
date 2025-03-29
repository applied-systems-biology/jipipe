package org.hkijena.jipipe.plugins.imagejdatatypes.datatypes;

import com.google.common.base.Charsets;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.services.OMEXMLService;
import ome.xml.meta.OMEXMLMetadata;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.plugins.strings.StringData;
import org.hkijena.jipipe.plugins.strings.XMLData;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Encapsulates OME an XML
 */
@SetJIPipeDocumentation(name = "OME XML", description = "XML data that follows the OME XML specifications")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single *.xml file that stores the current data.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/xml-data.schema.json")
public class OMEXMLData extends XMLData {

    private OMEXMLMetadata metadata;

    public OMEXMLData(String data) {
        super(data);
    }

    public OMEXMLData(StringData other) {
        super(other);
    }

    public static XMLData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path file = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".xml");
        try {
            return new XMLData(new String(Files.readAllBytes(file), Charsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads OME XML metadata from a string
     * @param metadataString the metadata string
     * @return the metadata
     */
    public OMEXMLMetadata readMetadataFromString(String metadataString) {
        try {
            ServiceFactory serviceFactory = new ServiceFactory();
            OMEXMLService omexmlService = serviceFactory.getInstance(OMEXMLService.class);
            return omexmlService.createOMEXMLMetadata(metadataString);
        } catch (DependencyException | ServiceException e) {
            throw new RuntimeException(e);
        }
    }

    public OMEXMLMetadata getMetadata() {
        if(metadata == null) {
            metadata = readMetadataFromString(getData());
        }
        return metadata;
    }
}
