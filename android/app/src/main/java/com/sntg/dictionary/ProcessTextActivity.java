package com.sntg.dictionary;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;

import com.getcapacitor.BridgeActivity;

/**
 * این Activity از BridgeActivity ارث‌بری می‌کند (نه یک WebView دستی)، چون
 * فقط از این طریق آدرس https://localhost/... به‌درستی به فایل‌های محلی
 * برنامه وصل می‌شود و در نتیجه به همان IndexedDB (واژه‌نامه‌های ذخیره‌شده)
 * دسترسی داریم. اما محتوای این WebView هیچ‌وقت واقعاً به کاربر نشان داده
 * نمی‌شود: بلافاصله یک ProgressDialog بومیِ اندروید رویش می‌آید، و بعد از
 * آماده‌شدن نتیجه، با یک AlertDialog بومی (دقیقاً مثل alert() که قبلاً در
 * برنامه استفاده شده) جایگزین می‌شود.
 */
public class ProcessTextActivity extends BridgeActivity {

    private ProgressDialog loadingDialog;
    private boolean firstLoadStarted = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // پل JS→Java: صفحهٔ وب، وقتی نتیجهٔ جستجو آماده شد، همین‌جا تحویل می‌دهد.
        getBridge().getWebView().addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void deliverResult(final String word, final String text) {
                runOnUiThread(() -> showResultDialog(word, text));
            }
        }, "AndroidPopup");

        String word = extractWord(getIntent());
        if (word == null) {
            finish();
            return;
        }

        showLoadingDialog();

        String encodedWord = Uri.encode(word);
        getBridge().getWebView().loadUrl("https://localhost/index.html?popup=1&headless=1&word=" + encodedWord);
        firstLoadStarted = true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        String word = extractWord(intent);
        if (word == null) return;

        showLoadingDialog();

        if (!firstLoadStarted) {
            return; // بعید است پیش بیاید: onCreate هنوز کامل نشده
        }

        // نمونهٔ قبلی زنده و از قبل بارگذاری‌شده است؛ فقط جستجوی جدید را
        // صدا می‌زنیم (کسری از ثانیه)، بدون بارگذاری دوبارهٔ کل صفحه.
        String escaped = word
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "");
        getBridge().getWebView().evaluateJavascript(
                "window.popupLookupPlainText && window.popupLookupPlainText('" + escaped + "');",
                null
        );
    }

    private String extractWord(Intent intent) {
        CharSequence selected = (intent != null) ? intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT) : null;
        if (selected == null) return null;
        String text = selected.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private void showLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) return;
        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("در حال جستجو…");
        loadingDialog.setCancelable(false);
        loadingDialog.show();
    }

    private void showResultDialog(String word, String text) {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
        new AlertDialog.Builder(this)
                .setTitle(word)
                .setMessage(text)
                .setPositiveButton("بستن", (dialog, which) -> finish())
                .setOnCancelListener(dialog -> finish())
                .show();
    }
}
