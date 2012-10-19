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

import java.util.Stack;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import com.ardor3d.math.Rectangle2;
import com.ardor3d.math.type.ReadOnlyRectangle2;
import com.ardor3d.renderer.state.record.RendererRecord;

public class AndroidRendererUtil {

    public static void switchMode(final GL10 gl, final RendererRecord rendRecord, final int mode) {
        if (!rendRecord.isMatrixValid() || rendRecord.getMatrixMode() != mode) {
            gl.glMatrixMode(mode);
            rendRecord.setMatrixMode(mode);
            rendRecord.setMatrixValid(true);
        }
    }

    public static void setBoundVBO(final GL11 gl, final RendererRecord rendRecord, final int id) {
        if (!rendRecord.isVboValid() || rendRecord.getCurrentVboId() != id) {
            gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, id);
            rendRecord.setCurrentVboId(id);
            rendRecord.setVboValid(true);
        }
    }

    public static void setBoundElementVBO(final GL11 gl, final RendererRecord rendRecord, final int id) {
        if (!rendRecord.isElementVboValid() || rendRecord.getCurrentElementVboId() != id) {
            gl.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, id);
            rendRecord.setCurrentElementVboId(id);
            rendRecord.setElementVboValid(true);
        }
    }

    public static void applyScissors(final GL10 gl, final RendererRecord rendRecord) {
        final Stack<ReadOnlyRectangle2> clips = rendRecord.getScissorClips();

        if (clips.size() > 0) {
            final Rectangle2 init = Rectangle2.fetchTempInstance();
            init.set(-1, -1, -1, -1);
            ReadOnlyRectangle2 r;
            boolean first = true;
            for (int i = clips.size(); --i >= 0;) {
                r = clips.get(i);

                if (r == null) {
                    break;
                }
                if (first) {
                    init.set(r);
                    first = false;
                } else {
                    init.intersect(r, init);
                }
                if (init.getWidth() <= 0 || init.getHeight() <= 0) {
                    init.setWidth(0);
                    init.setHeight(0);
                    break;
                }
            }

            if (init.getWidth() == -1) {
                AndroidRendererUtil.setClippingEnabled(gl, rendRecord, false);
            } else {
                AndroidRendererUtil.setClippingEnabled(gl, rendRecord, true);
                gl.glScissor(init.getX(), init.getY(), init.getWidth(), init.getHeight());
            }
            Rectangle2.releaseTempInstance(init);
        } else {
            // no clips, so disable
            AndroidRendererUtil.setClippingEnabled(gl, rendRecord, false);
        }
    }

    public static void setClippingEnabled(final GL10 gl, final RendererRecord rendRecord, final boolean enabled) {
        if (enabled && (!rendRecord.isClippingTestValid() || !rendRecord.isClippingTestEnabled())) {
            gl.glEnable(GL10.GL_SCISSOR_TEST);
            rendRecord.setClippingTestEnabled(true);
        } else if (!enabled && (!rendRecord.isClippingTestValid() || rendRecord.isClippingTestEnabled())) {
            gl.glDisable(GL10.GL_SCISSOR_TEST);
            rendRecord.setClippingTestEnabled(false);
        }
        rendRecord.setClippingTestValid(true);
    }
}
