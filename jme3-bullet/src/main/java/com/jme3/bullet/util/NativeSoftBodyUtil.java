/*
 * Copyright (c) 2009-2016 jMonkeyEngine
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
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bullet SoftBody don't use btCollisionShape, its not possible to use the
 * DebugShapeFactory. The following code is almost the same , but specially for
 * SoftBody.
 *
 * @author dokthar
 */
public class NativeSoftBodyUtil {

    // SoftBodies need to have a direct access to JME's Mesh buffer in order a have a more
    // efficient way when updating the Mesh each frame
    // Must remove "duplicated" vertex (vertex with same position, hapen with uv mapping) to prevent the mesh from tearing apart.
    public static IntBuffer generateIndexMap(FloatBuffer jmePositions) {
        /*
         * Exemple :
         * mesh :   P1\P3----P4
         *            ¦¯\_   ¦
         *            ¦   ¯\_¦
         *           P2----P6\P5
         *
         * with P1==P3 and P6==P5
         * Buffers indexes        :  | 0| 1| 2| 3| 4| 5|
         * >-  JME PositionBuffer :  [P1,P2,P3,P4,P5,P6] 
         * >-  JME IndexBuffer    :  [ 0, 1, 5, 2, 4, 3]
         * <-> JME -> Bullet map  :  [ 0, 1, 0, 2, 3, 3]
         *  -> Bullet Positions   :  [P3,P2,P4,P6]       == [P1,P2,P4,P5]
         *  -> Bullet Index       :  [ 0, 1, 3, 0, 3, 2]
         */
        int jmePositionSize = jmePositions.capacity();

        IntBuffer jmeToBulletMap = BufferUtils.createIntBuffer(jmePositionSize / 3);
        Map<Vector3f, Integer> uniquePositions = new HashMap<Vector3f, Integer>();

        for (int i = 0, indice = 0; i < jmePositionSize; i += 3) {
            float x = jmePositions.get(i + 0);
            float y = jmePositions.get(i + 1);
            float z = jmePositions.get(i + 2);
            Vector3f p = new Vector3f(x, y, z);
            if (!uniquePositions.containsKey(p)) {
                uniquePositions.put(p, indice);
                jmeToBulletMap.put(indice);
                indice++;
            } else {
                jmeToBulletMap.put(uniquePositions.get(p));
            }
        }
        // TODO : is rewind really necessary ?
        jmeToBulletMap.rewind();

        return jmeToBulletMap;
    }

    public static void apply(IntBuffer map, IndexBuffer inBuffer, IndexBuffer outBuffer) {
        int size = inBuffer.size();
        for (int i = 0; i < size; i++) {
            outBuffer.put(i, map.get(inBuffer.get(i)));
        }
    }

    public static IndexBuffer cloneApply(IntBuffer map, IndexBuffer buffer) {
        IndexBuffer ib = IndexBuffer.wrapIndexBuffer(BufferUtils.clone(buffer.getBuffer()));
        apply(map, buffer, ib);
        return ib;
    }

    public static FloatBuffer apply(IntBuffer jme2BulletMap, FloatBuffer positions) {
        int positionSize = positions.limit();
        FloatBuffer bulletPositions = BufferUtils.createFloatBuffer(positionSize);
        for (int i = 0; i < positionSize / 3; i++) {
            // create the bullet positions, do the conversion from JME -> Bullet (not 1:1, with some overwrite)
            int bulletIndex = jme2BulletMap.get(i);
            bulletPositions.put(bulletIndex * 3 + 0, positions.get(i * 3 + 0));
            bulletPositions.put(bulletIndex * 3 + 1, positions.get(i * 3 + 1));
            bulletPositions.put(bulletIndex * 3 + 2, positions.get(i * 3 + 2));
        }

        return bulletPositions;
    }

    public static <T extends PhysicsSoftBody> T createRopeFromMesh(Mesh mesh, T emptySoftBody) {
        emptySoftBody.createSoftBody(mesh.getFloatBuffer(VertexBuffer.Type.Position),
                mesh.getIndexBuffer(), null, null);
        return emptySoftBody;
    }

    public static <T extends PhysicsSoftBody> T createFromTriMesh(Mesh mesh, T emptySoftBody) {
        FloatBuffer position = mesh.getFloatBuffer(VertexBuffer.Type.Position);
        IndexBuffer triangles = mesh.getIndexBuffer();
        Set<Edge> uniqueEdges = new HashSet<Edge>();

        for (int i = 0, size = triangles.size(); i < size; i += 3) {
            int t0 = triangles.get(i);
            int t1 = triangles.get(i + 1);
            int t2 = triangles.get(i + 2);

            // t0 -- t1
            if (t0 < t1) {
                uniqueEdges.add(new Edge(t0, t1));
            } else {
                uniqueEdges.add(new Edge(t1, t0));
            }
            // t1 -- t2
            if (t1 < t2) {
                uniqueEdges.add(new Edge(t1, t2));
            } else {
                uniqueEdges.add(new Edge(t2, t1));
            }
            // t2 -- t0
            if (t2 < t0) {
                uniqueEdges.add(new Edge(t2, t0));
            } else {
                uniqueEdges.add(new Edge(t0, t2));
            }
        }

        IndexBuffer links = IndexBuffer.createIndexBuffer(position.capacity(), uniqueEdges.size() * 2);
        int index = 0;
        for (Edge e : uniqueEdges) {
            links.put(index++, e.index1);
            links.put(index++, e.index2);
        }

        emptySoftBody.createSoftBody(position, links, triangles, null);
        return emptySoftBody;
    }

    public static void updateMesh(PhysicsSoftBody body, Mesh store, boolean meshInLocalSpace, boolean updateNormals) {
        updateMesh(body, null, store, meshInLocalSpace, updateNormals);
    }

    public static void updateMesh(PhysicsSoftBody body, IntBuffer jmeToBulletMap, Mesh store, boolean meshInLocalSpace, boolean updateNormals) {
        FloatBuffer positionBuffer = store.getFloatBuffer(VertexBuffer.Type.Position);
        FloatBuffer normalBuffer = store.getFloatBuffer(VertexBuffer.Type.Normal);
        if (jmeToBulletMap != null) {
            updateMesh(body.getObjectId(), jmeToBulletMap, positionBuffer, normalBuffer, meshInLocalSpace, updateNormals && normalBuffer != null);
        } else {
            updateMesh(body.getObjectId(), positionBuffer, normalBuffer, meshInLocalSpace, updateNormals && normalBuffer != null);
        }
        store.getBuffer(VertexBuffer.Type.Position).setUpdateNeeded();
        if (updateNormals && normalBuffer != null) {
            store.getBuffer(VertexBuffer.Type.Normal).setUpdateNeeded();
        }
    }

    private static native void updateMesh(long bodyId, IntBuffer inIndexMapping, FloatBuffer outPositionBuffer, FloatBuffer outNormalBuffer, boolean meshInLocalSpace, boolean updateNormals);

    private static native void updateMesh(long bodyId, FloatBuffer outPositionBuffer, FloatBuffer outNormalBuffer, boolean meshInLocalSpace, boolean updateNormals);

    /**
     * Utility class for createFromTriMesh
     */
    private static final class Edge {

        private final int index1, index2;

        private Edge(int indexA, int indexB) {
            index1 = indexA;
            index2 = indexB;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof Edge) {
                Edge e = (Edge) o;
                return e.index1 == this.index1 && e.index2 == this.index2;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + this.index1;
            hash = 23 * hash + this.index2;
            return hash;
        }
    }
}
