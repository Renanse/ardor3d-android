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

import javax.microedition.khronos.opengles.GL10;

import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.Scene;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.Camera.ProjectionMode;
import com.ardor3d.renderer.android.AndroidContextCapabilities;
import com.ardor3d.renderer.android.AndroidRenderer;

public class AndroidCanvasRenderer implements CanvasRenderer {
    protected Scene _scene;
    protected Camera _camera = new Camera(1, 1);
    protected boolean _doSwap;
    protected AndroidRenderer _renderer;
    protected Object _context = new Object();
    protected int _frameClear = Renderer.BUFFER_COLOR_AND_DEPTH;

    private RenderContext _currentContext;

    public AndroidCanvasRenderer(final Scene scene) {
        _scene = scene;
        _renderer = new AndroidRenderer();

        /** Set up a default camera. */
        _camera.setProjectionMode(ProjectionMode.Perspective);
        _camera.setFrustumPerspective(45.0f, 1.0, 1.0f, 1000.0f);

        final Vector3 loc = new Vector3(0.0f, 0.0f, 10.0f);
        final Vector3 left = new Vector3(-1.0f, 0.0f, 0.0f);
        final Vector3 up = new Vector3(0.0f, 1.0f, 0.0f);
        final Vector3 dir = new Vector3(0.0f, 0f, -1.0f);
        _camera.setFrame(loc, left, up, dir);
    }

    public void init(final DisplaySettings settings, final boolean doSwap) {
        _doSwap = doSwap;

        final AndroidContextCapabilities caps = new AndroidContextCapabilities(_renderer.getGL());
        _currentContext = new RenderContext(this, caps, null);

        ContextManager.addContext(this, _currentContext);
        ContextManager.switchContext(this);

        if (settings.getSamples() != 0 && caps.isMultisampleSupported()) {
            _renderer.getGL().glEnable(GL10.GL_MULTISAMPLE);
        }

        _renderer.setBackgroundColor(ColorRGBA.BLACK);

        _camera.resize(settings.getWidth(), settings.getHeight());
        _camera.setFrustumPerspective(_camera.getFovY(), (float) settings.getWidth() / (float) settings.getHeight(),
                _camera.getFrustumNear(), _camera.getFrustumFar());
    }

    public void setGL(final GL10 gl) {
        if (_renderer != null) {
            _renderer.setGL(gl);
        }
    }

    public boolean draw() {

        // set up context for rendering this canvas
        makeCurrentContext();

        // render stuff, first apply our camera if we have one
        if (_camera != null) {
            if (Camera.getCurrentCamera() != _camera) {
                _camera.update();
            }
            _camera.apply(_renderer);
        }
        _renderer.clearBuffers(_frameClear);

        final boolean drew = _scene.renderUnto(_renderer);
        _renderer.flushFrame(drew && _doSwap);

        // release the context
        releaseCurrentContext();

        return drew;
    }

    public Camera getCamera() {
        return _camera;
    }

    public RenderContext getRenderContext() {
        return _currentContext;
    }

    public Renderer getRenderer() {
        return _renderer;
    }

    public Scene getScene() {
        return _scene;
    }

    public void makeCurrentContext() {
        // XXX: Can/should we ask for thread to grab opengl context?
        ContextManager.switchContext(this);
    }

    public void releaseCurrentContext() {
        ; // Nothing to do here yet.
    }

    public void setCamera(final Camera camera) {
        _camera = camera;
    }

    public void setScene(final Scene scene) {
        _scene = scene;
    }

    public int getFrameClear() {
        return _frameClear;
    }

    public void setFrameClear(final int buffers) {
        _frameClear = buffers;
    }
}