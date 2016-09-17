package ch.hackzurich.idscan;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

// TODO: fix the rotate issue (save file path on onPause and create file again onCreate)
public class OpenPhotoActivity extends AppCompatActivity {
    
    private static final String TAG = OpenPhotoActivity.class.getSimpleName();
    
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    
    public static OpenPhotoActivity activity;
    
    public LabelService labelService = new LabelService();
    
    /**
     * The file which stores the photo that we just took.
     */
    private File photoFile;

    /**
     * Create a unique file name.
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        photoFile = image;
    
        return image;
    }
    
    /**
     * Takes a picture and stores it in the external storage.
     */
    private void dispatchTakePictureIntent() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (!Settings.System.canWrite(this)) {
//                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                        Manifest.permission.READ_EXTERNAL_STORAGE}, 2909);
//            }
//        }
    
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(TAG, ex.getMessage(), ex);
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "ch.hackzurich.idscan.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }
    
    /**
     * Returns the bitmap from the taken photo or null in case the file was not found.
     */
    private Bitmap createBitMap() {
        try (FileInputStream fileInputStream = new FileInputStream(photoFile)) {
            return BitmapFactory.decodeStream(fileInputStream);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Failed to find image: " + e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, "Failed to open/close image file: " + e.getMessage(), e);
        }
        return null;
    }
    
    private Bitmap showImage() {
        ImageView imageView = (ImageView) findViewById(R.id.photo);
        Bitmap bitmap = createBitMap();
        imageView.setImageBitmap(bitmap);
        return bitmap;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Check which request we're responding to
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Bitmap bitmap = showImage();
                extractIdentityInformation(bitmap);
                detectFace(bitmap);
            }
        }
    }

    static class DisplayIdentityCard implements Runnable {
    
        private final IdentityCard identityCard;
        
        DisplayIdentityCard(IdentityCard identityCard) {
            this.identityCard = identityCard;
        }
        
        @Override
        public void run() {
            TextView surnameText = (TextView) OpenPhotoActivity.activity.findViewById(R.id.text_surname);
            surnameText.setText(identityCard.getSurname());
            TextView givenNameText = (TextView) OpenPhotoActivity.activity.findViewById(R.id.text_givenname);
            givenNameText.setText(identityCard.getGivenNames());
            TextView nationalityText = (TextView) OpenPhotoActivity.activity.findViewById(R.id.text_nationality);
            nationalityText.setText(identityCard.getNationality());
            TextView dateOfBirth = (TextView) OpenPhotoActivity.activity.findViewById(R.id.text_dateofbirth);
            if (identityCard.getDateOfBirth() != null) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.mm.yyyy");
                dateOfBirth.setText(simpleDateFormat.format(identityCard.getDateOfBirth()));
            }
        }
    }
    
    private void displayText(IdentityCard identityCard) {
        runOnUiThread(new DisplayIdentityCard(identityCard));
    }
    
    private void extractIdentityInformation(Bitmap image) {
        new AsyncTask<Bitmap, Void, Void>() {
            @Override
            protected Void doInBackground(Bitmap... images) {
                Bitmap image = images[0];
                Log.v(TAG, "width: " + image.getWidth());
                Log.v(TAG, "height: " + image.getHeight());
                try {
                    final JSONObject jsonObject = ImageService.extractText(image);
                    IdentityCard identityCard = labelService.parse(jsonObject);
                    
                    Log.d(TAG, "Parsed identity: " + identityCard);
                    displayText(identityCard);
                    
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
                return null;
            }
        }.execute(image);
    }
    

    private String lastFaceId = null;

    private void detectFace(Bitmap image) {
        new AsyncTask<Bitmap, Void, Void>() {
            @Override
            protected Void doInBackground(Bitmap... images) {
                Bitmap image = images[0];
                Log.v(TAG, "width: " + image.getWidth());
                Log.v(TAG, "height: " + image.getHeight());
                try {
                    final JSONArray faces = ImageService.detectFaces(image);
                    if (faces.length() == 0) {
                        Log.d(TAG, "no faces detected");
                    } else {
                        for (int i = 0; i < faces.length(); i++) {
                            final JSONObject face = faces.getJSONObject(i);
                            final String faceId = face.getString("faceId");
                            Log.d(TAG, "faceId: " + faceId);
                            final JSONObject faceAttributes = face.getJSONObject("faceAttributes");
                            Log.d(TAG, "gender: " + faceAttributes.getString("gender"));
                            Log.d(TAG, "age: " + faceAttributes.getString("age"));

                            if (lastFaceId != null) {
                                final JSONObject response = ImageService.verifyFaces(lastFaceId, faceId);
                                Log.d(TAG, "isIdentical: " + response.getString("isIdentical"));
                                Log.d(TAG, "confidence: " + response.getString("confidence"));
                            }
                            lastFaceId = faceId;
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
                return null;
            }
        }.execute(image);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (photoFile != null) {
            bundle.putString("photoFile", photoFile.getAbsolutePath());
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_photo);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    
        if (savedInstanceState != null && savedInstanceState.getString("photoFile") != null) {
            photoFile = new File(savedInstanceState.getString("photoFile"));
            showImage();
        }
    
        activity = this;
    
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
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
