
package com.ardor3d.example.android;

import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.RendererCallable;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.TextureManager;

public class AndroidBoxExample extends AndroidExampleBase {
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

        // add a texture to box
        final Image img = getImageFromResources(R.drawable.ardor3d_white, getResources(), true);
        final Texture t = TextureManager.loadFromImage(img, MinificationFilter.BilinearNearestMipMap);
        final TextureState ts = new TextureState();
        ts.setTexture(t);
        box.setRenderState(ts);
    }
}