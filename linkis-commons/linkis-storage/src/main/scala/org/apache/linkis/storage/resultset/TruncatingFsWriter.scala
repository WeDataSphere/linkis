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

package org.apache.linkis.storage.resultset

import org.apache.linkis.common.io._
import org.apache.linkis.common.utils.Logging
import org.apache.linkis.storage.resultset.table.TableRecord
import org.apache.linkis.storage.utils.FieldTruncationUtils

/**
 * FsWriter wrapper that truncates oversized fields before writing
 *
 * @param delegate
 *   the underlying FsWriter
 * @param maxLength
 *   maximum field length
 */
class TruncatingFsWriter(delegate: FsWriter, maxLength: Int) extends FsWriter with Logging {

  override def addMetaData(metaData: MetaData): Unit = {
    delegate.addMetaData(metaData)
  }

  override def addRecord(record: Record): Unit = {
    val truncatedRecord = record match {
      case tableRecord: TableRecord =>
        val originalRow = tableRecord.row
        if (FieldTruncationUtils.hasOversizedFields(originalRow, maxLength)) {
          val truncatedRow = FieldTruncationUtils.truncateRecord(originalRow, maxLength)
          logger.info(s"Truncated record with ${originalRow.length} fields")
          new TableRecord(truncatedRow)
        } else {
          tableRecord
        }
      case _ => record
    }
    delegate.addRecord(truncatedRecord)
  }

  override def flush(): Unit = delegate.flush()

  override def close(): Unit = delegate.close()

  // Implement other required methods by delegating to underlying writer
  override def getPath: FsPath = delegate.getPath

  override def getFs: Fs = delegate.getFs
}

object TruncatingFsWriter {

  /**
   * Wrap an FsWriter with field truncation capability
   *
   * @param writer
   *   the underlying writer
   * @param maxLength
   *   maximum field length
   * @param enabled
   *   whether truncation is enabled
   * @return
   *   wrapped writer if enabled, otherwise original writer
   */
  def wrap(writer: FsWriter, maxLength: Int, enabled: Boolean): FsWriter = {
    if (enabled) {
      new TruncatingFsWriter(writer, maxLength)
    } else {
      writer
    }
  }

}
