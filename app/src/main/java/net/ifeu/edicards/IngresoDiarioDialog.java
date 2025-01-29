package net.ifeu.edicards;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsFolders;
import net.ifeu.edicards.DataTier.TransactionMetadata;
import net.ifeu.library.Controls.ButtonColor;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class IngresoDiarioDialog extends Activity {

    private static final int REQUEST_IMAGE_INGRESO_DIARIO = 3;

    AppConfig _appConfig;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingresos_diarios_dialog);

        _appConfig = (AppConfig) this.getApplicationContext();

        ButtonColor takePhoto = findViewById(R.id.btnTakePhoto);
        takePhoto.changeAspect(this, R.color.Black, getResources().getDrawable(R.drawable.ic_camera));

        ButtonColor close = findViewById(R.id.btnClose);
        close.changeAspect(this, R.color.Black, getResources().getDrawable(R.drawable.ic_close));

        takePhoto.setOnClickListener( (View v) -> {
            dispatchTakePictureIntent();
        });


        if (!StringUtils.isEmpty(_appConfig.getWorkingArea().CurrentTransactionMetadata.IngresoDocument)) {
            ImageView imageView = (ImageView) findViewById(R.id.imgPhoto);
            imageView.setImageURI(Uri.parse(_appConfig.getWorkingArea().CurrentTransactionMetadata.IngresoDocument));
        }

        close.setOnClickListener( (View v)-> {
            finish();
        });

        if (_appConfig.getWorkingArea().CurrentTransactionMetadata == null) {
            _appConfig.getWorkingArea().CurrentTransactionMetadata = new TransactionMetadata();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_articulo_dialog, menu);
        return true;
    }

    private void dispatchTakePictureIntent() {

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            if (photoFile != null) {
                Uri photoURI = Uri.fromFile(photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_INGRESO_DIARIO);
            }
        }
    }

    private File createImageFile() throws IOException {
        String imageFileName = _appConfig.getWorkingArea().CurrentTransactionMetadata.GUID;
        File storageDir = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_INGRESO_DIARIO + "/");
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        _appConfig.getWorkingArea().CurrentTransactionMetadata.IngresoDocument = image.getAbsolutePath();

        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_INGRESO_DIARIO && resultCode == RESULT_OK) {
            // The photo was taken and saved successfully
            // Display the photo
            ImageView imageView = findViewById(R.id.imgPhoto);
            imageView.setImageURI(Uri.parse(_appConfig.getWorkingArea().CurrentTransactionMetadata.IngresoDocument));
        }
    }


}
