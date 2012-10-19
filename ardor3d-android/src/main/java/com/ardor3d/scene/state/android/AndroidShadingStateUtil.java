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
import com.ardor3d.renderer.state.ShadingState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.ShadingState.ShadingMode;
import com.ardor3d.renderer.state.record.ShadingStateRecord;

public abstract class AndroidShadingStateUtil {

    public static void apply(final GL10 gl, final ShadingState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final ShadingStateRecord record = (ShadingStateRecord) context.getStateRecord(StateType.Shading);
        context.setCurrentState(StateType.Shading, state);

        // If not enabled, we'll use smooth
        final int toApply = state.isEnabled() ? AndroidShadingStateUtil.getGLShade(state.getShadingMode())
                : GL10.GL_SMOOTH;
        // only apply if we're different. Update record to reflect any changes.
        if (!record.isValid() || toApply != record.lastShade) {
            gl.glShadeModel(toApply);
            record.lastShade = toApply;
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static int getGLShade(final ShadingMode shadeMode) {
        switch (shadeMode) {
            case Smooth:
                return GL10.GL_SMOOTH;
            case Flat:
                return GL10.GL_FLAT;
        }
        throw new IllegalStateException("unknown shade mode: " + shadeMode);
    }

}
