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

package eu.cdevreeze.tqa.base.taxonomy

import java.net.URI

import scala.collection.immutable
import scala.reflect.ClassTag
import scala.reflect.classTag

import eu.cdevreeze.tqa.SubstitutionGroupMap
import eu.cdevreeze.tqa.XmlFragmentKey
import eu.cdevreeze.tqa.base.dom.ConceptDeclaration
import eu.cdevreeze.tqa.base.dom.GlobalAttributeDeclaration
import eu.cdevreeze.tqa.base.dom.GlobalElementDeclaration
import eu.cdevreeze.tqa.base.dom.NamedTypeDefinition
import eu.cdevreeze.tqa.base.dom.TaxonomyBase
import eu.cdevreeze.tqa.base.dom.TaxonomyDocument
import eu.cdevreeze.tqa.base.dom.TaxonomyElem
import eu.cdevreeze.tqa.base.dom.XLinkArc
import eu.cdevreeze.tqa.base.dom.XsdSchema
import eu.cdevreeze.tqa.base.queryapi.TaxonomyLike
import eu.cdevreeze.tqa.base.relationship.InterConceptRelationship
import eu.cdevreeze.tqa.base.relationship.NonStandardRelationship
import eu.cdevreeze.tqa.base.relationship.Relationship
import eu.cdevreeze.tqa.base.relationship.RelationshipFactory
import eu.cdevreeze.tqa.base.relationship.StandardRelationship
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.ElemApi.anyElem

/**
 * Basic implementation of a taxonomy that offers the TaxonomyApi query API. It does not enforce closure
 * under DTS discovery rules, or uniqueness of "target expanded names" of concept declarations etc.
 * It does not know anything about tables and formulas. It also does not know anything about networks
 * of relationships.
 *
 * The passed relationships must be backed by XLink arcs in the underlying taxonomy, or else the
 * instance is corrupt. This is not checked by this class.
 *
 * This object is expensive to create (through the build method), primarily due to the mappings from source
 * and target concepts to standard relationships. Looking up schema content by EName (or by URI for global
 * element declarations) is also fast.
 *
 * @author Chris de Vreeze
 */
final class BasicTaxonomy private (
  val taxonomyBase:                      TaxonomyBase,
  val extraSubstitutionGroupMap:         SubstitutionGroupMap,
  val netSubstitutionGroupMap:           SubstitutionGroupMap,
  val relationships:                     immutable.IndexedSeq[Relationship],
  val conceptDeclarationsByEName:        Map[EName, ConceptDeclaration],
  val standardRelationshipsBySource:     Map[EName, immutable.IndexedSeq[StandardRelationship]],
  val nonStandardRelationshipsBySource:  Map[XmlFragmentKey, immutable.IndexedSeq[NonStandardRelationship]],
  val interConceptRelationshipsBySource: Map[EName, immutable.IndexedSeq[InterConceptRelationship]],
  val interConceptRelationshipsByTarget: Map[EName, immutable.IndexedSeq[InterConceptRelationship]]) extends TaxonomyLike {

  def taxonomyDocs: immutable.IndexedSeq[TaxonomyDocument] = taxonomyBase.taxonomyDocs

  def rootElems: immutable.IndexedSeq[TaxonomyElem] = taxonomyBase.rootElems

  def substitutionGroupMap: SubstitutionGroupMap = netSubstitutionGroupMap

  def getRootElem(elem: TaxonomyElem): TaxonomyElem = {
    val docUri = elem.docUri
    val rootElem =
      taxonomyBase.rootElemUriMap.getOrElse(docUri, sys.error(s"Missing root elem for document URI $docUri"))
    rootElem
  }

  def findAllXsdSchemas: immutable.IndexedSeq[XsdSchema] = {
    taxonomyBase.rootElems.flatMap(_.findTopmostElemsOrSelfOfType(classTag[XsdSchema])(anyElem))
  }

  def findAllGlobalElementDeclarations: immutable.IndexedSeq[GlobalElementDeclaration] = {
    taxonomyBase.rootElems.flatMap(_.findTopmostElemsOrSelfOfType(classTag[GlobalElementDeclaration])(anyElem))
  }

  def findGlobalElementDeclaration(ename: EName): Option[GlobalElementDeclaration] = {
    taxonomyBase.findGlobalElementDeclarationByEName(ename)
  }

  def findGlobalElementDeclarationByUri(uri: URI): Option[GlobalElementDeclaration] = {
    taxonomyBase.findElemByUri(uri) collectFirst { case decl: GlobalElementDeclaration => decl }
  }

  def findAllGlobalAttributeDeclarations: immutable.IndexedSeq[GlobalAttributeDeclaration] = {
    taxonomyBase.rootElems.flatMap(_.findTopmostElemsOrSelfOfType(classTag[GlobalAttributeDeclaration])(anyElem))
  }

  def findGlobalAttributeDeclaration(ename: EName): Option[GlobalAttributeDeclaration] = {
    taxonomyBase.findGlobalAttributeDeclarationByEName(ename)
  }

  def findAllNamedTypeDefinitions: immutable.IndexedSeq[NamedTypeDefinition] = {
    taxonomyBase.rootElems.flatMap(_.findTopmostElemsOrSelfOfType(classTag[NamedTypeDefinition])(anyElem))
  }

  def findNamedTypeDefinition(ename: EName): Option[NamedTypeDefinition] = {
    taxonomyBase.findNamedTypeDefinitionByEName(ename)
  }

  def findBaseTypeOrSelfUntil(typeEName: EName, p: EName => Boolean): Option[EName] = {
    taxonomyBase.findBaseTypeOrSelfUntil(typeEName, p)
  }

  def findConceptDeclaration(ename: EName): Option[ConceptDeclaration] = {
    conceptDeclarationsByEName.get(ename)
  }

  def findAllStandardRelationshipsOfType[A <: StandardRelationship](
    relationshipType: ClassTag[A]): immutable.IndexedSeq[A] = {

    implicit val clsTag = relationshipType
    relationships collect { case rel: A => rel }
  }

  def findAllNonStandardRelationshipsOfType[A <: NonStandardRelationship](
    relationshipType: ClassTag[A]): immutable.IndexedSeq[A] = {

    implicit val clsTag = relationshipType
    relationships collect { case rel: A => rel }
  }

  def findAllInterConceptRelationshipsOfType[A <: InterConceptRelationship](
    relationshipType: ClassTag[A]): immutable.IndexedSeq[A] = {

    implicit val clsTag = relationshipType
    relationships collect { case rel: A => rel }
  }

  /**
   * Creates a "sub-taxonomy" in which only the given document URIs occur.
   * It can be used for a specific entry point DTS, or to make query methods (not taking an EName) cheaper.
   * In order to keep the same net substitution groups, they are passed as the extra substitution groups
   * to the subset BasicTaxonomy.
   */
  def filterDocumentUris(docUris: Set[URI]): BasicTaxonomy = {
    new BasicTaxonomy(
      taxonomyBase.filterDocumentUris(docUris),
      netSubstitutionGroupMap,
      netSubstitutionGroupMap,
      relationships.filter(rel => docUris.contains(rel.docUri)),
      conceptDeclarationsByEName.filter(kv => docUris.contains(kv._2.globalElementDeclaration.docUri)),
      standardRelationshipsBySource.mapValues(_.filter(rel => docUris.contains(rel.docUri))).filter(_._2.nonEmpty),
      nonStandardRelationshipsBySource.mapValues(_.filter(rel => docUris.contains(rel.docUri))).filter(_._2.nonEmpty),
      interConceptRelationshipsBySource.mapValues(_.filter(rel => docUris.contains(rel.docUri))).filter(_._2.nonEmpty),
      interConceptRelationshipsByTarget.mapValues(_.filter(rel => docUris.contains(rel.docUri))).filter(_._2.nonEmpty))
  }

  /**
   * Creates a "sub-taxonomy" in which only relationships passing the filter occur.
   * Schema and linkbase DOM content remains the same. Only relationships are filtered.
   * It can be used to make query methods (not taking an EName) cheaper.
   */
  def filterRelationships(p: Relationship => Boolean): BasicTaxonomy = {
    new BasicTaxonomy(
      taxonomyBase,
      extraSubstitutionGroupMap,
      netSubstitutionGroupMap,
      relationships.filter(p),
      conceptDeclarationsByEName,
      standardRelationshipsBySource.mapValues(_.filter(p)).filter(_._2.nonEmpty),
      nonStandardRelationshipsBySource.mapValues(_.filter(p)).filter(_._2.nonEmpty),
      interConceptRelationshipsBySource.mapValues(_.filter(p)).filter(_._2.nonEmpty),
      interConceptRelationshipsByTarget.mapValues(_.filter(p)).filter(_._2.nonEmpty))
  }

  /**
   * Returns the "guessed Scope" from the documents in the taxonomy. This can be handy for finding
   * prefixes for namespace names, or for generating ENames from QNames.
   *
   * The resulting Scope is taken from the Scopes of the root elements, ignoring the default namespace,
   * if any. If different root element Scopes are conflicting, it is undetermined which one wins.
   */
  def guessedScope: Scope = taxonomyBase.guessedScope

  /**
   * Returns the effective taxonomy, after resolving prohibition and overriding.
   */
  def resolveProhibitionAndOverriding(relationshipFactory: RelationshipFactory): BasicTaxonomy = {
    val baseSetNetworkComputationMap =
      relationshipFactory.computeNetworks(relationships, taxonomyBase)

    // Relationships are bad Set members or Map keys, but within this local method scope they
    // should cause no problem. Moreover, typically at most a few relationships are removed
    // by network computation (by prohibition/overriding resolution), so the set membership tests
    // should be relatively efficient.

    val removedRelationships: Set[Relationship] =
      baseSetNetworkComputationMap.values.flatMap(_.removedRelationships).toSet

    def acceptRelationship(rel: Relationship): Boolean = {
      !removedRelationships.contains(rel)
    }

    val outputTaxo = filterRelationships(acceptRelationship)
    outputTaxo
  }
}

object BasicTaxonomy {

  /**
   * Expensive build method (but the private constructor is cheap, and so are the Scala getters of the maps).
   * This method invokes the overloaded build method having as 4th parameter the arc filter that always returns true.
   */
  def build(
    taxonomyBase:              TaxonomyBase,
    extraSubstitutionGroupMap: SubstitutionGroupMap,
    relationshipFactory:       RelationshipFactory): BasicTaxonomy = {

    build(taxonomyBase, extraSubstitutionGroupMap, relationshipFactory, _ => true)
  }

  /**
   * Expensive build method (but the private constructor is cheap, and so are the Scala getters of the maps).
   * This method first extracts relationships from the underlying taxonomy, and then calls the overloaded
   * build method that takes as parameters the underlying taxonomy base, extra substitution group map, and extracted
   * relationships.
   *
   * The arc filter is only used during relationship extraction. It is not used to filter any taxonomy DOM content.
   */
  def build(
    taxonomyBase:              TaxonomyBase,
    extraSubstitutionGroupMap: SubstitutionGroupMap,
    relationshipFactory:       RelationshipFactory,
    arcFilter:                 XLinkArc => Boolean): BasicTaxonomy = {

    val relationships = relationshipFactory.extractRelationships(taxonomyBase, arcFilter)

    build(taxonomyBase, extraSubstitutionGroupMap, relationships)
  }

  /**
   * Expensive build method (but the private constructor is cheap, and so are the Scala getters of the maps).
   * Make sure that the relationships are backed by arcs in the underlying taxonomy. This is not checked.
   */
  def build(
    taxonomyBase:              TaxonomyBase,
    extraSubstitutionGroupMap: SubstitutionGroupMap,
    relationships:             immutable.IndexedSeq[Relationship]): BasicTaxonomy = {

    val netSubstitutionGroupMap =
      taxonomyBase.computeDerivedSubstitutionGroupMap.append(extraSubstitutionGroupMap)

    val conceptDeclarationBuilder = new ConceptDeclaration.Builder(netSubstitutionGroupMap)

    val conceptDeclarationsByEName: Map[EName, ConceptDeclaration] = {
      (taxonomyBase.globalElementDeclarationMap.toSeq collect {
        case (ename, decl) if conceptDeclarationBuilder.optConceptDeclaration(decl).isDefined =>
          (ename -> conceptDeclarationBuilder.optConceptDeclaration(decl).get)
      }).toMap
    }

    val standardRelationships = relationships collect { case rel: StandardRelationship => rel }

    val standardRelationshipsBySource: Map[EName, immutable.IndexedSeq[StandardRelationship]] = {
      standardRelationships groupBy (_.sourceConceptEName)
    }

    val nonStandardRelationships = relationships collect { case rel: NonStandardRelationship => rel }

    val nonStandardRelationshipsBySource: Map[XmlFragmentKey, immutable.IndexedSeq[NonStandardRelationship]] = {
      nonStandardRelationships groupBy (_.sourceElem.key)
    }

    val interConceptRelationships = standardRelationships collect { case rel: InterConceptRelationship => rel }

    val interConceptRelationshipsBySource: Map[EName, immutable.IndexedSeq[InterConceptRelationship]] = {
      interConceptRelationships groupBy (_.sourceConceptEName)
    }

    val interConceptRelationshipsByTarget: Map[EName, immutable.IndexedSeq[InterConceptRelationship]] = {
      interConceptRelationships groupBy (_.targetConceptEName)
    }

    new BasicTaxonomy(
      taxonomyBase,
      extraSubstitutionGroupMap,
      netSubstitutionGroupMap,
      relationships,
      conceptDeclarationsByEName,
      standardRelationshipsBySource,
      nonStandardRelationshipsBySource,
      interConceptRelationshipsBySource,
      interConceptRelationshipsByTarget)
  }
}
