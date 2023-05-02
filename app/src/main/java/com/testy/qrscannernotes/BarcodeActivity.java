package com.testy.qrscannernotes;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class BarcodeActivity extends AppCompatActivity {

    static SQLiteDatabase myDatabase;
    Date currentTime;
    SimpleDateFormat df;
    private SurfaceView surfaceView;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    private ToneGenerator toneGenerator;
    private TextView barcodeText;
    private String barcodeData;

    ArrayList<String> QrObject = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode);

        Intent i = getIntent();
        Bundle extras = i.getExtras();
        ArrayList<String> s = extras.getStringArrayList("array");
        QrObject.addAll(s);

        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        surfaceView = findViewById(R.id.surface_view);
        barcodeText = findViewById(R.id.barcode_text);
        initialiseDetectorsAndSources();
        performSql();
    }

    public void performSql() {
        myDatabase = this.openOrCreateDatabase("Users", MODE_PRIVATE, null);
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS lastfourth (name VARCHAR , date VARCHAR , spec VARCHAR)");
    }

    private void addDataToDatabase() {
        String scannedResult = barcodeData;
        currentTime = Calendar.getInstance().getTime();
        df = new SimpleDateFormat("dd-MMM-yyyy");
        String formattedDate = df.format(currentTime);
        String d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
        System.out.println(formattedDate);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            boolean isContains = QrObject.stream().anyMatch(s -> s != scannedResult);
            if (isContains) {
                Toast.makeText(this, "Code already exist", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                String sql = "INSERT INTO lastfourth (name, date , spec) VALUES (? , ?, ?)";
                SQLiteStatement statement = myDatabase.compileStatement(sql);
                statement.bindString(1, scannedResult);
                statement.bindString(2, formattedDate);
                statement.bindString(3, d);
                statement.execute();
                QrDataModel qrDataModel = new QrDataModel(scannedResult, formattedDate, d);
                Intent intent = new Intent();
                intent.putExtra("item", qrDataModel);
                setResult(10, intent);
                finish();
            }
            Log.e("isContains", String.valueOf(isContains));
        }
    }

    private void initialiseDetectorsAndSources() {
        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setAutoFocusEnabled(true)
                .build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(BarcodeActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(surfaceView.getHolder());
                    } else {
                        ActivityCompat.requestPermissions(BarcodeActivity.this, new
                                String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });


        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {
                    barcodeText.post(() -> {
                        if (barcodes.valueAt(0).email != null) {
                            barcodeText.removeCallbacks(null);
                            barcodeData = barcodes.valueAt(0).email.address;
                            barcodeText.setText(barcodeData);
                            toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                        } else {
                            barcodeData = barcodes.valueAt(0).displayValue;
                            barcodeText.setText(barcodeData);
                            toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                            addDataToDatabase();
                        }
                    });

                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        getSupportActionBar().hide();
        cameraSource.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportActionBar().hide();
        initialiseDetectorsAndSources();
    }
}