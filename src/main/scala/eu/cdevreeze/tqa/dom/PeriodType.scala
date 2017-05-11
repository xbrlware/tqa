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

package eu.cdevreeze.tqa.dom

/**
 * Value of (item declaration) attribute xbrli:periodType, so either instant or duration.
 *
 * @author Chris de Vreeze
 */
sealed trait PeriodType

object PeriodType {

  case object Instant extends PeriodType
  case object Duration extends PeriodType

  def fromString(s: String): PeriodType = s match {
    case "instant"  => Instant
    case "duration" => Duration
    case _          => sys.error(s"Not a valid 'xbrli:periodType': $s")
  }
}