package com.sntg.dictionary;

import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.getcapacitor.BridgeActivity;

/**
 * Activity اصلی برنامه.
 * پل AndroidApp.close تا دکمهٔ × در صفحهٔ وب بتواند برنامه را ببندد
 * (مشابه AndroidPopup در ProcessTextActivity).
 */
public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = getBridge() != null ? getBridge().getWebView() : null;
        if (webView != null) {
            webView.addJavascriptInterface(new Object() {
                @JavascriptInterface
                public void close() {
                    runOnUiThread(() -> {
                        finishAffinity();
                        // اگر finishAffinity کافی نبود (نسخه‌های قدیمی):
                        // finish();
                    });
                }
            }, "AndroidApp");
        }
    }
}
