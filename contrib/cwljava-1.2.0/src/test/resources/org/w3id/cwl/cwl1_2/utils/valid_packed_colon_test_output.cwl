arguments: ['mkdir $(inputs.outdir_name);

    cp $(inputs.input_file.path) $(inputs.outdir_name)/;

    ']
baseCommand: [bash, -c]
class: CommandLineTool
cwlVersion: v1.2
hints:
  DockerRequirement: {dockerPull: 'docker.io/bash:4.4'}
inputs:
- {id: input_file, type: File}
- {id: outdir_name, type: string}
outputs:
- id: result
  outputBinding: {glob: $(inputs.outdir_name)}
  type: Directory
requirements: []
