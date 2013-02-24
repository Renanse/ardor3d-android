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

package com.ardor3d.framework.android;

import java.util.concurrent.CountDownLatch;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;

public class AndroidCanvas extends GLSurfaceView implements com.ardor3d.framework.Canvas, GLSurfaceView.Renderer {

    public static final String TAG = "Ardor3D";

    private final AndroidCanvasRenderer _canvasRenderer;
    private final DisplaySettings _settings;

    private volatile boolean _updated = false;
    private CountDownLatch _latch = null;

    public AndroidCanvas(final DisplaySettings settings, final AndroidCanvasRenderer canvasRenderer,
            final Context context) {
        super(context);
        _settings = settings;
        _canvasRenderer = canvasRenderer;

        // Uncomment if you need to debug things.
        // setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);

        setEGLConfigChooser(8, 8, 8, 8, _settings.getDepthBits(), _settings.getStencilBits());
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void draw(final CountDownLatch latch) {
        if (_updated) {
            return;
        }

        _updated = true;
        _latch = latch;

        requestRender();
    }

    public CanvasRenderer getCanvasRenderer() {
        return _canvasRenderer;
    }

    public void init() {
        Log.d(AndroidCanvas.TAG, "AndroidCanvas.init() - init canvas");
        _canvasRenderer.init(_settings, false); // false - do not do back buffer swap, android will do that.
    }

    public void onDrawFrame(final GL10 gl) {
        if (!_updated) {
            return;
        }

        // make sure GL is recent
        _canvasRenderer.setGL(gl);

        // draw canvas
        _canvasRenderer.draw();

        _updated = false;
        _latch.countDown();
    }

    public void onSurfaceChanged(final GL10 gl, final int width, final int height) {
        Log.d(AndroidCanvas.TAG, "AndroidCanvas.onSurfaceChanged - " + width + ", " + height);

        // make sure GL is recent
        _canvasRenderer.setGL(gl);

        final Camera cam = _canvasRenderer.getCamera();
        cam.resize(width, height);
        cam.setFrustumPerspective(cam.getFovY(), width / (float) height, cam.getFrustumNear(), cam.getFrustumFar());
    }

    public void onSurfaceCreated(final GL10 gl, final EGLConfig arg1) {
        Log.d(AndroidCanvas.TAG, "AndroidCanvas.onSurfaceCreated");

        // make sure GL is recent
        _canvasRenderer.setGL(gl);

        // save
        final Camera save = new Camera(_canvasRenderer.getCamera());
        final ColorRGBA bgColor = new ColorRGBA(_canvasRenderer.getRenderer().getBackgroundColor());

        init();

        // restore
        _canvasRenderer.getCamera().set(save);
        _canvasRenderer.getRenderer().setBackgroundColor(bgColor);

        // invalidate items tied to the canvas, if applicable.
        if (ContextManager.getContextForKey(_canvasRenderer) != null) {
            Log.d(AndroidCanvas.TAG, "AndroidCanvas.onSurfaceCreated: invalidating context");
            ContextManager.getContextForKey(_canvasRenderer).contextLost();
        }

        // TODO: Store the EGLConfig somewhere so we can create matching Pbuffers.
    }
}
