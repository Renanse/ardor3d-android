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

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.CullState.Face;
import com.ardor3d.renderer.state.CullState.PolygonWind;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.CullStateRecord;

public abstract class AndroidCullStateUtil {

    public static void apply(final GL10 gl, final CullState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final CullStateRecord record = (CullStateRecord) context.getStateRecord(StateType.Cull);
        context.setCurrentState(StateType.Cull, state);

        if (state.isEnabled()) {
            final Face useCullMode = state.getCullFace();

            switch (useCullMode) {
                case Front:
                    AndroidCullStateUtil.setCull(gl, GL10.GL_FRONT, state, record);
                    AndroidCullStateUtil.setCullEnabled(gl, true, state, record);
                    break;
                case Back:
                    AndroidCullStateUtil.setCull(gl, GL10.GL_BACK, state, record);
                    AndroidCullStateUtil.setCullEnabled(gl, true, state, record);
                    break;
                case FrontAndBack:
                    AndroidCullStateUtil.setCull(gl, GL10.GL_FRONT_AND_BACK, state, record);
                    AndroidCullStateUtil.setCullEnabled(gl, true, state, record);
                    break;
                case None:
                    AndroidCullStateUtil.setCullEnabled(gl, false, state, record);
                    break;
            }
            AndroidCullStateUtil.setGLPolygonWind(gl, state.getPolygonWind(), state, record);
        } else {
            AndroidCullStateUtil.setCullEnabled(gl, false, state, record);
            AndroidCullStateUtil.setGLPolygonWind(gl, PolygonWind.CounterClockWise, state, record);
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void setCullEnabled(final GL10 gl, final boolean enable, final CullState state,
            final CullStateRecord record) {
        if (!record.isValid() || record.enabled != enable) {
            if (enable) {
                gl.glEnable(GL10.GL_CULL_FACE);
            } else {
                gl.glDisable(GL10.GL_CULL_FACE);
            }
            record.enabled = enable;
        }
    }

    private static void setCull(final GL10 gl, final int face, final CullState state, final CullStateRecord record) {
        if (!record.isValid() || record.face != face) {
            gl.glCullFace(face);
            record.face = face;
        }
    }

    private static void setGLPolygonWind(final GL10 gl, final PolygonWind windOrder, final CullState state,
            final CullStateRecord record) {
        if (!record.isValid() || record.windOrder != windOrder) {
            switch (windOrder) {
                case CounterClockWise:
                    gl.glFrontFace(GL10.GL_CCW);
                    break;
                case ClockWise:
                    gl.glFrontFace(GL10.GL_CW);
                    break;
            }
            record.windOrder = windOrder;
        }
    }
}
