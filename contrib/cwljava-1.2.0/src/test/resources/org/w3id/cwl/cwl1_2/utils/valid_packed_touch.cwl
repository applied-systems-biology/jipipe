baseCommand: [touch]
class: CommandLineTool
cwlVersion: v1.2
hints:
  DockerRequirement: {dockerPull: debian:stretch-slim}
inputs:
- id: name
  inputBinding: {position: 0}
  type: string
outputs:
- id: empty_file
  outputBinding: {glob: $(inputs.name)}
  type: File
requirements:
- {class: InlineJavascriptRequirement}
