
package com.ardor3d.example.android;

import android.os.Bundle;
import android.util.Log;

import com.ardor3d.framework.Scene;
import com.ardor3d.framework.android.AndroidCanvas;
import com.ardor3d.framework.android.BasicArdor3DActivity;
import com.ardor3d.input.android.AndroidFirstPersonControl;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.light.DirectionalLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ContextGarbageCollector;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;

public abstract class AndroidExampleBase extends BasicArdor3DActivity implements Scene {

    protected final Node rootNode;
    protected final Node uiNode;
    protected final BasicText frameRateLabel;
    protected LightState lightState;

    public AndroidExampleBase() {
        Log.i(AndroidCanvas.TAG, "Creating " + getClass().getName() + "...");

        // setup a node to act as root of our scene
        rootNode = new Node("root");

        // setup a node to act as root of our hud
        uiNode = new Node("UI");

        // add a text label
        frameRateLabel = BasicText.createDefaultTextLabel("fpsLabel", "test");
        frameRateLabel.setTranslation(0, 0, 0);
        frameRateLabel.setTextColor(ColorRGBA.WHITE);
        uiNode.attachChild(frameRateLabel);

        // add z-depth state to box
        final ZBufferState zbuff = new ZBufferState();
        zbuff.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
        rootNode.setRenderState(zbuff);

        lightState = new LightState();
        lightState.setTwoSidedLighting(false);
        final DirectionalLight light = new DirectionalLight();
        light.setDirection(new Vector3(-1, -1, -1).normalizeLocal());
        light.setDiffuse(ColorRGBA.WHITE);
        light.setAmbient(new ColorRGBA(.5f, .5f, .5f, 1));
        light.setEnabled(true);
        lightState.attach(light);
        rootNode.setRenderState(lightState);

        registerInputTriggers();
    }

    protected void registerInputTriggers() {
        new AndroidFirstPersonControl().registerCallbacks(_logicalLayer);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupExample();
    }

    protected abstract void setupExample();

    public boolean renderUnto(final Renderer renderer) {
        // Execute renderQueue item
        GameTaskQueueManager.getManager(_canvas).getQueue(GameTaskQueue.RENDER).execute(renderer);

        // Clean up card garbage such as textures, vbos, etc.
        ContextGarbageCollector.doRuntimeCleanup(renderer);

        // Call renderExample in any derived classes.
        renderExample(renderer);
        return true;
    }

    protected void renderExample(final Renderer renderer) {
        rootNode.onDraw(renderer);
        renderer.renderBuckets();
        uiNode.onDraw(renderer);
    }

    protected int frames = 0;
    protected long startTime = System.currentTimeMillis();

    @Override
    public void doUpdate(final double tpf) {
        updateLogicalLayer(tpf);

        // Execute updateQueue item
        GameTaskQueueManager.getManager(_canvas).getQueue(GameTaskQueue.UPDATE).execute();

        /** Call simpleUpdate in any derived classes of ExampleBase. */
        updateExample(tpf);

        final long now = System.currentTimeMillis();
        final long dt = now - startTime;
        if (dt > 2000) {
            final long fps = Math.round(1e3 * frames / dt);
            frameRateLabel.setText(fps + " fps");

            startTime = now;
            frames = 0;
        }
        frames++;

        /** Update controllers/render states/transforms/bounds for rootNode. */
        rootNode.updateGeometricState(tpf, true);
        uiNode.updateGeometricState(tpf, true);
    }

    protected void updateLogicalLayer(final double tpf) {
        // check and execute any input triggers, if we are concerned with input
        if (_logicalLayer != null) {
            _logicalLayer.checkTriggers(tpf);
        }
    }

    protected void updateExample(final double tpf) {
        // does nothing
    }

    @Override
    protected Scene getScene() {
        return this;
    }

    public PickResults doPick(final Ray3 arg0) {
        return null;
    }
}