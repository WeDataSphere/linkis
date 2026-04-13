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

import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

object DateTypeUtils {

  val dateFormatLocal = new ThreadLocal[SimpleDateFormat]() {
    override protected def initialValue = new SimpleDateFormat("yyyyMMdd")
  }

  val dateFormatStdLocal = new ThreadLocal[SimpleDateFormat]() {
    override protected def initialValue = new SimpleDateFormat("yyyy-MM-dd")
  }

  val dateFormatMonLocal = new ThreadLocal[SimpleDateFormat]() {
    override protected def initialValue = new SimpleDateFormat("yyyyMM")
  }

  val dateFormatMonStdLocal = new ThreadLocal[SimpleDateFormat]() {
    override protected def initialValue = new SimpleDateFormat("yyyy-MM")
  }

  val dateFormatHourLocal = new ThreadLocal[SimpleDateFormat]() {
    override protected def initialValue = new SimpleDateFormat("yyyyMMddHH")
  }

  val dateFormatHourStdLocal = new ThreadLocal[SimpleDateFormat]() {
    override protected def initialValue = new SimpleDateFormat("yyyy-MM-dd HH")
  }

  val dateFormatSecondLocal = new ThreadLocal[SimpleDateFormat]() {
    override protected def initialValue = new SimpleDateFormat("yyyyMMddHHmmss")
  }

  /**
   * Get Today"s date
   *
   * @param std
   *   :2017-11-16
   * @return
   */
  def getToday(std: Boolean = true, dateString: String = null): String = {
    val dateFormat = dateFormatLocal.get()
    val dateFormat_std = dateFormatStdLocal.get()
    val cal: Calendar = Calendar.getInstance()
    if (dateString != null) {
      cal.setTime(dateFormat.parse(dateString))
    }
    if (std) {
      dateFormat_std.format(cal.getTime)
    } else {
      dateFormat.format(cal.getTime)
    }
  }

  /**
   * Get Yesterday"s date
   *
   * @param std
   *   :2017-11-16
   * @return
   */
  def getYesterday(std: Boolean = true): String = {
    val dateFormat = dateFormatLocal.get()
    val dateFormat_std = dateFormatStdLocal.get()
    val cal: Calendar = Calendar.getInstance()
    cal.add(Calendar.DATE, -1)
    if (std) {
      dateFormat_std.format(cal.getTime)
    } else {
      dateFormat.format(cal.getTime)
    }
  }

  /**
   * @param std
   *   202106
   * @return
   */
  def getMonthDay(std: Boolean = true, date: Date = null): String = {
    val dateFormat = dateFormatMonLocal.get()
    val dateFormat_std = dateFormatMonStdLocal.get()
    if (std) {
      dateFormat_std.format(date)
    } else {
      dateFormat.format(date)
    }
  }

  /**
   * Get Month"s date
   *
   * @param std
   *   :2017-11-01
   * @param isEnd
   *   :01 or 30,31
   * @return
   */
  def getMonth(std: Boolean = true, isEnd: Boolean = false, date: Date): String = {
    val dateFormat = dateFormatLocal.get()
    val dateFormat_std = dateFormatStdLocal.get()
    val cal: Calendar = Calendar.getInstance()
    cal.setTime(date)
    cal.set(Calendar.DATE, 1)
    if (isEnd) {
      cal.roll(Calendar.DATE, -1)
    }
    if (std) {
      dateFormat_std.format(cal.getTime)
    } else {
      dateFormat.format(cal.getTime)
    }
  }

  def getMon(std: Boolean = true, isEnd: Boolean = false, date: Date): String = {
    val dateFormat = dateFormatMonLocal.get()
    val dateFormat_std = dateFormatMonStdLocal.get()
    val cal: Calendar = Calendar.getInstance()
    cal.setTime(date)
    if (isEnd) {
      cal.set(Calendar.MONTH, Calendar.DECEMBER)
    }
    if (std) {
      dateFormat_std.format(cal.getTime)
    } else {
      dateFormat.format(cal.getTime)
    }
  }

  /**
   * get 1st day or last day of a Quarter
   *
   * @param std
   * @param isEnd
   * @param date
   * @return
   */
  def getQuarter(std: Boolean = true, isEnd: Boolean = false, date: Date): String = {
    val dateFormat = dateFormatLocal.get()
    val dateFormat_std = dateFormatStdLocal.get()
    val cal: Calendar = Calendar.getInstance()
    cal.setTime(date)
    cal.set(Calendar.DATE, 1)
    val monthDigit: Int = cal.get(Calendar.MONTH) // get method with MONTH field returns 0-11
    if (0 <= monthDigit && monthDigit <= 2) {
      cal.set(Calendar.MONTH, 0)
    } else if (3 <= monthDigit && monthDigit <= 5) {
      cal.set(Calendar.MONTH, 3)
    } else if (6 <= monthDigit && monthDigit <= 8) {
      cal.set(Calendar.MONTH, 6)
    } else if (9 <= monthDigit && monthDigit <= 11) {
      cal.set(Calendar.MONTH, 9)
    }
    if (isEnd) {
      cal.add(Calendar.MONTH, 2)
      cal.roll(Calendar.DATE, -1)
    }
    if (std) {
      dateFormat_std.format(cal.getTime)
    } else {
      dateFormat.format(cal.getTime)
    }
  }

  /**
   * get 1st day or last day of a HalfYear
   *
   * @param std
   * @param isEnd
   * @param date
   * @return
   */
  def getHalfYear(std: Boolean = true, isEnd: Boolean = false, date: Date): String = {
    val dateFormat = dateFormatLocal.get()
    val dateFormat_std = dateFormatStdLocal.get()
    val cal: Calendar = Calendar.getInstance()
    cal.setTime(date)
    cal.set(Calendar.DATE, 1)
    val monthDigit: Int = cal.get(Calendar.MONTH) // get method with MONTH field returns 0-11
    if (0 <= monthDigit && monthDigit <= 5) {
      cal.set(Calendar.MONTH, 0)
    } else if (6 <= monthDigit && monthDigit <= 11) {
      cal.set(Calendar.MONTH, 6)
    }
    if (isEnd) {
      cal.add(Calendar.MONTH, 5)
      cal.roll(Calendar.DATE, -1)
    }
    if (std) {
      dateFormat_std.format(cal.getTime)
    } else {
      dateFormat.format(cal.getTime)
    }
  }

  /**
   * get 1st day or last day of a year
   *
   * @param std
   * @param isEnd
   * @param date
   * @return
   */
  def getYear(std: Boolean = true, isEnd: Boolean = false, date: Date): String = {
    val dateFormat = dateFormatLocal.get()
    val dateFormat_std = dateFormatStdLocal.get()
    val cal: Calendar = Calendar.getInstance()
    cal.setTime(date)
    cal.set(Calendar.DATE, 1)
    cal.set(Calendar.MONTH, 0) // set methods with field MONTH accepts 0-11
    if (isEnd) {
      cal.add(Calendar.MONTH, 11)
      cal.roll(Calendar.DATE, -1)
    }
    if (std) {
      dateFormat_std.format(cal.getTime)
    } else {
      dateFormat.format(cal.getTime)
    }
  }

  def getCurHour(std: Boolean = true, dateString: String = null): String = {
    val dateFormat = dateFormatHourLocal.get()
    val dateFormat_std = dateFormatHourStdLocal.get()
    val cal: Calendar = Calendar.getInstance()
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val hourOfDayStd = if (hour < 10) "0" + hour else "" + hour
    val curHourStr = dateString + hourOfDayStd
    val curHour = dateFormat.parse(curHourStr)
    if (std) {
      dateFormat_std.format(curHour)
    } else {
      curHourStr
    }
  }

  /**
   * Get the start date of the week (Monday)
   *
   * @param std
   *   Whether to use standard format (true: yyyy-MM-dd, false: yyyyMMdd)
   * @param date
   *   Base date
   * @return
   *   Monday date string
   */
  def getWeekBegin(std: Boolean = true, date: Date): String = {
    try {
      val dateFormat = dateFormatLocal.get()
      val dateFormat_std = dateFormatStdLocal.get()
      val cal: Calendar = Calendar.getInstance()
      cal.setTime(date)

      // Get current day of week (Calendar.SUNDAY=1, Calendar.MONDAY=2, ..., Calendar.SATURDAY=7)
      val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

      // Calculate days to Monday
      // Sunday(1) needs to go back 6 days to this week's Monday
      // Monday(2) doesn't need adjustment
      // Tuesday(3) needs to go back 1 day
      // ...
      // Saturday(7) needs to go back 5 days
      val daysToMonday = if (dayOfWeek == Calendar.SUNDAY) {
        -6 // Sunday goes back 6 days to this week's Monday
      } else {
        Calendar.MONDAY - dayOfWeek // Other dates go back to this week's Monday
      }

      cal.add(Calendar.DAY_OF_MONTH, daysToMonday)

      if (std) {
        dateFormat_std.format(cal.getTime)
      } else {
        dateFormat.format(cal.getTime)
      }
    } catch {
      case e: Exception =>
        // Return current date as fallback on error
        val fallbackFormat = if (std) dateFormatStdLocal.get() else dateFormatLocal.get()
        fallbackFormat.format(date)
    }
  }

  /**
   * Get the end date of the week (Sunday)
   *
   * @param std
   *   Whether to use standard format (true: yyyy-MM-dd, false: yyyyMMdd)
   * @param date
   *   Base date
   * @return
   *   Sunday date string
   */
  def getWeekEnd(std: Boolean = true, date: Date): String = {
    try {
      val dateFormat = dateFormatLocal.get()
      val dateFormat_std = dateFormatStdLocal.get()
      val cal: Calendar = Calendar.getInstance()
      cal.setTime(date)

      // Get current day of week
      val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

      // Calculate days to Sunday
      // Sunday(1) doesn't need adjustment
      // Monday(2) needs to go forward 6 days
      // Tuesday(3) needs to go forward 5 days
      // ...
      // Saturday(7) needs to go forward 1 day
      val daysToSunday = if (dayOfWeek == Calendar.SUNDAY) {
        0 // Sunday doesn't need adjustment
      } else {
        Calendar.SUNDAY - dayOfWeek + 7 // Other dates go forward to this week's Sunday
      }

      cal.add(Calendar.DAY_OF_MONTH, daysToSunday)

      if (std) {
        dateFormat_std.format(cal.getTime)
      } else {
        dateFormat.format(cal.getTime)
      }
    } catch {
      case e: Exception =>
        // Return current date as fallback on error
        val fallbackFormat = if (std) dateFormatStdLocal.get() else dateFormatLocal.get()
        fallbackFormat.format(date)
    }
  }

}
