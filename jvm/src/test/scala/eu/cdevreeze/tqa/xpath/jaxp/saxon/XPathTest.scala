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

package eu.cdevreeze.tqa.xpath.jaxp.saxon

import java.io.File
import java.net.URI

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.tqa.ENames
import eu.cdevreeze.tqa.Namespaces
import eu.cdevreeze.tqa.backingelem.nodeinfo.SaxonElem
import eu.cdevreeze.tqa.backingelem.nodeinfo.SaxonNode
import eu.cdevreeze.tqa.backingelem.nodeinfo.YaidomSaxonToSimpleElemConverter
import eu.cdevreeze.tqa.backingelem.nodeinfo.YaidomSimpleToSaxonElemConverter
import eu.cdevreeze.tqa.backingelem.nodeinfo.docbuilder.SaxonDocumentBuilder
import eu.cdevreeze.tqa.docbuilder.jvm.UriConverters
import eu.cdevreeze.tqa.docbuilder.jvm.UriResolvers
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.core.jvm.JavaQNames
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple
import javax.xml.xpath.XPathFunction
import javax.xml.xpath.XPathFunctionResolver
import javax.xml.xpath.XPathVariableResolver
import net.sf.saxon.om.NodeInfo
import net.sf.saxon.s9api.Processor

/**
 * XPath test case using JAXP backed by Saxon.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XPathTest extends FunSuite {

  private val processor = new Processor(false)

  private val MyFuncNamespace = "http://example.com/xbrl-xpath-functions"
  private val MyVarNamespace = "http://example.com/xbrl-xpath-variables"

  private val rootDir = new File(classOf[XPathTest].getResource("/taxonomies").toURI)

  private val instanceDir = new File(classOf[XPathTest].getResource("/sample-instances").toURI)
  private val docUri = (new File(instanceDir, "sample-xbrl-instance.xml")).toURI

  private val docBuilder = getDocumentBuilder(rootDir)

  private val rootElem: SaxonElem = docBuilder.build(docUri).documentElement

  private def useXbrliPrefix(e: SaxonElem): SaxonElem = {
    require(
      e.scope.filterNamespaces(Set(Namespaces.XbrliNamespace)).keySet == Set("", "xbrli"),
      s"Expected namespace ${Namespaces.XbrliNamespace} as default namespace and having prefix 'xbrli' as well")

    def convert(elm: simple.Elem): simple.Elem = {
      if (elm.qname.prefixOption.isEmpty) elm.copy(qname = QName("xbrli", elm.qname.localPart)) else elm
    }

    val converterToSaxon = new YaidomSimpleToSaxonElemConverter(processor)

    val simpleRootElem = YaidomSaxonToSimpleElemConverter.convertSaxonElem(e.rootElem)
    val simpleResultRootElem = simpleRootElem.updateElemOrSelf(e.path)(_.transformElemsOrSelf(convert))
    // The conversion above happens to leave the Path to the resulting element the same!
    converterToSaxon.convertSimpleElem(simpleResultRootElem).getElemOrSelfByPath(e.path).ensuring(_.path == e.path)
  }

  private val xpathEvaluatorFactory =
    JaxpXPathEvaluatorFactoryUsingSaxon.newInstance(processor.getUnderlyingConfiguration)

  xpathEvaluatorFactory.underlyingEvaluatorFactory.setXPathFunctionResolver(new XPathFunctionResolver {

    def resolveFunction(functionName: javax.xml.namespace.QName, arity: Int): XPathFunction = {
      if (arity == 1 && (functionName == JavaQNames.enameToJavaQName(EName(MyFuncNamespace, "contexts"), None))) {
        new FindAllXbrliContexts
      } else if (arity == 2 && (functionName == JavaQNames.enameToJavaQName(EName(MyFuncNamespace, "transform"), None))) {
        new TransformElem
      } else {
        sys.error(s"Unknown function with name $functionName and arity $arity")
      }
    }
  })

  xpathEvaluatorFactory.underlyingEvaluatorFactory.setXPathVariableResolver(new XPathVariableResolver {

    def resolveVariable(variableName: javax.xml.namespace.QName): AnyRef = {
      if (variableName == JavaQNames.enameToJavaQName(EName("contextPosition"), None)) {
        java.lang.Integer.valueOf(4)
      } else if (variableName == JavaQNames.enameToJavaQName(EName(MyVarNamespace, "contextPosition"), None)) {
        java.lang.Integer.valueOf(4)
      } else if (variableName == JavaQNames.enameToJavaQName(EName(MyVarNamespace, "identity"), None)) {
        { e: SaxonElem => e }
      } else if (variableName == JavaQNames.enameToJavaQName(EName(MyVarNamespace, "useXbrliPrefix"), None)) {
        { e: SaxonElem => useXbrliPrefix(e) }
      } else {
        sys.error(s"Unknown variable with name $variableName")
      }
    }
  })

  private val xpathEvaluator =
    JaxpXPathEvaluatorUsingSaxon.newInstance(
      xpathEvaluatorFactory,
      rootElem.docUri,
      rootElem.scope ++ JaxpXPathEvaluatorUsingSaxon.MinimalScope ++
        Scope.from("myfun" -> MyFuncNamespace, "myvar" -> MyVarNamespace),
      new SimpleUriResolver(UriConverters.fromLocalMirrorRootDirectory(rootDir)))

  test("testSimpleStringXPathWithoutContextItem") {
    val exprString = "string(count((1, 2, 3, 4, 5)))"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsString(expr, None)

    assertResult("5") {
      result
    }
  }

  test("testSimpleNumberXPathWithoutContextItem") {
    val exprString = "count((1, 2, 3, 4, 5))"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsBigDecimal(expr, None)

    assertResult(5) {
      result.toInt
    }
  }

  test("testSimpleBooleanXPathWithoutContextItem") {
    val exprString = "empty((1, 2, 3, 4, 5))"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsBoolean(expr, None)

    assertResult(false) {
      result
    }
  }

  test("testSimpleENameXPathWithoutContextItem") {
    val exprString = "xs:QName('xbrli:item')"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsEName(expr, None)

    assertResult(ENames.XbrliItemEName) {
      result
    }
  }

  test("testLoopingXPathWithoutContextItem") {
    val exprString = "max(for $i in (1 to 5) return $i * 2)"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsBigDecimal(expr, None)

    assertResult(BigDecimal("10")) {
      result
    }
  }

  test("testSimpleNodeXPath") {
    val exprString = "//xbrli:context[1]/xbrli:entity/xbrli:segment/xbrldi:explicitMember[1]"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsNode(expr, Some(rootElem.wrappedNode))

    assertResult("gaap:ABCCompanyDomain") {
      SaxonNode.wrapElement(result.asInstanceOf[NodeInfo]).text.trim
    }
  }

  test("testSimpleNodeSeqXPath") {
    val exprString = "//xbrli:context/xbrli:entity/xbrli:segment/xbrldi:explicitMember"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsNodeSeq(expr, Some(rootElem.wrappedNode))

    assertResult(true) {
      result.size > 100
    }
  }

  test("testYaidomQueryOnXPathNodeResults") {
    val exprString = "//xbrli:context/xbrli:entity/xbrli:segment/xbrldi:explicitMember"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsNodeSeq(expr, Some(rootElem.wrappedNode))

    // Use yaidom query API on results

    val resultElems = result.map(e => SaxonNode.wrapElement(e))

    assertResult(true) {
      val someDimQNames =
        Set(QName("gaap:EntityAxis"), QName("gaap:VerificationAxis"), QName("gaap:PremiseAxis"), QName("gaap:ShareOwnershipPlanIdentifierAxis"))

      val someDimENames = someDimQNames.map(qn => rootElem.scope.resolveQNameOption(qn).get)

      val foundDimensions =
        resultElems.flatMap(_.attributeAsResolvedQNameOption(ENames.DimensionEName)).toSet

      someDimENames.subsetOf(foundDimensions)
    }

    // The Paths are not lost!

    val resultElemPaths = resultElems.map(_.path)

    assertResult(Set(List("context", "entity", "segment", "explicitMember"))) {
      resultElemPaths.map(_.entries.map(_.elementName.localPart)).toSet
    }
    assertResult(Set(EName(Namespaces.XbrliNamespace, "xbrl"))) {
      resultElems.map(_.rootElem.resolvedName).toSet
    }

    assertResult(resultElems) {
      resultElems.map(e => e.rootElem.getElemOrSelfByPath(e.path))
    }
  }

  test("testSimpleBackingElemXPath") {
    val exprString = "//xbrli:context[1]/xbrli:entity/xbrli:segment/xbrldi:explicitMember[1]"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val resultElem = xpathEvaluator.evaluateAsBackingElem(expr, Some(rootElem.wrappedNode))

    assertResult("gaap:ABCCompanyDomain") {
      resultElem.text.trim
    }
  }

  test("testSimpleBackingElemSeqXPath") {
    val exprString = "//xbrli:context/xbrli:entity/xbrli:segment/xbrldi:explicitMember"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val resultElems = xpathEvaluator.evaluateAsBackingElemSeq(expr, Some(rootElem.wrappedNode))

    assertResult(true) {
      resultElems.size > 100
    }
  }

  test("testYaidomQueryOnXPathBackingElemResults") {
    val exprString = "//xbrli:context/xbrli:entity/xbrli:segment/xbrldi:explicitMember"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val resultElems = xpathEvaluator.evaluateAsBackingElemSeq(expr, Some(rootElem.wrappedNode))

    // Use yaidom query API on results

    assertResult(true) {
      val someDimQNames =
        Set(QName("gaap:EntityAxis"), QName("gaap:VerificationAxis"), QName("gaap:PremiseAxis"), QName("gaap:ShareOwnershipPlanIdentifierAxis"))

      val someDimENames = someDimQNames.map(qn => rootElem.scope.resolveQNameOption(qn).get)

      val foundDimensions =
        resultElems.flatMap(_.attributeAsResolvedQNameOption(ENames.DimensionEName)).toSet

      someDimENames.subsetOf(foundDimensions)
    }

    // The Paths are not lost!

    val resultElemPaths = resultElems.map(_.path)

    assertResult(Set(List("context", "entity", "segment", "explicitMember"))) {
      resultElemPaths.map(_.entries.map(_.elementName.localPart)).toSet
    }
    assertResult(Set(EName(Namespaces.XbrliNamespace, "xbrl"))) {
      resultElems.map(_.rootElem.resolvedName).toSet
    }

    assertResult(resultElems) {
      resultElems.map(e => e.rootElem.getElemOrSelfByPath(e.path))
    }
  }

  test("testBaseUri") {
    val exprString = "base-uri(/xbrli:xbrl)"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val resultAsString = xpathEvaluator.evaluateAsString(expr, Some(rootElem.wrappedNode))
    val result = URI.create(resultAsString)

    assertResult(true) {
      resultAsString.contains("sample-instances") && resultAsString.contains("sample-xbrl-instance.xml")
    }
    assertResult(result) {
      rootElem.baseUri
    }
  }

  test("testDocFunction") {
    val exprString =
      "doc('http://www.nltaxonomie.nl/nt11/kvk/20170419/presentation/kvk-balance-sheet-education-pre.xml')//link:presentationLink[1]/link:loc[10]"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsNode(expr, Some(rootElem.wrappedNode))

    val resultElem = SaxonNode.wrapElement(result.asInstanceOf[NodeInfo])

    assertResult(Some("urn:kvk:linkrole:balance-sheet-education")) {
      // Getting parent element, to make the example more exciting
      resultElem.parent.attributeOption(ENames.XLinkRoleEName)
    }
  }

  test("testCustomFunction") {
    val exprString = "myfun:contexts(.)[4]"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsNode(expr, Some(rootElem.wrappedNode))

    val resultElem = SaxonNode.wrapElement(result.asInstanceOf[NodeInfo])

    assertResult(EName(Namespaces.XbrliNamespace, "context")) {
      resultElem.resolvedName
    }
    assertResult(Some("I-2005")) {
      resultElem.attributeOption(ENames.IdEName)
    }
  }

  test("testCustomFunctionAndVariable") {
    val exprString = "myfun:contexts(.)[$contextPosition]"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsNode(expr, Some(rootElem.wrappedNode))

    val resultElem = SaxonNode.wrapElement(result.asInstanceOf[NodeInfo])

    assertResult(EName(Namespaces.XbrliNamespace, "context")) {
      resultElem.resolvedName
    }
    assertResult(Some("I-2005")) {
      resultElem.attributeOption(ENames.IdEName)
    }
  }

  test("testCustomFunctionAndPrefixedVariable") {
    val exprString = "myfun:contexts(.)[$myvar:contextPosition]"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsNode(expr, Some(rootElem.wrappedNode))

    val resultElem = SaxonNode.wrapElement(result.asInstanceOf[NodeInfo])

    assertResult(EName(Namespaces.XbrliNamespace, "context")) {
      resultElem.resolvedName
    }
    assertResult(Some("I-2005")) {
      resultElem.attributeOption(ENames.IdEName)
    }
  }

  test("testIdentityTransformation") {
    val exprString = "myfun:transform(., $myvar:identity)"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsNode(expr, Some(rootElem.wrappedNode))

    val resultElem = SaxonNode.wrapElement(result.asInstanceOf[NodeInfo])

    assertResult(EName(Namespaces.XbrliNamespace, "xbrl")) {
      resultElem.resolvedName
    }
    assertResult(rootElem.findAllElemsOrSelf.size) {
      resultElem.findAllElemsOrSelf.size
    }
  }

  test("testUseXbrliPrefixTransformation") {
    val exprString = "myfun:transform(., $myvar:useXbrliPrefix)"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsNode(expr, Some(rootElem.wrappedNode))

    val resultElem = SaxonNode.wrapElement(result.asInstanceOf[NodeInfo])

    assertResult(EName(Namespaces.XbrliNamespace, "xbrl")) {
      resultElem.resolvedName
    }
    assertResult(rootElem.findAllElemsOrSelf.size) {
      resultElem.findAllElemsOrSelf.size
    }
    assertResult(List.empty) {
      resultElem.filterElemsOrSelf(_.qname.prefixOption.isEmpty)
    }
    assertResult(resolved.Elem(rootElem)) {
      resolved.Elem(resultElem)
    }
  }

  test("testUseXbrliPrefixLocalTransformation") {
    val exprString = "myfun:transform(., $myvar:useXbrliPrefix)"

    val firstContext =
      rootElem.findElem(e => e.resolvedName == EName(Namespaces.XbrliNamespace, "context") &&
        e.attributeOption(ENames.IdEName).contains("I-2007")).head

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsNode(expr, Some(firstContext.wrappedNode))

    val resultContextElem = SaxonNode.wrapElement(result.asInstanceOf[NodeInfo])
    val resultRootElem = resultContextElem.rootElem

    assertResult(EName(Namespaces.XbrliNamespace, "xbrl")) {
      resultRootElem.resolvedName
    }
    assertResult(rootElem.findAllElemsOrSelf.size) {
      resultRootElem.findAllElemsOrSelf.size
    }
    assertResult(List.empty) {
      resultContextElem.filterElemsOrSelf(_.qname.prefixOption.isEmpty)
    }
    assertResult(false) {
      resultRootElem.filterElemsOrSelf(_.qname.prefixOption.isEmpty).isEmpty
    }
    assertResult(resolved.Elem(firstContext)) {
      resolved.Elem(resultContextElem)
    }
    assertResult(resolved.Elem(rootElem)) {
      resolved.Elem(resultRootElem)
    }
  }

  test("testInstanceOfElement") {
    val exprString = ". instance of element()"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsBoolean(expr, Some(rootElem.wrappedNode))

    assertResult(true) {
      result
    }
  }

  test("testNotInstanceOfElement") {
    val exprString = "3 instance of element()"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsBoolean(expr, None)

    assertResult(false) {
      result
    }
  }

  test("testInstanceOfElementSeq") {
    val exprString = "myfun:contexts(.) instance of element()+"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsBoolean(expr, Some(rootElem.wrappedNode))

    assertResult(true) {
      result
    }
  }

  test("testSumOfEmptySeq") {
    val exprString = "sum(())"

    val expr = xpathEvaluator.toXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsBigDecimal(expr, None)

    assertResult(0) {
      result.toInt
    }
  }

  private def getDocumentBuilder(rootDir: File): SaxonDocumentBuilder = {
    val uriConverterUsingRootDir = UriConverters.fromLocalMirrorRootDirectory(rootDir)

    def convertUri(uri: URI): URI = {
      if (uri.getScheme == "file") uri else uriConverterUsingRootDir(uri)
    }

    SaxonDocumentBuilder(
      processor.newDocumentBuilder(),
      UriResolvers.fromUriConverter(convertUri _))
  }
}
