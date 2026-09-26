package com.sntg.dictionary;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;

import com.getcapacitor.BridgeActivity;

public class ProcessTextActivity extends BridgeActivity {

    private boolean pageLoaded = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // WebView به‌طور پیش‌فرض یک پس‌زمینهٔ سفیدِ مات دارد، صرف‌نظر از
        // شفافیتِ خودِ پنجره؛ باید صریحاً شفافش کنیم تا برنامهٔ زیرین
        // (مرورگر/PDF) از پشتش دیده شود.
        getBridge().getWebView().setBackgroundColor(android.graphics.Color.TRANSPARENT);

        // یک پل کوچک JS↔Java تا دکمهٔ «بستن» داخل صفحهٔ وب بتواند
        // همین Activity را ببندد (چون این یک پنجرهٔ معمولی است، نه تب مرورگر).
        getBridge().getWebView().addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void close() {
                runOnUiThread(ProcessTextActivity.this::finish);
            }
        }, "AndroidPopup");

        // فقط همین یک‌بار (اولین ساخته‌شدنِ Activity) کل صفحه را بارگذاری
        // می‌کنیم. کلمهٔ اول از طریق پارامتر URL منتقل می‌شود چون در این
        // لحظه هنوز جاوااسکریپت صفحه آماده نیست تا evaluateJavascript کار کند.
        // دفعات بعدی، چون این Activity به‌صورت singleTask تعریف شده،
        // onNewIntent صدا زده می‌شود (نه onCreate) و از همان تابعِ JS استفاده می‌شود.
        CharSequence selected = getIntent().getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        String selectedText = (selected != null) ? selected.toString() : "";
        String encodedWord = Uri.encode(selectedText);
        getBridge().getWebView().loadUrl("https://localhost/index.html?popup=1&word=" + encodedWord);
        pageLoaded = true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (!pageLoaded) {
            // احتیاطاً: اگر به هر دلیلی هنوز صفحه بارگذاری نشده، صبر می‌کنیم
            // که onCreate خودش کار را انجام دهد.
            return;
        }

        // نمونهٔ قبلی هنوز زنده و بارگذاری‌شده است؛ فقط کلمهٔ جدید را
        // جستجو می‌کنیم (کسری از ثانیه)، بدون بارگذاری دوبارهٔ کل صفحه.
        searchWordFromIntent(intent);
    }

    private void searchWordFromIntent(Intent intent) {
        CharSequence selected = (intent != null) ? intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT) : null;
        String selectedText = (selected != null) ? selected.toString() : "";
        if (selectedText.isEmpty()) return;

        // متن را به‌شکل امن به‌عنوان یک رشتهٔ جاوااسکریپتی escape می‌کنیم
        // (نه فقط URL-encode، چون این‌بار مستقیم به‌عنوان کد JS اجرا می‌شود).
        String escaped = selectedText
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "");

        getBridge().getWebView().evaluateJavascript(
                "window.popupSearchWord && window.popupSearchWord('" + escaped + "');",
                null
        );
    }
}
