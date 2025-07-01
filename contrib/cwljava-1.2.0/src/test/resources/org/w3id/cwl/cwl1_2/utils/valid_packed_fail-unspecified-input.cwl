arguments: [echo, $(inputs.in), $(inputs.in2)]
class: CommandLineTool
cwlVersion: v1.2
inputs:
- {id: in, type: string}
outputs:
- id: out
  outputBinding: {glob: out.txt, loadContents: true, outputEval: '$(self[0].contents)'}
  type: string
requirements:
- {class: InlineJavascriptRequirement}
stdout: out.txt
