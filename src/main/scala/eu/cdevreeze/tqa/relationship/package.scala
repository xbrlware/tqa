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

package eu.cdevreeze.tqa

/**
 * This package contains '''relationships'''. Relationships are like their underlying arcs, but resolving the locators.
 * Note that an arc may represent more than 1 relationship.
 *
 * This package mainly contains:
 * <ul>
 * <li>Type [[eu.cdevreeze.tqa.relationship.Relationship]] and its sub-types</li>
 * <li>Type [[eu.cdevreeze.tqa.relationship.RelationshipFactory]] and its sub-types</li>
 * </ul>
 *
 * Relationship factories extract relationships from a [[eu.cdevreeze.tqa.dom.TaxonomyBase]]. They can be used
 * directly, but typically they are used implicitly when creating a [[eu.cdevreeze.tqa.taxonomy.BasicTaxonomy]].
 *
 * For the usage of this API, see packages [[eu.cdevreeze.tqa.queryapi]] and [[eu.cdevreeze.tqa.taxonomy]].
 *
 * This package unidirectionally depends on the [[eu.cdevreeze.tqa.dom]] package.
 *
 * @author Chris de Vreeze
 */
package object relationship
