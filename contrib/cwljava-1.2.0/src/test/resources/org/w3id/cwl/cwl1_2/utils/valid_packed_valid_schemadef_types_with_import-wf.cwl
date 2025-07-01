class: Workflow
cwlVersion: v1.2
inputs:
- id: readgroups_bam
  type:
    fields:
    - {name: bam, type: File}
    - name: readgroup_meta_list
      type:
        items:
          fields:
          - {name: CN, type: string}
          - {name: DT, type: string}
          - {name: ID, type: string}
          - {name: LB, type: string}
          - {name: PI, type: string}
          - {name: PL, type: string}
          - {name: SM, type: string}
          name: readgroup_meta
          type: record
        type: array
    name: readgroups_bam_file
    type: record
outputs:
- {id: out, outputSource: step1/out, type: string}
requirements: []
steps:
- id: step1
  in:
  - {id: message, source: readgroups_bam}
  out: [out]
  run:
    baseCommand: echo
    class: CommandLineTool
    cwlVersion: v1.2
    inputs:
    - id: message
      inputBinding: {valueFrom: '$(self.readgroup_meta_list[0][''DT''])'}
      type:
        fields:
        - {name: bam, type: File}
        - name: readgroup_meta_list
          type:
            items:
              fields:
              - {name: CN, type: string}
              - {name: DT, type: string}
              - {name: ID, type: string}
              - {name: LB, type: string}
              - {name: PI, type: string}
              - {name: PL, type: string}
              - {name: SM, type: string}
              name: readgroup_meta_23
              type: record
            type: array
        name: readgroups_bam_file_18
        type: record
    outputs:
    - id: out
      outputBinding: {glob: output.txt, loadContents: true, outputEval: '$(self[0].contents)'}
      type: string
    requirements:
    - {class: InlineJavascriptRequirement}
    stdout: output.txt
