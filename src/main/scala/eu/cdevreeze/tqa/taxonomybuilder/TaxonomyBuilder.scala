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

package eu.cdevreeze.tqa.taxonomybuilder

import eu.cdevreeze.tqa.SubstitutionGroupMap
import eu.cdevreeze.tqa.backingelem.DocumentBuilder
import eu.cdevreeze.tqa.dom.TaxonomyBase
import eu.cdevreeze.tqa.dom.XLinkArc
import eu.cdevreeze.tqa.relationship.DefaultRelationshipFactory
import eu.cdevreeze.tqa.relationship.RelationshipFactory
import eu.cdevreeze.tqa.taxonomy.BasicTaxonomy

/**
 * Fluent interface for bootstrapping a taxonomy. The type system helps in getting bootstrapping right,
 * if we start with the TaxonomyBuilder companion object.
 *
 * @author Chris de Vreeze
 */
final class TaxonomyBuilder(
    val documentBuilder: DocumentBuilder,
    val documentCollector: DocumentCollector,
    val substitutionGroupMap: SubstitutionGroupMap,
    val relationshipFactory: RelationshipFactory,
    val arcFilter: XLinkArc => Boolean) {

  def withSubstitutionGroupMap(newSubstitutionGroupMap: SubstitutionGroupMap): TaxonomyBuilder = {
    new TaxonomyBuilder(documentBuilder, documentCollector, newSubstitutionGroupMap, relationshipFactory, arcFilter)
  }

  def withArcFilter(newArcFilter: XLinkArc => Boolean): TaxonomyBuilder = {
    new TaxonomyBuilder(documentBuilder, documentCollector, substitutionGroupMap, relationshipFactory, newArcFilter)
  }

  def build(): BasicTaxonomy = {
    val taxoRootElems = documentCollector.collectTaxonomyRootElems(documentBuilder)

    val taxonomyBase = TaxonomyBase.build(taxoRootElems)

    BasicTaxonomy.build(taxonomyBase, substitutionGroupMap, relationshipFactory, arcFilter)
  }
}

object TaxonomyBuilder {

  def withDocumentBuilder(documentBuilder: DocumentBuilder): HasDocumentBuilder = {
    new HasDocumentBuilder(documentBuilder)
  }

  final class HasDocumentBuilder(val documentBuilder: DocumentBuilder) {

    def withDocumentCollector(documentCollector: DocumentCollector): HasDocumentCollector = {
      new HasDocumentCollector(documentBuilder, documentCollector)
    }
  }

  final class HasDocumentCollector(
      val documentBuilder: DocumentBuilder,
      val documentCollector: DocumentCollector) {

    // TODO We miss caching of root elements now. DTS discovery may repeatedly parse the same documents.

    def withRelationshipFactory(relationshipFactory: RelationshipFactory): TaxonomyBuilder = {
      new TaxonomyBuilder(documentBuilder, documentCollector, SubstitutionGroupMap.Empty, relationshipFactory, (_ => true))
    }

    def withStrictRelationshipFactory: TaxonomyBuilder = {
      withRelationshipFactory(DefaultRelationshipFactory.StrictInstance)
    }

    def withLenientRelationshipFactory: TaxonomyBuilder = {
      withRelationshipFactory(DefaultRelationshipFactory.LenientInstance)
    }
  }
}