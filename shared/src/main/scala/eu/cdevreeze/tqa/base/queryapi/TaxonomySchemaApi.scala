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

package eu.cdevreeze.tqa.base.queryapi

import scala.collection.immutable

import eu.cdevreeze.tqa.base.dom.ConceptDeclaration
import eu.cdevreeze.tqa.base.dom.DimensionDeclaration
import eu.cdevreeze.tqa.base.dom.ExplicitDimensionDeclaration
import eu.cdevreeze.tqa.base.dom.GlobalElementDeclaration
import eu.cdevreeze.tqa.base.dom.HypercubeDeclaration
import eu.cdevreeze.tqa.base.dom.ItemDeclaration
import eu.cdevreeze.tqa.base.dom.PrimaryItemDeclaration
import eu.cdevreeze.tqa.base.dom.TupleDeclaration
import eu.cdevreeze.tqa.base.dom.TypedDimensionDeclaration
import eu.cdevreeze.yaidom.core.EName

/**
 * Purely abstract trait offering a dimensionally-aware XBRL taxonomy schema API.
 *
 * See super-type SchemaApi for more information.
 *
 * Primary item declarations are concept declarations that are neither hypercubes nor dimensions,
 * regardless of whether they are used as primary items or domain members!
 *
 * @author Chris de Vreeze
 */
trait TaxonomySchemaApi extends SchemaApi {

  // Concept declarations, across documents

  def findAllConceptDeclarations: immutable.IndexedSeq[ConceptDeclaration]

  def filterConceptDeclarations(p: ConceptDeclaration => Boolean): immutable.IndexedSeq[ConceptDeclaration]

  def findConceptDeclaration(p: ConceptDeclaration => Boolean): Option[ConceptDeclaration]

  def findConceptDeclaration(ename: EName): Option[ConceptDeclaration]

  def getConceptDeclaration(ename: EName): ConceptDeclaration

  // Item declarations, across documents

  def findAllItemDeclarations: immutable.IndexedSeq[ItemDeclaration]

  def filterItemDeclarations(p: ItemDeclaration => Boolean): immutable.IndexedSeq[ItemDeclaration]

  def findItemDeclaration(p: ItemDeclaration => Boolean): Option[ItemDeclaration]

  def findItemDeclaration(ename: EName): Option[ItemDeclaration]

  def getItemDeclaration(ename: EName): ItemDeclaration

  // Tuple declarations, across documents

  def findAllTupleDeclarations: immutable.IndexedSeq[TupleDeclaration]

  def filterTupleDeclarations(p: TupleDeclaration => Boolean): immutable.IndexedSeq[TupleDeclaration]

  def findTupleDeclaration(p: TupleDeclaration => Boolean): Option[TupleDeclaration]

  def findTupleDeclaration(ename: EName): Option[TupleDeclaration]

  def getTupleDeclaration(ename: EName): TupleDeclaration

  // Primary item declarations, across documents

  def findAllPrimaryItemDeclarations: immutable.IndexedSeq[PrimaryItemDeclaration]

  def filterPrimaryItemDeclarations(p: PrimaryItemDeclaration => Boolean): immutable.IndexedSeq[PrimaryItemDeclaration]

  def findPrimaryItemDeclaration(p: PrimaryItemDeclaration => Boolean): Option[PrimaryItemDeclaration]

  def findPrimaryItemDeclaration(ename: EName): Option[PrimaryItemDeclaration]

  def getPrimaryItemDeclaration(ename: EName): PrimaryItemDeclaration

  // Hypercube declarations, across documents

  def findAllHypercubeDeclarations: immutable.IndexedSeq[HypercubeDeclaration]

  def filterHypercubeDeclarations(p: HypercubeDeclaration => Boolean): immutable.IndexedSeq[HypercubeDeclaration]

  def findHypercubeDeclaration(p: HypercubeDeclaration => Boolean): Option[HypercubeDeclaration]

  def findHypercubeDeclaration(ename: EName): Option[HypercubeDeclaration]

  def getHypercubeDeclaration(ename: EName): HypercubeDeclaration

  // Dimension declarations, across documents

  def findAllDimensionDeclarations: immutable.IndexedSeq[DimensionDeclaration]

  def filterDimensionDeclarations(p: DimensionDeclaration => Boolean): immutable.IndexedSeq[DimensionDeclaration]

  def findDimensionDeclaration(p: DimensionDeclaration => Boolean): Option[DimensionDeclaration]

  def findDimensionDeclaration(ename: EName): Option[DimensionDeclaration]

  def getDimensionDeclaration(ename: EName): DimensionDeclaration

  // Explicit dimension declarations, across documents

  def findAllExplicitDimensionDeclarations: immutable.IndexedSeq[ExplicitDimensionDeclaration]

  def filterExplicitDimensionDeclarations(p: ExplicitDimensionDeclaration => Boolean): immutable.IndexedSeq[ExplicitDimensionDeclaration]

  def findExplicitDimensionDeclaration(p: ExplicitDimensionDeclaration => Boolean): Option[ExplicitDimensionDeclaration]

  def findExplicitDimensionDeclaration(ename: EName): Option[ExplicitDimensionDeclaration]

  def getExplicitDimensionDeclaration(ename: EName): ExplicitDimensionDeclaration

  // Typed dimension declarations, across documents

  def findAllTypedDimensionDeclarations: immutable.IndexedSeq[TypedDimensionDeclaration]

  def filterTypedDimensionDeclarations(p: TypedDimensionDeclaration => Boolean): immutable.IndexedSeq[TypedDimensionDeclaration]

  def findTypedDimensionDeclaration(p: TypedDimensionDeclaration => Boolean): Option[TypedDimensionDeclaration]

  def findTypedDimensionDeclaration(ename: EName): Option[TypedDimensionDeclaration]

  def getTypedDimensionDeclaration(ename: EName): TypedDimensionDeclaration

  // Typed dimension member declarations

  def findMemberDeclarationOfTypedDimension(typedDimension: EName): Option[GlobalElementDeclaration]

  def getMemberDeclarationOfTypedDimension(typedDimension: EName): GlobalElementDeclaration
}
