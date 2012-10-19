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
import javax.microedition.khronos.opengles.GL11ExtensionPack;

import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.StencilState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.StencilState.StencilFunction;
import com.ardor3d.renderer.state.StencilState.StencilOperation;
import com.ardor3d.renderer.state.record.StencilStateRecord;

public abstract class AndroidStencilStateUtil {

    public static void apply(final GL10 gl, final StencilState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();
        final StencilStateRecord record = (StencilStateRecord) context.getStateRecord(StateType.Stencil);
        context.setCurrentState(StateType.Stencil, state);

        AndroidStencilStateUtil.setEnabled(gl, state.isEnabled(), caps.isTwoSidedStencilSupported() ? state
                .isUseTwoSided() : false, record, caps);
        if (state.isEnabled()) {
            if (state.isUseTwoSided() && caps.isTwoSidedStencilSupported()) {
                // XXX: two sided stencil not supported in OES 1.0 or 1.1
                // gl.glActiveStencilFaceEXT(GL10.GL_BACK);
                // AndroidStencilStateUtil.applyMask(gl, state.getStencilWriteMaskBack(), record, 2);
                // AndroidStencilStateUtil.applyFunc(gl, AndroidStencilStateUtil.getGLStencilFunction(state
                // .getStencilFunctionBack()), state.getStencilReferenceBack(), state.getStencilFuncMaskBack(),
                // record, 2);
                // AndroidStencilStateUtil.applyOp(gl, AndroidStencilStateUtil.getGLStencilOp(
                // state.getStencilOpFailBack(), caps), AndroidStencilStateUtil.getGLStencilOp(state
                // .getStencilOpZFailBack(), caps), AndroidStencilStateUtil.getGLStencilOp(state
                // .getStencilOpZPassBack(), caps), record, 2);
                //
                // gl.glActiveStencilFaceEXT(GL10.GL_FRONT);
                // AndroidStencilStateUtil.applyMask(gl, state.getStencilWriteMaskFront(), record, 1);
                // AndroidStencilStateUtil.applyFunc(gl, AndroidStencilStateUtil.getGLStencilFunction(state
                // .getStencilFunctionFront()), state.getStencilReferenceFront(), state.getStencilFuncMaskFront(),
                // record, 1);
                // AndroidStencilStateUtil.applyOp(AndroidStencilStateUtil.getGLStencilOp(state.getStencilOpFailFront(),
                // caps), AndroidStencilStateUtil.getGLStencilOp(state.getStencilOpZFailFront(), caps),
                // AndroidStencilStateUtil.getGLStencilOp(state.getStencilOpZPassFront(), caps), record, 1);
            } else {
                AndroidStencilStateUtil.applyMask(gl, state.getStencilWriteMaskFront(), record, 0);
                AndroidStencilStateUtil.applyFunc(gl, AndroidStencilStateUtil.getGLStencilFunction(state
                        .getStencilFunctionFront()), state.getStencilReferenceFront(), state.getStencilFuncMaskFront(),
                        record, 0);
                AndroidStencilStateUtil.applyOp(gl, AndroidStencilStateUtil.getGLStencilOp(state
                        .getStencilOpFailFront(), caps), AndroidStencilStateUtil.getGLStencilOp(state
                        .getStencilOpZFailFront(), caps), AndroidStencilStateUtil.getGLStencilOp(state
                        .getStencilOpZPassFront(), caps), record, 0);
            }
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static int getGLStencilFunction(final StencilFunction function) {
        switch (function) {
            case Always:
                return GL10.GL_ALWAYS;
            case Never:
                return GL10.GL_NEVER;
            case EqualTo:
                return GL10.GL_EQUAL;
            case NotEqualTo:
                return GL10.GL_NOTEQUAL;
            case GreaterThan:
                return GL10.GL_GREATER;
            case GreaterThanOrEqualTo:
                return GL10.GL_GEQUAL;
            case LessThan:
                return GL10.GL_LESS;
            case LessThanOrEqualTo:
                return GL10.GL_LEQUAL;
        }
        throw new IllegalArgumentException("unknown function: " + function);
    }

    private static int getGLStencilOp(final StencilOperation operation, final ContextCapabilities caps) {
        switch (operation) {
            case Keep:
                return GL10.GL_KEEP;
            case DecrementWrap:
                if (caps.isStencilWrapSupported()) {
                    return GL11ExtensionPack.GL_DECR_WRAP;
                }
                // FALLS THROUGH
            case Decrement:
                return GL10.GL_DECR;
            case IncrementWrap:
                if (caps.isStencilWrapSupported()) {
                    return GL11ExtensionPack.GL_INCR_WRAP;
                }
                // FALLS THROUGH
            case Increment:
                return GL10.GL_INCR;
            case Invert:
                return GL10.GL_INVERT;
            case Replace:
                return GL10.GL_REPLACE;
            case Zero:
                return GL10.GL_ZERO;
        }
        throw new IllegalArgumentException("unknown operation: " + operation);
    }

    private static void setEnabled(final GL10 gl, final boolean enable, final boolean twoSided,
            final StencilStateRecord record, final ContextCapabilities caps) {
        if (record.isValid()) {
            if (enable && !record.enabled) {
                gl.glEnable(GL10.GL_STENCIL_TEST);
            } else if (!enable && record.enabled) {
                gl.glDisable(GL10.GL_STENCIL_TEST);
            }
        } else {
            if (enable) {
                gl.glEnable(GL10.GL_STENCIL_TEST);
            } else {
                gl.glDisable(GL10.GL_STENCIL_TEST);
            }
        }

        AndroidStencilStateUtil.setTwoSidedEnabled(gl, enable ? twoSided : false, record, caps);
        record.enabled = enable;
    }

    private static void setTwoSidedEnabled(final GL10 gl, final boolean enable, final StencilStateRecord record,
            final ContextCapabilities caps) {
        if (caps.isTwoSidedStencilSupported()) {
            // XXX: Two sided stencil not supported in OES 1.0 or 1.1
            // if (record.isValid()) {
            // if (enable && !record.useTwoSided) {
            // gl.glEnable(GL10.GL_STENCIL_TEST_TWO_SIDE_EXT);
            // } else if (!enable && record.useTwoSided) {
            // gl.glDisable(GL10.GL_STENCIL_TEST_TWO_SIDE_EXT);
            // }
            // } else {
            // if (enable) {
            // gl.glEnable(GL10.GL_STENCIL_TEST_TWO_SIDE_EXT);
            // } else {
            // gl.glDisable(GL10.GL_STENCIL_TEST_TWO_SIDE_EXT);
            // }
            // }
        }
        record.useTwoSided = enable;
    }

    private static void applyMask(final GL10 gl, final int writeMask, final StencilStateRecord record, final int face) {
        // if (!record.isValid() || writeMask != record.writeMask[face]) {
        gl.glStencilMask(writeMask);
        // record.writeMask[face] = writeMask;
        // }
    }

    private static void applyFunc(final GL10 gl, final int glfunc, final int stencilRef, final int funcMask,
            final StencilStateRecord record, final int face) {
        // if (!record.isValid() || glfunc != record.func[face] || stencilRef != record.ref[face]
        // || funcMask != record.funcMask[face]) {
        gl.glStencilFunc(glfunc, stencilRef, funcMask);
        // record.func[face] = glfunc;
        // record.ref[face] = stencilRef;
        // record.funcMask[face] = funcMask;
        // }
    }

    private static void applyOp(final GL10 gl, final int fail, final int zfail, final int zpass,
            final StencilStateRecord record, final int face) {
        // if (!record.isValid() || fail != record.fail[face] || zfail != record.zfail[face]
        // || zpass != record.zpass[face]) {
        gl.glStencilOp(fail, zfail, zpass);
        // record.fail[face] = fail;
        // record.zfail[face] = zfail;
        // record.zpass[face] = zpass;
        // }
    }
}
