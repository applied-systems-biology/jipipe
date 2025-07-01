baseCommand: [/bin/sh, -c, echo $TEST_ENV]
class: CommandLineTool
cwlVersion: v1.2
hints:
  EnvVarRequirement:
    envDef: {TEST_ENV: $(inputs.in)}
inputs:
- {id: in, type: string}
outputs:
- id: out
  outputBinding: {glob: out}
  type: File
requirements:
- {class: InlineJavascriptRequirement}
stdout: out
