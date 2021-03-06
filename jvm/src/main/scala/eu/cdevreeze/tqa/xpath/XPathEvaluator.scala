/*
 * Copyright 2011-2017 Chris de Vreeze
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cdevreeze.tqa.xpath

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.BackingElemApi

/**
 * A very simple XPath evaluator abstraction. It has no knowledge about static and dynamic contexts (other than the
 * optional context item), etc. It also has no knowledge about specific implementations, such as Saxon. Moreover,
 * it has no knowledge about XPath versions.
 *
 * An XPath evaluator is needed as context when querying formula and table link content where XPath expressions are used.
 *
 * This trait looks a bit like the JAXP `XPath` interface. Like the `XPath` interface, this trait does not support the
 * XDM data types that succeeded XPath 1.0. In contrast to the JAXP `XPath` interface, this trait is more Scala-esque and type-safe.
 *
 * @author Chris de Vreeze
 */
trait XPathEvaluator {

  /**
   * XPath expression. Typically (but not necessarily) a "compiled" one.
   */
  type XPathExpression

  /**
   * The DOM node type in (DOM) evaluation results.
   */
  type Node

  /**
   * The context item type.
   */
  type ContextItem

  // TODO Simple XDM model, and evaluation methods that return XDM items.

  def evaluateAsString(expr: XPathExpression, contextItemOption: Option[ContextItem]): String

  def evaluateAsNode(expr: XPathExpression, contextItemOption: Option[ContextItem]): Node

  def evaluateAsNodeSeq(expr: XPathExpression, contextItemOption: Option[ContextItem]): immutable.IndexedSeq[Node]

  def evaluateAsBackingElem(expr: XPathExpression, contextItemOption: Option[ContextItem]): BackingElemApi

  def evaluateAsBackingElemSeq(expr: XPathExpression, contextItemOption: Option[ContextItem]): immutable.IndexedSeq[BackingElemApi]

  def evaluateAsBigDecimal(expr: XPathExpression, contextItemOption: Option[ContextItem]): BigDecimal

  def evaluateAsBoolean(expr: XPathExpression, contextItemOption: Option[ContextItem]): Boolean

  def evaluateAsEName(expr: XPathExpression, contextItemOption: Option[ContextItem]): EName

  /**
   * Creates an XPathExpression from the given expression string. Typically (but not necessarily) "compiles" the XPath string.
   * Make sure to pass only XPath strings for which all needed namespace bindings are known to the XPath evaluator.
   */
  def toXPathExpression(xPathString: String): XPathExpression

  def toString(expr: XPathExpression): String

  /**
   * Returns the namespace context as yaidom Scope
   */
  def scope: Scope
}

object XPathEvaluator {

  type Aux[E, N, C] = XPathEvaluator {
    type XPathExpression = E
    type Node = N
    type ContextItem = C
  }
}
