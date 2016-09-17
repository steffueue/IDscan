package ch.hackzurich.idscan;

import android.graphics.Bitmap;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import ch.hackzurich.idscan.config.Configuration;

public class ImageService {

    private static final String TAG = ImageService.class.getSimpleName();

    private static String getConfigValue(String name) {
        return Configuration.getConfigValue(OpenPhotoActivity.activity, name);
    }

    private static HttpURLConnection getConnection(String endPoint, String contentType, String subscriptionKey) throws IOException, JSONException {
        final URL url = new URL(getConfigValue(endPoint));
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-type", "application/" + contentType);
        connection.setRequestProperty("Ocp-Apim-Subscription-Key", getConfigValue(subscriptionKey));
        return connection;
    }

    private static void writeImage(Bitmap image, HttpURLConnection connection) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] bytes = stream.toByteArray();

        Log.v(TAG, "image size: " + (bytes.length / 1024) + " kb");

        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(bytes);
        outputStream.close();
    }

    private static String getResponse(HttpURLConnection connection) throws IOException, JSONException {
        Log.v(TAG, "Response Code: " + connection.getResponseCode());

        final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        StringBuilder response = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        Log.v(TAG, "Response Content: " + response.toString());
        return response.toString();
    }

    public static JSONObject extractText(Bitmap image) throws IOException, JSONException {
        Log.v(TAG, "extract text");
        HttpURLConnection connection = getConnection("microsoft.ocr.api.url", "octet-stream", "microsoft.ocr.api.key");
        writeImage(image, connection);
        return new JSONObject(getResponse(connection));
    }

    public static JSONArray detectFaces(Bitmap image) throws Exception {
        Log.v(TAG, "detect face");
        HttpURLConnection connection = getConnection("microsoft.face.detect.api.url", "octet-stream", "microsoft.face.api.key");
        writeImage(image, connection);
        return new JSONArray(getResponse(connection));
    }

    public static JSONObject verifyFaces(String faceId1, String faceId2) throws Exception {
        Log.v(TAG, "verify faces");
        HttpURLConnection connection = getConnection("microsoft.face.verify.api.url", "json", "microsoft.face.api.key");

        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("faceId1", faceId1);
        jsonObject.put("faceId2", faceId2);
        final String request = jsonObject.toString();
        Log.v(TAG, request);

        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(request.getBytes("UTF-8"));
        outputStream.close();
        return new JSONObject(getResponse(connection));
    }

}
