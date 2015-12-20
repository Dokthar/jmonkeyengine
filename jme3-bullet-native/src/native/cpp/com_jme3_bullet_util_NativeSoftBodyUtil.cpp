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

/**
 * Author: Dokthar
 */
#include "com_jme3_bullet_util_NativeSoftBodyUtil.h"
#include "jmeBulletUtil.h"
#include "BulletSoftBody/btSoftBody.h"

#ifdef __cplusplus
extern "C" {
#endif

    /*
     * Class:     com_jme3_bullet_util_NativeSoftBodyUtil
     * Method:    getVertices
     * Signature: (JLcom/jme3/bullet/util/DebugMeshCallback;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_util_NativeSoftBodyUtil_getVertices
    (JNIEnv *env, jclass clazz, jlong bodyId, jobject callback) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }

        btVector3 vertexA, vertexB, vertexC;

        for (int i = 0; i < body->m_faces.size(); i++) {
            const btSoftBody::Face& f = body->m_faces[i];

            // Grab the data for this triangle from the hull
            vertexA = f.m_n[0]->m_x;
            vertexB = f.m_n[1]->m_x;
            vertexC = f.m_n[2]->m_x;


            // Put the vertices into the vertex buffer
            env->CallVoidMethod(callback, jmeClasses::DebugMeshCallback_addVector, vertexA.getX(), vertexA.getY(), vertexA.getZ());
            if (env->ExceptionCheck()) {
                env->Throw(env->ExceptionOccurred());
                return;
            }
            env->CallVoidMethod(callback, jmeClasses::DebugMeshCallback_addVector, vertexB.getX(), vertexB.getY(), vertexB.getZ());
            if (env->ExceptionCheck()) {
                env->Throw(env->ExceptionOccurred());
                return;
            }
            env->CallVoidMethod(callback, jmeClasses::DebugMeshCallback_addVector, vertexC.getX(), vertexC.getY(), vertexC.getZ());
            if (env->ExceptionCheck()) {
                env->Throw(env->ExceptionOccurred());
                return;
            }
        }
    }

    /*
     * Class:     com_jme3_bullet_util_NativeSoftBodyUtil
     * Method:    getIndexes
     * Signature: (JLjava/nio/IntBuffer;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_util_NativeSoftBodyUtil_getIndexes
    (JNIEnv *env, jclass clazz, jlong bodyId, jobject indexBuffer) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jint* indexes = (jint*) env->GetDirectBufferAddress(indexBuffer);

        btSoftBody::Node* firstNode = &body->m_nodes[0];

        for (int i = 0; i < body->m_faces.size(); i++) {
            const btSoftBody::Face& f = body->m_faces[i];
            indexes[i * 3 + 0] = int(f.m_n[0] - firstNode);
            indexes[i * 3 + 1] = int(f.m_n[1] - firstNode);
            indexes[i * 3 + 2] = int(f.m_n[2] - firstNode);
        }

    }

    /*
     * Class:     com_jme3_bullet_util_NativeSoftBodyUtil
     * Method:    getNumTriangle
     * Signature: (J)I
     */
    JNIEXPORT jint JNICALL Java_com_jme3_bullet_util_NativeSoftBodyUtil_getNumTriangle
    (JNIEnv *env, jclass clazz, jlong bodyId) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return body->m_faces.size();
    }

    /*
     * Class:     com_jme3_bullet_util_NativeSoftBodyUtil
     * Method:    getNbVertices
     * Signature: (J)I
     */
    JNIEXPORT jint JNICALL Java_com_jme3_bullet_util_NativeSoftBodyUtil_getNbVertices
    (JNIEnv *env, jclass clazz, jlong bodyId) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        
        return body->m_nodes.size();
    }

    /*
     * Class:     com_jme3_bullet_util_NativeSoftBodyUtil
     * Method:    updateDebugMesh
     * Signature: (JLjava/nio/FloatBuffer;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_util_NativeSoftBodyUtil_updateDebugMesh
    (JNIEnv *env, jclass clazz, jlong bodyId, jobject verticesBuffer) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }

        jfloat* vertices = (jfloat*) env->GetDirectBufferAddress(verticesBuffer);

        for (int i = 0; i < body->m_nodes.size(); ++i) {
            const btSoftBody::Node& n = body->m_nodes[i];
            vertices[i * 3 + 0] = n.m_x.getX();
            vertices[i * 3 + 1] = n.m_x.getY();
            vertices[i * 3 + 2] = n.m_x.getZ();
        }
    }

#ifdef __cplusplus
}
#endif
