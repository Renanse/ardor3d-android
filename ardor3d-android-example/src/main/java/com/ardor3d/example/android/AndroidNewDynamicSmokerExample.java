
package com.ardor3d.example.android;

import com.ardor3d.extension.effect.particle.ParticleFactory;
import com.ardor3d.extension.effect.particle.ParticleSystem;
import com.ardor3d.extension.effect.particle.ParticleSystem.ParticleType;
import com.ardor3d.extension.effect.particle.emitter.MeshEmitter;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.input.MouseState;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.MouseMovedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.RendererCallable;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.shape.Arrow;
import com.ardor3d.scenegraph.shape.Disk;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.TextureManager;

public class AndroidNewDynamicSmokerExample extends AndroidExampleBase {
    private final double ROCKET_TURN_SPEED = 10;
    private final double ROCKET_PROPEL_SPEED = 80;

    private ParticleSystem smoke;
    private final Node rocketEntityNode = new Node("rocket-node");
    private final Vector2 mouseLoc = new Vector2();
    private final Vector3 worldStore = new Vector3();
    private final Ray3 ray = new Ray3();
    private final Plane rocketPlane = new Plane(Vector3.NEG_UNIT_Z, 0);

    @Override
    protected void setupExample() {
        // set background color
        GameTaskQueueManager.getManager(_canvas).render(new RendererCallable<Void>() {
            public Void call() throws Exception {
                getRenderer().setBackgroundColor(ColorRGBA.BLUE);
                return null;
            }
        });

        // set our camera at a fixed position
        final Camera cam = _canvas.getCanvasRenderer().getCamera();
        cam.setLocation(0, 0, 300);

        // add our "rocket"
        buildRocket();

        // add smoke to the end of the rocket
        addEngineSmoke();

        // set initial mouse position to near center
        mouseLoc.set(cam.getWidth() / 2 + 0.1, cam.getHeight() / 2 + 0.1);
    }

    @Override
    protected void registerInputTriggers() {
        _logicalLayer.registerTrigger(new InputTrigger(new MouseMovedCondition(), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                mouseLoc.set(mouse.getX(), mouse.getY());
            }
        }));
    }

    @Override
    public void updateExample(final double tpf) {
        super.updateExample(tpf);

        // Update rocket
        updateRocket(tpf);
    }

    private void updateRocket(double tpf) {
        // keep it sane.
        if (tpf > .1) {
            tpf = .1;
        }
        // find mouse location in world coords
        _canvas.getCanvasRenderer().getCamera().getPickRay(mouseLoc, false, ray);
        ray.intersectsPlane(rocketPlane, worldStore);

        // get rocket's current orientation as quat
        final Quaternion currentOrient = new Quaternion().fromRotationMatrix(rocketEntityNode.getWorldRotation());

        // get orientation that points rocket nose straight at mouse
        final Vector3 dirTowardsMouse = new Vector3(worldStore).subtractLocal(rocketEntityNode.getWorldTranslation())
                .normalizeLocal();
        final Quaternion targetOrient = new Quaternion().fromVectorToVector(Vector3.NEG_UNIT_Z, dirTowardsMouse);

        // get a scale representing choice between direction of old and new quats
        final double scale = currentOrient.dot(targetOrient) * tpf * ROCKET_TURN_SPEED;
        currentOrient.addLocal(targetOrient.multiplyLocal(scale)).normalizeLocal();

        rocketEntityNode.setRotation(currentOrient);

        // propel forward
        rocketEntityNode.addTranslation(currentOrient.apply(Vector3.NEG_UNIT_Z, null).multiplyLocal(
                ROCKET_PROPEL_SPEED * tpf));
    }

    private void buildRocket() {
        final Arrow rocket = new Arrow("rocket", 5, 2);
        rocket.setRotation(new Quaternion().fromAngleAxis(MathUtils.DEG_TO_RAD * -90, Vector3.UNIT_X));
        rocketEntityNode.attachChild(rocket);
        rootNode.attachChild(rocketEntityNode);
    }

    private void addEngineSmoke() {
        final Disk emitDisc = new Disk("disc", 6, 6, 1.5f);
        emitDisc.setTranslation(new Vector3(0, 0, 2.5));
        emitDisc.getSceneHints().setCullHint(CullHint.Always);
        rocketEntityNode.attachChild(emitDisc);

        smoke = ParticleFactory.buildParticles("particles", 50, ParticleType.Triangle);
        smoke.setEmissionDirection(new Vector3(0f, 0f, 1f));
        smoke.setMaximumAngle(0.0f);
        smoke.setSpeed(1.0f);
        smoke.setMinimumLifeTime(600);
        smoke.setMaximumLifeTime(1000);
        smoke.setStartSize(1.0f);
        smoke.setEndSize(12.0f);
        smoke.setStartColor(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
        smoke.setEndColor(new ColorRGBA(.22f, .2f, .18f, 0.0f));
        smoke.setInitialVelocity(0.03f);
        smoke.setParticleEmitter(new MeshEmitter(emitDisc, false));
        smoke.setRotateWithScene(true);
        // smoke.getParticleGeometry().getSceneHints().setDataMode(DataMode.VBO);

        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        blend.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        smoke.setRenderState(blend);

        final TextureState ts = new TextureState();
        final Image img = getImageFromResources(R.drawable.flare, getResources(), true);
        final Texture tex = TextureManager.loadFromImage(img, MinificationFilter.BilinearNearestMipMap);
        tex.setWrap(WrapMode.BorderClamp);
        ts.setTexture(tex);
        ts.setEnabled(true);
        smoke.setRenderState(ts);

        final ZBufferState zstate = new ZBufferState();
        zstate.setWritable(false);
        smoke.setRenderState(zstate);
        rocketEntityNode.attachChild(smoke);
    }
}