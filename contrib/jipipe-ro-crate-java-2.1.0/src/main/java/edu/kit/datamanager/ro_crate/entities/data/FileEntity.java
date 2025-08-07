package edu.kit.datamanager.ro_crate.entities.data;

/**
 * A helping class representing a File entity.
 *
 * @author Nikola Tzotchev on 5.2.2022 г.
 * @version 1
 */
public class FileEntity extends DataEntity {

  private static final String TYPE = "File";

  public FileEntity(AbstractFileEntityBuilder<?> entityBuilder) {
    super(entityBuilder);
    this.addType(TYPE);
  }

  abstract static class AbstractFileEntityBuilder<T extends AbstractFileEntityBuilder<T>> extends
      AbstractDataEntityBuilder<T> {

    public T setEncodingFormat(String encodingFormat) {
      this.addProperty("encodingFormat", encodingFormat);
      return self();
    }

    @Override
    public abstract FileEntity build();
  }

  /**
   * Builder class for the easier creation of the File entities.
   */
  public static final class FileEntityBuilder extends AbstractFileEntityBuilder<FileEntityBuilder> {

    @Override
    public FileEntityBuilder self() {
      return this;
    }

    @Override
    public FileEntity build() {
      return new FileEntity(this);
    }
  }
}
