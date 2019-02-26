package com.example.myapplication;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final int REQUEST_IMAGE_CAPTURE = 1, REQUEST_IMAGE_GALLEY = 2;
    String path = Environment.getExternalStorageDirectory().toString();
    String uploadUri = "http://192.168.100.4:5000/Home/Scan";
    private ImageView image;
    private Button camera, gallery, scan;
    Bitmap bitmap;
    ProgressDialog progressDialog;
    String imageString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        boolean permissions = isStoragePermissionGranted();
        image = (ImageView) findViewById(R.id.image);
        camera = (Button) findViewById(R.id.camera);
        camera.setOnClickListener(this);
        gallery = (Button) findViewById(R.id.gallery);
        gallery.setOnClickListener(this);
        scan = (Button) findViewById(R.id.scanButton);
        scan.setEnabled(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.camera:
                Intent iCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (iCamera.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(iCamera, REQUEST_IMAGE_CAPTURE);
                }
                break;
            case R.id.gallery:
                Intent iGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                iGallery.setType("image/'");
                startActivityForResult(iGallery, REQUEST_IMAGE_GALLEY);
                break;
            case R.id.scanButton:
                Toast.makeText(this, "Scan Button Clicked", Toast.LENGTH_SHORT).show();
                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setMessage("Uploading, please wait...");
                progressDialog.show();
                bitmap = ((BitmapDrawable) image.getDrawable()).getBitmap();
                try {
                    File file = saveBitmap(bitmap);
                    FileInputStream fileInputStream = new FileInputStream(file);
                    byte imageBytes[] = new byte[(int) file.length()];
                    fileInputStream.read(imageBytes);
                    imageString = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                StringRequest request = new StringRequest(Request.Method.POST, uploadUri, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String s) {
                        progressDialog.dismiss();
                        if (!s.equals("Server Error")) {
                            if(s.equals("Unable to recognise the image")) {
                                Toast.makeText(MainActivity.this, s, Toast.LENGTH_LONG).show();
                            } else {
                                try {
                                    bitmap = strToBitmap(s);
                                    image.setImageBitmap(bitmap);
                                } catch(Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Some error occurred! Please try again later", Toast.LENGTH_LONG).show();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Toast.makeText(MainActivity.this, "Some error occurred -> " + volleyError, Toast.LENGTH_LONG).show();
                        ;
                    }
                }) {
                    //adding parameters to send
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> parameters = new HashMap<String, String>();
                        parameters.put("scanStr", imageString);
                        return parameters;
                    }
                };
                request.setRetryPolicy(new DefaultRetryPolicy(
                        300000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                RequestQueue rQueue = Volley.newRequestQueue(MainActivity.this);
                rQueue.add(request);
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                image.setImageBitmap(bitmap);
                scan.setEnabled(true);
            } else if (requestCode == REQUEST_IMAGE_GALLEY) {
                Uri uri = data.getData();
                String x = getPath(uri);
                Toast.makeText(getApplicationContext(), x, Toast.LENGTH_LONG).show();
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    image.setImageBitmap(bitmap);
                    scan.setEnabled(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getPath(Uri uri) {
        if (uri == null) return null;
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        return uri.getPath();
    }

    public static File saveBitmap(Bitmap bmp) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 70, bytes);
        File f = new File(Environment.getExternalStorageDirectory()
                + File.separator + "tempScan.png");

        if(!f.exists())
            f.createNewFile();
        FileOutputStream fo = new FileOutputStream(f);
        fo.write(bytes.toByteArray());
        fo.close();
        return f;
    }

    public static Bitmap strToBitmap(String scannedStr) throws IOException {
        byte scannedBytes[] = Base64.decode(scannedStr, Base64.DEFAULT);
        File f = new File(Environment.getExternalStorageDirectory()
                + File.separator + "scannedImage.png");

        if(!f.exists())
            f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(scannedBytes);
        fos.close();

        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try {
            bitmap = BitmapFactory.decodeStream(new FileInputStream(f), null, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                if(checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE )== PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
                else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                    return false;
                }
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                if(checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE )== PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
                else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                    return false;
                }
            }
        }
        else {
            return true;
        }
    }
}
