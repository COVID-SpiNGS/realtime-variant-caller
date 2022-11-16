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

        val subElapsed1 = measureTimeMillis { variantCaller.processBam("input/input-1/input.bam") }
        println("1 $subElapsed1")

        val subElapsed2 = measureTimeMillis { variantCaller.processBam("input/input-2/input.bam") }
        println("2 $subElapsed2")

        val subElapsed3 = measureTimeMillis { variantCaller.processBam("input/input-3/input.bam") }
        println("3 $subElapsed3")

        val subElapsed4 = measureTimeMillis { variantCaller.processBam("input/input-4/input.bam") }
        println("4 $subElapsed4")

        val subElapsed5 = measureTimeMillis { variantCaller.processBam("input/input-5/input.bam") }
        println("5 $subElapsed5")

        val subElapsed6 = measureTimeMillis { variantCaller.processBam("input/input-6/input.bam") }
        println("6 $subElapsed6")

        val subElapsed7 = measureTimeMillis { variantCaller.processBam("input/input-7/input.bam") }
        println("7 $subElapsed7")

        val subElapsed8 = measureTimeMillis { variantCaller.processBam("input/input-8/input.bam") }
        println("8 $subElapsed8")

        val subElapsed9 = measureTimeMillis { variantCaller.processBam("input/input-9/input.bam") }
        println("9 $subElapsed9")

        val subElapsed10 = measureTimeMillis { variantCaller.processBam("input/input-10/input.bam") }
        println("10 $subElapsed10")

        variantCaller.writeVcf("output/output.vcf")
        // variantCaller.writeJson("output/output.json")

    }

    println("Total $elapsed")
}