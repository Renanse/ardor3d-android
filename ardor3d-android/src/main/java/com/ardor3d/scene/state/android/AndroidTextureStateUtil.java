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
import java.util.Collection;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11ExtensionPack;

import android.graphics.Bitmap;
import android.opengl.GLUtils;
import android.util.Log;

import com.ardor3d.extension.android.AndroidImage;
import com.ardor3d.framework.android.AndroidCanvas;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.ApplyMode;
import com.ardor3d.image.Texture.CombinerFunctionAlpha;
import com.ardor3d.image.Texture.CombinerFunctionRGB;
import com.ardor3d.image.Texture.CombinerOperandAlpha;
import com.ardor3d.image.Texture.CombinerOperandRGB;
import com.ardor3d.image.Texture.Type;
import com.ardor3d.image.Texture.WrapAxis;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.record.RendererRecord;
import com.ardor3d.renderer.state.record.TextureRecord;
import com.ardor3d.renderer.state.record.TextureStateRecord;
import com.ardor3d.renderer.state.record.TextureUnitRecord;
import com.ardor3d.scene.state.android.util.AndroidImageConverter;
import com.ardor3d.scene.state.android.util.AndroidRendererUtil;
import com.ardor3d.scene.state.android.util.AndroidTextureUtil;
import com.ardor3d.util.Constants;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;

public abstract class AndroidTextureStateUtil {

    private static FloatBuffer tmp_matrixBuffer = BufferUtils.createFloatBuffer(16);

    public final static void load(final GL10 gl, final Texture texture, final int unit) {
        if (texture == null) {
            return;
        }

        final RenderContext context = ContextManager.getCurrentContext();
        if (context == null) {
            Log.w(AndroidCanvas.TAG, "AndroidTextureStateUtil.load - RenderContext is null for texture: " + texture);
            return;
        }

        final ContextCapabilities caps = context.getCapabilities();
        final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(StateType.Texture);

        // Check we are in the right unit
        if (record != null) {
            AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
        }

        // Create the texture...
        // First, look for a texture in the cache just like ours
        final Texture cached = TextureManager.findCachedTexture(texture.getTextureKey());

        if (cached == null) {
            TextureManager.addToCache(texture);
        } else {
            final int textureId = cached.getTextureIdForContext(context.getGlContextRep());
            if (textureId != 0) {
                AndroidTextureStateUtil.doTextureBind(gl, cached, unit, false);
                return;
            }
        }

        final int[] buffer = new int[1];
        gl.glGenTextures(1, buffer, 0);
        final int textureId = buffer[0];

        // store the new id by our current gl context.
        texture.setTextureIdForContext(context.getGlContextRep(), textureId);

        AndroidTextureStateUtil.update(gl, texture, unit);
    }

    /**
     * bind texture and upload image data to card
     */
    public static void update(final GL10 gl, final Texture texture, final int unit) {
        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();

        texture.getTextureKey().setClean(context.getGlContextRep());

        // our texture type:
        final Texture.Type type = texture.getType();

        // check for null
        if (texture.getImage() == null) {
            Log.w(AndroidCanvas.TAG, "AndroidTextureState.update - Image data for texture is null.");
            return;
        }

        // Pull android image.
        final AndroidImage image;

        // check that we are an AndroidImage with Bitmap data.
        if (!(texture.getImage() instanceof AndroidImage)) {
            try {
                image = AndroidImageConverter.convert(texture.getImage());
                if (image == null) {
                    Log.w(AndroidCanvas.TAG, "AndroidTextureState.update - Unable to process image into texture.");
                    return;
                }
            } catch (final Exception e) {
                Log.e(AndroidCanvas.TAG, "AndroidTextureState.update - Unable to process image into texture.", e);
                return;
            }
        } else {
            image = (AndroidImage) texture.getImage();
        }

        // borders do not seem to be supported...
        if (texture.hasBorder()) {
            Log.w(AndroidCanvas.TAG, "AndroidTextureState.update - texture borders are not supported..  ignored.");
        }

        // bind our texture id to this unit.
        AndroidTextureStateUtil.doTextureBind(gl, texture, unit, false);

        // set alignment to support images with width % 4 != 0, as images are
        // not aligned
        gl.glPixelStorei(GL10.GL_UNPACK_ALIGNMENT, 1);

        // Get texture image data. Not all textures have image data.
        // For example, ApplyMode.Combine modes can use primary colors,
        // texture output, and constants to modify fragments via the
        // texture units.
        if (image != null) {

            final int maxSize = caps.getMaxTextureSize();
            final int actualWidth = image.getWidth();
            final int actualHeight = image.getHeight();

            // See if we need to rescale our texture
            final boolean needsPowerOfTwo = !caps.isNonPowerOfTwoTextureSupported()
                    && (!MathUtils.isPowerOfTwo(image.getWidth()) || !MathUtils.isPowerOfTwo(image.getHeight()));
            if (actualWidth > maxSize || actualHeight > maxSize || needsPowerOfTwo) {
                if (needsPowerOfTwo) {
                    Log.w(AndroidCanvas.TAG,
                            "AndroidTextureState.update - (card unsupported) Attempted to apply texture with size that is not power of 2: "
                                    + image.getWidth() + " x " + image.getHeight());
                }
                if (actualWidth > maxSize || actualHeight > maxSize) {
                    Log.w(AndroidCanvas.TAG,
                            "AndroidTextureState.update - (card unsupported) Attempted to apply texture with size bigger than max texture size ["
                                    + maxSize + "]: " + image.getWidth() + " x " + image.getHeight());
                }

                int w = actualWidth;
                if (needsPowerOfTwo) {
                    w = MathUtils.nearestPowerOfTwo(actualWidth);
                }
                if (w > maxSize) {
                    w = maxSize;
                }

                int h = actualHeight;
                if (needsPowerOfTwo) {
                    h = MathUtils.nearestPowerOfTwo(actualHeight);
                }
                if (h > maxSize) {
                    h = maxSize;
                }
                Log.w(AndroidCanvas.TAG, "AndroidTextureState.update - Rescaling image to " + w + " x " + h + " !!!");

                for (int i = 0; i < image.getBitmaps().size(); i++) {
                    final Bitmap bm = image.getBitmap(i);
                    image.getBitmaps().set(i, Bitmap.createScaledBitmap(bm, w, h, true));
                    bm.recycle();
                }

                image.setWidth(w);
                image.setHeight(h);
            }

            if (!texture.getMinificationFilter().usesMipMapLevels() && !texture.getTextureStoreFormat().isCompressed()) {
                // Load textures which do not need mipmap auto-generating and
                // which aren't using compressed images.

                switch (texture.getType()) {
                    case TwoDimensional:
                        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, image.getBitmap(0), 0);
                        break;
                    case CubeMap:
                        if (caps.isTextureCubeMapSupported()) {
                            for (final TextureCubeMap.Face face : TextureCubeMap.Face.values()) {
                                // send top level to card
                                GLUtils.texImage2D(AndroidTextureStateUtil.getGLCubeMapFace(face), 0,
                                        image.getBitmap(face.ordinal()), 0);
                            }
                        } else {
                            Log.w(AndroidCanvas.TAG, "AndroidTextureState.load - This card does not support Cubemaps.");
                        }
                        break;
                    default:
                        Log.w(AndroidCanvas.TAG, "AndroidTextureState.update - " + texture.getType()
                                + " not supported by this renderer.");
                        break;
                }
            } else if (texture.getMinificationFilter().usesMipMapLevels() && !image.hasMipmaps()
                    && !texture.getTextureStoreFormat().isCompressed()) {

                // For textures which need mipmaps auto-generating and which aren't using compressed images, generate
                // the mipmaps. A new mipmap builder may be needed to build mipmaps for compressed textures.

                if (caps.isAutomaticMipmapsSupported()) {
                    // Flag the card to generate mipmaps
                    gl.glTexParameterx(AndroidTextureStateUtil.getGLType(type), GL11.GL_GENERATE_MIPMAP, GL10.GL_TRUE);
                }

                switch (type) {
                    case TwoDimensional:
                        if (caps.isAutomaticMipmapsSupported()) {
                            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, image.getBitmap(0), 0);
                        } else {
                            AndroidTextureStateUtil.buildMipmaps(gl, GL10.GL_TEXTURE_2D, image.getBitmap(0));
                        }
                        break;
                    case CubeMap:
                        // NOTE: Cubemaps MUST be square, so height is ignored
                        // on purpose.
                        if (caps.isTextureCubeMapSupported()) {
                            for (final TextureCubeMap.Face face : TextureCubeMap.Face.values()) {
                                if (caps.isAutomaticMipmapsSupported()) {
                                    GLUtils.texImage2D(AndroidTextureStateUtil.getGLCubeMapFace(face), 0,
                                            image.getBitmap(face.ordinal()), 0);
                                } else {
                                    AndroidTextureStateUtil.buildMipmaps(gl,
                                            AndroidTextureStateUtil.getGLCubeMapFace(face),
                                            image.getBitmap(face.ordinal()));
                                }
                            }
                        } else {
                            Log.w(AndroidCanvas.TAG,
                                    "AndroidTextureState.update - This card does not support Cubemaps.");
                            return;
                        }
                        break;
                    default:
                        Log.w(AndroidCanvas.TAG, "AndroidTextureState.update - " + texture.getType()
                                + " not supported by this renderer.");
                        break;
                }

            } else {
                // Here we would handle textures that are either compressed or have predefined mipmaps.
                Log.w(AndroidCanvas.TAG, "AndroidTextureState.update - mipmap/compressed textures not yet supported");
                return;
            }
        }
    }

    public static void apply(final GL10 gl, final TextureState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();
        final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(StateType.Texture);
        context.setCurrentState(StateType.Texture, state);

        if (state.isEnabled()) {

            Texture texture;
            Texture.Type type;
            TextureUnitRecord unitRecord;
            TextureRecord texRecord;

            final int glHint = AndroidTextureUtil.getPerspHint(state.getCorrectionType());
            if (!record.isValid() || record.hint != glHint) {
                // set up correction mode
                gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, glHint);
                record.hint = glHint;
            }

            // loop through all available texture units...
            for (int i = 0; i < caps.getNumberOfTotalTextureUnits(); i++) {
                unitRecord = record.units[i];

                // grab a texture for this unit, if available
                texture = state.getTexture(i);

                // pull our texture id for this texture, for this context.
                int textureId = texture != null ? texture.getTextureIdForContext(context.getGlContextRep()) : -1;

                // check for invalid textures - ones that have no opengl id and
                // no image data
                if (texture != null && textureId == 0 && texture.getImage() == null) {
                    texture = null;
                }

                // null textures above fixed limit do not need to be disabled
                // since they are not really part of the pipeline.
                if (texture == null) {
                    if (i >= caps.getNumberOfFixedTextureUnits()) {
                        continue;
                    } else {
                        // a null texture indicates no texturing at this unit
                        // Disable texturing on this unit if enabled.
                        AndroidTextureStateUtil.disableTexturing(gl, unitRecord, record, i, caps);

                        if (i < state._keyCache.length) {
                            state._keyCache[i] = null;
                        }

                        // next texture!
                        continue;
                    }
                }

                type = texture.getType();

                // disable other texturing types for this unit, if enabled.
                AndroidTextureStateUtil.disableTexturing(gl, unitRecord, record, i, type, caps);

                // Time to bind the texture, so see if we need to load in image
                // data for this texture.
                if (textureId == 0) {
                    // texture not yet loaded.
                    // this will load and bind and set the records...
                    AndroidTextureStateUtil.load(gl, texture, i);
                    textureId = texture.getTextureIdForContext(context.getGlContextRep());
                    if (textureId == 0) {
                        continue;
                    }
                } else if (texture.isDirty(context.getGlContextRep())) {
                    AndroidTextureStateUtil.update(gl, texture, i);
                    textureId = texture.getTextureIdForContext(context.getGlContextRep());
                    if (textureId == 0) {
                        continue;
                    }
                } else {
                    // texture already exists in OpenGL, just bind it if needed
                    if (!unitRecord.isValid() || unitRecord.boundTexture != textureId) {
                        AndroidTextureStateUtil.checkAndSetUnit(gl, i, record, caps);
                        gl.glBindTexture(AndroidTextureStateUtil.getGLType(type), textureId);
                        if (Constants.stats) {
                            StatCollector.addStat(StatType.STAT_TEXTURE_BINDS, 1);
                        }
                        unitRecord.boundTexture = textureId;
                    }
                }

                // Grab our record for this texture
                texRecord = record.getTextureRecord(textureId, texture.getType());

                // Set the keyCache value for this unit of this texture state
                // This is done so during state comparison we don't have to
                // spend a lot of time pulling out classes and finding field
                // data.
                state._keyCache[i] = texture.getTextureKey();

                // Some texture things only apply to fixed function pipeline
                if (i < caps.getNumberOfFixedTextureUnits()) {

                    // Enable 2D texturing on this unit if not enabled.
                    if (!unitRecord.isValid() || !unitRecord.enabled[type.ordinal()]) {
                        AndroidTextureStateUtil.checkAndSetUnit(gl, i, record, caps);
                        gl.glEnable(AndroidTextureStateUtil.getGLType(type));
                        unitRecord.enabled[type.ordinal()] = true;
                    }

                    // Set our blend color, if needed.
                    AndroidTextureStateUtil.applyBlendColor(gl, texture, unitRecord, i, record, caps);

                    // Set the texture environment mode if this unit isn't
                    // already set properly
                    AndroidTextureStateUtil.applyEnvMode(gl, texture.getApply(), unitRecord, i, record, caps);

                    // If our mode is combine, and we support multitexturing
                    // apply combine settings.
                    if (texture.getApply() == ApplyMode.Combine && caps.isMultitextureSupported()
                            && caps.isEnvCombineSupported()) {
                        AndroidTextureStateUtil.applyCombineFactors(gl, texture, unitRecord, i, record, caps);
                    }
                }

                // Other items only apply to textures below the frag unit limit
                if (i < caps.getNumberOfFragmentTextureUnits()) {

                    // texture specific params
                    AndroidTextureStateUtil.applyFilter(gl, texture, texRecord, i, record, caps);
                    AndroidTextureStateUtil.applyWrap(gl, texture, texRecord, i, record, caps);

                    // all states have now been applied for a tex record, so we
                    // can safely make it valid
                    if (!texRecord.isValid()) {
                        texRecord.validate();
                    }

                }

                // Other items only apply to textures below the frag tex coord
                // unit limit
                if (i < caps.getNumberOfFragmentTexCoordUnits()) {

                    // Now time to play with texture matrices
                    // Determine which transforms to do.
                    AndroidTextureStateUtil.applyTextureTransforms(gl, texture, i, record, caps);
                }

            }

        } else {
            // turn off texturing
            TextureUnitRecord unitRecord;

            if (caps.isMultitextureSupported()) {
                for (int i = 0; i < caps.getNumberOfFixedTextureUnits(); i++) {
                    unitRecord = record.units[i];
                    AndroidTextureStateUtil.disableTexturing(gl, unitRecord, record, i, caps);
                }
            } else {
                unitRecord = record.units[0];
                AndroidTextureStateUtil.disableTexturing(gl, unitRecord, record, 0, caps);
            }
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void disableTexturing(final GL10 gl, final TextureUnitRecord unitRecord,
            final TextureStateRecord record, final int unit, final Type exceptedType, final ContextCapabilities caps) {
        if (exceptedType != Type.TwoDimensional) {
            if (!unitRecord.isValid() || unitRecord.enabled[Type.TwoDimensional.ordinal()]) {
                // Check we are in the right unit
                AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
                gl.glDisable(GL10.GL_TEXTURE_2D);
                unitRecord.enabled[Type.TwoDimensional.ordinal()] = false;
            }
        }

        // XXX: 1D unsupported.
        // XXX: 3D only supported by extensions we don't currently have access to.

        if (caps.isTextureCubeMapSupported() && exceptedType != Type.CubeMap) {
            if (!unitRecord.isValid() || unitRecord.enabled[Type.CubeMap.ordinal()]) {
                // Check we are in the right unit
                AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
                gl.glDisable(GL11ExtensionPack.GL_TEXTURE_CUBE_MAP);
                unitRecord.enabled[Type.CubeMap.ordinal()] = false;
            }
        }

    }

    private static void disableTexturing(final GL10 gl, final TextureUnitRecord unitRecord,
            final TextureStateRecord record, final int unit, final ContextCapabilities caps) {
        if (!unitRecord.isValid() || unitRecord.enabled[Type.TwoDimensional.ordinal()]) {
            // Check we are in the right unit
            AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
            gl.glDisable(GL10.GL_TEXTURE_2D);
            unitRecord.enabled[Type.TwoDimensional.ordinal()] = false;
        }

        // XXX: 1D unsupported.
        // XXX: 3D only supported by extensions we don't currently have access to.

        if (caps.isTextureCubeMapSupported()) {
            if (!unitRecord.isValid() || unitRecord.enabled[Type.CubeMap.ordinal()]) {
                // Check we are in the right unit
                AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
                gl.glDisable(GL11ExtensionPack.GL_TEXTURE_CUBE_MAP);
                unitRecord.enabled[Type.CubeMap.ordinal()] = false;
            }
        }

    }

    public static void applyCombineFactors(final GL10 gl, final Texture texture, final TextureUnitRecord unitRecord,
            final int unit, final TextureStateRecord record, final ContextCapabilities caps) {

        // XXX: NOTE... in OES 1.X, texture combine sources for RGB and Alpha appear to be set perm. as TEXTURE,
        // PREVIOUS and CONSTANT (in that order)

        // check that this is a valid fixed function unit. glTexEnv is only
        // supported for unit < GL_MAX_TEXTURE_UNITS
        if (unit >= caps.getNumberOfFixedTextureUnits()) {
            return;
        }

        // first thing's first... if we are doing dot3 and don't
        // support it, disable this texture.
        boolean checked = false;
        if (!caps.isEnvDot3TextureCombineSupported()
                && (texture.getCombineFuncRGB() == CombinerFunctionRGB.Dot3RGB || texture.getCombineFuncRGB() == CombinerFunctionRGB.Dot3RGBA)) {

            // disable
            AndroidTextureStateUtil.disableTexturing(gl, unitRecord, record, unit, caps);

            // No need to continue
            return;
        }

        // Okay, now let's set our scales if we need to:
        // First RGB Combine scale
        if (!unitRecord.isValid() || unitRecord.envRGBScale != texture.getCombineScaleRGB()) {
            if (!checked) {
                AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
                checked = true;
            }
            gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL11.GL_RGB_SCALE, texture.getCombineScaleRGB().floatValue());
            unitRecord.envRGBScale = texture.getCombineScaleRGB();
        } // Then Alpha Combine scale
        if (!unitRecord.isValid() || unitRecord.envAlphaScale != texture.getCombineScaleAlpha()) {
            if (!checked) {
                AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
                checked = true;
            }
            gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL11.GL_ALPHA_SCALE, texture.getCombineScaleAlpha().floatValue());
            unitRecord.envAlphaScale = texture.getCombineScaleAlpha();
        }

        // Time to set the RGB combines
        final CombinerFunctionRGB rgbCombineFunc = texture.getCombineFuncRGB();
        if (!unitRecord.isValid() || unitRecord.rgbCombineFunc != rgbCombineFunc) {
            if (!checked) {
                AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
                checked = true;
            }
            gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_COMBINE_RGB,
                    AndroidTextureUtil.getGLCombineFuncRGB(rgbCombineFunc));
            unitRecord.rgbCombineFunc = rgbCombineFunc;
        }

        // CombinerSource combSrcRGB = texture.getCombineSrc0RGB();
        // if (!unitRecord.isValid() || unitRecord.combSrcRGB0 != combSrcRGB) {
        // if (!checked) {
        // AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
        // checked = true;
        // }
        // gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_SOURCE0_RGB, AndroidTextureUtil.getGLCombineSrc(combSrcRGB));
        // unitRecord.combSrcRGB0 = combSrcRGB;
        // }

        CombinerOperandRGB combOpRGB = texture.getCombineOp0RGB();
        if (!unitRecord.isValid() || unitRecord.combOpRGB0 != combOpRGB) {
            if (!checked) {
                AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
                checked = true;
            }
            gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_OPERAND0_RGB, AndroidTextureUtil.getGLCombineOpRGB(combOpRGB));
            unitRecord.combOpRGB0 = combOpRGB;
        }

        // We only need to do Arg1 or Arg2 if we aren't in Replace mode
        if (rgbCombineFunc != CombinerFunctionRGB.Replace) {

            // combSrcRGB = texture.getCombineSrc1RGB();
            // if (!unitRecord.isValid() || unitRecord.combSrcRGB1 != combSrcRGB) {
            // if (!checked) {
            // AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
            // checked = true;
            // }
            // gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_SOURCE1_RGB, AndroidTextureUtil.getGLCombineSrc(combSrcRGB));
            // unitRecord.combSrcRGB1 = combSrcRGB;
            // }

            combOpRGB = texture.getCombineOp1RGB();
            if (!unitRecord.isValid() || unitRecord.combOpRGB1 != combOpRGB) {
                if (!checked) {
                    AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
                    checked = true;
                }
                gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_OPERAND1_RGB, AndroidTextureUtil.getGLCombineOpRGB(combOpRGB));
                unitRecord.combOpRGB1 = combOpRGB;
            }

            // We only need to do Arg2 if we are in Interpolate mode
            if (rgbCombineFunc == CombinerFunctionRGB.Interpolate) {

                // combSrcRGB = texture.getCombineSrc2RGB();
                // if (!unitRecord.isValid() || unitRecord.combSrcRGB2 != combSrcRGB) {
                // if (!checked) {
                // AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
                // checked = true;
                // }
                // gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_SOURCE2_RGB, AndroidTextureUtil
                // .getGLCombineSrc(combSrcRGB));
                // unitRecord.combSrcRGB2 = combSrcRGB;
                // }

                combOpRGB = texture.getCombineOp2RGB();
                if (!unitRecord.isValid() || unitRecord.combOpRGB2 != combOpRGB) {
                    if (!checked) {
                        AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
                        checked = true;
                    }
                    gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_OPERAND2_RGB,
                            AndroidTextureUtil.getGLCombineOpRGB(combOpRGB));
                    unitRecord.combOpRGB2 = combOpRGB;
                }

            }
        }

        // Now Alpha combines
        final CombinerFunctionAlpha alphaCombineFunc = texture.getCombineFuncAlpha();
        if (!unitRecord.isValid() || unitRecord.alphaCombineFunc != alphaCombineFunc) {
            if (!checked) {
                AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
                checked = true;
            }
            gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_COMBINE_ALPHA,
                    AndroidTextureUtil.getGLCombineFuncAlpha(alphaCombineFunc));
            unitRecord.alphaCombineFunc = alphaCombineFunc;
        }

        // CombinerSource combSrcAlpha = texture.getCombineSrc0Alpha();
        // if (!unitRecord.isValid() || unitRecord.combSrcAlpha0 != combSrcAlpha) {
        // if (!checked) {
        // AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
        // checked = true;
        // }
        // gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_SOURCE0_ALPHA, AndroidTextureUtil.getGLCombineSrc(combSrcAlpha));
        // unitRecord.combSrcAlpha0 = combSrcAlpha;
        // }

        CombinerOperandAlpha combOpAlpha = texture.getCombineOp0Alpha();
        if (!unitRecord.isValid() || unitRecord.combOpAlpha0 != combOpAlpha) {
            if (!checked) {
                AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
                checked = true;
            }
            gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_OPERAND0_ALPHA,
                    AndroidTextureUtil.getGLCombineOpAlpha(combOpAlpha));
            unitRecord.combOpAlpha0 = combOpAlpha;
        }

        // We only need to do Arg1 or Arg2 if we aren't in Replace mode
        if (alphaCombineFunc != CombinerFunctionAlpha.Replace) {

            // combSrcAlpha = texture.getCombineSrc1Alpha();
            // if (!unitRecord.isValid() || unitRecord.combSrcAlpha1 != combSrcAlpha) {
            // if (!checked) {
            // AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
            // checked = true;
            // }
            // gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_SOURCE1_ALPHA, AndroidTextureUtil
            // .getGLCombineSrc(combSrcAlpha));
            // unitRecord.combSrcAlpha1 = combSrcAlpha;
            // }

            combOpAlpha = texture.getCombineOp1Alpha();
            if (!unitRecord.isValid() || unitRecord.combOpAlpha1 != combOpAlpha) {
                if (!checked) {
                    AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
                    checked = true;
                }
                gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_OPERAND1_ALPHA,
                        AndroidTextureUtil.getGLCombineOpAlpha(combOpAlpha));
                unitRecord.combOpAlpha1 = combOpAlpha;
            }

            // We only need to do Arg2 if we are in Interpolate mode
            if (alphaCombineFunc == CombinerFunctionAlpha.Interpolate) {

                // combSrcAlpha = texture.getCombineSrc2Alpha();
                // if (!unitRecord.isValid() || unitRecord.combSrcAlpha2 != combSrcAlpha) {
                // if (!checked) {
                // AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
                // checked = true;
                // }
                // gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_SOURCE2_ALPHA, AndroidTextureUtil
                // .getGLCombineSrc(combSrcAlpha));
                // unitRecord.combSrcAlpha2 = combSrcAlpha;
                // }

                combOpAlpha = texture.getCombineOp2Alpha();
                if (!unitRecord.isValid() || unitRecord.combOpAlpha2 != combOpAlpha) {
                    if (!checked) {
                        AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
                        checked = true;
                    }
                    gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_OPERAND2_ALPHA,
                            AndroidTextureUtil.getGLCombineOpAlpha(combOpAlpha));
                    unitRecord.combOpAlpha2 = combOpAlpha;
                }
            }
        }
    }

    public static void applyEnvMode(final GL10 gl, final ApplyMode mode, final TextureUnitRecord unitRecord,
            final int unit, final TextureStateRecord record, final ContextCapabilities caps) {
        if (!unitRecord.isValid() || unitRecord.envMode != mode) {
            AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
            gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, AndroidTextureUtil.getGLEnvMode(mode));
            unitRecord.envMode = mode;
        }
    }

    public static void applyBlendColor(final GL10 gl, final Texture texture, final TextureUnitRecord unitRecord,
            final int unit, final TextureStateRecord record, final ContextCapabilities caps) {
        final ReadOnlyColorRGBA texBlend = texture.getConstantColor();
        if (!unitRecord.isValid() || !unitRecord.blendColor.equals(texBlend)) {
            AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
            TextureRecord.colorBuffer.clear();
            TextureRecord.colorBuffer.put(texBlend.getRed()).put(texBlend.getGreen()).put(texBlend.getBlue())
                    .put(texBlend.getAlpha());
            TextureRecord.colorBuffer.rewind();
            gl.glTexEnvfv(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_COLOR, TextureRecord.colorBuffer);
            unitRecord.blendColor.set(texBlend);
        }
    }

    public static void applyTextureTransforms(final GL10 gl, final Texture texture, final int unit,
            final TextureStateRecord record, final ContextCapabilities caps) {
        final boolean needsReset = !record.units[unit].identityMatrix;

        // Should we apply the transform?
        final boolean doTrans = !texture.getTextureMatrix().isIdentity();

        // Now do them.
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        if (doTrans) {
            AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
            AndroidRendererUtil.switchMode(gl, matRecord, GL10.GL_TEXTURE);

            AndroidTextureStateUtil.tmp_matrixBuffer.rewind();
            texture.getTextureMatrix().toFloatBuffer(AndroidTextureStateUtil.tmp_matrixBuffer, true);
            AndroidTextureStateUtil.tmp_matrixBuffer.rewind();
            gl.glLoadMatrixf(AndroidTextureStateUtil.tmp_matrixBuffer);

            record.units[unit].identityMatrix = false;
        } else if (needsReset) {
            AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
            AndroidRendererUtil.switchMode(gl, matRecord, GL10.GL_TEXTURE);
            gl.glLoadIdentity();
            record.units[unit].identityMatrix = true;
        }
        // Switch back to the modelview matrix for further operations
        AndroidRendererUtil.switchMode(gl, matRecord, GL10.GL_MODELVIEW);
    }

    // If we support multitexturing, specify the unit we are affecting.
    public static void checkAndSetUnit(final GL10 gl, final int unit, final TextureStateRecord record,
            final ContextCapabilities caps) {
        // No need to worry about valid record, since invalidate sets record's
        // currentUnit to -1.
        if (record.currentUnit != unit) {
            if (unit >= caps.getNumberOfTotalTextureUnits() || !caps.isMultitextureSupported() || unit < 0) {
                // ignore this request as it is not valid for the user's hardware.
                return;
            }
            gl.glActiveTexture(GL10.GL_TEXTURE0 + unit);
            record.currentUnit = unit;
        }
    }

    /**
     * Check if the filter settings of this particular texture have been changed and apply as needed.
     * 
     * @param texture
     *            our texture object
     * @param texRecord
     *            our record of the last state of the texture in gl
     * @param record
     */
    public static void applyFilter(final GL10 gl, final Texture texture, final TextureRecord texRecord, final int unit,
            final TextureStateRecord record, final ContextCapabilities caps) {
        final Type type = texture.getType();

        final int magFilter = AndroidTextureUtil.getGLMagFilter(texture.getMagnificationFilter());
        // set up magnification filter
        if (!texRecord.isValid() || texRecord.magFilter != magFilter) {
            AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
            gl.glTexParameterx(AndroidTextureStateUtil.getGLType(type), GL10.GL_TEXTURE_MAG_FILTER, magFilter);
            texRecord.magFilter = magFilter;
        }

        final int minFilter = AndroidTextureUtil.getGLMinFilter(texture.getMinificationFilter());
        // set up mipmap filter
        if (!texRecord.isValid() || texRecord.minFilter != minFilter) {
            AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
            gl.glTexParameterx(AndroidTextureStateUtil.getGLType(type), GL10.GL_TEXTURE_MIN_FILTER, minFilter);
            texRecord.minFilter = minFilter;
        }

        // XXX: Aniso extension is not exposed by Android. :(
    }

    /**
     * Check if the wrap mode of this particular texture has been changed and apply as needed.
     * 
     * @param texture
     *            our texture object
     * @param texRecord
     *            our record of the last state of the unit in gl
     * @param record
     */
    public static void applyWrap(final GL10 gl, final Texture texture, final TextureRecord texRecord, final int unit,
            final TextureStateRecord record, final ContextCapabilities caps) {
        if (texture instanceof Texture2D) {
            AndroidTextureStateUtil.applyWrap(gl, (Texture2D) texture, texRecord, unit, record, caps);
        } else if (texture instanceof TextureCubeMap) {
            AndroidTextureStateUtil.applyWrap(gl, (TextureCubeMap) texture, texRecord, unit, record, caps);
        }
    }

    /**
     * Check if the wrap mode of this particular texture has been changed and apply as needed.
     * 
     * @param texture
     *            our texture object
     * @param texRecord
     *            our record of the last state of the unit in gl
     * @param record
     */
    public static void applyWrap(final GL10 gl, final Texture2D texture, final TextureRecord texRecord, final int unit,
            final TextureStateRecord record, final ContextCapabilities caps) {
        final int wrapS = AndroidTextureStateUtil.getGLWrap(texture.getWrap(WrapAxis.S), caps);
        final int wrapT = AndroidTextureStateUtil.getGLWrap(texture.getWrap(WrapAxis.T), caps);

        if (!texRecord.isValid() || texRecord.wrapS != wrapS) {
            AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
            gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, wrapS);
            texRecord.wrapS = wrapS;
        }
        if (!texRecord.isValid() || texRecord.wrapT != wrapT) {
            AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
            gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, wrapT);
            texRecord.wrapT = wrapT;
        }

    }

    /**
     * Check if the wrap mode of this particular texture has been changed and apply as needed.
     * 
     * @param cubeMap
     *            our texture object
     * @param texRecord
     *            our record of the last state of the unit in gl
     * @param record
     */
    public static void applyWrap(final GL10 gl, final TextureCubeMap cubeMap, final TextureRecord texRecord,
            final int unit, final TextureStateRecord record, final ContextCapabilities caps) {
        if (!caps.isTextureCubeMapSupported()) {
            return;
        }

        final int wrapS = AndroidTextureStateUtil.getGLWrap(cubeMap.getWrap(WrapAxis.S), caps);
        final int wrapT = AndroidTextureStateUtil.getGLWrap(cubeMap.getWrap(WrapAxis.T), caps);
        // final int wrapR = AndroidTextureStateUtil.getGLWrap(cubeMap.getWrap(WrapAxis.R), caps);

        if (!texRecord.isValid() || texRecord.wrapS != wrapS) {
            AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
            gl.glTexParameterx(GL11ExtensionPack.GL_TEXTURE_CUBE_MAP, GL10.GL_TEXTURE_WRAP_S, wrapS);
            texRecord.wrapS = wrapS;
        }
        if (!texRecord.isValid() || texRecord.wrapT != wrapT) {
            AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
            gl.glTexParameterx(GL11ExtensionPack.GL_TEXTURE_CUBE_MAP, GL10.GL_TEXTURE_WRAP_T, wrapT);
            texRecord.wrapT = wrapT;
        }
        // FIXME: Is wrap even supported for cubemaps? WRAP_R is not given.
        // if (!texRecord.isValid() || texRecord.wrapR != wrapR) {
        // AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);
        // gl.glTexParameterx(GL11ExtensionPack.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_R, wrapR);
        // texRecord.wrapR = wrapR;
        // }
    }

    public static void deleteTexture(final GL10 gl, final Texture texture) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(StateType.Texture);

        final int id = texture.getTextureIdForContext(context.getGlContextRep());
        if (id == 0) {
            // Not on card... return.
            return;
        }

        final int[] buffer = new int[] { id };
        gl.glDeleteTextures(1, buffer, 0);
        record.removeTextureRecord(id);
        texture.removeFromIdCache(context.getGlContextRep());
    }

    public static void deleteTextureIds(final GL10 gl, final Collection<Integer> ids) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(StateType.Texture);

        final int[] buffer = new int[ids.size()];
        int index = 0;
        for (final Integer i : ids) {
            if (i != null) {
                buffer[index++] = i.intValue();
                record.removeTextureRecord(i.intValue());
            }
        }
        if (index > 0) {
            gl.glDeleteTextures(index, buffer, 0);
        }
    }

    /**
     * Useful for external classes that need to safely set the current texture.
     */
    public static void doTextureBind(final GL10 gl, final Texture texture, final int unit, final boolean invalidateState) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();
        final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(StateType.Texture);
        if (invalidateState) {
            // Set this to null because no current state really matches anymore
            context.setCurrentState(StateType.Texture, null);
        }
        AndroidTextureStateUtil.checkAndSetUnit(gl, unit, record, caps);

        final int id = texture.getTextureIdForContext(context.getGlContextRep());
        gl.glBindTexture(AndroidTextureStateUtil.getGLType(texture.getType()), id);
        if (Constants.stats) {
            StatCollector.addStat(StatType.STAT_TEXTURE_BINDS, 1);
        }
        if (record != null) {
            record.units[unit].boundTexture = id;
        }
    }

    public static int getGLType(final Type type) {
        switch (type) {
            case TwoDimensional:
                return GL10.GL_TEXTURE_2D;
            case CubeMap:
                return GL11ExtensionPack.GL_TEXTURE_CUBE_MAP;
            default:
                throw new IllegalArgumentException("unsupported texture type: " + type);
        }
    }

    public static int getGLCubeMapFace(final TextureCubeMap.Face face) {
        switch (face) {
            case PositiveX:
                return GL11ExtensionPack.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
            case NegativeX:
                return GL11ExtensionPack.GL_TEXTURE_CUBE_MAP_NEGATIVE_X;
            case PositiveY:
                return GL11ExtensionPack.GL_TEXTURE_CUBE_MAP_POSITIVE_Y;
            case NegativeY:
                return GL11ExtensionPack.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y;
            case PositiveZ:
                return GL11ExtensionPack.GL_TEXTURE_CUBE_MAP_POSITIVE_Z;
            case NegativeZ:
                return GL11ExtensionPack.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
        }
        throw new IllegalArgumentException("invalid cubemap face: " + face);
    }

    private static int getGLWrap(final WrapMode wrap, final ContextCapabilities caps) {
        switch (wrap) {
            case Repeat:
                return GL10.GL_REPEAT;
            case MirroredRepeat:
                if (caps.isTextureMirroredRepeatSupported()) {
                    return GL11ExtensionPack.GL_MIRRORED_REPEAT;
                } else {
                    return GL10.GL_REPEAT;
                }
            case MirrorClamp:
                // if (caps.isTextureMirrorClampSupported()) {
                // return GL11ExtensionPack.GL_MIRROR_CLAMP_EXT;
                // }
                // FALLS THROUGH
            case Clamp:
                // No GL_CLAMP?
                return GL10.GL_CLAMP_TO_EDGE;
            case MirrorBorderClamp:
                // if (caps.isTextureMirrorBorderClampSupported()) {
                // return GL11ExtensionPack.GL_MIRROR_CLAMP_TO_BORDER_EXT;
                // }
                // FALLS THROUGH
            case BorderClamp:
                // if (caps.isTextureBorderClampSupported()) {
                // return GL11ExtensionPack.GL_CLAMP_TO_BORDER;
                // } else {
                // return GL11.GL_CLAMP;
                // }
                return GL10.GL_CLAMP_TO_EDGE;
            case MirrorEdgeClamp:
                // if (caps.isTextureMirrorEdgeClampSupported()) {
                // return GL11ExtensionPack.GL_MIRROR_CLAMP_TO_EDGE_EXT;
                // }
                // FALLS THROUGH
            case EdgeClamp:
                if (caps.isTextureEdgeClampSupported()) {
                    return GL10.GL_CLAMP_TO_EDGE;
                } else {
                    // No GL_CLAMP?
                    return GL10.GL_CLAMP_TO_EDGE;
                }
        }
        throw new IllegalArgumentException("invalid WrapMode type: " + wrap);
    }

    private static void buildMipmaps(final GL10 gl, final int texType, Bitmap bitmap) {
        int level = 0;
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

        Log.w(AndroidCanvas.TAG, width + "," + height);

        boolean original = true;
        while (height >= 1 || width >= 1) {
            // First of all, generate the texture from our bitmap and set it to the according level
            GLUtils.texImage2D(texType, level, bitmap, 0);

            // escape if this is the lowest level
            if (height == 1 || width == 1) {
                break;
            }

            // Increase the mipmap level
            level++;

            // drop by power of 2
            height >>= 1;
            width >>= 1;

            Log.w(AndroidCanvas.TAG, width + "," + height);

            // resample image
            final Bitmap bitmap2 = Bitmap.createScaledBitmap(bitmap, width, height, true);

            // Clean up
            if (!original) {
                bitmap.recycle();
            } else {
                original = false;
            }
            bitmap = bitmap2;
        }
    }
}
