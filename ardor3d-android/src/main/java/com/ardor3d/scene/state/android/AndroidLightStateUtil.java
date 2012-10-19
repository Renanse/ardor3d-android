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

import javax.microedition.khronos.opengles.GL10;

import com.ardor3d.light.DirectionalLight;
import com.ardor3d.light.Light;
import com.ardor3d.light.PointLight;
import com.ardor3d.light.SpotLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.LightRecord;
import com.ardor3d.renderer.state.record.LightStateRecord;

public abstract class AndroidLightStateUtil {

    public static void apply(final GL10 gl, final LightState state) {
        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();
        final LightStateRecord record = (LightStateRecord) context.getStateRecord(StateType.Light);
        context.setCurrentState(StateType.Light, state);

        if (state.isEnabled() && LightState.LIGHTS_ENABLED) {
            AndroidLightStateUtil.setLightEnabled(gl, true, record);
            AndroidLightStateUtil.setTwoSided(gl, state.getTwoSidedLighting(), record);
            AndroidLightStateUtil.setLocalViewer(gl, state.getLocalViewer(), record);
            if (caps.isOpenGL1_2Supported()) {
                AndroidLightStateUtil.setSpecularControl(gl, state.getSeparateSpecular(), record);
            }

            for (int i = 0, max = state.getNumberOfChildren(); i < max; i++) {
                final Light light = state.get(i);
                LightRecord lr = record.getLightRecord(i);
                // TODO: use the reference to get the lightrecord - rherlitz

                if (lr == null) {
                    lr = new LightRecord();
                    record.setLightRecord(lr, i);
                }

                if (light == null) {
                    AndroidLightStateUtil.setSingleLightEnabled(gl, false, i, record, lr);
                } else {
                    if (light.isEnabled()) {
                        AndroidLightStateUtil.setLight(gl, i, light, record, state, lr);
                    } else {
                        AndroidLightStateUtil.setSingleLightEnabled(gl, false, i, record, lr);
                    }
                }
            }

            // disable lights at and above the max count in this state
            for (int i = state.getNumberOfChildren(); i < LightState.MAX_LIGHTS_ALLOWED; i++) {
                LightRecord lr = record.getLightRecord(i);

                if (lr == null) {
                    lr = new LightRecord();
                    record.setLightRecord(lr, i);
                }
                AndroidLightStateUtil.setSingleLightEnabled(gl, false, i, record, lr);
            }

            if ((state.getLightMask() & LightState.MASK_GLOBALAMBIENT) == 0) {
                AndroidLightStateUtil.setModelAmbient(gl, record, state.getGlobalAmbient());
            } else {
                AndroidLightStateUtil.setModelAmbient(gl, record, ColorRGBA.BLACK_NO_ALPHA);
            }

            if (!record.isValid()) {
                record.validate();
            }
        } else {
            AndroidLightStateUtil.setLightEnabled(gl, false, record);
        }
    }

    private static void setLight(final GL10 gl, final int index, final Light light, final LightStateRecord record,
            final LightState state, final LightRecord lr) {
        AndroidLightStateUtil.setSingleLightEnabled(gl, true, index, record, lr);

        if ((state.getLightMask() & LightState.MASK_AMBIENT) == 0
                && (light.getLightMask() & LightState.MASK_AMBIENT) == 0) {
            AndroidLightStateUtil.setAmbient(gl, index, record, light.getAmbient(), lr);
        } else {
            AndroidLightStateUtil.setAmbient(gl, index, record, ColorRGBA.BLACK_NO_ALPHA, lr);
        }

        if ((state.getLightMask() & LightState.MASK_DIFFUSE) == 0
                && (light.getLightMask() & LightState.MASK_DIFFUSE) == 0) {

            AndroidLightStateUtil.setDiffuse(gl, index, record, light.getDiffuse(), lr);
        } else {
            AndroidLightStateUtil.setDiffuse(gl, index, record, ColorRGBA.BLACK_NO_ALPHA, lr);
        }

        if ((state.getLightMask() & LightState.MASK_SPECULAR) == 0
                && (light.getLightMask() & LightState.MASK_SPECULAR) == 0) {

            AndroidLightStateUtil.setSpecular(gl, index, record, light.getSpecular(), lr);
        } else {
            AndroidLightStateUtil.setSpecular(gl, index, record, ColorRGBA.BLACK_NO_ALPHA, lr);
        }

        if (light.isAttenuate()) {
            AndroidLightStateUtil.setAttenuate(gl, true, index, light, record, lr);

        } else {
            AndroidLightStateUtil.setAttenuate(gl, false, index, light, record, lr);

        }

        switch (light.getType()) {
            case Directional: {
                final DirectionalLight dirLight = (DirectionalLight) light;

                final ReadOnlyVector3 direction = dirLight.getDirection();
                AndroidLightStateUtil.setPosition(gl, index, record, -direction.getXf(), -direction.getYf(), -direction
                        .getZf(), 0, lr);
                break;
            }
            case Point:
            case Spot: {
                final PointLight pointLight = (PointLight) light;
                final ReadOnlyVector3 location = pointLight.getLocation();
                AndroidLightStateUtil.setPosition(gl, index, record, location.getXf(), location.getYf(), location
                        .getZf(), 1, lr);
                break;
            }
        }

        if (light.getType() == Light.Type.Spot) {
            final SpotLight spot = (SpotLight) light;
            final ReadOnlyVector3 direction = spot.getDirection();
            AndroidLightStateUtil.setSpotCutoff(gl, index, record, spot.getAngle(), lr);
            AndroidLightStateUtil.setSpotDirection(gl, index, record, direction.getXf(), direction.getYf(), direction
                    .getZf(), 0);
            AndroidLightStateUtil.setSpotExponent(gl, index, record, spot.getExponent(), lr);
        } else {
            // set the cutoff to 180, which causes the other spot params to be
            // ignored.
            AndroidLightStateUtil.setSpotCutoff(gl, index, record, 180, lr);
        }
    }

    private static void setSingleLightEnabled(final GL10 gl, final boolean enable, final int index,
            final LightStateRecord record, final LightRecord lr) {
        if (!record.isValid() || lr.isEnabled() != enable) {
            if (enable) {
                gl.glEnable(GL10.GL_LIGHT0 + index);
            } else {
                gl.glDisable(GL10.GL_LIGHT0 + index);
            }

            lr.setEnabled(enable);
        }
    }

    private static void setLightEnabled(final GL10 gl, final boolean enable, final LightStateRecord record) {
        if (!record.isValid() || record.isEnabled() != enable) {
            if (enable) {
                gl.glEnable(GL10.GL_LIGHTING);
            } else {
                gl.glDisable(GL10.GL_LIGHTING);
            }
            record.setEnabled(enable);
        }
    }

    private static void setTwoSided(final GL10 gl, final boolean twoSided, final LightStateRecord record) {
        if (!record.isValid() || record.isTwoSidedOn() != twoSided) {
            if (twoSided) {
                gl.glLightModelx(GL10.GL_LIGHT_MODEL_TWO_SIDE, GL10.GL_TRUE);
            } else {
                gl.glLightModelx(GL10.GL_LIGHT_MODEL_TWO_SIDE, GL10.GL_FALSE);
            }
            record.setTwoSidedOn(twoSided);
        }
    }

    private static void setLocalViewer(final GL10 gl, final boolean localViewer, final LightStateRecord record) {
    // XXX: Viewer is always at 0,0,INF in OpenGL ES 1.X
    // if (!record.isValid() || record.isLocalViewer() != localViewer) {
    // if (localViewer) {
    // gl.glLightModelx(GL10.GL_LIGHT_MODEL_LOCAL_VIEWER, GL10.GL_TRUE);
    // } else {
    // gl.glLightModelx(GL10.GL_LIGHT_MODEL_LOCAL_VIEWER, GL10.GL_FALSE);
    // }
    // record.setLocalViewer(localViewer);
    // }
    }

    private static void setSpecularControl(final GL10 gl, final boolean separateSpecularOn,
            final LightStateRecord record) {
    // XXX: Separate specular not supported in OpenGL ES 1.X
    // if (!record.isValid() || record.isSeparateSpecular() != separateSpecularOn) {
    // if (separateSpecularOn) {
    // gl.glLightModelx(GL11Ext.GL_LIGHT_MODEL_COLOR_CONTROL, GL10.GL_SEPARATE_SPECULAR_COLOR);
    // } else {
    // gl.glLightModelx(GL10.GL_LIGHT_MODEL_COLOR_CONTROL, GL10.GL_SINGLE_COLOR);
    // }
    // record.setSeparateSpecular(separateSpecularOn);
    // }
    }

    private static void setModelAmbient(final GL10 gl, final LightStateRecord record,
            final ReadOnlyColorRGBA globalAmbient) {
        if (!record.isValid() || !record.globalAmbient.equals(globalAmbient)) {
            record.lightBuffer.clear();
            record.lightBuffer.put(globalAmbient.getRed());
            record.lightBuffer.put(globalAmbient.getGreen());
            record.lightBuffer.put(globalAmbient.getBlue());
            record.lightBuffer.put(globalAmbient.getAlpha());
            record.lightBuffer.flip();
            gl.glLightModelfv(GL10.GL_LIGHT_MODEL_AMBIENT, record.lightBuffer); // TODO Check for float
            record.globalAmbient.set(globalAmbient);
        }
    }

    private static void setAmbient(final GL10 gl, final int index, final LightStateRecord record,
            final ReadOnlyColorRGBA ambient, final LightRecord lr) {
        if (!record.isValid() || !lr.ambient.equals(ambient)) {
            record.lightBuffer.clear();
            record.lightBuffer.put(ambient.getRed());
            record.lightBuffer.put(ambient.getGreen());
            record.lightBuffer.put(ambient.getBlue());
            record.lightBuffer.put(ambient.getAlpha());
            record.lightBuffer.flip();
            gl.glLightfv(GL10.GL_LIGHT0 + index, GL10.GL_AMBIENT, record.lightBuffer); // TODO Check for float
            lr.ambient.set(ambient);
        }
    }

    private static void setDiffuse(final GL10 gl, final int index, final LightStateRecord record,
            final ReadOnlyColorRGBA diffuse, final LightRecord lr) {
        if (!record.isValid() || !lr.diffuse.equals(diffuse)) {
            record.lightBuffer.clear();
            record.lightBuffer.put(diffuse.getRed());
            record.lightBuffer.put(diffuse.getGreen());
            record.lightBuffer.put(diffuse.getBlue());
            record.lightBuffer.put(diffuse.getAlpha());
            record.lightBuffer.flip();
            gl.glLightfv(GL10.GL_LIGHT0 + index, GL10.GL_DIFFUSE, record.lightBuffer); // TODO Check for float
            lr.diffuse.set(diffuse);
        }
    }

    private static void setSpecular(final GL10 gl, final int index, final LightStateRecord record,
            final ReadOnlyColorRGBA specular, final LightRecord lr) {
        if (!record.isValid() || !lr.specular.equals(specular)) {
            record.lightBuffer.clear();
            record.lightBuffer.put(specular.getRed());
            record.lightBuffer.put(specular.getGreen());
            record.lightBuffer.put(specular.getBlue());
            record.lightBuffer.put(specular.getAlpha());
            record.lightBuffer.flip();
            gl.glLightfv(GL10.GL_LIGHT0 + index, GL10.GL_SPECULAR, record.lightBuffer); // TODO Check for float
            lr.specular.set(specular);
        }
    }

    private static void setPosition(final GL10 gl, final int index, final LightStateRecord record,
            final float positionX, final float positionY, final float positionZ, final float positionW,
            final LightRecord lr) {
        // From OpenGL Docs:
        // The light position is transformed by the contents of the current top
        // of the ModelView matrix stack when you specify the light position
        // with a call to glLightfv(GL_LIGHT_POSITION,...). If you later change
        // the ModelView matrix, such as when the view changes for the next
        // frame, the light position isn't automatically retransformed by the
        // new contents of the ModelView matrix. If you want to update the
        // light's position, you must again specify the light position with a
        // call to glLightfv(GL_LIGHT_POSITION,...).

        // XXX: This is a hack until we get a better lighting model up
        final ReadOnlyMatrix4 modelViewMatrix = Camera.getCurrentCamera().getModelViewMatrix();

        if (!record.isValid() || lr.position.getXf() != positionX || lr.position.getYf() != positionY
                || lr.position.getZf() != positionZ || lr.position.getWf() != positionW
                || !lr.modelViewMatrix.equals(modelViewMatrix)) {

            record.lightBuffer.clear();
            record.lightBuffer.put(positionX);
            record.lightBuffer.put(positionY);
            record.lightBuffer.put(positionZ);
            record.lightBuffer.put(positionW);
            record.lightBuffer.flip();
            gl.glLightfv(GL10.GL_LIGHT0 + index, GL10.GL_POSITION, record.lightBuffer);

            lr.position.set(positionX, positionY, positionZ, positionW);
            lr.modelViewMatrix.set(modelViewMatrix);
        }
    }

    private static void setSpotDirection(final GL10 gl, final int index, final LightStateRecord record,
            final float directionX, final float directionY, final float directionZ, final float value) {
        // From OpenGL Docs:
        // The light position is transformed by the contents of the current top
        // of the ModelView matrix stack when you specify the light position
        // with a call to glLightfv(GL_LIGHT_POSITION,...). If you later change
        // the ModelView matrix, such as when the view changes for the next
        // frame, the light position isn't automatically retransformed by the
        // new contents of the ModelView matrix. If you want to update the
        // light's position, you must again specify the light position with a
        // call to glLightfv(GL_LIGHT_POSITION,...).
        record.lightBuffer.clear();
        record.lightBuffer.put(directionX);
        record.lightBuffer.put(directionY);
        record.lightBuffer.put(directionZ);
        record.lightBuffer.put(value);
        record.lightBuffer.flip();
        gl.glLightfv(GL10.GL_LIGHT0 + index, GL10.GL_SPOT_DIRECTION, record.lightBuffer); // TODO Check for float
    }

    private static void setConstant(final GL10 gl, final int index, final float constant, final LightRecord lr,
            final boolean force) {
        if (force || constant != lr.getConstant()) {
            gl.glLightf(GL10.GL_LIGHT0 + index, GL10.GL_CONSTANT_ATTENUATION, constant);
            lr.setConstant(constant);
        }
    }

    private static void setLinear(final GL10 gl, final int index, final float linear, final LightRecord lr,
            final boolean force) {
        if (force || linear != lr.getLinear()) {
            gl.glLightf(GL10.GL_LIGHT0 + index, GL10.GL_LINEAR_ATTENUATION, linear);
            lr.setLinear(linear);
        }
    }

    private static void setQuadratic(final GL10 gl, final int index, final float quad, final LightRecord lr,
            final boolean force) {
        if (force || quad != lr.getQuadratic()) {
            gl.glLightf(GL10.GL_LIGHT0 + index, GL10.GL_QUADRATIC_ATTENUATION, quad);
            lr.setQuadratic(quad);
        }
    }

    private static void setAttenuate(final GL10 gl, final boolean attenuate, final int index, final Light light,
            final LightStateRecord record, final LightRecord lr) {
        if (attenuate) {
            AndroidLightStateUtil.setConstant(gl, index, light.getConstant(), lr, !record.isValid());
            AndroidLightStateUtil.setLinear(gl, index, light.getLinear(), lr, !record.isValid());
            AndroidLightStateUtil.setQuadratic(gl, index, light.getQuadratic(), lr, !record.isValid());
        } else {
            AndroidLightStateUtil.setConstant(gl, index, 1, lr, !record.isValid());
            AndroidLightStateUtil.setLinear(gl, index, 0, lr, !record.isValid());
            AndroidLightStateUtil.setQuadratic(gl, index, 0, lr, !record.isValid());
        }
        lr.setAttenuate(attenuate);
    }

    private static void setSpotExponent(final GL10 gl, final int index, final LightStateRecord record,
            final float exponent, final LightRecord lr) {
        if (!record.isValid() || lr.getSpotExponent() != exponent) {
            gl.glLightf(GL10.GL_LIGHT0 + index, GL10.GL_SPOT_EXPONENT, exponent);
            lr.setSpotExponent(exponent);
        }
    }

    private static void setSpotCutoff(final GL10 gl, final int index, final LightStateRecord record,
            final float cutoff, final LightRecord lr) {
        if (!record.isValid() || lr.getSpotCutoff() != cutoff) {
            gl.glLightf(GL10.GL_LIGHT0 + index, GL10.GL_SPOT_CUTOFF, cutoff);
            lr.setSpotCutoff(cutoff);
        }
    }
}
