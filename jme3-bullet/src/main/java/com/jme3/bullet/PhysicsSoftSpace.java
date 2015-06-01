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
package com.jme3.bullet;

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.bullet.objects.infos.SoftBodyWorldInfo;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dokthar
 */
public class PhysicsSoftSpace extends PhysicsSpace {

    private static final Logger logger = Logger.getLogger(PhysicsSpace.class.getName());
    private Map<Long, PhysicsSoftBody> softBodies = new ConcurrentHashMap<Long, PhysicsSoftBody>();

    /*
    
     btSoftBodyArray m_softBodies;
     int	m_drawFlags;
     bool	m_drawNodeTree;
     bool	m_drawFaceTree;
     bool	m_drawClusterTree;
     btSoftBodyWorldInfo m_sbi;
     ///Solver classes that encapsulate multiple soft bodies for solving
     btSoftBodySolver *m_softBodySolver;
     bool	m_ownsSolver;
     */
    /*
     public void addSoftBody(PhysicsSoftBody body,
     short int collisionFilterGroup=btBroadphaseProxy::DefaultFilter,
     short int collisionFilterMask=btBroadphaseProxy::AllFilter){
    
     }*/
    @Override
    public void add(Object obj) {
        if (obj instanceof PhysicsSoftBody) {
            addSoftBody((PhysicsSoftBody) obj);
        } else {
            super.add(obj);
        }
    }

    @Override
    public void addCollisionObject(PhysicsCollisionObject obj) {
        if (obj instanceof PhysicsSoftBody) {
            addSoftBody((PhysicsSoftBody) obj);
        } else {
            super.addCollisionObject(obj);
        }
    }

    @Override
    public void remove(Object obj) {
        if (obj instanceof PhysicsSoftBody) {
            removeSoftBody((PhysicsSoftBody) obj);
        } else {
            super.remove(obj);
        }
    }

    ///removeCollisionObject will first check if it is a rigid body, if so call removeRigidBody otherwise call btDiscreteDynamicsWorld::removeCollisionObject
    @Override
    public void removeCollisionObject(PhysicsCollisionObject obj) {
        if (obj instanceof PhysicsSoftBody) {
            removeSoftBody((PhysicsSoftBody) obj);
        } else {
            super.removeCollisionObject(obj);
        }
    }

    private void addSoftBody(PhysicsSoftBody body) {
        if (softBodies.containsKey(body.getObjectId())) {
            logger.log(Level.WARNING, "SoftBody {0} already exists in PhysicsSpace, cannot add.", body);
            return;
        }
        softBodies.put(body.getObjectId(), body);
        logger.log(Level.FINE, "Adding SoftBody {0} to physics space.", body.getObjectId());
        addSoftBody(getSpaceId(), body.getObjectId());
    }

    private native void addSoftBody(long space, long id);

    private void removeSoftBody(PhysicsSoftBody body) {
        if (!softBodies.containsKey(body.getObjectId())) {
            logger.log(Level.WARNING, "SoftObject {0} does not exist in PhysicsSpace, cannot remove.", body);
            return;
        }
        softBodies.remove(body.getObjectId());
        logger.log(Level.FINE, "Removing SoftBody {0} to physics space.", body.getObjectId());
        removeSoftBody(getSpaceId(), body.getObjectId());
        
    }

    private native void removeSoftBody(long space, long id);

    /*
    public int getDrawFlags() {
        return 0;
    }

    public void setDrawFlags(int f) {

    }*/

    public SoftBodyWorldInfo getWorldInfo() {
        SoftBodyWorldInfo worldInfo = new SoftBodyWorldInfo();
        getWorldInfo(getSpaceId(), worldInfo);
        return worldInfo;
    }

    private native void getWorldInfo(long objectId, SoftBodyWorldInfo worldInfo);

    /*
     virtual btDynamicsWorldType getWorldType() const
     {
     return	BT_SOFT_RIGID_DYNAMICS_WORLD;
     }
     */
    public Collection<PhysicsSoftBody> getSoftBodyList() {
        return new LinkedList<PhysicsSoftBody>(softBodies.values());
    }
}
/*
 public: from btSoftRidiWorld
 btSoftRigidDynamicsWorld(btDispatcher* dispatcher,btBroadphaseInterface* pairCache,btConstraintSolver* constraintSolver, btCollisionConfiguration* collisionConfiguration, btSoftBodySolver *softBodySolver = 0 );
 virtual ~btSoftRigidDynamicsWorld();
 virtual void	debugDrawWorld();

 virtual void rayTest(const btVector3& rayFromWorld, const btVector3& rayToWorld, RayResultCallback& resultCallback) const;
 /// rayTestSingle performs a raycast call and calls the resultCallback. It is used internally by rayTest.
 /// In a future implementation, we consider moving the ray test as a virtual method in btCollisionShape.
 /// This allows more customization.
 static void	rayTestSingle(const btTransform& rayFromTrans,const btTransform& rayToTrans,
 btCollisionObject* collisionObject,
 const btCollisionShape* collisionShape,
 const btTransform& colObjWorldTransform,
 RayResultCallback& resultCallback);
 virtual	void	serialize(btSerializer* serializer);
 */
