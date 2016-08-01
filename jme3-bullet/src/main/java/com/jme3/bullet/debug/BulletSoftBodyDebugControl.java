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
package com.jme3.bullet.debug;

import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.bullet.util.NativeSoftBodyUtil;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;

/**
 * This class can't be into the jme3-bullet common.
 *
 * @author dokthar
 */
public class BulletSoftBodyDebugControl extends AbstractPhysicsDebugControl {

    protected final PhysicsSoftBody body;
    protected final Vector3f location = new Vector3f();
    protected final Quaternion rotation = new Quaternion();
    protected final Geometry linksGeom;
    protected final Geometry facesGeom;

    protected final Material DEBUG_LINK;
    protected final Material DEBUG_FACE;

    public BulletSoftBodyDebugControl(BulletDebugAppState debugAppState, PhysicsSoftBody body) {
        super(debugAppState);
        DEBUG_LINK = debugAppState.DEBUG_RED.clone();
        DEBUG_LINK.setColor("Color", ColorRGBA.Orange);
        DEBUG_FACE = debugAppState.DEBUG_RED.clone();

        this.body = body;

        linksGeom = createDebugLinksShape(body);
        if (linksGeom != null) {
            linksGeom.setName(body.toString() + " debug softbody links");
            linksGeom.setMaterial(DEBUG_LINK);
            linksGeom.getMesh().setStreamed();
        }
        facesGeom = createDebugFacesShape(body);
        if (facesGeom != null) {
            facesGeom.setName(body.toString() + " debug softbody faces");
            facesGeom.setMaterial(DEBUG_FACE);
            facesGeom.getMesh().setStreamed();
        }
    }

    @Override
    public void setSpatial(Spatial spatial) {
        if (spatial != null && spatial instanceof Node) {
            Node node = (Node) spatial;
            if (linksGeom != null) {
                node.attachChild(linksGeom);
            }
            if (facesGeom != null) {
                node.attachChild(facesGeom);
            }
        } else if (spatial == null && this.spatial != null) {
            Node node = (Node) this.spatial;
            if (linksGeom != null) {
                node.detachChild(linksGeom);
            }
            if (facesGeom != null) {
                node.detachChild(facesGeom);
            }
        }
        super.setSpatial(spatial);
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (linksGeom != null) {
            NativeSoftBodyUtil.updateMesh(body, linksGeom.getMesh(), false, false);
            linksGeom.updateModelBound();
        }
        if (facesGeom != null) {
            NativeSoftBodyUtil.updateMesh(body, facesGeom.getMesh(), false, false);
            facesGeom.updateModelBound();
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    public static Geometry createDebugLinksShape(PhysicsSoftBody softBody) {
        if (softBody == null) {
            return null;
        }
        if (softBody.getNbLinks() > 0) {
            Geometry debugLinks = new Geometry();

            Mesh mesh = new Mesh();
            mesh.setBuffer(VertexBuffer.Type.Position, 3, softBody.getNodesPositions());
            mesh.setBuffer(VertexBuffer.Type.Index, 2, softBody.getLinksIndexes());
            mesh.setMode(Mesh.Mode.Lines);

            mesh.getFloatBuffer(VertexBuffer.Type.Position).clear();
            mesh.getIndexBuffer().getBuffer().clear();
            mesh.updateCounts();
            mesh.updateBound();

            debugLinks.setMesh(mesh);

            debugLinks.updateModelBound();
            debugLinks.updateGeometricState();
            return debugLinks;
        }
        return null;
    }

    public static Geometry createDebugFacesShape(PhysicsSoftBody softBody) {
        if (softBody == null) {
            return null;
        }
        if (softBody.getNbFaces() > 0) {
            Geometry debugFaces = new Geometry();

            Mesh mesh = new Mesh();
            mesh.setBuffer(VertexBuffer.Type.Position, 3, softBody.getNodesPositions());
            mesh.setBuffer(VertexBuffer.Type.Index, 3, softBody.getFacesIndexes());
            mesh.setMode(Mesh.Mode.Triangles);

            mesh.getFloatBuffer(VertexBuffer.Type.Position).clear();
            mesh.getIndexBuffer().getBuffer().clear();
            mesh.updateCounts();
            mesh.updateBound();

            debugFaces.setMesh(mesh);

            debugFaces.updateModelBound();
            debugFaces.updateGeometricState();
            return debugFaces;
        }
        return null;
    }
}
