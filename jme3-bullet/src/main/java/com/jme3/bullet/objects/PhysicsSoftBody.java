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

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;

/**
 *
 * @author dokthar
 */
public class PhysicsSoftBody extends PhysicsCollisionObject {

    //Constructor past form btSoftBody
    //btSoftBodyWorldInfo is a data struct stored into btSoftBody.h
    //public PhysicsSoftBody(SoftBodyWorldInfo worldInfo, int node_count, Vector3f x, float m) 
    public PhysicsSoftBody(CollisionShape shape) {
        collisionShape = shape;
        //rebuildRigidBody();
    }

//private SoftBodyWorldInfo softworldinfo;
    public PhysicsSoftBody() {
        initDefault();
    }

    public PhysicsSoftBody(SoftBodyWorldInfo worldInfo) {
        initDefault();
        setSoftBodyWorldInfo(worldInfo);
    }

    private void initDefault() {
        initDefault(objectId);
    }

    private native void initDefault(long objectId);

    public void setSoftBodyWorldInfo(SoftBodyWorldInfo worldinfo) {
    }

    private native void setSoftBodyWorldInfo(long objectId, SoftBodyWorldInfo worldinfo);

    public SoftBodyWorldInfo getSoftBodyWorldInfo() {
        SoftBodyWorldInfo worldInfo = new SoftBodyWorldInfo();
        getSoftBodyWorldInfo(objectId, worldInfo);
        return worldInfo;
    }

    private native void getSoftBodyWorldInfo(long objectId, SoftBodyWorldInfo worldInfo);

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

    }

    /* Add aero force to a node of the body */
    public void addAeroForceToNode(Vector3f windVelocity, int nodeIndex) {

    }
    /* Add aero force to a face of the body */

    public void addAeroForceToFace(Vector3f windVelocity, int faceIndex) {

    }

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
    public void setTransform(Transform trs) {

    }

    /* Translate */
    public void translate(Vector3f vec) {

    }
    /* Rotate */

    public void rotate(Quaternion rot) {

    }
    /* Scale */

    public void scale(Vector3f scl) {

    }

}

class SoftBodyWorldInfo {

    private float airDensity;
    private float waterDensity;
    private float waterOffset;
    private float maxDisplacement; //avoid soft body from 'exploding' so use some upper threshold of maximum motion that a node can travel per frame
    private Vector3f waterNormal;
    //private btBroadphaseInterface broadphase;
    //private btDispatcher dispatcher;
    private Vector3f gravity;
    //private btSparseSdf<3> sparseSdf; // <- SparseSdf can be avoided or at least just store the Cellsize (3) : private int sparseSdfSize;
    //SparseSdf is used by bullet but never modifyed

    public SoftBodyWorldInfo() {  //create with default values
        airDensity = 1.2f;
        waterDensity = 0;
        waterOffset = 0;
        maxDisplacement = 1000.f;
        waterNormal = Vector3f.ZERO;
        //broadphase(0);
        //dispatcher(0);
        gravity = Vector3f.UNIT_Y.mult(-10);
    }

    public void setSoftBodyWorldInfo(SoftBodyWorldInfo worldInfo) {
        this.airDensity = worldInfo.airDensity;
        this.waterDensity = worldInfo.waterDensity;
        this.waterOffset = worldInfo.waterOffset;
        this.maxDisplacement = worldInfo.maxDisplacement;
        this.waterNormal = worldInfo.waterNormal;
        this.gravity = worldInfo.gravity;
    }

    /**
     * @return the airDensity
     */
    public float getAirDensity() {
        return airDensity;
    }

    /**
     * @param airDensity the airDensity to set
     */
    public void setAirDensity(float airDensity) {
        this.airDensity = airDensity;
    }

    /**
     * @return the waterDensity
     */
    public float getWaterDensity() {
        return waterDensity;
    }

    /**
     * @param waterDensity the waterDensity to set
     */
    public void setWaterDensity(float waterDensity) {
        this.waterDensity = waterDensity;
    }

    /**
     * @return the waterOffset
     */
    public float getWaterOffset() {
        return waterOffset;
    }

    /**
     * @param waterOffset the waterOffset to set
     */
    public void setWaterOffset(float waterOffset) {
        this.waterOffset = waterOffset;
    }

    /**
     * @return the maxDisplacement
     */
    public float getMaxDisplacement() {
        return maxDisplacement;
    }

    /**
     * @param maxDisplacement the maxDisplacement to set
     */
    public void setMaxDisplacement(float maxDisplacement) {
        this.maxDisplacement = maxDisplacement;
    }

    /**
     * @return the waterNormal
     */
    public Vector3f getWaterNormal() {
        return waterNormal;
    }

    /**
     * @param waterNormal the waterNormal to set
     */
    public void setWaterNormal(Vector3f waterNormal) {
        this.waterNormal = waterNormal;
    }

    /**
     * @return the gravity
     */
    public Vector3f getGravity() {
        return gravity;
    }

    /**
     * @param gravity the gravity to set
     */
    public void setGravity(Vector3f gravity) {
        this.gravity = gravity;
    }
}


/*public PhysicsSoftBody(SoftBodyWorldInfo worldInfo, ) /*
 btSoftBody::btSoftBody(btSoftBodyWorldInfo* worldInfo,int node_count, const btVector3* x, const btScalar* m)
 :m_softBodySolver(0),m_worldInfo(worldInfo)
 {
 // Init 
 initDefaults();
 // Default material 
 Material* pm=appendMaterial();
 pm->m_kLST = 1;
 pm->m_kAST = 1;
 pm->m_kVST = 1;
 pm->m_flags = fMaterial::Default;
 // Nodes 
 const btScalar margin=getCollisionShape()->getMargin();
 m_nodes.resize(node_count);
 for(int i=0,ni=node_count;i<ni;++i)
 {
 Node& n=m_nodes[i];
 ZeroInitialize(n);
 n.m_x	= x?*x++:btVector3(0,0,0);
 n.m_q	= n.m_x;
 n.m_im	= m?*m++:1;
 n.m_im	= n.m_im>0?1/n.m_im:0;
 n.m_leaf	= m_ndbvt.insert(btDbvtVolume::FromCR(n.m_x,margin),&n);
 n.m_material= pm;
 }
 updateBounds();
 }
            
 */ /*
 btSoftBody::btSoftBody(btSoftBodyWorldInfo* worldInfo)
 :m_worldInfo(worldInfo)
 {
 initDefaults();
 }
 */
/*  
      
 public void setLinearVelocity(Vector3f vec){
        
 }

 public Vector3f getLinearVelocity(){
 return Vector3f.NAN;
 }
    
 public void setMass(float value, int node){
        
 }
    
 public float getMass(int node){
 return 0;
 }
    
 public void setTotalMass(float value){
        
 }
    
 */

/* USE CASE : 
      
 create a btSoftBody (psb) 
 add the SoftBody into a SoftDynamicsWorld
 psb.setVolumeMass
 psb.m_cfg.piterations 2 ?????????
      
 psb->m_cfg.collisions = 
 */
