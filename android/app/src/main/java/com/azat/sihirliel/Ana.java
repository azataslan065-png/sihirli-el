package com.azat.sihirliel;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/*
 * Sihirli El kabuğu: oyun APK'nın içinden, sitenin adresiyle açılır (kamera için güvenli adres gerekir).
 * Sayfa kamerayı isteyince Android kamera izni sorulur ve WebView'e aktarılır. Yatay, tam ekran.
 */
public class Ana extends Activity {
    private static final String EV = "azataslan065-png.github.io", YOL = "/sihirli-el/";
    private static final String ADRES = "https://" + EV + YOL + "index.html";
    private static final int KAMERA = 11;
    private WebView w;
    private PermissionRequest bekleyen;   // Android izni beklenirken sayfanın isteği

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 28) getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        w = new WebView(this);
        w.setBackgroundColor(Color.parseColor("#0b0820"));
        setContentView(w);
        tamEkran();
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setTextZoom(100);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setUserAgentString(s.getUserAgentString() + " SihirliElApp/" + BuildConfig.VERSION_CODE);
        w.setOverScrollMode(View.OVER_SCROLL_NEVER);
        w.setVerticalScrollBarEnabled(false);
        w.setHorizontalScrollBarEnabled(false);
        if (Build.VERSION.SDK_INT >= 26) w.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false);
        w.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                if (!r.isForMainFrame()) return false;
                Uri u = r.getUrl(); String p = u.getPath();
                if (EV.equals(u.getHost()) && p != null && p.startsWith(YOL)) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
                return true;
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) { return ver(r); }
        });
        w.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(final PermissionRequest r) {
                runOnUiThread(() -> {
                    boolean kamera = Arrays.asList(r.getResources()).contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE);
                    if (!kamera) { r.deny(); return; }
                    if (kameraIzniVar()) { r.grant(new String[]{ PermissionRequest.RESOURCE_VIDEO_CAPTURE }); return; }
                    bekleyen = r;
                    if (Build.VERSION.SDK_INT >= 23) requestPermissions(new String[]{ Manifest.permission.CAMERA }, KAMERA);
                });
            }
            @Override public void onPermissionRequestCanceled(PermissionRequest r) { if (bekleyen == r) bekleyen = null; }
        });
        w.loadUrl(ADRES);
        Magaza.denetle(this);   // güncelleme varsa Uygulama Merkezi'ne yönlendirir (bkz. Magaza)
    }

    private boolean kameraIzniVar() {
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override public void onRequestPermissionsResult(int kod, String[] izinler, int[] sonuc) {
        super.onRequestPermissionsResult(kod, izinler, sonuc);
        if (kod != KAMERA || bekleyen == null) return;
        if (sonuc.length > 0 && sonuc[0] == PackageManager.PERMISSION_GRANTED) bekleyen.grant(new String[]{ PermissionRequest.RESOURCE_VIDEO_CAPTURE });
        else bekleyen.deny();
        bekleyen = null;
    }

    private WebResourceResponse ver(WebResourceRequest r) {
        try {
            Uri u = r.getUrl();
            if (!"GET".equalsIgnoreCase(r.getMethod()) || !EV.equals(u.getHost())) return null;
            String p = u.getPath();
            if (p == null || !p.startsWith(YOL)) return null;
            String ad = p.substring(YOL.length());
            if (ad.isEmpty()) ad = "index.html";
            if (ad.contains("..") || ad.startsWith("/")) return null;
            String tur = ad.endsWith(".html") ? "text/html" : ad.endsWith(".js") ? "text/javascript" : ad.endsWith(".png") ? "image/png" : null;
            if (tur == null) return null;
            InputStream in = getAssets().open(ad);
            Map<String, String> hd = new HashMap<>();
            hd.put("Cache-Control", "no-store");
            return new WebResourceResponse(tur, tur.startsWith("image/") ? null : "utf-8", 200, "OK", hd, in);
        } catch (Exception e) { return null; }
    }

    @SuppressWarnings("deprecation")
    private void tamEkran() {
        w.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override public void onWindowFocusChanged(boolean f) { super.onWindowFocusChanged(f); if (f) tamEkran(); }

    /* Geri tuşu oyunu duraklatır; menüdeyken uygulamadan çıkar. */
    @Override public void onBackPressed() {
        w.evaluateJavascript("(window.sihirliGeri&&window.sihirliGeri())?'1':'0'", v -> { if (v == null || !v.contains("1")) finish(); });
    }

    @Override protected void onPause() { super.onPause(); w.onPause(); }
    @Override protected void onResume() { super.onResume(); w.onResume(); tamEkran(); Magaza.denetle(this); }
    @Override protected void onDestroy() { if (w != null) w.destroy(); super.onDestroy(); }
}
