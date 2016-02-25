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
package com.jme3.bullet.objects;

import com.jme3.bullet.PhysicsSoftSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.joints.SoftPhysicsJoint;
import com.jme3.bullet.objects.infos.SoftBodyWorldInfo;
import com.jme3.bullet.util.NativeSoftBodyUtil;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.scene.mesh.IndexIntBuffer;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dokthar
 */
public class PhysicsSoftBody extends PhysicsCollisionObject implements Savable {

    private final Config config = new Config(this);
    private Material material = null;
    private IntBuffer jmeToBulletMap;
    private boolean ropeMesh = false;
    protected ArrayList<SoftPhysicsJoint> joints = new ArrayList<SoftPhysicsJoint>();

    public PhysicsSoftBody() {
    }

    public PhysicsSoftBody(Mesh mesh) {
        createFromMesh(mesh);
    }

    protected final void createFromMesh(Mesh mesh) {
        if (mesh.getMode() == Mesh.Mode.Triangles) {
            createFromTriMesh(mesh);
        } else if (mesh.getMode() == Mesh.Mode.Lines) {
            createRope(mesh);
        }
        postRebuild(false);
    }

    // SoftBodies need to have a direct access to JME's Mesh buffer in order a have a more
    // efficient way when updating the Mesh each frame
    // Must remove "duplicated" vertex (vertex with same position, hapen with uv mapping) to prevent the mesh from tearing apart.
    private long createFromTriMesh(Mesh triMesh) {
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
        FloatBuffer jmePositions = triMesh.getFloatBuffer(Type.Position);
        IndexBuffer jmeIndex = triMesh.getIndexBuffer();

        // Generation of jmeToBulletMap, mapping JME index to Bullet index.
        int jmePositionSize = jmePositions.capacity();
        jmeToBulletMap = BufferUtils.createIntBuffer(jmePositionSize / 3);
        HashMap<Vector3f, Integer> uniquePositions = new HashMap<Vector3f, Integer>();
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
        jmeToBulletMap.rewind();
        jmePositions.rewind();

        // Generation of bullet indexes with the help of jmeToBulletMap
        int jmeIndexSize = jmeIndex.size();
        IntBuffer bulletIndexBuffer = BufferUtils.createIntBuffer(jmeIndexSize);
        for (int i = 0; i < jmeIndexSize; i += 3) {
            // create the bullet index to recreate each face with bullet position
            bulletIndexBuffer.put(jmeToBulletMap.get(jmeIndex.get(i + 0)));
            bulletIndexBuffer.put(jmeToBulletMap.get(jmeIndex.get(i + 1)));
            bulletIndexBuffer.put(jmeToBulletMap.get(jmeIndex.get(i + 2)));
        }

        bulletIndexBuffer.rewind();

        // Generation of bullet position with the help of jmeToBulletMap
        int bulletNbPosition = uniquePositions.size() * 3;
        FloatBuffer bulletPositions = BufferUtils.createFloatBuffer(bulletNbPosition);
        for (int i = 0; i < jmePositionSize / 3; i++) {
            // create the bullet positions, do the conversion from JME -> Bullet (not 1:1, with some overwrite)
            int iBullet = jmeToBulletMap.get(i);
            bulletPositions.put(iBullet * 3 + 0, jmePositions.get(i * 3 + 0));
            bulletPositions.put(iBullet * 3 + 1, jmePositions.get(i * 3 + 1));
            bulletPositions.put(iBullet * 3 + 2, jmePositions.get(i * 3 + 2));
        }

        objectId = createFromTriMesh(bulletIndexBuffer, bulletPositions, jmeIndexSize / 3, false);
        return objectId;
    }

    private native long createFromTriMesh(IntBuffer triangles, FloatBuffer vertices, int numTriangles, boolean randomizeConstraints);

    private long createRope(Mesh mesh) {
        FloatBuffer jmePositions = mesh.getFloatBuffer(Type.Position);
        IndexBuffer jmeIndex = mesh.getIndexBuffer();

        int jmePositionSize = jmePositions.capacity();

        //set a 1:1 map
        jmeToBulletMap = BufferUtils.createIntBuffer(jmePositionSize / 3);
        for (int i = 0; i < (jmePositionSize / 3); i++) {
            jmeToBulletMap.put(i);
        }
        jmeToBulletMap.rewind();

        int indexSize = jmeIndex.size();
        IntBuffer bulletIndex = BufferUtils.createIntBuffer(indexSize);
        for (int i = 0; i < indexSize; i++) {
            bulletIndex.put(jmeIndex.get(i));
        }
        objectId = createRope(bulletIndex, jmePositions);
        ropeMesh = true;

        return objectId;
    }

    private native long createRope(IntBuffer lines, FloatBuffer vertices);

    public boolean isRope() {
        return ropeMesh;
    }

    public void rebuildFromMesh(Mesh mesh) {
        // {} => {old Native object is removed & destroyed; new Native object is created & added}
        if (mesh != null) {
            if (objectId != 0) {
                boolean wasInWorld = isInWorld();
                preRebuild();
                createFromMesh(mesh);
                postRebuild(wasInWorld);
            } else {
                createFromMesh(mesh);
            }
        }
    }

    protected final void preRebuild() {
        // {} = > {remove the body from the physics space and detroy the native object}

        /* if (collisionShape instanceof MeshCollisionShape && mass != 0) {
         throw new IllegalStateException("Dynamic rigidbody can not have mesh collision shape!");
         }*/
        if (objectId != 0) {
            if (isInWorld()) {
                PhysicsSoftSpace.getPhysicsSoftSpace().remove(this);
            }
            Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Clearing RigidBody {0}", Long.toHexString(objectId));
            finalizeNative(objectId);
        }
    }

    protected final void postRebuild(boolean wasInWorld) {
        // {} => {initUserPoint on the native object, if this wasInWorld add this into the physicsSpace}
        initUserPointer();
        if (wasInWorld) {
            PhysicsSoftSpace.getPhysicsSoftSpace().add(this);
        }
    }

    private void initDefault() {
        initDefault(objectId);
    }

    private native void initDefault(long objectId);

    /**
     * Set the SoftBodyWorldInfo of this physics softbody. This is automatically
     * called when the softbody is added into a SoftPhysicsSpace.
     *
     * @param worldinfo , the world info to set.
     */
    public void setSoftBodyWorldInfo(SoftBodyWorldInfo worldinfo) {
        setSoftBodyWorldInfo(objectId, worldinfo.getWorldInfoId());
    }

    private native void setSoftBodyWorldInfo(long objectId, long worldinfoId);

    /**
     * Return the SoftBodyWorldInfo used by this softbody. By default this
     * SoftBodyWorld info is shared by all softBodies into the SoftPhysicsSpace.
     *
     * @return the used SoftBodyWorldInfo
     */
    public SoftBodyWorldInfo getSoftBodyWorldInfo() {
        long worldInfoId = getSoftBodyWorldInfo(objectId);
        SoftBodyWorldInfo worldInfo = new SoftBodyWorldInfo(worldInfoId); // <-point on the same native object
        return worldInfo;
    }

    private native long getSoftBodyWorldInfo(long objectId);

    /**
     * Get the material of this softbody.
     *
     * @return the body Material
     */
    public Material material() {
        if (material == null) {
            material = new Material(this);
        }
        return material;
    }

    private native long getMaterial(long bodyId);


    /* Append anchor */
    public void appendAnchor(int node, PhysicsRigidBody rigidBody, boolean collisionBetweenLinkedBodies, float influence) {
        appendAnchor(objectId, node, rigidBody.getObjectId(), null, collisionBetweenLinkedBodies, influence);
    }

    public void appendAnchor(int node, PhysicsRigidBody rigidBody, Vector3f localPivot, boolean collisionBetweenLinkedBodies, float influence) {
        appendAnchor(objectId, node, rigidBody.getObjectId(), localPivot, collisionBetweenLinkedBodies, influence);
    }

    public void appendAnchor(int node, PhysicsRigidBody rigidBody) {
        appendAnchor(node, rigidBody, true, 1);
    }

    private native void appendAnchor(long bodyId, int node, long rigidId, Vector3f localPivot, boolean collisionBetweenLinkedBodies, float influence);

    public void removeAnchor(int node, PhysicsRigidBody rigidBody) {
        removeAnchor(objectId, node, rigidBody.getObjectId());
    }
    
    private native void removeAnchor(long bodyId, int node, long rigidId);
    
    public void addJoint(SoftPhysicsJoint joint) {
        if (!joints.contains(joint)) {
            joints.add(joint);
        }
    }

    public void removeJoint(SoftPhysicsJoint joint) {
        joints.remove(joint);
    }

    public List<SoftPhysicsJoint> getJoints() {
        return joints;
    }

    /**
     * Add force (or gravity) to the entire body.
     *
     * @param force the force to add.
     */
    public void addForce(Vector3f force) {
        addForce(objectId, force);
    }

    private native void addForce(long objectId, Vector3f force);

    /**
     * Add force (or gravity) to specific node (vertex) of the body.
     *
     * @param force the force to add.
     * @param node the vertex index
     */
    public void addForce(Vector3f force, int node) {
        addForce(objectId, force, node);
    }

    private native void addForce(long objectId, Vector3f force, int node);

    /**
     * Add aero force to specific node (vertex) of the body.
     *
     * @param windVelocity the aero force to add.
     * @param nodeIndex the vertex index
     */
    public void addAeroForceToNode(Vector3f windVelocity, int nodeIndex) {
        addAeroForceToNode(objectId, windVelocity, nodeIndex);
    }

    private native void addAeroForceToNode(long objectId, Vector3f windVelocity, int nodeIndex);

    /**
     * Add velocity to the entire body.
     *
     * @param velocity the value to add.
     */
    public void addVelocity(Vector3f velocity) {
        addVelocity(objectId, velocity);
    }

    private native void addVelocity(long objectId, Vector3f velocity);

    /**
     * Set velocity for the entire body
     *
     * @param velocity the value to set.
     */
    public void setVelocity(Vector3f velocity) {
        setVelocity(objectId, velocity);
    }

    private native void setVelocity(long objectId, Vector3f velocity);

    /**
     * Add velocity to a node (vertex) of the body.
     *
     * @param velocity the value to add.
     * @param node the vertex index.
     */
    public void addVelocity(Vector3f velocity, int node) {
        addVelocity(objectId, velocity, node);
    }

    private native void addVelocity(long objectId, Vector3f velocity, int nodeId);

    /**
     * Set the mass for a given node (vertex) of the body.
     *
     * @param node
     * @param mass
     */
    public void setMass(int node, float mass) {
        setMass(objectId, node, mass);
    }

    private native void setMass(long objectId, int node, float mass);

    /**
     * Get the mass of a specific node (vertex).
     *
     * @param node the vertex index
     * @return the mass of the node
     */
    public float getMass(int node) {
        return getMass(objectId, node);
    }

    private native float getMass(long objectId, int node);

    public void setMasses(FloatBuffer masses) {
        setMasses(objectId, masses);
    }

    private native void setMasses(long objectId, FloatBuffer masses);

    public FloatBuffer getMasses() {
        return getMasses(null);
    }

    public FloatBuffer getMasses(FloatBuffer store) {
        if (store == null) {
            int nbVertex = NativeSoftBodyUtil.getNbVertices(objectId);
            store = BufferUtils.createFloatBuffer(nbVertex);
        }
        getMasses(objectId, store);
        return store;
    }

    private native void getMasses(long objectId, FloatBuffer masses);

    /**
     * Get the total mass of the body.
     *
     * @return the total mass.
     */
    public float getTotalMass() {
        return getTotalMass(objectId);
    }

    private native float getTotalMass(long objectId);

    /**
     * Set the total mass (weighted by previous masses) for the body.
     *
     * @param mass the total mass to set
     * @param fromfaces weighted by faces surface instead of previous masses.
     */
    public void setTotalMass(float mass, boolean fromfaces) {
        setTotalMass(objectId, mass, fromfaces);
    }

    /**
     * Set the total mass (weighted by previous masses) for the body.
     *
     * @param mass the total mass to set
     */
    public void setTotalMass(float mass) {
        setTotalMass(mass, false);
    }

    private native void setTotalMass(long objectId, float mass, boolean fromFaces);

    /**
     * Set the total mass of the body based on the density. (weighted by the
     * density * volume)
     *
     * @param density
     */
    public void setTotalDensity(float density) {
        setTotalDensity(objectId, density);
    }

    private native void setTotalDensity(long objectId, float density);

    /**
     * Set volume mass (using tetrahedrons)
     *
     * @param mass
     */
    public void setVolumeMass(float mass) {
        setVolumeMass(objectId, mass);
    }

    private native void setVolumeMass(long objectId, float mass);

    /**
     * Set volume density (using tetrahedrons)
     *
     * @param density
     */
    public void setVolumeDensity(float density) {
        setVolumeDensity(objectId, density);
    }

    private native void setVolumeDensity(long objectId, float density);

    /**
     * Transform the body.
     *
     * @param trs the transform to apply.
     */
    public void applyPhysicsTransform(Transform trs) {
        applyPhysicsTransform(objectId, trs);
    }

    private native void applyPhysicsTransform(long objectId, Transform trs);

    /**
     * Translate (or move) the body.
     *
     * @param vec the translation to apply.
     */
    public void applyPhysicsTranslate(Vector3f vec) {
        applyPhysicsTranslate(objectId, vec);
    }

    private native void applyPhysicsTranslate(long objectId, Vector3f vec);

    /**
     * Rotate the body.
     *
     * @param rot the rotation to apply.
     */
    public void applyPhysicsRotation(Quaternion rot) {
        applyPhysicsRotation(objectId, rot);
    }

    private native void applyPhysicsRotation(long objectId, Quaternion rot);

    /**
     * Scale the body. To use carefully.
     *
     * @param vec the scaling to apply.
     */
    public void applyPhysicsScale(Vector3f vec) {
        applyPhysicsScale(objectId, vec);
    }

    private native void applyPhysicsScale(long objectId, Vector3f vec);

    /* =======================
     * These methods are not native. ( set-getPhysicsTransform/Location)
     * and will only work if a center can be determinated
     * ======================= */

 /* Transform */
    public void setPhysicsTransform(Transform trs) {
        setPhysicsTransform(objectId, trs);
    }

    private native void setPhysicsTransform(long objectId, Transform trs);

    public Transform getPhysicsTransform() {
        Transform trs = new Transform();
        getPhysicsTransform(objectId, trs);
        return trs;
    }

    private native void getPhysicsTransform(long objectId, Transform trs);

    /* Translate */
    public void setPhysicsLocation(Vector3f vec) {
        setPhysicsLocation(objectId, vec);
    }

    private native void setPhysicsLocation(long objectId, Vector3f vec);

    public Vector3f getPhysicsLocation(Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        getPhysicsLocation(objectId, store);
        return store;
    }

    public Vector3f getPhysicsLocation() {
        return getPhysicsLocation(null);
    }

    private native void getPhysicsLocation(long objectId, Vector3f vec);

    /* Rotate */
    public void setPhysicsRotation(Quaternion rot) {
        setPhysicsRotation(objectId, rot);
    }

    private native void setPhysicsRotation(long objectId, Quaternion rot);

    public Quaternion getPhysicsRotation(Quaternion store) {
        if (store == null) {
            store = new Quaternion();
        }
        getPhysicsRotation(objectId, store);
        return store;
    }

    public Quaternion getPhysicsRotation() {
        return getPhysicsRotation(null);
    }

    private native void getPhysicsRotation(long objectId, Quaternion rot);

    /**
     * Get link resting lengths scale.
     *
     * @return the resting lengths scale.
     */
    public float getRestLengthScale() {
        return getRestLenghtScale(objectId);
    }

    private native float getRestLenghtScale(long objectId);

    /**
     * Scale resting length of all springs
     *
     * @param scale
     */
    public void setRestLengthScale(float scale) {
        setRestLenghtScale(objectId, scale);
    }

    private native void setRestLenghtScale(long objectId, float scale);

    /**
     * Set current state of the softbody as "default pose" or "lowest energy
     * state".
     *
     * @param bvolume boolean volume pose
     * @param bframe boolean frame pose
     */
    public void setPose(boolean bvolume, boolean bframe) {
        setPose(objectId, bvolume, bframe);
    }

    private native void setPose(long objectId, boolean bvolume, boolean bframe);

    /**
     * Set current link lengths as the resting lengths.
     */
    public void resetLinkRestLengths() {
        resetLinkRestLengths(objectId);
    }

    private native void resetLinkRestLengths(long objectId);

    /**
     * Get the volume of the body.
     *
     * @return the volume.
     */
    public float getVolume() {
        return getVolume(objectId);
    }

    private native float getVolume(long objectId);

    /**
     * Get the cluster count, the number of cluster used by this body.
     *
     * @return the number of cluster.
     */
    public int getClusterCount() {
        return getClusterCount(objectId);
    }

    private native int getClusterCount(long objectId);

    /**
     * Generate bending constraints based on distance in the adjency graph.
     *
     * @param distance greater than 1 (else do nothing)
     * @param mat the material to append links
     */
    public void generateBendingConstraints(int distance, Material mat) {
        generateBendingConstraints(objectId, distance, mat.materialId);
    }

    private native void generateBendingConstraints(long bodyId, int distance, long matId);

    /**
     * Randomize constraints to reduce solver bias.
     */
    public void randomizeConstraints() {
        randomizeConstraints(objectId);
    }

    private native void randomizeConstraints(long objectId);

    /**
     * Release a cluster.
     *
     * @param index the index of the cluster to remove.
     */
    public void releaseCluster(int index) {
        releaseCluster(objectId, index);
    }

    private native void releaseCluster(long objectId, int index);

    /**
     * Release all the clusters.
     */
    public void releaseClusters() {
        releaseClusters(objectId);
    }

    private native void releaseClusters(long objectId);

    /**
     * Generate clusters (K-mean) : generateClusters with k=0 will create a
     * convex cluster for each tetrahedron or triangle, otherwise an
     * approximation will be used (better performance). By default maxiterations
     * is set to 8192.
     *
     * @param k the number of cluster to create, can't be bigger than the
     * number of nodes.
     */
    public void generateClusters(int k) {
        generateClusters(k, 8192);
    }

    /**
     * Generate clusters (K-mean) : generateClusters with k=0 will create a
     * convex cluster for each tetrahedron or triangle, otherwise an
     * approximation will be used (better performance).
     *
     * @param k the number of cluster to create, can't be bigger than the
     * number of nodes.
     * @param maxiterations the maximum of iterations, used for the generation
     * of cluster. By default maxiterations is set to 8192.
     */
    public void generateClusters(int k, int maxiterations) {
        generateClusters(objectId, k, maxiterations);
    }

    private native void generateClusters(long objectId, int k, int maxiterations);

    /**
     * Tell if the current softBody is in added into a physicsSpace.
     *
     * @return true if the softBody is in a physicsSpace.
     */
    public boolean isInWorld() {
        return isInWorld(objectId);
    }

    private native boolean isInWorld(long objectId);

    /**
     * Get the center of the bouding volume of this body. This "center" may not
     * be exactly in the center of the object as it's move. The value of the
     * boundingCenter isn't updated each frame.
     *
     * @return the position of the bounding center.
     */
    public Vector3f getBoundingCenter() {
        Vector3f result = new Vector3f();
        getBoundingCenter(objectId, result);
        return result;
    }

    private native void getBoundingCenter(long bodyId, Vector3f store);

    public final void updateTriMesh(Mesh store, boolean meshInLocalSpace, boolean updateNormals) {
        FloatBuffer positionBuffer = store.getFloatBuffer(VertexBuffer.Type.Position);
        FloatBuffer normalBuffer = store.getFloatBuffer(VertexBuffer.Type.Normal);
        updateTriMesh(this.getObjectId(), this.jmeToBulletMap.capacity(), this.jmeToBulletMap, positionBuffer, normalBuffer, meshInLocalSpace, updateNormals);

        store.getBuffer(VertexBuffer.Type.Position).setUpdateNeeded();
        if (updateNormals) {
            store.getBuffer(VertexBuffer.Type.Normal).setUpdateNeeded();
        }
    }

    private native void updateTriMesh(long bodyId, int bufferSize, IntBuffer positionMapping, FloatBuffer positionBuffer, FloatBuffer normalBuffer, boolean meshInLocalSpace, boolean updateNormals);

    /**
     * Get the config object which hold methods to access to the native config
     * fields.
     *
     * @return the config object of this softbody.
     */
    public Config config() {
        return this.config;
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);

        capsule.write(NativeSoftBodyUtil.getDebugMesh(this), "nativeBulletMesh", null);
        capsule.write(jmeToBulletMap, "jmeToBulletMap", null);
        capsule.write(getMasses(), "masses", null);

        capsule.write(getRestLengthScale(), "RestLengthScale", 0);
        capsule.write(getClusterCount(), "ClusterCount", 0);
        capsule.write(getPhysicsLocation(), "PhysicsLocation", Vector3f.ZERO);

        config().write(capsule);
        material().write(capsule);
    }

    @Override
    public void read(JmeImporter e) throws IOException {
        super.read(e);
        InputCapsule capsule = e.getCapsule(this);

        createFromMesh((Mesh) capsule.readSavable("nativeBulletMesh", null));
        jmeToBulletMap = (IntBuffer) capsule.readSavable("jmeToBulletMap", null);
        setMasses(capsule.readFloatBuffer("masses", null));

        setRestLengthScale(capsule.readFloat("RestLengthScale", 0));
        int clusterCount = capsule.readInt("ClusterCount", 0);
        if (clusterCount > 0) {
            generateClusters(clusterCount);
        }
        setPhysicsLocation((Vector3f) capsule.readSavable("PhysicsLocation", Vector3f.ZERO));

        config().read(capsule);
        material().read(capsule);
    }

    /**
     * This class hold methods to access (get and set) natives fields of the
     * Config struct (of btSoftBody). This class is final. And the constructor
     * is private, in order to only have the access through a PhysicsSoftBody,
     * each hysicsSoftBody have only one Config object associated with.
     */
    public final class Config {

        private final PhysicsSoftBody body;

        // /!\ the objectId field from softbody is directly used here because it's a protected field.
        private Config(PhysicsSoftBody body) {
            this.body = body;
        }

        /**
         * Copy the values of the Config conf, into this Config.
         *
         * @param conf the Config with values to set.
         */
        public void copyValues(Config conf) {
            copyValues(objectId, conf.body.objectId);
        }

        private native void copyValues(long thisId, long bodyId);

        /**
         * Set the velocities correction factor (Baumgarte), (aka kVCF).
         *
         * @param factor the value to set.
         */
        public void setVelocitiesCorrectionFactor(float factor) {
            setVelocitiesCorrectionFactor(objectId, factor);
        }

        private native void setVelocitiesCorrectionFactor(long bodyId, float factor);

        /**
         * Get the velocities correction factor (Baumgarte), (aka kVCF).
         *
         * @return the factor value.
         */
        public float getVelocitiesCorrectionFactor() {
            return getVelocitiesCorrectionFactor(objectId);
        }

        private native float getVelocitiesCorrectionFactor(long bodyId);

        /**
         * Set the damping coefficient (aka kDP).
         *
         * @param coefficient the value to set, between [0,1].
         */
        public void setDampingCoef(float coefficient) {
            setDampingCoef(objectId, coefficient);
        }

        private native void setDampingCoef(long bodyId, float coefficient);

        /**
         * Get the damping coefficient (aka kDP).
         *
         * @return the coefficient value.
         */
        public float getDampingCoef() {
            return getDampingCoef(objectId);
        }

        private native float getDampingCoef(long bodyId);

        /**
         * Set the drag coefficient (aka kDG).
         *
         * @param coefficient the value to set, between [0,+inf].
         */
        public void setDragCoef(float coefficient) {
            setDragCoef(objectId, coefficient);
        }

        private native void setDragCoef(long bodyId, float coefficient);

        /**
         * Get the drag coefficient.
         *
         * @return the coefficient value.
         */
        public float getDragCoef() {
            return getDragCoef(objectId);
        }

        private native float getDragCoef(long bodyId);

        /**
         * Set the lift coefficient (aka kLF).
         *
         * @param coefficient the value to set, between [O,+inf].
         */
        public void setLiftCoef(float coefficient) {
            setLiftCoef(objectId, coefficient);
        }

        private native void setLiftCoef(long bodyId, float coefficient);

        /**
         * Get the lift coefficient (aka kLF).
         *
         * @return the coefficient value.
         */
        public float getLiftCoef() {
            return getLiftCoef(objectId);
        }

        private native float getLiftCoef(long bodyId);

        /**
         * Set the pressure coefficient (aka kPR).
         *
         * @param coefficient the value to set, between [-inf,+inf].
         */
        public void setPressureCoef(float coefficient) {
            setPressureCoef(objectId, coefficient);
        }

        private native void setPressureCoef(long bodyId, float coefficient);

        /**
         * Get the pressure coefficient (aka kPR).
         *
         * @return the coefficient value.
         */
        public float getPressureCoef() {
            return getPressureCoef(objectId);
        }

        private native float getPressureCoef(long bodyId);

        /**
         * Set the volume conservation coefficient (aka kVC).
         *
         * @param coefficient the value to set, between [0,+inf].
         */
        public void setVolumeConservationCoef(float coefficient) {
            setVolumeConservationCoef(objectId, coefficient);
        }

        private native void setVolumeConservationCoef(long bodyId, float coefficient);

        /**
         * Get the volume conservation coefficient.
         *
         * @return the coefficient value.
         */
        public float getVolumeConservationCoef() {
            return getVolumeConservationCoef(objectId);
        }

        private native float getVolumeConservationCoef(long bodyId);

        /**
         * Set the dynamic friction coefficient (aka kDF).
         *
         * @param coefficient the value to set, between [0,1].
         */
        public void setDynamicFrictionCoef(float coefficient) {
            setDynamicFrictionCoef(objectId, coefficient);
        }

        private native void setDynamicFrictionCoef(long objectId, float coefficient);

        /**
         * Get the dynamic friction coefficient (aka kDF).
         *
         * @return the coefficient value.
         */
        public float getDynamicFrictionCoef() {
            return getDynamicFrictionCoef(objectId);
        }

        private native float getDynamicFrictionCoef(long objectId);

        /**
         * Set the pose matching coefficient (aka kMT). This coefficient
         * correspond how much the softbody will try to match his "pose".
         *
         * @param coefficient the value to set, between [0,1].
         */
        public void setPoseMatchingCoef(float coefficient) {
            setPoseMatchingCoef(objectId, coefficient);
        }

        private native void setPoseMatchingCoef(long objectId, float coefficient);

        /**
         * Get the pose matching coefficient (aka kMT).
         *
         * @return the value of the coefficient.
         */
        public float getPoseMatchingCoef() {
            return getPoseMatchingCoef(objectId);
        }

        private native float getPoseMatchingCoef(long bodyId);

        /**
         * Set the rigid contacts hardness coefficient (aka kCHR).
         *
         * @param hardness the value to set, between [0,1].
         */
        public void setRigidContactsHardness(float hardness) {
            setRigidContactsHardness(objectId, hardness);
        }

        private native void setRigidContactsHardness(long bodyId, float hardness);

        /**
         * Get the rigid contacts hardness coefficient (aka kCHR).
         *
         * @return the hardness coefficient value.
         */
        public float getRigidContactsHardness() {
            return getRigidContactsHardness(objectId);
        }

        private native float getRigidContactsHardness(long bodyId);

        /**
         * Set the kinectic contacts hardness coefficient (aka kKHR).
         *
         * @param hardness the value to set, between [0,1].
         */
        public void setKineticContactsHardness(float hardness) {
            setKineticContactsHardness(objectId, hardness);
        }

        private native void setKineticContactsHardness(long bodyId, float hardness);

        /**
         * Get the kinetic contacts hardness coefficient (aka kKHR).
         *
         * @return the hardness coefficient value.
         */
        public float getKineticContactsHardness() {
            return getKineticContactsHardness(objectId);
        }

        private native float getKineticContactsHardness(long bodyId);

        /**
         * Set the soft contacts hardness coefficient (aka kSHR).
         *
         * @param hardness the value to set, between [0,1].
         */
        public void setSoftContactsHardness(float hardness) {
            setSoftContactsHardness(objectId, hardness);
        }

        private native void setSoftContactsHardness(long bodyId, float hardness);

        /**
         * Get the soft contact hardness coefficient (aka kSHR).
         *
         * @return the hardness coefficient value.
         */
        public float getSoftContactsHardness() {
            return getSoftContactsHardness(objectId);
        }

        private native float getSoftContactsHardness(long bodyId);

        /**
         * Set the anchors hardness coefficient (aka kAHR).
         *
         * @param hardness the value to set, between [0,1].
         */
        public void setAnchorsHardness(float hardness) {
            setAnchorsHardness(objectId, hardness);
        }

        private native void setAnchorsHardness(long bodyId, float hardness);

        /**
         * Get the anchors hardness coefficient (aka kAHR).
         *
         * @return the hardness coefficient value.
         */
        public float getAnchorsHardness() {
            return getAnchorsHardness(objectId);
        }

        private native float getAnchorsHardness(long bodyId);

        /**
         * Set the soft cluster versus rigid hardness coefficient(aka kSRHR_CL).
         *
         * @param hardness the value to set, between [0,1].
         */
        public void setClusterRigidHardness(float hardness) {
            setClusterRigidHardness(objectId, hardness);
        }

        private native void setClusterRigidHardness(long bodyId, float hardness);

        /**
         * Get the soft cluster versus rigid hardness coefficient(aka kSRHR_CL).
         *
         * @return the hardness coefficient value.
         */
        public float getClusterRigidHardness() {
            return getClusterRigidHardness(objectId);
        }

        private native float getClusterRigidHardness(long bodyId);

        /**
         * Set the soft cluster versus kinetic hardness coefficient (aka
         * kSKHR_CL).
         *
         * @param hardness the value to set, between [0,1].
         */
        public void setClusterKineticHardness(float hardness) {
            setClusterKineticHardness(objectId, hardness);
        }

        private native void setClusterKineticHardness(long bodyId, float hardness);

        /**
         * Get the cluster versus kinetic hardness coefficient (aka kSKHR_CL).
         *
         * @return the hardness coefficient value.
         */
        public float getClusterKineticHardness() {
            return getClusterKineticHardness(objectId);
        }

        private native float getClusterKineticHardness(long bodyId);

        /**
         * Set the soft cluster versus soft hardness coefficient (aka kSSHR_CL).
         *
         * @param hardness the value to set, between [0,1]
         */
        public void setClusterSoftHardness(float hardness) {
            setClusterSoftHardness(objectId, hardness);
        }

        private native void setClusterSoftHardness(long bodyId, float hardness);

        /**
         * Get the soft cluster versus soft hardness coefficient (aka kSSHR_CL).
         *
         * @return the hardness coefficient value.
         */
        public float getClusterSoftHardness() {
            return getClusterSoftHardness(objectId);
        }

        private native float getClusterSoftHardness(long bodyId);

        /**
         * Set the soft cluster versus rigid, impulse split coefficient (aka
         * kSR_SPLT_CL).
         *
         * @param coef the value to set, between [0,1].
         */
        public void setClusterRigidImpulseSplitCoef(float coef) {
            setClusterRigidImpulseSplitCoef(objectId, coef);
        }

        private native void setClusterRigidImpulseSplitCoef(long bodyId, float coef);

        /**
         * Get the soft cluster versus rigid, impulse split coefficient (aka
         * kSR_SPLT_CL).
         *
         * @return the impulse split coefficient value.
         */
        public float getClusterRigidImpulseSplitCoef() {
            return getClusterRigidImpulseSplitCoef(objectId);
        }

        private native float getClusterRigidImpulseSplitCoef(long bodyId);

        /**
         * Set the soft cluster versus kinetic, impulse split coefficient (aka
         * kSK_SPLT_CL).
         *
         * @param coef the value to set, between [0,1].
         */
        public void setClusterKineticImpulseSplitCoef(float coef) {
            setClusterKineticImpulseSplitCoef(objectId, coef);
        }

        private native void setClusterKineticImpulseSplitCoef(long bodyId, float coef);

        /**
         * Get the soft cluster versus kinetic, impulse split coefficient (aka
         * kSK_SPLT_CL).
         *
         * @return the impulse split coefficient value.
         */
        public float getClusterKineticImpulseSplitCoef() {
            return getClusterKineticImpulseSplitCoef(objectId);
        }

        private native float getClusterKineticImpulseSplitCoef(long bodyId);

        /**
         * Set the soft cluster versus soft, impulse split coefficient (aka
         * kSS_SPLT_CL).
         *
         * @param coef the value to set, between [0,1].
         */
        public void setClusterSoftImpulseSplitCoef(float coef) {
            setClusterSoftImpulseSplitCoef(objectId, coef);
        }

        private native void setClusterSoftImpulseSplitCoef(long bodyId, float coef);

        /**
         * Get the soft cluster versus soft, impulse split coefficient (aka
         * kSS_SPLT_CL).
         *
         * @return the impulse split coefficient value.
         */
        public float getClusterSoftImpulseSplitCoef() {
            return getClusterSoftImpulseSplitCoef(objectId);
        }

        private native float getClusterSoftImpulseSplitCoef(long bodyId);

        /**
         * Set the maximum volume ratio for pose.
         *
         * @param ratio the value to set.
         */
        public void setMaximumVolumeRatio(float ratio) {
            setMaximumVolumeRatio(objectId, ratio);
        }

        private native void setMaximumVolumeRatio(long bodyId, float ratio);

        /**
         * Get the maximum volume ratio for pose.
         *
         * @return the maximum volume ratio value.
         */
        public float getMaximumVolumeRatio() {
            return getMaximumVolumeRatio(objectId);
        }

        private native float getMaximumVolumeRatio(long bodyId);

        /**
         * Set the time scale.
         *
         * @param scale the value to set.
         */
        public void setTimeScale(float scale) {
            setTimeScale(objectId, scale);
        }

        private native void setTimeScale(long bodyId, float scale);

        /**
         * Get the time scale.
         *
         * @return the value of the time scale.
         */
        public float getTimeScale() {
            return getTimeScale(objectId);
        }

        private native float getTimeScale(long bodyId);

        /**
         * Set the velocities solver iterations (aka viterations).
         *
         * @param iterations the value to set.
         */
        public void setVelocitiesIterations(int iterations) {
            setVelocitiesIterations(objectId, iterations);
        }

        private native void setVelocitiesIterations(long objectId, int iteration);

        /**
         * Get the velocities solver iterations (aka viterations).
         *
         * @return the velocities solver iterations value.
         */
        public int getVelocitiesIterations() {
            return getVelocitiesIterations(objectId);
        }

        private native int getVelocitiesIterations(long objectId);

        /**
         * Set the positions solver iterations (aka piterations).
         *
         * @param iterations the value to set.
         */
        public void setPositionIterations(int iterations) {
            setPositionIterations(objectId, iterations);
        }

        private native void setPositionIterations(long objectId, int iteration);

        /**
         * Get the positions solver iterations (aka piterations).
         *
         * @return the positions solver iterations value.
         */
        public int getPositionIterations() {
            return getPositionIterations(objectId);
        }

        private native int getPositionIterations(long objectId);

        /**
         * Set the drift solver iterations (aka diterations).
         *
         * @param iterations the value to set.
         */
        public void setDriftIterations(int iterations) {
            setDriftIterations(objectId, iterations);
        }

        private native void setDriftIterations(long objectId, int iteration);

        /**
         * Get the drift solver iterations (aka diterations).
         *
         * @return the drift solver iterations value.
         */
        public int getDriftIterations() {
            return getDriftIterations(objectId);
        }

        private native int getDriftIterations(long objectId);

        /**
         * Set the cluster solver iterations (aka citerations).
         *
         * @param iterations the value to set.
         */
        public void setClusterIterations(int iterations) {
            setClusterIterations(objectId, iterations);
        }

        private native void setClusterIterations(long objectId, int iteration);

        /**
         * Get the cluster solver iterations (aka citerations).
         *
         * @return the cluster solver iterations value.
         */
        public int getClusterIterations() {
            return getClusterIterations(objectId);
        }

        private native int getClusterIterations(long objectId);

        /**
         * Config collision flag : Rigid versus soft mask.
         */
        public final static int RVSmask = 0x000f;

        /**
         * Config collision flag : SDF based rigid vs soft.
         */
        public final static int SDF_RS = 0x0001;

        /**
         * Config collision flag : Cluster vs convex rigid vs soft.
         */
        public final static int CL_RS = 0x0002;

        /**
         * Config collision flag : Soft versus soft mask.
         */
        public final static int SVSmask = 0x0030;

        /**
         * Config collision flag : Vertex vs face soft cluster vs soft handling.
         */
        public final static int VF_SS = 0x0010;

        /**
         * Config collision flag : Cluster vs cluster soft vs soft handling.
         */
        public final static int CL_SS = 0x0020;

        /**
         * Config collision flag : Cluster soft body self collision.
         */
        public final static int CL_SELF = 0x0040;

        /**
         * Config collision flag : Default value = SDF_RS. (SDF based rigid vs
         * soft)
         */
        public final static int Default = SDF_RS;

        /**
         * Set the collision flag as the value of flags. Avaliable flags values
         * are defined into the Config class.
         *
         * @param flag the flag value to set.
         * @param flags ... optional values of flags, if there are values they
         * will be added to the flag value by a bitwise inclusive OR.
         */
        public void setCollisionsFlags(int flag, int... flags) {
            int allFlags = flag;
            for (int i = 0; i < flags.length; i++) {
                allFlags |= flags[i];
            }
            setCollisionsFlags(objectId, allFlags);
        }

        private native void setCollisionsFlags(long bodyId, int flags);

        /**
         * Get the collisions flags value.
         *
         * @return the value of the collisions flags.
         */
        public int getCollisionsFlags() {
            return getCollisionsFlags(objectId);
        }

        private native int getCollisionsFlags(long bodyId);

        protected void write(OutputCapsule capsule) throws IOException {

            capsule.write(getVelocitiesCorrectionFactor(), "VelocitiesCorrectionFactor", 1f);
            capsule.write(getDampingCoef(), "DampingCoef", 0);
            capsule.write(getDragCoef(), "DragCoef", 0);
            capsule.write(getLiftCoef(), "LiftCoef", 0);
            capsule.write(getPressureCoef(), "PressureCoef", 0);
            capsule.write(getVolumeConservationCoef(), "VolumeConservationCoef", 0);
            capsule.write(getDynamicFrictionCoef(), "DynamicFrictionCoef", 0.2f);
            capsule.write(getPoseMatchingCoef(), "PoseMatchingCoef", 0);

            capsule.write(getRigidContactsHardness(), "RigidContactsHardness", 1.0f);
            capsule.write(getKineticContactsHardness(), "KineticContactsHardness", 0.1f);
            capsule.write(getSoftContactsHardness(), "SoftContactsHardness", 1.0f);
            capsule.write(getAnchorsHardness(), "AnchorsHardness", 0.7f);

            capsule.write(getClusterRigidHardness(), "ClusterRigidHardness", 0.1f);
            capsule.write(getClusterKineticHardness(), "ClusterKineticHardness", 1f);
            capsule.write(getClusterSoftHardness(), "ClusterSoftHardness", 0.5f);
            capsule.write(getClusterRigidImpulseSplitCoef(), "ClusterRigidImpulseSplitCoef", 0.5f);
            capsule.write(getClusterKineticImpulseSplitCoef(), "ClusterKineticImpulseSplitCoef", 0.5f);
            capsule.write(getClusterSoftImpulseSplitCoef(), "ClusterSoftImpulseSplitCoef", 0.5f);

            capsule.write(getMaximumVolumeRatio(), "MaximumVolumeRatio", 1f);
            capsule.write(getTimeScale(), "TimeScale", 1f);

            capsule.write(getVelocitiesIterations(), "VelocitiesIterations", 0);
            capsule.write(getPositionIterations(), "PositionIterations", 1);
            capsule.write(getDriftIterations(), "DriftIterations", 0);
            capsule.write(getClusterIterations(), "ClusterIterations", 4);

            capsule.write(getCollisionsFlags(), "CollisionsFlags", Default);
        }

        protected void read(InputCapsule capsule) throws IOException {

            setVelocitiesCorrectionFactor(capsule.readFloat("VelocitiesCorrectionFactor", 1f));
            setDampingCoef(capsule.readFloat("DampingCoef", 0));
            setDragCoef(capsule.readFloat("DragCoef", 0));
            setLiftCoef(capsule.readFloat("LiftCoef", 0));
            setPressureCoef(capsule.readFloat("PressureCoef", 0));
            setVolumeConservationCoef(capsule.readFloat("VolumeConservationCoef", 0));
            setDynamicFrictionCoef(capsule.readFloat("DynamicFrictionCoef", 0.2f));
            setPoseMatchingCoef(capsule.readFloat("PoseMatchingCoef", 0));

            setRigidContactsHardness(capsule.readFloat("RigidContactsHardness", 1.0f));
            setKineticContactsHardness(capsule.readFloat("KineticContactsHardness", 0.1f));
            setSoftContactsHardness(capsule.readFloat("SoftContactsHardness", 1.0f));
            setAnchorsHardness(capsule.readFloat("AnchorsHardness", 0.7f));

            setClusterRigidHardness(capsule.readFloat("ClusterRigidHardness", 0.1f));
            setClusterKineticHardness(capsule.readFloat("ClusterKineticHardness", 1f));
            setClusterSoftHardness(capsule.readFloat("ClusterSoftHardness", 0.5f));
            setClusterRigidImpulseSplitCoef(capsule.readFloat("ClusterRigidImpulseSplitCoef", 0.5f));
            setClusterKineticImpulseSplitCoef(capsule.readFloat("ClusterKineticImpulseSplitCoef", 0.5f));
            setClusterSoftImpulseSplitCoef(capsule.readFloat("ClusterSoftImpulseSplitCoef", 0.5f));

            setMaximumVolumeRatio(capsule.readFloat("MaximumVolumeRatio", 1f));
            setTimeScale(capsule.readFloat("TimeScale", 1f));

            setVelocitiesIterations(capsule.readInt("VelocitiesIterations", 0));
            setPositionIterations(capsule.readInt("PositionIterations", 1));
            setDriftIterations(capsule.readInt("DriftIterations", 0));
            setClusterIterations(capsule.readInt("ClusterIterations", 4));

            setCollisionsFlags(capsule.readInt("CollisionsFlags", Default));
        }

    };

    /**
     * The Material class hold methods to access (get and set) natives fields of
     * the Material struct (of btSoftBody). This class is final.
     * <p>
     * The constructor is protected and should only used internally. The only
     * way to create new Material is to use the method <code> appendMaterial()
     * </code> from a PhysicsSoftBody. This way a new native Material will be
     * created and added to the body (as done in native).</p>
     * <p>
     * Native materials have a field m_flags, this fields is not available
     * through this binding because the only flag is DebugDraw and it's will be
     * disable by default when appending a new Material (done with the binding
     * not native).</p>
     */
    public final class Material {

        private long materialId;

        private Material(PhysicsSoftBody body) {
            this.materialId = body.getMaterial(body.objectId);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj.getClass() == Material.class) {
                return this.materialId == ((Material) obj).materialId;
            } else {
                return false;
            }
        }

        /**
         * Get the linear stiffness coefficient (aka m_KLST).
         *
         * @return the linearStiffnessFactor
         */
        public float getLinearStiffnessFactor() {
            return getLinearStiffnessFactor(materialId);
        }

        private native float getLinearStiffnessFactor(long materialId);

        /**
         * Set the linear stiffness coefficient (aka m_kLST).
         *
         * @param linearStiffnessFactor the value to set, between [0,1]
         */
        public void setLinearStiffnessFactor(float linearStiffnessFactor) {
            setLinearStiffnessFactor(materialId, linearStiffnessFactor);
        }

        private native void setLinearStiffnessFactor(long materialId, float linearStiffnessFactor);

        /**
         * Get the angular stiffness factor (aka m_kAST).
         *
         * @return the angularStiffnessFactor
         */
        public float getAngularStiffnessFactor() {
            return getAngularStiffnessFactor(materialId);
        }

        private native float getAngularStiffnessFactor(long materialId);

        /**
         * Set the angular stiffness factor (aka m_kAST).
         *
         * @param angularStiffnessFactor the value to set, between [0,1].
         */
        public void setAngularStiffnessFactor(float angularStiffnessFactor) {
            setAngularStiffnessFactor(materialId, angularStiffnessFactor);
        }

        private native void setAngularStiffnessFactor(long materialId, float angularStiffnessFactor);

        /**
         * Get the volume stiffness factor (aka m_kVST).
         *
         * @return the volumeStiffnessFactor
         */
        public float getVolumeStiffnessFactor() {
            return getVolumeStiffnessFactor(materialId);
        }

        private native float getVolumeStiffnessFactor(long materialId);

        /**
         * Set the volume stiffness factor (aka m_kVST).
         *
         * @param volumeStiffnessFactor the value to set, between [0,1].
         */
        public void setVolumeStiffnessFactor(float volumeStiffnessFactor) {
            setVolumeStiffnessFactor(materialId, volumeStiffnessFactor);
        }

        private native void setVolumeStiffnessFactor(long materialId, float volumeStiffnessFactor);

        public void write(OutputCapsule capsule) throws IOException {

            capsule.write(getAngularStiffnessFactor(), "AngularStiffnessFactor", 1f);
            capsule.write(getLinearStiffnessFactor(), "LinearStiffnessFactor", 1f);
            capsule.write(getVolumeStiffnessFactor(), "VolumeStiffnessFactor", 1f);

        }

        public void read(InputCapsule capsule) throws IOException {

            setAngularStiffnessFactor(capsule.readFloat("AngularStiffnessFactor", 1f));
            setLinearStiffnessFactor(capsule.readFloat("LinearStiffnessFactor", 1f));
            setVolumeStiffnessFactor(capsule.readFloat("VolumeStiffnessFactor", 1f));
        }

    }
}
