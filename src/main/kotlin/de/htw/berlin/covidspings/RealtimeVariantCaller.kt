package de.htw.berlin.covidspings

import com.google.gson.GsonBuilder
import htsjdk.samtools.SAMSequenceDictionary
import htsjdk.samtools.SAMSequenceRecord
import htsjdk.samtools.SamReaderFactory
import htsjdk.samtools.ValidationStringency
import htsjdk.samtools.filter.DuplicateReadFilter
import htsjdk.samtools.reference.IndexedFastaSequenceFile
import htsjdk.samtools.util.SamLocusIterator
import htsjdk.variant.variantcontext.Allele
import htsjdk.variant.variantcontext.VariantContextBuilder
import htsjdk.variant.variantcontext.writer.Options
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder
import htsjdk.variant.vcf.*
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import org.jline.terminal.TerminalBuilder
import java.io.File
import java.util.logging.Logger
import kotlin.math.*


class RealtimeVariantCaller(
    private val fastaFile: IndexedFastaSequenceFile,
    private val minBaseQuality: Byte = 30,
    private val minMappingQuality: Byte = 20,
    private val minTotalDepth: Int = 10,
    private val minAlleleDepth: Int = 5,
    private val minEvidenceRatio: Float = 0.1f,
    private val maxVariants: Int = 5,
    private val filterDuplicates: Boolean = true,
    private val includeSitesWithoutReads: Boolean = false
) {
    private val sequenceDictionary: SAMSequenceDictionary
    private val samReaderFactory = SamReaderFactory
        .makeDefault()
        .validationStringency(ValidationStringency.SILENT)

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val memory = mutableMapOf<Int, Site>()
    private val terminalWidth = TerminalBuilder.terminal().width
    private val queue = PublishSubject.create<String>()
    private var busy: Boolean = false

    private val logger: Logger = Logger.getLogger(
        RealtimeVariantCaller::class.qualifiedName
    )

    constructor(fastaFile: String) : this(
        IndexedFastaSequenceFile(File(fastaFile)),
    ) {
        this.queue
            .observeOn(Schedulers.computation())
            .subscribe { this.processBam(it) }
    }

    init {
        val sequences = mutableListOf<SAMSequenceRecord>()
        do {
            val sequence = this.fastaFile.nextSequence()

            if (sequence != null) {
                val record = SAMSequenceRecord(sequence.name, sequence.length())

                sequences.add(record)
            }
        } while (sequence != null)

        this.fastaFile.reset()
        this.sequenceDictionary = SAMSequenceDictionary(sequences)
    }

    fun queueBam(bamFile: String) {
        this.queue.onNext(bamFile)
    }

    fun busy(): Boolean {
        return this.busy
    }

    fun writeJson(jsonFile: String) {
        this.busy = true
        this.logger.info("Start writing JSON $jsonFile")
        val json: Map<Int, JsonSite> = this.memory.mapValues { site ->
            JsonSite(
                site.value.referenceName,
                site.value.reference.toInt().toChar().toString(),
                site.value.totalDepth,
                site.value.variants.mapKeys { variant -> variant.key.toInt().toChar().toString() }
            )
        }

        File(jsonFile).writeText(this.gson.toJson(json))
        this.logger.info("Finished writing JSON $jsonFile")
        this.busy = false
    }

    private fun genotypeLikelihood(hypothesis: Byte, variants: Map<Byte, List<Double>>): Double {
        if (variants.containsKey(hypothesis)) {
            val hypothesisValue = variants[hypothesis]!!
                .map { 1.0 - it }
                .reduce { acc, cur -> acc * cur }

            val nonHypothesisValue = variants
                .filterKeys { it != hypothesis }
                .flatMap { it.value }
                .fold(1.0) { acc, cur -> acc * cur }

            return hypothesisValue * max(nonHypothesisValue, Double.MIN_VALUE)
        } else {
            throw Exception("Hypothesis $hypothesis not existent in variants")
        }
    }

    private fun toLog10PError(probabilities: List<Double>): List<Double> {
        return probabilities.map { this.toLog10PError(it) }
    }

    private fun toLog10PError(probability: Double): Double {
        return if (probability > 0.0) {
            ((10.0 * log10(probability))) / 10.0
        } else {
            Double.MAX_VALUE
        }

    }

    private fun toPhredScale(probabilities: List<Double>): List<Byte> {
        return probabilities.map { this.toPhredScale(it) }
    }

    private fun toPhredScale(probability: Double): Byte {
        return if (probability > 0.0) {
            ((-10.0 * log10(probability)).roundToInt()).toByte()
        } else {
            Byte.MAX_VALUE
        }
    }

    private fun fromPhredScale(scores: List<Byte>): List<Double> {
        return scores.map { this.fromPhredScale(it) }
    }

    private fun fromPhredScale(score: Byte): Double {
        return 10.0.pow((score).toDouble() / -10.0)
    }

    fun processBam(bamFile: String) {
        this.busy = true
        this.logger.info("Start processing BAM $bamFile")

        val samReader = this.samReaderFactory.open(File(bamFile))
        val iterator = SamLocusIterator(samReader)

        iterator.qualityScoreCutoff = this.minBaseQuality.toInt()
        iterator.mappingQualityScoreCutoff = this.minMappingQuality.toInt()

        if (this.filterDuplicates) {
            iterator.setSamFilters(
                listOf(
                    DuplicateReadFilter()
                )
            )
        }

        val sites = iterator.toList()
        val recordsCount = sites.count()
        val progressBarBuilder = ProgressBarBuilder()

        val progressBar = progressBarBuilder
            .setTaskName("Reading BAM")
            .setInitialMax(recordsCount.toLong())
            .setUpdateIntervalMillis(50)
            .setMaxRenderedLength(this.terminalWidth)
            .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
            .build()

        progressBar.extraMessage = bamFile

        sites.forEach { site ->
            // Thread.sleep(1)
            progressBar.stepTo(site.position.toLong())
            val referenceBase = fastaFile.getSequence(site.sequenceName).bases[site.position - 1]

            if (site.recordAndOffsets.isNotEmpty() || this.includeSitesWithoutReads) {
                if (!this.memory.containsKey(site.position)) {
                    this.memory[site.position] = Site(
                        site.sequenceName,
                        referenceBase,
                        site.recordAndOffsets.count()
                    )
                } else {
                    this.memory[site.position]!!.totalDepth += site.recordAndOffsets.count()
                }
            }

            site.recordAndOffsets.forEach { record ->
                if (!this.memory[site.position]!!.variants.containsKey(record.readBase)) {
                    this.memory[site.position]!!.variants[record.readBase] = mutableListOf()
                }

                this.memory[site.position]!!.variants[record.readBase]!!.add(record.baseQuality)
            }
        }

        samReader.close()
        progressBar.refresh()
        println()

        this.logger.info("Finished processing BAM $bamFile")
        this.busy = false
    }

    fun writeVcf(vcfFile: String, includeRefCalls: Boolean = false) {
        this.busy = true
        this.logger.info("Start writing VCF $vcfFile")
        val header = VCFHeader(
            setOf(
                VCFFilterHeaderLine("PASS", "All filters passed"),
                VCFFilterHeaderLine("RefCall", "Reference base was called"),
                // VCFFilterHeaderLine("lowGQ", "Low genotype quality"),
                // VCFFilterHeaderLine("lowQUAL", "Low variant call quality"),
                // VCFFilterHeaderLine("conflictPos", "Overlapping record"),
                VCFInfoHeaderLine("DP", 1, VCFHeaderLineType.Integer, "Depth"),
                VCFInfoHeaderLine("AD", VCFHeaderLineCount.A, VCFHeaderLineType.Integer, "Allele depth"),
                VCFInfoHeaderLine(
                    "ER",
                    1,
                    VCFHeaderLineType.Float,
                    "Evidence ratio"
                ),
                VCFInfoHeaderLine("AN", 1, VCFHeaderLineType.Integer, "Total number of alleles"),
                // VCFInfoHeaderLine("EPM", 1, VCFHeaderLineType.Float, "Error probability mean"),
                VCFInfoHeaderLine(
                    "GL",
                    VCFHeaderLineCount.A,
                    VCFHeaderLineType.Float,
                    "Genotype likelihoods comprised of comma separated floating point log10-scaled likelihoods for all possible genotypes given the set of alleles defined in the REF and ALT fields"
                ),
                VCFInfoHeaderLine(
                    "PL",
                    VCFHeaderLineCount.A,
                    VCFHeaderLineType.Integer,
                    "The phred-scaled genotype likelihoods rounded to the closest integer (and otherwise defined precisely as the GL field)"
                ),
            )
        )

        header.sequenceDictionary = this.sequenceDictionary

        val builder = VariantContextWriterBuilder()
            .setReferenceDictionary(this.sequenceDictionary)
            .setOutputFile(vcfFile)
            .unsetOption(Options.INDEX_ON_THE_FLY)

        val writer = builder.build()
        val progressBarBuilder = ProgressBarBuilder()
        val memoryList = this.memory.toList()
        val memoryListCount = memoryList.count().toLong()

        val progressBar = progressBarBuilder
            .setTaskName("Writing VCF")
            .setInitialMax(memoryListCount)
            .setUpdateIntervalMillis(50)
            .setMaxRenderedLength(this.terminalWidth)
            .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
            .build()

        progressBar.extraMessage = vcfFile

        val variants = memoryList.flatMap { site ->
            progressBar.stepTo(site.first.toLong())

            val variantsWithErrorProbabilities = site.second.variants.mapValues { this.fromPhredScale(it.value) }
            val variantsWithGenotypeLikelihood = variantsWithErrorProbabilities.mapValues {
                this.genotypeLikelihood(
                    it.key,
                    variantsWithErrorProbabilities
                )
            }

            val sumGenotypeLikelihoods = variantsWithGenotypeLikelihood.values.sum()

            site.second.variants.map { variant ->
                val dp = site.second.totalDepth
                val ad = variant.value.count()
                val er = ad.toFloat() / dp.toFloat()
                val an = this.memory[site.first]!!.variants.count()
                val gl = variantsWithGenotypeLikelihood.values.map { log10(it) }
                val pl = gl.map { (-10.0 * it).roundToInt() }

                val variantContextBuilder = VariantContextBuilder(
                    site.second.referenceName,
                    site.second.referenceName,
                    site.first.toLong(),
                    site.first.toLong(),
                    listOf(
                        Allele.create(site.second.reference, true),
                        if (site.second.reference != variant.key) Allele.create(variant.key)
                        else Allele.UNSPECIFIED_ALTERNATE_ALLELE
                    )
                ).attributes(
                    mapOf(
                        "DP" to dp,
                        "AD" to ad,
                        "ER" to er,
                        "AN" to an,
                        "GL" to gl,
                        "PL" to pl
                    )
                ).log10PError(
                    min(
                        -0.0,
                        -0.1 * this.toPhredScale(1.0 - variantsWithGenotypeLikelihood[variant.key]!! / sumGenotypeLikelihoods)
                    )
                )

                if (site.second.reference != variant.key) {
                    variantContextBuilder.passFilters()
                } else {
                    variantContextBuilder.filters("RefCall")
                }

                variantContextBuilder.make()
            }
        }

        progressBar.stepTo(progressBar.max)

        writer.writeHeader(header)

        variants
            .filter { it.attributes["DP"] as Int >= this.minTotalDepth }
            .filter { it.attributes["AD"] as Int >= this.minAlleleDepth }
            .filter { it.attributes["ER"] as Float >= this.minEvidenceRatio }
            .filter { includeRefCalls || !it.filters.contains("RefCall") }
            .forEach { writer.add(it) }

        writer.close()
        progressBar.refresh()
        println()
        this.logger.info("Finished writing VCF $vcfFile")
        this.busy = false
    }
}