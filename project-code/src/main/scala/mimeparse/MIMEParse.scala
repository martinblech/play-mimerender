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

  private val rangeSeparator = "\\s*,\\s*".r
  private val paramSeparator = "\\s*;\\s*".r
  private val typeSplitter = "\\s*(.*?)\\s*/\\s*(.*?)\\s*".r
  private val paramSplitter = "\\s*(.*?)\\s*=\\s*(.*?)\\s*".r

  private def parseMediaRange(mimeType: String): ParseResults = {
    val Array(rawFullType, rawParams @ _*) = paramSeparator.split(mimeType.trim)
    val fullType = if (rawFullType == "*") "*/*" else rawFullType
    val typeSplitter(_type, subType) = fullType
    val params = rawParams.map({ case paramSplitter(k, v) =>
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
    parsedRanges.map(_.fit(target)).max
  }

  private def parseHeader(header: String) =
    rangeSeparator.split(header.trim).map(parseMediaRange(_))

  /** Takes a list of supported mime-types and finds the best match for all the
   * media-ranges listed in header. The value of header must be a string that
   * conforms to the format of the HTTP Accept: header. The value of
   * 'supported' is a list of mime-types. */
  def bestMatch(supported: Seq[String], header: String): Option[String] = {
    val ranges = parseHeader(header)
    // pack f/q and mimeType
    supported.map({ mimeType =>
      (fitnessAndQuality(mimeType, ranges), mimeType)
      // filter out q >= 0 
    }).filter(_._1._2 > 0) match {
      case Nil => None
      // find max by f/q and extract the mimeType
      case candidates => Some(candidates.maxBy({ case (fq, _) => fq })._2)
    }
  }
}
