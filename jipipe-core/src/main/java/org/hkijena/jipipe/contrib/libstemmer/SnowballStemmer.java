package org.hkijena.jipipe.contrib.libstemmer;

/**
 * Parent class of all snowball stemmers, which must implement <code>stem</code>
 */
public abstract class SnowballStemmer extends SnowballProgram {
    static final long serialVersionUID = 2016072500L;

    public abstract boolean stem();
};
