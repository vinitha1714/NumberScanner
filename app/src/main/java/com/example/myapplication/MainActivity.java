package com.example.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView image;
    private Button camera, gallery, scan;
    private final int REQUEST_IMAGE_CAPTURE=1,REQUEST_IMAGE_GALLEY=2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        image = (ImageView)findViewById(R.id.image);
        camera = (Button)findViewById(R.id.camera);
        camera.setOnClickListener(this);
        gallery = (Button)findViewById(R.id.gallery);
        gallery.setOnClickListener(this);
        scan = (Button)findViewById(R.id.scanButton);
        scan.setEnabled(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.camera:
                Intent iCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(iCamera.resolveActivity(getPackageManager()) != null){
                    startActivityForResult(iCamera,REQUEST_IMAGE_CAPTURE);
                }
                break;
            case R.id.gallery:
                Intent iGallery = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                iGallery.setType("image/'");
                startActivityForResult(iGallery,REQUEST_IMAGE_GALLEY);
                break;
            case R.id.scanButton:
                Toast.makeText(this, "Scan Button Clicked",Toast.LENGTH_SHORT).show();
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == REQUEST_IMAGE_CAPTURE){
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                image.setImageBitmap(bitmap);
                scan.setEnabled(true);
            }else if (requestCode == REQUEST_IMAGE_GALLEY){
                Uri uri = data.getData();
                String x = getPath(uri);
                Toast.makeText(getApplicationContext(),x,Toast.LENGTH_LONG).show();
                Bitmap bitmap = null;
                try{
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),uri);
                    image.setImageBitmap(bitmap);
                    scan.setEnabled(true);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    public String getPath(Uri uri){
        if(uri == null) return null;
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if(cursor!=null){
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        return uri.getPath();
    }

}
