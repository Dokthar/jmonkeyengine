/*
 * Copyright (c) 2009-2015 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.bullet.util;

import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author dokthar
 */
public class NativeSoftBodyUtil {
      /*====================*  
     SoftBody to Mesh - UTILS 
     *====================*/ /*
     Since bullet SoftBody don't use btCollisionShape, its not possible to use the DebugShapeFactory.
     The following code is almost the same , but specially for SoftBody. 
     Theses methods are static (same as in DebugShapeFactory) so the code can be easily moved somewhere else.
     */

    public static Geometry createDebugShape(PhysicsSoftBody softBody) {
        if (softBody == null) {
            return null;
        }
        Geometry debugShape = new Geometry();
        debugShape.setMesh(getDebugMesh(softBody));
        debugShape.updateModelBound();
        debugShape.updateGeometricState();
        return debugShape;
    }

    public static Mesh getDebugMesh(PhysicsSoftBody softBody) {
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, getVertices(softBody));
        mesh.setBuffer(VertexBuffer.Type.Index, 3, getIndexes(softBody));
        mesh.getFloatBuffer(VertexBuffer.Type.Position).clear();
        return mesh;
    }

    private static FloatBuffer getVertices(PhysicsSoftBody softBody) {
        DebugMeshCallback callback = new DebugMeshCallback();
        getVertices(softBody.getObjectId(), callback);
        return callback.getVertices();
    }

    private static native void getVertices(long bodyId, DebugMeshCallback buffer);

    private static IntBuffer getIndexes(PhysicsSoftBody softBody) {
        IntBuffer indexes = BufferUtils.createIntBuffer(getNumTriangle(softBody.getObjectId()) * 3);
        getIndexes(softBody.getObjectId(), indexes);
        return indexes;
    }

    private static native void getIndexes(long bodyId, IntBuffer buffer);

    private static native int getNumTriangle(long bodyId);

    public static void updateMesh(PhysicsSoftBody softBody, Mesh store, boolean meshInLocalOrigin, boolean updateNormals) {
        FloatBuffer positionBuffer = store.getFloatBuffer(VertexBuffer.Type.Position);
        FloatBuffer normalBuffer = store.getFloatBuffer(VertexBuffer.Type.Normal);
        updateMesh(softBody.getObjectId(), positionBuffer, meshInLocalOrigin, updateNormals, normalBuffer);
        store.getBuffer(VertexBuffer.Type.Position).setUpdateNeeded();
        if (updateNormals) {
            store.getBuffer(VertexBuffer.Type.Normal).setUpdateNeeded();
        }
    }

    private static native void updateMesh(long bodyId, FloatBuffer vertices, boolean meshInLocalOrigin, boolean updateNormals, FloatBuffer normals);

}
