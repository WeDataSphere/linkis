/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.linkis.storage.utils

import org.apache.linkis.common.utils.Logging

object FieldTruncationUtils extends Logging {

  /**
   * Truncate oversized fields in a record array
   *
   * @param record
   *   record array
   * @param maxLength
   *   max field length
   * @return
   *   truncated record array
   */
  def truncateRecord(record: Array[String], maxLength: Int): Array[String] = {
    if (record == null) return record

    record.map { field =>
      if (field == null) {
        null
      } else if (field.length > maxLength) {
        logger.warn(s"Field length ${field.length} exceeds max length $maxLength, truncating...")
        field.substring(0, maxLength)
      } else {
        field
      }
    }
  }

  /**
   * Check if record has oversized fields
   *
   * @param record
   *   record array
   * @param maxLength
   *   max field length
   * @return
   *   true if has oversized fields
   */
  def hasOversizedFields(record: Array[String], maxLength: Int): Boolean = {
    if (record == null) return false
    record.exists(field => field != null && field.length > maxLength)
  }

}
