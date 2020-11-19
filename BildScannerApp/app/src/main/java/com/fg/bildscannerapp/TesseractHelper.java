package com.fg.bildscannerapp;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class TesseractHelper {

    public static final String TAG = "TESSERACTHELPER";

    public static final String TESS_DATA = "/tessdata";

    private TessBaseAPI tessBaseAPI;

    //private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString()+"/Tess";
    private static String DATA_PATH = "";


    public TesseractHelper(AssetManager a, File path){
        DATA_PATH = path.getAbsolutePath();
        Log.d("LOL", DATA_PATH);
        prepareTessData(a);
    }

    private void prepareTessData(AssetManager a){
        try {
            File dir = new File(DATA_PATH + TESS_DATA);
            if(!dir.exists()){
                dir.mkdir();
            }

            String[] fileList = a.list("");
            for (String fileName : fileList){
                String pathToData = DATA_PATH+TESS_DATA+"/"+fileName;

                if(!(new File(pathToData)).exists()){
                    InputStream in = a.open(fileName);
                    OutputStream out = new FileOutputStream(pathToData);

                    byte[] buff = new byte[1024];
                    int len;
                    while((len = in.read(buff)) > 0){
                        out.write(buff, 0, len);
                    }
                    in.close();
                    out.close();
                }
            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public String startOCR(Bitmap image){
        try {
            tessBaseAPI = new TessBaseAPI();
        } catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        tessBaseAPI.init(DATA_PATH, "deu");
        //tessBaseAPI.setVariable("tessedit_char_whitelist", " -@.abcdefghijklmnopqrstuvwxyzäöüÄÖÜABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
        tessBaseAPI.setImage(image);

        String result = "Bisher kein Ergebnis";
        try {
            result = tessBaseAPI.getUTF8Text();
        } catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        tessBaseAPI.end();
        return result;
    }
}
