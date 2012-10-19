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

package com.ardor3d.input.android;

import com.ardor3d.framework.Canvas;
import com.ardor3d.input.ButtonState;
import com.ardor3d.input.Key;
import com.ardor3d.input.KeyboardState;
import com.ardor3d.input.MouseState;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TriggerConditions;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

public class AndroidFirstPersonControl {
    private double _rotateSpeed = 0.002;
    private double _moveSpeed = 20;
    private double _zoomSpeed = -5;

    private final Matrix3 _workerMatrix = new Matrix3();
    private final Vector3 _workerStoreA = new Vector3();
    private final Vector3 _upAxis = new Vector3(Vector3.UNIT_Y);

    public void registerCallbacks(final LogicalLayer lLayer) {
        setupKeyTriggers(lLayer);
        setupMouseTriggers(lLayer);
    }

    protected void setupMouseTriggers(final LogicalLayer lLayer) {
        // Mouse look
        final Predicate<TwoInputStates> someMouseDown = Predicates.or(TriggerConditions.leftButtonDown(),
                TriggerConditions.rightButtonDown());
        final Predicate<TwoInputStates> dragged = Predicates.and(TriggerConditions.mouseMoved(), someMouseDown);
        final TriggerAction dragAction = new TriggerAction() {

            // Test boolean to allow us to ignore first mouse event. First event can wildly vary based on platform.
            private boolean firstPing = true;

            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                // ignore initial press to prevent "hopping"
                if (!inputStates.getPrevious().getMouseState().hasButtonState(ButtonState.DOWN)) {
                    return;
                }

                final MouseState mouse = inputStates.getCurrent().getMouseState();
                if (mouse.getDx() != 0 || mouse.getDy() != 0) {
                    if (!firstPing) {
                        rotate(source.getCanvasRenderer().getCamera(), -mouse.getDx(), -mouse.getDy());
                    } else {
                        firstPing = false;
                    }
                }
            }
        };

        lLayer.registerTrigger(new InputTrigger(dragged, dragAction));

        // pinch / zoom control
        final Predicate<TwoInputStates> pinched = new Predicate<TwoInputStates>() {
            public boolean apply(final TwoInputStates states) {
                return states.getCurrent().getMouseState().getDwheel() != states.getPrevious().getMouseState()
                        .getDwheel();
            }
        };

        final TriggerAction moveAction = new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final Camera camera = source.getCanvasRenderer().getCamera();
                final Vector3 loc = new Vector3(camera.getDirection());
                loc.normalizeLocal().multiplyLocal(
                        _zoomSpeed * tpf * inputStates.getCurrent().getMouseState().getDwheel()).addLocal(
                        camera.getLocation());
                camera.setLocation(loc);
            }
        };

        lLayer.registerTrigger(new InputTrigger(pinched, moveAction));
    }

    protected void setupKeyTriggers(final LogicalLayer lLayer) {

        // WASD control
        final Predicate<TwoInputStates> keysHeld = new Predicate<TwoInputStates>() {
            Key[] keys = new Key[] { Key.LEFT, Key.RIGHT, Key.UP, Key.DOWN };

            public boolean apply(final TwoInputStates states) {
                for (final Key k : keys) {
                    if (states.getCurrent() != null && states.getCurrent().getKeyboardState().isDown(k)) {
                        return true;
                    }
                }
                return false;
            }
        };

        final TriggerAction moveAction = new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                move(source.getCanvasRenderer().getCamera(), inputStates.getCurrent().getKeyboardState(), tpf);
            }
        };
        lLayer.registerTrigger(new InputTrigger(keysHeld, moveAction));
    }

    protected void rotate(final Camera camera, final double dx, final double dy) {

        if (dx != 0) {
            _workerMatrix.fromAngleNormalAxis(_rotateSpeed * dx, _upAxis != null ? _upAxis : camera.getUp());
            _workerMatrix.applyPost(camera.getLeft(), _workerStoreA);
            camera.setLeft(_workerStoreA);
            _workerMatrix.applyPost(camera.getDirection(), _workerStoreA);
            camera.setDirection(_workerStoreA);
            _workerMatrix.applyPost(camera.getUp(), _workerStoreA);
            camera.setUp(_workerStoreA);
        }

        if (dy != 0) {
            _workerMatrix.fromAngleNormalAxis(_rotateSpeed * dy, camera.getLeft());
            _workerMatrix.applyPost(camera.getLeft(), _workerStoreA);
            camera.setLeft(_workerStoreA);
            _workerMatrix.applyPost(camera.getDirection(), _workerStoreA);
            camera.setDirection(_workerStoreA);
            _workerMatrix.applyPost(camera.getUp(), _workerStoreA);
            camera.setUp(_workerStoreA);
        }

        camera.normalize();
    }

    protected void move(final Camera camera, final KeyboardState kb, final double tpf) {
        // MOVEMENT
        int moveFB = 0, strafeLR = 0;
        if (kb.isDown(Key.UP)) {
            moveFB += 1;
        }
        if (kb.isDown(Key.DOWN)) {
            moveFB -= 1;
        }
        if (kb.isDown(Key.LEFT)) {
            strafeLR += 1;
        }
        if (kb.isDown(Key.RIGHT)) {
            strafeLR -= 1;
        }

        if (moveFB != 0 || strafeLR != 0) {
            final Vector3 loc = _workerStoreA.zero();
            if (moveFB == 1) {
                loc.addLocal(camera.getDirection());
            } else if (moveFB == -1) {
                loc.subtractLocal(camera.getDirection());
            }
            if (strafeLR == 1) {
                loc.addLocal(camera.getLeft());
            } else if (strafeLR == -1) {
                loc.subtractLocal(camera.getLeft());
            }
            loc.normalizeLocal().multiplyLocal(_moveSpeed * tpf).addLocal(camera.getLocation());
            camera.setLocation(loc);
        }
    }

    public double getMoveSpeed() {
        return _moveSpeed;
    }

    public void setMoveSpeed(final double speed) {
        _moveSpeed = speed;
    }

    public double getZoomSpeed() {
        return _zoomSpeed;
    }

    public void setZoomSpeed(final double speed) {
        _zoomSpeed = speed;
    }

    public double getRotateSpeed() {
        return _rotateSpeed;
    }

    public void setRotateSpeed(final double rotateSpeed) {
        _rotateSpeed = rotateSpeed;
    }

    public void setUpAxis(final ReadOnlyVector3 upAxis) {
        _upAxis.set(upAxis);
    }

    public ReadOnlyVector3 getUpAxis() {
        return new Vector3(_upAxis);
    }
}
