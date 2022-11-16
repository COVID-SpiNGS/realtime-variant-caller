package de.htw.berlin.covidspings

import kotlin.system.measureTimeMillis

fun main() {
    val elapsed = measureTimeMillis {
        val minAlleleDepth = 5
        val minEvidenceRatio = 0.0f
        val maxVariants = 1
        val minTotalDepth = 10
        val minMappingQuality: Byte = 20
        val minBaseQuality: Byte = 30

        val variantCaller = RealtimeVariantCaller(
            "input/reference.fasta",
            minBaseQuality,
            minMappingQuality,
            minTotalDepth,
            minAlleleDepth,
            minEvidenceRatio,
            maxVariants,
            filterDuplicates = true,
            includeSitesWithoutReads = false
        )

        val subElapsed1 = measureTimeMillis { variantCaller.processBam("input/real/input_001.bam") }
        println("1 $subElapsed1")

        val subElapsed2 = measureTimeMillis { variantCaller.processBam("input/real/input_002.bam") }
        println("2 $subElapsed2")

        val subElapsed3 = measureTimeMillis { variantCaller.processBam("input/real/input_003.bam") }
        println("3 $subElapsed3")

        val subElapsed4 = measureTimeMillis { variantCaller.processBam("input/real/input_004.bam") }
        println("4 $subElapsed4")

        val subElapsed5 = measureTimeMillis { variantCaller.processBam("input/real/input_005.bam") }
        println("5 $subElapsed5")

        val subElapsed6 = measureTimeMillis { variantCaller.processBam("input/real/input_006.bam") }
        println("6 $subElapsed6")

        val subElapsed7 = measureTimeMillis { variantCaller.processBam("input/real/input_007.bam") }
        println("7 $subElapsed7")

        val subElapsed8 = measureTimeMillis { variantCaller.processBam("input/real/input_008.bam") }
        println("8 $subElapsed8")

        val subElapsed9 = measureTimeMillis { variantCaller.processBam("input/real/input_009.bam") }
        println("9 $subElapsed9")

        val subElapsed10 = measureTimeMillis { variantCaller.processBam("input/real/input_010.bam") }
        println("10 $subElapsed10")

        variantCaller.writeVcf("output/output.vcf")
        // variantCaller.writeJson("output/output.json")

    }

    println("Total $elapsed")
}