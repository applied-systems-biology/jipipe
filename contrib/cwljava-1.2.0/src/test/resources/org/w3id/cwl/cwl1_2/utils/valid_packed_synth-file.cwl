baseCommand: [cat]
class: CommandLineTool
cwlVersion: v1.2
inputs:
- id: names
  inputBinding: {position: 1}
  type: File
outputs:
- {id: sequence, type: stdout}
requirements: []
