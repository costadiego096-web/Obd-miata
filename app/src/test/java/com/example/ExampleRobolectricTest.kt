package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.Mx5EngineType
import com.example.model.Mx5Transmission
import com.example.model.VehicleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MX-5 Telemetria", appName)
    }

    @Test
    fun `verify vehicle config tire circumference and redline`() {
        val config = VehicleConfig(
            engineType = Mx5EngineType.BP_Z3_1_8_VVT,
            transmission = Mx5Transmission.SIX_SPEED,
            tireWidthMm = 195,
            tireAspectPct = 50,
            rimDiameterInches = 15
        )
        // Tire diameter for 195/50R15 is ~576mm, circumference ~1.81m
        assertTrue(config.tireCircumferenceMeters > 1.75f && config.tireCircumferenceMeters < 1.88f)
        assertEquals(7200, config.redlineRpm)
        assertEquals(6800, config.shiftLightRpm)
    }
}
