package com.fg.bildscannerapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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
import android.widget.TextView;

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
import java.util.Random;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "MainActivity";


    JavaCameraView javaCameraView;
    Mat mGrayCannyTest;
    TesseractHelper tessHelper;
    OpenCVHelper openCVHelper;



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

        javaCameraView = (JavaCameraView) findViewById(R.id.opencv_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(MainActivity.this);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        AssetManager assetManager = getAssets();

        tessHelper = new TesseractHelper(assetManager, getWindow().getContext().getFilesDir());
        openCVHelper = new OpenCVHelper();

        // UI
        TextView displayResult = (TextView) findViewById(R.id.displayResult);
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayResult.setText("");
                scanButton.setText("Bild wird analysiert...");
                scanButton.setClickable(false);
                new Thread(new Runnable() {
                    public void run() {
                        Bitmap b = Bitmap.createBitmap(mGrayCannyTest.width(), mGrayCannyTest.height(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(mGrayCannyTest, b);
                        String result = tessHelper.startOCR(b);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                scanButton.setText("Analysieren");
                                scanButton.setClickable(true);
                                displayResult.setText(result);
                            }
                        });
                    }
                }).start();
            }
        });

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mGrayCannyTest = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mGrayCannyTest.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        System.gc(); // TODO May be unnecessary
        mGrayCannyTest.release();

        mGrayCannyTest = inputFrame.rgba();

        //TODO license plate recognition
        //Imgproc.adaptiveThreshold(mGrayCannyTest, mGrayCannyTest, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 53, 34);
        //Imgproc.Canny(mGrayCannyTest, mGrayCannyTest, 60, 60*3);

        //TODO implement maybe click feature, so the user can specify area

        Rect r = openCVHelper.getRectAroundLicense(inputFrame.gray(), mGrayCannyTest);
        Point p1 = new Point(r.x, r.y);
        Point p2 = new Point(r.x + r.width, r.y + r.height);
        Imgproc.rectangle(mGrayCannyTest, p1, p2, new Scalar(0, 0, 255), 2);

        return mGrayCannyTest;
    }

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