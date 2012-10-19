/**
 * Copyright (c) 2009-2011 Ardor Labs, Inc. (http://ardorlabs.com/)
 *   
 * This file is part of Ardor3D-Android (http://ardor3d.com/).
 *   
 * Ardor3D-Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 2.1 of the License, or
 * (at your option) any later version.
 *   
 * Ardor3D-Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *   
 * You should have received a copy of the GNU Lesser General Public License
 * along with Ardor3D-Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ardor3d.extension.android;

import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import com.ardor3d.framework.android.AndroidCanvas;
import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.util.ImageLoader;

public class AndroidImageLoader implements ImageLoader {

    private static Matrix yFlipMatrix;
    static {
        AndroidImageLoader.yFlipMatrix = new Matrix();
        AndroidImageLoader.yFlipMatrix.postScale(1, -1); // flip Y axis
    }

    public Image load(final InputStream is, final boolean flipped) throws IOException {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        return loadFromBitMap(BitmapFactory.decodeStream(is), flipped, options);
    }

    public Image loadFromBitMap(Bitmap bitmap, final boolean flipped, final BitmapFactory.Options options) {
        if (bitmap == null) {
            // TODO: perhaps use other loaders as backup?
            Log.w(AndroidCanvas.TAG, "bitmap was null");
            return null;
        }

        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();

        // defaults
        ImageDataFormat format = bitmap.hasAlpha() ? ImageDataFormat.RGBA : ImageDataFormat.RGB;
        PixelDataType type = PixelDataType.UnsignedByte;

        if (bitmap.getConfig() != null) {
            switch (bitmap.getConfig()) {
                case ALPHA_8:
                    format = ImageDataFormat.Alpha;
                    type = PixelDataType.UnsignedByte;
                    break;
                case ARGB_4444:
                    format = ImageDataFormat.RGBA;
                    type = PixelDataType.UnsignedByte;
                    break;
                case ARGB_8888:
                    format = ImageDataFormat.RGBA;
                    type = PixelDataType.UnsignedByte;
                    break;
                case RGB_565:
                    format = ImageDataFormat.RGB;
                    type = PixelDataType.UnsignedByte;
                    break;
                default:
                    Log.w(AndroidCanvas.TAG, "Unhandled bitmap config.");
                    return null;
            }
        }

        if (flipped) {
            final Bitmap oldBitmap = bitmap;
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                    AndroidImageLoader.yFlipMatrix, false);
            oldBitmap.recycle();
        }

        return new AndroidImage(format, type, width, height, bitmap, null);
    }
}
