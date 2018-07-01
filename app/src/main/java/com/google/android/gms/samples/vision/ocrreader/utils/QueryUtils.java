package com.google.android.gms.samples.vision.ocrreader.utils;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

public class QueryUtils {

    private static final String BASE_URL =
            "https://translate.yandex.net/api/v1.5/tr.json/translate";
    private static final String PARAM_KEY = "key";
    private static final String PARAM_FORMAT = "format";
    private static final String PARAM_LANG = "lang";
    private static final String PARAM_TEXT = "text";

    private static final String KEY =
            "trnsl.1.1.20180625T205312Z.10f795f595e8add0.c0c2d0a5d04837478e453a356faa5c8fbaf9488b";


    public synchronized static String translate(String text) {
        if (TextUtils.isEmpty(text)) return null;

        Uri uri = Uri.parse(BASE_URL).buildUpon()
                .appendQueryParameter(PARAM_KEY, KEY)
                .appendQueryParameter(PARAM_FORMAT, "plain")
                .appendQueryParameter(PARAM_LANG, "en-ar")
                .appendQueryParameter(PARAM_TEXT, text)
                .build();

        Log.d("uri", uri.toString());

        try {
            URL url = new URL(uri.toString());
             String result = fetchDataFromUrl(url);
             Log.d("json", result + "");
             return parseJSon(result);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String parseJSon(String result) {
        if (TextUtils.isEmpty(result)) return null;

        try {
            JSONObject baseResponse = new JSONObject(result);

            JSONArray text = baseResponse.getJSONArray("text");

            return text.getString(0);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String fetchDataFromUrl(URL url) throws IOException {

        try {

            InputStream inputStream = url.openStream();

            Scanner scanner = new Scanner(inputStream);
            scanner.useDelimiter("\\A");

            if (scanner.hasNext()) {
                return scanner.next();
            } else {
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
