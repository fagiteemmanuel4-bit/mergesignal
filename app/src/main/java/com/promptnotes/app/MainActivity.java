package com.promptnotes.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    private static final int AUDIO_PERMISSION_REQUEST = 410;
    private static final String LIVE_URL = "https://prompt-notes-live.vercel.app/index.html";
    private static final String PREFS = "prompt_notes_live";
    private static final String HTML_KEY = "live_html";
    private static final String HASH_KEY = "live_hash";

    private WebView webView;
    private PermissionRequest pendingPermissionRequest;
    private final ExecutorService updateExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean updateCheckRunning = new AtomicBoolean(false);
    private SharedPreferences liveCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        liveCache = getSharedPreferences(PREFS, MODE_PRIVATE);

        getWindow().setStatusBarColor(Color.rgb(251, 250, 248));
        getWindow().setNavigationBarColor(Color.rgb(251, 250, 248));
        getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(251, 250, 248));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setDefaultTextEncodingName("UTF-8");
        settings.setTextZoom(100);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) settings.setSafeBrowsingEnabled(true);

        webView.addJavascriptInterface(new NativeBridge(), "PromptNotesNative");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("file".equals(uri.getScheme()) && uri.getPath() != null
                        && uri.getPath().endsWith("/android_asset/index.html")) {
                    return liveDocument();
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("file".equals(uri.getScheme())) return false;
                if ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (Exception ignored) { }
                }
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                boolean asksForAudio = false;
                for (String resource : request.getResources()) {
                    if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) asksForAudio = true;
                }
                if (!asksForAudio) {
                    request.deny();
                    return;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    pendingPermissionRequest = request;
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION_REQUEST);
                } else {
                    request.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
                }
            }
        });

        setContentView(webView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION_REQUEST);
        }
        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl("file:///android_asset/index.html");
        }
    }

    private WebResourceResponse liveDocument() {
        byte[] html = downloadLiveHtml();
        if (html != null) {
            saveLiveHtml(html);
            return htmlResponse(html);
        }
        String cached = liveCache.getString(HTML_KEY, null);
        if (cached != null && cached.contains("<html")) {
            return htmlResponse(cached.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        // Returning null lets WebView use the packaged offline HTML fallback.
        return null;
    }

    private byte[] downloadLiveHtml() {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(LIVE_URL + "?release=1.1.0&check=" + System.currentTimeMillis());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(8000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Cache-Control", "no-cache, no-store");
            connection.setRequestProperty("Pragma", "no-cache");
            connection.setRequestProperty("User-Agent", "PromptNotesAndroid/1.1.0");
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) return null;
            String type = connection.getContentType();
            if (type != null && !type.toLowerCase().contains("text/html")) return null;
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                    if (output.size() > 2 * 1024 * 1024) return null;
                }
                byte[] result = output.toByteArray();
                String probe = new String(result, java.nio.charset.StandardCharsets.UTF_8);
                return probe.contains("<html") && probe.contains("</html>") ? result : null;
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void saveLiveHtml(byte[] html) {
        String content = new String(html, java.nio.charset.StandardCharsets.UTF_8);
        liveCache.edit().putString(HTML_KEY, content).putString(HASH_KEY, sha256(html)).apply();
    }

    private WebResourceResponse htmlResponse(byte[] html) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Cache-Control", "no-store, no-cache, must-revalidate");
        headers.put("Pragma", "no-cache");
        headers.put("X-Content-Type-Options", "nosniff");
        return new WebResourceResponse("text/html", "UTF-8", 200, "OK", headers,
                new ByteArrayInputStream(html));
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder result = new StringBuilder();
            for (byte b : hash) result.append(String.format(java.util.Locale.US, "%02x", b & 0xff));
            return result.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private void checkForUpdates() {
        if (liveCache == null || !updateCheckRunning.compareAndSet(false, true)) return;
        updateExecutor.execute(() -> {
            try {
                byte[] remote = downloadLiveHtml();
                if (remote == null) return;
                String remoteHash = sha256(remote);
                String currentHash = liveCache.getString(HASH_KEY, "");
                if (!remoteHash.isEmpty() && !remoteHash.equals(currentHash)) {
                    saveLiveHtml(remote);
                    runOnUiThread(() -> {
                        if (webView != null) {
                            webView.evaluateJavascript(
                                    "window.dispatchEvent(new Event('promptnotesupdate'))", null);
                        }
                    });
                }
            } finally {
                updateCheckRunning.set(false);
            }
        });
    }

    private class NativeBridge {
        @android.webkit.JavascriptInterface
        public void share(String title, String content) {
            runOnUiThread(() -> {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_SUBJECT, title == null ? "Prompt Notes" : title);
                send.putExtra(Intent.EXTRA_TEXT, content == null ? "" : content);
                startActivity(Intent.createChooser(send, "Share with"));
            });
        }

        @android.webkit.JavascriptInterface
        public void checkForUpdates() {
            MainActivity.this.checkForUpdates();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkForUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AUDIO_PERMISSION_REQUEST && pendingPermissionRequest != null) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingPermissionRequest.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
            } else {
                pendingPermissionRequest.deny();
            }
            pendingPermissionRequest = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (webView == null) {
            super.onBackPressed();
            return;
        }
        String script = "(function(){"
                + "var sheet=document.querySelector('.sheet.open');"
                + "if(sheet){sheet.classList.remove('open');return 'handled';}"
                + "var editor=document.getElementById('editor');"
                + "if(editor&&!editor.hidden&&typeof home==='function'){home();return 'handled';}"
                + "var settings=document.getElementById('settings');"
                + "if(settings&&!settings.hidden&&typeof show==='function'){show('home');render(1);return 'handled';}"
                + "return 'exit';"
                + "})();";
        webView.evaluateJavascript(script, value -> {
            if ("\"exit\"".equals(value) || "exit".equals(value)) {
                MainActivity.super.onBackPressed();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (pendingPermissionRequest != null) pendingPermissionRequest.deny();
        updateExecutor.shutdownNow();
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.removeJavascriptInterface("PromptNotesNative");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
