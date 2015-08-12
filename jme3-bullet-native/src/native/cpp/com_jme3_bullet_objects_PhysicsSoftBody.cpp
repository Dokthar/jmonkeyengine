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
#include "com_jme3_bullet_objects_PhysicsSoftBody.h"
#include "jmeBulletUtil.h"
#include "BulletSoftBody/btSoftBody.h"
#include "BulletSoftBody/btSoftBodyHelpers.h"

#ifdef __cplusplus
extern "C" {
#endif

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    createFromTriMesh
     * Signature: (Lcom/jme3/scene/mesh/IntBuffer;Ljava/nio/FloatBuffer;IZ)J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_createFromTriMesh
    (JNIEnv *env, jobject object, jobject triangleIndexBuffer, jobject vertexBuffer, jint nbTriangles, jboolean random) {
        jmeClasses::initJavaClasses(env);
        btSoftBodyWorldInfo* worldInfo = new btSoftBodyWorldInfo();

        int* triangles = (int*) env->GetDirectBufferAddress(triangleIndexBuffer);
        float* vertices = (float*) env->GetDirectBufferAddress(vertexBuffer);

        btSoftBody* body = btSoftBodyHelpers::CreateFromTriMesh(*worldInfo, vertices, triangles, nbTriangles, &random);

        body->setUserPointer(NULL);
        return reinterpret_cast<jlong> (body);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    ctr_PhysicsSoftBody
     * Signature: ()J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_ctr_1PhysicsSoftBody__
    (JNIEnv *env, jobject object) {
        jmeClasses::initJavaClasses(env);
        btSoftBodyWorldInfo* wordInfo = new btSoftBodyWorldInfo();
        btSoftBody* body = new btSoftBody(wordInfo);
        body->setUserPointer(NULL);
        return reinterpret_cast<jlong> (body);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    ctr_PhysicsSoftBody
     * Signature: (I[Lcom/jme3/math/Vector3f;[F)J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_ctr_1PhysicsSoftBody__I_3Lcom_jme3_math_Vector3f_2_3F
    (JNIEnv *env, jobject object, jint nodeCount, jobjectArray vec, jfloatArray massArray) {
        jmeClasses::initJavaClasses(env);
        btSoftBodyWorldInfo* wordInfo = new btSoftBodyWorldInfo();

        //converting arrays
        btVector3 positions[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            jmeBulletUtil::convert(env, vec + i, positions + i);
        }
        jfloat *masses = env->GetFloatArrayElements(massArray, NULL);

        btSoftBody* body = new btSoftBody(wordInfo, nodeCount, positions, masses);
        body->setUserPointer(NULL);
        return reinterpret_cast<jlong> (body);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    initDefault
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_initDefault
    (JNIEnv *env, jobject object, jlong bodyId) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        body->initDefaults();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    setSoftBodyWorldInfo
     * Signature: (JJ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_setSoftBodyWorldInfo
    (JNIEnv *env, jobject object, jlong bodyId, jlong worldId) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }

        btSoftBodyWorldInfo* worldInfo = reinterpret_cast<btSoftBodyWorldInfo*> (worldId);
        if (worldInfo == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }

        body->m_worldInfo = worldInfo;
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    getSoftBodyWorldInfo
     * Signature: (J)J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_getSoftBodyWorldInfo
    (JNIEnv *env, jobject object, jlong bodyId) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }

        return reinterpret_cast<jlong> (body->getWorldInfo());
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    appendMaterial
     * Signature: (J)J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_appendMaterial
    (JNIEnv *env, jobject object, jlong bodyId) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        btSoftBody::Material* mat = body->appendMaterial();
        return reinterpret_cast<long> (mat);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    getMaterials
     * Signature: (J)[J
     */
    JNIEXPORT jlongArray JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_getMaterials
    (JNIEnv *env, jobject object, jlong bodyId) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return NULL;
        }

        int size = body->m_materials.size();

        jlongArray result;
        result = env->NewLongArray(size);
        if (result == NULL) {
            return NULL; /* out of memory error thrown */
        }
        // fill a temp structure to use to populate the java long array
        jlong fill[size];
        for (int i = 0; i < size; i++) {
            fill[i] = reinterpret_cast<long> (body->m_materials[i]);
        }
        // move from the temp structure to the java structure
        env->SetLongArrayRegion(result, 0, size, fill);
        return result;
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    addForce
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_addForce__JLcom_jme3_math_Vector3f_2
    (JNIEnv *env, jobject object, jlong bodyId, jobject force) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 vec = btVector3();
        jmeBulletUtil::convert(env, force, &vec);
        body->addForce(vec);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    addForce
     * Signature: (JLcom/jme3/math/Vector3f;I)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_addForce__JLcom_jme3_math_Vector3f_2I
    (JNIEnv *env, jobject object, jlong bodyId, jobject force, jint nodeId) {
        //@TODO
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    addAeroForceToNode
     * Signature: (JLcom/jme3/math/Vector3f;I)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_addAeroForceToNode
    (JNIEnv *env, jobject object, jlong bodyId, jobject force, jint nodeId) {
        //@TODO
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    addAeroForceToFace
     * Signature: (JLcom/jme3/math/Vector3f;I)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_addAeroForceToFace
    (JNIEnv *env, jobject object, jlong bodyId, jobject force, jint faceId) {
        //@TODO
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    setMass
     * Signature: (JIF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_setMass
    (JNIEnv *env, jobject object, jlong bodyId, jint nodeId, jfloat mass) {
        //@TODO
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    getMass
     * Signature: (JI)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_getMass
    (JNIEnv *env, jobject object, jlong bodyId, jint nodeId) {
        //@TODO
        return 0;
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    getTotalMass
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_getTotalMass
    (JNIEnv *env, jobject object, jlong bodyId) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }

        return body->getTotalMass();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    setTotalMass
     * Signature: (JFZ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_setTotalMass
    (JNIEnv *env, jobject object, jlong bodyId, jfloat mass, jboolean fromFaces) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }

        body->setTotalMass(mass, fromFaces);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    setTotalDensity
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_setTotalDensity
    (JNIEnv *env, jobject object, jlong bodyId, jfloat density) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }

        body->setTotalDensity(density);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    setVolumeMass
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_setVolumeMass
    (JNIEnv *env, jobject object, jlong bodyId, jfloat mass) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }

        body->setVolumeMass(mass);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    setVolumeDensity
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_setVolumeDensity
    (JNIEnv *env, jobject object, jlong bodyId, jfloat density) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }

        body->setVolumeDensity(density);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    setPhysicsTransform
     * Signature: (JLcom/jme3/math/Transform;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_setPhysicsTransform
    (JNIEnv *env, jobject object, jlong bodyId, jobject transform) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        // rotation still unavailable with btTransform
        btTransform trs = btTransform();
        jmeBulletUtil::convert(env, transform, &trs);
        // body->transform(body->m_initialWorldTransform.inverse());
        // body->transform(trs);
        body->transform(body->m_initialWorldTransform.inverse() * trs);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    getPhysicsTransform
     * Signature: (JLcom/jme3/math/Transform;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_getPhysicsTransform
    (JNIEnv *env, jobject object, jlong bodyId, jobject transform) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        // rotation still unavailable with btTransform
        jmeBulletUtil::convert(env, &body->m_initialWorldTransform, transform);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    setPhysicsLocation
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_setPhysicsLocation
    (JNIEnv *env, jobject object, jlong bodyId, jobject location) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 vec = btVector3();
        jmeBulletUtil::convert(env, location, &vec);
        // body->translate(body->m_initialWorldTransform.inverse().getOrigin());
        // body->translate(vec);
        body->translate(body->m_initialWorldTransform.inverse().getOrigin() * vec);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    getPhysicsLocation
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_getPhysicsLocation
    (JNIEnv *env, jobject object, jlong bodyId, jobject location) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, &body->getWorldTransform().getOrigin(), location);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    setPhysicsRotation
     * Signature: (JLcom/jme3/math/Quaternion;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_setPhysicsRotation
    (JNIEnv *env, jobject object, jlong bodyId, jobject rotation) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btMatrix3x3 rot = btMatrix3x3();
        jmeBulletUtil::convert(env, rotation, &rot);
        rot = body->m_initialWorldTransform.inverse().getBasis() * rot;
        btQuaternion quat;
        rot.getRotation(quat);
        body->rotate(quat);

    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    getPhysicsRotation
     * Signature: (JLcom/jme3/math/Quaternion;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_getPhysicsRotation
    (JNIEnv *env, jobject object, jlong bodyId, jobject rotation) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convertQuat(env, &body->getWorldTransform().getBasis(), rotation);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    setPhysicsScale
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_setPhysicsScale
    (JNIEnv *env, jobject object, jlong bodyId, jobject scale) {
        // not straightforward with softBody
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    getPhysicsScale
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_getPhysicsScale
    (JNIEnv *env, jobject object, jlong bodyId, jobject scale) {
        // not straightforward with softBody
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    getRestLenghtScale
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_getRestLenghtScale
    (JNIEnv *env, jobject object, jlong bodyId) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return body->getRestLengthScale();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    setRestLenghtScale
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_setRestLenghtScale
    (JNIEnv *env, jobject object, jlong bodyId, jfloat value) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        body->setRestLengthScale(value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    setPose
     * Signature: (JZZ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_setPose
    (JNIEnv *env, jobject object, jlong bodyId, jboolean bvolume, jboolean bframe) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        body->setPose(bvolume, bframe);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    resetLinkRestLengths
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_resetLinkRestLengths
    (JNIEnv *env, jobject object, jlong bodyId) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        body->resetLinkRestLengths();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    getVolume
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_getVolume
    (JNIEnv *env, jobject object, jlong bodyId) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return body->getVolume();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    getClusterCount
     * Signature: (J)I
     */
    JNIEXPORT jint JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_getClusterCount
    (JNIEnv *env, jobject object, jlong bodyId) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return body->clusterCount();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    getClusterCenterOfMass
     * Signature: (JILcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_getClusterCenterOfMass
    (JNIEnv *, jobject, jlong, jint, jobject);

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    randomizeConstraints
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_randomizeConstraints
    (JNIEnv *env, jobject object, jlong bodyId) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        body->randomizeConstraints();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    releaseCluster
     * Signature: (JI)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_releaseCluster
    (JNIEnv *env, jobject object, jlong bodyId, jint index) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        body->releaseCluster(index);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    releaseClusters
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_releaseClusters
    (JNIEnv *env, jobject object, jlong bodyId) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        body->releaseClusters();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    generateClusters
     * Signature: (JII)I
     */
    JNIEXPORT jint JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_generateClusters
    (JNIEnv *env, jobject object, jlong bodyId, jint k, jint maxIter) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return body->generateClusters(k,maxIter);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    predictMotion
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_predictMotion
    (JNIEnv *, jobject, jlong, jfloat);

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    solveConstraints
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_solveConstraints
    (JNIEnv *, jobject, jlong);

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    staticSolve
     * Signature: (JI)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_staticSolve
    (JNIEnv *, jobject, jlong, jint);

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    integrateMotion
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_integrateMotion
    (JNIEnv *, jobject, jlong);

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    defaultCollisionHandler
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_defaultCollisionHandler
    (JNIEnv *, jobject, jlong);

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    isInWorld
     * Signature: (J)Z
     */
    JNIEXPORT jboolean JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_isInWorld
    (JNIEnv *env, jobject object, jlong bodyId) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return false;
        }
        //boolean isInWorld = body->getBroadphaseHandle() != 0; // from bullet RigidBody
        return body->getBroadphaseHandle() != 0;
    }

    /*====================*  
     SoftBody to Mesh
     *====================*/

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    getVertices
     * Signature: (JLcom/jme3/bullet/util/DebugMeshCallback;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_getVertices
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


            // Put the verticies into the vertex buffer
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
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    getIndexes
     * Signature: (JLjava/nio/IntBuffer;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_getIndexes
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
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    getNumTriangle
     * Signature: (J)I
     */
    JNIEXPORT jint JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_getNumTriangle
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
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    updateMesh
     * Signature: (JLjava/nio/FloatBuffer;IZLjava/nio/FloatBuffer;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_updateMesh
    (JNIEnv *env, jclass clazz, jlong bodyId, jobject verticesBuffer, jint nbTriangles, jboolean doNormalUpdate, jobject normalsBuffer) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }

        jfloat* vertices = (jfloat*) env->GetDirectBufferAddress(verticesBuffer);
        if (doNormalUpdate) {
            jfloat* normals = (jfloat*) env->GetDirectBufferAddress(normalsBuffer);

            for (int i = 0; i < body->m_nodes.size(); ++i) {
                const btSoftBody::Node& n = body->m_nodes[i];
                vertices[i * 3 + 0] = n.m_x.getX();
                vertices[i * 3 + 1] = n.m_x.getY();
                vertices[i * 3 + 2] = n.m_x.getZ();
                //--- normals
                normals[i * 3 + 0] = n.m_n.getX();
                normals[i * 3 + 1] = n.m_n.getY();
                normals[i * 3 + 2] = n.m_n.getZ();
            }
        } else {
            for (int i = 0; i < body->m_nodes.size(); ++i) {
                const btSoftBody::Node& n = body->m_nodes[i];
                vertices[i * 3 + 0] = n.m_x.getX();
                vertices[i * 3 + 1] = n.m_x.getY();
                vertices[i * 3 + 2] = n.m_x.getZ();
            }
        }
    }


#ifdef __cplusplus
}
#endif