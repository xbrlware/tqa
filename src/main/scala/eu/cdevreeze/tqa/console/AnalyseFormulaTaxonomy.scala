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

package eu.cdevreeze.tqa.console

import java.io.File
import java.net.URI
import java.util.logging.Logger

import scala.collection.immutable

import eu.cdevreeze.tqa.backingelem.DocumentBuilder
import eu.cdevreeze.tqa.backingelem.UriConverters
import eu.cdevreeze.tqa.backingelem.indexed.IndexedDocumentBuilder
import eu.cdevreeze.tqa.backingelem.nodeinfo.SaxonDocumentBuilder
import eu.cdevreeze.tqa.extension.formula.relationship.FormulaRelationship
import eu.cdevreeze.tqa.extension.formula.taxonomy.BasicFormulaTaxonomy
import eu.cdevreeze.tqa.relationship.DefaultRelationshipFactory
import eu.cdevreeze.tqa.taxonomybuilder.DefaultDtsCollector
import eu.cdevreeze.tqa.taxonomybuilder.TaxonomyBuilder
import eu.cdevreeze.yaidom.parse.DocumentParserUsingStax
import net.sf.saxon.s9api.Processor

/**
 * Formula-aware taxonomy parser and analyser, showing some statistics about the formula-aware taxonomy.
 *
 * @author Chris de Vreeze
 */
object AnalyseFormulaTaxonomy {

  private val logger = Logger.getGlobal

  def main(args: Array[String]): Unit = {
    require(args.size >= 2, s"Usage: AnalyseFormulaTaxonomy <taxo root dir> <entrypoint URI 1> ...")
    val rootDir = new File(args(0))
    require(rootDir.isDirectory, s"Not a directory: $rootDir")

    val entrypointUris = args.drop(1).map(u => URI.create(u)).toSet

    val useSaxon = System.getProperty("useSaxon", "false").toBoolean

    val documentBuilder = getDocumentBuilder(useSaxon, rootDir)
    val documentCollector = DefaultDtsCollector(entrypointUris)

    val lenient = System.getProperty("lenient", "false").toBoolean

    val relationshipFactory =
      if (lenient) DefaultRelationshipFactory.LenientInstance else DefaultRelationshipFactory.StrictInstance

    val taxoBuilder =
      TaxonomyBuilder.
        withDocumentBuilder(documentBuilder).
        withDocumentCollector(documentCollector).
        withRelationshipFactory(relationshipFactory)

    logger.info(s"Starting building the DTS with entrypoint(s) ${entrypointUris.mkString(", ")}")

    val basicTaxo = taxoBuilder.build()

    logger.info(s"Starting building the formula-aware taxonomy with entrypoint(s) ${entrypointUris.mkString(", ")}")

    val formulaTaxo = BasicFormulaTaxonomy.build(basicTaxo)

    val formulaRelationships = formulaTaxo.formulaRelationships

    logger.info(s"The taxonomy has ${formulaRelationships.size} formula relationships")

    val formulaRelationshipGroups: Map[String, immutable.IndexedSeq[FormulaRelationship]] =
      formulaRelationships.groupBy(_.getClass.getSimpleName)

    logger.info(s"Formula relationship group sizes (topmost 15): ${formulaRelationshipGroups.mapValues(_.size).toSeq.sortBy(_._2).reverse.take(15).mkString(", ")}")

    val sortedFormulaRelationshipGroups = formulaRelationshipGroups.toIndexedSeq.sortBy(_._2.size).reverse

    sortedFormulaRelationshipGroups foreach {
      case (relationshipName, relationships) =>
        val relationshipsByUri: Map[URI, immutable.IndexedSeq[FormulaRelationship]] = relationships.groupBy(_.docUri)

        val uris = relationshipsByUri.keySet.toSeq.sortBy(_.toString)

        uris foreach { uri =>
          val currentRelationships = relationshipsByUri.getOrElse(uri, Vector())
          val elrs = currentRelationships.map(_.elr).distinct.sorted
          val arcroles = currentRelationships.map(_.arcrole).distinct.sorted

          logger.info(s"Found ${currentRelationships.size} ${relationshipName}s in doc '${uri}'. ELRs: ${elrs.mkString(", ")}. Arcroles: ${arcroles.mkString(", ")}.")
        }
    }
  }

  private def getDocumentBuilder(useSaxon: Boolean, rootDir: File): DocumentBuilder = {
    if (useSaxon) {
      val processor = new Processor(false)

      new SaxonDocumentBuilder(processor.newDocumentBuilder(), UriConverters.uriToLocalUri(_, rootDir))
    } else {
      new IndexedDocumentBuilder(DocumentParserUsingStax.newInstance(), UriConverters.uriToLocalUri(_, rootDir))
    }
  }
}