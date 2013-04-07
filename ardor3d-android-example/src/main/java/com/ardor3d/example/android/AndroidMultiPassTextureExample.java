
package com.ardor3d.example.android;

import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.RendererCallable;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.extension.PassNode;
import com.ardor3d.scenegraph.extension.PassNodeState;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.TextureManager;

public class AndroidMultiPassTextureExample extends AndroidExampleBase {
    @Override
    protected void setupExample() {
        // set background color
        GameTaskQueueManager.getManager(_canvas).render(new RendererCallable<Void>() {
            public Void call() throws Exception {
                getRenderer().setBackgroundColor(ColorRGBA.BLUE);
                return null;
            }
        });

        // add our box and a rotating controller
        final Box box = new Box("box", Vector3.ZERO, 1, 1, 1);
        box.addController(new SpatialController<Spatial>() {
            private final Vector3 _axis = new Vector3(1, 1, 0.5f).normalizeLocal();
            private final Matrix3 _rotate = new Matrix3();
            private double _angle = 0;

            public void update(final double time, final Spatial caller) {
                // update our rotation
                _angle = _angle + _timer.getTimePerFrame() * 25;
                if (_angle > 180) {
                    _angle = -180;
                }

                _rotate.fromAngleNormalAxis(_angle * MathUtils.DEG_TO_RAD, _axis);
                box.setRotation(_rotate);
            }
        });
        rootNode.attachChild(box);

        // Create our states to use in the passes
        final TextureState ts1 = new TextureState();
        final Image img1 = getImageFromResources(R.drawable.ardor3d_white, getResources(), true);
        final Texture t = TextureManager.loadFromImage(img1, MinificationFilter.BilinearNearestMipMap);
        ts1.setTexture(t);

        final TextureState ts2 = new TextureState();
        final Image img2 = getImageFromResources(R.drawable.flaresmall, getResources(), true);
        final Texture t2 = TextureManager.loadFromImage(img2, MinificationFilter.BilinearNearestMipMap);
        ts2.setTexture(t2);

        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        blend.setSourceFunction(BlendState.SourceFunction.DestinationColor);
        blend.setDestinationFunction(BlendState.DestinationFunction.SourceColor);

        // Set up our passes
        final PassNodeState pass1 = new PassNodeState();
        pass1.setPassState(ts1);

        final PassNodeState pass2 = new PassNodeState();
        pass2.setPassState(ts2);
        pass2.setPassState(blend);

        // Add the passes to the pass node
        final PassNode pNode = new PassNode();
        pNode.addPass(pass1);
        pNode.addPass(pass2);

        // Attach the box to the pass node.
        pNode.attachChild(box);

        // Attach the pass node to the scenegraph root.
        rootNode.attachChild(pNode);
    }
}