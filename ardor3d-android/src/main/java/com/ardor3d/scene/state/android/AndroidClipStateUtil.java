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

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.android.AndroidContextCapabilities;
import com.ardor3d.renderer.state.ClipState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.ClipStateRecord;
import com.ardor3d.util.geom.BufferUtils;

public abstract class AndroidClipStateUtil {

    private static FloatBuffer buf = BufferUtils.createFloatBuffer(4);

    public static void apply(final GL10 gl, final ClipState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final ClipStateRecord record = (ClipStateRecord) context.getStateRecord(StateType.Clip);

        if (((AndroidContextCapabilities) context.getCapabilities()).isOES11Suported()) {
            context.setCurrentState(StateType.Clip, state);

            final ContextCapabilities caps = context.getCapabilities();
            final int max = Math.min(ClipState.MAX_CLIP_PLANES, caps.getMaxUserClipPlanes());

            if (state.isEnabled()) {
                for (int i = 0; i < max; i++) {
                    AndroidClipStateUtil.enableClipPlane((GL11) gl, i, state.getPlaneEnabled(i), state, record);
                }
            } else {
                for (int i = 0; i < max; i++) {
                    AndroidClipStateUtil.enableClipPlane((GL11) gl, i, false, state, record);
                }
            }
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void enableClipPlane(final GL11 gl, final int planeIndex, final boolean enable,
            final ClipState state, final ClipStateRecord record) {
        if (enable) {
            if (!record.isValid() || !record.planeEnabled[planeIndex]) {
                gl.glEnable(GL11.GL_CLIP_PLANE0 + planeIndex);
                record.planeEnabled[planeIndex] = true;
            }

            AndroidClipStateUtil.buf.rewind();
            for (final double eq : state.getPlaneEquations(planeIndex)) {
                AndroidClipStateUtil.buf.put((float) eq);
            }
            AndroidClipStateUtil.buf.flip();
            gl.glClipPlanef(GL11.GL_CLIP_PLANE0 + planeIndex, AndroidClipStateUtil.buf);

        } else {
            if (!record.isValid() || record.planeEnabled[planeIndex]) {
                gl.glDisable(GL11.GL_CLIP_PLANE0 + planeIndex);
                record.planeEnabled[planeIndex] = false;
            }
        }
    }
}
