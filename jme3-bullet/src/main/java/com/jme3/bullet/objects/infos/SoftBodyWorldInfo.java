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
package com.jme3.bullet.objects.infos;

import com.jme3.math.Vector3f;

/**
 * The SoftBodyWorldInfo class store information of the world into each
 * softBody. By default all softBody used the same softBodyWorldInfo, which is
 * not by default on the native side.
 *
 * @author dokthar
 */
public class SoftBodyWorldInfo {

    private final long worldInfoId;

    /**
     * Create a SofBodyWorldInfo with default (natives) values. airDensity =
     * 1.2; waterDensity = 0; waterOffset = 0; maxDisplacement = 1000.f;
     * waterNormal = Vector3f.ZERO; gravity = Vector3f.UNIT_Y(-10);
     */
    public SoftBodyWorldInfo() {
        worldInfoId = createSoftBodyWorldInfo();
    }

    /**
     * <!> Used internally !
     *
     * @param worldId
     */
    public SoftBodyWorldInfo(long worldId) {
        worldInfoId = worldId;
    }

    private native long createSoftBodyWorldInfo();

    /**
     * Create a SoftBodyWorldInfo and copy the value of the param.
     *
     * @param worldInfo , the values used from.
     */
    public SoftBodyWorldInfo(SoftBodyWorldInfo worldInfo) {
        worldInfoId = createSoftBodyWorldInfo();
        setSoftBodyWorldInfo(worldInfo);
    }

    /**
     * Copy the values of the worldInfo into this.
     *
     * @param worldInfo
     */
    public final void setSoftBodyWorldInfo(SoftBodyWorldInfo worldInfo) {
        setSoftBodyWorldInfo(worldInfoId, worldInfo.getWorldInfoId());
    }

    private native long setSoftBodyWorldInfo(long worldInfoId, long copy);

    /**
     * Get the air density.
     *
     * @return the airDensity
     */
    public float getAirDensity() {
        return getAirDensity(worldInfoId);
    }

    private native float getAirDensity(long worldInfoId);

    /**
     * Set the air density.
     *
     * @param airDensity the airDensity to set
     */
    public void setAirDensity(float airDensity) {
        setAirDensity(worldInfoId, airDensity);
    }

    private native void setAirDensity(long worldInfoId, float airDensity);

    /**
     * Get the water density.
     *
     * @return the waterDensity
     */
    public float getWaterDensity() {
        return getWaterDensity(worldInfoId);
    }

    private native float getWaterDensity(long worldInfoId);

    /**
     * Set the water density.
     *
     * @param waterDensity the waterDensity to set
     */
    public void setWaterDensity(float waterDensity) {
        setWaterDensity(worldInfoId, waterDensity);
    }

    private native void setWaterDensity(long worldInfoId, float waterDensity);

    /**
     * Get the water offset.
     *
     * @return the waterOffset
     */
    public float getWaterOffset() {
        return getWaterOffset(worldInfoId);
    }

    private native float getWaterOffset(long worldInfoId);

    /**
     * Set the water offset.
     *
     * @param waterOffset the waterOffset to set
     */
    public void setWaterOffset(float waterOffset) {
        setWaterOffset(worldInfoId, waterOffset);
    }

    private native void setWaterOffset(long worldInfoId, float waterOffset);

    /**
     * Get the max displacement value.
     *
     * @return the maxDisplacement
     */
    public float getMaxDisplacement() {
        return getMaxDisplacement(worldInfoId);
    }

    private native float getMaxDisplacement(long worldInfoId);

    /**
     * Set the max displacement value.
     *
     * @param maxDisplacement the maxDisplacement to set
     */
    public void setMaxDisplacement(float maxDisplacement) {
        setMaxDisplacement(worldInfoId, maxDisplacement);
    }

    public native void setMaxDisplacement(long worldInfoId, float maxDisplacement);

    /**
     * Get the water normal.
     *
     * @return a Vector3f, the waterNormal
     */
    public Vector3f getWaterNormal() {
        Vector3f vec = new Vector3f();
        getWaterNormal(worldInfoId, vec);
        return vec;
    }

    private native void getWaterNormal(long worldInfoId, Vector3f vec);

    /**
     * Set the water normal.
     *
     * @param waterNormal the waterNormal to set
     */
    public void setWaterNormal(Vector3f waterNormal) {
        setWaterNormal(worldInfoId, waterNormal);
    }

    private native void setWaterNormal(long worldInfoId, Vector3f vec);

    /**
     * Get the gravity vector3f.
     *
     * @return a Vector3f, the gravity
     */
    public Vector3f getGravity() {
        Vector3f vec = new Vector3f();
        getGravity(worldInfoId, vec);
        return vec;
    }

    private native void getGravity(long worldInfoId, Vector3f vec);

    /**
     * Set the gravity Vector3f.
     *
     * @param gravity the gravity to set
     */
    public void setGravity(Vector3f gravity) {
        setGravity(worldInfoId, gravity);
    }

    private native void setGravity(long worldInfoId, Vector3f vec);

    /**
     * <!> used internally !
     *
     * @return the native Id of the SoftBodyWorldInfo
     */
    public long getWorldInfoId() {
        return worldInfoId;
    }
}
