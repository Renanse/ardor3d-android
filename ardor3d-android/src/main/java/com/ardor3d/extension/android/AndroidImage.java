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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;

import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.google.common.collect.Lists;

public class AndroidImage extends Image {
    private static final long serialVersionUID = 1L;

    protected List<Bitmap> _bitmaps = Lists.newArrayList();

    public AndroidImage(final ImageDataFormat format, final PixelDataType type, final int width, final int height,
            final List<Bitmap> bitmaps, final int[] mipMapSizes) {
        super(format, type, width, height, new ArrayList<ByteBuffer>(), mipMapSizes);
        _bitmaps.addAll(bitmaps);
    }

    public AndroidImage(final ImageDataFormat format, final PixelDataType type, final int width, final int height,
            final Bitmap bitmap, final int[] mipMapSizes) {
        super(format, type, width, height, new ArrayList<ByteBuffer>(), mipMapSizes);
        _bitmaps.add(bitmap);
    }

    public List<Bitmap> getBitmaps() {
        return _bitmaps;
    }

    @Override
    public int getDataSize() {
        if (_bitmaps == null) {
            return 0;
        } else {
            return _bitmaps.size();
        }
    }

    public Bitmap getBitmap(final int index) {
        if (_bitmaps.size() > index) {
            return _bitmaps.get(index);
        } else {
            return null;
        }
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        // TODO: write bitmaps?
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        // TODO: read bitmaps
    }
}
