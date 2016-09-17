package ch.hackzurich.idscan;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import ch.hackzurich.idscan.config.Configuration;

public class ImageService {

    private static final String TAG = ImageService.class.getSimpleName();

    private static String getConfigValue(String name) {
        return Configuration.getConfigValue(OpenPhotoActivity.activity, name);
    }

    public static JSONObject extractText(Bitmap image) throws IOException, JSONException {
        Log.i(TAG, "extract text");

        URL url = new URL(getConfigValue("microsoft.ocr.api.url"));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-type", "application/octet-stream");
        connection.setRequestProperty("Ocp-Apim-Subscription-Key", getConfigValue("microsoft.ocr.api.key"));

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        byte[] bytes = stream.toByteArray();

        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(bytes);
        outputStream.close();

        Log.d(TAG, "Response Code: " + connection.getResponseCode());

        final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        StringBuilder response = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        Log.d(TAG, "Received: " + response.toString());
        return new JSONObject(response.toString());
    }

    public static JSONObject detectFace(Bitmap image) throws Exception {
        Log.i(TAG, "detect face");
        return null;
    }

    public static JSONObject verifyFaces(String faceId1, String faceId2) throws Exception {
        Log.i(TAG, "verify faces");
        return null;
    }

}
