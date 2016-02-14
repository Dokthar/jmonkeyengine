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
package com.jme3.bullet.joints;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.export.*;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>
 * SoftPhysicsJoint - Basic Phyiscs Joint for softbody</p>
 *
 * @author dokthar
 */
public abstract class SoftPhysicsJoint extends PhysicsJoint {

    protected PhysicsSoftBody softA;
    protected PhysicsSoftBody softB;
    //protected PhysicsRigidBody rigidB;
    //protected Vector3f pivotA;
    //protected Vector3f pivotA;

    protected float errorReductionParameter = 1;
    protected float constraintForceMixing = 1;
    protected float split = 1;

    protected SoftPhysicsJoint() {
    }

    /**
     * @param nodeA
     * @param nodeB
     * @param pivotA local translation of the joint connection point in node A
     * @param pivotB local translation of the joint connection point in node B
     */
    public SoftPhysicsJoint(PhysicsSoftBody nodeA, PhysicsRigidBody nodeB, Vector3f pivotA, Vector3f pivotB) {
        this.softA = nodeA;
        this.nodeB = nodeB;
        this.nodeA = null;
        this.softB = null;
        this.pivotA = pivotA;
        this.pivotB = pivotB;
        softA.addJoint(this);
        nodeB.addJoint(this);
    }

    /**
     * @param nodeA
     * @param nodeB
     * @param pivotA local translation of the joint connection point in node A
     * @param pivotB local translation of the joint connection point in node B
     */
    public SoftPhysicsJoint(PhysicsSoftBody nodeA, PhysicsSoftBody nodeB, Vector3f pivotA, Vector3f pivotB) {
        this.softA = nodeA;
        this.softB = nodeB;
        this.nodeA = null;
        this.nodeB = null;
        this.pivotA = pivotA;
        this.pivotB = pivotB;
        softA.addJoint(this);
        softB.addJoint(this);
    }

    @Override
    public boolean isCollisionBetweenLinkedBodys() {
        return false;
    }

    /**
     * Get the error reduction parameter coefficient (aka ERP).
     * <p>
     * The ERP specifies what proportion of the joint error will be fixed during
     * the next simulation step.
     * </p>
     * <ul>
     * <li>If ERP=0 then no correcting force is applied and the bodies will
     * eventually drift apart as the simulation proceeds.
     * <li>If ERP = 1 then the simulation will attempt to fix all joint error
     * during the next time step. However, setting ERP=1 is not recommended, as
     * the joint error will not be completely fixed due to various internal
     * approximations.
     * </ul>
     * A value of ERP = 0.1 to 0.8 is recommended (1 is the default).
     *
     * @return the error reduction parameter value
     */
    public float getErrorReductionParameter() {
        return getErrorReductionParameter(objectId);
    }

    private native float getErrorReductionParameter(long jointId);

    /**
     * Set the error reduction parameter coefficient (aka ERP).
     * <p>
     * The ERP specifies what proportion of the joint error will be fixed during
     * the next simulation step.
     * </p>
     * <ul>
     * <li>If ERP=0 then no correcting force is applied and the bodies will
     * eventually drift apart as the simulation proceeds.
     * <li>If ERP = 1 then the simulation will attempt to fix all joint error
     * during the next time step. However, setting ERP=1 is not recommended, as
     * the joint error will not be completely fixed due to various internal
     * approximations.
     * </ul>
     * A value of ERP = 0.1 to 0.8 is recommended (1 is the default).
     *
     * @param erp the value to set, between [0,1].
     */
    public void setErrorReductionParameter(float erp) {
        setErrorReductionParameter(objectId, erp);
    }

    private native void setErrorReductionParameter(long jointId, float erp);

    /**
     * Get the constraint force mixing coefficient (aka CFM).
     * <ul>
     * <li>If CFM = 0 then the constraint will be hard.
     * <li>If CFM is set to a positive value, it will be possible to violate the
     * constraint by "pushing on it" (for example, for contact constraints by
     * forcing the two contacting objects together). In other words the
     * constraint will be soft, and the softness will increase as CFM increases.
     * </ul>
     * <p>
     * Note that setting CFM to a negative value can have undesirable bad
     * effects, such as instability. Don't do it.
     * </p>
     * (1 is the default).
     *
     * @return the constraint force mixing value
     */
    public float getConstraintForceMixing() {
        return getConstraintForceMixing(objectId);
    }

    private native float getConstraintForceMixing(long jointId);

    /**
     * Set the constraint force mixing coefficient (aka CFM).
     * <ul>
     * <li>If CFM = 0 then the constraint will be hard.
     * <li>If CFM is set to a positive value, it will be possible to violate the
     * constraint by "pushing on it" (for example, for contact constraints by
     * forcing the two contacting objects together). In other words the
     * constraint will be soft, and the softness will increase as CFM increases.
     * </ul>
     * <p>
     * Note that setting CFM to a negative value can have undesirable bad
     * effects, such as instability. Don't do it.
     * </p>
     * (1 is the default).
     *
     * @param cfm the value to set, between [0,+inf].
     */
    public void setConstraintForceMixing(float cfm) {
        setConstraintForceMixing(objectId, cfm);
    }

    private native void setConstraintForceMixing(long jointId, float cfm);

    public float getSplit() {
        return getSplit(objectId);
    }

    private native float getSplit(long jointId);

    public void setSplit(float split) {
        setSplit(objectId, split);
    }

    private native void setSplit(long jointId, float split);

    /**
     * The bodyA must be a SoftBody.
     *
     * @return null
     */
    @Override
    public PhysicsRigidBody getBodyA() {
        return super.getBodyA(); //To change body of generated methods, choose Tools | Templates.
    }

    public PhysicsSoftBody getSoftBodyA() {
        return softA;
    }

    public PhysicsSoftBody getSoftBodyB() {
        return softB;
    }

    public boolean isSoftSoftJoint() {
        return nodeB == null; // == (softB != null)
    }

    public boolean isSoftRigidJoint() {
        return nodeB != null;
    }

    /**
     * destroys this joint and removes it from its connected Body joint lists
     */
    @Override
    public void destroy() {
        getSoftBodyA().removeJoint(this);
        if (isSoftRigidJoint()) {
            getBodyB().removeJoint(this);
        } else {
            getSoftBodyB().removeJoint(this);
        }
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);

        capsule.write(nodeB, "nodeB", null);
        capsule.write(softA, "softA", null);
        capsule.write(softB, "softB", null);

        capsule.write(pivotA, "pivotA", Vector3f.ZERO);
        capsule.write(pivotB, "pivotB", Vector3f.ZERO);

        capsule.write(getConstraintForceMixing(), "constraintForceMixing", 1);
        capsule.write(getErrorReductionParameter(), "errorReductionParameter", 1);
        capsule.write(getSplit(), "split", 1);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);

        this.softA = (PhysicsSoftBody) capsule.readSavable("softA", null);
        this.softB = (PhysicsSoftBody) capsule.readSavable("softB", null);
        this.nodeB = (PhysicsRigidBody) capsule.readSavable("nodeB", null);

        this.pivotA = (Vector3f) capsule.readSavable("pivotA", Vector3f.ZERO);
        this.pivotB = (Vector3f) capsule.readSavable("pivotB", Vector3f.ZERO);

        this.constraintForceMixing = capsule.readFloat("constraintForceMixing", 1);
        this.errorReductionParameter = capsule.readFloat("errorReductionParameter", 1);
        this.split = capsule.readFloat("split", 1);

    }

    /**
     * Add the constraint to bodies. The constraint must be already created.
     * This method shouldn't be called in the user code, already called by the
     * SoftPhysicsSpace.
     */
    public void addConstraint() {
        addConstraint(objectId, softA.getObjectId());
    }

    private native void addConstraint(long jointId, long bodyId);

    /**
     * Remove the constraint to bodies. The constraint must be already added.
     * This method shouldn't be called in the user code, already called by the
     * SoftPhysicsSpace.
     */
    public void removeConstraint() {
        removeConstraint(objectId, softA.getObjectId());
    }

    private native void removeConstraint(long jointId, long bodyId);

    @Override
    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } catch (Exception e) { // XXX hack : call Object.finalize then catch the NPE exception throw by native finalize from PhysicsJoint
        }
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Finalizing Joint {0}", Long.toHexString(objectId));
        finalizeNative(objectId);

    }

    private native void finalizeNative(long objectId);
}
