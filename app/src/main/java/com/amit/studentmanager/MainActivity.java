package com.amit.studentmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ContentValues;
import android.provider.MediaStore;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import android.widget.Toast;
import android.app.DownloadManager;
import java.io.File;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST = 1001;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.addJavascriptInterface(new PrintBridge(this), "AndroidPrint");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                Intent intent = params.createIntent();
                try { startActivityForResult(intent, FILE_CHOOSER_REQUEST); }
                catch (Exception e) { filePathCallback = null; Toast.makeText(MainActivity.this, "File picker unavailable", Toast.LENGTH_SHORT).show(); return false; }
                return true;
            }
        });
        webView.setDownloadListener(new DownloadListener() {
            @Override public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(mimetype);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.setTitle(URLUtilCompat.fileName(contentDisposition, url));
                    request.setDescription("Downloading Student Manager export");
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtilCompat.fileName(contentDisposition, url));
                    ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
                    Toast.makeText(MainActivity.this, "Export downloading to Downloads", Toast.LENGTH_SHORT).show();
                } catch (Exception e) { Toast.makeText(MainActivity.this, "Download failed", Toast.LENGTH_LONG).show(); }
            }
        });
        setContentView(webView);
        if (savedInstanceState == null) webView.loadUrl("https://amit07hz.github.io/STUDENT-MANAGER-/");
        else webView.restoreState(savedInstanceState);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();
                if (uri != null) results = new Uri[]{uri};
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) webView.saveState(outState);
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    private static class PrintBridge {
        private final Activity activity;
        PrintBridge(Activity activity) { this.activity = activity; }
        @JavascriptInterface public void saveFile(final String name, final String content, final String mimeType) {
            activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    OutputStream out = null;
                    try {
                        String safeName = name == null ? "student-manager-export.txt" : name.replaceAll("[^A-Za-z0-9._-]", "_");
                        if (android.os.Build.VERSION.SDK_INT >= 29) {
                            ContentValues values = new ContentValues();
                            values.put(MediaStore.MediaColumns.DISPLAY_NAME, safeName);
                            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType == null ? "text/plain" : mimeType);
                            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                            Uri uri = activity.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                            if (uri == null) throw new Exception("Could not create file");
                            out = activity.getContentResolver().openOutputStream(uri);
                        } else {
                            File dir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                            if (dir == null) throw new Exception("Downloads unavailable");
                            if (!dir.exists()) dir.mkdirs();
                            out = new FileOutputStream(new File(dir, safeName));
                        }
                        if (out == null) throw new Exception("Could not open file");
                        out.write((content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
                        out.flush();
                        Toast.makeText(activity, "Saved: " + safeName, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(activity, "Could not save export", Toast.LENGTH_LONG).show();
                    } finally { try { if (out != null) out.close(); } catch (Exception ignored) {} }
                }
            });
        }
        @JavascriptInterface public void printHtml(final String html) {
            activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    try {
                        final WebView printView = new WebView(activity);
                        printView.getSettings().setJavaScriptEnabled(false);
                        printView.setWebViewClient(new WebViewClient() {
                            @Override public void onPageFinished(WebView view, String url) {
                                PrintManager manager = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
                                String jobName = "Student Manager Report";
                                manager.print(jobName, view.createPrintDocumentAdapter(jobName), new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).setResolution(new android.print.PrintAttributes.Resolution("pdf", "pdf", 300, 300)).setMinMargins(PrintAttributes.Margins.NO_MARGINS).build());
                            }
                        });
                        printView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null);
                    } catch (Exception e) {
                        Toast.makeText(activity, "Could not open print dialog", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    private static class URLUtilCompat {
        static String fileName(String contentDisposition, String url) {
            String name = "student-manager-export";
            if (contentDisposition != null) {
                int i = contentDisposition.indexOf("filename=");
                if (i >= 0) name = contentDisposition.substring(i + 9).replace("\"", "").trim();
            }
            if (name.equals("student-manager-export") && url != null) {
                int slash = url.lastIndexOf('/');
                if (slash >= 0 && slash < url.length() - 1) name = url.substring(slash + 1).split("\\?")[0];
            }
            return name.replaceAll("[^A-Za-z0-9._-]", "_");
        }
    }
}
