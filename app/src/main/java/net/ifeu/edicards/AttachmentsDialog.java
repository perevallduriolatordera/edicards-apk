package net.ifeu.edicards;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsFolders;
import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.library.Controls.ButtonColor;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;

public class AttachmentsDialog extends Activity {

    AppConfig _appConfig;
    private static final int REQUEST_IMAGE_CAPTURE_FRONT = 1;
    private static final int REQUEST_IMAGE_CAPTURE_BACK = 2;
    private static final int REQUEST_CAMERA_PERMISSION = 3;

    private boolean pendingFrontPhoto = false;
    private boolean pendingBackPhoto = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attachments_dialog);

        _appConfig = (AppConfig) this.getApplicationContext();

        ButtonColor takeFrontPhoto = (ButtonColor) findViewById(R.id.btnTakeFrontPhoto);
        takeFrontPhoto.changeAspect(this, R.color.Black, getResources().getDrawable(R.drawable.ic_camera));

        ButtonColor takeBackPhoto = (ButtonColor) findViewById(R.id.btnTakeBackPhoto);
        takeBackPhoto.changeAspect(this, R.color.Black, getResources().getDrawable(R.drawable.ic_camera));

        ButtonColor close = (ButtonColor) findViewById(R.id.btnClose);
        close.changeAspect(this, R.color.Black, getResources().getDrawable(R.drawable.ic_close));

        takeFrontPhoto.setOnClickListener( (View v) -> {
            if (checkCameraPermission()) {
                dispatchTakePictureIntent(true);
            } else {
                pendingFrontPhoto = true;
                pendingBackPhoto = false;
                requestCameraPermission();
            }
        });

        takeBackPhoto.setOnClickListener( (View v)-> {
            if (checkCameraPermission()) {
                dispatchTakePictureIntent(false);
            } else {
                pendingFrontPhoto = false;
                pendingBackPhoto = true;
                requestCameraPermission();
            }
        });

        if (!StringUtils.isEmpty(_appConfig.getWorkingArea().CurrentTransactionMetadata.NewCustomerFrontDocument)) {
            ImageView imageView = (ImageView) findViewById(R.id.imgFrontPhoto);
            imageView.setImageURI(Uri.parse(_appConfig.getWorkingArea().CurrentTransactionMetadata.NewCustomerFrontDocument));
        }

        if (!StringUtils.isEmpty(_appConfig.getWorkingArea().CurrentTransactionMetadata.NewCustomerBackDocument)) {
            ImageView imageView = (ImageView) findViewById(R.id.imgBackPhoto);
            imageView.setImageURI(Uri.parse(_appConfig.getWorkingArea().CurrentTransactionMetadata.NewCustomerBackDocument));
        }

        close.setOnClickListener( (View v)-> {
            finish();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_articulo_dialog, menu);
        return true;
    }

    private void dispatchTakePictureIntent(boolean isFrontDocument) {

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile;
            try {
                photoFile = createImageFile(isFrontDocument);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            if (photoFile != null) {
                Uri photoURI = Uri.fromFile(photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, isFrontDocument ? REQUEST_IMAGE_CAPTURE_FRONT : REQUEST_IMAGE_CAPTURE_BACK);
            }
        }
    }

    private File createImageFile(boolean isFrontDocument) throws IOException {
        String imageFileName = (isFrontDocument ? "F" : "B") + "_" + _appConfig.getWorkingArea().CurrentTransactionMetadata.GUID;
        File storageDir = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_CUSTOMER_DOCUMENT + "/");
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);

        if (isFrontDocument)
            _appConfig.getWorkingArea().CurrentTransactionMetadata.NewCustomerFrontDocument = image.getAbsolutePath();
        else
            _appConfig.getWorkingArea().CurrentTransactionMetadata.NewCustomerBackDocument = image.getAbsolutePath();

        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE_FRONT && resultCode == RESULT_OK) {
            // The photo was taken and saved successfully
            // Display the photo
            ImageView imageView = (ImageView) findViewById(R.id.imgFrontPhoto);
            imageView.setImageURI(Uri.parse(_appConfig.getWorkingArea().CurrentTransactionMetadata.NewCustomerFrontDocument));
        }

        if (requestCode == REQUEST_IMAGE_CAPTURE_BACK && resultCode == RESULT_OK) {
            // The photo was taken and saved successfully
            // Display the photo
            ImageView imageView = (ImageView) findViewById(R.id.imgBackPhoto);
            imageView.setImageURI(Uri.parse(_appConfig.getWorkingArea().CurrentTransactionMetadata.NewCustomerBackDocument));
        }
    }

    private boolean checkCameraPermission() {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            new String[]{Manifest.permission.CAMERA},
            REQUEST_CAMERA_PERMISSION
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso otorgado - ejecutar la acción pendiente
                if (pendingFrontPhoto) {
                    dispatchTakePictureIntent(true);
                    pendingFrontPhoto = false;
                } else if (pendingBackPhoto) {
                    dispatchTakePictureIntent(false);
                    pendingBackPhoto = false;
                }
            } else {
                // Permiso denegado
                Toast.makeText(this, "Se necesita permiso de cámara para tomar fotos de documentos", Toast.LENGTH_LONG).show();
                pendingFrontPhoto = false;
                pendingBackPhoto = false;
            }
        }
    }


}
