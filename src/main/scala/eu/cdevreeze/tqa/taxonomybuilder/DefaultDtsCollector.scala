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

import java.net.URI

import scala.reflect.classTag

import eu.cdevreeze.tqa.ENames.SchemaLocationEName
import eu.cdevreeze.tqa.dom.ArcroleRef
import eu.cdevreeze.tqa.dom.Include
import eu.cdevreeze.tqa.dom.Linkbase
import eu.cdevreeze.tqa.dom.LinkbaseRef
import eu.cdevreeze.tqa.dom.RoleRef
import eu.cdevreeze.tqa.dom.StandardLoc
import eu.cdevreeze.tqa.dom.TaxonomyElem
import eu.cdevreeze.tqa.dom.TaxonomyRootElem
import eu.cdevreeze.tqa.dom.XsdSchema

/**
 * Default DTS discovery implementation. It will fail for all found URIs that cannot be resolved to
 * taxonomy documents.
 *
 * @author Chris de Vreeze
 */
final class DefaultDtsCollector(
    entrypointUris: Set[URI]) extends AbstractDtsCollector(entrypointUris) {

  def findAllUsedDocUris(rootElem: TaxonomyRootElem): Set[URI] = {
    rootElem match {
      case xsdSchema: XsdSchema =>
        // Minding embedded linkbases

        findAllUsedDocUrisInXsdSchema(xsdSchema) union {
          xsdSchema.findAllElemsOfType(classTag[Linkbase]).flatMap(lb => findAllUsedDocUrisInLinkbase(lb)).toSet
        }
      case linkbase: Linkbase =>
        findAllUsedDocUrisInLinkbase(linkbase)
    }
  }

  def findAllUsedDocUrisInXsdSchema(rootElem: XsdSchema): Set[URI] = {
    // Using the base URI instead of document URI for xs:import and xs:include (although XML Schema knows nothing about XML Base)

    val imports = rootElem.findAllImports
    val includes = rootElem.findAllElemsOfType(classTag[Include])
    val linkbaseRefs = rootElem.findAllElemsOfType(classTag[LinkbaseRef])

    val importUris =
      imports.flatMap(e => (e \@ SchemaLocationEName).map(u => makeAbsoluteWithoutFragment(URI.create(u), e)))
    val includeUris =
      includes.flatMap(e => (e \@ SchemaLocationEName).map(u => makeAbsoluteWithoutFragment(URI.create(u), e)))
    val linkbaseRefUris =
      linkbaseRefs.filter(e => e.rawHref != EmptyUri).map(e => makeAbsoluteWithoutFragment(e.rawHref, e))

    (importUris ++ includeUris ++ linkbaseRefUris).toSet.diff(Set(rootElem.docUri))
  }

  def findAllUsedDocUrisInLinkbase(rootElem: Linkbase): Set[URI] = {
    // Only link:loc locators are used in DTS discovery.

    val locs = rootElem.findAllElemsOfType(classTag[StandardLoc])
    val roleRefs = rootElem.findAllElemsOfType(classTag[RoleRef])
    val arcroleRefs = rootElem.findAllElemsOfType(classTag[ArcroleRef])

    val locUris =
      locs.filter(e => e.rawHref != EmptyUri).map(e => makeAbsoluteWithoutFragment(e.rawHref, e))
    val roleRefUris =
      roleRefs.filter(e => e.rawHref != EmptyUri).map(e => makeAbsoluteWithoutFragment(e.rawHref, e))
    val arcroleRefUris =
      arcroleRefs.filter(e => e.rawHref != EmptyUri).map(e => makeAbsoluteWithoutFragment(e.rawHref, e))

    (locUris ++ roleRefUris ++ arcroleRefUris).toSet.diff(Set(rootElem.docUri))
  }

  private def makeAbsoluteWithoutFragment(uri: URI, elem: TaxonomyElem): URI = {
    removeFragment(elem.baseUri.resolve(uri))
  }

  private def removeFragment(uri: URI): URI = {
    new URI(uri.getScheme, uri.getSchemeSpecificPart, null)
  }

  private val EmptyUri = URI.create("")
}

object DefaultDtsCollector {

  def apply(entrypointUris: Set[URI]): DefaultDtsCollector = {
    new DefaultDtsCollector(entrypointUris)
  }
}
