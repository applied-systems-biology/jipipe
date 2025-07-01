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

package org.w3id.cwl.cwl1_1;

import org.w3id.cwl.cwl1_1.utils.Saveable;

/**
* Auto-generated interface for <I>https://w3id.org/cwl/cwl#DockerRequirement</I><BR>This interface is implemented by {@link DockerRequirementImpl}<BR> <BLOCKQUOTE>
 Indicates that a workflow component should be run in a
 [Docker](http://docker.com) or Docker-compatible (such as
 [Singularity](https://www.sylabs.io/) and [udocker](https://github.com/indigo-dc/udocker)) container environment and
 specifies how to fetch or build the image.
 
 If a CommandLineTool lists `DockerRequirement` under
 `hints` (or `requirements`), it may (or must) be run in the specified Docker
 container.
 
 The platform must first acquire or install the correct Docker image as
 specified by `dockerPull`, `dockerImport`, `dockerLoad` or `dockerFile`.
 
 The platform must execute the tool in the container using `docker run` with
 the appropriate Docker image and tool command line.
 
 The workflow platform may provide input files and the designated output
 directory through the use of volume bind mounts.  The platform should rewrite
 file paths in the input object to correspond to the Docker bind mounted
 locations. That is, the platform should rewrite values in the parameter context
 such as `runtime.outdir`, `runtime.tmpdir` and others to be valid paths
 within the container. The platform must ensure that `runtime.outdir` and
 `runtime.tmpdir` are distinct directories.
 
 When running a tool contained in Docker, the workflow platform must not
 assume anything about the contents of the Docker container, such as the
 presence or absence of specific software, except to assume that the
 generated command line represents a valid command within the runtime
 environment of the container.
 
 A container image may specify an
 [ENTRYPOINT](https://docs.docker.com/engine/reference/builder/#entrypoint)
 and/or
 [CMD](https://docs.docker.com/engine/reference/builder/#cmd).
 Command line arguments will be appended after all elements of
 ENTRYPOINT, and will override all elements specified using CMD (in
 other words, CMD is only used when the CommandLineTool definition
 produces an empty command line).
 
 Use of implicit ENTRYPOINT or CMD are discouraged due to reproducibility
 concerns of the implicit hidden execution point (For further discussion, see
 [https://doi.org/10.12688/f1000research.15140.1](https://doi.org/10.12688/f1000research.15140.1)). Portable
 CommandLineTool wrappers in which use of a container is optional must not rely on ENTRYPOINT or CMD.
 CommandLineTools which do rely on ENTRYPOINT or CMD must list `DockerRequirement` in the
 `requirements` section.
 
 ## Interaction with other requirements
 
 If [EnvVarRequirement](#EnvVarRequirement) is specified alongside a
 DockerRequirement, the environment variables must be provided to Docker
 using `--env` or `--env-file` and interact with the container's preexisting
 environment as defined by Docker.
  </BLOCKQUOTE>
 */
public interface DockerRequirement extends ProcessRequirement, Saveable {
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#DockerRequirement/class</I><BR>
   * <BLOCKQUOTE>
   * Always 'DockerRequirement'   * </BLOCKQUOTE>
   */

  DockerRequirement_class getClass_();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#DockerRequirement/dockerPull</I><BR>
   * <BLOCKQUOTE>
   * Specify a Docker image to retrieve using `docker pull`. Can contain the
   * immutable digest to ensure an exact container is used:
   * `dockerPull: ubuntu@sha256:45b23dee08af5e43a7fea6c4cf9c25ccf269ee113168c19722f87876677c5cb2`
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<String> getDockerPull();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#DockerRequirement/dockerLoad</I><BR>
   * <BLOCKQUOTE>
   * Specify a HTTP URL from which to download a Docker image using `docker load`.   * </BLOCKQUOTE>
   */

  java.util.Optional<String> getDockerLoad();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#DockerRequirement/dockerFile</I><BR>
   * <BLOCKQUOTE>
   * Supply the contents of a Dockerfile which will be built using `docker build`.   * </BLOCKQUOTE>
   */

  java.util.Optional<String> getDockerFile();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#DockerRequirement/dockerImport</I><BR>
   * <BLOCKQUOTE>
   * Provide HTTP URL to download and gunzip a Docker images using `docker import.   * </BLOCKQUOTE>
   */

  java.util.Optional<String> getDockerImport();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#DockerRequirement/dockerImageId</I><BR>
   * <BLOCKQUOTE>
   * The image id that will be used for `docker run`.  May be a
   * human-readable image name or the image identifier hash.  May be skipped
   * if `dockerPull` is specified, in which case the `dockerPull` image id
   * must be used.
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<String> getDockerImageId();
  /**
   * Getter for property <I>https://w3id.org/cwl/cwl#DockerRequirement/dockerOutputDirectory</I><BR>
   * <BLOCKQUOTE>
   * Set the designated output directory to a specific location inside the
   * Docker container.
   *    * </BLOCKQUOTE>
   */

  java.util.Optional<String> getDockerOutputDirectory();
}
