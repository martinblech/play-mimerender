package mimeparse

/**
 * MIME-Type Parser
 * 
 * This object provides basic functions for handling mime-types. It can handle
 * matching mime-types against a list of media-ranges. See section 14.1 of the
 * HTTP specification [RFC 2616] for a complete explanation.
 * 
 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1
 * 
 * A port to Scala of Tom Zellman's Java MIME-Type Parser:
 * 
 * http://code.google.com/p/mimeparse/
 * 
 * Ported by Martin Blech <martinblech@gmail.com>.
 * 
 */
object MIMEParse {
  private case class ParseResults(_type: String, subType: String, q: Float,
      params: Map[String, String]) {
    def fit(other: ParseResults): FitnessAndQuality =
      if (!((_type == "*" || _type == other._type) &&
           (subType == "*" || subType == other.subType)))
        (-1, 0)
      else {
        val typeFitness = if (_type == other._type) 100 else 0
        val subTypeFitness = if (subType == other.subType) 10 else 0
        val paramFitness = (params.toSet & other.params.toSet).size
        val fitness = typeFitness + subTypeFitness + paramFitness
        (fitness, q)
      }
  }

  private val paramSeparator = "\\s*;\\s*".r
  private val typeSeparator = "\\s*/\\s*".r
  private val paramSplitter = "\\s*=\\s*".r
  private val rangeSeparator = "\\s*,\\s*".r

  private def parseMediaRange(mimeType: String): ParseResults = {
    val Array(rawFullType, rawParams @ _*) = paramSeparator.split(mimeType.trim)
    val fullType = if (rawFullType == "*") "*/*" else rawFullType
    val Array(_type, subType) = typeSeparator.split(fullType)
    val params = rawParams.map({ param =>
      val Array(k, v) = paramSplitter.split(param)
      (k, v)
    }).toMap
    val q = params.get("q").map(_.toFloat).getOrElse(1f)
    val fixedQ = if (q >=0 && q <=1) q else 1f
    ParseResults(_type, subType, fixedQ, params - "q")
  }

  private type FitnessAndQuality = (Int, Float)

  private def fitnessAndQuality(mimeType: String,
      parsedRanges: Seq[ParseResults]): FitnessAndQuality = {
    val target = parseMediaRange(mimeType)
    parsedRanges.map(_.fit(target)).sorted.last
  }

  private def parseHeader(header: String) =
    rangeSeparator.split(header.trim).map(parseMediaRange(_))

  /** Takes a list of supported mime-types and finds the best match for all the
   * media-ranges listed in header. The value of header must be a string that
   * conforms to the format of the HTTP Accept: header. The value of
   * 'supported' is a list of mime-types. */
  def bestMatch(supported: Seq[String], header: String): Option[String] = {
    val ranges = parseHeader(header)
    // pack f/q and mimeType, filter out q <= 0 and sort by decreasing f/q
    val sorted = supported.map({ mimeType =>
      (fitnessAndQuality(mimeType, ranges), mimeType)
    }).filter({ case ((_, q), _) => q > 0 })
      .sortBy({ case ((f, q), _) => (-f, -q) })
    // take the head and extract the mimeType
    sorted.firstOption.map({ case (_, mimeType) => mimeType })
  }
}
