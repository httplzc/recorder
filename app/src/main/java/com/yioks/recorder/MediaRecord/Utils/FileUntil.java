package com.yioks.recorder.MediaRecord.Utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Lzc on 2018/3/21 0021.
 */

public class FileUntil {

    private final static float ignoreSize = 1024 * 50;
    public static Bitmap rotateBitmap(Bitmap img, int angle) {
        if (angle == 0)
            return img;
        try {
            Matrix matrix = new Matrix();
            matrix.postRotate(angle); /*翻转90度*/
            int width = img.getWidth();
            int height = img.getHeight();
            img = Bitmap.createBitmap(img, 0, 0, width, height, matrix, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return img;
    }

    //清除文件内容
    public static void clearFile(File file) throws IOException {
        if (file == null || !file.exists())
            return;
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("");
        fileWriter.flush();
        fileWriter.close();
    }

    //保存bitmap 为文件
    public static File saveImageAndGetFile(Bitmap bitmap, File file, float limitSize, Bitmap.CompressFormat compressFormat) {
        if (bitmap == null || file == null) {
            return null;
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            if (limitSize != -1) {
                compressImageFileSize(bitmap, fos, limitSize);
            } else {
                bitmap.compress(compressFormat, 100, fos);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    /**
     * @param image
     * @param outputStream
     * @param limitSize    单位byte
     * @throws IOException
     */
    public static void compressImageFileSize(Bitmap image, OutputStream outputStream, float limitSize) throws Exception {
        ByteArrayOutputStream imgBytes = new ByteArrayOutputStream();
        int options = 100;
        image.compress(Bitmap.CompressFormat.JPEG, options, imgBytes);
        while (imgBytes.size() > limitSize && imgBytes.size() > ignoreSize && options > 20) {
            imgBytes.reset();
            int dx = 0;
            float dz = (float) imgBytes.size() / limitSize;
            if (dz > 2)
                dx = 30;
            else if (dz > 1)
                dx = 25;
            else
                dx = 20;
            options -= dx;
            image.compress(Bitmap.CompressFormat.JPEG, options, imgBytes);
//            Log.i("lzc", "compressImageFileSize   " + options + "---" + imgBytes.size() +"---"+image.getWidth()+"---"+image.getHeight());
        }

        outputStream.write(imgBytes.toByteArray());
        imgBytes.close();
        outputStream.close();
    }
}
