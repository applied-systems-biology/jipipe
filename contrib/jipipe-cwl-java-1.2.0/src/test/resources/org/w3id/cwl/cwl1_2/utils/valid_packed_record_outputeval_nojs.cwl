arguments: [$(inputs.ref.genome_fa) $(inputs.ref.annotation_gtf)]
baseCommand: echo
class: CommandLineTool
cwlVersion: v1.2
inputs:
- default: {annotation_gtf: gencode.vM21.primary_assembly.annotation.gtf, genome_fa: GRCm38.primary_assembly.genome.fa}
  id: ref
  type:
    fields:
    - {name: genome_fa, type: string}
    - {name: annotation_gtf, type: string}
    type: record
outputs:
- id: references
  outputBinding: {outputEval: $(inputs.ref)}
  type:
    fields:
    - {name: genome_fa, type: string}
    - {name: annotation_gtf, type: string}
    type: record
- {id: summary, type: stdout}
requirements: []
