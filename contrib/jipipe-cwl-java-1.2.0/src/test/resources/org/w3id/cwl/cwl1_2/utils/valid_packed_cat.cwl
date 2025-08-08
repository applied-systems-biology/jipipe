baseCommand: [echo]
class: CommandLineTool
cwlVersion: v1.2
inputs:
- {id: in1, type: int}
- {id: in2, type: int}
- {id: in3, type: int}
outputs:
- id: out1
  outputBinding: {outputEval: $(inputs.in1)$(inputs.in2)$(inputs.in3)}
  type: string
requirements:
- {class: InlineJavascriptRequirement}
