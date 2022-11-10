package de.htw.berlin.covidspings

fun main() {
    val variantCaller = RealtimeVariantCaller("input/reference.fasta")

    variantCaller.processBam("input/input-1/input.bam")
    // variantCaller.processBam("input/input-2/input.bam")
    // variantCaller.processBam("input/input-3/input.bam")
    // variantCaller.processBam("input/input-4/input.bam")
    // variantCaller.processBam("input/input-5/input.bam")
    // variantCaller.processBam("input/input-6/input.bam")
    // variantCaller.processBam("input/input-7/input.bam")
    // variantCaller.processBam("input/input-8/input.bam")
    // variantCaller.processBam("input/input-9/input.bam")
    // variantCaller.processBam("input/input-10/input.bam")

    variantCaller.writeVcf("output/output.vcf")
    variantCaller.writeJson("output/output.json")
}