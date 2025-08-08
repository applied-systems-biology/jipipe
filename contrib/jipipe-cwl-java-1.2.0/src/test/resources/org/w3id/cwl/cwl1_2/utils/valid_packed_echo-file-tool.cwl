baseCommand: [echo]
class: CommandLineTool
cwlVersion: v1.2
inputs:
- id: in
  inputBinding: {position: 1}
  type: string
- id: name
  type: ['null', string]
outputs:
- {id: out, type: stdout}
requirements:
- {class: InlineJavascriptRequirement}
stdout: out.txt
