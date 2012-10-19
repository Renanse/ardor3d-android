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
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.BlendState.BlendEquation;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.BlendStateRecord;

public abstract class AndroidBlendStateUtil {

    public static void apply(final GL10 gl, final BlendState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final BlendStateRecord record = (BlendStateRecord) context.getStateRecord(StateType.Blend);
        final ContextCapabilities caps = context.getCapabilities();
        context.setCurrentState(StateType.Blend, state);

        if (state.isEnabled()) {
            AndroidBlendStateUtil.applyBlendEquations(gl, state.isBlendEnabled(), state, record, caps);
            AndroidBlendStateUtil.applyBlendColor(gl, state.isBlendEnabled(), state, record, caps);
            AndroidBlendStateUtil.applyBlendFunctions(gl, state.isBlendEnabled(), state, record, caps);

            AndroidBlendStateUtil.applyTest(gl, state.isTestEnabled(), state, record);
        } else {
            // disable blend
            AndroidBlendStateUtil.applyBlendEquations(gl, false, state, record, caps);

            // disable alpha test
            AndroidBlendStateUtil.applyTest(gl, false, state, record);
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void applyBlendEquations(final GL10 gl, final boolean enabled, final BlendState state,
            final BlendStateRecord record, final ContextCapabilities caps) {
        if (record.isValid()) {
            if (enabled) {
                if (!record.blendEnabled) {
                    gl.glEnable(GL10.GL_BLEND);
                    record.blendEnabled = true;
                }
                final int blendEqRGB = AndroidBlendStateUtil.getGLEquationValue(state.getBlendEquationRGB(), caps);
                if (caps.isSeparateBlendEquationsSupported()) {
                    final int blendEqAlpha = AndroidBlendStateUtil.getGLEquationValue(state.getBlendEquationAlpha(),
                            caps);
                    if (record.blendEqRGB != blendEqRGB || record.blendEqAlpha != blendEqAlpha) {
                        ((GL11ExtensionPack) gl).glBlendEquationSeparate(blendEqRGB, blendEqAlpha);
                        record.blendEqRGB = blendEqRGB;
                        record.blendEqAlpha = blendEqAlpha;
                    }
                } else if (caps.isBlendEquationSupported()) {
                    if (record.blendEqRGB != blendEqRGB) {
                        ((GL11ExtensionPack) gl).glBlendEquation(blendEqRGB);
                        record.blendEqRGB = blendEqRGB;
                    }
                }
            } else if (record.blendEnabled) {
                gl.glDisable(GL10.GL_BLEND);
                record.blendEnabled = false;
            }

        } else {
            if (enabled) {
                gl.glEnable(GL10.GL_BLEND);
                record.blendEnabled = true;
                final int blendEqRGB = AndroidBlendStateUtil.getGLEquationValue(state.getBlendEquationRGB(), caps);
                if (caps.isSeparateBlendEquationsSupported()) {
                    final int blendEqAlpha = AndroidBlendStateUtil.getGLEquationValue(state.getBlendEquationAlpha(),
                            caps);
                    ((GL11ExtensionPack) gl).glBlendEquationSeparate(blendEqRGB, blendEqAlpha);
                    record.blendEqRGB = blendEqRGB;
                    record.blendEqAlpha = blendEqAlpha;
                } else if (caps.isBlendEquationSupported()) {
                    ((GL11ExtensionPack) gl).glBlendEquation(blendEqRGB);
                    record.blendEqRGB = blendEqRGB;
                }
            } else {
                gl.glDisable(GL10.GL_BLEND);
                record.blendEnabled = false;
            }
        }
    }

    private static void applyBlendColor(final GL10 gl, final boolean enabled, final BlendState state,
            final BlendStateRecord record, final ContextCapabilities caps) {
    // XXX: Blend constant color does not appear to be supported is OES 1.X
    // if (enabled) {
    // final boolean applyConstant = state.getDestinationFunctionRGB().usesConstantColor()
    // || state.getSourceFunctionRGB().usesConstantColor()
    // || caps.isConstantBlendColorSupported()
    // && (state.getDestinationFunctionAlpha().usesConstantColor() || state.getSourceFunctionAlpha()
    // .usesConstantColor());
    // if (applyConstant && caps.isConstantBlendColorSupported()) {
    // final ReadOnlyColorRGBA constant = state.getConstantColor();
    // if (!record.isValid() || caps.isConstantBlendColorSupported() && !record.blendColor.equals(constant)) {
    // ((GL11ExtensionPack) gl).glBlendColor(constant.getRed(), constant.getGreen(), constant.getBlue(),
    // constant.getAlpha());
    // record.blendColor.set(constant);
    // }
    // }
    // }
    }

    private static void applyBlendFunctions(final GL10 gl, final boolean enabled, final BlendState state,
            final BlendStateRecord record, final ContextCapabilities caps) {
        if (record.isValid()) {
            if (enabled) {
                final int glSrcRGB = AndroidBlendStateUtil.getGLSrcValue(state.getSourceFunctionRGB(), caps);
                final int glDstRGB = AndroidBlendStateUtil.getGLDstValue(state.getDestinationFunctionRGB(), caps);
                if (caps.isSeparateBlendFunctionsSupported()) {
                    final int glSrcAlpha = AndroidBlendStateUtil.getGLSrcValue(state.getSourceFunctionAlpha(), caps);
                    final int glDstAlpha = AndroidBlendStateUtil.getGLDstValue(state.getDestinationFunctionAlpha(),
                            caps);
                    if (record.srcFactorRGB != glSrcRGB || record.dstFactorRGB != glDstRGB
                            || record.srcFactorAlpha != glSrcAlpha || record.dstFactorAlpha != glDstAlpha) {
                        ((GL11ExtensionPack) gl).glBlendFuncSeparate(glSrcRGB, glDstRGB, glSrcAlpha, glDstAlpha);
                        record.srcFactorRGB = glSrcRGB;
                        record.dstFactorRGB = glDstRGB;
                        record.srcFactorAlpha = glSrcAlpha;
                        record.dstFactorAlpha = glDstAlpha;
                    }
                } else if (record.srcFactorRGB != glSrcRGB || record.dstFactorRGB != glDstRGB) {
                    gl.glBlendFunc(glSrcRGB, glDstRGB);
                    record.srcFactorRGB = glSrcRGB;
                    record.dstFactorRGB = glDstRGB;
                }
            }
        } else {
            if (enabled) {
                final int glSrcRGB = AndroidBlendStateUtil.getGLSrcValue(state.getSourceFunctionRGB(), caps);
                final int glDstRGB = AndroidBlendStateUtil.getGLDstValue(state.getDestinationFunctionRGB(), caps);
                if (caps.isSeparateBlendFunctionsSupported()) {
                    final int glSrcAlpha = AndroidBlendStateUtil.getGLSrcValue(state.getSourceFunctionAlpha(), caps);
                    final int glDstAlpha = AndroidBlendStateUtil.getGLDstValue(state.getDestinationFunctionAlpha(),
                            caps);
                    ((GL11ExtensionPack) gl).glBlendFuncSeparate(glSrcRGB, glDstRGB, glSrcAlpha, glDstAlpha);
                    record.srcFactorRGB = glSrcRGB;
                    record.dstFactorRGB = glDstRGB;
                    record.srcFactorAlpha = glSrcAlpha;
                    record.dstFactorAlpha = glDstAlpha;
                } else {
                    gl.glBlendFunc(glSrcRGB, glDstRGB);
                    record.srcFactorRGB = glSrcRGB;
                    record.dstFactorRGB = glDstRGB;
                }
            }
        }
    }

    private static int getGLSrcValue(final SourceFunction function, final ContextCapabilities caps) {
        switch (function) {
            case Zero:
                return GL10.GL_ZERO;
            case DestinationColor:
                return GL10.GL_DST_COLOR;
            case OneMinusDestinationColor:
                return GL10.GL_ONE_MINUS_DST_COLOR;
            case SourceAlpha:
                return GL10.GL_SRC_ALPHA;
            case OneMinusSourceAlpha:
                return GL10.GL_ONE_MINUS_SRC_ALPHA;
            case DestinationAlpha:
                return GL10.GL_DST_ALPHA;
            case OneMinusDestinationAlpha:
                return GL10.GL_ONE_MINUS_DST_ALPHA;
            case SourceAlphaSaturate:
                return GL10.GL_SRC_ALPHA_SATURATE;
            case ConstantColor:
                // if (caps.isConstantBlendColorSupported()) {
                // return GL11ExtensionPack.GL_CONSTANT_COLOR;
                // }
                // FALLS THROUGH
            case OneMinusConstantColor:
                // if (caps.isConstantBlendColorSupported()) {
                // return GL11ExtensionPack.GL_ONE_MINUS_CONSTANT_COLOR;
                // }
                // FALLS THROUGH
            case ConstantAlpha:
                // if (caps.isConstantBlendColorSupported()) {
                // return GL11ExtensionPack.GL_CONSTANT_ALPHA;
                // }
                // FALLS THROUGH
            case OneMinusConstantAlpha:
                // if (caps.isConstantBlendColorSupported()) {
                // return GL11ExtensionPack.GL_ONE_MINUS_CONSTANT_ALPHA;
                // }
                // FALLS THROUGH
            case One:
                return GL10.GL_ONE;
        }
        throw new IllegalArgumentException("Invalid source function type: " + function);
    }

    private static int getGLDstValue(final DestinationFunction function, final ContextCapabilities caps) {
        switch (function) {
            case Zero:
                return GL10.GL_ZERO;
            case SourceColor:
                return GL10.GL_SRC_COLOR;
            case OneMinusSourceColor:
                return GL10.GL_ONE_MINUS_SRC_COLOR;
            case SourceAlpha:
                return GL10.GL_SRC_ALPHA;
            case OneMinusSourceAlpha:
                return GL10.GL_ONE_MINUS_SRC_ALPHA;
            case DestinationAlpha:
                return GL10.GL_DST_ALPHA;
            case OneMinusDestinationAlpha:
                return GL10.GL_ONE_MINUS_DST_ALPHA;
            case ConstantColor:
                // if (caps.isConstantBlendColorSupported()) {
                // return GL10.GL_CONSTANT_COLOR;
                // }
                // FALLS THROUGH
            case OneMinusConstantColor:
                // if (caps.isConstantBlendColorSupported()) {
                // return GL10.GL_ONE_MINUS_CONSTANT_COLOR;
                // }
                // FALLS THROUGH
            case ConstantAlpha:
                // if (caps.isConstantBlendColorSupported()) {
                // return GL10.GL_CONSTANT_ALPHA;
                // }
                // FALLS THROUGH
            case OneMinusConstantAlpha:
                // if (caps.isConstantBlendColorSupported()) {
                // return GL10.GL_ONE_MINUS_CONSTANT_ALPHA;
                // }
                // FALLS THROUGH
            case One:
                return GL10.GL_ONE;
        }
        throw new IllegalArgumentException("Invalid destination function type: " + function);
    }

    private static int getGLEquationValue(final BlendEquation eq, final ContextCapabilities caps) {
        switch (eq) {
            case Min:
                // XXX: OES extensions are available for Min/Max, but it appears Android/Khronos does not expose them.
                // if (caps.isMinMaxBlendEquationsSupported()) {
                // return GL10.GL_MIN;
                // }
                // FALLS THROUGH
            case Max:
                // if (caps.isMinMaxBlendEquationsSupported()) {
                // return GL10.GL_MAX;
                // } else {
                return GL11ExtensionPack.GL_FUNC_ADD;
                // }
            case Subtract:
                if (caps.isSubtractBlendEquationsSupported()) {
                    return GL11ExtensionPack.GL_FUNC_SUBTRACT;
                }
                // FALLS THROUGH
            case ReverseSubtract:
                if (caps.isSubtractBlendEquationsSupported()) {
                    return GL11ExtensionPack.GL_FUNC_REVERSE_SUBTRACT;
                }
                // FALLS THROUGH
            case Add:
                return GL11ExtensionPack.GL_FUNC_ADD;
        }
        throw new IllegalArgumentException("Invalid blend equation: " + eq);
    }

    private static void applyTest(final GL10 gl, final boolean enabled, final BlendState state,
            final BlendStateRecord record) {
        if (record.isValid()) {
            if (enabled) {
                if (!record.testEnabled) {
                    gl.glEnable(GL10.GL_ALPHA_TEST);
                    record.testEnabled = true;
                }
                final int glFunc = AndroidBlendStateUtil.getGLFuncValue(state.getTestFunction());
                if (record.alphaFunc != glFunc || record.alphaRef != state.getReference()) {
                    gl.glAlphaFunc(glFunc, state.getReference());
                    record.alphaFunc = glFunc;
                    record.alphaRef = state.getReference();
                }
            } else if (record.testEnabled) {
                gl.glDisable(GL10.GL_ALPHA_TEST);
                record.testEnabled = false;
            }

        } else {
            if (enabled) {
                gl.glEnable(GL10.GL_ALPHA_TEST);
                record.testEnabled = true;
                final int glFunc = AndroidBlendStateUtil.getGLFuncValue(state.getTestFunction());
                gl.glAlphaFunc(glFunc, state.getReference());
                record.alphaFunc = glFunc;
                record.alphaRef = state.getReference();
            } else {
                gl.glDisable(GL10.GL_ALPHA_TEST);
                record.testEnabled = false;
            }
        }
    }

    private static int getGLFuncValue(final BlendState.TestFunction function) {
        switch (function) {
            case Never:
                return GL10.GL_NEVER;
            case LessThan:
                return GL10.GL_LESS;
            case EqualTo:
                return GL10.GL_EQUAL;
            case LessThanOrEqualTo:
                return GL10.GL_LEQUAL;
            case GreaterThan:
                return GL10.GL_GREATER;
            case NotEqualTo:
                return GL10.GL_NOTEQUAL;
            case GreaterThanOrEqualTo:
                return GL10.GL_GEQUAL;
            case Always:
                return GL10.GL_ALWAYS;
        }
        throw new IllegalArgumentException("Invalid test function type: " + function);
    }
}
