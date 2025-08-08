/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.contrib.cwl.cwl1_2;

import org.hkijena.jipipe.contrib.cwl.cwl1_2.utils.Saveable;

/**
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#SoftwarePackage</I><BR>This interface is implemented by {@link SoftwarePackageImpl}<BR>
 */
public interface SoftwarePackage extends Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#SoftwarePackage/package</I><BR>
   * <BLOCKQUOTE>
   * The name of the software to be made available. If the name is
   * common, inconsistent, or otherwise ambiguous it should be combined with
   * one or more identifiers in the `specs` field.
   *    * </BLOCKQUOTE>
   */

  String getPackage();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#SoftwarePackage/version</I><BR>
   * <BLOCKQUOTE>
   * The (optional) versions of the software that are known to be
   * compatible.
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<java.util.List<String>> getVersion();
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

  java.util.Optional<java.util.List<String>> getSpecs();
}
