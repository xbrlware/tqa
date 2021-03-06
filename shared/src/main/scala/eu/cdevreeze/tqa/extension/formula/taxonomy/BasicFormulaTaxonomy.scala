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

package eu.cdevreeze.tqa.extension.formula.taxonomy

import scala.collection.immutable
import scala.reflect.classTag

import eu.cdevreeze.tqa.XmlFragmentKey
import eu.cdevreeze.tqa.base.dom.NonStandardArc
import eu.cdevreeze.tqa.base.dom.NonStandardResource
import eu.cdevreeze.tqa.base.dom.OtherNonXLinkElem
import eu.cdevreeze.tqa.base.queryapi.TaxonomyApi
import eu.cdevreeze.tqa.base.relationship.NonStandardRelationship
import eu.cdevreeze.tqa.extension.formula.dom.ExistenceAssertion
import eu.cdevreeze.tqa.extension.formula.dom.FormulaArc
import eu.cdevreeze.tqa.extension.formula.dom.FormulaResource
import eu.cdevreeze.tqa.extension.formula.dom.OtherFormulaElem
import eu.cdevreeze.tqa.extension.formula.dom.ValueAssertion
import eu.cdevreeze.tqa.extension.formula.dom.VariableSetAssertion
import eu.cdevreeze.tqa.extension.formula.dom.VariableSet
import eu.cdevreeze.tqa.extension.formula.queryapi.FormulaRelationshipContainerLike
import eu.cdevreeze.tqa.extension.formula.relationship.FormulaRelationship

/**
 * Basic implementation of a taxonomy that offers the FormulaRelationshipContainerApi query API, while wrapping
 * a taxonomy that offers the TaxonomyApi query API.
 *
 * @author Chris de Vreeze
 */
final class BasicFormulaTaxonomy private (
    val underlyingTaxonomy: TaxonomyApi,
    val formulaRelationships: immutable.IndexedSeq[FormulaRelationship],
    val formulaRelationshipsBySource: Map[XmlFragmentKey, immutable.IndexedSeq[FormulaRelationship]],
    val formulaArcs: immutable.IndexedSeq[FormulaArc],
    val formulaResources: immutable.IndexedSeq[FormulaResource],
    val otherFormulaElems: immutable.IndexedSeq[OtherFormulaElem]) extends FormulaRelationshipContainerLike {

  def findAllVariableSets: immutable.IndexedSeq[VariableSet] = {
    formulaResources collect { case vs: VariableSet => vs }
  }

  def findAllValueAssertions: immutable.IndexedSeq[ValueAssertion] = {
    findAllVariableSets collect { case vs: ValueAssertion => vs }
  }

  def findAllExistenceAssertions: immutable.IndexedSeq[ExistenceAssertion] = {
    findAllVariableSets collect { case vs: ExistenceAssertion => vs }
  }

  def findAllVariableSetAssertions: immutable.IndexedSeq[VariableSetAssertion] = {
    findAllVariableSets collect { case vs: VariableSetAssertion => vs }
  }
}

object BasicFormulaTaxonomy {

  /**
   * Expensive build method (but the private constructor is cheap, and so are the Scala getters of the maps).
   */
  def build(underlyingTaxonomy: TaxonomyApi): BasicFormulaTaxonomy = {
    val nonStandardRelationships = underlyingTaxonomy.findAllNonStandardRelationshipsOfType(classTag[NonStandardRelationship])
    val formulaRelationships = nonStandardRelationships.flatMap(rel => FormulaRelationship.opt(rel))

    val formulaRelationshipsBySource = formulaRelationships.groupBy(_.sourceElem.key)

    val rootElems = underlyingTaxonomy.rootElems

    val nonStandardArcs = rootElems.flatMap(_.findAllElemsOrSelfOfType(classTag[NonStandardArc]))
    val nonStandardResources = rootElems.flatMap(_.findAllElemsOrSelfOfType(classTag[NonStandardResource]))
    val otherElems = rootElems.flatMap(_.findAllElemsOrSelfOfType(classTag[OtherNonXLinkElem]))

    val formulaArcs = nonStandardArcs.flatMap(e => FormulaArc.opt(e))
    val formulaResources = nonStandardResources.flatMap(e => FormulaResource.opt(e))
    val otherFormulaElems = otherElems.flatMap(e => OtherFormulaElem.opt(e))

    new BasicFormulaTaxonomy(
      underlyingTaxonomy,
      formulaRelationships,
      formulaRelationshipsBySource,
      formulaArcs,
      formulaResources,
      otherFormulaElems)
  }
}
