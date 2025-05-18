package com.example.bowling;

import okhttp3.*;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImgurUploader {

    private static final String IMGUR_CLIENT_ID = "0b177a9bdea895f";  // ide tedd a Client ID-t

    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String error);
    }

    public static void uploadImage(Uri imageUri, Context context, UploadCallback callback) {
        try {
            // Kép beolvasása Uri-ból és Base64 kódolása
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            assert inputStream != null;
            byte[] bytes = getBytes(inputStream);
            String base64Image = Base64.encodeToString(bytes, Base64.DEFAULT);

            OkHttpClient client = new OkHttpClient();

            RequestBody requestBody = new FormBody.Builder()
                    .add("image", base64Image)
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.imgur.com/3/image")
                    .addHeader("Authorization", "Client-ID " + IMGUR_CLIENT_ID)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onFailure("Upload failed: " + response.message());
                        return;
                    }

                    String responseBody = response.body().string();

                    // JSON válasz feldolgozása, hogy kiolvasd a kép URL-jét
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        String link = json.getJSONObject("data").getString("link");
                        callback.onSuccess(link);
                    } catch (JSONException e) {
                        callback.onFailure("JSON parse error: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            callback.onFailure("Error: " + e.getMessage());
        }
    }

    private static byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }
}
