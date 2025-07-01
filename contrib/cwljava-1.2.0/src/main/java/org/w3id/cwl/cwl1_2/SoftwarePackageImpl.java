// Copyright Common Workflow Language project contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.w3id.cwl.cwl1_2;

import org.w3id.cwl.cwl1_2.utils.LoaderInstances;
import org.w3id.cwl.cwl1_2.utils.LoadingOptions;
import org.w3id.cwl.cwl1_2.utils.LoadingOptionsBuilder;
import org.w3id.cwl.cwl1_2.utils.SaveableImpl;
import org.w3id.cwl.cwl1_2.utils.ValidationException;

/**
* Auto-generated class implementation for <I>https://w3id.org/cwl/cwl#SoftwarePackage</I><BR>
 */
public class SoftwarePackageImpl extends SaveableImpl implements SoftwarePackage {
  private LoadingOptions loadingOptions_ = new LoadingOptionsBuilder().build();
  private java.util.Map<String, Object> extensionFields_ =
      new java.util.HashMap<String, Object>();

  private String package_;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#SoftwarePackage/package</I><BR>
   * <BLOCKQUOTE>
   * The name of the software to be made available. If the name is
   * common, inconsistent, or otherwise ambiguous it should be combined with
   * one or more identifiers in the `specs` field.
   *    * </BLOCKQUOTE>
   */

  public String getPackage() {
    return this.package_;
  }

  private java.util.Optional<java.util.List<String>> version;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#SoftwarePackage/version</I><BR>
   * <BLOCKQUOTE>
   * The (optional) versions of the software that are known to be
   * compatible.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<java.util.List<String>> getVersion() {
    return this.version;
  }

  private java.util.Optional<java.util.List<String>> specs;

  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#SoftwarePackage/specs</I><BR>
   * <BLOCKQUOTE>
   * One or more [IRI](https://en.wikipedia.org/wiki/Internationalized_Resource_Identifier)s
   * identifying resources for installing or enabling the software named in
   * the `package` field. Implementations may provide resolvers which map
   * these software identifier IRIs to some configuration action; or they can
   * use only the name from the `package` field on a best effort basis.
   * 
   * For example, the IRI https://packages.debian.org/bowtie could
   * be resolved with `apt-get install bowtie`. The IRI
   * https://anaconda.org/bioconda/bowtie could be resolved with `conda
   * install -c bioconda bowtie`.
   * 
   * IRIs can also be system independent and used to map to a specific
   * software installation or selection mechanism.
   * Using [RRID](https://www.identifiers.org/rrid/) as an example:
   * https://identifiers.org/rrid/RRID:SCR_005476
   * could be fulfilled using the above-mentioned Debian or bioconda
   * package, a local installation managed by [Environment Modules](https://modules.sourceforge.net/),
   * or any other mechanism the platform chooses. IRIs can also be from
   * identifier sources that are discipline specific yet still system
   * independent. As an example, the equivalent [ELIXIR Tools and Data
   * Service Registry](https://bio.tools) IRI to the previous RRID example is
   * https://bio.tools/tool/bowtie2/version/2.2.8.
   * If supported by a given registry, implementations are encouraged to
   * query these system independent software identifier IRIs directly for
   * links to packaging systems.
   * 
   * A site specific IRI can be listed as well. For example, an academic
   * computing cluster using Environment Modules could list the IRI
   * `https://hpc.example.edu/modules/bowtie-tbb/1.22` to indicate that
   * `module load bowtie-tbb/1.1.2` should be executed to make available
   * `bowtie` version 1.1.2 compiled with the TBB library prior to running
   * the accompanying Workflow or CommandLineTool. Note that the example IRI
   * is specific to a particular institution and computing environment as
   * the Environment Modules system does not have a common namespace or
   * standardized naming convention.
   * 
   * This last example is the least portable and should only be used if
   * mechanisms based off of the `package` field or more generic IRIs are
   * unavailable or unsuitable. While harmless to other sites, site specific
   * software IRIs should be left out of shared CWL descriptions to avoid
   * clutter.
   *    * </BLOCKQUOTE>
   */

  public java.util.Optional<java.util.List<String>> getSpecs() {
    return this.specs;
  }

  /**
   * Used by {@link org.w3id.cwl.cwl1_2.utils.RootLoader} to construct instances of SoftwarePackageImpl.
   *
   * @param __doc_            Document fragment to load this record object from (presumably a
                              {@link java.util.Map}).
   * @param __baseUri_        Base URI to generate child document IDs against.
   * @param __loadingOptions  Context for loading URIs and populating objects.
   * @param __docRoot_        ID at this position in the document (if available) (maybe?)
   * @throws ValidationException If the document fragment is not a {@link java.util.Map}
   *                             or validation of fields fails.
   */
  public SoftwarePackageImpl(
      final Object __doc_,
      final String __baseUri_,
      LoadingOptions __loadingOptions,
      final String __docRoot_) {
    super(__doc_, __baseUri_, __loadingOptions, __docRoot_);
    // Prefix plumbing variables with '__' to reduce likelihood of collision with
    // generated names.
    String __baseUri = __baseUri_;
    String __docRoot = __docRoot_;
    if (!(__doc_ instanceof java.util.Map)) {
      throw new ValidationException("SoftwarePackageImpl called on non-map");
    }
    final java.util.Map<String, Object> __doc = (java.util.Map<String, Object>) __doc_;
    final java.util.List<ValidationException> __errors =
        new java.util.ArrayList<ValidationException>();
    if (__loadingOptions != null) {
      this.loadingOptions_ = __loadingOptions;
    }
    String package_;
    try {
      package_ =
          LoaderInstances
              .StringInstance
              .loadField(__doc.get("package"), __baseUri, __loadingOptions);
    } catch (ValidationException e) {
      package_ = null; // won't be used but prevents compiler from complaining.
      final String __message = "the `package` field is not valid because:";
      __errors.add(new ValidationException(__message, e));
    }
    java.util.Optional<java.util.List<String>> version;

    if (__doc.containsKey("version")) {
      try {
        version =
            LoaderInstances
                .optional_array_of_StringInstance
                .loadField(__doc.get("version"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        version = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `version` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      version = null;
    }
    java.util.Optional<java.util.List<String>> specs;

    if (__doc.containsKey("specs")) {
      try {
        specs =
            LoaderInstances
                .uri_optional_array_of_StringInstance_False_False_None_True
                .loadField(__doc.get("specs"), __baseUri, __loadingOptions);
      } catch (ValidationException e) {
        specs = null; // won't be used but prevents compiler from complaining.
        final String __message = "the `specs` field is not valid because:";
        __errors.add(new ValidationException(__message, e));
      }

    } else {
      specs = null;
    }
    if (!__errors.isEmpty()) {
      throw new ValidationException("Trying 'RecordField'", __errors);
    }
    this.package_ = (String) package_;
    this.version = (java.util.Optional<java.util.List<String>>) version;
    this.specs = (java.util.Optional<java.util.List<String>>) specs;
  }
}
