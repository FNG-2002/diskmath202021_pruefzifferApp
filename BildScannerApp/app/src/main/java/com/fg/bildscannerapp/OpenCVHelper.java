package com.fg.bildscannerapp;

import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OpenCVHelper {

    public OpenCVHelper(){

    }

    public Rect getRectAroundLicense(Mat imageToProcessGray, Mat rgbImage){

        Mat bilateral = new Mat();
        Imgproc.bilateralFilter(imageToProcessGray, bilateral, 13, 75, 25);

        Mat edges = new Mat();
        Imgproc.Canny(imageToProcessGray, edges, 30, 200);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 1));
        Imgproc.dilate(edges, edges, kernel);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat(); // TODO delete/release directly because unnecessary
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        hierarchy.release();

        try {
            Collections.sort(contours, new Comparator<MatOfPoint>() {
                public int compare(MatOfPoint o1, MatOfPoint o2) {
                    double area1 = Imgproc.contourArea(o1);
                    double area2 = Imgproc.contourArea(o2);
                    return (int) (area2 - area1);

                }
            });
        } catch (Exception e){
            Log.e("LOL", "Nerviger comparison error");
        }

        // release unnecessary
        bilateral.release();
        edges.release();
        kernel.release();

        Rect rectLicense = new Rect();
        double angle = 0;
        for (MatOfPoint contour : contours){
            MatOfPoint2f approx = new MatOfPoint2f();
            MatOfPoint2f c = new MatOfPoint2f();
            contour.convertTo(c, CvType.CV_32FC2);

            double peri = Imgproc.arcLength(c, true);
            // default is 0.018
            double acc = 0.018;
            Imgproc.approxPolyDP(c, approx, acc*peri, true);
            if(approx.total() == 4) {
                rectLicense = Imgproc.boundingRect(contour);
                if ((rectLicense.width > rectLicense.height) && (rectLicense.width/rectLicense.height) == 4 && rectLicense.area() > 7500 && rectLicense.area() < 40000){
                    Log.d("LOL", "ratio: " + rectLicense.width*1.0/rectLicense.height);
                    Log.d("LOL", "area: " + rectLicense.area());
                    angle = Imgproc.minAreaRect(c).angle;
                    break;
                }
            }
        }


        return rectLicense;
    }
}
