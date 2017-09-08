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

import java.net.URI

import scala.collection.immutable
import scala.reflect.classTag

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.tqa.ENames.XbrldtDimensionItemEName
import eu.cdevreeze.tqa.ENames.XbrldtHypercubeItemEName
import eu.cdevreeze.tqa.ENames.XbrliItemEName
import eu.cdevreeze.tqa.SubstitutionGroupMap
import eu.cdevreeze.tqa.dom.TaxonomyBase
import eu.cdevreeze.tqa.dom.TaxonomyElem
import eu.cdevreeze.tqa.relationship.DefaultRelationshipFactory
import eu.cdevreeze.tqa.relationship.DimensionalRelationship
import eu.cdevreeze.tqa.relationship.HasHypercubeRelationship
import eu.cdevreeze.tqa.relationship.HypercubeDimensionRelationship
import eu.cdevreeze.tqa.relationship.PresentationRelationship
import eu.cdevreeze.tqa.taxonomy.BasicTaxonomy
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.parse.DocumentParserUsingStax

/**
 * Formula query API test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class FormulaQueryApiTest extends FunSuite {

  test("testQueryExistenceAssertions") {
    val docParser = DocumentParserUsingStax.newInstance()

    val docUris = Vector(
      classOf[FormulaQueryApiTest].getResource("/taxonomies/www.nltaxonomie.nl/nt11/kvk/20170419/validation/kvk-balance-sheet-banks-for.xml").toURI)

    val docs = docUris.map(uri => docParser.parse(uri).withUriOption(Some(uri)))

    val taxoRootElems = docs.map(d => TaxonomyElem.build(indexed.Document(d).documentElement))

    val taxoBase = TaxonomyBase.build(taxoRootElems)
    val taxo = BasicTaxonomy.build(taxoBase, SubstitutionGroupMap.Empty, DefaultRelationshipFactory.LenientInstance)

    val formulaTaxo = BasicFormulaTaxonomy.build(taxo)

    val guessedScope = taxo.guessedScope

    import guessedScope._

    assertResult(1) {
      formulaTaxo.findAllExistenceAssertions.size
    }

    val existenceAssertion = formulaTaxo.findAllExistenceAssertions.head

    assertResult(Some("ExistenceAssertion_BalanceSheetBanks_MsgAdimExistence1")) {
      existenceAssertion.underlyingResource.idOption
    }
    assertResult("dimensional") {
      existenceAssertion.aspectModel
    }
    assertResult(true) {
      existenceAssertion.implicitFiltering
    }

    assertResult(Vector(
      (QName("varArc_BalanceSheetBanks_MsgAdimExistence1_BalanceSheetBeforeAfterAppropriationResults").res, BigDecimal(1), 0))) {

      formulaTaxo.findAllOutgoingVariableSetRelationships(existenceAssertion).map(rel => (rel.name, rel.order, rel.priority))
    }

    val factVariables = formulaTaxo.findAllOutgoingVariableSetRelationships(existenceAssertion).map(_.variableOrParameter)
  }

  test("testQueryValueAssertions") {
    val docParser = DocumentParserUsingStax.newInstance()

    val docUris = Vector(
      classOf[FormulaQueryApiTest].getResource("/taxonomies/www.nltaxonomie.nl/nt11/kvk/20170419/validation/kvk-balance-sheet-banks-for.xml").toURI)

    val docs = docUris.map(uri => docParser.parse(uri).withUriOption(Some(uri)))

    val taxoRootElems = docs.map(d => TaxonomyElem.build(indexed.Document(d).documentElement))

    val taxoBase = TaxonomyBase.build(taxoRootElems)
    val taxo = BasicTaxonomy.build(taxoBase, SubstitutionGroupMap.Empty, DefaultRelationshipFactory.LenientInstance)

    val formulaTaxo = BasicFormulaTaxonomy.build(taxo)

    val guessedScope = taxo.guessedScope

    import guessedScope._

    assertResult(17) {
      formulaTaxo.findAllValueAssertions.size
    }
  }
}
