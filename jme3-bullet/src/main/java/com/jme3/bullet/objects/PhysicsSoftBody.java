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
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dokthar
 */
public class PhysicsSoftBody extends PhysicsCollisionObject {

    private final Config config = new Config();

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
    public Material appendMaterial() {
        long matId = appendMaterial(objectId);
        return new Material(matId);
    }

    private native long appendMaterial(long objectId);

    public ArrayList<Material> getMaterialList() {
        long matIds[] = getMaterials(objectId);
        ArrayList<Material> matList = new ArrayList<Material>(matIds.length);
        for (int i = 0; i < matIds.length; i++) {
            matList.add(new Material(matIds[i]));
        }
        return matList;
    }

    private native long[] getMaterials(long bodyId);


    /* Append anchor */
    /* public void appendAnchor(int node, PhysicsRigidBody rigidBody, boolean collisionBetweenLinkedBodies, float influence) {

     }

     public void appendAnchor(int node, PhysicsRigidBody rigidBody, Vector3f localPivot, boolean collisionBetweenLinkedBodies, float influence) {

     }*/

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

    /* Set mass */
    public void setMass(int node, float mass) {
        setMass(objectId, node, mass);
    }

    private native void setMass(long objectId, int node, float mass);

    /* Get mass */
    public float getMass(int node) {
        return getMass(objectId, node);
    }

    private native float getMass(long objectId, int node);

    /* Get total mass */
    public float getTotalMass() {
        return getTotalMass(objectId);
    }

    private native float getTotalMass(long objectId);

    /* Set total mass (weighted by previous masses) */
    public void setTotalMass(float mass, boolean fromfaces) {
        setTotalMass(objectId, mass, fromfaces);
    }

    public void setTotalMass(float mass) {
        setTotalMass(mass, false);
    }

    private native void setTotalMass(long objectId, float mass, boolean fromFaces);

    /* Set total density */
    public void setTotalDensity(float density) {
        setTotalDensity(objectId, density);
    }

    private native void setTotalDensity(long objectId, float density);

    /* Set volume mass (using tetrahedrons) */
    public void setVolumeMass(float mass) {
        setVolumeMass(objectId, mass);
    }

    private native void setVolumeMass(long objectId, float mass);

    /* Set volume density (using tetrahedrons) */
    public void setVolumeDensity(float density) {
        setVolumeDensity(objectId, density);
    }

    private native void setVolumeDensity(long objectId, float density);

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
    public int generateBendingConstraints(int distance, Material mat) {
        return generateBendingConstraints(objectId, distance, mat.materialId);
    }

    private native int generateBendingConstraints(long bodyId, int distance, long matId);

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
        staticSolve(objectId, iterations);
    }

    private native void staticSolve(long objectId, int iterations);

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

    public Vector3f getBoundingCenter() {
        Vector3f result = new Vector3f();
        getCenter(objectId, result);
        return result;
    }

    private native void getCenter(long bodyId, Vector3f store);

    /*====================*
     Config access
     *====================*/
    public Config getConfig() {
        return this.config;
    }

    public final class Config {

        // /!\ the objectId field is directly used here because it's a protected fields.
        private Config() {
            super();
        }

        //struct	Config
//     eAeroModel::_ aeromodel; // Aerodynamic model (default: V_Point)
//     btScalar kVCF; // Velocities correction factor (Baumgarte)
        public void setVelocitiesCorrectionFactor(float factor) {
            setVelocitiesCorrectionFactor(objectId, factor);
        }

        private native void setVelocitiesCorrectionFactor(long bodyId, float factor);

        public float getVelocitiesCorrectionFactor() {
            return getVelocitiesCorrectionFactor(objectId);
        }

        private native float getVelocitiesCorrectionFactor(long bodyId);

//     btScalar kDP; // Damping coefficient [0,1]
        public void setDampingCoef(float coefficient) {
            setDampingCoef(objectId, coefficient);
        }

        private native void setDampingCoef(long bodyId, float coefficient);

        public float getDampingCoef() {
            return getDampingCoef(objectId);
        }

        private native float getDampingCoef(long bodyId);

//     btScalar kDG; // Drag coefficient [0,+inf]
        public void setDragCoef(float coefficient) {
            setDragCoef(objectId, coefficient);
        }

        private native void setDragCoef(long bodyId, float coefficient);

        public float getDragCoef() {
            return getDragCoef(objectId);
        }

        private native float getDragCoef(long bodyId);

//     btScalar kLF; // Lift coefficient [0,+inf]
        public void setLiftCoef(float coefficient) {
            setLiftCoef(objectId, coefficient);
        }

        private native void setLiftCoef(long bodyId, float coefficient);

        public float getLiftCoef() {
            return getLiftCoef(objectId);
        }

        private native float getLiftCoef(long bodyId);

//     btScalar kPR; // Pressure coefficient [-inf,+inf]
        public void setPressureCoef(float coefficient) {
            setPressureCoef(objectId, coefficient);
        }

        private native void setPressureCoef(long bodyId, float coefficient);

        public float getPressureCoef() {
            return getPressureCoef(objectId);
        }

        private native float getPressureCoef(long bodyId);

//     btScalar kVC; // Volume conversation coefficient [0,+inf]
        public void setVolumeConservationCoef(float coefficient) {
            setVolumeConservationCoef(objectId, coefficient);
        }

        private native void setVolumeConservationCoef(long bodyId, float coefficient);

        public float getVolumeConservationCoef() {
            return getVolumeConservationCoef(objectId);
        }

        private native float getVolumeConservationCoef(long bodyId);

//     -> btScalar kDF; // Dynamic friction coefficient [0,1]
        /**
         * set the Dynamic friction coefficient [0,1] (aka kDF).
         *
         * @param coefficient
         */
        public void setDynamicFrictionCoef(float coefficient) {
            setDynamicFrictionCoef(objectId, coefficient);
        }

        private native void setDynamicFrictionCoef(long objectId, float coefficient);

        public float getDynamicFrictionCoef() {
            return getDynamicFrictionCoef(objectId);
        }

        private native float getDynamicFrictionCoef(long objectId);

//     -> btScalar kMT; // Pose matching coefficient [0,1]
        public void setPoseMatchingCoef(float coefficient) {
            setPoseMatchingCoef(objectId, coefficient);
        }

        private native void setPoseMatchingCoef(long objectId, float coefficient);

        public float getPoseMatchingCoef() {
            return getPoseMatchingCoef(objectId);
        }

        private native float getPoseMatchingCoef(long bodyId);

//     btScalar kCHR; // Rigid contacts hardness [0,1]
        public void setRigidContactsHardness(float hardness) {
            setRigidContactsHardness(objectId, hardness);
        }

        private native void setRigidContactsHardness(long bodyId, float hardness);

        public float getRigidContactsHardness() {
            return getRigidContactsHardness(objectId);
        }

        private native float getRigidContactsHardness(long bodyId);

//     btScalar kKHR; // Kinetic contacts hardness [0,1]
        public void setKineticContactsHardness(float hardness) {
            setKineticContactsHardness(objectId, hardness);
        }

        private native void setKineticContactsHardness(long bodyId, float hardness);

        public float getKineticContactsHardness() {
            return getKineticContactsHardness(objectId);
        }

        private native float getKineticContactsHardness(long bodyId);

//     btScalar kSHR; // Soft contacts hardness [0,1]
        public void setSoftContactsHardness(float hardness) {
            setSoftContactsHardness(objectId, hardness);
        }

        private native void setSoftContactsHardness(long bodyId, float hardness);

        public float getSoftContactsHardness() {
            return getSoftContactsHardness(objectId);
        }

        private native float getSoftContactsHardness(long bodyId);

//     btScalar kAHR; // Anchors hardness [0,1]
        public void setAnchorsHardness(float hardness) {
            setAnchorsHardness(objectId, hardness);
        }

        private native void setAnchorsHardness(long bodyId, float hardness);

        public float getAnchorsHardness() {
            return getAnchorsHardness(objectId);
        }

        private native float getAnchorsHardness(long bodyId);

//     btScalar kSRHR_CL; // Soft vs rigid hardness [0,1] (cluster only)
        public void setClusterRigidHardness(float hardness) {
            setClusterRigidHardness(objectId, hardness);
        }

        private native void setClusterRigidHardness(long bodyId, float hardness);

        public float getClusterRigidHardness() {
            return getClusterRigidHardness(objectId);
        }

        private native float getClusterRigidHardness(long bodyId);

//     btScalar kSKHR_CL; // Soft vs kinetic hardness [0,1] (cluster only)
        public void setClusterKineticHardness(float hardness) {
            setClusterKineticHardness(objectId, hardness);
        }

        private native void setClusterKineticHardness(long bodyId, float hardness);

        public float getClusterKineticHardness() {
            return getClusterKineticHardness(objectId);
        }

        private native float getClusterKineticHardness(long bodyId);

//     btScalar kSSHR_CL; // Soft vs soft hardness [0,1] (cluster only)
        public void setClusterSoftHardness(float hardness) {
            setClusterSoftHardness(objectId, hardness);
        }

        private native void setClusterSoftHardness(long bodyId, float hardness);

        public float getClusterSoftHardness() {
            return getClusterSoftHardness(objectId);
        }

        private native float getClusterSoftHardness(long bodyId);

//     btScalar kSR_SPLT_CL; // Soft vs rigid impulse split [0,1] (cluster only)
        public void setClusterRigidImpulseSplitCoef(float coef) {
            setClusterRigidImpulseSplitCoef(objectId, coef);
        }

        private native void setClusterRigidImpulseSplitCoef(long bodyId, float coef);

        public float getClusterRigidImpulseSplitCoef() {
            return getClusterRigidImpulseSplitCoef(objectId);
        }

        private native float getClusterRigidImpulseSplitCoef(long bodyId);

//     btScalar kSK_SPLT_CL; // Soft vs kinetic impulse split [0,1] (cluster only)
        public void setClusterKineticImpulseSplitCoef(float coef) {
            setClusterKineticImpulseSplitCoef(objectId, coef);
        }

        private native void setClusterKineticImpulseSplitCoef(long bodyId, float coef);

        public float getClusterKineticImpulseSplitCoef() {
            return getClusterKineticImpulseSplitCoef(objectId);
        }

        private native float getClusterKineticImpulseSplitCoef(long bodyId);

//     btScalar kSS_SPLT_CL; // Soft vs soft impulse split [0,1] (cluster only)
        public void setClusterSoftImpulseSplitCoef(float coef) {
            setClusterSoftImpulseSplitCoef(objectId, coef);
        }

        private native void setClusterSoftImpulseSplitCoef(long bodyId, float coef);

        public float getClusterSoftImpulseSplitCoef() {
            return getClusterSoftImpulseSplitCoef(objectId);
        }

        private native float getClusterSoftImpulseSplitCoef(long bodyId);

//     btScalar maxvolume; // Maximum volume ratio for pose
        public void setMaximumVolumeRatio(float ratio) {
            setMaximumVolumeRatio(objectId, ratio);
        }

        private native void setMaximumVolumeRatio(long bodyId, float ratio);

        public float getMaximumVolumeRatio() {
            return getMaximumVolumeRatio(objectId);
        }

        private native float getMaximumVolumeRatio(long bodyId);

//     btScalar timescale; // Time scale
        public void setTimeScale(float scale) {
            setTimeScale(objectId, scale);
        }

        private native void setTimeScale(long bodyId, float scale);

        public float getTimeScale() {
            return getTimeScale(objectId);
        }

        private native float getTimeScale(long bodyId);

//     int	viterations; // Velocities solver iterations
        public void setVelocitiesIterations(int iterations) {
            setVelocitiesIterations(objectId, iterations);
        }

        private native void setVelocitiesIterations(long objectId, int iteration);

        public int getVelocitiesIterations() {
            return getVelocitiesIterations(objectId);
        }

        private native int getVelocitiesIterations(long objectId);

//     -> int	piterations; // Positions solver iterations
        public void setPositionIterations(int iterations) {
            setPositionIterations(objectId, iterations);
        }

        private native void setPositionIterations(long objectId, int iteration);

        public int getPositionIterations() {
            return getPositionIterations(objectId);
        }

        private native int getPositionIterations(long objectId);

//     int	diterations; // Drift solver iterations
        public void setDriftIterations(int iterations) {
            setDriftIterations(objectId, iterations);
        }

        private native void setDriftIterations(long objectId, int iteration);

        public int getDriftIterations() {
            return getDriftIterations(objectId);
        }

        private native int getDriftIterations(long objectId);

//     int	citerations; // Cluster solver iterations
        public void setClusterIterations(int iterations) {
            setClusterIterations(objectId, iterations);
        }

        private native void setClusterIterations(long objectId, int iteration);

        public int getClusterIterations() {
            return getClusterIterations(objectId);
        }

        private native int getClusterIterations(long objectId);
//     int	collisions; // Collisions flags
        //public final class CollisionFlags {
        public final static int RVSmask = 0x000f; ///Rigid versus soft mask
        public final static int SDF_RS = 0x0001; ///SDF based rigid vs soft
        public final static int CL_RS = 0x0002; ///Cluster vs convex rigid vs soft
        public final static int SVSmask = 0x0030; ///Rigid versus soft mask
        public final static int VF_SS = 0x0010; ///Vertex vs face soft vs soft handling
        public final static int CL_SS = 0x0020; ///Cluster vs cluster soft vs soft handling
        public final static int CL_SELF = 0x0040; ///Cluster soft body self collision
        // presets 
        public final static int Default = SDF_RS;
        //};

        public void setCollisionsFlags(int flags) {
            setCollisionsFlags(objectId, flags);
        }

        private native void setCollisionsFlags(long bodyId, int flags);

        public int getCollisionsFlags() {
            return getCollisionFlags(objectId);
        }

        private native int getCollisionFlags(long bodyId);

//     tVSolverArray m_vsequence; // Velocity solvers sequence
//     tPSolverArray m_psequence; // Position solvers sequence
//     tPSolverArray m_dsequence; // Drift solvers sequence
    };

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
        if (updateNormals) {
            store.getBuffer(Type.Normal).setUpdateNeeded();
        }
    }

    private static native void updateMesh(long bodyId, FloatBuffer vertices, int numVertices, boolean updateNormals, FloatBuffer normals);

    /*============*
     * data Struct
     *============*/
    public final class Material {

        private long materialId;

        /*public Material(float linearStiffnessFactor, float angularStiffnessFactor, float volumeStiffnessFactor, int flags) {
         this();
         setLinearStiffnessFactor(materialId, linearStiffnessFactor);
         setAngularStiffnessFactor(materialId, angularStiffnessFactor);
         setVolumeStiffnessFactor(materialId, volumeStiffnessFactor);
         }

         private Material() {
         this.materialId = createMaterial();
         }*/
        protected Material(long nativeId) {
            this.materialId = nativeId;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj.getClass() == Material.class) {
                return this.materialId == ((Material) obj).materialId;
            } else {
                return false;
            }
        }

        private native long createMaterial();

        /**
         * @return the linearStiffnessFactor
         */
        public float getLinearStiffnessFactor() {
            return getLinearStiffnessFactor(materialId);
        }

        private native float getLinearStiffnessFactor(long materialId);

        /**
         * @param linearStiffnessFactor the linearStiffnessFactor to set
         */
        public void setLinearStiffnessFactor(float linearStiffnessFactor) {
            setLinearStiffnessFactor(materialId, linearStiffnessFactor);
        }

        private native void setLinearStiffnessFactor(long materialId, float linearStiffnessFactor);

        /**
         * @return the angularStiffnessFactor
         */
        public float getAngularStiffnessFactor() {
            return getAngularStiffnessFactor(materialId);
        }

        private native float getAngularStiffnessFactor(long materialId);

        /**
         * @param angularStiffnessFactor the angularStiffnessFactor to set
         */
        public void setAngularStiffnessFactor(float angularStiffnessFactor) {
            setAngularStiffnessFactor(materialId, angularStiffnessFactor);
        }

        private native void setAngularStiffnessFactor(long materialId, float angularStiffnessFactor);

        /**
         * @return the volumeStiffnessFactor
         */
        public float getVolumeStiffnessFactor() {
            return getVolumeStiffnessFactor(materialId);
        }

        private native float getVolumeStiffnessFactor(long materialId);

        /**
         * @param volumeStiffnessFactor the volumeStiffnessFactor to set
         */
        public void setVolumeStiffnessFactor(float volumeStiffnessFactor) {
            setVolumeStiffnessFactor(materialId, volumeStiffnessFactor);
        }

        private native void setVolumeStiffnessFactor(long materialId, float volumeStiffnessFactor);

        /**
         * @return the flags
         */
        public int getFlags() {
            return getFlags(materialId);
        }

        private native int getFlags(long materialId);

        /**
         * @param flags the flags to set
         */
        public void setFlags(int flags) {
            setFlags(materialId, flags);
        }

        private native void setFlags(long materialId, int flags);

    }
}
