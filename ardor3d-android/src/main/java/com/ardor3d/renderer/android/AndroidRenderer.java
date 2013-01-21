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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Collection;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import android.opengl.GLException;
import android.util.Log;

import com.ardor3d.framework.android.AndroidCanvas;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture1D;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.Texture3D;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.image.TextureCubeMap.Face;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyRectangle2;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.AbstractRenderer;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.DrawBufferTarget;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.ClipState;
import com.ardor3d.renderer.state.ColorMaskState;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.FogState;
import com.ardor3d.renderer.state.FragmentProgramState;
import com.ardor3d.renderer.state.GLSLShaderObjectsState;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.OffsetState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.ShadingState;
import com.ardor3d.renderer.state.StencilState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.VertexProgramState;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.state.record.LineRecord;
import com.ardor3d.renderer.state.record.RendererRecord;
import com.ardor3d.scene.state.android.AndroidBlendStateUtil;
import com.ardor3d.scene.state.android.AndroidClipStateUtil;
import com.ardor3d.scene.state.android.AndroidColorMaskStateUtil;
import com.ardor3d.scene.state.android.AndroidCullStateUtil;
import com.ardor3d.scene.state.android.AndroidFogStateUtil;
import com.ardor3d.scene.state.android.AndroidFragmentProgramStateUtil;
import com.ardor3d.scene.state.android.AndroidLightStateUtil;
import com.ardor3d.scene.state.android.AndroidMaterialStateUtil;
import com.ardor3d.scene.state.android.AndroidOffsetStateUtil;
import com.ardor3d.scene.state.android.AndroidShaderObjectsStateUtil;
import com.ardor3d.scene.state.android.AndroidShadingStateUtil;
import com.ardor3d.scene.state.android.AndroidStencilStateUtil;
import com.ardor3d.scene.state.android.AndroidTextureStateUtil;
import com.ardor3d.scene.state.android.AndroidVertexProgramStateUtil;
import com.ardor3d.scene.state.android.AndroidWireframeStateUtil;
import com.ardor3d.scene.state.android.AndroidZBufferStateUtil;
import com.ardor3d.scene.state.android.util.AndroidRendererUtil;
import com.ardor3d.scene.state.android.util.AndroidTextureUtil;
import com.ardor3d.scenegraph.AbstractBufferData;
import com.ardor3d.scenegraph.AbstractBufferData.VBOAccessMode;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Renderable;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.NormalsMode;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.Constants;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;

public class AndroidRenderer extends AbstractRenderer {

    private GL10 _gl;

    private final FloatBuffer _transformBuffer = BufferUtils.createFloatBuffer(16);
    private final Matrix4 _transformMatrix = new Matrix4();

    /**
     * Constructor instantiates a new <code>AndroidRenderer</code> object.
     */
    public AndroidRenderer() {
        Log.i(AndroidCanvas.TAG, "AndroidRenderer() - AndroidRenderer created.");
    }

    public void setBackgroundColor(final ReadOnlyColorRGBA color) {
        _backgroundColor.set(color);
        _gl.glClearColor(_backgroundColor.getRed(), _backgroundColor.getGreen(), _backgroundColor.getBlue(),
                _backgroundColor.getAlpha());
    }

    public void renderBuckets() {
        renderBuckets(true, true);
    }

    public void renderBuckets(final boolean doSort, final boolean doClear) {
        _processingQueue = true;
        if (doSort && doClear) {
            _queue.renderBuckets(this);
        } else {
            if (doSort) {
                _queue.sortBuckets();
            }
            _queue.renderOnly(this);
            if (doClear) {
                _queue.clearBuckets();
            }
        }
        _processingQueue = false;
    }

    /**
     * clear the render queue
     */
    public void clearQueue() {
        _queue.clearBuckets();
    }

    public void clearBuffers(final int buffers) {
        clearBuffers(buffers, false);
    }

    public void clearBuffers(final int buffers, final boolean strict) {

        int clear = 0;

        if ((buffers & Renderer.BUFFER_COLOR) != 0) {
            clear |= GL10.GL_COLOR_BUFFER_BIT;
        }

        if ((buffers & Renderer.BUFFER_DEPTH) != 0) {
            clear |= GL10.GL_DEPTH_BUFFER_BIT;

            // make sure no funny business is going on in the z before clearing.
            if (defaultStateList.containsKey(RenderState.StateType.ZBuffer)) {
                defaultStateList.get(RenderState.StateType.ZBuffer).setNeedsRefresh(true);
                doApplyState(defaultStateList.get(RenderState.StateType.ZBuffer));
            }
        }

        if ((buffers & Renderer.BUFFER_STENCIL) != 0) {
            clear |= GL10.GL_STENCIL_BUFFER_BIT;

            _gl.glClearStencil(_stencilClearValue);
            _gl.glStencilMask(~0);
            _gl.glClear(GL10.GL_STENCIL_BUFFER_BIT);
        }

        if ((buffers & Renderer.BUFFER_ACCUMULATION) != 0) {
            // ignore
            // clear |= GL_ACCUM_BUFFER_BIT;
        }

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();

        if (strict) {
            // grab our camera to get width and height info.
            final Camera cam = Camera.getCurrentCamera();

            _gl.glEnable(GL10.GL_SCISSOR_TEST);
            _gl.glScissor(0, 0, cam.getWidth(), cam.getHeight());
            record.setClippingTestEnabled(true);
        }

        _gl.glClear(clear);

        if (strict) {
            // put us back.
            AndroidRendererUtil.applyScissors(_gl, record);
        }
    }

    public void flushFrame(final boolean doSwap) {
        renderBuckets();

        _gl.glFlush();

        checkCardError();
        if (doSwap) {
            doApplyState(defaultStateList.get(RenderState.StateType.ColorMask));

            // if (Constants.stats) {
            // StatCollector.startStat(StatType.STAT_DISPLAYSWAP_TIMER);
            // }
            //
            // // gl.swapBuffers();
            //
            // if (Constants.stats) {
            // StatCollector.endStat(StatType.STAT_DISPLAYSWAP_TIMER);
            // }
            // XXX: Android handles this.
        }

        if (Constants.stats) {
            StatCollector.addStat(StatType.STAT_FRAMES, 1);
        }
    }

    public void setOrtho() {
        if (_inOrthoMode) {
            throw new Ardor3dException("Already in Orthographic mode.");
        }
        // set up ortho mode
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        AndroidRendererUtil.switchMode(_gl, matRecord, GL10.GL_PROJECTION);
        _gl.glPushMatrix();
        _gl.glLoadIdentity();
        final Camera camera = Camera.getCurrentCamera();
        final double viewportWidth = camera.getWidth() * (camera.getViewPortRight() - camera.getViewPortLeft());
        final double viewportHeight = camera.getHeight() * (camera.getViewPortTop() - camera.getViewPortBottom());
        _gl.glOrthof(0, (float) viewportWidth, 0, (float) viewportHeight, -1, 1);
        AndroidRendererUtil.switchMode(_gl, matRecord, GL10.GL_MODELVIEW);
        _gl.glPushMatrix();
        _gl.glLoadIdentity();
        _inOrthoMode = true;
    }

    public void unsetOrtho() {
        if (!_inOrthoMode) {
            throw new Ardor3dException("Not in Orthographic mode.");
        }
        // remove ortho mode, and go back to original
        // state
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        AndroidRendererUtil.switchMode(_gl, matRecord, GL10.GL_PROJECTION);
        _gl.glPopMatrix();
        AndroidRendererUtil.switchMode(_gl, matRecord, GL10.GL_MODELVIEW);
        _gl.glPopMatrix();
        _inOrthoMode = false;
    }

    public void grabScreenContents(final ByteBuffer buff, final ImageDataFormat format, final int x, final int y,
            final int w, final int h) {
        final int pixFormat = AndroidTextureUtil.getGLPixelFormat(format);
        _gl.glReadPixels(x, y, w, h, pixFormat, GL10.GL_UNSIGNED_BYTE, buff);
    }

    public void draw(final Spatial s) {
        if (s != null) {
            s.onDraw(this);
        }
    }

    public boolean checkAndAdd(final Spatial s) {
        final RenderBucketType rqMode = s.getSceneHints().getRenderBucketType();
        if (rqMode != RenderBucketType.Skip) {
            getQueue().addToQueue(s, rqMode);
            return true;
        }
        return false;
    }

    public void flushGraphics() {
        _gl.glFlush();
    }

    public void finishGraphics() {
        _gl.glFinish();
    }

    public void applyNormalsMode(final NormalsMode normalsMode, final ReadOnlyTransform worldTransform) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        if (normalsMode != NormalsMode.Off) {
            final ContextCapabilities caps = context.getCapabilities();
            switch (normalsMode) {
                case NormalizeIfScaled:
                    if (worldTransform.isRotationMatrix()) {
                        final ReadOnlyVector3 scale = worldTransform.getScale();
                        if (!(scale.getX() == 1.0 && scale.getY() == 1.0 && scale.getZ() == 1.0)) {
                            if (scale.getX() == scale.getY() && scale.getY() == scale.getZ()
                                    && caps.isOpenGL1_2Supported()
                                    && rendRecord.getNormalMode() != GL10.GL_RESCALE_NORMAL) {
                                if (rendRecord.getNormalMode() == GL10.GL_NORMALIZE) {
                                    _gl.glDisable(GL10.GL_NORMALIZE);
                                }
                                _gl.glEnable(GL10.GL_RESCALE_NORMAL);
                                rendRecord.setNormalMode(GL10.GL_RESCALE_NORMAL);
                            } else if (rendRecord.getNormalMode() != GL10.GL_NORMALIZE) {
                                if (rendRecord.getNormalMode() == GL10.GL_RESCALE_NORMAL) {
                                    _gl.glDisable(GL10.GL_RESCALE_NORMAL);
                                }
                                _gl.glEnable(GL10.GL_NORMALIZE);
                                rendRecord.setNormalMode(GL10.GL_NORMALIZE);
                            }
                        } else {
                            if (rendRecord.getNormalMode() == GL10.GL_RESCALE_NORMAL) {
                                _gl.glDisable(GL10.GL_RESCALE_NORMAL);
                            } else if (rendRecord.getNormalMode() == GL10.GL_NORMALIZE) {
                                _gl.glDisable(GL10.GL_NORMALIZE);
                            }
                            rendRecord.setNormalMode(GL10.GL_ZERO);
                        }
                    } else {
                        if (!worldTransform.getMatrix().isIdentity()) {
                            // *might* be scaled...
                            if (rendRecord.getNormalMode() != GL10.GL_NORMALIZE) {
                                if (rendRecord.getNormalMode() == GL10.GL_RESCALE_NORMAL) {
                                    _gl.glDisable(GL10.GL_RESCALE_NORMAL);
                                }
                                _gl.glEnable(GL10.GL_NORMALIZE);
                                rendRecord.setNormalMode(GL10.GL_NORMALIZE);
                            }
                        } else {
                            // not scaled
                            if (rendRecord.getNormalMode() == GL10.GL_RESCALE_NORMAL) {
                                _gl.glDisable(GL10.GL_RESCALE_NORMAL);
                            } else if (rendRecord.getNormalMode() == GL10.GL_NORMALIZE) {
                                _gl.glDisable(GL10.GL_NORMALIZE);
                            }
                            rendRecord.setNormalMode(GL10.GL_ZERO);
                        }
                    }
                    break;
                case AlwaysNormalize:
                    if (rendRecord.getNormalMode() != GL10.GL_NORMALIZE) {
                        if (rendRecord.getNormalMode() == GL10.GL_RESCALE_NORMAL) {
                            _gl.glDisable(GL10.GL_RESCALE_NORMAL);
                        }
                        _gl.glEnable(GL10.GL_NORMALIZE);
                        rendRecord.setNormalMode(GL10.GL_NORMALIZE);
                    }
                    break;
                case UseProvided:
                default:
                    if (rendRecord.getNormalMode() == GL10.GL_RESCALE_NORMAL) {
                        _gl.glDisable(GL10.GL_RESCALE_NORMAL);
                    } else if (rendRecord.getNormalMode() == GL10.GL_NORMALIZE) {
                        _gl.glDisable(GL10.GL_NORMALIZE);
                    }
                    rendRecord.setNormalMode(GL10.GL_ZERO);
                    break;
            }
        } else {
            if (rendRecord.getNormalMode() == GL10.GL_RESCALE_NORMAL) {
                _gl.glDisable(GL10.GL_RESCALE_NORMAL);
            } else if (rendRecord.getNormalMode() == GL10.GL_NORMALIZE) {
                _gl.glDisable(GL10.GL_NORMALIZE);
            }
            rendRecord.setNormalMode(GL10.GL_ZERO);
        }
    }

    public void applyDefaultColor(final ReadOnlyColorRGBA defaultColor) {
        if (defaultColor != null) {
            _gl.glColor4f(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(),
                    defaultColor.getAlpha());
        } else {
            _gl.glColor4f(1, 1, 1, 1);
        }
    }

    public void deleteVBOs(final Collection<Integer> ids) {
        if (!ContextManager.getCurrentContext().getCapabilities().isVBOSupported() || ids == null || ids.isEmpty()) {
            return;
        }
        final int idBuffer[] = new int[ids.size()];
        int index = 0;
        for (final Integer i : ids) {
            if (i != null && i != 0) {
                idBuffer[index++] = i;
            }
        }
        if (index != 0) {
            ((GL11) _gl).glDeleteBuffers(index, idBuffer, 0);
        }
    }

    public void deleteVBOs(final AbstractBufferData<?> buffer) {
        if (!ContextManager.getCurrentContext().getCapabilities().isVBOSupported() || buffer == null) {
            return;
        }

        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();

        final int id = buffer.getVBOID(context.getGlContextRep());
        if (id == 0) {
            // Not on card... return.
            return;
        }

        buffer.removeVBOID(context.getGlContextRep());

        final int[] idBuffer = new int[] { id };
        ((GL11) _gl).glDeleteBuffers(1, idBuffer, 0);
    }

    public void deleteDisplayLists(final Collection<Integer> ids) {
        Log.w(AndroidCanvas.TAG, "AndroidRenderer.deleteDisplayLists - DisplayLists not supported.");
        // for (final Integer i : ids) {
        // if (i != null && i != 0) {
        // _gl.glDeleteLists(i, 1);
        // }
        // }
    }

    public void updateTexture1DSubImage(final Texture1D destination, final int dstOffsetX, final int dstWidth,
            final ByteBuffer source, final int srcOffsetX) {
        updateTexSubImage(destination, dstOffsetX, 0, 0, dstWidth, 0, 0, source, srcOffsetX, 0, 0, 0, 0, null);
    }

    public void updateTexture2DSubImage(final Texture2D destination, final int dstOffsetX, final int dstOffsetY,
            final int dstWidth, final int dstHeight, final ByteBuffer source, final int srcOffsetX,
            final int srcOffsetY, final int srcTotalWidth) {
        updateTexSubImage(destination, dstOffsetX, dstOffsetY, 0, dstWidth, dstHeight, 0, source, srcOffsetX,
                srcOffsetY, 0, srcTotalWidth, 0, null);
    }

    public void updateTexture3DSubImage(final Texture3D destination, final int dstOffsetX, final int dstOffsetY,
            final int dstOffsetZ, final int dstWidth, final int dstHeight, final int dstDepth, final ByteBuffer source,
            final int srcOffsetX, final int srcOffsetY, final int srcOffsetZ, final int srcTotalWidth,
            final int srcTotalHeight) {
        updateTexSubImage(destination, dstOffsetX, dstOffsetY, dstOffsetZ, dstWidth, dstHeight, dstDepth, source,
                srcOffsetX, srcOffsetY, srcOffsetZ, srcTotalWidth, srcTotalHeight, null);
    }

    public void updateTextureCubeMapSubImage(final TextureCubeMap destination, final TextureCubeMap.Face dstFace,
            final int dstOffsetX, final int dstOffsetY, final int dstWidth, final int dstHeight,
            final ByteBuffer source, final int srcOffsetX, final int srcOffsetY, final int srcTotalWidth) {
        updateTexSubImage(destination, dstOffsetX, dstOffsetY, 0, dstWidth, dstHeight, 0, source, srcOffsetX,
                srcOffsetY, 0, srcTotalWidth, 0, dstFace);
    }

    private void updateTexSubImage(final Texture destination, final int dstOffsetX, final int dstOffsetY,
            final int dstOffsetZ, final int dstWidth, final int dstHeight, final int dstDepth, final ByteBuffer source,
            final int srcOffsetX, final int srcOffsetY, final int srcOffsetZ, final int srcTotalWidth,
            final int srcTotalHeight, final Face dstFace) {

        // Ignore textures that do not have an id set
        if (destination.getTextureIdForContext(ContextManager.getCurrentContext().getGlContextRep()) == 0) {
            Log.w(AndroidCanvas.TAG,
                    "AndroidRenderer.updateTexSubImage - Attempting to update a texture that is not currently on the card.");
            return;
        }

        // Determine the original texture configuration, so that this method can
        // restore the texture configuration to its original state.
        final int[] idBuffer = new int[1];
        _gl.glGetIntegerv(GL10.GL_UNPACK_ALIGNMENT, idBuffer, 0);
        final int origAlignment = idBuffer[0];
        // final int origRowLength = 0;
        // final int origImageHeight = 0;
        // final int origSkipPixels = 0;
        // final int origSkipRows = 0;
        // final int origSkipImages = 0;

        final int alignment = 1;

        // int rowLength;
        // if (srcTotalWidth == dstWidth) {
        // // When the row length is zero, then the width parameter is used.
        // // We use zero in these cases in the hope that we can avoid two
        // // unnecessary calls to glPixelStorei.
        // rowLength = 0;
        // } else {
        // // The number of pixels in a row is different than the number of
        // // pixels in the region to be uploaded to the texture.
        // rowLength = srcTotalWidth;
        // }

        // int imageHeight;
        // if (srcTotalHeight == dstHeight) {
        // // When the image height is zero, then the height parameter is used.
        // // We use zero in these cases in the hope that we can avoid two
        // // unnecessary calls to glPixelStorei.
        // imageHeight = 0;
        // } else {
        // // The number of pixels in a row is different than the number of
        // // pixels in the region to be uploaded to the texture.
        // imageHeight = srcTotalHeight;
        // }

        // Grab pixel format
        final int pixelFormat = AndroidTextureUtil.getGLPixelFormat(destination.getImage().getDataFormat());

        // bind...
        AndroidTextureStateUtil.doTextureBind(_gl, destination, 0, false);

        // Update the texture configuration (when necessary).

        if (origAlignment != alignment) {
            _gl.glPixelStorei(GL10.GL_UNPACK_ALIGNMENT, alignment);
        }

        // NOTE: The below is unsupported... which might mean this whole method is not very useful.

        // if (origRowLength != rowLength) {
        // _gl.glPixelStorei(GL10.GL_UNPACK_ROW_LENGTH, rowLength);
        // }
        // if (origSkipPixels != srcOffsetX) {
        // _gl.glPixelStorei(GL10.GL_UNPACK_SKIP_PIXELS, srcOffsetX);
        // }
        // // NOTE: The below will be skipped for texture types that don't support them because we are passing in 0's.
        // if (origSkipRows != srcOffsetY) {
        // _gl.glPixelStorei(GL10.GL_UNPACK_SKIP_ROWS, srcOffsetY);
        // }
        // if (origImageHeight != imageHeight) {
        // _gl.glPixelStorei(GL10.GL_UNPACK_IMAGE_HEIGHT, imageHeight);
        // }
        // if (origSkipImages != srcOffsetZ) {
        // _gl.glPixelStorei(GL10.GL_UNPACK_SKIP_IMAGES, srcOffsetZ);
        // }

        // Upload the image region into the texture.
        try {
            switch (destination.getType()) {
                case TwoDimensional:
                    _gl.glTexSubImage2D(GL10.GL_TEXTURE_2D, 0, dstOffsetX, dstOffsetY, dstWidth, dstHeight,
                            pixelFormat, GL10.GL_UNSIGNED_BYTE, source);
                    break;
                case OneDimensional:
                    throw new Ardor3dException("1D Textures not supported in this renderer.");
                    // _gl.glTexSubImage1D(GL10.GL_TEXTURE_1D, 0, dstOffsetX, dstWidth, pixelFormat,
                    // GL10.GL_UNSIGNED_BYTE, source);
                case ThreeDimensional:
                    throw new Ardor3dException("3D Textures not supported in this renderer.");
                    // _gl.glTexSubImage3D(GL11.GL_TEXTURE_3D, 0, dstOffsetX, dstOffsetY, dstOffsetZ, dstWidth,
                    // dstHeight,
                    // dstDepth, pixelFormat, GL10.GL_UNSIGNED_BYTE, source);
                case CubeMap:
                    _gl.glTexSubImage2D(AndroidTextureStateUtil.getGLCubeMapFace(dstFace), 0, dstOffsetX, dstOffsetY,
                            dstWidth, dstHeight, pixelFormat, GL10.GL_UNSIGNED_BYTE, source);
                    break;
                default:
                    throw new Ardor3dException("Unsupported type for updateTextureSubImage: " + destination.getType());
            }
        } finally {
            // Restore the texture configuration (when necessary)...
            // Restore alignment.
            if (origAlignment != alignment) {
                _gl.glPixelStorei(GL10.GL_UNPACK_ALIGNMENT, origAlignment);
            }
            // // Restore row length.
            // if (origRowLength != rowLength) {
            // _gl.glPixelStorei(GL10.GL_UNPACK_ROW_LENGTH, origRowLength);
            // }
            // // Restore skip pixels.
            // if (origSkipPixels != srcOffsetX) {
            // _gl.glPixelStorei(GL10.GL_UNPACK_SKIP_PIXELS, origSkipPixels);
            // }
            // // Restore skip rows.
            // if (origSkipRows != srcOffsetY) {
            // _gl.glPixelStorei(GL10.GL_UNPACK_SKIP_ROWS, origSkipRows);
            // }
            // // Restore image height.
            // if (origImageHeight != imageHeight) {
            // _gl.glPixelStorei(GL10.GL_UNPACK_IMAGE_HEIGHT, origImageHeight);
            // }
            // // Restore skip images.
            // if (origSkipImages != srcOffsetZ) {
            // _gl.glPixelStorei(GL10.GL_UNPACK_SKIP_IMAGES, origSkipImages);
            // }
        }
    }

    public void checkCardError() throws Ardor3dException {
        try {
            final int errorCode = _gl.glGetError();
            if (errorCode != GL10.GL_NO_ERROR) {
                throw new GLException(errorCode);
            }
        } catch (final GLException exception) {
            throw new Ardor3dException("Error in opengl: " + exception.getMessage(), exception);
        }
    }

    public void draw(final Renderable renderable) {
        if (renderLogic != null) {
            renderLogic.apply(renderable);
        }
        renderable.render(this);
        if (renderLogic != null) {
            renderLogic.restore(renderable);
        }
    }

    public boolean doTransforms(final ReadOnlyTransform transform) {
        // set world matrix
        if (!transform.isIdentity()) {
            synchronized (_transformMatrix) {
                transform.getGLApplyMatrix(_transformBuffer);

                final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
                AndroidRendererUtil.switchMode(_gl, matRecord, GL10.GL_MODELVIEW);
                _gl.glPushMatrix();
                _gl.glMultMatrixf(_transformBuffer);
                return true;
            }
        }
        return false;
    }

    public void undoTransforms(final ReadOnlyTransform transform) {
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        AndroidRendererUtil.switchMode(_gl, matRecord, GL10.GL_MODELVIEW);
        _gl.glPopMatrix();
    }

    public void setupVertexData(final FloatBufferData vertexBufferData) {
        final FloatBuffer vertexBuffer = vertexBufferData != null ? vertexBufferData.getBuffer() : null;

        if (vertexBuffer == null) {
            _gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        } else {
            _gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            vertexBuffer.rewind();
            _gl.glVertexPointer(vertexBufferData.getValuesPerTuple(), GL10.GL_FLOAT, 0, vertexBuffer);
        }
    }

    public void setupNormalData(final FloatBufferData normalBufferData) {
        final FloatBuffer normalBuffer = normalBufferData != null ? normalBufferData.getBuffer() : null;

        if (normalBuffer == null) {
            _gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
        } else {
            _gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
            normalBuffer.rewind();
            _gl.glNormalPointer(GL10.GL_FLOAT, 0, normalBuffer);
        }
    }

    public void setupColorData(final FloatBufferData colorBufferData) {
        final FloatBuffer colorBuffer = colorBufferData != null ? colorBufferData.getBuffer() : null;

        if (colorBuffer == null) {
            _gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        } else {
            _gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
            colorBuffer.rewind();
            _gl.glColorPointer(colorBufferData.getValuesPerTuple(), GL10.GL_FLOAT, 0, colorBuffer);
        }
    }

    public void setupFogData(final FloatBufferData fogBufferData) {
        // Ignore

        // final FloatBuffer fogBuffer = fogBufferData != null ? fogBufferData.getBuffer() : null;
        //
        // if (fogBuffer == null) {
        // GL11.glDisableClientState(EXTFogCoord.GL_FOG_COORDINATE_ARRAY_EXT);
        // } else if (_oldFogBuffer != fogBuffer) {
        // GL11.glEnableClientState(EXTFogCoord.GL_FOG_COORDINATE_ARRAY_EXT);
        // fogBuffer.rewind();
        // EXTFogCoord.glFogCoordPointerEXT(0, fogBuffer);
        // }
        //
        // _oldFogBuffer = fogBuffer;
    }

    public void setupTextureData(final List<FloatBufferData> textureCoords) {
        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();
        final RendererRecord rendRecord = context.getRendererRecord();

        final TextureState ts = (TextureState) context.getCurrentState(RenderState.StateType.Texture);
        int enabledTextures = rendRecord.getEnabledTextures();
        final boolean valid = rendRecord.isTexturesValid();
        boolean isOn, wasOn;
        if (ts != null) {
            final int max = caps.isMultitextureSupported() ? Math.min(caps.getNumberOfFragmentTexCoordUnits(),
                    TextureState.MAX_TEXTURES) : 1;
            for (int i = 0; i < max; i++) {
                wasOn = (enabledTextures & 2 << i) != 0;
                isOn = textureCoords != null && i < textureCoords.size() && textureCoords.get(i) != null
                        && textureCoords.get(i).getBuffer() != null;

                if (!isOn) {
                    if (valid && !wasOn) {
                        continue;
                    } else {
                        checkAndSetTextureArrayUnit(i, rendRecord, caps);

                        // disable bit in tracking int
                        enabledTextures &= ~(2 << i);

                        // disable state
                        _gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

                        continue;
                    }
                } else {
                    checkAndSetTextureArrayUnit(i, rendRecord, caps);

                    if (!valid || !wasOn) {
                        // enable state
                        _gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

                        // enable bit in tracking int
                        enabledTextures |= 2 << i;
                    }

                    final FloatBufferData textureBufferData = textureCoords.get(i);
                    final FloatBuffer textureBuffer = textureBufferData != null ? textureBufferData.getBuffer() : null;

                    _gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
                    textureBuffer.rewind();
                    _gl.glTexCoordPointer(textureBufferData.getValuesPerTuple(), GL10.GL_FLOAT, 0, textureBuffer);
                }
            }
        }

        rendRecord.setEnabledTextures(enabledTextures);
        rendRecord.setTexturesValid(true);
    }

    public void drawElements(final IndexBufferData<?> indices, final int[] indexLengths, final IndexMode[] indexModes,
            final int primcount) {
        if (indices == null || indices.getBuffer() == null) {
            Log.e(AndroidCanvas.TAG, "AndroidRenderer.drawElements - Missing indices for drawElements call without VBO");
            return;
        }
        if (primcount >= 0) {
            throw new UnsupportedOperationException("No support for instancing.");
        }

        if (indexLengths == null) {
            final int glIndexMode = getGLIndexMode(indexModes[0]);

            indices.position(0);

            int dataFormat;
            if (indices.getBuffer() instanceof ByteBuffer) {
                dataFormat = GL10.GL_UNSIGNED_BYTE;
            } else if (indices.getBuffer() instanceof ShortBuffer) {
                dataFormat = GL10.GL_UNSIGNED_SHORT;
            } else {
                Log.w(AndroidCanvas.TAG, "uh oh... Unable to render mesh with indices buffer of type: "
                        + indices.getBuffer().getClass());
                return;
            }

            _gl.glDrawElements(glIndexMode, indices.getBufferLimit(), dataFormat, indices.getBuffer());

            if (Constants.stats) {
                addStats(indexModes[0], indices.getBufferLimit());
            }
        } else {
            int offset = 0;
            int indexModeCounter = 0;
            for (int i = 0; i < indexLengths.length; i++) {
                final int count = indexLengths[i];

                final int glIndexMode = getGLIndexMode(indexModes[indexModeCounter]);

                indices.getBuffer().position(offset);
                indices.getBuffer().limit(offset + count);

                int dataFormat;
                if (indices.getBuffer() instanceof ByteBuffer) {
                    dataFormat = GL10.GL_UNSIGNED_BYTE;
                } else if (indices.getBuffer() instanceof ShortBuffer) {
                    dataFormat = GL10.GL_UNSIGNED_SHORT;
                } else {
                    Log.w(AndroidCanvas.TAG, "Unable to render mesh with indices buffer of type: "
                            + indices.getBuffer().getClass() + " - section: " + i);
                    return;
                }
                _gl.glDrawElements(glIndexMode, count, dataFormat, indices.getBuffer());

                if (Constants.stats) {
                    addStats(indexModes[indexModeCounter], count);
                }

                offset += count;

                if (indexModeCounter < indexModes.length - 1) {
                    indexModeCounter++;
                }
            }
        }
    }

    private int setupVBO(final FloatBufferData data, final RenderContext context, final RendererRecord rendRecord) {
        if (data == null) {
            return 0;
        }

        int vboID = data.getVBOID(context.getGlContextRep());
        if (vboID != 0) {
            updateVBO(data, rendRecord, vboID, 0);

            return vboID;
        }

        final FloatBuffer dataBuffer = data.getBuffer();
        if (dataBuffer != null) {
            // XXX: should we be rewinding? Maybe make that the programmer's responsibility.
            dataBuffer.rewind();
            vboID = makeVBOId(rendRecord);
            data.setVBOID(context.getGlContextRep(), vboID);

            rendRecord.invalidateVBO();
            AndroidRendererUtil.setBoundVBO((GL11) _gl, rendRecord, vboID);
            ((GL11) _gl).glBufferData(GL11.GL_ARRAY_BUFFER, dataBuffer.limit() * 4, dataBuffer,
                    getGLVBOAccessMode(data.getVboAccessMode()));
        } else {
            throw new Ardor3dException("Attempting to create a vbo id for a FloatBufferData with no Buffer value.");
        }
        return vboID;
    }

    private void updateVBO(final FloatBufferData data, final RendererRecord rendRecord, final int vboID,
            final int offsetBytes) {
        if (data.isNeedsRefresh()) {
            final FloatBuffer dataBuffer = data.getBuffer();
            dataBuffer.rewind();
            AndroidRendererUtil.setBoundVBO((GL11) _gl, rendRecord, vboID);
            ((GL11) _gl).glBufferSubData(GL11.GL_ARRAY_BUFFER, offsetBytes, dataBuffer.limit() * 4, dataBuffer);
            data.setNeedsRefresh(false);
        }
    }

    private int setupIndicesVBO(final IndexBufferData<?> data, final RenderContext context,
            final RendererRecord rendRecord) {
        if (data == null) {
            return 0;
        }

        int vboID = data.getVBOID(context.getGlContextRep());
        if (vboID != 0) {
            if (data.isNeedsRefresh()) {
                final Buffer dataBuffer = data.getBuffer();
                dataBuffer.rewind();
                AndroidRendererUtil.setBoundElementVBO((GL11) _gl, rendRecord, vboID);
                ((GL11) _gl).glBufferSubData(GL11.GL_ELEMENT_ARRAY_BUFFER, 0, dataBuffer.limit() * data.getByteCount(),
                        dataBuffer);
                data.setNeedsRefresh(false);
            }

            return vboID;
        }

        final Buffer dataBuffer = data.getBuffer();
        if (dataBuffer != null) {
            // XXX: should we be rewinding? Maybe make that the programmer's responsibility.
            dataBuffer.rewind();
            vboID = makeVBOId(rendRecord);
            data.setVBOID(context.getGlContextRep(), vboID);

            rendRecord.invalidateVBO();
            AndroidRendererUtil.setBoundElementVBO((GL11) _gl, rendRecord, vboID);
            ((GL11) _gl).glBufferData(GL11.GL_ELEMENT_ARRAY_BUFFER, dataBuffer.limit() * data.getByteCount(),
                    dataBuffer, getGLVBOAccessMode(data.getVboAccessMode()));
        } else {
            throw new Ardor3dException("Attempting to create a vbo id for a IndexBufferData with no Buffer value.");
        }
        return vboID;
    }

    public void setupVertexDataVBO(final FloatBufferData data) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();

        final int vboID = setupVBO(data, context, rendRecord);

        if (vboID != 0) {
            _gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            AndroidRendererUtil.setBoundVBO((GL11) _gl, rendRecord, vboID);
            ((GL11) _gl).glVertexPointer(data.getValuesPerTuple(), GL10.GL_FLOAT, 0, 0);
        } else {
            _gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
            AndroidRendererUtil.setBoundVBO((GL11) _gl, rendRecord, 0);
        }
    }

    public void setupNormalDataVBO(final FloatBufferData data) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();

        final int vboID = setupVBO(data, context, rendRecord);

        if (vboID != 0) {
            _gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
            AndroidRendererUtil.setBoundVBO((GL11) _gl, rendRecord, vboID);
            ((GL11) _gl).glNormalPointer(GL10.GL_FLOAT, 0, 0);
        } else {
            _gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
            AndroidRendererUtil.setBoundVBO((GL11) _gl, rendRecord, 0);
        }
    }

    public void setupColorDataVBO(final FloatBufferData data) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();

        final int vboID = setupVBO(data, context, rendRecord);

        if (vboID != 0) {
            _gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
            AndroidRendererUtil.setBoundVBO((GL11) _gl, rendRecord, vboID);
            ((GL11) _gl).glColorPointer(data.getValuesPerTuple(), GL10.GL_FLOAT, 0, 0);
        } else {
            _gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
            AndroidRendererUtil.setBoundVBO((GL11) _gl, rendRecord, 0);
        }
    }

    public void setupFogDataVBO(final FloatBufferData data) {
        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();

        if (!caps.isFogCoordinatesSupported()) {
            return;
        }

        // Not supported
        // final RendererRecord rendRecord = context.getRendererRecord();
        // final int vboID = setupVBO(data, context, rendRecord);
        //
        // if (vboID != 0) {
        // _gl.glEnableClientState(GL10.GL_FOG_COORDINATE_ARRAY);
        // AndroidRendererUtil.setBoundVBO((GL11) _gl, rendRecord, vboID);
        // ((GL11)_gl).glFogCoordPointer(GL10.GL_FLOAT, 0, 0);
        // } else {
        // _gl.glDisableClientState(GL10.GL_FOG_COORDINATE_ARRAY);
        // AndroidRendererUtil.setBoundVBO((GL11) _gl, rendRecord, 0);
        // }
    }

    public void setupTextureDataVBO(final List<FloatBufferData> textureCoords) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        final TextureState ts = (TextureState) context.getCurrentState(RenderState.StateType.Texture);
        int enabledTextures = rendRecord.getEnabledTextures();
        final boolean valid = rendRecord.isTexturesValid();
        boolean exists, wasOn;
        if (ts != null) {
            final int max = caps.isMultitextureSupported() ? Math.min(caps.getNumberOfFragmentTexCoordUnits(),
                    TextureState.MAX_TEXTURES) : 1;
            for (int i = 0; i < max; i++) {
                wasOn = (enabledTextures & 2 << i) != 0;
                exists = textureCoords != null && i < textureCoords.size();

                if (!exists) {
                    if (valid && !wasOn) {
                        continue;
                    } else {
                        checkAndSetTextureArrayUnit(i, rendRecord, caps);

                        // disable bit in tracking int
                        enabledTextures &= ~(2 << i);

                        // disable state
                        _gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

                        continue;
                    }
                } else {
                    checkAndSetTextureArrayUnit(i, rendRecord, caps);

                    // grab a vboID and make sure it exists and is up to date.
                    final FloatBufferData data = textureCoords.get(i);
                    final int vboID = setupVBO(data, context, rendRecord);

                    // Found good vbo
                    if (vboID != 0) {
                        if (!valid || !wasOn) {
                            // enable bit in tracking int
                            enabledTextures |= 2 << i;

                            // enable state
                            _gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
                        }

                        // set our active vbo
                        AndroidRendererUtil.setBoundVBO((GL11) _gl, rendRecord, vboID);

                        // send data
                        ((GL11) _gl).glTexCoordPointer(data.getValuesPerTuple(), GL10.GL_FLOAT, 0, 0);
                    }
                    // Not a good vbo, disable it.
                    else {
                        if (!valid || wasOn) {
                            // disable bit in tracking int
                            enabledTextures &= ~(2 << i);

                            // disable state
                            _gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
                        }

                        // set our active vbo to 0
                        AndroidRendererUtil.setBoundVBO((GL11) _gl, rendRecord, 0);
                    }
                }
            }
        }

        rendRecord.setEnabledTextures(enabledTextures);
        rendRecord.setTexturesValid(true);
    }

    public void setupInterleavedDataVBO(final FloatBufferData interleaved, final FloatBufferData vertexCoords,
            final FloatBufferData normalCoords, final FloatBufferData colorCoords,
            final List<FloatBufferData> textureCoords) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        final int lengthBytes = getTotalInterleavedSize(context, vertexCoords, normalCoords, colorCoords, textureCoords);
        int currLengthBytes = 0;
        if (interleaved.getBufferLimit() > 0) {
            interleaved.getBuffer().rewind();
            currLengthBytes = Math.round(interleaved.getBuffer().get());
        }

        if (lengthBytes != currLengthBytes || interleaved.getVBOID(context.getGlContextRep()) <= 0
                || interleaved.isNeedsRefresh()) {
            initializeInterleavedVBO(context, interleaved, vertexCoords, normalCoords, colorCoords, textureCoords,
                    lengthBytes);
        }

        final int vboID = interleaved.getVBOID(context.getGlContextRep());
        AndroidRendererUtil.setBoundVBO((GL11) _gl, rendRecord, vboID);

        int offsetBytes = 0;

        if (normalCoords != null) {
            updateVBO(normalCoords, rendRecord, vboID, offsetBytes);
            ((GL11) _gl).glNormalPointer(GL10.GL_FLOAT, 0, offsetBytes);
            _gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
            offsetBytes += normalCoords.getBufferLimit() * 4;
        } else {
            _gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
        }

        if (colorCoords != null) {
            updateVBO(colorCoords, rendRecord, vboID, offsetBytes);
            ((GL11) _gl).glColorPointer(colorCoords.getValuesPerTuple(), GL10.GL_FLOAT, 0, offsetBytes);
            _gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
            offsetBytes += colorCoords.getBufferLimit() * 4;
        } else {
            _gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        }

        if (textureCoords != null) {
            final TextureState ts = (TextureState) context.getCurrentState(RenderState.StateType.Texture);
            int enabledTextures = rendRecord.getEnabledTextures();
            final boolean valid = rendRecord.isTexturesValid();
            boolean exists, wasOn;
            if (ts != null) {
                final int max = caps.isMultitextureSupported() ? Math.min(caps.getNumberOfFragmentTexCoordUnits(),
                        TextureState.MAX_TEXTURES) : 1;
                for (int i = 0; i < max; i++) {
                    wasOn = (enabledTextures & 2 << i) != 0;
                    exists = i < textureCoords.size() && textureCoords.get(i) != null
                            && i <= ts.getMaxTextureIndexUsed();

                    if (!exists) {
                        if (valid && !wasOn) {
                            continue;
                        } else {
                            checkAndSetTextureArrayUnit(i, rendRecord, caps);

                            // disable bit in tracking int
                            enabledTextures &= ~(2 << i);

                            // disable state
                            _gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

                            continue;
                        }

                    }

                    checkAndSetTextureArrayUnit(i, rendRecord, caps);

                    // grab a vboID and make sure it exists and is up to date.
                    final FloatBufferData textureBufferData = textureCoords.get(i);
                    updateVBO(textureBufferData, rendRecord, vboID, offsetBytes);

                    if (!valid || !wasOn) {
                        // enable bit in tracking int
                        enabledTextures |= 2 << i;

                        // enable state
                        _gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
                    }

                    // send data
                    ((GL11) _gl)
                            .glTexCoordPointer(textureBufferData.getValuesPerTuple(), GL10.GL_FLOAT, 0, offsetBytes);
                    offsetBytes += textureBufferData.getBufferLimit() * 4;
                }
            }

            rendRecord.setEnabledTextures(enabledTextures);
            rendRecord.setTexturesValid(true);
        }

        if (vertexCoords != null) {
            updateVBO(vertexCoords, rendRecord, vboID, offsetBytes);
            ((GL11) _gl).glVertexPointer(vertexCoords.getValuesPerTuple(), GL10.GL_FLOAT, 0, offsetBytes);
            _gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        } else {
            _gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        }

        AndroidRendererUtil.setBoundVBO((GL11) _gl, rendRecord, 0);
    }

    private void initializeInterleavedVBO(final RenderContext context, final FloatBufferData interleaved,
            final FloatBufferData vertexCoords, final FloatBufferData normalCoords, final FloatBufferData colorCoords,
            final List<FloatBufferData> textureCoords, final int bufferSize) {

        // keep around buffer size
        if (interleaved.getBufferCapacity() != 1) {
            final FloatBuffer buffer = BufferUtils.createFloatBufferOnHeap(1);
            interleaved.setBuffer(buffer);
        }
        interleaved.getBuffer().rewind();
        interleaved.getBuffer().put(bufferSize);

        final RendererRecord rendRecord = context.getRendererRecord();
        final ContextCapabilities caps = context.getCapabilities();

        final int vboID = makeVBOId(rendRecord);
        interleaved.setVBOID(context.getGlContextRep(), vboID);

        rendRecord.invalidateVBO();
        AndroidRendererUtil.setBoundVBO((GL11) _gl, rendRecord, vboID);
        ((GL11) _gl).glBufferData(GL11.GL_ARRAY_BUFFER, bufferSize, null,
                getGLVBOAccessMode(interleaved.getVboAccessMode()));

        int offsetBytes = 0;
        if (normalCoords != null) {
            normalCoords.getBuffer().rewind();
            ((GL11) _gl).glBufferSubData(GL11.GL_ARRAY_BUFFER, offsetBytes, normalCoords.getBufferLimit() * 4,
                    normalCoords.getBuffer());
            offsetBytes += normalCoords.getBufferLimit() * 4;
        }
        if (colorCoords != null) {
            colorCoords.getBuffer().rewind();
            ((GL11) _gl).glBufferSubData(GL11.GL_ARRAY_BUFFER, offsetBytes, colorCoords.getBufferLimit() * 4,
                    colorCoords.getBuffer());
            offsetBytes += colorCoords.getBufferLimit() * 4;
        }
        if (textureCoords != null) {
            final TextureState ts = (TextureState) context.getCurrentState(RenderState.StateType.Texture);
            if (ts != null) {
                for (int i = 0; i <= ts.getMaxTextureIndexUsed() && i < caps.getNumberOfFragmentTexCoordUnits(); i++) {
                    if (textureCoords == null || i >= textureCoords.size()) {
                        continue;
                    }

                    final FloatBufferData textureBufferData = textureCoords.get(i);
                    final FloatBuffer textureBuffer = textureBufferData != null ? textureBufferData.getBuffer() : null;
                    if (textureBuffer != null) {
                        textureBuffer.rewind();
                        ((GL11) _gl).glBufferSubData(GL11.GL_ARRAY_BUFFER, offsetBytes,
                                textureBufferData.getBufferLimit() * 4, textureBuffer);
                        offsetBytes += textureBufferData.getBufferLimit() * 4;
                    }
                }
            }
        }
        if (vertexCoords != null) {
            vertexCoords.getBuffer().rewind();
            ((GL11) _gl).glBufferSubData(GL11.GL_ARRAY_BUFFER, offsetBytes, vertexCoords.getBufferLimit() * 4,
                    vertexCoords.getBuffer());
        }

        interleaved.setNeedsRefresh(false);
    }

    public void drawElementsVBO(final IndexBufferData<?> indices, final int[] indexLengths,
            final IndexMode[] indexModes, final int primcount) {
        if (primcount >= 0) {
            throw new UnsupportedOperationException("No support for instancing.");
        }

        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord rendRecord = context.getRendererRecord();

        final int vboID = setupIndicesVBO(indices, context, rendRecord);

        AndroidRendererUtil.setBoundElementVBO((GL11) _gl, rendRecord, vboID);

        if (indexLengths == null) {
            final int glIndexMode = getGLIndexMode(indexModes[0]);

            if (indices.getBuffer() instanceof ByteBuffer) {
                ((GL11) _gl).glDrawElements(glIndexMode, indices.getBufferLimit(), GL10.GL_UNSIGNED_BYTE, 0);
            } else {
                ((GL11) _gl).glDrawElements(glIndexMode, indices.getBufferLimit(), GL10.GL_UNSIGNED_SHORT, 0);
            }
            if (Constants.stats) {
                addStats(indexModes[0], indices.getBufferLimit());
            }
        } else {
            int offset = 0;
            int indexModeCounter = 0;
            for (int i = 0; i < indexLengths.length; i++) {
                final int count = indexLengths[i];

                final int glIndexMode = getGLIndexMode(indexModes[indexModeCounter]);

                // offset in this call is done in bytes.
                if (indices.getBuffer() instanceof ByteBuffer) {
                    ((GL11) _gl).glDrawElements(glIndexMode, count, GL10.GL_UNSIGNED_BYTE, offset);
                } else {
                    ((GL11) _gl).glDrawElements(glIndexMode, count, GL10.GL_UNSIGNED_SHORT, offset * 2);
                }

                if (Constants.stats) {
                    addStats(indexModes[indexModeCounter], count);
                }

                offset += count;

                if (indexModeCounter < indexModes.length - 1) {
                    indexModeCounter++;
                }
            }
        }
    }

    public void drawArrays(final FloatBufferData vertices, final int[] indexLengths, final IndexMode[] indexModes,
            final int primcount) {
        if (primcount >= 0) {
            throw new UnsupportedOperationException("No support for instancing.");
        }
        if (indexLengths == null) {
            final int glIndexMode = getGLIndexMode(indexModes[0]);

            _gl.glDrawArrays(glIndexMode, 0, vertices.getTupleCount());

            if (Constants.stats) {
                addStats(indexModes[0], vertices.getTupleCount());
            }
        } else {
            int offset = 0;
            int indexModeCounter = 0;
            for (int i = 0; i < indexLengths.length; i++) {
                final int count = indexLengths[i];

                final int glIndexMode = getGLIndexMode(indexModes[indexModeCounter]);

                _gl.glDrawArrays(glIndexMode, offset, count);

                if (Constants.stats) {
                    addStats(indexModes[indexModeCounter], count);
                }

                offset += count;

                if (indexModeCounter < indexModes.length - 1) {
                    indexModeCounter++;
                }
            }
        }
    }

    public int makeVBOId(final RendererRecord rendRecord) {
        final int[] buffer = new int[1];
        ((GL11) _gl).glGenBuffers(1, buffer, 0);
        return buffer[0];
    }

    public void unbindVBO() {
        final RenderContext context = ContextManager.getCurrentContext();
        if (context.getCapabilities().isVBOSupported()) {
            final RendererRecord rendRecord = context.getRendererRecord();
            AndroidRendererUtil.setBoundVBO((GL11) _gl, rendRecord, 0);
            AndroidRendererUtil.setBoundElementVBO((GL11) _gl, rendRecord, 0);
        }
    }

    private int getGLVBOAccessMode(final VBOAccessMode vboAccessMode) {
        int glMode = GL11.GL_STATIC_DRAW;
        switch (vboAccessMode) {
            case StaticDraw:
                glMode = GL11.GL_STATIC_DRAW;
                break;
            case DynamicDraw:
                glMode = GL11.GL_DYNAMIC_DRAW;
                break;
            case StaticRead:
                // glMode = GL11.GL_STATIC_READ;
                // break;
            case StaticCopy:
                // glMode = GL11.GL_STATIC_COPY;
                // break;
            case DynamicRead:
                // glMode = GL11.GL_DYNAMIC_READ;
                // break;
            case DynamicCopy:
                // glMode = GL11.GL_DYNAMIC_COPY;
                // break;
            case StreamDraw:
                // glMode = GL11.GL_STREAM_DRAW;
                // break;
            case StreamRead:
                // glMode = GL11.GL_STREAM_READ;
                // break;
            case StreamCopy:
                // glMode = GL11.GL_STREAM_COPY;
                // break;
            default:
                Log.e(AndroidCanvas.TAG,
                        "AndroidRenderer.getGLVBOAccessMode - This renderer does not support VBO mode: "
                                + vboAccessMode);
                throw new Ardor3dException("This renderer does not support VBO mode: " + vboAccessMode);
        }
        return glMode;
    }

    private int getGLIndexMode(final IndexMode indexMode) {
        int glMode = GL10.GL_TRIANGLES;
        switch (indexMode) {
            case Triangles:
                glMode = GL10.GL_TRIANGLES;
                break;
            case TriangleStrip:
                glMode = GL10.GL_TRIANGLE_STRIP;
                break;
            case TriangleFan:
                glMode = GL10.GL_TRIANGLE_FAN;
                break;
            case Quads:
            case QuadStrip:
                throw new Ardor3dException("Quads are not supported in this renderer.");
            case Lines:
                glMode = GL10.GL_LINES;
                break;
            case LineStrip:
                glMode = GL10.GL_LINE_STRIP;
                break;
            case LineLoop:
                glMode = GL10.GL_LINE_LOOP;
                break;
            case Points:
                glMode = GL10.GL_POINTS;
                break;
        }
        return glMode;
    }

    public void setModelViewMatrix(final FloatBuffer matrix) {
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        AndroidRendererUtil.switchMode(_gl, matRecord, GL10.GL_MODELVIEW);
        loadMatrix(matrix);
    }

    public void setProjectionMatrix(final FloatBuffer matrix) {
        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        AndroidRendererUtil.switchMode(_gl, matRecord, GL10.GL_PROJECTION);
        loadMatrix(matrix);
    }

    private void loadMatrix(final FloatBuffer matrix) {
        _gl.glLoadMatrixf(matrix);
    }

    public FloatBuffer getModelViewMatrix(final FloatBuffer store) {
        return getMatrix(GL11.GL_MODELVIEW_MATRIX, store);
    }

    public FloatBuffer getProjectionMatrix(final FloatBuffer store) {
        return getMatrix(GL11.GL_PROJECTION_MATRIX, store);
    }

    private FloatBuffer getMatrix(final int matrixType, final FloatBuffer store) {
        FloatBuffer result = store;
        if (result.remaining() < 16) {
            result = BufferUtils.createFloatBuffer(16);
        }
        ((GL11) _gl).glGetFloatv(matrixType, store);
        return result;
    }

    public void setViewport(final int x, final int y, final int width, final int height) {
        _gl.glViewport(x, y, width, height);
    }

    public void setDepthRange(final double depthRangeNear, final double depthRangeFar) {
        _gl.glDepthRangef((float) depthRangeNear, (float) depthRangeFar);
    }

    public void setDrawBuffer(final DrawBufferTarget target) {
        Log.w(AndroidCanvas.TAG, "AndroidRenderer.setDrawBuffer - Not supported by this renderer.");
        // final RendererRecord record = ContextManager.getCurrentContext().getRendererRecord();
        // if (record.getDrawBufferTarget() != target) {
        // int buffer = GL10.GL_BACK;
        // switch (target) {
        // case Back:
        // break;
        // case Front:
        // buffer = GL10.GL_FRONT;
        // break;
        // case FrontAndBack:
        // buffer = GL10.GL_FRONT_AND_BACK;
        // break;
        // // case BackLeft:
        // // buffer = GL11.GL_BACK_LEFT;
        // // break;
        // // case BackRight:
        // // buffer = GL11.GL_BACK_RIGHT;
        // // break;
        // // case FrontLeft:
        // // buffer = GL11.GL_FRONT_LEFT;
        // // break;
        // // case FrontRight:
        // // buffer = GL11.GL_FRONT_RIGHT;
        // // break;
        // // case Left:
        // // buffer = GL11.GL_LEFT;
        // // break;
        // // case Right:
        // // buffer = GL11.GL_RIGHT;
        // // break;
        // // case Aux0:
        // // buffer = GL11.GL_AUX0;
        // // break;
        // // case Aux1:
        // // buffer = GL11.GL_AUX1;
        // // break;
        // // case Aux2:
        // // buffer = GL11.GL_AUX2;
        // // break;
        // // case Aux3:
        // // buffer = GL11.GL_AUX3;
        // // break;
        // default:
        // throw new Ardor3dException("DrawBufferTarget not supported by this renderer: " + target);
        // }
        //
        // // _gl.glDrawBuffer(buffer);
        // // record.setDrawBufferTarget(target);
        // }
    }

    public void setupLineParameters(final float lineWidth, final int stippleFactor, final short stipplePattern,
            final boolean antialiased) {
        final LineRecord lineRecord = ContextManager.getCurrentContext().getLineRecord();

        if (!lineRecord.isValid() || lineRecord.width != lineWidth) {
            _gl.glLineWidth(lineWidth);
            lineRecord.width = lineWidth;
        }

        // STIPPLE NOT SUPPORTED
        // if (stipplePattern != (short) 0xFFFF) {
        // if (!lineRecord.isValid() || !lineRecord.stippled) {
        // GL11.glEnable(GL11.GL_LINE_STIPPLE);
        // lineRecord.stippled = true;
        // }
        //
        // if (!lineRecord.isValid() || stippleFactor != lineRecord.stippleFactor
        // || stipplePattern != lineRecord.stipplePattern) {
        // _gl.glLineStipple(stippleFactor, stipplePattern);
        // lineRecord.stippleFactor = stippleFactor;
        // lineRecord.stipplePattern = stipplePattern;
        // }
        // } else if (!lineRecord.isValid() || lineRecord.stippled) {
        // GL11.glDisable(GL11.GL_LINE_STIPPLE);
        // lineRecord.stippled = false;
        // }

        if (antialiased) {
            if (!lineRecord.isValid() || !lineRecord.smoothed) {
                _gl.glEnable(GL10.GL_LINE_SMOOTH);
                lineRecord.smoothed = true;
            }
            if (!lineRecord.isValid() || lineRecord.smoothHint != GL10.GL_NICEST) {
                _gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
                lineRecord.smoothHint = GL10.GL_NICEST;
            }
        } else if (!lineRecord.isValid() || lineRecord.smoothed) {
            _gl.glDisable(GL10.GL_LINE_SMOOTH);
            lineRecord.smoothed = false;
        }

        if (!lineRecord.isValid()) {
            lineRecord.validate();
        }
    }

    public void setupPointParameters(final float pointSize, final boolean antialiased, final boolean isSprite,
            final boolean useDistanceAttenuation, final FloatBuffer attenuationCoefficients, final float minPointSize,
            final float maxPointSize) {
        final RenderContext context = ContextManager.getCurrentContext();

        // TODO: make a record for point states
        _gl.glPointSize(pointSize);

        if (isSprite && context.getCapabilities().isPointSpritesSupported()) {
            _gl.glEnable(GL11.GL_POINT_SPRITE_OES);
            _gl.glTexEnvx(GL11.GL_POINT_SPRITE_OES, GL11.GL_COORD_REPLACE_OES, GL10.GL_TRUE);
        }

        if (useDistanceAttenuation && context.getCapabilities().isPointParametersSupported()) {
            ((GL11) _gl).glPointParameterfv(GL11.GL_POINT_DISTANCE_ATTENUATION, attenuationCoefficients);
            ((GL11) _gl).glPointParameterf(GL11.GL_POINT_SIZE_MIN, minPointSize);
            ((GL11) _gl).glPointParameterf(GL11.GL_POINT_SIZE_MAX, maxPointSize);
        }

        if (antialiased) {
            _gl.glEnable(GL10.GL_POINT_SMOOTH);
            _gl.glHint(GL10.GL_POINT_SMOOTH_HINT, GL10.GL_NICEST);
        }
    }

    @Override
    public void doApplyState(final RenderState state) {
        if (state == null) {
            Log.w(AndroidCanvas.TAG, "AndroidRenderer.doApplyState - tried to apply a null state.");
            return;
        }

        switch (state.getType()) {
            case Texture:
                AndroidTextureStateUtil.apply(_gl, (TextureState) state);
                return;
            case Light:
                AndroidLightStateUtil.apply(_gl, (LightState) state);
                return;
            case Blend:
                AndroidBlendStateUtil.apply(_gl, (BlendState) state);
                return;
            case Clip:
                AndroidClipStateUtil.apply(_gl, (ClipState) state);
                return;
            case ColorMask:
                AndroidColorMaskStateUtil.apply(_gl, (ColorMaskState) state);
                return;
            case Cull:
                AndroidCullStateUtil.apply(_gl, (CullState) state);
                return;
            case Fog:
                AndroidFogStateUtil.apply(_gl, (FogState) state);
                return;
            case FragmentProgram:
                AndroidFragmentProgramStateUtil.apply((FragmentProgramState) state);
                return;
            case GLSLShader:
                AndroidShaderObjectsStateUtil.apply((GLSLShaderObjectsState) state);
                return;
            case Material:
                AndroidMaterialStateUtil.apply(_gl, (MaterialState) state);
                return;
            case Offset:
                AndroidOffsetStateUtil.apply(_gl, (OffsetState) state);
                return;
            case Shading:
                AndroidShadingStateUtil.apply(_gl, (ShadingState) state);
                return;
            case Stencil:
                AndroidStencilStateUtil.apply(_gl, (StencilState) state);
                return;
            case VertexProgram:
                AndroidVertexProgramStateUtil.apply((VertexProgramState) state);
                return;
            case Wireframe:
                AndroidWireframeStateUtil.apply((WireframeState) state);
                return;
            case ZBuffer:
                AndroidZBufferStateUtil.apply(_gl, (ZBufferState) state);
                return;
        }
        // throw new IllegalArgumentException("Unknown state: " + state);
    }

    public void deleteTexture(final Texture texture) {
        AndroidTextureStateUtil.deleteTexture(_gl, texture);
    }

    public void loadTexture(final Texture texture, final int unit) {
        AndroidTextureStateUtil.load(_gl, texture, unit);
    }

    public void deleteTextureIds(final Collection<Integer> ids) {
        AndroidTextureStateUtil.deleteTextureIds(_gl, ids);
    }

    /**
     * Start a new display list. All further renderer commands that can be stored in a display list are part of this new
     * list until {@link #endDisplayList()} is called.
     * 
     * @return id of new display list
     */
    public int startDisplayList() {
        Log.w(AndroidCanvas.TAG, "startDisplayList - Display Lists not supported by this renderer.");
        // final int id = GL11.glGenLists(1);
        //
        // GL11.glNewList(id, GL11.GL_COMPILE);
        //
        // return id;
        return -1;
    }

    /**
     * Ends a display list. Will likely cause an OpenGL exception is a display list is not currently being generated.
     */
    public void endDisplayList() {
        Log.w(AndroidCanvas.TAG, "endDisplayList - Display Lists not supported by this renderer.");
        // _gl.glEndList();
    }

    /**
     * Draw the given display list.
     */
    public void renderDisplayList(final int displayListID) {
        Log.w(AndroidCanvas.TAG, "AndroidRenderer.renderDisplayList - Display Lists not supported by this renderer.");
        // _gl.glCallList(displayListID);

        // invalidate "current arrays"
        // reset();
    }

    public void clearClips() {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();
        record.getScissorClips().clear();

        AndroidRendererUtil.applyScissors(_gl, record);
    }

    public void popClip() {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();
        record.getScissorClips().pop();

        AndroidRendererUtil.applyScissors(_gl, record);
    }

    public void pushClip(final ReadOnlyRectangle2 rectangle) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();
        record.getScissorClips().push(rectangle);

        AndroidRendererUtil.applyScissors(_gl, record);
    }

    public void pushEmptyClip() {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();
        record.getScissorClips().push(null);

        AndroidRendererUtil.applyScissors(_gl, record);
    }

    public void setClipTestEnabled(final boolean enabled) {
        final RenderContext context = ContextManager.getCurrentContext();
        final RendererRecord record = context.getRendererRecord();

        AndroidRendererUtil.setClippingEnabled(_gl, record, enabled);
    }

    public void setGL(final GL10 gl) {
        _gl = gl;
    }

    public GL10 getGL() {
        return _gl;
    }

    public void checkAndSetTextureArrayUnit(final int unit, final RendererRecord record, final ContextCapabilities caps) {
        if (record.getCurrentTextureArraysUnit() != unit && caps.isMultitextureSupported()) {
            _gl.glClientActiveTexture(GL10.GL_TEXTURE0 + unit);
            record.setCurrentTextureArraysUnit(unit);
        }
    }
}
