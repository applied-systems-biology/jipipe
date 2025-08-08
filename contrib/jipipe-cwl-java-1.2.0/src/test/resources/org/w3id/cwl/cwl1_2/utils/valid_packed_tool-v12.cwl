arguments: [echo, $(inputs.inp1)]
class: CommandLineTool
cwlVersion: v1.2
inputs:
- id: inp1
  secondaryFiles:
  - {pattern: '.2', required: true}
  type: File
outputs: []
requirements:
- {class: ResourceRequirement, coresMin: 0.5}
- {class: InlineJavascriptRequirement}
