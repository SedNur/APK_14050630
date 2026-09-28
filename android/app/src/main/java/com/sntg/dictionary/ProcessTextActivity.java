package com.sntg.dictionary;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.getcapacitor.BridgeActivity;

/**
 * Activity شناور برای ACTION_PROCESS_TEXT.
 * صفحهٔ پشت باید کاملاً دیده شود؛ هیچ dim / لایهٔ مات نباشد.
 */
public class ProcessTextActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        Window window = getWindow();
        if (window != null) {
            // شفافیت واقعی پنجره (نه فقط تم)
            window.setFormat(PixelFormat.TRANSLUCENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0f);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            // اجازهٔ دیدن و لمس محتوای پشت در نواحی خالی (اختیاری؛ با backdrop CSS هم بسته می‌شود)
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;
            lp.dimAmount = 0f;
            lp.format = PixelFormat.TRANSLUCENT;
            window.setAttributes(lp);
        }

        // ریشهٔ layout و همهٔ والدین WebView را شفاف کن (Capacitor اغلب سفید می‌گذارد)
        View content = findViewById(android.R.id.content);
        if (content != null) {
            makeViewTreeTransparent(content);
        }

        CharSequence selected = getIntent().getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        String selectedText = (selected != null) ? selected.toString() : "";
        String encodedWord = Uri.encode(selectedText);

        WebView webView = getBridge().getWebView();
        if (webView != null) {
            // HARDWARE layer شفافیت WebView را روی خیلی از دستگاه‌ها خراب می‌کند
            webView.setBackgroundColor(Color.TRANSPARENT);
            webView.setLayerType(View.LAYER_TYPE_NONE, null);
            makeViewTreeTransparent(webView);

            webView.addJavascriptInterface(new Object() {
                @JavascriptInterface
                public void close() {
                    runOnUiThread(ProcessTextActivity.this::finish);
                }
            }, "AndroidPopup");

            webView.loadUrl("https://localhost/index.html?popup=1&word=" + encodedWord);

            // بعد از لود هم یک‌بار دیگر شفافیت را اعمال کن (Capacitor گاهی بعد از load رنگ می‌گذارد)
            webView.post(() -> {
                webView.setBackgroundColor(Color.TRANSPARENT);
                makeViewTreeTransparent(webView);
                View root = findViewById(android.R.id.content);
                if (root != null) makeViewTreeTransparent(root);
            });
        }
    }

    /** همهٔ لایه‌های View تا ریشه را پس‌زمینهٔ شفاف می‌کند. */
    private static void makeViewTreeTransparent(View view) {
        if (view == null) return;
        view.setBackgroundColor(Color.TRANSPARENT);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            // بعضی layoutهای Capacitor background drawable دارند
            group.setBackground(null);
            group.setBackgroundColor(Color.TRANSPARENT);
            for (int i = 0; i < group.getChildCount(); i++) {
                makeViewTreeTransparent(group.getChildAt(i));
            }
        }
        View parent = view.getParent() instanceof View ? (View) view.getParent() : null;
        // فقط چند سطح بالا، نه بی‌نهایت
        int guard = 0;
        while (parent != null && guard++ < 12) {
            parent.setBackgroundColor(Color.TRANSPARENT);
            if (parent instanceof ViewGroup) {
                parent.setBackground(null);
                parent.setBackgroundColor(Color.TRANSPARENT);
            }
            parent = parent.getParent() instanceof View ? (View) parent.getParent() : null;
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
