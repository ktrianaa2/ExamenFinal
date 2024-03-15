package com.example.examenfinal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.examenfinal.ml.Banderas;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {


    public static int REQUEST_CAMERA = 111;
    public static int REQUEST_GALLERY = 222;
    private GoogleMap mMap;


    Bitmap mSelectedImage;
    ImageView mImageView;
    TextView txtResults;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtResults = findViewById(R.id.txtresults);
        mImageView = findViewById(R.id.image_view);



        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);

        // Configurar el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    public void abrirGaleria(View view) {
        Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_GALLERY);
    }


    public void abrirCamera(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }


    public void PersonalizedModel(View v){
        try {
            String[] etiquetas =
                    {"AR","BE","BR","CO","CR","EC","ES","FR","GB","MX","PT","SE","UY"};
            Banderas model = Banderas.newInstance(getApplicationContext());
            TensorBuffer inputFeature0 =
                    TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3},
                            DataType.FLOAT32);
            inputFeature0.loadBuffer(convertirImagenATensorBuffer(mSelectedImage));
            Banderas.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 =
                    outputs.getOutputFeature0AsTensorBuffer();
            txtResults.setText(obtenerEtiquetayProbabilidad(etiquetas,
                    outputFeature0.getFloatArray()));
            model.close();
        } catch (Exception e) {
            txtResults.setText(e.getMessage());
        }
    }

    public ByteBuffer convertirImagenATensorBuffer(Bitmap mSelectedImage){
        Bitmap imagen = Bitmap.createScaledBitmap(mSelectedImage, 224,
                224, true);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[224 * 224];
        imagen.getPixels(intValues, 0, imagen.getWidth(), 0, 0,
                imagen.getWidth(), imagen.getHeight());
        int pixel = 0;
        for(int i = 0; i < imagen.getHeight(); i ++){
            for(int j = 0; j < imagen.getWidth(); j++){
                int val = intValues[pixel++]; // RGB
                byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
            }
        }
        return byteBuffer;
    }

    public String obtenerEtiquetayProbabilidad(String[] etiquetas,
                                               float[] probabilidades){
        float valorMayor=Float.MIN_VALUE;
        int pos=-1;
        for (int i = 0; i < probabilidades.length; i++) {
            if (probabilidades[i] > valorMayor) {
                valorMayor = probabilidades[i];
                pos = i;
            }
        }
        return "Predicción: " + etiquetas[pos] + ", Probabilidad: " +
                (new DecimalFormat("0.00").format(probabilidades[pos]
                        * 100)) + "%";
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        mMap.setOnMapClickListener(this);


    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        
    }
}