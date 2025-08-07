package edu.kit.datamanager.ro_crate.writer;

/**
 * An Interface for {@link GenericWriterStrategy} implementations which support writing
 * <a href=https://github.com/TheELNConsortium/TheELNFileFormat>ELN-Style crates</a>.
 *
 * @param <DESTINATION_TYPE> the type which determines the destination of the result
 */
public interface ElnFormatWriter<DESTINATION_TYPE> extends GenericWriterStrategy<DESTINATION_TYPE> {

    /**
     * Write in ELN format style, meaning with a root subfolder in the zip file.
     * Same as {@link #withRootSubdirectory()}.
     *
     * @return this writer
     */
    ElnFormatWriter<DESTINATION_TYPE> usingElnStyle();

    /**
     * Alias with more generic name for {@link #usingElnStyle()}.
     *
     * @return this writer
     */
    default ElnFormatWriter<DESTINATION_TYPE> withRootSubdirectory() {
        return this.usingElnStyle();
    }
}
