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

package com.ardor3d.renderer.android;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import android.util.Log;

import com.ardor3d.framework.android.AndroidCanvas;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.util.geom.BufferUtils;

public class AndroidContextCapabilities extends ContextCapabilities {

    private final String _exts;
    private final boolean _supports_ES1_1;
    private final boolean _supports_ES1_0;

    /**
     * Detect supported extensions.
     * 
     * XXX: Some of this is guess work.
     * 
     * @param gl
     */
    public AndroidContextCapabilities(final GL10 gl) {
        // Grab our version
        final String version = gl.glGetString(GL10.GL_VERSION);
        Log.i(AndroidCanvas.TAG, "AndroidContextCapabilities() - reported gl version: " + version);
        _supports_ES1_1 = version.indexOf("1.1") >= 0;
        _supports_ES1_0 = _supports_ES1_1 || version.indexOf("1.0") >= 0;

        // Grab our extensions
        _exts = gl.glGetString(GL10.GL_EXTENSIONS);
        Log.i(AndroidCanvas.TAG, "AndroidContextCapabilities() - reported gl extensions: " + _exts);

        // Reusable buffer
        final int[] idBuffer = new int[1];

        _supportsVBO = _supports_ES1_1 || isExtensionAvailable("GL_ARB_vertex_buffer_object")
                || isExtensionAvailable("GL_OES_vertex_buffer_object");

        _supportsMultisample = _supports_ES1_0;

        _supportsConstantColor = false; // Not available in 1.0 or 1.1
        _supportsSeparateFunc = false;
        // isExtensionAvailable("GL_EXT_blend_func_separate")
        // || isExtensionAvailable("GL_OES_blend_func_separate");
        _supportsEq = _supportsSeparateEq = false;
        // isExtensionAvailable("GL_EXT_blend_equation_separate")
        // || isExtensionAvailable("GL_OES_blend_equation_separate");
        _supportsMinMax = isExtensionAvailable("GL_EXT_blend_minmax") || isExtensionAvailable("GL_OES_blend_minmax");
        _supportsSubtract = isExtensionAvailable("GL_EXT_blend_subtract")
                || isExtensionAvailable("GL_OES_blend_subtract");

        _supportsFogCoords = false;
        _supportsFragmentProgram = false;
        _supportsVertexProgram = false;

        _supportsPointSprites = isExtensionAvailable("GL_ARB_point_sprite");
        _supportsPointParameters = isExtensionAvailable("GL_ARB_point_parameters");

        _supportsTextureLodBias = false;
        if (_supportsTextureLodBias) {
            // gl.glGetInteger(GL11.GL_MAX_TEXTURE_LOD_BIAS_EXT, buf);
            // _maxTextureLodBias = buf.get(0);
        } else {
            _maxTextureLodBias = 0f;
        }

        // max user clips
        gl.glGetIntegerv(GL11.GL_MAX_CLIP_PLANES, idBuffer, 0);
        _maxUserClipPlanes = idBuffer[0];

        _glslSupported = false;

        if (_glslSupported) {
            // gl.glGetInteger(GL10.GL_MAX_VERTEX_ATTRIBS_ARB, buf);
            // _maxGLSLVertexAttribs = buf.get(0);
        }

        // Pbuffer
        _pbufferSupported = false; // XXX: not sure here...

        // FBO
        _fboSupported = isExtensionAvailable("GL_OES_framebuffer_object");
        if (_fboSupported) {
            _maxFBOColorAttachments = 1;
        } else {
            _maxFBOColorAttachments = 0;
        }
        _maxFBOSamples = 0;

        _twoSidedStencilSupport = false;
        _stencilWrapSupport = isExtensionAvailable("GL_OES_stencil_wrap");

        // number of available auxiliary draw buffers
        _numAuxDrawBuffers = 0;

        // max texture size.
        gl.glGetIntegerv(GL10.GL_MAX_TEXTURE_SIZE, idBuffer, 0);
        _maxTextureSize = idBuffer[0];

        // Check for support of multitextures.
        _supportsMultiTexture = _supports_ES1_0;

        // Check for support of fixed function dot3 environment settings
        _supportsEnvDot3 = _supports_ES1_1;

        // Check for support of combine environment settings
        _supportsEnvCombine = _supports_ES1_1;

        // Check for support of automatic mipmap generation
        _automaticMipMaps = _supports_ES1_1;

        _supportsDepthTexture = false; // No GL_DEPTH_TEXTURE_MODE?
        _supportsShadow = false; // No GL_TEXTURE_COMPARE_MODE?

        // If we do support multitexturing, find out how many textures we
        // can handle.
        if (_supportsMultiTexture) {
            gl.glGetIntegerv(GL10.GL_MAX_TEXTURE_UNITS, idBuffer, 0);
            _numFixedTexUnits = idBuffer[0];
        } else {
            _numFixedTexUnits = 1;
        }

        // Go on to check number of texture units supported for vertex and
        // fragment shaders
        // if (caps.GL_ARB_shader_objects && caps.GL_ARB_vertex_shader && caps.GL_ARB_fragment_shader) {
        // gl.glGetInteger(GL10.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS_ARB, buf);
        // _numVertexTexUnits = buf.get(0);
        // gl.glGetInteger(GL10.GL_MAX_TEXTURE_IMAGE_UNITS_ARB, buf);
        // _numFragmentTexUnits = buf.get(0);
        // gl.glGetInteger(GL10.GL_MAX_TEXTURE_COORDS_ARB, buf);
        // _numFragmentTexCoordUnits = buf.get(0);
        // } else {
        // based on nvidia dev doc:
        // http://developer.nvidia.com/object/General_FAQ.html#t6
        // "For GPUs that do not support GL_ARB_fragment_program and
        // GL_NV_fragment_program, those two limits are set equal to
        // GL_MAX_TEXTURE_UNITS."
        _numFragmentTexCoordUnits = _numFixedTexUnits;
        _numFragmentTexUnits = _numFixedTexUnits;

        // We'll set this to 0 for now since we do not know:
        _numVertexTexUnits = 0;
        // }

        // Now determine the maximum number of supported texture units
        _numTotalTexUnits = Math.max(_numFragmentTexCoordUnits, Math.max(_numFixedTexUnits, Math.max(
                _numFragmentTexUnits, _numVertexTexUnits)));

        // Check for S3 texture compression capability.
        _supportsS3TCCompression = false; // No support for GL_COMPRESSED_RGB_S3TC_XXX... XXX: probably another way.

        // Check for LA texture compression capability.
        _supportsLATCCompression = false; // Not supported

        // Check for generic texture compression capability.
        _supportsGenericCompression = false; // XXX: look at this

        // Check for 3D texture capability.
        _supportsTexture3D = false; // No support for GL_TEXTURE_3D in Android

        // Check for cubemap capability.
        _supportsTextureCubeMap = isExtensionAvailable("GL_OES_texture_cube_map");

        // See if we support anisotropic filtering
        _supportsAniso = isExtensionAvailable("GL_EXT_texture_filter_anisotropic");

        if (_supportsAniso) {
            final FloatBuffer max_a = BufferUtils.createFloatBuffer(16);
            max_a.rewind();

            // Grab the maximum anisotropic filter. -- not available in Android?
            // gl.glGetFloat(GL11ExtensionPack.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max_a);

            // set max.
            _maxAnisotropic = max_a.get(0);
        }

        // See if we support textures that are not power of 2 in size.
        _supportsNonPowerTwo = false; // XXX: Not sure what we can check here.

        // See if we support textures that do not have width == height.
        _supportsRectangular = false; // No support for GL_TEXTURE_RECTANGLE?

        _supportsMirroredRepeat = isExtensionAvailable("GL_OES_texture_mirrored_repeat");
        // No support for GL_MIRROR_CLAMP_XXX?
        _supportsMirrorClamp = _supportsMirrorEdgeClamp = _supportsMirrorBorderClamp = false;
        _supportsBorderClamp = false; // No support for GL_CLAMP_TO_BORDER?
        _supportsEdgeClamp = _supports_ES1_0;

        try {
            _displayVendor = gl.glGetString(GL10.GL_VENDOR);
            Log.d(AndroidCanvas.TAG, "AndroidContextCapabilities() - reported GL_VENDOR: " + _displayVendor);
        } catch (final Exception e) {
            _displayVendor = "Unable to retrieve vendor.";
        }

        try {
            _displayRenderer = gl.glGetString(GL10.GL_RENDERER);
            Log.d(AndroidCanvas.TAG, "AndroidContextCapabilities() - reported GL_RENDERER: " + _displayRenderer);
        } catch (final Exception e) {
            _displayRenderer = "Unable to retrieve adapter details.";
        }

        try {
            _displayVersion = gl.glGetString(GL10.GL_VERSION);
            Log.d(AndroidCanvas.TAG, "AndroidContextCapabilities() - reported GL_VERSION: " + _displayVersion);
        } catch (final Exception e) {
            _displayVersion = "Unable to retrieve API version.";
        }
    }

    private boolean isExtensionAvailable(final String extName) {
        return _exts.indexOf(extName) >= 0;
    }

    public boolean isOES10Suported() {
        return _supports_ES1_0;
    }

    public boolean isOES11Suported() {
        return _supports_ES1_1;
    }
}
