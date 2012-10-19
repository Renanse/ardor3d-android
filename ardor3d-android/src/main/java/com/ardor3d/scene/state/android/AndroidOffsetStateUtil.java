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

package com.ardor3d.scene.state.android;

import javax.microedition.khronos.opengles.GL10;

import android.util.Log;

import com.ardor3d.framework.android.AndroidCanvas;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.OffsetState;
import com.ardor3d.renderer.state.OffsetState.OffsetType;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.OffsetStateRecord;

public abstract class AndroidOffsetStateUtil {

    public static void apply(final GL10 gl, final OffsetState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final OffsetStateRecord record = (OffsetStateRecord) context.getStateRecord(StateType.Offset);
        context.setCurrentState(StateType.Offset, state);

        if (state.isEnabled()) {
            // enable any set offset types
            AndroidOffsetStateUtil.setOffsetEnabled(gl, OffsetType.Fill, state.isTypeEnabled(OffsetType.Fill), record);

            // set factor and units.
            AndroidOffsetStateUtil.setOffset(gl, state.getFactor(), state.getUnits(), record);
        } else {
            // disable all offset types
            AndroidOffsetStateUtil.setOffsetEnabled(gl, OffsetType.Fill, false, record);

            // set factor and units to default 0, 0.
            AndroidOffsetStateUtil.setOffset(gl, 0, 0, record);
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void setOffsetEnabled(final GL10 gl, final OffsetType type, final boolean typeEnabled,
            final OffsetStateRecord record) {
        final int glType = AndroidOffsetStateUtil.getGLType(type);
        if (!record.isValid() || typeEnabled != record.enabledOffsets.contains(type)) {
            if (typeEnabled) {
                gl.glEnable(glType);
            } else {
                gl.glDisable(glType);
            }
        }
    }

    private static void setOffset(final GL10 gl, final float factor, final float units, final OffsetStateRecord record) {
        if (!record.isValid() || record.factor != factor || record.units != units) {
            gl.glPolygonOffset(factor, units);
            record.factor = factor;
            record.units = units;
        }
    }

    private static int getGLType(final OffsetType type) {
        switch (type) {
            case Fill:
                return GL10.GL_POLYGON_OFFSET_FILL;
            case Line:
            case Point:
                Log.w(AndroidCanvas.TAG, "OffsetType not support by this renderer: " + type);
                return GL10.GL_POLYGON_OFFSET_FILL;
        }
        throw new IllegalArgumentException("invalid type: " + type);
    }
}
