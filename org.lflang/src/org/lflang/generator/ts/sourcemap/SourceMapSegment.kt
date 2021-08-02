package org.lflang.generator.ts.sourcemap

/**
 * Represents a source map segment that follows the specification provided in
 * the Source Map Revision 3 Proposal. Only supports segments that encode the
 * first four fields (column, source file, source line source column), and not
 * the fifth field (name).
 *
 * By their nature, source map segments must contain references to the segments
 * that precede them in order to be correctly as strings; for this reason, they
 * may be considered linked lists.
 *
 * @see <a href="https://sourcemaps.info/spec.html">Source Map Revision
 *     3 Proposal</a>
 * @param targetLine the absolute line in the target code corresponding to this
 * segment
 * @param targetColumn the absolute column in the target code corresponding to
 * this segment
 * @param sourceFile the absolute zero-based index of the source file, as listed
 * in the source map
 * @param sourceLine the absolute zero-based starting line in the original
 * source
 * @param sourceColumn the absolute zero-based starting column in the original
 * source
 */
class SourceMapSegment(
    private val targetLine: Int,
    private val targetColumn: Int,
    private val sourceFile: Int,
    private val sourceLine: Int,
    private val sourceColumn: Int,
    private var precedingSegment: SourceMapSegment?
) {
    /**
     * Generates a source map segment as specified in the Source Map Revision
     * 3 Proposal.
     */
    override fun toString(): String {
        val out = StringBuilder()
        out.append(VLQ.toVLQ(targetColumn))
        if (precedingSegment !== null) {
            // Preceded by nullity check, and the var doesn't mutate here.
            out.append(VLQ.toVLQ(sourceFile - precedingSegment!!.sourceFile))
            out.append(VLQ.toVLQ(sourceLine - precedingSegment!!.sourceLine))
            out.append(VLQ.toVLQ(sourceColumn - precedingSegment!!.sourceColumn))
        } else {
            out.append(VLQ.toVLQ(sourceFile))
            out.append(VLQ.toVLQ(sourceLine))
            out.append(VLQ.toVLQ(sourceColumn))
        }
        return out.toString()
    }

    /**
     * Returns a string representation of all segments up to and including
     * the current one.
     */
    fun getMappings(): String {
        return getMappingsRecursive().toString()
    }

    /**
     * Prepends a SourceMapSegment to the sequence of SourceMapSegments tracked
     * by this SourceMapSegment.
     * @param newTail the SourceMapSegment to be prepended
     */
    fun setTail(newTail: SourceMapSegment?) {
        var tail: SourceMapSegment = this
        while (tail.precedingSegment !== null) {
            tail = tail.precedingSegment!!
        }
        tail.precedingSegment = newTail
    }

    /**
     * Returns a StringBuilder representing all segments up to and including
     * the current one.
     */
    private fun getMappingsRecursive(): StringBuilder {
        if (precedingSegment === null) {
            return StringBuilder(toString())
        }
        // Preceded by nullity check, and the var doesn't mutate in this method.
        val builder = precedingSegment!!.getMappingsRecursive()
        if (targetLine == precedingSegment!!.targetLine) {
            builder.append(',')
        }
        builder.append(";".repeat(targetLine - precedingSegment!!.targetLine))
        builder.append(toString())
        return builder
    }

    companion object {
        /**
         * Initializes a SourceMapSegment relative to precedingSegment using data
         * encoded in s, a string that complies with the Source Map Revision 3
         * Proposal.
         *
         * @param precedingSegment the SourceMapSegment preceding this
         *     SourceMapSegment
         * @param s a string that complies with the Source Map Revision 3
         *     Proposal
         * @param incrementLine whether the target line represented by s is the
         *     is the line that follows that of precedingSegment, rather than the
         *     same line
         */
        fun fromString(
                precedingSegment: SourceMapSegment?,
                s: String,
                incrementLine: Boolean
        ): SourceMapSegment {
            val decoded = VLQ.fromVLQ(s)
            var targetLine = 0
            var targetColumn = decoded.get(0)
            var sourceFile = decoded.get(1)
            var sourceLine = decoded.get(2)
            var sourceColumn = decoded.get(3)
            if (precedingSegment !== null) {
                if (precedingSegment.targetLine == targetLine) {
                    targetColumn += precedingSegment.targetColumn
                    sourceFile += precedingSegment.sourceFile
                    sourceLine += precedingSegment.sourceLine
                    sourceColumn += precedingSegment.sourceColumn
                    targetLine = precedingSegment.targetLine
                }
            }
            if (incrementLine) {
                targetLine++
            }
            return SourceMapSegment(
                    targetLine,
                    targetColumn,
                    sourceFile,
                    sourceLine,
                    sourceColumn,
                    precedingSegment
            )
        }
    }
}