/* NSC -- new Scala compiler
 * Copyright 2005-2010 LAMP/EPFL
 * @author Paul Phillips
 */

package scala.tools.nsc
package interpreter

import scala.util.NameTransformer

/** An interface for objects which are aware of tab completion and
 *  will supply their own candidates and resolve their own paths.
 */
trait CompletionAware {
  /** The complete list of unqualified Strings to which this
   *  object will complete.
   */
  def completions(): List[String]
  def completions(start: String): List[String] = completions filter (_ startsWith start)

  /** Default filter to apply to completions.
   */
  def filterNotFunction(s: String): Boolean = ReflectionCompletion shouldHide s

  /** Default sort.
   */
  def sortFunction(s1: String, s2: String): Boolean = s1 < s2

  /** Default map.
   */
  def mapFunction(s: String): String = s

  /** The next completor in the chain.
   */
  def follow(id: String): Option[CompletionAware] = None

  /** What to return if this completion is given as a command.  It
   *  returns None by default, which means to allow the repl to interpret
   *  the line normally.  Returning Some(_) means the line will never
   *  reach the scala interpreter.
   */
  def execute(id: String): Option[Any] = None

  /** Given string 'buf', return a list of all the strings
   *  to which it can complete.  This may involve delegating
   *  to other CompletionAware objects.
   */
  def completionsFor(buf: String): List[String] = {
    val parsed = new Parsed(buf)
    import parsed._

    (
      if (isEmpty) completions()
      else if (isFirstCharDot) Nil    // XXX for now
      else if (isUnqualified && !isLastCharDot) completions(buf)
      else follow(hd) match {
        case Some(next) => next completionsFor remainder
        case _          => Nil
      }
    ) filterNot filterNotFunction map mapFunction sortWith (sortFunction _)
  }

  def executionFor(buf: String): Option[Any] = {
    val parsed = new Parsed(buf)
    import parsed._

    if (isUnqualified && !isLastCharDot && (completions contains buf)) execute(buf)
    else if (!isQualified) None
    else follow(hd) match {
      case Some(next) => next executionFor remainder
      case _          => None
    }
  }
}

object CompletionAware {
  def unapply(that: Any): Option[CompletionAware] = that match {
    case x: CompletionAware => Some((x))
    case _                  => None
  }

  def apply(xs: List[String]) = new CompletionAware { val completions = xs }
}

