arguments: [python, $(inputs.script), $(inputs.r.basename)]
class: CommandLineTool
cwlVersion: v1.2
hints:
  DockerRequirement: {dockerPull: python:2.7.15-alpine3.7}
inputs:
- {id: r, type: File}
- default: {class: File, location: updateval.py}
  id: script
  type: File
outputs:
- id: out
  outputBinding: {glob: $(inputs.r.basename)}
  type: File
requirements:
- class: InitialWorkDirRequirement
  listing:
  - {entry: $(inputs.r), writable: true}
- {class: InplaceUpdateRequirement, inplaceUpdate: true}
- {class: InlineJavascriptRequirement}
