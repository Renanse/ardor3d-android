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

import android.view.KeyEvent;

import com.ardor3d.input.Key;

public enum AndroidKey {

    ZERO(KeyEvent.KEYCODE_0, Key.ZERO), //
    ONE(KeyEvent.KEYCODE_1, Key.ONE), //
    TWO(KeyEvent.KEYCODE_2, Key.TWO), //
    THREE(KeyEvent.KEYCODE_3, Key.THREE), //
    FOUR(KeyEvent.KEYCODE_4, Key.FOUR), //
    FIVE(KeyEvent.KEYCODE_5, Key.FIVE), //
    SIX(KeyEvent.KEYCODE_6, Key.SIX), //
    SEVEN(KeyEvent.KEYCODE_7, Key.SEVEN), //
    EIGHT(KeyEvent.KEYCODE_8, Key.EIGHT), //
    NINE(KeyEvent.KEYCODE_9, Key.NINE), //
    A(KeyEvent.KEYCODE_A, Key.A), //
    LMENU(KeyEvent.KEYCODE_ALT_LEFT, Key.LMENU), //
    RMENU(KeyEvent.KEYCODE_ALT_RIGHT, Key.RMENU), //
    APOSTROPHE(KeyEvent.KEYCODE_APOSTROPHE, Key.APOSTROPHE), //
    AT(KeyEvent.KEYCODE_AT, Key.AT), //
    B(KeyEvent.KEYCODE_B, Key.B), //
    BACK(KeyEvent.KEYCODE_BACK, Key.BACK), //
    BACKSLASH(KeyEvent.KEYCODE_BACKSLASH, Key.BACKSLASH), //
    C(KeyEvent.KEYCODE_C, Key.C), //
    CALL(KeyEvent.KEYCODE_CALL, Key.CALL), //
    CAMERA(KeyEvent.KEYCODE_CAMERA, Key.CAMERA), //
    CLEAR(KeyEvent.KEYCODE_CLEAR, Key.CLEAR), //
    COMMA(KeyEvent.KEYCODE_COMMA, Key.COMMA), //
    D(KeyEvent.KEYCODE_D, Key.D), //
    DELETE(KeyEvent.KEYCODE_DEL, Key.DELETE), //
    CENTER(KeyEvent.KEYCODE_DPAD_CENTER, Key.CENTER), //
    DOWN(KeyEvent.KEYCODE_DPAD_DOWN, Key.DOWN), //
    LEFT(KeyEvent.KEYCODE_DPAD_LEFT, Key.LEFT), //
    RIGHT(KeyEvent.KEYCODE_DPAD_RIGHT, Key.RIGHT), //
    UP(KeyEvent.KEYCODE_DPAD_UP, Key.UP), //
    E(KeyEvent.KEYCODE_E, Key.E), //
    ENDCALL(KeyEvent.KEYCODE_ENDCALL, Key.ENDCALL), //
    RETURN(KeyEvent.KEYCODE_ENTER, Key.RETURN), //
    ENVELOPE(KeyEvent.KEYCODE_ENVELOPE, Key.ENVELOPE), //
    EQUALS(KeyEvent.KEYCODE_EQUALS, Key.EQUALS), //
    EXPLORER(KeyEvent.KEYCODE_EXPLORER, Key.EXPLORER), //
    F(KeyEvent.KEYCODE_F, Key.F), //
    FOCUS(KeyEvent.KEYCODE_FOCUS, Key.FOCUS), //
    G(KeyEvent.KEYCODE_G, Key.G), //
    GRAVE(KeyEvent.KEYCODE_GRAVE, Key.GRAVE), //
    H(KeyEvent.KEYCODE_H, Key.H), //
    HEADSETHOOK(KeyEvent.KEYCODE_HEADSETHOOK, Key.HEADSETHOOK), //
    HOME(KeyEvent.KEYCODE_HOME, Key.HOME), //
    I(KeyEvent.KEYCODE_I, Key.I), //
    J(KeyEvent.KEYCODE_J, Key.J), //
    K(KeyEvent.KEYCODE_K, Key.K), //
    L(KeyEvent.KEYCODE_L, Key.L), //
    LBRACKET(KeyEvent.KEYCODE_LEFT_BRACKET, Key.LBRACKET), //
    M(KeyEvent.KEYCODE_M, Key.M), //
    MEDIA_FAST_FORWARD(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, Key.MEDIA_FAST_FORWARD), //
    MEDIA_NEXT(KeyEvent.KEYCODE_MEDIA_NEXT, Key.MEDIA_NEXT), //
    PLAY_PAUSE(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, Key.PLAY_PAUSE), //
    MEDIA_PREVIOUS(KeyEvent.KEYCODE_MEDIA_PREVIOUS, Key.MEDIA_PREVIOUS), //
    MEDIA_REWIND(KeyEvent.KEYCODE_MEDIA_REWIND, Key.MEDIA_REWIND), //
    MEDIA_STOP(KeyEvent.KEYCODE_MEDIA_STOP, Key.MEDIA_STOP), //
    MENU(KeyEvent.KEYCODE_MENU, Key.MENU), //
    MINUS(KeyEvent.KEYCODE_MINUS, Key.MINUS), //
    MUTE(KeyEvent.KEYCODE_MUTE, Key.MUTE), //
    N(KeyEvent.KEYCODE_N, Key.N), //
    NOTIFICATION(KeyEvent.KEYCODE_NOTIFICATION, Key.NOTIFICATION), //
    NUM(KeyEvent.KEYCODE_NUM, Key.NUMLOCK), //
    O(KeyEvent.KEYCODE_O, Key.O), //
    P(KeyEvent.KEYCODE_P, Key.P), //
    PERIOD(KeyEvent.KEYCODE_PERIOD, Key.PERIOD), //
    PLUS(KeyEvent.KEYCODE_PLUS, Key.PLUS), //
    POUND(KeyEvent.KEYCODE_POUND, Key.POUND), //
    POWER(KeyEvent.KEYCODE_POWER, Key.POWER), //
    Q(KeyEvent.KEYCODE_Q, Key.Q), //
    R(KeyEvent.KEYCODE_R, Key.R), //
    RBRACKET(KeyEvent.KEYCODE_RIGHT_BRACKET, Key.RBRACKET), //
    S(KeyEvent.KEYCODE_S, Key.S), //
    SEARCH(KeyEvent.KEYCODE_SEARCH, Key.SEARCH), //
    SEMICOLON(KeyEvent.KEYCODE_SEMICOLON, Key.SEMICOLON), //
    LSHIFT(KeyEvent.KEYCODE_SHIFT_LEFT, Key.LSHIFT), //
    RSHIFT(KeyEvent.KEYCODE_SHIFT_RIGHT, Key.RSHIFT), //
    SLASH(KeyEvent.KEYCODE_SLASH, Key.SLASH), //
    SPACE(KeyEvent.KEYCODE_SPACE, Key.SPACE), //
    STAR(KeyEvent.KEYCODE_STAR, Key.STAR), //
    SYM(KeyEvent.KEYCODE_SYM, Key.SYM), //
    T(KeyEvent.KEYCODE_T, Key.T), //
    TAB(KeyEvent.KEYCODE_TAB, Key.TAB), //
    U(KeyEvent.KEYCODE_U, Key.U), //
    UNKNOWN(KeyEvent.KEYCODE_UNKNOWN, Key.UNKNOWN), //
    V(KeyEvent.KEYCODE_V, Key.V), //
    VOULME_DOWN(KeyEvent.KEYCODE_VOLUME_DOWN, Key.VOLUME_DOWN), //
    VOLUME_UP(KeyEvent.KEYCODE_VOLUME_UP, Key.VOLUME_UP), //
    W(KeyEvent.KEYCODE_W, Key.W), //
    X(KeyEvent.KEYCODE_X, Key.X), //
    Y(KeyEvent.KEYCODE_Y, Key.Y), //
    Z(KeyEvent.KEYCODE_Z, Key.Z); //

    private final int _androidCode;
    private final Key _key;

    private AndroidKey(final int androidCode, final Key key) {
        _androidCode = androidCode;
        _key = key;
    }

    public static Key findByCode(final int keyCode) {
        for (final AndroidKey ak : AndroidKey.values()) {
            if (ak._androidCode == keyCode) {
                return ak._key;
            }
        }

        return null;
    }

    public int getAndroidCode() {
        return _androidCode;
    }
}
