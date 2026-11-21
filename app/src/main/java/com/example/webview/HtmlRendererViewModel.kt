package com.example.webview

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed interface HtmlSource {
  data object BundledSiwarta : HtmlSource

  data class CustomHtml(
    val title: String,
    val content: String,
    val revision: Long = System.currentTimeMillis(),
  ) : HtmlSource
}

enum class RendererTab {
  PREVIEW,
  EDITOR,
}

data class ConsoleLogEntry(
  val id: Long,
  val level: String,
  val message: String,
  val lineNumber: Int,
)

data class HtmlRendererUiState(
  val source: HtmlSource = HtmlSource.BundledSiwarta,
  val activeTab: RendererTab = RendererTab.PREVIEW,
  val editorText: String = HtmlRendererViewModel.DEFAULT_INTERACTIVE_HTML_TEMPLATE,
  val autoLiveRender: Boolean = true,
  val isFullscreen: Boolean = false,
  val pageTitle: String = "SIWARTA - Perumahan Tamaland",
  val loadProgress: Int = 0,
  val isPageLoading: Boolean = true,
  val hasMainFrameError: Boolean = false,
  val errorDescription: String = "",
  val consoleLogs: List<ConsoleLogEntry> = emptyList(),
  val showConsoleSheet: Boolean = false,
)

class HtmlRendererViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(HtmlRendererUiState())
  val uiState: StateFlow<HtmlRendererUiState> = _uiState.asStateFlow()

  private var nextLogId = 1L

  fun selectTab(tab: RendererTab) {
    _uiState.update { it.copy(activeTab = tab) }
  }

  fun toggleFullscreen(fullscreen: Boolean) {
    _uiState.update { it.copy(isFullscreen = fullscreen) }
  }

  fun toggleConsoleSheet(show: Boolean) {
    _uiState.update { it.copy(showConsoleSheet = show) }
  }

  fun clearConsoleLogs() {
    _uiState.update { it.copy(consoleLogs = emptyList()) }
  }

  fun appendConsoleLog(level: String, message: String, lineNumber: Int) {
    val entry =
      ConsoleLogEntry(
        id = nextLogId++,
        level = level,
        message = message,
        lineNumber = lineNumber,
      )
    _uiState.update { state ->
      val updated = (listOf(entry) + state.consoleLogs).take(100)
      state.copy(consoleLogs = updated)
    }
  }

  fun updateProgress(progress: Int) {
    _uiState.update {
      it.copy(
        loadProgress = progress,
        isPageLoading = progress < 100,
      )
    }
  }

  fun onPageStarted() {
    _uiState.update {
      it.copy(
        isPageLoading = true,
        hasMainFrameError = false,
        errorDescription = "",
      )
    }
  }

  fun onPageFinished() {
    _uiState.update {
      it.copy(
        isPageLoading = false,
        loadProgress = 100,
      )
    }
  }

  fun onPageTitleReceived(title: String?) {
    val clean = title?.trim().orEmpty()
    if (clean.isNotEmpty() && !clean.startsWith("https://")) {
      _uiState.update { it.copy(pageTitle = clean) }
    }
  }

  fun onMainFrameError(description: String) {
    _uiState.update {
      it.copy(
        isPageLoading = false,
        hasMainFrameError = true,
        errorDescription = description,
      )
    }
  }

  fun loadPickedHtmlFile(fileName: String, htmlContent: String) {
    val editorSafeContent =
      if (htmlContent.length <= MAX_EDITOR_CHARS) {
        htmlContent
      } else {
        _uiState.value.editorText
      }
    _uiState.update {
      it.copy(
        source = HtmlSource.CustomHtml(title = fileName, content = htmlContent),
        editorText = editorSafeContent,
        pageTitle = fileName,
        activeTab = RendererTab.PREVIEW,
        hasMainFrameError = false,
        consoleLogs = emptyList(),
      )
    }
  }

  fun updateEditorText(newText: String) {
    _uiState.update { it.copy(editorText = newText) }
  }

  fun setAutoLiveRender(enabled: Boolean) {
    _uiState.update { it.copy(autoLiveRender = enabled) }
  }

  fun applyEditorHtml() {
    val currentText = _uiState.value.editorText
    _uiState.update {
      it.copy(
        source = HtmlSource.CustomHtml(title = "Editor HTML Interaktif", content = currentText),
        pageTitle = "Editor HTML Interaktif",
        hasMainFrameError = false,
      )
    }
  }

  fun resetToBundledSiwarta() {
    _uiState.update {
      it.copy(
        source = HtmlSource.BundledSiwarta,
        pageTitle = "SIWARTA - Perumahan Tamaland",
        activeTab = RendererTab.PREVIEW,
        hasMainFrameError = false,
        consoleLogs = emptyList(),
      )
    }
  }

  companion object {
    private const val MAX_EDITOR_CHARS = 180_000

    val DEFAULT_INTERACTIVE_HTML_TEMPLATE =
      """
      <!DOCTYPE html>
      <html lang="id">
      <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <title>HTML Interaktif</title>
        <style>
          * { box-sizing: border-box; font-family: system-ui, -apple-system, sans-serif; }
          body { margin: 0; padding: 20px; background: #0f172a; color: #f8fafc; }
          .card { background: #1e293b; border: 1px solid #334155; border-radius: 16px; padding: 18px; max-width: 480px; margin: 0 auto 16px; }
          h2 { margin: 0 0 6px; font-size: 18px; color: #38bdf8; }
          p { margin: 0 0 12px; font-size: 13px; color: #cbd5e1; }
          label { display: block; font-size: 12px; font-weight: 600; margin-bottom: 6px; color: #94a3b8; }
          input, select { width: 100%; padding: 10px 12px; border-radius: 10px; border: 1px solid #475569; background: #0f172a; color: #fff; font-size: 14px; margin-bottom: 12px; }
          .row { display: flex; gap: 8px; flex-wrap: wrap; }
          button { flex: 1; padding: 11px 14px; border: none; border-radius: 10px; font-weight: 700; font-size: 13px; cursor: pointer; background: #0284c7; color: #fff; }
          button.secondary { background: #10b981; color: #052e16; }
          button.danger { background: #e11d48; }
          .output { margin-top: 12px; padding: 12px; border-radius: 10px; background: #0f172a; border: 1px dashed #38bdf8; font-size: 13px; }
          ul { padding-left: 18px; margin: 8px 0 0; }
          li { margin-bottom: 6px; }
        </style>
      </head>
      <body>
        <div class="card">
          <h2>Pratinjau HTML Interaktif</h2>
          <p>Ubah kode di tab Editor atau isi kolom di bawah ini untuk melihat pembaruan real-time.</p>
          <label for="namaInput">Nama / Catatan</label>
          <input id="namaInput" type="text" placeholder="Ketik nama atau catatan..." oninput="updatePreview()" />
          <label for="kategoriSelect">Kategori</label>
          <select id="kategoriSelect" onchange="updatePreview()">
            <option value="Warga">Warga</option>
            <option value="Pengurus">Pengurus</option>
            <option value="Tamu">Tamu</option>
          </select>
          <div class="row">
            <button type="button" onclick="tambahItem()">Tambah ke Daftar</button>
            <button type="button" class="secondary" onclick="hitungKlik()">Klik (+1): <span id="counter">0</span></button>
          </div>
          <div class="output" id="liveOutput">Menunggu input Anda...</div>
        </div>
        <div class="card">
          <h2>Daftar Real-Time</h2>
          <ul id="itemList"></ul>
        </div>
        <script>
          var count = 0;
          function updatePreview() {
            var nama = document.getElementById('namaInput').value.trim() || '-';
            var kat = document.getElementById('kategoriSelect').value;
            document.getElementById('liveOutput').textContent = 'Halo ' + nama + ' (' + kat + ') • Terakhir diperbarui: ' + new Date().toLocaleTimeString('id-ID');
          }
          function tambahItem() {
            var input = document.getElementById('namaInput');
            var kat = document.getElementById('kategoriSelect').value;
            var val = input.value.trim();
            if (!val) { alert('Silakan isi kolom nama/catatan terlebih dahulu.'); return; }
            var li = document.createElement('li');
            li.textContent = '[' + kat + '] ' + val;
            document.getElementById('itemList').prepend(li);
            console.log('Ditambahkan:', val, kat);
            input.value = '';
            updatePreview();
          }
          function hitungKlik() {
            count += 1;
            document.getElementById('counter').textContent = count;
            console.log('Counter sekarang:', count);
          }
        </script>
      </body>
      </html>
      """.trimIndent()
  }
}
