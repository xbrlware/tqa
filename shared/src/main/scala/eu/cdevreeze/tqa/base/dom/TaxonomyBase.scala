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

package eu.cdevreeze.tqa.base.dom

import java.net.URI

import scala.collection.immutable
import scala.reflect.classTag

import eu.cdevreeze.tqa.ENames.IdEName
import eu.cdevreeze.tqa.SubstitutionGroupMap
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.ElemApi.anyElem

/**
 * Very limited notion of a taxonomy, as a collection of taxonomy documents. It contains a map from URIs
 * (with fragments) to taxonomy documents, for quick element lookups based on URIs with fragments. It also contains
 * a map from ENames (names with target namespace) of global element declarations and named type definitions.
 *
 * It '''does not understand (resolved) relationships''', and it has no taxonomy query API, but it supports creation of such
 * a taxonomy that does know about relationships and does have a taxonomy query API. In that sense, the reason for this class to
 * exist is mainly its role in creating rich taxonomy objects.
 *
 * Not only does this class not understand (resolved) relationships, it also '''does not know about substitution
 * groups''' and therefore it does not know about concept declarations (unless all substitution groups are
 * in the taxonomy base and we are prepared to follow them all).
 *
 * This object is rather expensive to create (through the build method), building the maps that support fast querying based on URI
 * (with fragment) or "target EName".
 *
 * TaxonomyBase creation should never fail, if correct URIs are passed. Even the instance methods are very lenient and
 * should never fail. Typically, a taxonomy instantiated as an object of this class has not yet been validated.
 *
 * For the taxonomyDocUriMap and elemUriMap, we have that data is silently lost in those maps if there are any duplicate IDs (per document).
 * In a valid taxonomy (as XML document set) this duplication is not allowed.
 *
 * For the globalElementDeclarationMap, namedTypeDefinitionMap, etc., we also have that data is silently lost if there
 * is more than 1 global element declaration (or named type definition) with the same "target EName".
 * In a valid taxonomy (as XML schema) this duplication is not allowed.
 *
 * @author Chris de Vreeze
 */
final class TaxonomyBase private (
  val taxonomyDocs:                  immutable.IndexedSeq[TaxonomyDocument],
  val taxonomyDocUriMap:             Map[URI, TaxonomyDocument],
  val elemUriMap:                    Map[URI, TaxonomyElem],
  val globalElementDeclarationMap:   Map[EName, GlobalElementDeclaration],
  val namedTypeDefinitionMap:        Map[EName, NamedTypeDefinition],
  val globalAttributeDeclarationMap: Map[EName, GlobalAttributeDeclaration]) {

  require(
    taxonomyDocs.forall(_.uriOption.forall(_.getFragment == null)),
    s"Expected document URIs but got at least one URI with fragment")

  def rootElems: immutable.IndexedSeq[TaxonomyElem] = taxonomyDocs.map(_.documentElement)

  def rootElemUriMap: Map[URI, TaxonomyElem] = taxonomyDocUriMap.mapValues(_.documentElement)

  /**
   * Returns the SubstitutionGroupMap that can be derived from this taxonomy base alone.
   * This is an expensive operation that should be performed only once, if possible.
   */
  def computeDerivedSubstitutionGroupMap: SubstitutionGroupMap = {
    val rawMappings: Map[EName, EName] =
      (globalElementDeclarationMap.toSeq collect {
        case (en, decl) if decl.substitutionGroupOption.isDefined => (en -> decl.substitutionGroupOption.get)
      }).toMap

    val substGroups: Set[EName] = rawMappings.values.toSet

    val mappings: Map[EName, EName] = rawMappings.filterKeys(substGroups)

    SubstitutionGroupMap.from(mappings)
  }

  /**
   * Finds the (first) optional element with the given URI. The fragment, if any, must be an XPointer or sequence thereof.
   * Only shorthand pointers or non-empty sequences of element scheme XPointers are accepted. If there is no fragment,
   * the first root element with the given document URI is searched for.
   *
   * This is a quick operation for shorthand pointers, which are the most commonly used XPointers in URI fragments anyway.
   *
   * The schema type of the ID attributes is not taken into account, although strictly speaking that is incorrect.
   */
  def findElemByUri(elemUri: URI): Option[TaxonomyElem] = {
    require(elemUri.isAbsolute, s"URI '${elemUri}' is not absolute")

    if (elemUri.getFragment == null) {
      rootElemUriMap.get(elemUri)
    } else {
      val xpointers = XPointer.parseXPointers(elemUri.getFragment)

      xpointers match {
        case ShorthandPointer(_) :: Nil =>
          // Do a fast map lookup on the entire URI with fragment
          elemUriMap.get(elemUri)
        case _ =>
          val rootElemOption = rootElemUriMap.get(removeFragment(elemUri))
          rootElemOption.flatMap(e => XPointer.findElem(e, xpointers))
      }
    }
  }

  // Some finder methods for very frequently queried taxonomy elements.

  /**
   * Finds the (first) optional global element declaration with the given target EName (name with target namespace).
   *
   * This is a quick operation.
   */
  def findGlobalElementDeclarationByEName(targetEName: EName): Option[GlobalElementDeclaration] = {
    globalElementDeclarationMap.get(targetEName)
  }

  /**
   * Finds the (first) optional named type definition with the given target EName (name with target namespace).
   *
   * This is a quick operation.
   */
  def findNamedTypeDefinitionByEName(targetEName: EName): Option[NamedTypeDefinition] = {
    namedTypeDefinitionMap.get(targetEName)
  }

  /**
   * Finds the (first) optional global attribute declaration with the given target EName (name with target namespace).
   *
   * This is a quick operation.
   */
  def findGlobalAttributeDeclarationByEName(targetEName: EName): Option[GlobalAttributeDeclaration] = {
    globalAttributeDeclarationMap.get(targetEName)
  }

  /**
   * If the given type obeys the type predicate, returns it, wrapped in an Option.
   * Otherwise, returns the optional base type if that type obeys the type predicate, and so on,
   * until either the predicate holds or no further base type can be found in the taxonomy.
   */
  def findBaseTypeOrSelfUntil(typeEName: EName, p: EName => Boolean): Option[EName] = {
    if (p(typeEName)) {
      Some(typeEName)
    } else {
      val typeDefinitionOption = findNamedTypeDefinitionByEName(typeEName)

      val baseTypeOption = typeDefinitionOption.flatMap(_.baseTypeOption)

      // Recursive call
      baseTypeOption.flatMap(baseType => findBaseTypeOrSelfUntil(baseType, p))
    }
  }

  // Creating a "sub-taxonomy".

  /**
   * Creates a "sub-taxonomy" in which only the given document URIs occur.
   * It can be used for a specific entry point DTS, or to make query methods (not taking an EName) cheaper.
   */
  def filterDocumentUris(docUris: Set[URI]): TaxonomyBase = {
    new TaxonomyBase(
      taxonomyDocs.filter(d => docUris.contains(d.uri)),
      taxonomyDocUriMap.filterKeys(u => docUris.contains(removeFragment(u))),
      elemUriMap.filterKeys(u => docUris.contains(removeFragment(u))),
      globalElementDeclarationMap.filter(kv => docUris.contains(kv._2.docUri)),
      namedTypeDefinitionMap.filter(kv => docUris.contains(kv._2.docUri)),
      globalAttributeDeclarationMap.filter(kv => docUris.contains(kv._2.docUri)))
  }

  // Finding some "duplication errors".

  /**
   * Returns all duplicate ID attributes in the DOM tree with the given root element.
   * If the result is non-empty, the taxonomy is incorrect, and the map from URIs to elements loses data.
   *
   * The type of the ID attributes is not taken into account, although strictly speaking that is incorrect.
   */
  def findAllDuplicateIds(rootElem: TaxonomyElem): Set[String] = {
    val elemsWithId = rootElem.filterElemsOrSelf(_.attributeOption(IdEName).isDefined)
    val elemsGroupedById = elemsWithId.groupBy(_.attribute(IdEName))

    elemsGroupedById.filter(kv => kv._2.size >= 2).keySet
  }

  /**
   * Returns all duplicate global element declaration "target ENames" over all DOM trees combined.
   * If the result is non-empty, the taxonomy is incorrect, and the map from ENames to global element declarations loses data.
   */
  def findAllDuplicateGlobalElementDeclarationENames: Set[EName] = {
    val globalElementDeclarations =
      rootElems.flatMap(_.findAllElemsOrSelfOfType(classTag[GlobalElementDeclaration]))

    val globalElementDeclarationsGroupedByEName = globalElementDeclarations.groupBy(_.targetEName)

    globalElementDeclarationsGroupedByEName.filter(kv => kv._2.size >= 2).keySet
  }

  /**
   * Returns all duplicate named type definition "target ENames" over all DOM trees combined.
   * If the result is non-empty, the taxonomy is incorrect, and the map from ENames to named type definitions loses data.
   */
  def findAllDuplicateNamedTypeDefinitionENames: Set[EName] = {
    val namedTypeDefinitions =
      rootElems.flatMap(_.findAllElemsOrSelfOfType(classTag[NamedTypeDefinition]))

    val namedTypeDefinitionsGroupedByEName = namedTypeDefinitions.groupBy(_.targetEName)

    namedTypeDefinitionsGroupedByEName.filter(kv => kv._2.size >= 2).keySet
  }

  /**
   * Returns the "guessed Scope" from the documents in the taxonomy. This can be handy for finding
   * prefixes for namespace names, or for generating ENames from QNames.
   *
   * The resulting Scope is taken from the Scopes of the root elements, ignoring the default namespace,
   * if any. If different root element Scopes are conflicting, it is undetermined which one wins.
   */
  def guessedScope: Scope = {
    rootElems.map(_.scope.withoutDefaultNamespace).foldLeft(Scope.Empty) {
      case (accScope, currScope) =>
        (accScope ++ currScope).ensuring(_.retainingDefaultNamespace.isEmpty)
    }
  }

  private def removeFragment(uri: URI): URI = {
    new URI(uri.getScheme, uri.getSchemeSpecificPart, null)
  }
}

object TaxonomyBase {

  /**
   * Expensive build method (but the private constructor is cheap, and so are the Scala getters of the maps).
   *
   * It is the responsibility of the caller to pass different taxonomy documents.
   */
  def build(taxonomyDocs: immutable.IndexedSeq[TaxonomyDocument]): TaxonomyBase = {
    val taxonomyDocUriMap: Map[URI, TaxonomyDocument] = {
      taxonomyDocs.groupBy(_.uri).mapValues(_.head)
    }

    val rootElems = taxonomyDocs.map(_.documentElement)

    val elemUriMap: Map[URI, TaxonomyElem] = {
      rootElems.flatMap(e => getElemUriMap(e).toSeq).toMap
    }

    val globalElementDeclarationMap: Map[EName, GlobalElementDeclaration] = {
      rootElems.flatMap(e => getGlobalElementDeclarationMap(e).toSeq).toMap
    }

    val namedTypeDefinitionMap: Map[EName, NamedTypeDefinition] = {
      rootElems.flatMap(e => getNamedTypeDefinitionMap(e).toSeq).toMap
    }

    val globalAttributeDeclarationMap: Map[EName, GlobalAttributeDeclaration] = {
      rootElems.flatMap(e => getGlobalAttributeDeclarationMap(e).toSeq).toMap
    }

    new TaxonomyBase(taxonomyDocs, taxonomyDocUriMap, elemUriMap, globalElementDeclarationMap, namedTypeDefinitionMap, globalAttributeDeclarationMap)
  }

  /**
   * Returns `build(rootElems.map(e => TaxonomyDocument(None, e)))`.
   */
  def buildFromRootElems(rootElems: immutable.IndexedSeq[TaxonomyElem]): TaxonomyBase = {
    build(rootElems.map(e => TaxonomyDocument(None, e)))
  }

  private def getGlobalElementDeclarationMap(rootElem: TaxonomyElem): Map[EName, GlobalElementDeclaration] = {
    // TODO Speed up by finding the target namespace (per xs:schema) only once.
    val globalElementDeclarations = rootElem.findTopmostElemsOrSelfOfType(classTag[GlobalElementDeclaration])(anyElem)

    globalElementDeclarations.groupBy(_.targetEName).mapValues(_.head)
  }

  private def getNamedTypeDefinitionMap(rootElem: TaxonomyElem): Map[EName, NamedTypeDefinition] = {
    // TODO Speed up by finding the target namespace (per xs:schema) only once.
    val namedTypeDefinitions = rootElem.findTopmostElemsOrSelfOfType(classTag[NamedTypeDefinition])(anyElem)

    namedTypeDefinitions.groupBy(_.targetEName).mapValues(_.head)
  }

  private def getGlobalAttributeDeclarationMap(rootElem: TaxonomyElem): Map[EName, GlobalAttributeDeclaration] = {
    // TODO Speed up by finding the target namespace (per xs:schema) only once.
    val globalAttributeDeclarations = rootElem.findTopmostElemsOrSelfOfType(classTag[GlobalAttributeDeclaration])(anyElem)

    globalAttributeDeclarations.groupBy(_.targetEName).mapValues(_.head)
  }

  private def getElemUriMap(rootElem: TaxonomyElem): Map[URI, TaxonomyElem] = {
    val docUri = rootElem.docUri
    require(docUri.isAbsolute, s"Expected absolute URI but found '$docUri'")

    // The schema type of the ID attributes is not checked! That would be very expensive without any real advantage.

    val elemsWithId = rootElem.filterElemsOrSelf(_.attributeOption(IdEName).isDefined)
    elemsWithId.map(e => (makeUriWithIdFragment(e.baseUri, e.attribute(IdEName)) -> e)).toMap
  }

  private def makeUriWithIdFragment(baseUri: URI, idFragment: String): URI = {
    require(baseUri.isAbsolute, s"Expected absolute base URI but got '${baseUri}'")
    new URI(baseUri.getScheme, baseUri.getSchemeSpecificPart, idFragment)
  }
}
