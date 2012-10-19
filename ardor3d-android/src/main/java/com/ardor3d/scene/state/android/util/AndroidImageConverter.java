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

package com.ardor3d.scene.state.android.util;

import java.nio.ByteBuffer;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.ardor3d.extension.android.AndroidImage;
import com.ardor3d.image.Image;
import com.ardor3d.image.PixelDataType;
import com.google.common.collect.Lists;

public class AndroidImageConverter {

    public static AndroidImage convert(final Image image) {
        if (image instanceof AndroidImage) {
            return (AndroidImage) image;
        }

        if (image.getDataType() != PixelDataType.UnsignedByte) {
            throw new Error("Unhandled Ardor3D image data type: " + image.getDataType());
        }

        // count the number of layers we will be converting.
        final int size = image.getData().size();

        // grab our image width and height
        final int width = image.getWidth(), height = image.getHeight();

        // create our bitmap list
        final List<Bitmap> bitmapList = Lists.newArrayList();

        final boolean alpha;
        final boolean rgb;
        final int components;
        switch (image.getDataFormat()) {
            case Alpha:
                rgb = false;
                alpha = true;
                components = 1;
                break;
            case RGBA:
                rgb = true;
                alpha = true;
                components = 4;
                break;
            // Falls through on purpose.
            case RGB:
                rgb = true;
                alpha = false;
                components = 3;
                break;
            default:
                throw new Error("Unhandled image data format: " + image.getDataFormat());
        }

        // go through each layer
        for (int i = 0; i < size; i++) {
            Bitmap bitmap;
            final ByteBuffer data = image.getData(i);
            data.rewind();

            if (rgb) {
                if (alpha) {
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
                } else {
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                }
                int index, r, g, b, a;
                // Go through each pixel
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        index = components * (y * width + x);
                        r = Math.round(data.get(index + 0) & 0xFF);
                        g = Math.round(data.get(index + 1) & 0xFF);
                        b = Math.round(data.get(index + 2) & 0xFF);

                        if (alpha) {
                            a = Math.round(data.get(index + 3) & 0xFF);
                            bitmap.setPixel(x, y, Color.argb(a, r, g, b));
                        } else {
                            bitmap.setPixel(x, y, Color.rgb(r, g, b));
                        }
                    }
                }
            } else {
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
                int a;
                // Go through each pixel
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        a = data.get(y * width + x) & 0xFF;
                        bitmap.setPixel(x, y, Color.alpha(a));
                    }
                }
            }

            // add to our list
            bitmapList.add(bitmap);
        }

        // return list
        return new AndroidImage(image.getDataFormat(), image.getDataType(), width, height, bitmapList, image
                .getMipMapByteSizes());
    }
}
