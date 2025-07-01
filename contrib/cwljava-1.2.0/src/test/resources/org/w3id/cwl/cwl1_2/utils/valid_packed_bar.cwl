baseCommand: [echo]
class: CommandLineTool
cwlVersion: v1.2
inputs:
- {id: in1, type: int}
outputs:
- id: out1
  outputBinding: {outputEval: bar $(inputs.in1)}
  type: string
requirements:
- {class: InlineJavascriptRequirement}
