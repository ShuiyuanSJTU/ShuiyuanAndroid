package com.sjtu.shuiyuan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final String FILE_TYPE = "*/*";    // file types to be allowed for upload
    private final boolean MultipleFiles = true;         // allowing multiple file upload

    private WebView webView;
    private ValueCallback<Uri[]> filePath;     // received file(s) temp. location

    ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    int resultCode = result.getResultCode();
                    Uri[] results = null;

                    if (resultCode == Activity.RESULT_CANCELED) {
                        filePath.onReceiveValue(null);
                        return;

                    }

                    if (resultCode == Activity.RESULT_OK) {
                        Intent intent = result.getData();
                        if (null == filePath) {
                            return;
                        }
                        ClipData clipData;
                        String stringData;
                        try {
                            assert intent != null;
                            clipData = intent.getClipData();
                            stringData = intent.getDataString();
                        } catch (Exception e) {
                            clipData = null;
                            stringData = null;
                        }

                        if (clipData != null) { // checking if multiple files selected or not
                            final int numSelectedFiles = clipData.getItemCount();
                            results = new Uri[numSelectedFiles];
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                results[i] = clipData.getItemAt(i).getUri();
                            }
                        } else {
                            results = new Uri[]{Uri.parse(stringData)};
                        }

                    }
                    filePath.onReceiveValue(results);
                    filePath = null;
                }
            });

    public boolean isDarkMode() {
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.setStatusBarColor(Color.TRANSPARENT);
        int uiOption = window.getDecorView().getSystemUiVisibility();
        if (isDarkMode()) {
            // 没有DARK_STATUS_BAR属性，通过位运算将LIGHT_STATUS_BAR属性去除
            window.getDecorView().setSystemUiVisibility(uiOption & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            // 这里是要注意的地方，如果需要补充新的FLAG，记得要带上之前的然后进行或运算
            window.getDecorView().setSystemUiVisibility(uiOption | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onResume() {
        super.onResume();
        webView.getSettings().setForceDark(isDarkMode() ? WebSettings.FORCE_DARK_ON : WebSettings.FORCE_DARK_OFF);
    }

    @SuppressLint({"SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarColor();
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        assert webView != null;
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);

        webSettings.setMixedContentMode(0);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setWebViewClient(new WebViewClient());

        final String url = "https://shuiyuan.sjtu.edu.cn";
        webView.loadUrl(url);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equals(scheme) || "https".equals(scheme)) {
                    return false;
                }
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {

                if (checkFilePermissions()) {
                    filePath = filePathCallback;

                    Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    contentSelectionIntent.setType(FILE_TYPE);
                    if (MultipleFiles) {
                        contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    }

                    Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                    chooserIntent.putExtra(Intent.EXTRA_TITLE, "File chooser");
                    pickImageLauncher.launch(chooserIntent);
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    public boolean checkFilePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

}