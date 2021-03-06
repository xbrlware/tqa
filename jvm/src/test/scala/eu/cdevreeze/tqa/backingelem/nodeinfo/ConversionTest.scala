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

package eu.cdevreeze.tqa.backingelem.nodeinfo

import java.io.File

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple
import javax.xml.transform.stream.StreamSource
import net.sf.saxon.s9api.Processor

/**
 * Conversion test for Saxon elements to native simple elements and vice versa.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class ConversionTest extends FunSuite {

  private val processor = new Processor(false)

  private val saxonToSimpleElemConverter = YaidomSaxonToSimpleElemConverter

  private val simpleToSaxonElemConverter = new YaidomSimpleToSaxonElemConverter(processor)

  val doc: SaxonDocument = {
    val docUri = classOf[ConversionTest].getResource("../some-data.xsd").toURI
    val inputSource = new StreamSource(new File(docUri))
    val doc = processor.getUnderlyingConfiguration.buildDocumentTree(inputSource)

    SaxonDocument.wrapDocument(doc).
      ensuring(_.documentElement.findAllElemsOrSelf.size > 100).
      ensuring(_.uriOption.isDefined).
      ensuring(_.uriOption.get.toString.nonEmpty)
  }

  test("testRoundtrippingStartingWithSaxon") {
    val simpleDoc = saxonToSimpleElemConverter.convertSaxonDocument(doc)

    assertResult(resolved.Elem(doc.documentElement)) {
      resolved.Elem(simpleDoc.documentElement)
    }
    assertResult(doc.uriOption) {
      simpleDoc.uriOption
    }

    val saxonDoc = simpleToSaxonElemConverter.convertSimpleDocument(simpleDoc)

    assertResult(resolved.Elem(simpleDoc.documentElement)) {
      resolved.Elem(saxonDoc.documentElement)
    }
    assertResult(simpleDoc.uriOption) {
      saxonDoc.uriOption
    }
  }

  test("testRoundtrippingStartingWithNativeYaidom") {
    val docParser = DocumentParserUsingSax.newInstance()

    val docUri = classOf[ConversionTest].getResource("../some-data.xsd").toURI
    val simpleDoc =
      docParser.parse(docUri).withUriOption(Some(docUri)).
        ensuring(_.documentElement.findAllElemsOrSelf.size > 100).
        ensuring(_.uriOption.isDefined).
        ensuring(_.uriOption.get.toString.nonEmpty)

    val saxonDoc = simpleToSaxonElemConverter.convertSimpleDocument(simpleDoc)

    assertResult(resolved.Elem(simpleDoc.documentElement)) {
      resolved.Elem(saxonDoc.documentElement)
    }
    assertResult(simpleDoc.uriOption) {
      saxonDoc.uriOption
    }

    val simpleDoc2 = saxonToSimpleElemConverter.convertSaxonDocument(saxonDoc)

    assertResult(resolved.Elem(saxonDoc.documentElement)) {
      resolved.Elem(simpleDoc2.documentElement)
    }
    assertResult(saxonDoc.uriOption) {
      simpleDoc2.uriOption
    }
  }

  test("testTransformViaConvertedElem") {
    val simpleDoc =
      saxonToSimpleElemConverter.convertSaxonDocument(doc) ensuring { d =>
        d.documentElement.filterElems(_.qname == QName("link:linkbaseRef")).nonEmpty
      }

    val scope = simpleDoc.documentElement.scope

    val newScope = scope ++ Scope.from("linkbase" -> scope.prefixNamespaceMap("link"))

    val editedRootElem =
      simpleDoc.documentElement transformElems { e =>
        if (e.qname == QName("link:linkbaseRef")) {
          simple.Node.elem(QName("linkbase:linkbaseRef"), e.attributes, newScope ++ e.scope, e.children)
        } else {
          e
        }
      }

    val saxonDoc =
      simpleToSaxonElemConverter.convertSimpleDocument(simpleDoc.withDocumentElement(editedRootElem))

    assertResult(true) {
      saxonDoc.documentElement.filterElems(_.qname == QName("link:linkbaseRef")).isEmpty
    }

    assertResult(resolved.Elem(editedRootElem)) {
      resolved.Elem(saxonDoc.documentElement)
    }

    assertResult(resolved.Elem(doc.documentElement)) {
      resolved.Elem(saxonDoc.documentElement)
    }
  }
}
