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

package eu.cdevreeze.tqa.instancevalidation

import eu.cdevreeze.tqa.XmlFragmentKey.XmlFragmentKeyAware
import eu.cdevreeze.yaidom.queryapi.BackingElemApi
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.resolved.ResolvedNodes

/**
 * Typed dimension member, which on the one hand knows its ancestry, and on the other hand can be
 * compared for equality.
 *
 * @author Chris de Vreeze
 */
final class TypedDimensionMember(val backingElem: BackingElemApi) {

  override def equals(other: Any): Boolean = other match {
    case other: TypedDimensionMember =>
      makeResolvedElem(this.backingElem) == makeResolvedElem(other.backingElem)
    case _ =>
      false
  }

  override def hashCode: Int = makeResolvedElem(backingElem).hashCode

  private def makeResolvedElem(elem: BackingElemApi): resolved.Elem = {
    require(
      elem.isInstanceOf[ResolvedNodes.Elem],
      s"Cannot treat element ${elem.key} as 'ResolvedNodes.Elem'. This is a bug in the program.")

    // Best effort, but not completely safe as a basis to compare elements.

    resolved.Elem(elem.asInstanceOf[ResolvedNodes.Elem]).
      removeAllInterElementWhitespace.coalesceAndNormalizeAllText
  }
}
