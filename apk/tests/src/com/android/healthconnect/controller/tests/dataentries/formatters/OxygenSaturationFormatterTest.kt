/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.tests.dataentries.formatters

import android.content.Context
import android.healthconnect.datatypes.OxygenSaturationRecord
import android.healthconnect.datatypes.units.Percentage
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.dataentries.formatters.OxygenSaturationFormatter
import com.android.healthconnect.controller.dataentries.units.UnitPreferences
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.getMetaData
import com.android.healthconnect.controller.tests.utils.setLocale
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class OxygenSaturationFormatterTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var formatter: OxygenSaturationFormatter
    @Inject lateinit var preferences: UnitPreferences
    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
    }

    @Test
    fun formatValue_zero() = runBlocking {
        val oxygenSaturation = getOxygenSaturation(0.0)
        assertThat(formatter.formatValue(oxygenSaturation, preferences)).isEqualTo("0%")
    }

    @Test
    fun formatValue_one() = runBlocking {
        val oxygenSaturation = getOxygenSaturation(1.0)
        assertThat(formatter.formatValue(oxygenSaturation, preferences)).isEqualTo("1%")
    }

    @Test
    fun formatValue_fraction() = runBlocking {
        val oxygenSaturation = getOxygenSaturation(32.2)
        assertThat(formatter.formatValue(oxygenSaturation, preferences)).isEqualTo("32.2%")
    }

    @Test
    fun formatA11yValue_zero() = runBlocking {
        val oxygenSaturation = getOxygenSaturation(0.0)
        assertThat(formatter.formatA11yValue(oxygenSaturation, preferences)).isEqualTo("0 percent")
    }

    @Test
    fun formatA11yValue_one() = runBlocking {
        val oxygenSaturation = getOxygenSaturation(1.0)
        assertThat(formatter.formatA11yValue(oxygenSaturation, preferences)).isEqualTo("1 percent")
    }

    @Test
    fun formatA11yValue_fraction() = runBlocking {
        val oxygenSaturation = getOxygenSaturation(32.2)
        assertThat(formatter.formatA11yValue(oxygenSaturation, preferences))
            .isEqualTo("32.2 percent")
    }

    private fun getOxygenSaturation(value: Double): OxygenSaturationRecord {
        return OxygenSaturationRecord.Builder(getMetaData(), NOW, Percentage.fromValue(value))
            .build()
    }
}
