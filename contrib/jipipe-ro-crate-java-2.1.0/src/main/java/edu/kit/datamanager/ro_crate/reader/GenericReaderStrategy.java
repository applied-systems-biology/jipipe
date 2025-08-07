package edu.kit.datamanager.ro_crate.reader;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;

/**
 * Generic interface for the strategy of the reader class.
 * This allows for flexible input types when implementing different reading strategies.
 *
 * @param <SOURCE_TYPE> the type which determines the source of the crate
 */
public interface GenericReaderStrategy<SOURCE_TYPE> {
    /**
     * Read the metadata.json file from the given location.
     *
     * @param location the location to read from
     * @return the parsed metadata.json as ObjectNode
     */
    ObjectNode readMetadataJson(SOURCE_TYPE location) throws IOException;

    /**
     * Read the content from the given location.
     *
     * @param location the location to read from
     * @return the content as a File
     */
    File readContent(SOURCE_TYPE location) throws IOException;
}