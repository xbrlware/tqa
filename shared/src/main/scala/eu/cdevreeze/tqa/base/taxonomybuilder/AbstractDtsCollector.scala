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

package eu.cdevreeze.tqa.base.taxonomybuilder

import java.net.URI

import scala.annotation.tailrec
import scala.collection.immutable

import eu.cdevreeze.tqa.base.dom.TaxonomyDocument
import eu.cdevreeze.tqa.docbuilder.DocumentBuilder

/**
 * Abstract partially implemented DTS discovery as document collector. It is memory-hungry in that
 * all found documents are stored in memory while finding the DTS. It is also unforgiving in that broken
 * links are not allowed.
 *
 * If there is a broken link due to a typo, consider tweaking the DocumentBuilder with some post-processing.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractDtsCollector extends DocumentCollector {

  final def collectTaxonomyDocuments(
    entryPointUris:  Set[URI],
    documentBuilder: DocumentBuilder): immutable.IndexedSeq[TaxonomyDocument] = {

    require(entryPointUris.nonEmpty, s"At least one entryPoint URI must be provided")

    val dts = findDts(entryPointUris, Map(), documentBuilder)

    dts.values.toIndexedSeq.sortBy(_.uri.toString)
  }

  /**
   * Finds all absolute URIs without fragment that must be found in the given document
   * according to DTS discovery rules. The result excludes the document URI of the given
   * document itself. Minds the possibility of having embedded linkbases in schemas.
   */
  def findAllUsedDocUris(rootElem: TaxonomyDocument): Set[URI]

  @tailrec
  private def findDts(
    docUris:         Set[URI],
    processedDocs:   Map[URI, TaxonomyDocument],
    documentBuilder: DocumentBuilder): Map[URI, TaxonomyDocument] = {

    val processedDocUris = processedDocs.keySet

    assert(processedDocUris.subsetOf(docUris))

    // One step, processing all URIs currently known, and not yet processed
    val docUrisToProcess = docUris.diff(processedDocUris)

    val taxoDocsFound = docUrisToProcess.toIndexedSeq.map(uri => buildTaxonomyDocument(uri, documentBuilder))

    val taxoDocMapFound: Map[URI, TaxonomyDocument] = taxoDocsFound.map(e => (e.uri -> e)).toMap

    val docUrisFound = taxoDocsFound.flatMap(e => findAllUsedDocUris(e)).toSet

    val newDocUris = docUris.union(docUrisFound)

    val newProcessedDocs: Map[URI, TaxonomyDocument] = processedDocs ++ taxoDocMapFound

    assert(newProcessedDocs.keySet == docUris)

    if (docUrisFound.subsetOf(docUris)) {
      assert(newDocUris == docUris)

      newProcessedDocs
    } else {
      assert(newDocUris.diff(docUris).nonEmpty)

      // Recursive call
      findDts(newDocUris, newProcessedDocs, documentBuilder)
    }
  }

  private def buildTaxonomyDocument(uri: URI, documentBuilder: DocumentBuilder): TaxonomyDocument = {
    val taxoDocOption = TaxonomyDocument.buildOptionally(documentBuilder.build(uri))

    taxoDocOption.getOrElse(sys.error(s"Could not find taxonomy document for $uri during DTS discovery"))
  }
}
