package com.axiom.promptnotes;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.webkit.JavascriptInterface;
import android.view.HapticFeedbackConstants;

import androidx.core.content.ContextCompat;

import com.getcapacitor.BridgeActivity;

import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends BridgeActivity {
    private static final int MIC_PERMISSION_REQUEST = 701;
    private NativeBridge nativeBridge;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nativeBridge = new NativeBridge();
        getWindow().getDecorView().setHapticFeedbackEnabled(true);
        getBridge().getWebView().addJavascriptInterface(nativeBridge, "AndroidNative");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                nativeBridge.beginRecognition();
            } else {
                nativeBridge.jsError("not-allowed");
            }
        }
    }

    @Override
    public void onDestroy() {
        if (nativeBridge != null) nativeBridge.destroyRecognizer();
        super.onDestroy();
    }

    private final class NativeBridge {
        private SpeechRecognizer recognizer;
        private String language = "en-US";

        @JavascriptInterface
        public void haptic(final int durationMs) {
            runOnUiThread(() -> {
                try {
                    if (Settings.System.getInt(getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 0) return;
                    int feedback = durationMs >= 35 ? HapticFeedbackConstants.LONG_PRESS : HapticFeedbackConstants.KEYBOARD_TAP;
                    if (getWindow().getDecorView().performHapticFeedback(feedback)) return;
                    Vibrator vibrator;
                    if (Build.VERSION.SDK_INT >= 31) {
                        VibratorManager manager = getSystemService(VibratorManager.class);
                        vibrator = manager == null ? null : manager.getDefaultVibrator();
                    } else {
                        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    }
                    if (vibrator == null || !vibrator.hasVibrator()) return;
                    if (Build.VERSION.SDK_INT >= 29) {
                        int effect = durationMs >= 35 ? VibrationEffect.EFFECT_HEAVY_CLICK : VibrationEffect.EFFECT_TICK;
                        vibrator.vibrate(VibrationEffect.createPredefined(effect));
                    } else if (Build.VERSION.SDK_INT >= 26) {
                        vibrator.vibrate(VibrationEffect.createOneShot(Math.min(Math.max(durationMs, 8), 60), VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(Math.min(Math.max(durationMs, 8), 60));
                    }
                } catch (Exception ignored) { }
            });
        }

        @JavascriptInterface
        public void startSpeech(final String requestedLanguage) {
            runOnUiThread(() -> {
                language = requestedLanguage == null || requestedLanguage.trim().isEmpty() ? "en-US" : requestedLanguage;
                if (!SpeechRecognizer.isRecognitionAvailable(MainActivity.this)) {
                    jsError("unavailable");
                    return;
                }
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_REQUEST);
                    return;
                }
                beginRecognition();
            });
        }

        private void beginRecognition() {
            runOnUiThread(() -> {
                try {
                    destroyRecognizer();
                    recognizer = SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
                    recognizer.setRecognitionListener(new RecognitionListener() {
                        @Override public void onReadyForSpeech(Bundle params) { }
                        @Override public void onBeginningOfSpeech() { }
                        @Override public void onRmsChanged(float rmsdB) { }
                        @Override public void onBufferReceived(byte[] buffer) { }
                        @Override public void onEndOfSpeech() { }
                        @Override public void onError(int error) { jsError(error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ? "not-allowed" : "recognition-error"); }
                        @Override public void onResults(Bundle results) {
                            ArrayList<String> matches = results == null ? null : results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                            jsResult(matches == null || matches.isEmpty() ? "" : matches.get(0), true);
                        }
                        @Override public void onPartialResults(Bundle results) {
                            ArrayList<String> matches = results == null ? null : results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                            if (matches != null && !matches.isEmpty()) jsResult(matches.get(0), false);
                        }
                        @Override public void onEvent(int eventType, Bundle params) { }
                    });
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
                    intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                    intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                    recognizer.startListening(intent);
                } catch (Exception e) {
                    jsError("unavailable");
                }
            });
        }

        @JavascriptInterface
        public void stopSpeech() {
            runOnUiThread(() -> {
                try { if (recognizer != null) recognizer.stopListening(); else jsResult("", true); }
                catch (Exception e) { jsError("recognition-error"); }
            });
        }

        @JavascriptInterface
        public void cancelSpeech() {
            runOnUiThread(() -> {
                try { if (recognizer != null) recognizer.cancel(); } catch (Exception ignored) { }
            });
        }

        private void jsResult(String text, boolean isFinal) {
            String safe = JSONObject.quote(text == null ? "" : text);
            getBridge().getWebView().post(() -> getBridge().getWebView().evaluateJavascript(
                "window.onNativeSpeechResult && window.onNativeSpeechResult(" + safe + "," + isFinal + ")", null));
        }

        private void jsError(String error) {
            String safe = JSONObject.quote(error);
            getBridge().getWebView().post(() -> getBridge().getWebView().evaluateJavascript(
                "window.onNativeSpeechError && window.onNativeSpeechError(" + safe + ")", null));
        }

        private void destroyRecognizer() {
            if (recognizer != null) {
                try { recognizer.destroy(); } catch (Exception ignored) { }
                recognizer = null;
            }
        }
    }
}
