package com.example

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.example.webview.HtmlRendererViewModel
import com.example.webview.HtmlSource
import com.example.webview.RendererTab
import com.example.webview.SiwartaDownloadHelper
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
    assertEquals("SIWARTA", appName)
  }

  @Test
  fun `bundled siwarta html asset exists`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val content =
      context.assets.open("Siwarta_non_Air_9fc435eb.html").bufferedReader().use { it.readText() }
    assertTrue(content.contains("SIWARTA"))
    assertTrue(content.contains("PERUMAHAN TAMALAND"))
    assertTrue(content.contains("meterAirPeriodeFilter"))
    assertTrue(content.contains("simpanMeterAirRow"))
    assertTrue(content.contains("startEditMeterAirRow"))
    assertTrue(content.contains("deleteMeterAir"))
    assertTrue(content.contains("sendMeterAirNotificationToWarga"))
    assertTrue(content.contains("Pending (Menunggu Verifikasi)"))
    assertTrue(content.contains("Inv-Air"))
    assertTrue(content.contains("wargaWaterRechartsContainer"))
    assertTrue(content.contains("renderWargaWaterUsageReport"))
    assertTrue(content.contains("Recharts"))
    assertTrue(content.contains("wargaDueReminderCard"))
    assertTrue(content.contains("checkAndTriggerWargaAutoReminder"))
    assertTrue(content.contains("runAdminAutoDuePaymentReminders"))
    assertTrue(content.contains("saveSetupReminderConfig"))
    assertTrue(content.contains("sendPaymentReminderNotification"))
  }

  @Test
  fun `notification helper persists reminder schedule config and clamps values`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.notification.SiwartaNotificationHelper.ensureNotificationChannel(context)
    com.example.notification.SiwartaNotificationHelper.saveReminderConfigAndSchedule(
      context = context,
      config =
        com.example.notification.ReminderScheduleConfig(
          enabled = true,
          dueDay = 31,
          daysBefore = 5,
          wargaNoRumah = "AA-01",
          wargaNama = "Budi",
          unpaidTitle = "H-5 Jatuh Tempo Iuran (AA-01)",
          unpaidMessage = "Segera bayar iuran bulan November 2026.",
          hasUnpaidDueSoon = true,
          periodeLabel = "November 2026",
        ),
    )

    val saved = com.example.notification.SiwartaNotificationHelper.getSavedReminderConfig(context)
    assertTrue(saved.enabled)
    assertEquals(28, saved.dueDay)
    assertEquals(5, saved.daysBefore)
    assertEquals("AA-01", saved.wargaNoRumah)
    assertTrue(saved.hasUnpaidDueSoon)
  }

  @Test
  fun `download helper saves base64 data url`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sampleBase64 = Base64.encodeToString("hello pdf".toByteArray(), Base64.NO_WRAP)
    val result =
      SiwartaDownloadHelper.saveBase64Export(
        context = context,
        dataUrlOrBase64 = "data:application/pdf;base64,$sampleBase64",
        rawFileName = "Resi_Test.pdf",
        rawMimeType = "application/pdf",
      )
    assertTrue(result.isSuccess)
    assertEquals("Resi_Test.pdf", result.getOrThrow().fileName)
  }

  @Test
  fun `viewModel handles custom html loading and live editor updates`() {
    val vm = HtmlRendererViewModel()
    assertEquals(HtmlSource.BundledSiwarta, vm.uiState.value.source)

    val customHtml = "<html><body><button onclick='alert(1)'>Klik</button></body></html>"
    vm.loadPickedHtmlFile("demo.html", customHtml)
    val loaded = vm.uiState.value.source as HtmlSource.CustomHtml
    assertEquals("demo.html", loaded.title)
    assertEquals(customHtml, loaded.content)
    assertEquals(RendererTab.PREVIEW, vm.uiState.value.activeTab)

    val editedHtml = "<html><body><input id='x' /><p id='out'></p></body></html>"
    vm.selectTab(RendererTab.EDITOR)
    vm.updateEditorText(editedHtml)
    vm.applyEditorHtml()
    val applied = vm.uiState.value.source as HtmlSource.CustomHtml
    assertEquals(editedHtml, applied.content)

    vm.appendConsoleLog("LOG", "Tombol ditekan", 42)
    assertEquals(1, vm.uiState.value.consoleLogs.size)
    assertEquals("Tombol ditekan", vm.uiState.value.consoleLogs.first().message)

    vm.resetToBundledSiwarta()
    assertEquals(HtmlSource.BundledSiwarta, vm.uiState.value.source)
  }
}
