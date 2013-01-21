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

import java.util.EnumSet;
import java.util.LinkedList;

import android.util.Log;

import com.ardor3d.framework.android.AndroidCanvas;
import com.ardor3d.input.Key;
import com.ardor3d.input.KeyEvent;
import com.ardor3d.input.KeyState;
import com.ardor3d.input.KeyboardWrapper;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;

public class AndroidKeyWrapper implements KeyboardWrapper {
    private final LinkedList<KeyEvent> _upcomingEvents = new LinkedList<KeyEvent>();
    private AndroidKeyboardIterator _currentIterator = null;
    private final EnumSet<Key> _pressedList = EnumSet.noneOf(Key.class);

    public void init() {
        ; // nothing to do
    }

    public PeekingIterator<KeyEvent> getEvents() {
        if (_currentIterator == null || !_currentIterator.hasNext()) {
            _currentIterator = new AndroidKeyboardIterator();
        }

        return _currentIterator;
    }

    public synchronized void keyPressed(final android.view.KeyEvent e) {
        final Key pressed = AndroidKey.findByCode(e.getKeyCode());
        if (pressed != null) {
            if (!_pressedList.contains(pressed)) {
                _upcomingEvents.add(new KeyEvent(pressed, KeyState.DOWN, (char) e.getUnicodeChar()));
                _pressedList.add(pressed);
            }
        } else {
            Log.w(AndroidCanvas.TAG, "AndroidKeyWrapper.keyPressed - key not found " + (char) e.getUnicodeChar());
        }
    }

    public synchronized void keyReleased(final android.view.KeyEvent e) {
        final Key released = AndroidKey.findByCode(e.getKeyCode());
        if (released != null) {
            _upcomingEvents.add(new KeyEvent(released, KeyState.UP, (char) e.getUnicodeChar()));
            _pressedList.remove(released);
        } else {
            Log.w(AndroidCanvas.TAG, "AndroidKeyWrapper.keyReleased - key not found " + (char) e.getUnicodeChar());
        }
    }

    private class AndroidKeyboardIterator extends AbstractIterator<KeyEvent> implements PeekingIterator<KeyEvent> {
        @Override
        protected KeyEvent computeNext() {
            synchronized (AndroidKeyboardIterator.this) {
                if (_upcomingEvents.isEmpty()) {
                    return endOfData();
                }
                return _upcomingEvents.poll();
            }
        }
    }
}
