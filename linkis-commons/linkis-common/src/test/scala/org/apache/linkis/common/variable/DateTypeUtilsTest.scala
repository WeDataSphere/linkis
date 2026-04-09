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

package org.apache.linkis.common.variable

import java.util.Calendar

import org.junit.jupiter.api.Assertions._
import org.junit.jupiter.api.Test

class DateTypeUtilsTest {

  @Test def testGetCurHour(): Unit = {
    val dateFormat = DateTypeUtils.dateFormatLocal.get()
    val runDateStr = "20220617"
    val cal: Calendar = Calendar.getInstance()
    val hourOfDay = cal.get(Calendar.HOUR_OF_DAY)
    val hourOfDayStd = if (hourOfDay < 10) "0" + hourOfDay else "" + hourOfDay
    val hour = runDateStr + hourOfDayStd
    val curHour = DateTypeUtils.getCurHour(false, runDateStr)
    assertEquals(hour, curHour)
  }

  // ========== Week Variable Tests ==========

  @Test def testGetWeekBegin_Thursday(): Unit = {
    // TC001: getWeekBegin - 周四返回本周一
    val dateFormat = DateTypeUtils.dateFormatLocal.get()
    val date = dateFormat.parse("20260409") // 2026-04-09 is Thursday
    val result = DateTypeUtils.getWeekBegin(std = false, date)
    assertEquals("20260406", result) // Monday is 2026-04-06
  }

  @Test def testGetWeekBegin_Monday(): Unit = {
    // TC002: getWeekBegin - 周一返回自身
    val dateFormat = DateTypeUtils.dateFormatLocal.get()
    val date = dateFormat.parse("20260406") // 2026-04-06 is Monday
    val result = DateTypeUtils.getWeekBegin(std = false, date)
    assertEquals("20260406", result) // Should return itself
  }

  @Test def testGetWeekBegin_Sunday(): Unit = {
    // TC003: getWeekBegin - 周日返回本周一
    val dateFormat = DateTypeUtils.dateFormatLocal.get()
    val date = dateFormat.parse("20260412") // 2026-04-12 is Sunday
    val result = DateTypeUtils.getWeekBegin(std = false, date)
    assertEquals("20260406", result) // Monday is 2026-04-06
  }

  @Test def testGetWeekBegin_StandardFormat(): Unit = {
    // TC004: getWeekBegin - 标准格式
    val dateFormat = DateTypeUtils.dateFormatLocal.get()
    val date = dateFormat.parse("20260409") // 2026-04-09 is Thursday
    val result = DateTypeUtils.getWeekBegin(std = true, date)
    assertEquals("2026-04-06", result) // Standard format yyyy-MM-dd
  }

  @Test def testGetWeekEnd_Thursday(): Unit = {
    // TC005: getWeekEnd - 周四返回本周日
    val dateFormat = DateTypeUtils.dateFormatLocal.get()
    val date = dateFormat.parse("20260409") // 2026-04-09 is Thursday
    val result = DateTypeUtils.getWeekEnd(std = false, date)
    assertEquals("20260412", result) // Sunday is 2026-04-12
  }

  @Test def testGetWeekEnd_Sunday(): Unit = {
    // TC006: getWeekEnd - 周日返回自身
    val dateFormat = DateTypeUtils.dateFormatLocal.get()
    val date = dateFormat.parse("20260412") // 2026-04-12 is Sunday
    val result = DateTypeUtils.getWeekEnd(std = false, date)
    assertEquals("20260412", result) // Should return itself
  }

  @Test def testGetWeekEnd_Monday(): Unit = {
    // TC007: getWeekEnd - 周一返回本周日
    val dateFormat = DateTypeUtils.dateFormatLocal.get()
    val date = dateFormat.parse("20260406") // 2026-04-06 is Monday
    val result = DateTypeUtils.getWeekEnd(std = false, date)
    assertEquals("20260412", result) // Sunday is 2026-04-12
  }

  @Test def testCrossYearWeek_EndOfYear(): Unit = {
    // TC008: 跨年周 - 年末(2025-12-31 周三)
    val dateFormat = DateTypeUtils.dateFormatLocal.get()
    val date = dateFormat.parse("20251231") // 2025-12-31 is Wednesday
    val begin = DateTypeUtils.getWeekBegin(std = false, date)
    val end = DateTypeUtils.getWeekEnd(std = false, date)
    assertEquals("20251229", begin) // Monday is 2025-12-29
    assertEquals("20260104", end) // Sunday is 2026-01-04 (cross year)
  }

  @Test def testCrossYearWeek_StartOfYear(): Unit = {
    // TC009: 跨年周 - 年初(2026-01-01 周四)
    val dateFormat = DateTypeUtils.dateFormatLocal.get()
    val date = dateFormat.parse("20260101") // 2026-01-01 is Thursday
    val begin = DateTypeUtils.getWeekBegin(std = false, date)
    val end = DateTypeUtils.getWeekEnd(std = false, date)
    assertEquals("20251229", begin) // Monday is 2025-12-29 (cross year)
    assertEquals("20260104", end) // Sunday is 2026-01-04
  }

  @Test def testLeapYear_2024(): Unit = {
    // TC010: 闰年 - 2024-02-29(闰日, 周四)
    val dateFormat = DateTypeUtils.dateFormatLocal.get()
    val date = dateFormat.parse("20240229") // 2024-02-29 is leap day, Thursday
    val begin = DateTypeUtils.getWeekBegin(std = false, date)
    val end = DateTypeUtils.getWeekEnd(std = false, date)
    assertEquals("20240226", begin) // Monday is 2024-02-26
    assertEquals("20240303", end) // Sunday is 2024-03-03
  }

  @Test def testLeapYear_2020(): Unit = {
    // TC011: 闰年 - 2020-02-29(闰日, 周六)
    val dateFormat = DateTypeUtils.dateFormatLocal.get()
    val date = dateFormat.parse("20200229") // 2020-02-29 is leap day, Saturday
    val begin = DateTypeUtils.getWeekBegin(std = false, date)
    val end = DateTypeUtils.getWeekEnd(std = false, date)
    assertEquals("20200224", begin) // Monday is 2020-02-24
    assertEquals("20200301", end) // Sunday is 2020-03-01
  }

  @Test def testNonLeapYear_February(): Unit = {
    // TC012: 非闰年 - 2023-02-28(周二)
    val dateFormat = DateTypeUtils.dateFormatLocal.get()
    val date = dateFormat.parse("20230228") // 2023-02-28 is Tuesday
    val begin = DateTypeUtils.getWeekBegin(std = false, date)
    val end = DateTypeUtils.getWeekEnd(std = false, date)
    assertEquals("20230227", begin) // Monday is 2023-02-27
    assertEquals("20230305", end) // Sunday is 2023-03-05
  }

  @Test def testEveryDayOfWeek(): Unit = {
    // TC013-TC019: 每日测试(周一到周日)
    val dateFormat = DateTypeUtils.dateFormatLocal.get()

    // Monday
    val monday = dateFormat.parse("20260406")
    assertEquals("20260406", DateTypeUtils.getWeekBegin(std = false, monday))
    assertEquals("20260412", DateTypeUtils.getWeekEnd(std = false, monday))

    // Tuesday
    val tuesday = dateFormat.parse("20260407")
    assertEquals("20260406", DateTypeUtils.getWeekBegin(std = false, tuesday))
    assertEquals("20260412", DateTypeUtils.getWeekEnd(std = false, tuesday))

    // Wednesday
    val wednesday = dateFormat.parse("20260408")
    assertEquals("20260406", DateTypeUtils.getWeekBegin(std = false, wednesday))
    assertEquals("20260412", DateTypeUtils.getWeekEnd(std = false, wednesday))

    // Thursday
    val thursday = dateFormat.parse("20260409")
    assertEquals("20260406", DateTypeUtils.getWeekBegin(std = false, thursday))
    assertEquals("20260412", DateTypeUtils.getWeekEnd(std = false, thursday))

    // Friday
    val friday = dateFormat.parse("20260410")
    assertEquals("20260406", DateTypeUtils.getWeekBegin(std = false, friday))
    assertEquals("20260412", DateTypeUtils.getWeekEnd(std = false, friday))

    // Saturday
    val saturday = dateFormat.parse("20260411")
    assertEquals("20260406", DateTypeUtils.getWeekBegin(std = false, saturday))
    assertEquals("20260412", DateTypeUtils.getWeekEnd(std = false, saturday))

    // Sunday
    val sunday = dateFormat.parse("20260412")
    assertEquals("20260406", DateTypeUtils.getWeekBegin(std = false, sunday))
    assertEquals("20260412", DateTypeUtils.getWeekEnd(std = false, sunday))
  }

}
