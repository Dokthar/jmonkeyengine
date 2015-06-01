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
 *
 * @author dokthar
 */
public class SoftBodyWorldInfo {

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

        public SoftBodyWorldInfo(SoftBodyWorldInfo worldInfo) {
            setSoftBodyWorldInfo(worldInfo);
        }

        public final void setSoftBodyWorldInfo(SoftBodyWorldInfo worldInfo) {
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
