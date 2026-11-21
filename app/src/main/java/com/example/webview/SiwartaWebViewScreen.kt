package com.example.webview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Message
import android.provider.OpenableColumns
import android.view.MotionEvent
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notification.ReminderScheduleConfig
import com.example.notification.SiwartaNotificationHelper
import com.example.ui.theme.BrandBlue400
import com.example.ui.theme.BrandBlue600
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Navy950
import com.example.ui.theme.Rose500
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SIWARTA_LOCAL_HOST = "siwarta.tamaland.local"
private const val SIWARTA_ASSET_FILE = "Siwarta_non_Air_9fc435eb.html"
private const val CUSTOM_PREVIEW_PATH = "custom_preview.html"
private const val SIWARTA_ENTRY_URL = "https://$SIWARTA_LOCAL_HOST/$SIWARTA_ASSET_FILE"

private enum class JsDialogType {
  ALERT,
  CONFIRM,
  PROMPT,
}

private data class JsDialogState(
  val type: JsDialogType,
  val message: String,
  val defaultValue: String = "",
  val jsResult: JsResult? = null,
  val jsPromptResult: JsPromptResult? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun SiwartaWebViewScreen(
  onExitRequested: () -> Unit,
  viewModel: HtmlRendererViewModel = viewModel(),
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  val latestSourceRef = remember { AtomicReference<HtmlSource>(HtmlSource.BundledSiwarta) }
  latestSourceRef.set(uiState.source)

  var lastBackPressTime by remember { mutableLongStateOf(0L) }
  var showOverflowMenu by remember { mutableStateOf(false) }

  var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
  var pendingCameraPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
  var pendingCaptureFileChooser by remember { mutableStateOf(false) }
  var activeJsDialog by remember { mutableStateOf<JsDialogState?>(null) }
  var jsPromptInput by remember(activeJsDialog) {
    mutableStateOf(activeJsDialog?.defaultValue.orEmpty())
  }

  val openHtmlFileLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri == null) return@rememberLauncherForActivityResult
      runCatching {
          val fileName = resolveFileDisplayName(context, uri) ?: "Dokumen.html"
          val htmlText =
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
              it.readText()
            } ?: ""
          if (htmlText.isBlank()) {
            coroutineScope.launch {
              snackbarHostState.showSnackbar("File HTML kosong atau tidak dapat dibaca.")
            }
          } else {
            viewModel.loadPickedHtmlFile(fileName, htmlText)
            coroutineScope.launch {
              snackbarHostState.showSnackbar("Berhasil memuat: $fileName")
            }
          }
        }
        .onFailure { err ->
          coroutineScope.launch {
            snackbarHostState.showSnackbar("Gagal membaca file HTML: ${err.message}")
          }
        }
    }

  val openDocumentLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
      pendingFileCallback?.onReceiveValue(uris.toTypedArray())
      pendingFileCallback = null
    }

  val takePictureLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
      val captured = pendingCameraUri
      if (success && captured != null) {
        pendingFileCallback?.onReceiveValue(arrayOf(captured))
      } else {
        pendingFileCallback?.onReceiveValue(null)
      }
      pendingFileCallback = null
      pendingCameraUri = null
    }

  val cameraPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      val webRequest = pendingCameraPermissionRequest
      if (webRequest != null) {
        if (granted) {
          webRequest.grant(webRequest.resources)
        } else {
          webRequest.deny()
        }
        pendingCameraPermissionRequest = null
      }

      if (pendingCaptureFileChooser) {
        pendingCaptureFileChooser = false
        if (granted) {
          val cameraUri = createTempCameraUri(context)
          pendingCameraUri = cameraUri
          takePictureLauncher.launch(cameraUri)
        } else {
          openDocumentLauncher.launch(arrayOf("image/jpeg", "image/png", "image/*"))
        }
      }
    }

  val webViewRef = remember { AtomicReference<WebView?>(null) }

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      webViewRef.get()?.post {
        webViewRef.get()?.evaluateJavascript(
          "window.__siwartaOnNotificationPermissionResult && window.__siwartaOnNotificationPermissionResult($granted);",
          null,
        )
      }
      coroutineScope.launch {
        snackbarHostState.showSnackbar(
          if (granted) {
            "Notifikasi pengingat jatuh tempo iuran aktif."
          } else {
            "Izin notifikasi ditolak. Anda dapat mengaktifkannya di pengaturan aplikasi."
          }
        )
      }
    }

  val requestPushNotificationPermission: () -> Unit = {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (SiwartaNotificationHelper.isNotificationPermissionGranted(context)) {
        webViewRef.get()?.post {
          webViewRef.get()?.evaluateJavascript(
            "window.__siwartaOnNotificationPermissionResult && window.__siwartaOnNotificationPermissionResult(true);",
            null,
          )
        }
      } else {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    } else {
      webViewRef.get()?.post {
        webViewRef.get()?.evaluateJavascript(
          "window.__siwartaOnNotificationPermissionResult && window.__siwartaOnNotificationPermissionResult(true);",
          null,
        )
      }
    }
  }

  LaunchedEffect(Unit) {
    SiwartaNotificationHelper.ensureNotificationChannel(context)
  }

  val handleDownloadSaved: (SavedExportResult) -> Unit = { saved ->
    coroutineScope.launch {
      val statusMsg =
        if (saved.savedToDownloads) {
          "Tersimpan di Downloads/SIWARTA: ${saved.fileName}"
        } else {
          "Siap dibuka: ${saved.fileName}"
        }
      val result =
        snackbarHostState.showSnackbar(
          message = statusMsg,
          actionLabel = "Buka",
          duration = SnackbarDuration.Long,
        )
      if (result == SnackbarResult.ActionPerformed) {
        SiwartaDownloadHelper.openExportedFile(context, saved)
      }
    }
  }

  val webView = remember {
    WebView(context).apply {
      setBackgroundColor(android.graphics.Color.parseColor("#070D1E"))
      isFocusable = true
      isFocusableInTouchMode = true
      setOnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_DOWN && !v.hasFocus()) {
          v.requestFocus()
        }
        false
      }

      settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        allowFileAccess = true
        allowContentAccess = true
        mediaPlaybackRequiresUserGesture = false
        useWideViewPort = true
        loadWithOverviewMode = true
        builtInZoomControls = false
        displayZoomControls = false
        setSupportMultipleWindows(true)
        javaScriptCanOpenWindowsAutomatically = true
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
      }

      addJavascriptInterface(
        SiwartaJsBridge(
          context = context,
          onSaved = { result -> post { handleDownloadSaved(result) } },
          onError = { message ->
            post {
              coroutineScope.launch {
                snackbarHostState.showSnackbar("Gagal menyimpan file: $message")
              }
            }
          },
          onRequestNotificationPermission = {
            post { requestPushNotificationPermission() }
          },
        ),
        "SiwartaAndroidBridge",
      )

      setDownloadListener { url, _, contentDisposition, mimeType, _ ->
        val guessedName =
          extractFileNameFromDisposition(contentDisposition)
            ?: "SIWARTA_${System.currentTimeMillis()}"
        if (url.startsWith("data:", ignoreCase = true)) {
          SiwartaDownloadHelper.saveBase64Export(context, url, guessedName, mimeType)
            .onSuccess(handleDownloadSaved)
        } else if (url.startsWith("blob:", ignoreCase = true)) {
          val safeName = guessedName.replace("'", "\\'")
          val safeMime = (mimeType ?: "application/octet-stream").replace("'", "\\'")
          evaluateJavascript(
            "window.__siwartaFetchBlobToAndroid && window.__siwartaFetchBlobToAndroid('$url', '$safeName', '$safeMime');",
            null,
          )
        } else {
          openExternalUri(context, Uri.parse(url))
        }
      }

      webViewClient =
        object : WebViewClient() {
          override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?,
          ): WebResourceResponse? {
            val uri = request?.url ?: return null
            if (uri.host == SIWARTA_LOCAL_HOST) {
              val path = uri.path?.trimStart('/') ?: ""
              if (path == CUSTOM_PREVIEW_PATH) {
                val activeSource = latestSourceRef.get()
                if (activeSource is HtmlSource.CustomHtml) {
                  val bytes = activeSource.content.toByteArray(Charsets.UTF_8)
                  return WebResourceResponse("text/html", "UTF-8", ByteArrayInputStream(bytes))
                }
              }
              if (path == SIWARTA_ASSET_FILE || path.isEmpty()) {
                return runCatching {
                    val stream = context.assets.open(SIWARTA_ASSET_FILE)
                    WebResourceResponse("text/html", "UTF-8", stream)
                  }
                  .getOrNull()
              }
              if (path == "favicon.ico") {
                return WebResourceResponse(
                  "image/x-icon",
                  "UTF-8",
                  204,
                  "No Content",
                  emptyMap(),
                  ByteArrayInputStream(ByteArray(0)),
                )
              }
            }
            return super.shouldInterceptRequest(view, request)
          }

          override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?,
          ): Boolean {
            val uri = request?.url ?: return false
            val scheme = uri.scheme?.lowercase() ?: ""
            if (uri.host == SIWARTA_LOCAL_HOST) return false
            if (scheme == "http" || scheme == "https") {
              openExternalUri(context, uri)
              return true
            }
            if (
              scheme == "tel" ||
                scheme == "mailto" ||
                scheme == "whatsapp" ||
                scheme == "intent"
            ) {
              openExternalUri(context, uri)
              return true
            }
            return false
          }

          override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            viewModel.onPageStarted()
          }

          override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            viewModel.onPageFinished()
            view?.evaluateJavascript(SIWARTA_BRIDGE_JS, null)
          }

          override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?,
          ) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame == true) {
              viewModel.onMainFrameError(
                error?.description?.toString() ?: "Tidak dapat memuat halaman HTML."
              )
            }
          }
        }

      webChromeClient =
        object : WebChromeClient() {
          override fun onProgressChanged(view: WebView?, newProgress: Int) {
            viewModel.updateProgress(newProgress)
          }

          override fun onReceivedTitle(view: WebView?, title: String?) {
            viewModel.onPageTitleReceived(title)
          }

          override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            if (consoleMessage != null) {
              viewModel.appendConsoleLog(
                level = consoleMessage.messageLevel().name,
                message = consoleMessage.message(),
                lineNumber = consoleMessage.lineNumber(),
              )
            }
            return true
          }

          override fun onJsAlert(
            view: WebView?,
            url: String?,
            message: String?,
            result: JsResult?,
          ): Boolean {
            activeJsDialog =
              JsDialogState(
                type = JsDialogType.ALERT,
                message = message.orEmpty(),
                jsResult = result,
              )
            return true
          }

          override fun onJsConfirm(
            view: WebView?,
            url: String?,
            message: String?,
            result: JsResult?,
          ): Boolean {
            activeJsDialog =
              JsDialogState(
                type = JsDialogType.CONFIRM,
                message = message.orEmpty(),
                jsResult = result,
              )
            return true
          }

          override fun onJsPrompt(
            view: WebView?,
            url: String?,
            message: String?,
            defaultValue: String?,
            result: JsPromptResult?,
          ): Boolean {
            activeJsDialog =
              JsDialogState(
                type = JsDialogType.PROMPT,
                message = message.orEmpty(),
                defaultValue = defaultValue.orEmpty(),
                jsPromptResult = result,
              )
            return true
          }

          override fun onPermissionRequest(request: PermissionRequest?) {
            if (request == null) return
            val wantsCamera =
              request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
            if (wantsCamera) {
              val hasCamPerm =
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                  PackageManager.PERMISSION_GRANTED
              if (hasCamPerm) {
                request.grant(request.resources)
              } else {
                pendingCameraPermissionRequest = request
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
              }
            } else {
              request.deny()
            }
          }

          override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?,
          ): Boolean {
            val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
            val tempWebView =
              WebView(context).apply {
                webViewClient =
                  object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                      v: WebView?,
                      req: WebResourceRequest?,
                    ): Boolean {
                      req?.url?.let { openExternalUri(context, it) }
                      destroy()
                      return true
                    }
                  }
              }
            transport.webView = tempWebView
            resultMsg.sendToTarget()
            return true
          }

          override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?,
          ): Boolean {
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = filePathCallback

            if (fileChooserParams?.isCaptureEnabled == true) {
              val hasCamPerm =
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                  PackageManager.PERMISSION_GRANTED
              if (hasCamPerm) {
                val cameraUri = createTempCameraUri(context)
                pendingCameraUri = cameraUri
                takePictureLauncher.launch(cameraUri)
              } else {
                pendingCaptureFileChooser = true
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
              }
              return true
            }

            val mimeTypes = resolveAcceptedMimeTypes(fileChooserParams?.acceptTypes)
            openDocumentLauncher.launch(mimeTypes)
            return true
          }
        }
    }.also { webViewRef.set(it) }
  }

  var lastLoadedSourceKey by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(uiState.source) {
    val targetUrl =
      when (val src = uiState.source) {
        is HtmlSource.BundledSiwarta -> SIWARTA_ENTRY_URL
        is HtmlSource.CustomHtml -> "https://$SIWARTA_LOCAL_HOST/$CUSTOM_PREVIEW_PATH?rev=${src.revision}"
      }
    if (lastLoadedSourceKey != targetUrl) {
      lastLoadedSourceKey = targetUrl
      webView.loadUrl(targetUrl)
    }
  }

  // Real-time auto-render when editing HTML in live editor
  LaunchedEffect(uiState.editorText, uiState.autoLiveRender, uiState.activeTab) {
    if (uiState.activeTab == RendererTab.EDITOR && uiState.autoLiveRender) {
      delay(450L)
      viewModel.applyEditorHtml()
    }
  }

  DisposableEffect(webView) {
    onDispose {
      activeJsDialog?.jsResult?.cancel()
      activeJsDialog?.jsPromptResult?.cancel()
      pendingFileCallback?.onReceiveValue(null)
      pendingFileCallback = null
      webView.stopLoading()
      webView.destroy()
    }
  }

  BackHandler {
    if (uiState.showConsoleSheet) {
      viewModel.toggleConsoleSheet(false)
      return@BackHandler
    }
    if (uiState.activeTab == RendererTab.EDITOR) {
      viewModel.selectTab(RendererTab.PREVIEW)
      return@BackHandler
    }
    if (uiState.isFullscreen) {
      viewModel.toggleFullscreen(false)
      return@BackHandler
    }

    webView.evaluateJavascript(
      "(function(){ return typeof window.__siwartaHandleAndroidBack === 'function' ? window.__siwartaHandleAndroidBack() : false; })();"
    ) { result ->
      if (result == "true") return@evaluateJavascript

      if (webView.canGoBack()) {
        webView.goBack()
      } else {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 2000L) {
          onExitRequested()
        } else {
          lastBackPressTime = now
          coroutineScope.launch {
            snackbarHostState.showSnackbar(
              message = "Tekan kembali sekali lagi untuk keluar",
              duration = SnackbarDuration.Short,
            )
          }
        }
      }
    }
  }

  Scaffold(
    containerColor = Navy900,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      if (!uiState.isFullscreen) {
        RendererTopBar(
          title = uiState.pageTitle,
          isCustomSource = uiState.source is HtmlSource.CustomHtml,
          activeTab = uiState.activeTab,
          consoleCount = uiState.consoleLogs.size,
          showOverflowMenu = showOverflowMenu,
          onSelectTab = { tab -> viewModel.selectTab(tab) },
          onOpenHtmlFile = {
            openHtmlFileLauncher.launch(arrayOf("text/html", "application/xhtml+xml", "text/plain"))
          },
          onReload = { webView.reload() },
          onToggleOverflow = { showOverflowMenu = it },
          onResetSiwarta = {
            showOverflowMenu = false
            viewModel.resetToBundledSiwarta()
          },
          onOpenConsole = {
            showOverflowMenu = false
            viewModel.toggleConsoleSheet(true)
          },
          onEnterFullscreen = {
            showOverflowMenu = false
            viewModel.selectTab(RendererTab.PREVIEW)
            viewModel.toggleFullscreen(true)
          },
        )
      }
    },
    snackbarHost = {
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.testTag("download_snackbar_host"),
      )
    },
  ) { innerPadding ->
    BoxWithConstraints(
      modifier =
        Modifier.fillMaxSize()
          .background(Navy900)
          .padding(innerPadding)
    ) {
      val isWideScreen = maxWidth >= 840.dp

      if (isWideScreen && uiState.activeTab == RendererTab.EDITOR && !uiState.isFullscreen) {
        Row(modifier = Modifier.fillMaxSize()) {
          LiveHtmlEditorPane(
            editorText = uiState.editorText,
            autoLiveRender = uiState.autoLiveRender,
            showSwitchToPreview = false,
            onTextChange = viewModel::updateEditorText,
            onToggleAutoLive = viewModel::setAutoLiveRender,
            onApplyClick = { viewModel.applyEditorHtml() },
            onApplyAndPreview = {
              viewModel.applyEditorHtml()
              viewModel.selectTab(RendererTab.PREVIEW)
            },
            modifier = Modifier.weight(1f).fillMaxHeight(),
          )
          Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            InteractiveWebViewPane(
              webView = webView,
              isPageLoading = uiState.isPageLoading,
              loadProgress = uiState.loadProgress,
              hasMainFrameError = uiState.hasMainFrameError,
              errorDescription = uiState.errorDescription,
              isFullscreen = false,
              onReload = { webView.reload() },
              onExitFullscreen = { viewModel.toggleFullscreen(false) },
            )
          }
        }
      } else {
        Box(modifier = Modifier.fillMaxSize()) {
          InteractiveWebViewPane(
            webView = webView,
            isPageLoading = uiState.isPageLoading,
            loadProgress = uiState.loadProgress,
            hasMainFrameError = uiState.hasMainFrameError,
            errorDescription = uiState.errorDescription,
            isFullscreen = uiState.isFullscreen,
            onReload = { webView.reload() },
            onExitFullscreen = { viewModel.toggleFullscreen(false) },
          )

          if (uiState.activeTab == RendererTab.EDITOR && !uiState.isFullscreen) {
            LiveHtmlEditorPane(
              editorText = uiState.editorText,
              autoLiveRender = uiState.autoLiveRender,
              showSwitchToPreview = true,
              onTextChange = viewModel::updateEditorText,
              onToggleAutoLive = viewModel::setAutoLiveRender,
              onApplyClick = { viewModel.applyEditorHtml() },
              onApplyAndPreview = {
                viewModel.applyEditorHtml()
                viewModel.selectTab(RendererTab.PREVIEW)
              },
              modifier = Modifier.fillMaxSize(),
            )
          }
        }
      }
    }
  }

  // Material 3 JS Dialog (alert, confirm, prompt)
  val dialog = activeJsDialog
  if (dialog != null) {
    AlertDialog(
      onDismissRequest = {
        dialog.jsResult?.cancel()
        dialog.jsPromptResult?.cancel()
        activeJsDialog = null
      },
      title = {
        Text(
          text =
            when (dialog.type) {
              JsDialogType.ALERT -> "Informasi Halaman"
              JsDialogType.CONFIRM -> "Konfirmasi"
              JsDialogType.PROMPT -> "Masukkan Nilai"
            },
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(text = dialog.message)
          if (dialog.type == JsDialogType.PROMPT) {
            OutlinedTextField(
              value = jsPromptInput,
              onValueChange = { jsPromptInput = it },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("js_prompt_input"),
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            when (dialog.type) {
              JsDialogType.ALERT,
              JsDialogType.CONFIRM -> dialog.jsResult?.confirm()
              JsDialogType.PROMPT -> dialog.jsPromptResult?.confirm(jsPromptInput)
            }
            activeJsDialog = null
          },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("js_dialog_confirm"),
        ) {
          Text("OK")
        }
      },
      dismissButton = {
        if (dialog.type != JsDialogType.ALERT) {
          TextButton(
            onClick = {
              dialog.jsResult?.cancel()
              dialog.jsPromptResult?.cancel()
              activeJsDialog = null
            },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("js_dialog_cancel"),
          ) {
            Text("Batal")
          }
        }
      },
    )
  }

  // Real-time JS Console Log Sheet
  if (uiState.showConsoleSheet) {
    ModalBottomSheet(
      onDismissRequest = { viewModel.toggleConsoleSheet(false) },
      containerColor = Slate900,
      contentColor = Color.White,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Terminal,
              contentDescription = null,
              tint = BrandBlue400,
            )
            Text(
              text = "Konsol JavaScript Real-Time",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
          TextButton(
            onClick = { viewModel.clearConsoleLogs() },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("clear_console_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.DeleteSweep,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Bersihkan")
          }
        }

        if (uiState.consoleLogs.isEmpty()) {
          Surface(
            color = Slate800,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
          ) {
            Text(
              text = "Belum ada log konsol dari halaman HTML yang dirender.",
              color = Color(0xFF94A3B8),
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(16.dp),
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxWidth().height(280.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(uiState.consoleLogs, key = { it.id }) { log ->
              val levelColor =
                when (log.level) {
                  "ERROR" -> Rose500
                  "WARNING" -> Color(0xFFF59E0B)
                  else -> Emerald500
                }
              Surface(
                color = Slate800,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text(
                    text = "${log.level} • Baris ${log.lineNumber}",
                    color = levelColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = log.message,
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RendererTopBar(
  title: String,
  isCustomSource: Boolean,
  activeTab: RendererTab,
  consoleCount: Int,
  showOverflowMenu: Boolean,
  onSelectTab: (RendererTab) -> Unit,
  onOpenHtmlFile: () -> Unit,
  onReload: () -> Unit,
  onToggleOverflow: (Boolean) -> Unit,
  onResetSiwarta: () -> Unit,
  onOpenConsole: () -> Unit,
  onEnterFullscreen: () -> Unit,
) {
  TopAppBar(
    colors =
      TopAppBarDefaults.topAppBarColors(
        containerColor = Navy950,
        titleContentColor = Color.White,
        actionIconContentColor = Color.White,
      ),
    title = {
      Column {
        Text(
          text = title,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = if (isCustomSource) "HTML Interaktif Kustom" else "SIWARTA • HTML Tersemat",
          maxLines = 1,
          style = MaterialTheme.typography.labelSmall,
          color = BrandBlue400,
        )
      }
    },
    actions = {
      SingleChoiceSegmentedButtonRow(
        modifier = Modifier.padding(end = 4.dp),
      ) {
        SegmentedButton(
          selected = activeTab == RendererTab.PREVIEW,
          onClick = { onSelectTab(RendererTab.PREVIEW) },
          shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
          colors =
            SegmentedButtonDefaults.colors(
              activeContainerColor = BrandBlue600,
              activeContentColor = Color.White,
              inactiveContainerColor = Navy800,
              inactiveContentColor = Color(0xFF94A3B8),
            ),
          icon = {
            Icon(
              imageVector = Icons.Rounded.Visibility,
              contentDescription = "Mode Pratinjau",
              modifier = Modifier.size(16.dp),
            )
          },
          modifier = Modifier.testTag("tab_preview_button"),
        ) {
          Text("Render", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        SegmentedButton(
          selected = activeTab == RendererTab.EDITOR,
          onClick = { onSelectTab(RendererTab.EDITOR) },
          shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
          colors =
            SegmentedButtonDefaults.colors(
              activeContainerColor = BrandBlue600,
              activeContentColor = Color.White,
              inactiveContainerColor = Navy800,
              inactiveContentColor = Color(0xFF94A3B8),
            ),
          icon = {
            Icon(
              imageVector = Icons.Rounded.Code,
              contentDescription = "Mode Editor HTML",
              modifier = Modifier.size(16.dp),
            )
          },
          modifier = Modifier.testTag("tab_editor_button"),
        ) {
          Text("Editor", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
      }

      IconButton(
        onClick = onOpenHtmlFile,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("open_html_file_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.FolderOpen,
          contentDescription = "Buka file HTML dari perangkat",
        )
      }

      Box {
        IconButton(
          onClick = { onToggleOverflow(true) },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("more_options_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = "Menu lainnya",
          )
        }
        DropdownMenu(
          expanded = showOverflowMenu,
          onDismissRequest = { onToggleOverflow(false) },
        ) {
          DropdownMenuItem(
            text = { Text("Muat Ulang Halaman") },
            leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
            onClick = {
              onToggleOverflow(false)
              onReload()
            },
          )
          DropdownMenuItem(
            text = { Text("Reset ke SIWARTA") },
            leadingIcon = { Icon(Icons.Rounded.RestartAlt, contentDescription = null) },
            onClick = onResetSiwarta,
          )
          DropdownMenuItem(
            text = { Text("Konsol JS ($consoleCount)") },
            leadingIcon = { Icon(Icons.Rounded.Terminal, contentDescription = null) },
            onClick = onOpenConsole,
          )
          DropdownMenuItem(
            text = { Text("Mode Layar Penuh") },
            leadingIcon = { Icon(Icons.Rounded.Fullscreen, contentDescription = null) },
            onClick = onEnterFullscreen,
          )
        }
      }
    },
  )
}

@Composable
private fun InteractiveWebViewPane(
  webView: WebView,
  isPageLoading: Boolean,
  loadProgress: Int,
  hasMainFrameError: Boolean,
  errorDescription: String,
  isFullscreen: Boolean,
  onReload: () -> Unit,
  onExitFullscreen: () -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize()) {
    AndroidView(
      factory = { webView },
      modifier = Modifier.fillMaxSize().testTag("siwarta_webview"),
    )

    AnimatedVisibility(
      visible = isPageLoading && loadProgress in 1..99,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.TopCenter),
    ) {
      LinearProgressIndicator(
        progress = { loadProgress / 100f },
        modifier = Modifier.fillMaxWidth().height(3.dp).testTag("webview_progress"),
        color = BrandBlue600,
        trackColor = Slate900,
      )
    }

    if (isFullscreen) {
      FilledTonalIconButton(
        onClick = onExitFullscreen,
        colors =
          IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = Navy950.copy(alpha = 0.85f),
            contentColor = Color.White,
          ),
        modifier =
          Modifier.align(Alignment.BottomEnd)
            .padding(16.dp)
            .minimumInteractiveComponentSize()
            .testTag("exit_fullscreen_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.FullscreenExit,
          contentDescription = "Keluar dari layar penuh",
        )
      }
    }

    AnimatedVisibility(
      visible = hasMainFrameError,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize(),
    ) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(Slate900)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Card(
          modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = Slate800),
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.WifiOff,
              contentDescription = "Halaman gagal dimuat",
              tint = BrandBlue400,
              modifier = Modifier.size(48.dp),
            )
            Text(
              text = "Gagal Memuat Halaman HTML",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              textAlign = TextAlign.Center,
            )
            Text(
              text =
                errorDescription.ifBlank {
                  "Periksa koneksi internet Anda lalu coba muat ulang."
                },
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF94A3B8),
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
              onClick = onReload,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("reload_webview_button"),
              colors = ButtonDefaults.buttonColors(containerColor = BrandBlue600),
              shape = RoundedCornerShape(14.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = "Muat Ulang",
                modifier = Modifier.padding(start = 8.dp),
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LiveHtmlEditorPane(
  editorText: String,
  autoLiveRender: Boolean,
  showSwitchToPreview: Boolean,
  onTextChange: (String) -> Unit,
  onToggleAutoLive: (Boolean) -> Unit,
  onApplyClick: () -> Unit,
  onApplyAndPreview: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.background(Slate900).padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Surface(
      color = Navy800,
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Switch(
            checked = autoLiveRender,
            onCheckedChange = onToggleAutoLive,
            modifier = Modifier.testTag("auto_live_render_switch"),
          )
          Text(
            text = "Render Real-Time",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (!showSwitchToPreview) {
            OutlinedButton(
              onClick = onApplyClick,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("apply_html_button"),
            ) {
              Text("Terapkan")
            }
          } else {
            Button(
              onClick = onApplyAndPreview,
              colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy950),
              modifier =
                Modifier.minimumInteractiveComponentSize().testTag("apply_and_preview_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Render", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    HorizontalDivider(color = Slate800)

    OutlinedTextField(
      value = editorText,
      onValueChange = onTextChange,
      label = { Text("Kode HTML / CSS / JavaScript") },
      textStyle =
        TextStyle(
          fontFamily = FontFamily.Monospace,
          fontSize = 13.sp,
          lineHeight = 19.sp,
          color = Color(0xFFE2E8F0),
        ),
      colors =
        OutlinedTextFieldDefaults.colors(
          focusedContainerColor = Navy950,
          unfocusedContainerColor = Navy950,
          focusedBorderColor = BrandBlue600,
          unfocusedBorderColor = Slate800,
          focusedLabelColor = BrandBlue400,
          unfocusedLabelColor = Color(0xFF94A3B8),
          cursorColor = BrandBlue400,
        ),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxSize().weight(1f).testTag("html_editor_input"),
    )
  }
}

private class SiwartaJsBridge(
  private val context: Context,
  private val onSaved: (SavedExportResult) -> Unit,
  private val onError: (String) -> Unit,
  private val onRequestNotificationPermission: () -> Unit,
) {
  @JavascriptInterface
  fun saveBase64File(dataUrl: String, fileName: String, mimeType: String) {
    SiwartaDownloadHelper.saveBase64Export(context, dataUrl, fileName, mimeType)
      .onSuccess(onSaved)
      .onFailure { err -> onError(err.message ?: "Error tidak diketahui") }
  }

  @JavascriptInterface
  fun isNotificationPermissionGranted(): Boolean {
    return SiwartaNotificationHelper.isNotificationPermissionGranted(context)
  }

  @JavascriptInterface
  fun requestNotificationPermission() {
    onRequestNotificationPermission()
  }

  @JavascriptInterface
  fun sendPaymentReminderNotification(title: String, message: String, tag: String): Boolean {
    return SiwartaNotificationHelper.showPaymentReminderNotification(
      context = context,
      title = title,
      message = message,
      tagKey = tag.ifBlank { "siwarta_due_reminder" },
    )
  }

  @JavascriptInterface
  fun syncReminderScheduleConfig(
    enabled: Boolean,
    dueDay: Int,
    daysBefore: Int,
    wargaNoRumah: String,
    wargaNama: String,
    unpaidTitle: String,
    unpaidMessage: String,
    hasUnpaidDueSoon: Boolean,
    periodeLabel: String,
  ) {
    SiwartaNotificationHelper.saveReminderConfigAndSchedule(
      context = context,
      config =
        ReminderScheduleConfig(
          enabled = enabled,
          dueDay = dueDay,
          daysBefore = daysBefore,
          wargaNoRumah = wargaNoRumah,
          wargaNama = wargaNama,
          unpaidTitle = unpaidTitle,
          unpaidMessage = unpaidMessage,
          hasUnpaidDueSoon = hasUnpaidDueSoon,
          periodeLabel = periodeLabel,
        ),
    )
  }
}

private fun resolveFileDisplayName(context: Context, uri: Uri): String? {
  context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
    cursor ->
    if (cursor.moveToFirst()) {
      val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (idx >= 0) return cursor.getString(idx)
    }
  }
  return uri.lastPathSegment
}

private fun createTempCameraUri(context: Context): Uri {
  val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
  val photoFile = File(cameraDir, "selfie_${System.currentTimeMillis()}.jpg")
  return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
}

private fun resolveAcceptedMimeTypes(acceptTypes: Array<String>?): Array<String> {
  if (acceptTypes.isNullOrEmpty()) return arrayOf("*/*")
  val mapped =
    acceptTypes
      .flatMap { it.split(',') }
      .map { it.trim().lowercase() }
      .filter { it.isNotEmpty() }
      .flatMap { token ->
        when {
          token == ".xlsx" ->
            listOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
          token == ".xls" -> listOf("application/vnd.ms-excel")
          token == ".json" -> listOf("application/json", "text/plain")
          token.startsWith(".") -> listOf("*/*")
          else -> listOf(token)
        }
      }
      .distinct()
  return if (mapped.isEmpty()) arrayOf("*/*") else mapped.toTypedArray()
}

private fun extractFileNameFromDisposition(disposition: String?): String? {
  if (disposition.isNullOrBlank()) return null
  val regex = Regex("""filename\*?=(?:UTF-8''|")?([^";]+)""", RegexOption.IGNORE_CASE)
  return regex.find(disposition)?.groupValues?.getOrNull(1)?.trim('"')
}

private fun openExternalUri(context: Context, uri: Uri) {
  val intent =
    Intent(Intent.ACTION_VIEW, uri).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  runCatching { context.startActivity(intent) }
}

private val SIWARTA_BRIDGE_JS =
  """
  (function() {
    if (window.__siwartaAndroidBridgeReady) return;
    window.__siwartaAndroidBridgeReady = true;

    window.__siwartaFetchBlobToAndroid = function(blobUrl, fileName, mimeType) {
      fetch(blobUrl)
        .then(function(res) { return res.blob(); })
        .then(function(blob) {
          var reader = new FileReader();
          reader.onloadend = function() {
            if (window.SiwartaAndroidBridge && typeof reader.result === 'string') {
              window.SiwartaAndroidBridge.saveBase64File(
                reader.result,
                fileName || ('SIWARTA_' + Date.now()),
                mimeType || blob.type || 'application/octet-stream'
              );
            }
          };
          reader.readAsDataURL(blob);
        })
        .catch(function() {});
    };

    document.addEventListener('click', function(e) {
      var anchor = e.target && e.target.closest ? e.target.closest('a[download]') : null;
      if (!anchor) return;
      var href = anchor.getAttribute('href') || '';
      var fileName = anchor.getAttribute('download') || ('SIWARTA_' + Date.now());
      if (href.indexOf('blob:') === 0) {
        e.preventDefault();
        window.__siwartaFetchBlobToAndroid(href, fileName, '');
      } else if (href.indexOf('data:') === 0 && window.SiwartaAndroidBridge) {
        e.preventDefault();
        window.SiwartaAndroidBridge.saveBase64File(href, fileName, '');
      }
    }, true);

    var origCreateObjectURL = URL.createObjectURL;
    var blobNameMap = new Map();
    URL.createObjectURL = function(obj) {
      var u = origCreateObjectURL.call(URL, obj);
      if (obj && obj.type) blobNameMap.set(u, obj.type);
      return u;
    };

    var origAppendChild = document.body.appendChild;
    document.body.appendChild = function(node) {
      var res = origAppendChild.call(document.body, node);
      try {
        if (node && node.tagName === 'A' && node.hasAttribute('download')) {
          var href = node.getAttribute('href') || '';
          var fname = node.getAttribute('download') || ('SIWARTA_' + Date.now());
          if (href.indexOf('blob:') === 0) {
            window.__siwartaFetchBlobToAndroid(href, fname, blobNameMap.get(href) || '');
          }
        }
      } catch (err) {}
      return res;
    };

    window.__siwartaHandleAndroidBack = function() {
      var modals = [
        'modalKwitansi', 'modalWarga', 'modalIuran', 'modalSkemaKategori',
        'modalBayarIuran', 'modalEditTransaksi', 'modalPelunasanKekurangan',
        'modalBayarMandiriWarga', 'modalBayarAir', 'modalEditBayarAir',
        'modalCatatMeterAir', 'modalPengeluaran', 'modalBuatPesanAdmin',
        'modalPengurus', 'modalGantiPasswordPengurus', 'modalHakAkses',
        'modalSelfieCamera'
      ];
      for (var i = 0; i < modals.length; i++) {
        var el = document.getElementById(modals[i]);
        if (el && !el.classList.contains('hidden')) {
          if (modals[i] === 'modalSelfieCamera' && typeof window.closeSelfieCameraModal === 'function') {
            window.closeSelfieCameraModal();
          } else {
            el.classList.add('hidden');
          }
          return true;
        }
      }
      var drawer = document.getElementById('mobileDrawer');
      if (drawer && !drawer.classList.contains('hidden') && typeof window.toggleMobileMenu === 'function') {
        window.toggleMobileMenu();
        return true;
      }
      var loginOverlay = document.getElementById('loginOverlay');
      if (loginOverlay && loginOverlay.classList.contains('hidden')) {
        var homeSec = document.getElementById('sec-home-dashboard');
        var portalSec = document.getElementById('sec-warga-portal');
        var isAdminHomeVisible = homeSec && !homeSec.classList.contains('hidden');
        var isPortalVisible = portalSec && !portalSec.classList.contains('hidden');
        if (!isAdminHomeVisible && !isPortalVisible) {
          if (typeof window.currentUserRole !== 'undefined' && window.currentUserRole === 'admin' && typeof window.showHomeDashboard === 'function') {
            window.showHomeDashboard();
            return true;
          }
          if (typeof window.currentUserRole !== 'undefined' && window.currentUserRole === 'warga' && typeof window.showSection === 'function') {
            window.showSection('sec-warga-portal');
            return true;
          }
        }
      }
      return false;
    };
  })();
  """.trimIndent()
