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

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.VertexProgramState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.VertexProgramStateRecord;

public abstract class AndroidVertexProgramStateUtil {

    public static void apply(final VertexProgramState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final VertexProgramStateRecord record = (VertexProgramStateRecord) context
                .getStateRecord(StateType.VertexProgram);
        context.setCurrentState(StateType.VertexProgram, state);

        // vertex program is not part of OpenGL ES 1.0 or 1.1...

        if (!record.isValid()) {
            record.validate();
        }
    }
}
