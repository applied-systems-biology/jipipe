package edu.kit.datamanager.ro_crate.writer;

import edu.kit.datamanager.ro_crate.Crate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Implementation of the writing strategy to provide a way of writing crates to
 * a zip archive.
 */
public class WriteZipStrategy implements
        GenericWriterStrategy<String>,
        ElnFormatWriter<String>
{
    private static final Logger logger = LoggerFactory.getLogger(WriteZipStrategy.class);
    protected ElnFormatWriter<OutputStream> delegate = new WriteZipStreamStrategy();

    @Override
    public ElnFormatWriter<String> usingElnStyle() {
        this.delegate = this.delegate.withRootSubdirectory();
        return this;
    }

    @Override
    public void save(Crate crate, String destination) throws IOException {
        this.delegate.save(crate, new FileOutputStream(destination));
    }
}
