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
package com.jme3.bullet.debug;

import com.jme3.bullet.PhysicsSoftSpace;
import static com.jme3.bullet.debug.BulletDebugAppState.logger;
import com.jme3.bullet.joints.SoftPhysicsJoint;
import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 *
 * @author dokthar
 */
public class BulletSoftBodyDebugAppState extends BulletDebugAppState {

    protected HashMap<PhysicsSoftBody, Spatial> softBodies = new HashMap<PhysicsSoftBody, Spatial>();
    protected HashMap<SoftPhysicsJoint, Spatial> softJoints = new HashMap<SoftPhysicsJoint, Spatial>();

    public BulletSoftBodyDebugAppState(PhysicsSoftSpace space) {
        super(space);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        updateSoftBodies();
        updateSoftJoints();
        physicsDebugRootNode.updateLogicalState(tpf);
        physicsDebugRootNode.updateGeometricState();
    }

    private void updateSoftBodies() {
        HashMap<PhysicsSoftBody, Spatial> oldObjects = softBodies;
        softBodies = new HashMap<PhysicsSoftBody, Spatial>();
        Collection<PhysicsSoftBody> current = ((PhysicsSoftSpace) space).getSoftBodyList();
        //create new map
        for (PhysicsSoftBody physicsObject : current) {
            //copy existing spatials
            if (oldObjects.containsKey(physicsObject)) {
                Spatial spat = oldObjects.get(physicsObject);
                softBodies.put(physicsObject, spat);
                oldObjects.remove(physicsObject);
            } else if (filter == null || filter.displayObject(physicsObject)) {
                logger.log(Level.FINE, "Create new debug SoftBody");
                //create new spatial
                Node node = new Node(physicsObject.toString());
                node.addControl(new BulletSoftBodyDebugControl(this, physicsObject));
                softBodies.put(physicsObject, node);
                physicsDebugRootNode.attachChild(node);
            }
        }
        //remove leftover spatials
        for (Map.Entry<PhysicsSoftBody, Spatial> entry : oldObjects.entrySet()) {
            PhysicsSoftBody object = entry.getKey();
            Spatial spatial = entry.getValue();
            spatial.removeFromParent();
        }
    }

    private void updateSoftJoints() {
        HashMap<SoftPhysicsJoint, Spatial> oldObjects = softJoints;
        softJoints = new HashMap<SoftPhysicsJoint, Spatial>();
        Collection<SoftPhysicsJoint> current = ((PhysicsSoftSpace) space).getSoftJointList();
        //create new map
        for (SoftPhysicsJoint physicsObject : current) {
            //copy existing spatials
            if (oldObjects.containsKey(physicsObject)) {
                Spatial spat = oldObjects.get(physicsObject);
                softJoints.put(physicsObject, spat);
                oldObjects.remove(physicsObject);
            } else if (filter == null || filter.displayObject(physicsObject)) {
                logger.log(Level.FINE, "Create new debug SoftJoint");
                //create new spatial
                Node node = new Node(physicsObject.toString());
                node.addControl(new BulletSoftJointDebugControl(this, physicsObject));
                softJoints.put(physicsObject, node);
                physicsDebugRootNode.attachChild(node);
            }
        }
        //remove leftover spatials
        for (Map.Entry<SoftPhysicsJoint, Spatial> entry : oldObjects.entrySet()) {
            SoftPhysicsJoint object = entry.getKey();
            Spatial spatial = entry.getValue();
            spatial.removeFromParent();
        }
    }
}
