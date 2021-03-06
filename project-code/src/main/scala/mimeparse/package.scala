/**
 * MIME-Type Parser
 * 
 * This package provides basic functions for handling mime-types. It can handle
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
package object mimeparse {

  case class ParseException(msg: String, cause: Throwable)
    extends IllegalArgumentException(msg, cause)

  private type FitnessAndQuality = (Int, Float)

  private case class ParseResults(fullType: String, _type: String,
      subType: String, q: Float, params: Map[String, String]) {
    def fit(other: ParseResults): FitnessAndQuality =
      if (!((_type == "*" || _type == other._type) &&
           (subType == "*" || subType == other.subType)))
        // type and subtype don't match, f=-1, q=0
        (-1, 0)
      else {
        val typeFitness = if (_type == other._type) 100 else 0
        val subTypeFitness = if (subType == other.subType) 10 else 0
        val paramFitness = (params.toSet & other.params.toSet).size
        val fitness = typeFitness + subTypeFitness + paramFitness
        (fitness, q)
      }
  }

  private val rangeSeparator = "\\s*,\\s*".r
  private val paramSeparator = "\\s*;\\s*".r
  private val typeSplitter = "\\s*(.*?)\\s*/\\s*(.*?)\\s*".r
  private val paramSplitter = "\\s*(.*?)\\s*=\\s*(.*?)\\s*".r

  private def parseMediaRange(mimeType: String): ParseResults = {
    try {
      val trimmed = mimeType.trim
      // split into rawFullType and rawParams using ';' as separator
      val Array(rawFullType, rawParams @ _*) = paramSeparator.split(trimmed)
      // expand * to */*
      val fullType = if (rawFullType == "*") "*/*" else rawFullType
      // split fullType into type and subType using '/' as separator
      val typeSplitter(_type, subType) = fullType
      // build params Map
      val params = rawParams.map({ case paramSplitter(k, v) => (k, v) }).toMap
      // extract q from map and fallback to 1 if outside range
      val q = { 
        val q = params.getOrElse("q", "1").toFloat
        if (q >=0 && q <=1) q else 1f
      }
      ParseResults(trimmed, _type, subType, q, params - "q")
    } catch {
      case e: Throwable => throw new ParseException(mimeType, e)
    }
  }

  private def fitnessAndQuality(mimeType: ParseResults,
      parsedRanges: Seq[ParseResults]): FitnessAndQuality =
    // find the max fit for this mime type among the parsed ranges
    parsedRanges.map(_.fit(mimeType)).max

  private def parseHeader(header: String) =
    // trim the header, split with with ',' as separator and parse each range
    rangeSeparator.split(header.trim).map(parseMediaRange(_))

  private def bestMatchParsed(supported: Seq[ParseResults],
      header: String): Option[String] = {
    val ranges = parseHeader(header)
    // pack f/q and mimeType
    supported.map({ range =>
      (fitnessAndQuality(range, ranges), range.fullType)
      // filter out q <= 0 
    }).filter({ case ((_, q), _) => q > 0 }) match {
      case Nil => None
      // find max by f/q and extract the mimeType
      case candidates => Some(candidates.maxBy({ case (fq, _) => fq })._2)
    }
  }

  private def parseSupported(supported: Seq[String]) =
    // parse each supported mime type as a media range
    supported.map(parseMediaRange(_))

  /** Takes a list of supported mime-types and finds the best match for all the
   * media-ranges listed in header. The value of header must be a string that
   * conforms to the format of the HTTP Accept: header. The value of
   * 'supported' is a list of mime-types. */
  def bestMatch(supported: Seq[String], header: String): Option[String] = {
    // parse supported mime types
    val parsedSupported = parseSupported(supported)
    // find the best match
    bestMatchParsed(parsedSupported, header)
  }

  /** Holds a list of supported mime types and is able to find the best match
   * without parsing everything every time. */
  class Matcher(supported: Seq[String]) {
    private val parsedSupported = parseSupported(supported)
    def apply(header: String) = bestMatchParsed(parsedSupported, header)
  }

}
