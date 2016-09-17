package ch.hackzurich.idscan;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import ch.hackzurich.idscan.config.Configuration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenPhotoActivity extends AppCompatActivity {
    
    private static final String TAG = Configuration.class.getSimpleName();
    
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private static String capturedPhotoFilePath;

    public static OpenPhotoActivity activity;
    
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    
        File tempFile = null;
        try {
            tempFile = File.createTempFile("idscan", ".jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        capturedPhotoFilePath = tempFile.getAbsolutePath();
        Log.d(TAG, "capturedPhotoFilePath: " + capturedPhotoFilePath);
        Uri capturedPhotoUri = Uri.fromFile(tempFile);
        
        takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, capturedPhotoUri);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
    
    protected void sendImageToOCRService(byte[] imageBytes) {
        try {
            URL url = new URL(Configuration.getConfigValue(this, "microsoft.ocr.api.url"));
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
    
            String contentType = "application/octet-stream";
    
            httpConn.setDoOutput(true);
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Accept", "*/*");
            httpConn.setRequestProperty("Content-type", contentType);
            httpConn.setRequestProperty("Ocp-Apim-Subscription-Key", Configuration.getConfigValue(this, "microsoft.ocr.api.key"));
            OutputStream outputStream = httpConn.getOutputStream();
            outputStream.write(imageBytes);
            outputStream.close();
            
            Log.d(TAG, "Response Code: " + httpConn.getResponseCode());
            Log.d(TAG, "Response Body: " + httpConn.getResponseMessage());
            
            final BufferedReader streamReader = new BufferedReader(new InputStreamReader(httpConn.getInputStream(), "UTF-8")); 
            StringBuilder responseStrBuilder = new StringBuilder();

            String inputStr;
            while ((inputStr = streamReader.readLine()) != null) {
                responseStrBuilder.append(inputStr);
            }
            Log.d(TAG, "Received: " + responseStrBuilder.toString());
            final JSONObject jsonObject = new JSONObject(responseStrBuilder.toString());
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (ProtocolException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }
    
    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString("fileName", capturedPhotoFilePath);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Check which request we're responding to
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Bitmap bitmap = BitmapFactory.decodeFile(capturedPhotoFilePath, new BitmapFactory.Options());
                
                ImageView imageView = (ImageView) findViewById(R.id.photo);
                imageView.setImageBitmap(bitmap);
                
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                
                new AsyncTask<byte[], Void, Void>() {
    
                    @Override 
                    protected Void doInBackground(byte[]... bytes) {
                        sendImageToOCRService(bytes[0]);
                        return null;
                    }
                }.execute(byteArray);
            }
        }
    }

    private void testImageService() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... none) {
                final Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.pass);
                Log.i(TAG, "width: " + image.getWidth());
                Log.i(TAG, "height: " + image.getHeight());
                try {
                    final JSONObject jsonObject = ImageService.extractText(image);
                    final JSONArray regions = jsonObject.getJSONArray("regions");
                    for (int i = 0; i < regions.length(); i++) {
                        Log.d(TAG, "Region " + (i + 1));
                        final JSONArray lines = regions.getJSONObject(i).getJSONArray("lines");
                        for (int j = 0; j < lines.length(); j++) {
                            Log.d(TAG, "Line " + (j + 1));
                            final JSONArray words = lines.getJSONObject(j).getJSONArray("words");
                            for (int k = 0; k < words.length(); k++) {
                                Log.d(TAG, "Word " + (k + 1) + ": " + words.getJSONObject(k).getString("text"));
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
                return null;
            }
        }.execute();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_photo);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        activity = this;
        
        if (savedInstanceState != null) {
            capturedPhotoFilePath = savedInstanceState.getString("fileName");
        }
        
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testImageService();
//                dispatchTakePictureIntent();
            }
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_open_photo, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
}
