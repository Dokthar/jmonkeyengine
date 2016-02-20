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
import com.jme3.bullet.control.SoftBodyControl;
import com.jme3.bullet.joints.SoftPhysicsJoint;
import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.bullet.objects.infos.SoftBodyWorldInfo;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
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
    private final Map<Long, PhysicsSoftBody> physicsSoftBodies = new ConcurrentHashMap<Long, PhysicsSoftBody>();
    private final Map<Long, SoftPhysicsJoint> physicsSoftJoints = new ConcurrentHashMap<Long, SoftPhysicsJoint>();

    /**
     * Get the current PhysicsSoftSpace <b>running on this thread</b><br> For
     * parallel physics, this can also be called from the OpenGL thread to
     * receive the PhysicsSoftSpace
     *
     * @return the PhysicsSoftSpace running on this thread
     */
    public static PhysicsSoftSpace getPhysicsSoftSpace() {
        return (PhysicsSoftSpace) physicsSpaceTL.get();
    }

    public PhysicsSoftSpace() {
        super();
    }

    public PhysicsSoftSpace(BroadphaseType broadphaseType) {
        super(broadphaseType);
    }

    public PhysicsSoftSpace(Vector3f worldMin, Vector3f worldMax) {
        super(worldMin, worldMax);
    }

    public PhysicsSoftSpace(Vector3f worldMin, Vector3f worldMax, BroadphaseType broadphaseType) {
        super(worldMin, worldMax, broadphaseType);
    }

    @Override
    public void create() {
        long id = createPhysicsSoftSpace(getWorldMin(), getWorldMax(), getBroadphaseType().ordinal(), false);
        setSpaceId(id);
        pQueueTL.set(pQueue);
        physicsSpaceTL.set(this);
    }

    private native long createPhysicsSoftSpace(Vector3f min, Vector3f max, int broadphaseType, boolean threading);

    @Override
    public void add(Object obj) {
        if (obj instanceof PhysicsSoftBody) {
            addSoftBody((PhysicsSoftBody) obj);
        } else if (obj instanceof SoftPhysicsJoint) {
            addSoftJoint((SoftPhysicsJoint) obj);
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
        } else if (obj instanceof SoftPhysicsJoint) {
            removeSoftJoint((SoftPhysicsJoint) obj);
        } else {
            super.remove(obj);
        }
    }

    ///removeCollisionObject will first check if it is a rigid body, if so call removeRigidBody otherwise call super (btDiscreteDynamicsWorld::removeCollisionObject)
    @Override
    public void removeCollisionObject(PhysicsCollisionObject obj) {
        if (obj instanceof PhysicsSoftBody) {
            removeSoftBody((PhysicsSoftBody) obj);
        } else {
            super.removeCollisionObject(obj);
        }
    }

    @Override
    public void addAll(Spatial spatial) {
        if (spatial.getControl(SoftBodyControl.class) != null) {
            SoftBodyControl physicsNode = spatial.getControl(SoftBodyControl.class);
            add(physicsNode);
            //add joints with physicsNode as BodyA
            List<SoftPhysicsJoint> joints = physicsNode.getJoints();
            for (SoftPhysicsJoint physicsJoint : joints) {
                if (physicsNode.equals(physicsJoint.getSoftBodyA())) {
                    add(physicsJoint);
                }
            }
        }
        super.addAll(spatial);
    }

    @Override
    public void removeAll(Spatial spatial) {
        if (spatial.getControl(SoftBodyControl.class) != null) {
            SoftBodyControl physicsNode = spatial.getControl(SoftBodyControl.class);
            //remove joints with physicsNode as BodyA
            List<SoftPhysicsJoint> joints = physicsNode.getJoints();
            for (SoftPhysicsJoint physicsJoint : joints) {
                if (physicsNode.equals(physicsJoint.getSoftBodyA())) {
                    removeSoftJoint(physicsJoint);
                }
            }
            remove(physicsNode);
        }
        super.removeAll(spatial);
    }

    private void addSoftBody(PhysicsSoftBody body) {
        if (physicsSoftBodies.containsKey(body.getObjectId())) {
            logger.log(Level.WARNING, "SoftBody {0} already exists in PhysicsSpace, cannot add.", body);
            return;
        }
        physicsSoftBodies.put(body.getObjectId(), body);
        logger.log(Level.FINE, "Adding SoftBody {0} to physics space.", body.getObjectId());
        //used to avoid having to set the SoftBodyWorldInfo in the SoftBody Constructor
        body.setSoftBodyWorldInfo(getWorldInfo());
        addSoftBody(getSpaceId(), body.getObjectId());
    }

    private native void addSoftBody(long space, long id);

    private void removeSoftBody(PhysicsSoftBody body) {
        if (!physicsSoftBodies.containsKey(body.getObjectId())) {
            logger.log(Level.WARNING, "SoftObject {0} does not exist in PhysicsSpace, cannot remove.", body);
            return;
        }
        physicsSoftBodies.remove(body.getObjectId());
        logger.log(Level.FINE, "Removing SoftBody {0} to physics space.", body.getObjectId());
        removeSoftBody(getSpaceId(), body.getObjectId());

    }

    private native void removeSoftBody(long space, long id);

    private void addSoftJoint(SoftPhysicsJoint joint) {
        if (physicsSoftJoints.containsKey(joint.getObjectId())) {
            logger.log(Level.WARNING, "Joint {0} already exists in PhysicsSpace, cannot add.", joint);
            return;
        }
        logger.log(Level.FINE, "Adding Joint {0} to physics space.", Long.toHexString(joint.getObjectId()));
        physicsSoftJoints.put(joint.getObjectId(), joint);
        //add the contraint at bullet level
        joint.addConstraint();
    }

    private void removeSoftJoint(SoftPhysicsJoint joint) {
        if (!physicsSoftJoints.containsKey(joint.getObjectId())) {
            logger.log(Level.WARNING, "Joint {0} does not exist in PhysicsSpace, cannot remove.", joint);
            return;
        }
        logger.log(Level.FINE, "Removing Joint {0} from physics space.", Long.toHexString(joint.getObjectId()));
        physicsSoftJoints.remove(joint.getObjectId());
        //remove the constraint at bullet level
        joint.removeConstraint();
    }

    public SoftBodyWorldInfo getWorldInfo() {
        long worlInfoId = getWorldInfo(getSpaceId());
        SoftBodyWorldInfo worldInfo = new SoftBodyWorldInfo(worlInfoId);
        return worldInfo;
    }

    private native long getWorldInfo(long objectId);

    public Collection<PhysicsSoftBody> getSoftBodyList() {
        return new LinkedList<PhysicsSoftBody>(physicsSoftBodies.values());
    }

    @Override
    public void destroy() {
        super.destroy(); //To change body of generated methods, choose Tools | Templates.
    }

}
