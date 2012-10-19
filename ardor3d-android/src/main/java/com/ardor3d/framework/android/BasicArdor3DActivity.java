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
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.ardor3d.extension.android.AndroidImageLoader;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.Scene;
import com.ardor3d.image.Image;
import com.ardor3d.image.util.ImageLoaderUtil;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.android.AndroidKeyWrapper;
import com.ardor3d.input.android.AndroidMouseWrapper;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scene.state.android.AndroidTextureStateUtil;
import com.ardor3d.util.ContextGarbageCollector;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.Timer;
import com.ardor3d.util.resource.URLResourceSource;

public abstract class BasicArdor3DActivity extends Activity implements Runnable {
    protected AndroidCanvas _canvas;
    protected AndroidCanvasRenderer _canvasRenderer;
    protected boolean _endThread = false;

    protected final Timer _timer = new Timer();

    protected AndroidKeyWrapper _keyWrapper;
    protected AndroidMouseWrapper _mouseWrapper;
    protected LogicalLayer _logicalLayer = new LogicalLayer();
    protected PhysicalLayer _physicalLayer;
    private AndroidImageLoader _loader = null;

    public BasicArdor3DActivity() {
        TextureState.DEFAULT_TEXTURE_SOURCE = new URLResourceSource(
                AndroidTextureStateUtil.class.getResource("notloaded.png"));
        ImageLoaderUtil.registerDefaultHandler(new AndroidImageLoader());
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e(AndroidCanvas.TAG, "*******ONCREATE");

        // Create our renderer and canvas
        _canvasRenderer = new AndroidCanvasRenderer(getScene());
        _canvas = new AndroidCanvas(getSettings(), _canvasRenderer, this);

        // input
        _keyWrapper = new AndroidKeyWrapper();
        _mouseWrapper = new AndroidMouseWrapper(_canvas);
        _physicalLayer = new PhysicalLayer(_keyWrapper, _mouseWrapper);
        _logicalLayer.registerInput(_canvas, _physicalLayer);

        // Add canvas to view
        setContentView(_canvas);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ContextManager.removeContext(_canvasRenderer);
        GameTaskQueueManager.clearManager(_canvas);
    }

    protected abstract Scene getScene();

    protected DisplaySettings getSettings() {
        return new DisplaySettings(100, 100, 16, 0, 0, 16, 0, 0, false, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        _canvas.onResume();

        // reset some vars
        _timer.reset();
        _endThread = false;

        // Kick off thread.
        new Thread(this).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        _canvas.onPause();

        // End thread.
        _endThread = true;
    }

    public void run() {
        Log.i(AndroidCanvas.TAG, "Ardor3DActivity.run - starting game loop");
        while (!_endThread) {
            // update timer
            _timer.update();

            doUpdate(_timer.getTimePerFrame());

            final CountDownLatch latch = new CountDownLatch(1);
            _canvas.draw(latch);

            try {
                latch.await(1, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                ; // ignore
            }

            Thread.yield();
        }
        ContextGarbageCollector.doFinalCleanup(_canvas.getCanvasRenderer().getRenderer());
        Log.i(AndroidCanvas.TAG, "Ardor3DActivity.run - ending game loop");
    }

    protected void doUpdate(final double tpf) {
        _logicalLayer.checkTriggers(tpf);

        // execute queue
        GameTaskQueueManager.getManager(_canvas).getQueue(GameTaskQueue.UPDATE).execute();
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        _mouseWrapper.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return super.onKeyDown(keyCode, event);
        }
        _keyWrapper.keyPressed(event);
        return true;
    }

    @Override
    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return super.onKeyUp(keyCode, event);
        }
        _keyWrapper.keyReleased(event);
        return true;
    }

    protected Image getImageFromResources(final int drawableId, final Resources resources, final boolean flipped) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        return getImageFromResources(drawableId, resources, flipped, options);
    }

    protected Image getImageFromResources(final int drawableId, final Resources resources, final boolean flipped,
            final BitmapFactory.Options options) {
        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), drawableId, options);
        if (_loader == null) {
            _loader = new AndroidImageLoader();
        }
        return _loader.loadFromBitMap(bitmap, flipped, options);
    }
}
