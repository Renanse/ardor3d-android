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

import android.util.Log;

import com.ardor3d.framework.android.AndroidCanvas;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.renderer.state.MaterialState.MaterialFace;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.MaterialStateRecord;

public abstract class AndroidMaterialStateUtil {

    public static void apply(final GL10 gl, final MaterialState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final MaterialStateRecord record = (MaterialStateRecord) context.getStateRecord(StateType.Material);
        context.setCurrentState(StateType.Material, state);

        if (state.isEnabled()) {
            // setup colormaterial, if changed.
            AndroidMaterialStateUtil.applyColorMaterial(gl, state.getColorMaterial(), state.getColorMaterialFace(),
                    record);

            // apply colors, if needed and not what is currently set.
            AndroidMaterialStateUtil.applyColor(gl, ColorMaterial.Ambient, state.getAmbient(), state.getBackAmbient(),
                    record);
            AndroidMaterialStateUtil.applyColor(gl, ColorMaterial.Diffuse, state.getDiffuse(), state.getBackDiffuse(),
                    record);
            AndroidMaterialStateUtil.applyColor(gl, ColorMaterial.Emissive, state.getEmissive(), state
                    .getBackEmissive(), record);
            AndroidMaterialStateUtil.applyColor(gl, ColorMaterial.Specular, state.getSpecular(), state
                    .getBackSpecular(), record);

            // set our shine
            AndroidMaterialStateUtil.applyShininess(gl, state.getShininess(), state.getBackShininess(), record);
        } else {
            // apply defaults
            AndroidMaterialStateUtil.applyColorMaterial(gl, MaterialState.DEFAULT_COLOR_MATERIAL,
                    MaterialState.DEFAULT_COLOR_MATERIAL_FACE, record);

            AndroidMaterialStateUtil.applyColor(gl, ColorMaterial.Ambient, MaterialState.DEFAULT_AMBIENT,
                    MaterialState.DEFAULT_AMBIENT, record);
            AndroidMaterialStateUtil.applyColor(gl, ColorMaterial.Diffuse, MaterialState.DEFAULT_DIFFUSE,
                    MaterialState.DEFAULT_DIFFUSE, record);
            AndroidMaterialStateUtil.applyColor(gl, ColorMaterial.Emissive, MaterialState.DEFAULT_EMISSIVE,
                    MaterialState.DEFAULT_EMISSIVE, record);
            AndroidMaterialStateUtil.applyColor(gl, ColorMaterial.Specular, MaterialState.DEFAULT_SPECULAR,
                    MaterialState.DEFAULT_SPECULAR, record);

            AndroidMaterialStateUtil.applyShininess(gl, MaterialState.DEFAULT_SHININESS,
                    MaterialState.DEFAULT_SHININESS, record);
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void applyColor(final GL10 gl, final ColorMaterial glMatColor, final ReadOnlyColorRGBA frontColor,
            final ReadOnlyColorRGBA backColor, final MaterialStateRecord record) {
        final int glMat = AndroidMaterialStateUtil.getGLColorMaterial(glMatColor);
        if (frontColor.equals(backColor)) {
            // consolidate to one call
            if (!AndroidMaterialStateUtil.isVertexProvidedColor(MaterialFace.FrontAndBack, glMatColor, record)) {
                if (!record.isValid() || !record.isSetColor(MaterialFace.FrontAndBack, glMatColor, frontColor, record)) {
                    record.tempColorBuff.clear();
                    record.tempColorBuff.put(frontColor.getRed()).put(frontColor.getGreen()).put(frontColor.getBlue())
                            .put(frontColor.getAlpha());
                    record.tempColorBuff.flip();
                    gl.glMaterialfv(AndroidMaterialStateUtil.getGLMaterialFace(MaterialFace.FrontAndBack), glMat,
                            record.tempColorBuff);
                    record.setColor(MaterialFace.FrontAndBack, glMatColor, frontColor);
                }
            }
        } else {
            if (!AndroidMaterialStateUtil.isVertexProvidedColor(MaterialFace.Front, glMatColor, record)) {
                if (!record.isValid() || !record.isSetColor(MaterialFace.Front, glMatColor, frontColor, record)) {
                    record.tempColorBuff.clear();
                    record.tempColorBuff.put(frontColor.getRed()).put(frontColor.getGreen()).put(frontColor.getBlue())
                            .put(frontColor.getAlpha());
                    record.tempColorBuff.flip();
                    gl.glMaterialfv(AndroidMaterialStateUtil.getGLMaterialFace(MaterialFace.Front), glMat,
                            record.tempColorBuff);
                    record.setColor(MaterialFace.Front, glMatColor, frontColor);
                }
            }

            if (!AndroidMaterialStateUtil.isVertexProvidedColor(MaterialFace.Back, glMatColor, record)) {
                if (!record.isValid() || !record.isSetColor(MaterialFace.Back, glMatColor, backColor, record)) {
                    record.tempColorBuff.clear();
                    record.tempColorBuff.put(backColor.getRed()).put(backColor.getGreen()).put(backColor.getBlue())
                            .put(backColor.getAlpha());
                    record.tempColorBuff.flip();
                    gl.glMaterialfv(AndroidMaterialStateUtil.getGLMaterialFace(MaterialFace.Back), glMat,
                            record.tempColorBuff);
                    record.setColor(MaterialFace.Back, glMatColor, backColor);
                }
            }
        }
    }

    private static boolean isVertexProvidedColor(final MaterialFace face, final ColorMaterial glMatColor,
            final MaterialStateRecord record) {
        if (face != record.colorMaterialFace) {
            return false;
        }
        switch (glMatColor) {
            case Ambient:
                return record.colorMaterial == ColorMaterial.Ambient
                        || record.colorMaterial == ColorMaterial.AmbientAndDiffuse;
            case Diffuse:
                return record.colorMaterial == ColorMaterial.Diffuse
                        || record.colorMaterial == ColorMaterial.AmbientAndDiffuse;
            case Specular:
                return record.colorMaterial == ColorMaterial.Specular;
            case Emissive:
                return record.colorMaterial == ColorMaterial.Emissive;
        }
        return false;
    }

    private static void applyColorMaterial(final GL10 gl, ColorMaterial colorMaterial, MaterialFace face,
            final MaterialStateRecord record) {
        if (!record.isValid() || face != record.colorMaterialFace || colorMaterial != record.colorMaterial) {
            if (colorMaterial == ColorMaterial.None) {
                gl.glDisable(GL10.GL_COLOR_MATERIAL);
            } else {
                if (colorMaterial != ColorMaterial.AmbientAndDiffuse) {
                    Log.w(AndroidCanvas.TAG, "ColorMaterial '" + colorMaterial
                            + "' not supported by this renderer.  Falling back to AmbientAndDiffuse.");
                    colorMaterial = ColorMaterial.AmbientAndDiffuse;
                }

                if (face != MaterialFace.FrontAndBack) {
                    Log.w(AndroidCanvas.TAG, "MaterialFace '" + face
                            + "' not supported by this renderer.  Falling back to FrontAndBack.");
                    face = MaterialFace.FrontAndBack;
                }

                gl.glEnable(GL10.GL_COLOR_MATERIAL);
                record.resetColorsForCM(face, colorMaterial);
            }
            record.colorMaterial = colorMaterial;
            record.colorMaterialFace = face;
        }
    }

    private static void applyShininess(final GL10 gl, final float frontShininess, final float backShininess,
            final MaterialStateRecord record) {
        if (frontShininess == backShininess) {
            // consolidate to one call
            if (!record.isValid() || frontShininess != record.frontShininess || record.backShininess != backShininess) {
                gl.glMaterialf(AndroidMaterialStateUtil.getGLMaterialFace(MaterialFace.FrontAndBack),
                        GL10.GL_SHININESS, frontShininess);
                record.backShininess = record.frontShininess = frontShininess;
            }
        } else {
            if (!record.isValid() || frontShininess != record.frontShininess) {
                gl.glMaterialf(AndroidMaterialStateUtil.getGLMaterialFace(MaterialFace.Front), GL10.GL_SHININESS,
                        frontShininess);
                record.frontShininess = frontShininess;
            }

            if (!record.isValid() || backShininess != record.backShininess) {
                gl.glMaterialf(AndroidMaterialStateUtil.getGLMaterialFace(MaterialFace.Back), GL10.GL_SHININESS,
                        backShininess);
                record.backShininess = backShininess;
            }
        }
    }

    /**
     * Converts the color material setting of this state to a GL constant.
     * 
     * @return the GL constant
     */
    private static int getGLColorMaterial(final ColorMaterial material) {
        switch (material) {
            case AmbientAndDiffuse:
                return GL10.GL_AMBIENT_AND_DIFFUSE;
            case Ambient:
                return GL10.GL_AMBIENT;
            case Diffuse:
                return GL10.GL_DIFFUSE;
            case Emissive:
                return GL10.GL_EMISSION;
            case Specular:
                return GL10.GL_SPECULAR;
        }
        throw new IllegalArgumentException("invalid color material setting: " + material);
    }

    /**
     * Converts the material face setting of this state to a GL constant.
     * 
     * @return the GL constant
     */
    private static int getGLMaterialFace(final MaterialFace face) {
        switch (face) {
            case Front:
            case Back:
                Log.w(AndroidCanvas.TAG, "MaterialFace '" + face
                        + "' not supported by this renderer.  Defaulting to FrontAndBack.");
            case FrontAndBack:
                return GL10.GL_FRONT_AND_BACK;
        }
        throw new IllegalArgumentException("invalid material face setting: " + face);
    }
}
