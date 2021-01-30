package com.fg.bildscannerapp;

import android.Manifest;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "MainActivity";


    Zoomcameraview javaCameraView;
    Mat mGrayCannyTest;
    TesseractHelper tessHelper;
    OpenCVHelper openCVHelper;

    boolean doOnce = false;

    ArrayList<Rect> rects = new ArrayList<>();
    TextView displayResult;


    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(MainActivity.this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    javaCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    static{
        if (OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV was successfully loaded!");
        } else {
            Log.e(TAG, "OpenCV was failed to load!");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                1);

        javaCameraView = (Zoomcameraview) findViewById(R.id.opencv_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setZoomControl((SeekBar) findViewById(R.id.camera_zoom_controls));
        javaCameraView.setCvCameraViewListener(MainActivity.this);


        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        AssetManager assetManager = getAssets();

        tessHelper = new TesseractHelper(assetManager, getWindow().getContext().getFilesDir());
        openCVHelper = new OpenCVHelper();

        // UI
        displayResult = (TextView) findViewById(R.id.displayResult);
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayResult.setText("");
                scanButton.setText("Bild wird analysiert...");
                scanButton.setClickable(false);
                new Thread(new Runnable() {
                    public void run() {
                        //Bitmap b = Bitmap.createBitmap(mGrayCannyTest.width(), mGrayCannyTest.height(), Bitmap.Config.ARGB_8888);
                        //Utils.matToBitmap(mGrayCannyTest, b);
                        //String result = tessHelper.startOCR(b);
                        rects = openCVHelper.getRectAroundLicense(mGrayCannyTest);
                        while(rects.size() == 0){
                            rects = openCVHelper.getRectAroundLicense(mGrayCannyTest);
                        }
                        rects.subList(1,rects.size());
                        doOnce = true;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                scanButton.setText("Finde Kennzeichen");
                                scanButton.setClickable(true);
                                //displayResult.setText("test");
                            }
                        });
                    }
                }).start();
            }
        });

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mGrayCannyTest = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        mGrayCannyTest.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        System.gc(); // TODO May be unnecessary
        //mGrayCannyTest.release();

        mGrayCannyTest = inputFrame.rgba();

        //TODO implement maybe click feature, so the user can specify area

        if (doOnce) {
            Rect r = rects.get(0);
            Point p1 = new Point(r.x, r.y);
            Point p2 = new Point(r.x + r.width, r.y + r.height);
            //Imgproc.rectangle(mGrayCannyTest, p1, p2, new Scalar(0, 0, 255), 2);


            //TODO license plate recognition
            Mat thresh = new Mat();
            Imgproc.cvtColor(mGrayCannyTest, thresh, Imgproc.COLOR_RGB2GRAY);
            Imgproc.adaptiveThreshold(thresh, thresh, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 33, 40);

            Mat cropped = new Mat(thresh, r);

            Bitmap analyzed2 = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cropped, analyzed2);

            saveTempImage(analyzed2);

            String result = tessHelper.startOCR(analyzed2);
            Log.d("TEST2", result);
            displayResult.setText(result);

            doOnce = false;

    }
        return mGrayCannyTest;

    }

    /*------------------------------------ Store Image -------------------------------------------*/
    public void saveTempImage(Bitmap bitmap) {
        if (isExternalStorageWritable()) {
            saveImage(bitmap);
        }else{
            Toast.makeText(this, "Please provide permission to write on the Storage!", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImage(Bitmap finalBitmap) {

        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        Log.d("TEST2", root);
        File myDir = new File(root + "/sams_images");

        if (! myDir.exists()){
            myDir.mkdir();
            // If you require it to make the entire directory path including parents,
            // use directory.mkdirs(); here instead.
        }

        String  timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
        String fname = timeStamp +".jpg";

        File file = new File(myDir, fname);
        if (file.exists()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
    /*------------------------------------ ************* -----------------------------------------*/

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (javaCameraView != null){
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (javaCameraView != null){
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV was successfully loaded!");
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        } else {
            Log.e(TAG, "OpenCV was failed to load!");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        }
    }
}