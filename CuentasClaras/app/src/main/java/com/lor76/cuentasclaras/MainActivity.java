package com.lor76.cuentasclaras;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.content.FileProvider;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ventana principal: muestra la interfaz (assets/index.html) y hace de puente con la cámara. */
public class MainActivity extends Activity {

    private static final int FOTO = 7;
    private static final int CONTACTO = 8;
    private WebView web;
    private Uri fotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        web.setBackgroundColor(0xFF1A1D20);
        web.addJavascriptInterface(new Puente(), "AndroidBridge");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return abrirFuera(request.getUrl());
            }
            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return abrirFuera(Uri.parse(url));
            }
        });
        setContentView(web);
        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl("file:///android_asset/index.html");
    }

    private boolean abrirFuera(Uri uri) {
        if ("file".equals(uri.getScheme())) return false;
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) { }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("(window.appBack&&window.appBack())?'1':'0'", r -> {
            if (r == null || !r.contains("1")) MainActivity.super.onBackPressed();
        });
    }

    private void js(String code) {
        runOnUiThread(() -> web.evaluateJavascript(code, null));
    }

    /** Funciones que la interfaz puede llamar como AndroidBridge.scan() */
    private class Puente {
        @JavascriptInterface
        public void scan() {
            runOnUiThread(MainActivity.this::abrirCamara);
        }

        @JavascriptInterface
        public void pickContact() {
            runOnUiThread(() -> {
                try {
                    Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                    startActivityForResult(i, CONTACTO);
                } catch (Exception e) {
                    js("window.toastText&&toastText('No se pudo abrir la agenda')");
                }
            });
        }

        @JavascriptInterface
        public void share(String texto) {
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, texto);
                try { startActivity(Intent.createChooser(i, "Invitar con…")); } catch (Exception ignored) { }
            });
        }
    }

    private void leerContacto(Uri uri) {
        if (uri == null) return;
        String[] campos = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
        try (Cursor c = getContentResolver().query(uri, campos, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                JSONObject o = new JSONObject();
                o.put("name", c.getString(0));
                o.put("phone", c.getString(1));
                js("window.onContact&&onContact(" + JSONObject.quote(o.toString()) + ")");
            }
        } catch (Exception e) {
            js("window.toastText&&toastText('No se pudo leer el contacto')");
        }
    }

    private void abrirCamara() {
        try {
            File dir = new File(getCacheDir(), "images");
            if (!dir.exists()) dir.mkdirs();
            File img = new File(dir, "ticket.jpg");
            fotoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", img);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(i, FOTO);
        } catch (ActivityNotFoundException e) {
            js("window.onScanError&&onScanError('sin_camara')");
        } catch (Exception e) {
            js("window.onScanError&&onScanError('error')");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONTACTO) {
            if (resultCode == RESULT_OK && data != null) leerContacto(data.getData());
            return;
        }
        if (requestCode != FOTO || resultCode != RESULT_OK || fotoUri == null) return;
        js("window.toastText&&toastText('Leyendo la foto…')");
        try {
            InputImage imagen = InputImage.fromFilePath(this, fotoUri);
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(imagen)
                    .addOnSuccessListener(t -> enviarFilas(t))
                    .addOnFailureListener(e -> js("window.onScanError&&onScanError('ocr')"));
        } catch (Exception e) {
            js("window.onScanError&&onScanError('foto')");
        }
    }

    /** Junta los trozos de texto que están a la misma altura (nombre ... precio) en una sola fila. */
    private void enviarFilas(Text texto) {
        List<Text.Line> lineas = new ArrayList<>();
        for (Text.TextBlock b : texto.getTextBlocks()) lineas.addAll(b.getLines());
        Collections.sort(lineas, (a, b) -> Integer.compare(centroY(a), centroY(b)));

        JSONArray filas = new JSONArray();
        List<Text.Line> actual = new ArrayList<>();
        int yActual = -100000, alto = 20;
        for (Text.Line l : lineas) {
            Rect r = l.getBoundingBox();
            if (r == null) continue;
            int cy = r.centerY();
            if (!actual.isEmpty() && Math.abs(cy - yActual) > Math.max(10, alto / 2)) {
                filas.put(unir(actual));
                actual.clear();
            }
            if (actual.isEmpty()) { yActual = cy; alto = r.height(); }
            actual.add(l);
        }
        if (!actual.isEmpty()) filas.put(unir(actual));
        js("window.onScanText&&onScanText(" + JSONObject.quote(filas.toString()) + ")");
    }

    private static int centroY(Text.Line l) {
        Rect r = l.getBoundingBox();
        return r == null ? 0 : r.centerY();
    }

    private static String unir(List<Text.Line> fila) {
        Collections.sort(fila, (a, b) -> {
            Rect ra = a.getBoundingBox(), rb = b.getBoundingBox();
            return Integer.compare(ra == null ? 0 : ra.left, rb == null ? 0 : rb.left);
        });
        StringBuilder sb = new StringBuilder();
        for (Text.Line l : fila) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(l.getText());
        }
        return sb.toString();
    }
}
