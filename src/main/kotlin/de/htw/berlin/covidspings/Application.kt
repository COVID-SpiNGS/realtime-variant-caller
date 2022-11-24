package de.htw.berlin.covidspings

import java.time.Instant

fun main() {
    // val start = Instant.now().toEpochMilli()




    val variantCaller = RealtimeVariantCaller("input/reference.fasta")

    variantCaller.processBam("input/input.bam")

    // val start = Instant.now().toEpochMilli()
    // variantCaller.processBam("input/input-10/input.bam")
    // println("${Instant.now().toEpochMilli() - start}")

    // variantCaller.processBam("input/real/input_001.bam")
    // println("${Instant.now().toEpochMilli() - start}")

    // variantCaller.processBam("input/real/input_002.bam")
    // println("${Instant.now().toEpochMilli() - start}")

    // variantCaller.processBam("input/real/input_003.bam")
    // println("${Instant.now().toEpochMilli() - start}")

    // variantCaller.processBam("input/real/input_004.bam")
    // println("${Instant.now().toEpochMilli() - start}")

    // variantCaller.processBam("input/real/input_005.bam")
    // println("${Instant.now().toEpochMilli() - start}")

    // variantCaller.processBam("input/real/input_006.bam")
    // println("${Instant.now().toEpochMilli() - start}")

    // variantCaller.processBam("input/real/input_007.bam")
    // println("${Instant.now().toEpochMilli() - start}")

    // variantCaller.processBam("input/real/input_008.bam")
    // println("${Instant.now().toEpochMilli() - start}")

    // variantCaller.processBam("input/real/input_009.bam")
    // println("${Instant.now().toEpochMilli() - start}")

    // variantCaller.processBam("input/real/input_010.bam")
    // println("${Instant.now().toEpochMilli() - start}")

    variantCaller.writeVcf("output/output.vcf")
    // variantCaller.writeJson("output/output.json")


    // println("Total ${Instant.now().toEpochMilli() - start}")
}