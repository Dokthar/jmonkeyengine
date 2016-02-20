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
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitorAdapter;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.Control;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author dokthar
 */
public class SoftBodyControl extends PhysicsSoftBody implements PhysicsControl {

    protected Spatial spatial;
    private Mesh mesh;
    private boolean enabled = true;
    private boolean added = false;
    private PhysicsSoftSpace space = null;
    private boolean doNormalUpdate = false;
    private boolean meshInLocalOrigin = true;
    private boolean meshHaveNormal = false;

    public SoftBodyControl() {
    }

    public SoftBodyControl(boolean doNormalUpdate) {
        this.doNormalUpdate = doNormalUpdate;
    }

    public SoftBodyControl(boolean meshInLocalOrigin, boolean doNormalUpdate) {
        this.meshInLocalOrigin = meshInLocalOrigin;
        this.doNormalUpdate = doNormalUpdate;
    }

    @Override
    public Control cloneForSpatial(Spatial spatial) {
        SoftBodyControl control = new SoftBodyControl(meshInLocalOrigin, doNormalUpdate);
        control.rebuildFromMesh(mesh);
        control.mesh = mesh;

        control.setMasses(getMasses());
        control.setRestLengthScale(getRestLengthScale());
        int nbCluster = getClusterCount();
        if (nbCluster > 0) {
            control.generateClusters(getClusterCount());
        }

        control.setPhysicsLocation(getPhysicsLocation());

        control.config().copyValues(config());

        control.material().setAngularStiffnessFactor(material().getAngularStiffnessFactor());
        control.material().setLinearStiffnessFactor(material().getLinearStiffnessFactor());
        control.material().setVolumeStiffnessFactor(material().getVolumeStiffnessFactor());

        return control;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        //must get a mesh and create a softbody with it
        if (spatial != null && this.spatial != spatial) {
            this.spatial = spatial;
            if (mesh == null) {
                this.mesh = getFirstGeometry(spatial).getMesh();
                this.meshHaveNormal = doHaveNormalBuffer(this.mesh);
                createFromMesh(mesh);
                setPhysicsLocation(spatial.getWorldTranslation());
                //setPhysicsRotation(spatial.getWorldRotation());
            }
        }
    }

    private static boolean doHaveNormalBuffer(Mesh mesh) {
        return mesh != null && mesh.getBuffer(VertexBuffer.Type.Normal) != null;
    }

    private Geometry getFirstGeometry(Spatial spatial) {
        if (spatial instanceof Geometry) {
            return (Geometry) spatial;
        } else if (!(spatial instanceof Node)) {
            return null;
        }
        final List<Geometry> geoms = new LinkedList<Geometry>();
        Node node = (Node) spatial;
        node.depthFirstTraversal(new SceneGraphVisitorAdapter() {
            @Override
            public void visit(Geometry geom) {
                if (geoms.isEmpty()) {
                    geoms.add(geom);
                }
            }
        });
        return (geoms.isEmpty()) ? null : (Geometry) geoms.remove(0);
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (space != null) {
            if (enabled && !added) {
                if (spatial != null) {
                    setPhysicsLocation(spatial.getWorldTranslation());
                    //setPhysicsRotation(getSpatialRotation());
                }
                space.addCollisionObject(this);
                added = true;
            } else if (!enabled && added) {
                space.removeCollisionObject(this);
                added = false;
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void update(float tpf) {
        if (enabled && spatial != null) {
            if (meshInLocalOrigin) {
                this.spatial.setLocalTranslation(this.getBoundingCenter());
            }
            if (mesh != null) {
                switch (mesh.getMode()) {
                    case Triangles:
                        this.updateTriMesh(mesh, meshInLocalOrigin, doNormalUpdate && meshHaveNormal);
                        break;
                    case Lines:
                        this.updateTriMesh(mesh, meshInLocalOrigin, false);
                        break;
                }
                spatial.updateModelBound();
            }
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

    /**
     * Only used internally, do not call.
     *
     * @param space, a PhysicsSpace that extends PhysicsSoftSpace.
     * @throws IllegalArgumentException, if the space isn't a PhysicsSoftSpace.
     */
    @Override
    public void setPhysicsSpace(PhysicsSpace space) {
        if (space instanceof PhysicsSoftSpace) {
            setPhysicsSpace((PhysicsSoftSpace) space);
        } else {
            throw new IllegalArgumentException("Setting a PhysicsSpace to a SoftBodyControl must be a PhysicsSoftSpace");
        }
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);

        capsule.write(enabled, "enabled", true);
        capsule.write(spatial, "spatial", null);
        capsule.write(mesh, "mesh", null);

        capsule.write(meshInLocalOrigin, "meshInLocalOrigin", true);
        capsule.write(doNormalUpdate, "doNormalUpdate", false);

    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);

        enabled = capsule.readBoolean("enabled", true);
        spatial = (Spatial) capsule.readSavable("spatial", null);
        mesh = (Mesh) capsule.readSavable("mesh", null);

        meshInLocalOrigin = capsule.readBoolean("meshInLocalorigin", true);
        doNormalUpdate = capsule.readBoolean("doNormalUpdate", false);
        meshHaveNormal = doHaveNormalBuffer(mesh);
    }

}
