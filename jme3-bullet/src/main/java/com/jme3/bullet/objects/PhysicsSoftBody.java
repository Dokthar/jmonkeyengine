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
import com.jme3.bullet.objects.infos.SoftBodyWorldInfo;
import com.jme3.bullet.util.DebugMeshCallback;
import com.jme3.bullet.util.NativeMeshUtil;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dokthar
 */
public class PhysicsSoftBody extends PhysicsCollisionObject {

    @Deprecated
    public PhysicsSoftBody(Vector3f[] vertices, float[] masses) {
        objectId = ctr_PhysicsSoftBody(vertices.length, vertices, masses);
        postRebuild(false);
    }

    public PhysicsSoftBody() {
        objectId = ctr_PhysicsSoftBody();
        initDefault();
        postRebuild(false);
    }

    public PhysicsSoftBody(Mesh triMesh) {
// must check if the mesh have %3 vertices
        createFromTriMesh(triMesh);
    }

    // SoftBodies need to have a direct access to JME's Mesh buffer in order a have a more
    // efficient way when updating the Mesh each frame
    private final long createFromTriMesh(Mesh triMesh) {
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(triMesh.getTriangleCount() * 3);
        FloatBuffer positionBuffer = triMesh.getFloatBuffer(Type.Position);

        IndexBuffer indices = triMesh.getIndicesAsList();
        int indicesLength = triMesh.getTriangleCount() * 3;
        for (int i = 0; i < indicesLength; i++) { // done because mesh indexs can use short or i am wrong
            indexBuffer.put(indices.get(i));
        }
        //Add
        objectId = createFromTriMesh(indexBuffer, positionBuffer, triMesh.getTriangleCount(), false);
        postRebuild(false);
        return objectId;
    }

    private native long createFromTriMesh(IntBuffer triangles, FloatBuffer vertices, int numTriangles, boolean randomizeConstraints);

    private native long ctr_PhysicsSoftBody();

    private native long ctr_PhysicsSoftBody(int size, Vector3f[] vertices, float[] mass);
    //private native long ctr_PhysicsSoftBody(int nodeCount, Vector3f x, float m);

    /**
     * Builds/rebuilds the physics body when parameters have changed
     */
    /* protected void rebuildSoftBody(Mesh mesh) {

     preRebuild();
     if (mesh != null) {
     objectId = createFromTriMesh(mesh);
     } else {
     objectId = ctr_PhysicsSoftBody();
     }
     Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Created RigidBody {0}", Long.toHexString(objectId));
     postRebuild();

     }*/
    protected void rebuildFromTriMesh(Mesh mesh) {
        // {mesh != null} => {old Native object is removed & destroyed; new Native object is created & added}
        boolean wasInWorld = isInWorld();
        preRebuild();
        objectId = createFromTriMesh(mesh);
        postRebuild(wasInWorld);
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

    public void setSoftBodyWorldInfo(SoftBodyWorldInfo worldinfo) {
        setSoftBodyWorldInfo(objectId, worldinfo.getWorldInfoId());
    }

    private native void setSoftBodyWorldInfo(long objectId, long worldinfoId);

    public SoftBodyWorldInfo getSoftBodyWorldInfo() {
        long worldInfoId = getSoftBodyWorldInfo(objectId);
        SoftBodyWorldInfo worldInfo = new SoftBodyWorldInfo(worldInfoId); // <-point on the same native object
        return worldInfo;
    }

    private native long getSoftBodyWorldInfo(long objectId);

    /*public static PhysicsSoftBody creatFromTriMesh(Mesh mesh);

     }*/

    /* API */
// public PhysicsSoftBody(SoftBodyWorldInfo worldInfo, int node_count, Vector3f x[], btScalar* m);
// btSoftBody(	btSoftBodyWorldInfo* worldInfo); done 
// void	initDefaults(); done
//	btSoftBodyWorldInfo*	getWorldInfo(); done
  /*  public boolean checkLink(int node0, int node1) {

     }

     public boolean checkLink(SoftBodyNode node0, SoftBodyNode node1) {

     }

     public boolean checkFace(int node0, int node1, int node2) {

     }

     public boolean checkFace(SoftBodyNode node0, SoftBodyNode node1, SoftBodyNode node2) { //<--- do not existe in bullet

     }
     */
    public void appendMaterial(SoftBodyMaterial material) {
        appendMaterial(objectId, material);
    }

    private native void appendMaterial(long objectId, SoftBodyMaterial material);

    /* Append anchor */
    public void appendAnchor(int node, PhysicsRigidBody rigidBody, boolean collisionBetweenLinkedBodies, float influence) {

    }

    public void appendAnchor(int node, PhysicsRigidBody rigidBody, Vector3f localPivot, boolean collisionBetweenLinkedBodies, float influence) {

    }

    /* Append linear joint	*/
    /*public void appendLinearJoint(const LJoint::Specs& specs,Cluster* body0,Body body1){
    
     }
     public void appendLinearJoint(const LJoint::Specs& specs,Body body=Body()) {
    
     }
     public void appendLinearJoint(const LJoint::Specs& specs,btSoftBody* body) {
    
     }
     */
    /* Append linear joint	*/
// public void appendAngularJoint(const AJoint::Specs& specs,Cluster* body0,Body body1);
// public void appendAngularJoint(const AJoint::Specs& specs,Body body=Body());
// public void appendAngularJoint(const AJoint::Specs& specs,btSoftBody* body);

    /* Add force (or gravity) to the entire body */
    public void addForce(Vector3f force) {
        addForce(objectId, force);
    }

    private native void addForce(long objectId, Vector3f force);

    /* Add force (or gravity) to a node of the body */
    public void addForce(Vector3f force, int node) {
        addForce(objectId, force, node);
    }

    private native void addForce(long objectId, Vector3f force, int node);

    /* Add aero force to a node of the body */
    public void addAeroForceToNode(Vector3f windVelocity, int nodeIndex) {
        addAeroForceToNode(objectId, windVelocity, nodeIndex);
    }

    private native void addAeroForceToNode(long objectId, Vector3f windVelocity, int nodeIndex);

    /* Add aero force to a face of the body */
    public void addAeroForceToFace(Vector3f windVelocity, int faceIndex) {
        addAeroForceToFace(objectId, windVelocity, faceIndex);
    }

    private native void addAeroForceToFace(long objectId, Vector3f windVelocity, int faceIndex);

    /* Add velocity to the entire body */
    public void addVelocity(Vector3f velocity) {

    }

    /* Set velocity for the entire body */
    public void setVelocity(Vector3f velocity) {

    }

    /* Add velocity to a node of the body */
    public void addVelocity(Vector3f velocity, int node) {

    }

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

    /* Scale */
    public void setPhysicsScale(Vector3f scl) {
        setPhysicsScale(objectId, scl);
    }

    private native void setPhysicsScale(long objectId, Vector3f scl);

    public Vector3f getPhysicsScale() {
        Vector3f scl = new Vector3f();
        getPhysicsScale(objectId, scl);
        return scl;
    }

    private native void getPhysicsScale(long objectId, Vector3f scl);

    public float getRestLengthScale() {
        return getRestLenghtScale(objectId);
    }

    /* Get link resting lengths scale */
    private native float getRestLenghtScale(long objectId);

    /* Scale resting length of all springs */
    public void setRestLengthScale(float scale) {
        setRestLenghtScale(objectId, scale);
    }

    private native void setRestLenghtScale(long objectId, float scale);

    /* Set current state as pose */
    public void setPose(boolean bvolume, boolean bframe) {
        setPose(objectId, bvolume, bframe);
    }

    private native void setPose(long objectId, boolean bvolume, boolean bframe);

    /* Set current link lengths as resting lengths */
    public void resetLinkRestLengths() {
        resetLinkRestLengths(objectId);
    }

    private native void resetLinkRestLengths(long objectId);

    /* Return the volume */
    public float getVolume() {
        return getVolume(objectId);
    }

    private native float getVolume(long objectId);

    /* Cluster count */
    public int getClusterCount() {
        return getClusterCount(objectId);
    }

    private native int getClusterCount(long objectId);

    /* Cluster center of mass */
    /*public static Vector3f clusterCom(const Cluster* cluster); */
    public Vector3f getClusterCenterOfMass(int clusterId) {
        Vector3f vec = new Vector3f();
        getClusterCenterOfMass(objectId, clusterId, vec);
        return vec;
    }

    private native void getClusterCenterOfMass(long objectId, int clusterId, Vector3f vec);

    /* Cluster velocity at rpos */
    /*public static Vector3f clusterVelocity(const Cluster* cluster,const btVector3& rpos);*/
    /* Cluster impulse */
    /*static void	clusterVImpulse(Cluster* cluster,const btVector3& rpos,const btVector3& impulse);
     static void	clusterDImpulse(Cluster* cluster,const btVector3& rpos,const btVector3& impulse);
     static void	clusterImpulse(Cluster* cluster,const btVector3& rpos,const Impulse& impulse);
     static void	clusterVAImpulse(Cluster* cluster,const btVector3& impulse);
     static void	clusterDAImpulse(Cluster* cluster,const btVector3& impulse);
     static void	clusterAImpulse(Cluster* cluster,const Impulse& impulse);
     static void	clusterDCImpulse(Cluster* cluster,const btVector3& impulse);*/
    /* Generate bending constraints based on distance in the adjency graph */
    /*public int generateBendingConstraints( int distance, Material* mat=0){
        
     }*/
    /* Randomize constraints to reduce solver bias */
    public void randomizeConstraints() {
        randomizeConstraints(objectId);
    }

    private native void randomizeConstraints(long objectId);

    /* Release clusters */
    public void releaseCluster(int index) {
        releaseCluster(objectId, index);
    }

    private native void releaseCluster(long objectId, int index);

    public void releaseClusters() {
        releaseClusters(objectId);
    }

    private native void releaseClusters(long objectId);

    /* Generate clusters (K-mean) */
///generateClusters with k=0 will create a convex cluster for each tetrahedron or triangle
///otherwise an approximation will be used (better performance)
    public int generateClusters(int k) {
        return generateClusters(k, 8192);
    }

    public int generateClusters(int k, int maxiterations) {
        return generateClusters(objectId, k, maxiterations);
    }

    private native int generateClusters(long objectId, int k, int maxiterations);

    /* Refine */
    /*public void refine(ImplicitFn* ifn,float accurary,boolean cut);*/
    /* CutLink */
    public boolean cutLink(int node0, int node1, float position) {
        return false;
    }
    /*public boolean cutLink(Node* node0, Node* node1, float position){
    
     }*/
///Ray casting using rayFrom and rayTo in worldspace, (not direction!)
/*public boolean rayTest(Vector3f rayFrom, Vector3f rayTo, sRayCast& results);*/
    /* Solver presets */
    /*public void setSolver(eSolverPresets::_ preset);*/

    /* predictMotion */
    public void predictMotion(float dt) {
        predictMotion(objectId, dt);
    }

    private native void predictMotion(long objectId, float dt);
    /* solveConstraints */

    public void solveConstraints() {
        solveConstraints(objectId);
    }

    private native void solveConstraints(long objectId);

    /* staticSolve */
    public void staticSolve(int iterations) {
        staticSolce(objectId, iterations);
    }

    private native void staticSolce(long objectId, int iterations);

    /* solveCommonConstraints */
    /*public static void solveCommonConstraints(btSoftBody** bodies,int count,int iterations){
    
     }*/
    /* solveClusters */
    /*public static void solveClusters(const btAlignedObjectArray<btSoftBody*>& bodies){
    
     }*/
    /* integrateMotion */
    public void integrateMotion() {
        integrateMotion(objectId);
    }

    private native void integrateMotion(long objectId);
    /* defaultCollisionHandlers */
    /*public void defaultCollisionHandler(const btCollisionObjectWrapper* pcoWrap){
    
     }*/

    public void defaultCollisionHandler(PhysicsSoftBody psb) {
        defaultCollisionHandler(psb.objectId);
    }

    private native void defaultCollisionHandler(long objectId);

    public boolean isInWorld() {
        return isInWorld(objectId);
    }

    private native boolean isInWorld(long objectId);

    /*
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
        mesh.setBuffer(Type.Position, 3, getVertices(softBody));
        mesh.setBuffer(Type.Index, 3, getIndexes(softBody));
        mesh.getFloatBuffer(Type.Position).clear();
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

    public static void updateMesh(PhysicsSoftBody softBody, Mesh store, boolean updateNormals) {
        FloatBuffer positionBuffer = store.getFloatBuffer(Type.Position);
        FloatBuffer normalBuffer = store.getFloatBuffer(Type.Normal);
        updateMesh(softBody.getObjectId(), positionBuffer, store.getVertexCount(), updateNormals, normalBuffer);
        store.getBuffer(Type.Position).setUpdateNeeded();
    }

    private static native void updateMesh(long bodyId, FloatBuffer vertices, int numVertices, boolean updateNormals, FloatBuffer normals);

    /*============*
     * data Struct
     *============*/
    class SoftBodyMaterial {

        private float linearStiffnessFactor; // Linear stiffness coefficient [0,1]
        private float angularStiffnessFactor; // Area/Angular stiffness coefficient [0,1]
        private float volumeStiffnessFactor; // Volume stiffness coefficient [0,1]
        private int flags; // Flags

        public SoftBodyMaterial(float linearStiffnessFactor, float angularStiffnessFactor, float volumeStiffnessFactor, int flags) {
            this.linearStiffnessFactor = linearStiffnessFactor;
            this.angularStiffnessFactor = angularStiffnessFactor;
            this.volumeStiffnessFactor = volumeStiffnessFactor;
            this.flags = flags;
        }

        /**
         * @return the linearStiffnessFactor
         */
        public float getLinearStiffnessFactor() {
            return linearStiffnessFactor;
        }

        /**
         * @param linearStiffnessFactor the linearStiffnessFactor to set
         */
        public void setLinearStiffnessFactor(float linearStiffnessFactor) {
            this.linearStiffnessFactor = linearStiffnessFactor;
        }

        /**
         * @return the angularStiffnessFactor
         */
        public float getAngularStiffnessFactor() {
            return angularStiffnessFactor;
        }

        /**
         * @param angularStiffnessFactor the angularStiffnessFactor to set
         */
        public void setAngularStiffnessFactor(float angularStiffnessFactor) {
            this.angularStiffnessFactor = angularStiffnessFactor;
        }

        /**
         * @return the volumeStiffnessFactor
         */
        public float getVolumeStiffnessFactor() {
            return volumeStiffnessFactor;
        }

        /**
         * @param volumeStiffnessFactor the volumeStiffnessFactor to set
         */
        public void setVolumeStiffnessFactor(float volumeStiffnessFactor) {
            this.volumeStiffnessFactor = volumeStiffnessFactor;
        }

        /**
         * @return the flags
         */
        public int getFlags() {
            return flags;
        }

        /**
         * @param flags the flags to set
         */
        public void setFlags(int flags) {
            this.flags = flags;
        }

    }
}
