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

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.image.Texture.ApplyMode;
import com.ardor3d.image.Texture.CombinerFunctionAlpha;
import com.ardor3d.image.Texture.CombinerFunctionRGB;
import com.ardor3d.image.Texture.CombinerOperandAlpha;
import com.ardor3d.image.Texture.CombinerOperandRGB;
import com.ardor3d.image.Texture.CombinerSource;
import com.ardor3d.image.Texture.DepthTextureCompareFunc;
import com.ardor3d.image.Texture.DepthTextureCompareMode;
import com.ardor3d.image.Texture.DepthTextureMode;
import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.renderer.state.TextureState.CorrectionType;
import com.ardor3d.util.Ardor3dException;

public abstract class AndroidTextureUtil {

    public static int getGLPixelDataType(final PixelDataType type) {
        switch (type) {
            case UnsignedByte:
                return GL10.GL_UNSIGNED_BYTE;
                // TODO: Add support for ushort 4444, 5551 and 565
            case Float:
            case Short:
            case UnsignedShort:
            case Byte:
            case Int:
            case UnsignedInt:
            case HalfFloat:
            default:
                throw new Error("Unsupported type: " + type);
        }
    }

    public static int getGLPixelFormat(final ImageDataFormat format) {
        switch (format) {
            case RGBA:
                return GL10.GL_RGBA;
            case RGB:
                return GL10.GL_RGB;
            case Alpha:
                return GL10.GL_ALPHA;
            case Luminance:
                return GL10.GL_LUMINANCE;
            case LuminanceAlpha:
                return GL10.GL_LUMINANCE_ALPHA;
            case Depth:
            case BGR:
            case BGRA:
            case Red:
            case Blue:
            case Green:
            case ColorIndex:
            case StencilIndex:
            case Intensity:
                throw new Error("Unsupported format: " + format);
        }
        throw new IllegalArgumentException("Incorrect format set: " + format);
    }

    public static int getGLPixelFormatFromStoreFormat(final TextureStoreFormat format) {
        switch (format) {
            case RGBA2:
            case RGBA4:
            case RGBA8:
            case RGB5A1:
            case RGB10A2:
            case RGBA12:
            case RGBA16:
            case CompressedRGBA:
            case NativeDXT1A:
            case NativeDXT3:
            case NativeDXT5:
            case RGBA16F:
            case RGBA32F:
                return GL10.GL_RGBA;
            case R3G3B2:
            case RGB4:
            case RGB5:
            case RGB8:
            case RGB10:
            case RGB12:
            case RGB16:
            case CompressedRGB:
            case NativeDXT1:
            case RGB16F:
            case RGB32F:
                return GL10.GL_RGB;
            case Alpha4:
            case Alpha8:
            case Alpha12:
            case Alpha16:
            case Alpha16F:
            case Alpha32F:
                return GL10.GL_ALPHA;
            case Luminance4:
            case Luminance8:
            case Luminance12:
            case Luminance16:
            case Luminance16F:
            case Luminance32F:
            case CompressedLuminance:
            case NativeLATC_L:
                return GL10.GL_LUMINANCE;
            case Intensity4:
            case Intensity8:
            case Intensity12:
            case Intensity16:
            case Intensity16F:
            case Intensity32F:
                throw new Error("Unsupported format: " + format);
            case Luminance4Alpha4:
            case Luminance6Alpha2:
            case Luminance8Alpha8:
            case Luminance12Alpha4:
            case Luminance12Alpha12:
            case Luminance16Alpha16:
            case LuminanceAlpha16F:
            case LuminanceAlpha32F:
            case CompressedLuminanceAlpha:
            case NativeLATC_LA:
                return GL10.GL_LUMINANCE_ALPHA;
            case Depth:
            case Depth16:
            case Depth24:
            case Depth32:
            case Depth32F:
                throw new Error("Unsupported format: " + format);
        }
        throw new IllegalArgumentException("Incorrect format set: " + format);
    }

    public static int getGLDepthTextureMode(final DepthTextureMode mode) {
        switch (mode) {
            case Alpha:
                return GL10.GL_ALPHA;
            case Luminance:
                return GL10.GL_LUMINANCE;
            case Intensity:
            default:
                throw new Ardor3dException("Unsupported mode: " + mode);
        }
    }

    public static int getGLDepthTextureCompareMode(final DepthTextureCompareMode mode) {
        switch (mode) {
            case RtoTexture:
                throw new Ardor3dException("Unsupported mode: " + mode);
            case None:
            default:
                throw new Ardor3dException("Unsupported mode: " + mode);
        }
    }

    public static int getGLDepthTextureCompareFunc(final DepthTextureCompareFunc func) {
        switch (func) {
            case GreaterThanEqual:
                return GL10.GL_GEQUAL;
            case LessThanEqual:
            default:
                return GL10.GL_LEQUAL;
        }
    }

    public static int getGLMagFilter(final MagnificationFilter magFilter) {
        switch (magFilter) {
            case Bilinear:
                return GL10.GL_LINEAR;
            case NearestNeighbor:
            default:
                return GL10.GL_NEAREST;

        }
    }

    public static int getGLMinFilter(final MinificationFilter filter) {
        switch (filter) {
            case BilinearNoMipMaps:
                return GL10.GL_LINEAR;
            case Trilinear:
                return GL10.GL_LINEAR_MIPMAP_LINEAR;
            case BilinearNearestMipMap:
                return GL10.GL_LINEAR_MIPMAP_NEAREST;
            case NearestNeighborNoMipMaps:
                return GL10.GL_NEAREST;
            case NearestNeighborNearestMipMap:
                return GL10.GL_NEAREST_MIPMAP_NEAREST;
            case NearestNeighborLinearMipMap:
                return GL10.GL_NEAREST_MIPMAP_LINEAR;
        }
        throw new IllegalArgumentException("invalid MinificationFilter type: " + filter);
    }

    public static int getGLEnvMode(final ApplyMode apply) {
        switch (apply) {
            case Replace:
                return GL10.GL_REPLACE;
            case Blend:
                return GL10.GL_BLEND;
            case Combine:
                return GL11.GL_COMBINE;
            case Decal:
                return GL10.GL_DECAL;
            case Add:
                return GL10.GL_ADD;
            case Modulate:
                return GL10.GL_MODULATE;
        }
        throw new IllegalArgumentException("invalid ApplyMode type: " + apply);
    }

    public static int getPerspHint(final CorrectionType type) {
        switch (type) {
            case Perspective:
                return GL10.GL_NICEST;
            case Affine:
                return GL10.GL_FASTEST;
        }
        throw new IllegalArgumentException("unknown correction type: " + type);
    }

    public static int getGLCombineOpRGB(final CombinerOperandRGB operand) {
        switch (operand) {
            case SourceColor:
                return GL10.GL_SRC_COLOR;
            case OneMinusSourceColor:
                return GL10.GL_ONE_MINUS_SRC_COLOR;
            case SourceAlpha:
                return GL10.GL_SRC_ALPHA;
            case OneMinusSourceAlpha:
                return GL10.GL_ONE_MINUS_SRC_ALPHA;
        }
        throw new IllegalArgumentException("invalid CombinerOperandRGB type: " + operand);
    }

    public static int getGLCombineOpAlpha(final CombinerOperandAlpha operand) {
        switch (operand) {
            case SourceAlpha:
                return GL10.GL_SRC_ALPHA;
            case OneMinusSourceAlpha:
                return GL10.GL_ONE_MINUS_SRC_ALPHA;
        }
        throw new IllegalArgumentException("invalid CombinerOperandAlpha type: " + operand);
    }

    public static int getGLCombineSrc(final CombinerSource combineSrc) {
        switch (combineSrc) {
            case CurrentTexture:
                return GL10.GL_TEXTURE;
            case PrimaryColor:
                return GL11.GL_PRIMARY_COLOR;
            case Constant:
                return GL11.GL_CONSTANT;
            case Previous:
                return GL11.GL_PREVIOUS;
            case TextureUnit0:
                return GL10.GL_TEXTURE0;
            case TextureUnit1:
                return GL10.GL_TEXTURE1;
            case TextureUnit2:
                return GL10.GL_TEXTURE2;
            case TextureUnit3:
                return GL10.GL_TEXTURE3;
            case TextureUnit4:
                return GL10.GL_TEXTURE4;
            case TextureUnit5:
                return GL10.GL_TEXTURE5;
            case TextureUnit6:
                return GL10.GL_TEXTURE6;
            case TextureUnit7:
                return GL10.GL_TEXTURE7;
            case TextureUnit8:
                return GL10.GL_TEXTURE8;
            case TextureUnit9:
                return GL10.GL_TEXTURE9;
            case TextureUnit10:
                return GL10.GL_TEXTURE10;
            case TextureUnit11:
                return GL10.GL_TEXTURE11;
            case TextureUnit12:
                return GL10.GL_TEXTURE12;
            case TextureUnit13:
                return GL10.GL_TEXTURE13;
            case TextureUnit14:
                return GL10.GL_TEXTURE14;
            case TextureUnit15:
                return GL10.GL_TEXTURE15;
            case TextureUnit16:
                return GL10.GL_TEXTURE16;
            case TextureUnit17:
                return GL10.GL_TEXTURE17;
            case TextureUnit18:
                return GL10.GL_TEXTURE18;
            case TextureUnit19:
                return GL10.GL_TEXTURE19;
            case TextureUnit20:
                return GL10.GL_TEXTURE20;
            case TextureUnit21:
                return GL10.GL_TEXTURE21;
            case TextureUnit22:
                return GL10.GL_TEXTURE22;
            case TextureUnit23:
                return GL10.GL_TEXTURE23;
            case TextureUnit24:
                return GL10.GL_TEXTURE24;
            case TextureUnit25:
                return GL10.GL_TEXTURE25;
            case TextureUnit26:
                return GL10.GL_TEXTURE26;
            case TextureUnit27:
                return GL10.GL_TEXTURE27;
            case TextureUnit28:
                return GL10.GL_TEXTURE28;
            case TextureUnit29:
                return GL10.GL_TEXTURE29;
            case TextureUnit30:
                return GL10.GL_TEXTURE30;
            case TextureUnit31:
                return GL10.GL_TEXTURE31;
        }
        throw new IllegalArgumentException("invalid CombinerSource type: " + combineSrc);
    }

    public static int getGLCombineFuncAlpha(final CombinerFunctionAlpha combineFunc) {
        switch (combineFunc) {
            case Modulate:
                return GL10.GL_MODULATE;
            case Replace:
                return GL10.GL_REPLACE;
            case Add:
                return GL10.GL_ADD;
            case AddSigned:
                return GL11.GL_ADD_SIGNED;
            case Subtract:
                return GL11.GL_SUBTRACT;
            case Interpolate:
                return GL11.GL_INTERPOLATE;
        }
        throw new IllegalArgumentException("invalid CombinerFunctionAlpha type: " + combineFunc);
    }

    public static int getGLCombineFuncRGB(final CombinerFunctionRGB combineFunc) {
        switch (combineFunc) {
            case Modulate:
                return GL10.GL_MODULATE;
            case Replace:
                return GL10.GL_REPLACE;
            case Add:
                return GL10.GL_ADD;
            case AddSigned:
                return GL11.GL_ADD_SIGNED;
            case Subtract:
                return GL11.GL_SUBTRACT;
            case Interpolate:
                return GL11.GL_INTERPOLATE;
            case Dot3RGB:
                return GL11.GL_DOT3_RGB;
            case Dot3RGBA:
                return GL11.GL_DOT3_RGBA;
        }
        throw new IllegalArgumentException("invalid CombinerFunctionRGB type: " + combineFunc);
    }

    public static int bytesPerPixel(final int format, final int type) {
        int n, m;

        switch (format) {
            case GL10.GL_ALPHA:
            case GL10.GL_LUMINANCE:
                n = 1;
                break;
            case GL10.GL_LUMINANCE_ALPHA:
                n = 2;
                break;
            case GL10.GL_RGB:
                n = 3;
                break;
            case GL10.GL_RGBA:
                n = 4;
                break;
            // case GL10.GL_COLOR_INDEX:
            // case GL10.GL_STENCIL_INDEX:
            // case GL10.GL_DEPTH_COMPONENT:
            // case GL10.GL_RED:
            // case GL10.GL_GREEN:
            // case GL10.GL_BLUE:
            // case GL10.GL_BGR:
            // case GL12.GL_BGRA:
            default:
                n = 0;
        }

        switch (type) {
            case GL10.GL_UNSIGNED_BYTE:
            case GL10.GL_BYTE:
                m = 1;
                break;
            case GL10.GL_UNSIGNED_SHORT:
            case GL10.GL_SHORT:
                m = 2;
                break;
            case GL10.GL_FLOAT:
                m = 4;
                break;
            // case GL11.GL_BITMAP:
            // case GL11.GL_HALF_FLOAT:
            // case GL11.GL_UNSIGNED_INT:
            // case GL11.GL_INT:
            default:
                m = 0;
        }

        return n * m;
    }
}
