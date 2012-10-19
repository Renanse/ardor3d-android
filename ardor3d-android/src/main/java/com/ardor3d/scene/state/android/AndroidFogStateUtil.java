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

import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.FogState;
import com.ardor3d.renderer.state.FogState.CoordinateSource;
import com.ardor3d.renderer.state.FogState.DensityFunction;
import com.ardor3d.renderer.state.FogState.Quality;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.FogStateRecord;

public abstract class AndroidFogStateUtil {

    public static void apply(final GL10 gl, final FogState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();
        final FogStateRecord record = (FogStateRecord) context.getStateRecord(StateType.Fog);
        context.setCurrentState(StateType.Fog, state);

        if (state.isEnabled()) {
            AndroidFogStateUtil.enableFog(gl, true, record);

            if (record.isValid()) {
                if (record.fogStart != state.getStart()) {
                    gl.glFogf(GL10.GL_FOG_START, state.getStart());
                    record.fogStart = state.getStart();
                }
                if (record.fogEnd != state.getEnd()) {
                    gl.glFogf(GL10.GL_FOG_END, state.getEnd());
                    record.fogEnd = state.getEnd();
                }
                if (record.density != state.getDensity()) {
                    gl.glFogf(GL10.GL_FOG_DENSITY, state.getDensity());
                    record.density = state.getDensity();
                }
            } else {
                gl.glFogf(GL10.GL_FOG_START, state.getStart());
                record.fogStart = state.getStart();
                gl.glFogf(GL10.GL_FOG_END, state.getEnd());
                record.fogEnd = state.getEnd();
                gl.glFogf(GL10.GL_FOG_DENSITY, state.getDensity());
                record.density = state.getDensity();
            }

            final ReadOnlyColorRGBA fogColor = state.getColor();
            AndroidFogStateUtil.applyFogColor(gl, fogColor, record);
            AndroidFogStateUtil.applyFogMode(gl, state.getDensityFunction(), record);
            AndroidFogStateUtil.applyFogHint(gl, state.getQuality(), record);
            AndroidFogStateUtil.applyFogSource(gl, state.getSource(), record, caps);
        } else {
            AndroidFogStateUtil.enableFog(gl, false, record);
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void enableFog(final GL10 gl, final boolean enable, final FogStateRecord record) {
        if (record.isValid()) {
            if (enable && !record.enabled) {
                gl.glEnable(GL10.GL_FOG);
                record.enabled = true;
            } else if (!enable && record.enabled) {
                gl.glDisable(GL10.GL_FOG);
                record.enabled = false;
            }
        } else {
            if (enable) {
                gl.glEnable(GL10.GL_FOG);
            } else {
                gl.glDisable(GL10.GL_FOG);
            }
            record.enabled = enable;
        }
    }

    private static void applyFogColor(final GL10 gl, final ReadOnlyColorRGBA color, final FogStateRecord record) {
        if (!record.isValid() || !color.equals(record.fogColor)) {
            record.fogColor.set(color);
            record.colorBuff.clear();
            record.colorBuff.put(record.fogColor.getRed()).put(record.fogColor.getGreen()).put(
                    record.fogColor.getBlue()).put(record.fogColor.getAlpha());
            record.colorBuff.flip();
            gl.glFogfv(GL10.GL_FOG_COLOR, record.colorBuff);
        }
    }

    private static void applyFogSource(final GL10 gl, final CoordinateSource source, final FogStateRecord record,
            final ContextCapabilities caps) {
        if (caps.isFogCoordinatesSupported()) {
            // XXX: Not supported in OES 1.0 or 1.1
            // if (!record.isValid() || !source.equals(record.source)) {
            // if (source == CoordinateSource.Depth) {
            // gl.glFogx(GL10.GL_FOG_COORDINATE_SOURCE_EXT, GL10.GL_FRAGMENT_DEPTH_EXT);
            // } else {
            // gl.glFogx(GL10.GL_FOG_COORDINATE_SOURCE_EXT, GL10.GL_FOG_COORDINATE_EXT);
            // }
            // }
        }
    }

    private static void applyFogMode(final GL10 gl, final DensityFunction densityFunction, final FogStateRecord record) {
        int glMode = 0;
        switch (densityFunction) {
            case Exponential:
                glMode = GL10.GL_EXP;
                break;
            case Linear:
                glMode = GL10.GL_LINEAR;
                break;
            case ExponentialSquared:
                glMode = GL10.GL_EXP2;
                break;
        }

        if (!record.isValid() || record.fogMode != glMode) {
            gl.glFogx(GL10.GL_FOG_MODE, glMode);
            record.fogMode = glMode;
        }
    }

    private static void applyFogHint(final GL10 gl, final Quality quality, final FogStateRecord record) {
        int glHint = 0;
        switch (quality) {
            case PerVertex:
                glHint = GL10.GL_FASTEST;
                break;
            case PerPixel:
                glHint = GL10.GL_NICEST;
                break;
        }

        if (!record.isValid() || record.fogHint != glHint) {
            gl.glHint(GL10.GL_FOG_HINT, glHint);
            record.fogHint = glHint;
        }
    }

}
