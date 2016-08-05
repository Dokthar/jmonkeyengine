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
package com.jme3.bullet.control;

import com.jme3.bullet.PhysicsSoftSpace;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.bullet.util.NativeSoftBodyUtil;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Quaternion;
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
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.TempVars;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
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

    private boolean meshHaveNormal = false;
    private IntBuffer jmeToBulletMap = null;

    private boolean meshInLocalOrigin;
    private boolean updateNormals;
    private boolean removeDuplicatedVertex;

    public SoftBodyControl() {
        this(false, false, true);
    }

    public SoftBodyControl(boolean updateNormals) {
        this(false, updateNormals, true);
    }

    public SoftBodyControl(boolean meshInLocalOrigin, boolean doNormalUpdate) {
        this(meshInLocalOrigin, doNormalUpdate, true);
    }

    /**
     * The softbody mesh is automatically generated from the first geometry
     * encountered when the Control is added to a Spatial.
     *
     * @param meshInLocalOrigin update vertexes position into the "localSapce"
     * of the body, (ie the bullet's bounding box center) and move the spatial
     * too.
     * @param doNormalUpdate boolean for updating the normal buffer as the same
     * time
     * @param removeDuplicatedVertex remove duplicated vertex when creating the
     * softbody, see {@link NativeSoftBodyUtil#generateIndexMap }
     */
    public SoftBodyControl(boolean meshInLocalOrigin, boolean doNormalUpdate, boolean removeDuplicatedVertex) {
        this.meshInLocalOrigin = meshInLocalOrigin;
        this.updateNormals = doNormalUpdate;
        this.removeDuplicatedVertex = removeDuplicatedVertex;
    }

    @Override
    public void createSoftBody(FloatBuffer positions, IndexBuffer links, IndexBuffer triangles, IndexBuffer tetras) {
        createSoftBody(positions, links, triangles, tetras, removeDuplicatedVertex);
    }

    public void createSoftBody(FloatBuffer positions, IndexBuffer links, IndexBuffer triangles, IndexBuffer tetras, boolean removeDuplicatedPositions) {
        if (removeDuplicatedPositions) {
            jmeToBulletMap = NativeSoftBodyUtil.generateIndexMap(positions);
            positions = NativeSoftBodyUtil.mapBulletPositions(jmeToBulletMap, positions);
            if (links != null) {
                links = NativeSoftBodyUtil.cloneMapBulletIndex(jmeToBulletMap, links);
            }
            if (triangles != null) {
                triangles = NativeSoftBodyUtil.cloneMapBulletIndex(jmeToBulletMap, triangles);
            }
            if (tetras != null) {
                tetras = NativeSoftBodyUtil.cloneMapBulletIndex(jmeToBulletMap, tetras);
            }
        } else {
            jmeToBulletMap = null;
        }

        super.createSoftBody(positions, links, triangles, tetras);
    }

    private void buildFromMesh(Mesh mesh) {
        newEmptySoftBody();

        this.mesh = mesh;
        this.meshHaveNormal = doHaveNormalBuffer(mesh);

        if (mesh.getMode() == Mesh.Mode.Lines) {
            NativeSoftBodyUtil.createRopeFromMesh(mesh, this);
        } else if (mesh.getMode() == Mesh.Mode.Triangles) {
            NativeSoftBodyUtil.createFromTriMesh(mesh, this);
        }
    }

    private void rebuildFromMesh(Mesh mesh) {
        if (mesh != null) {

            boolean wasInWorld = objectId != 0 && added;
            if (objectId != 0) {
                // remove the body from the physics space and detroy the native object
                if (wasInWorld) {
                    space.remove(this);
                }
                destroySoftBody();
            }

            buildFromMesh(mesh);

            if (wasInWorld) {
                space.add(this);
            }
        }
    }

    @Override
    public Control cloneForSpatial(Spatial spatial) {
        SoftBodyControl control = new SoftBodyControl(meshInLocalOrigin, updateNormals);
        control.rebuildFromMesh(mesh);

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
                buildFromMesh(mesh);
                setPhysicsLocation(spatial.getWorldTranslation());
                setPhysicsRotation(spatial.getWorldRotation());
                Spatial parent = spatial.getParent();
                if (parent != null) {
                    Quaternion rot = parent.getWorldRotation().inverse();
                    //rot.multLocal(Quaternion.IDENTITY);
                    spatial.setLocalRotation(rot);
                } else {
                    spatial.setLocalRotation(new Quaternion(Quaternion.IDENTITY));
                }
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
                    setPhysicsRotation(spatial.getWorldRotation());
                    TempVars vars = TempVars.get();
                    try {
                        if (meshInLocalOrigin) {
                            getBoundingCenter(vars.vect1);
                            spatial.getParent().worldToLocal(vars.vect1, vars.vect2);
                            spatial.setLocalTranslation(vars.vect2);
                        } else {
                            spatial.getParent().worldToLocal(Vector3f.ZERO, vars.vect2);
                            spatial.setLocalTranslation(vars.vect2);
                        }
                        if (!spatial.getWorldRotation().equals(Quaternion.IDENTITY)) {
                            Spatial parent = spatial.getParent();
                            if (parent != null) {
                                Quaternion rot = parent.getWorldRotation().inverse();
                                //rot.multLocal(Quaternion.IDENTITY);
                                spatial.setLocalRotation(rot);
                            } else {
                                spatial.setLocalRotation(new Quaternion(Quaternion.IDENTITY));
                            }
                        }
                    } finally {
                        vars.release();
                    }
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
            TempVars vars = TempVars.get();
            try {
                if (meshInLocalOrigin) {
                    getBoundingCenter(vars.vect1);
                    spatial.getParent().worldToLocal(vars.vect1, vars.vect2);
                    spatial.setLocalTranslation(vars.vect2);
                } else {
                    spatial.getParent().worldToLocal(Vector3f.ZERO, vars.vect2);
                    spatial.setLocalTranslation(vars.vect2);
                }
                if (!spatial.getWorldRotation().equals(Quaternion.IDENTITY)) {
                    Spatial parent = spatial.getParent();
                    if (parent != null) {
                        Quaternion rot = parent.getWorldRotation().inverse();
                        //rot.multLocal(Quaternion.IDENTITY);
                        spatial.setLocalRotation(rot);
                    } else {
                        spatial.setLocalRotation(new Quaternion(Quaternion.IDENTITY));
                    }
                }
                if (mesh != null) {
                    NativeSoftBodyUtil.updateMesh(this, jmeToBulletMap, mesh, meshInLocalOrigin, updateNormals && meshHaveNormal);
                    spatial.updateModelBound();
                }
            } finally {
                vars.release();
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
     * @param space a PhysicsSpace that extends PhysicsSoftSpace.
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

        capsule.write(meshInLocalOrigin, "meshInLocalOrigin", false);
        capsule.write(updateNormals, "updateNormals", false);
        capsule.write(removeDuplicatedVertex, "removeDuplicatedVertex", false);

    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);

        enabled = capsule.readBoolean("enabled", true);
        spatial = (Spatial) capsule.readSavable("spatial", null);
        mesh = (Mesh) capsule.readSavable("mesh", null);

        meshInLocalOrigin = capsule.readBoolean("meshInLocalorigin", false);
        updateNormals = capsule.readBoolean("updateNormals", false);
        removeDuplicatedVertex = capsule.readBoolean("removeDuplicatedVertex", false);

        meshHaveNormal = doHaveNormalBuffer(mesh);
    }

}
