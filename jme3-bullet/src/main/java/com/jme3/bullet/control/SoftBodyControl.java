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
package com.jme3.bullet.control;

import com.jme3.bullet.PhysicsSoftSpace;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.Control;
import java.io.IOException;

/**
 *
 * @author dokthar
 */
public class SoftBodyControl extends PhysicsSoftBody implements PhysicsControl {

    protected Spatial spatial;
    private Mesh mesh;
    protected boolean enabled = true;
    protected boolean added = false;
    protected PhysicsSoftSpace space = null;
    //protected boolean kinematicSpatial = true;
    protected boolean doNormalUpdate = true;
    private boolean meshHaveNormal = false;

    public SoftBodyControl() {
    }

    public SoftBodyControl(boolean doNormalUpdate) {
        this.doNormalUpdate = doNormalUpdate;
    }

    @Override
    public Control cloneForSpatial(Spatial spatial) {
//@TODO
        return null;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        //must get a mesh and create a softbody with it
        if (this.spatial != spatial) {
            this.spatial = spatial;
            this.mesh = getFirstGeometry(spatial).getMesh();
            this.meshHaveNormal = doHaveNormalBuffer(this.mesh);
            rebuildFromTriMesh(mesh);
        }
    }

    private static boolean doHaveNormalBuffer(Mesh mesh){
        return mesh.getBuffer(VertexBuffer.Type.Normal) != null;
    }
    
    private static Geometry getFirstGeometry(Spatial spatial) {
        if (spatial instanceof Geometry) {
            return (Geometry) spatial;
        }
        else if (spatial instanceof Node) {
            Node n = (Node) spatial;
            for (Spatial s : n.getChildren()) {
                return getFirstGeometry(s);
            }
        }
        return null;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (space != null) {
            /*if (enabled && !added) {
             if (spatial != null) {
             setPhysicsLocation(getSpatialTranslation());
             setPhysicsRotation(getSpatialRotation());
             }
             space.addCollisionObject(this);
             added = true;
             } else if (!enabled && added) {
             space.removeCollisionObject(this);
             added = false;
             }*/
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void update(float tpf) {
        if (enabled && spatial != null && mesh != null) {
            /*if (isKinematic() && kinematicSpatial) {
             super.setPhysicsLocation(getSpatialTranslation());
             super.setPhysicsRotation(getSpatialRotation());
             } else {
             getMotionState().applyTransform(spatial);
             }*/
            PhysicsSoftBody.updateMesh(this, mesh, doNormalUpdate && meshHaveNormal);
        }
    }

    @Override
    public void render(RenderManager rm, ViewPort vp) {
    }

    public void setPhysicsSpace(PhysicsSoftSpace space) {
        if (space == null) {
            if (this.space != null) {
                this.space.removeCollisionObject(this);
                added = false;
            }
        } else {
            if (this.space == space) {
                return;
            }
            space.addCollisionObject(this);
            added = true;
        }
        this.space = space;
    }

    @Override
    public PhysicsSoftSpace getPhysicsSpace() {
        return space;
    }

    @Override
    public void setPhysicsSpace(PhysicsSpace space) {
        if (space instanceof PhysicsSoftSpace) {
            setPhysicsSpace((PhysicsSoftSpace) space);
        } else {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates
        }
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
    }

    @Override
    public void read(JmeImporter im) throws IOException {
    }

}
