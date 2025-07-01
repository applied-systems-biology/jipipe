class: Workflow
cwlVersion: v1.2
inputs: []
outputs:
- id: hello_world_in_two_lines
  outputSource: [step1/out, step2/out]
  type: {items: string, type: array}
requirements:
- {class: MultipleInputFeatureRequirement}
steps:
- id: step1
  in:
  - {default: hello, id: in}
  out: [out]
  run:
    baseCommand: echo
    class: CommandLineTool
    cwlVersion: v1.2
    inputs:
    - id: in
      inputBinding: {}
      type: Any
    outputs:
    - id: out
      outputBinding: {glob: out.txt, loadContents: true, outputEval: '$(self[0].contents)'}
      type: string
    requirements: []
    stdout: out.txt
- id: step2
  in:
  - {default: world, id: in}
  out: [out]
  run:
    baseCommand: echo
    class: CommandLineTool
    cwlVersion: v1.2
    inputs:
    - id: in
      inputBinding: {}
      type: Any
    outputs:
    - id: out
      outputBinding: {glob: out.txt, loadContents: true, outputEval: '$(self[0].contents)'}
      type: string
    requirements: []
    stdout: out.txt
