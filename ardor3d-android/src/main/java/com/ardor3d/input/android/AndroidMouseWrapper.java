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

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Set;

import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;

import com.ardor3d.input.ButtonState;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.MouseState;
import com.ardor3d.input.MouseWrapper;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;

/**
 * TODO: Implement multitouch in a more meaningful way.
 */
public class AndroidMouseWrapper implements MouseWrapper {
    private MouseState _lastState = null;
    private final LinkedList<MouseState> _upcomingEvents = new LinkedList<MouseState>();

    private AndroidMouseIterator _currentIterator = null;
    private final View _view;
    private float _oldDistance = 0;

    public AndroidMouseWrapper(final View view) {
        _view = view;
    }

    public void init() {
        ; // nothing to do
    }

    public PeekingIterator<MouseState> getEvents() {
        // only create a new iterator if there isn't an existing, valid, one.
        if (_currentIterator == null || !_currentIterator.hasNext()) {
            _currentIterator = new AndroidMouseIterator();
        }

        return _currentIterator;
    }

    public void onTouchEvent(final MotionEvent event) {
        synchronized (this) {
            if (_lastState == null) {
                _lastState = new MouseState(Math.round(event.getX()), getArdor3DY(event), 0, 0, 0, null, null);
            }

            final int type = event.getAction();
            final EnumMap<MouseButton, ButtonState> buttons = _lastState.getButtonStates();

            // check for "touch down" event
            if (type == MotionEvent.ACTION_DOWN) {
                for (int index = 0; index < event.getPointerCount(); index++) {
                    final MouseButton button = getButtonForPointerIndex(event.getPointerId(index));
                    buttons.put(button, ButtonState.DOWN);
                }
                addNewState(event, buttons);
            } else if (type == MotionEvent.ACTION_UP || type == MotionEvent.ACTION_CANCEL) {
                final Set<MouseButton> buttonList = buttons.keySet();
                for (final MouseButton b : buttonList) {
                    buttons.put(b, ButtonState.UP);
                }
                addNewState(event, buttons);
            } else if ((type & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN) {
                final int pointIndex = (type & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final MouseButton button = getButtonForPointerIndex(pointIndex);
                buttons.put(button, ButtonState.DOWN);
                addNewState(event, buttons);
            } else if ((type & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP) {
                final int pointIndex = (type & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final MouseButton button = getButtonForPointerIndex(pointIndex);
                buttons.put(button, ButtonState.UP);
                addNewState(event, buttons);
                _oldDistance = 0;
            }
            // Only add if we've actually moved.
            else if (getDX(event) != 0 || getDY(event) != 0) {
                addNewState(event, _lastState.getButtonStates());
            }
        }
    }

    /**
     * @param e
     *            our motion event
     * @return the Y coordinate of the event, flipped relative to the component since we expect an origin in the lower
     *         left corner.
     */
    private int getArdor3DY(final MotionEvent e) {
        final int height = _view.getHeight();
        return height - Math.round(e.getY());
    }

    private MouseButton getButtonForPointerIndex(final int pointerId) {
        switch (pointerId) {
            case 0:
                return MouseButton.LEFT;
            case 1:
                return MouseButton.RIGHT;
            case 2:
                return MouseButton.MIDDLE;
        }
        return null;
    }

    private class AndroidMouseIterator extends AbstractIterator<MouseState> implements PeekingIterator<MouseState> {
        @Override
        protected MouseState computeNext() {
            synchronized (AndroidMouseWrapper.this) {
                if (_upcomingEvents.isEmpty()) {
                    return endOfData();
                }

                return _upcomingEvents.poll();
            }

        }
    }

    private void addNewState(final MotionEvent event, final EnumMap<MouseButton, ButtonState> enumMap) {
        final MouseState newState;
        if (event.getPointerCount() > 1) {
            // We have a gesture, assume pinch-to-zoom
            newState = new MouseState(Math.round(event.getX()), getArdor3DY(event), 0, 0, (int) getWheel(event),
                    enumMap, null);
        } else {
            newState = new MouseState(Math.round(event.getX()), getArdor3DY(event), getDX(event), getDY(event), 0,
                    enumMap, null);
        }
        _upcomingEvents.add(newState);
        _lastState = newState;
    }

    private int getDX(final MotionEvent event) {
        return Math.round(event.getX()) - _lastState.getX();
    }

    private int getDY(final MotionEvent event) {
        return getArdor3DY(event) - _lastState.getY();
    }

    private float getWheel(final MotionEvent event) {
        // Only called when event.GetPointerCount()>1
        float wheel = 0;
        if (_oldDistance != 0) {
            wheel = _oldDistance - AndroidMouseWrapper.spacing(event);
        }
        _oldDistance = AndroidMouseWrapper.spacing(event);

        return wheel;
    }

    /* Utility Methods */
    public static final float spacing(final MotionEvent event) {
        final float x = event.getX(0) - event.getX(1);
        final float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }
}
