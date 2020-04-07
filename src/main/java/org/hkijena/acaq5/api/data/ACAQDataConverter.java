package org.hkijena.acaq5.api.data;

/**
 * Interface that converts between {@link ACAQData} instances
 */
public interface ACAQDataConverter {

    /**
     * @return the data type that is the input of the conversion function
     */
    Class<? extends ACAQData> getInputType();

    /**
     * @return the data type that is the output of the conversion function
     */
    Class<? extends ACAQData> getOutputType();

    /**
     * Converts the supported input type to the output type
     *
     * @param input the input data
     * @return the converted input data
     */
    ACAQData convert(ACAQData input);
}
