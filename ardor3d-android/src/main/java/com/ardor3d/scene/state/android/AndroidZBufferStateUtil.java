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
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.state.record.ZBufferStateRecord;

public abstract class AndroidZBufferStateUtil {

    public static void apply(final GL10 gl, final ZBufferState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final ZBufferStateRecord record = (ZBufferStateRecord) context.getStateRecord(RenderState.StateType.ZBuffer);
        context.setCurrentState(RenderState.StateType.ZBuffer, state);

        AndroidZBufferStateUtil.enableDepthTest(gl, state.isEnabled(), record);
        if (state.isEnabled()) {
            int depthFunc = 0;
            switch (state.getFunction()) {
                case Never:
                    depthFunc = GL10.GL_NEVER;
                    break;
                case LessThan:
                    depthFunc = GL10.GL_LESS;
                    break;
                case EqualTo:
                    depthFunc = GL10.GL_EQUAL;
                    break;
                case LessThanOrEqualTo:
                    depthFunc = GL10.GL_LEQUAL;
                    break;
                case GreaterThan:
                    depthFunc = GL10.GL_GREATER;
                    break;
                case NotEqualTo:
                    depthFunc = GL10.GL_NOTEQUAL;
                    break;
                case GreaterThanOrEqualTo:
                    depthFunc = GL10.GL_GEQUAL;
                    break;
                case Always:
                    depthFunc = GL10.GL_ALWAYS;
            }
            AndroidZBufferStateUtil.applyFunction(gl, depthFunc, record);
        }

        AndroidZBufferStateUtil.enableWrite(gl, state.isWritable(), record);

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void enableDepthTest(final GL10 gl, final boolean enable, final ZBufferStateRecord record) {
        if (enable && (!record.depthTest || !record.isValid())) {
            gl.glEnable(GL10.GL_DEPTH_TEST);
            record.depthTest = true;
        } else if (!enable && (record.depthTest || !record.isValid())) {
            gl.glDisable(GL10.GL_DEPTH_TEST);
            record.depthTest = false;
        }
    }

    private static void applyFunction(final GL10 gl, final int depthFunc, final ZBufferStateRecord record) {
        if (depthFunc != record.depthFunc || !record.isValid()) {
            gl.glDepthFunc(depthFunc);
            record.depthFunc = depthFunc;
        }
    }

    private static void enableWrite(final GL10 gl, final boolean enable, final ZBufferStateRecord record) {
        if (enable != record.writable || !record.isValid()) {
            gl.glDepthMask(enable);
            record.writable = enable;
        }
    }

}
