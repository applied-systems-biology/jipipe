arguments:
- echo
- a
- {shellQuote: false, valueFrom: '&&'}
- echo
- b
class: CommandLineTool
cwlVersion: v1.2
inputs: []
outputs:
- id: out
  outputBinding: {glob: out.txt, loadContents: true, outputEval: '$(self[0].contents)'}
  type: string
requirements:
- {class: ShellCommandRequirement}
stdout: out.txt
