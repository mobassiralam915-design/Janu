package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.persona.ZoyaMood
import com.example.persona.ZoyaPersona
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ZARA AI", appName)
    }

    @Test
    fun `verify zoya persona moods`() {
        val defaultMood = ZoyaMood.SASSY
        assertNotNull(defaultMood)
        val prompt = ZoyaPersona.getSystemPrompt(defaultMood)
        assertTrue(prompt.contains("Zoya"))
    }

    @Test
    fun `verify checkBatteryLevel tool execution`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val toolsManager = com.example.data.tools.DeviceToolsManager(context)
        val result = toolsManager.executeTool("checkBatteryLevel", emptyMap())
        assertTrue(result.success)
        assertTrue(result.data.containsKey("batteryPercent"))
        assertTrue(result.data.containsKey("wittyEnergyComparison"))
        val remark = result.data["wittyEnergyComparison"] as String
        assertTrue(remark.isNotEmpty())
    }
}
