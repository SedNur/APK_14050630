package com.sntg.dictionary;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;

import com.getcapacitor.BridgeActivity;

public class ProcessTextActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // متنی که کاربر در برنامهٔ دیگر (مرورگر، PDF، دفترچه یادداشت و ...) انتخاب کرده
        CharSequence selected = getIntent().getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        String selectedText = (selected != null) ? selected.toString() : "";
        String encodedWord = Uri.encode(selectedText);

        // یک پل کوچک JS↔Java تا دکمهٔ «بستن» داخل صفحهٔ وب بتواند
        // همین Activity را ببندد (چون این یک پنجرهٔ معمولی است، نه تب مرورگر).
        getBridge().getWebView().addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void close() {
                runOnUiThread(ProcessTextActivity.this::finish);
            }
        }, "AndroidPopup");

        // از همان bridge/WebView داخلیِ Capacitor استفاده می‌کنیم (نه یک WebView دستی)،
        // چون این‌طوری دقیقاً همان origin (https://localhost) برنامهٔ اصلی حفظ می‌شود
        // و در نتیجه به همان IndexedDB (گلوساری‌های ذخیره‌شده) دسترسی داریم.
        getBridge().getWebView().loadUrl("https://localhost/index.html?popup=1&word=" + encodedWord);

        // نکته: اندازهٔ پنجره را دیگر دستی (بعد از ساخته‌شدن WebView) تغییر
        // نمی‌دهیم، چون همین کار باعث بهم‌ریختن layout و کندی می‌شد.
        // اندازه‌گیری اکنون فقط از طریق تمِ اختصاصی (styles.xml) انجام می‌شود.
    }
}
