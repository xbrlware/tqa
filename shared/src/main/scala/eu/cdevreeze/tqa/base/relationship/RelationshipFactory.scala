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

package eu.cdevreeze.tqa.base.relationship

import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.tqa.ENames
import eu.cdevreeze.tqa.base.dom.BaseSetKey
import eu.cdevreeze.tqa.base.dom.ExtendedLink
import eu.cdevreeze.tqa.base.dom.LabeledXLink
import eu.cdevreeze.tqa.base.dom.TaxonomyBase
import eu.cdevreeze.tqa.base.dom.XLinkArc

/**
 * Extractor of relationships from a "taxonomy base".
 *
 * @author Chris de Vreeze
 */
trait RelationshipFactory {

  /**
   * The configuration used by this RelationshipFactory.
   */
  def config: RelationshipFactory.Config

  /**
   * Returns all relationships in the given `TaxonomyBase` passing the provided arc filter.
   */
  def extractRelationships(
    taxonomyBase: TaxonomyBase,
    arcFilter: XLinkArc => Boolean): immutable.IndexedSeq[Relationship]

  /**
   * Returns all relationships in the given document in the given `TaxonomyBase` passing the provided arc filter.
   */
  def extractRelationshipsFromDocument(
    docUri: URI,
    taxonomyBase: TaxonomyBase,
    arcFilter: XLinkArc => Boolean): immutable.IndexedSeq[Relationship]

  /**
   * Returns all relationships in the given extended link in the given `TaxonomyBase` passing the provided arc filter.
   */
  def extractRelationshipsFromExtendedLink(
    extendedLink: ExtendedLink,
    taxonomyBase: TaxonomyBase,
    arcFilter: XLinkArc => Boolean): immutable.IndexedSeq[Relationship]

  /**
   * Returns all relationships (typically one) having the given underlying XLink arc in the given `TaxonomyBase`.
   * For performance a mapping from XLink labels to XLink locators and resources must be provided, and this mapping
   * should be computed only once per extended link.
   *
   * This method must respect the configuration of this RelationshipFactory.
   */
  def extractRelationshipsFromArc(
    arc: XLinkArc,
    labeledXlinkMap: Map[String, immutable.IndexedSeq[LabeledXLink]],
    taxonomyBase: TaxonomyBase): immutable.IndexedSeq[Relationship]

  /**
   * Returns the networks of relationships, computed from the given collection of relationships. The `TaxonomyBase`
   * is passed as second parameter.
   */
  def computeNetworks(
    relationships: immutable.IndexedSeq[Relationship],
    taxonomyBase: TaxonomyBase): Map[BaseSetKey, RelationshipFactory.NetworkComputationResult]

  /**
   * Gets the key of the relationship that is the same (only) for equivalent relationships.
   */
  def getRelationshipKey(relationship: Relationship, taxonomyBase: TaxonomyBase): RelationshipKey
}

object RelationshipFactory {

  /**
   * Arc filter that returns true for each arc.
   */
  val AnyArc: XLinkArc => Boolean = (_ => true)

  /**
   * Arc filter that returns false if the XLink arcrole attribute is missing, and true otherwise.
   * Note that it is an error for an XLink arc (in XBRL) not to have an XLink arcrole attribute.
   */
  val AnyArcHavingArcrole: XLinkArc => Boolean = (_.attributeOption(ENames.XLinkArcroleEName).nonEmpty)

  final case class NetworkComputationResult(
    val retainedRelationships: immutable.IndexedSeq[Relationship],
    val removedRelationships: immutable.IndexedSeq[Relationship])

  /**
   * Configuration object used by a RelationshipFactory. It says to what extent the RelationshipFactory
   * using it allows syntax errors in XLink arcs and locators, as well as "dead" locator href URIs.
   *
   * Handling missing XLink arcrole attributes of arcs cannot be configured. This can be handled through
   * the use of an appropriate arc filter, though.
   *
   * @constructor create a new configuration from the passed boolean flags
   * @param allowSyntaxError if true, allows syntax errors such as XPointer syntax errors or missing arcrole attributes
   * @param allowUnresolvedXLinkLabel if true, allows "dead" arc XLink "from" or "to" attributes within any extended link
   * @param allowUnresolvedLocator if true, allows "dead" locator href URIs within the taxonomy
   */
  final case class Config(
      val allowSyntaxError: Boolean,
      val allowUnresolvedXLinkLabel: Boolean,
      val allowUnresolvedLocator: Boolean) {

    /**
     * Returns true if XPointer syntax errors in any locator href URI fragment are allowed
     */
    def allowWrongXPointer: Boolean = allowSyntaxError

    /**
     * Returns true if missing arcrole attributes in arcs are allowed
     */
    def allowMissingArcrole: Boolean = allowSyntaxError
  }

  object Config {

    /**
     * Accepts unresolved locators, XPointer syntax errors and broken XLink labels (in XLink arcs).
     * Such erroneous locators and arcs are silently skipped.
     */
    val VeryLenient = Config(true, true, true)

    /**
     * Accepts unresolved locators but does not accept any (found) XPointer syntax errors or broken XLink labels.
     * Such unresolved locators are silently skipped.
     */
    val Lenient = Config(false, false, true)

    /**
     * Does not accept any unresolved locators or syntax errors (in XPointer) or broken XLink labels.
     * Exceptions will be thrown instead.
     */
    val Strict = Config(false, false, false)
  }
}
