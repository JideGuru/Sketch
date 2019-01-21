package com.jideguru.sketch;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.IntBuffer;

public class MainActivity extends AppCompatActivity {

    private ImageView image;
    private Button select, sketch, colored;
    private final int SELECT_PHOTO = 1;
    private Bitmap originBitmap = null;
    private File tempFile = new File("/sdcard/a.jpg");
    private Bitmap processImg;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OpenCVLoader.initDebug();

        image = findViewById(R.id.image);

        select = findViewById(R.id.select_img);
        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showImageChooser();
            }
        });

        sketch = findViewById(R.id.sketch);
        sketch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (originBitmap != null) {
                    Sketch(originBitmap, "normal");
                }
            }
        });
        colored = findViewById(R.id.colored_sketch);
        colored.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (originBitmap != null) {
                    Sketch(originBitmap, "colored");
                }
            }
        });


    }



    private void showImageChooser() {

        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, SELECT_PHOTO);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_PHOTO && resultCode == RESULT_OK && null != data) {

            Uri selectedImage = data.getData();
            InputStream imageStream;
            try {
                imageStream = getContentResolver().openInputStream(selectedImage);
                originBitmap = BitmapFactory.decodeStream(imageStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if (originBitmap != null) {
                tempFile.delete();
                this.image.setImageBitmap(originBitmap);
            }
        }
    }


    public void Sketch(Bitmap bitmap, String type){

        bitmap=bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Mat src = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
        Mat dest = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
        Mat grey = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
        Mat invert = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
        Bitmap inv, gray;
        Utils.bitmapToMat(bitmap, src);
        Imgproc.cvtColor(src, grey, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(grey, grey, Imgproc.COLOR_GRAY2RGBA, 4);
        Core.bitwise_not( grey, invert);
        Imgproc.GaussianBlur(invert,invert, new Size(11,11),0);

        inv = Bitmap.createBitmap(invert.cols(),invert.rows(),Bitmap.Config.ARGB_8888);
        gray = Bitmap.createBitmap(invert.cols(),invert.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(invert, inv);
        Utils.matToBitmap(grey, gray);
        Bitmap b = null;
        if (type.equals("normal")) {
            b = ColorDodgeBlend(inv, gray);
        }else if(type.equals("colored")){
            b = ColorDodgeBlend(inv, bitmap);
        }
        processImg = Bitmap.createBitmap(dest.cols(),dest.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dest, processImg);

        image.setImageBitmap(b);

    }

    private static int colordodge(int in1, int in2) {
        float image = (float)in2;
        float mask = (float)in1;
        return ((int) ((image == 255) ? image:Math.min(255, (((long)mask << 8 ) / (255 - image)))));

    }

    public static Bitmap ColorDodgeBlend(Bitmap source, Bitmap layer) {
        Bitmap base = source.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap blend = layer.copy(Bitmap.Config.ARGB_8888, false);

        IntBuffer buffBase = IntBuffer.allocate(base.getWidth() * base.getHeight());
        base.copyPixelsToBuffer(buffBase);
        buffBase.rewind();

        IntBuffer buffBlend = IntBuffer.allocate(blend.getWidth() * blend.getHeight());
        blend.copyPixelsToBuffer(buffBlend);
        buffBlend.rewind();

        IntBuffer buffOut = IntBuffer.allocate(base.getWidth() * base.getHeight());
        buffOut.rewind();

        while (buffOut.position() < buffOut.limit()) {
            int filterInt = buffBlend.get();
            int srcInt = buffBase.get();

            int redValueFilter = Color.red(filterInt);
            int greenValueFilter = Color.green(filterInt);
            int blueValueFilter = Color.blue(filterInt);

            int redValueSrc = Color.red(srcInt);
            int greenValueSrc = Color.green(srcInt);
            int blueValueSrc = Color.blue(srcInt);

            int redValueFinal = colordodge(redValueFilter, redValueSrc);
            int greenValueFinal = colordodge(greenValueFilter, greenValueSrc);
            int blueValueFinal = colordodge(blueValueFilter, blueValueSrc);

            int pixel = Color.argb(255, redValueFinal, greenValueFinal, blueValueFinal);

            buffOut.put(pixel);
        }

        buffOut.rewind();

        base.copyPixelsFromBuffer(buffOut);
        blend.recycle();

        return base;
    }
}
