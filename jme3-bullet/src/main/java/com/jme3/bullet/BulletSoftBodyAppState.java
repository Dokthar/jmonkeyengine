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

import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.debug.BulletSoftBodyDebugAppState;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dokthar
 */
public class BulletSoftBodyAppState extends BulletAppState {

    private boolean startPhysicsOnExecutor() {
        if (executor != null) {
            executor.shutdown();
        }
        executor = new ScheduledThreadPoolExecutor(1);
        final BulletSoftBodyAppState softApp = this;
        Callable<Boolean> call = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                detachedPhysicsLastUpdate = System.currentTimeMillis();
                pSpace = new PhysicsSoftSpace(worldMin, worldMax, broadphaseType);
                pSpace.addTickListener(softApp);
                return true;
            }
        };
        try {
            return executor.submit(call).get();
        } catch (InterruptedException ex) {
            Logger.getLogger(BulletSoftBodyAppState.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (ExecutionException ex) {
            Logger.getLogger(BulletSoftBodyAppState.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public PhysicsSoftSpace getPhysicsSoftSpace() {
        return (PhysicsSoftSpace) pSpace;
    }

    /**
     * The physics system is started automatically on attaching, if you want to
     * start it before for some reason, you can use this method.
     */
    @Override
    public void startPhysics() {
        if (initialized) {
            return;
        }
        //start physics thread(pool)
        if (threadingType == ThreadingType.PARALLEL) {
            startPhysicsOnExecutor();
        } else {
            pSpace = new PhysicsSoftSpace(worldMin, worldMax, broadphaseType);
        }
        pSpace.addTickListener(this);
        initialized = true;
    }

    @Override
    public void stateAttached(AppStateManager stateManager) {
        if (!initialized) {
            startPhysics();
        }
        if (threadingType == ThreadingType.PARALLEL) {
            PhysicsSpace.setLocalThreadPhysicsSpace(pSpace);
        }
        if (debugEnabled) {
            debugAppState = new BulletSoftBodyDebugAppState(getPhysicsSoftSpace());
            stateManager.attach(debugAppState);
        }
    }

    @Override
    public void update(float tpf) {
        if (debugEnabled && debugAppState == null && pSpace != null) {
            debugAppState = new BulletSoftBodyDebugAppState(getPhysicsSoftSpace());
            stateManager.attach(debugAppState);
        } else if (!debugEnabled && debugAppState != null) {
            stateManager.detach(debugAppState);
            debugAppState = null;
        }
        if (!active) {
            return;
        }
        pSpace.distributeEvents();
        this.tpf = tpf;
    }

}
