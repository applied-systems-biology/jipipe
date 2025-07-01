class: Workflow
cwlVersion: v1.2
inputs:
- {id: bam, type: string}
- id: capture_kit[]
  type:
    fields:
    - {name: bait, type: string}
    name: user_type_3
    type: record
outputs:
- {id: output_bam, outputSource: touch_bam/empty_file, type: File}
requirements:
- {class: SubworkflowFeatureRequirement}
- {class: InlineJavascriptRequirement}
steps:
- id: touch_bam
  in:
  - {id: name, source: bam}
  out: [empty_file]
  run:
    baseCommand: [touch]
    class: CommandLineTool
    hints:
      DockerRequirement: {dockerPull: debian:stretch-slim}
    inputs:
    - id: name
      inputBinding: {position: 0}
      type: string
    outputs:
    - id: empty_file
      outputBinding: {glob: $(inputs.name)}
      type: File
    requirements:
    - {class: InlineJavascriptRequirement}
