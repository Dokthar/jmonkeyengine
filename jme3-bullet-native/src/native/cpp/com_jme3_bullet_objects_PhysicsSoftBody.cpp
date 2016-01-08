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

    static inline btVector3 getBoundingCenter(btSoftBody* body) {
        return (body->m_bounds[0] + body->m_bounds[1]) / 2;
    }

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
        body->getCollisionShape()->setMargin(0);
        // The only available flag for Materials is DebugDraw (by Default)
        // we don't want to use Bullet's debug draw, (we use JME instead).
        body->m_materials[0]->m_flags = 0x0000;
        return reinterpret_cast<jlong> (body);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    createRope
     * Signature: (Ljava/nio/IntBuffer;Ljava/nio/FloatBuffer;)J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_createRope
    (JNIEnv *env, jobject object, jobject linesIndexBuffer, jobject vertexBuffer) {
        jmeClasses::initJavaClasses(env);
        btSoftBodyWorldInfo* worldInfo = new btSoftBodyWorldInfo();

        int* lines = (int*) env->GetDirectBufferAddress(linesIndexBuffer);
        float* vertices = (float*) env->GetDirectBufferAddress(vertexBuffer);


        /* Create nodes	*/

        const int nbNodes = env->GetDirectBufferCapacity(vertexBuffer) / 3;

        btVector3* nodes = new btVector3[nbNodes];
        btScalar* masses = new btScalar[nbNodes];

        for (int i = 0; i < nbNodes; ++i) {
            nodes[i] = btVector3(vertices[i * 3], vertices[i * 3 + 1], vertices[i * 3 + 2]);
            masses[i] = 1;
        }

        btSoftBody* body = new btSoftBody(worldInfo, nbNodes, nodes, masses);

        /* Create links	*/
        const int nbLinks = env->GetDirectBufferCapacity(linesIndexBuffer) / 2;
        for (int i = 0; i < nbLinks; ++i) {
            body->appendLink(lines[i * 2], lines[i * 2 + 1]);
        }

        delete[] masses;
        delete[] nodes;

        body->setUserPointer(NULL);
        body->getCollisionShape()->setMargin(0);
        // The only available flag for Materials is DebugDraw (by Default)
        // we don't want to use Bullet's debug draw, (we use JME instead).
        body->m_materials[0]->m_flags = 0x0000;
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
     * Method:    getMaterial
     * Signature: (J)J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_getMaterial
    (JNIEnv *env, jobject object, jlong bodyId) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }

        return reinterpret_cast<long> (body->m_materials[0]);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    appendAnchor
     * Signature: (JIJLcom/jme3/math/Vector3f;ZF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_appendAnchor
    (JNIEnv *env, jobject object, jlong bodyId, jint nodeId, jlong rigidId, jobject localPivot, jboolean collisionBetweenLinkedBodies, jfloat influence) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }

        btRigidBody* rigid = reinterpret_cast<btRigidBody*> (rigidId);
        if (rigid == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }

        if (localPivot != NULL) {
            btVector3 vec = btVector3();
            jmeBulletUtil::convert(env, localPivot, &vec);
            
            body->appendAnchor(nodeId, rigid, vec, !collisionBetweenLinkedBodies, influence);
        } else {
            body->appendAnchor(nodeId, rigid, !collisionBetweenLinkedBodies, influence);
        }
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
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 vec = btVector3();
        jmeBulletUtil::convert(env, force, &vec);
        body->addForce(vec, nodeId);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    addAeroForceToNode
     * Signature: (JLcom/jme3/math/Vector3f;I)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_addAeroForceToNode
    (JNIEnv *env, jobject object, jlong bodyId, jobject force, jint nodeId) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 vec = btVector3();
        jmeBulletUtil::convert(env, force, &vec);
        body->addAeroForceToNode(vec, nodeId);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    addAeroForceToFace
     * Signature: (JLcom/jme3/math/Vector3f;I)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_addAeroForceToFace
    (JNIEnv *env, jobject object, jlong bodyId, jobject force, jint faceId) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 vec = btVector3();
        jmeBulletUtil::convert(env, force, &vec);
        body->addAeroForceToFace(vec, faceId);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    setMass
     * Signature: (JIF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_setMass
    (JNIEnv *env, jobject object, jlong bodyId, jint nodeId, jfloat mass) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        body->setMass(nodeId, mass);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    getMass
     * Signature: (JI)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_getMass
    (JNIEnv *env, jobject object, jlong bodyId, jint nodeId) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return body->getMass(nodeId);
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
     * Method:    setMasses
     * Signature: (JLjava/nio/FloatBuffer;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_setMasses
    (JNIEnv *env, jobject object, jlong bodyId, jobject massBuffer) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jfloat* masses = (jfloat*) env->GetDirectBufferAddress(massBuffer);
        int length = env->GetDirectBufferCapacity(massBuffer);
        for (int i = 0; i < length; ++i) {
            body->setMass(i, masses[i]);
        }
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    getMasses
     * Signature: (JLjava/nio/FloatBuffer;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_getMasses
    (JNIEnv *env, jobject object, jlong bodyId, jobject massBuffer) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jfloat* masses = (jfloat*) env->GetDirectBufferAddress(massBuffer);
        int length = env->GetDirectBufferCapacity(massBuffer);
        for (int i = 0; i < length; ++i) {
            masses[i] = body->getMass(i);
        }
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    applyPhysicsTransform
     * Signature: (JLcom/jme3/math/Transform;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_applyPhysicsTransform
    (JNIEnv *env, jobject object, jlong bodyId, jobject transform) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }

        btTransform trs = btTransform();
        jmeBulletUtil::convert(env, transform, &trs);
        body->transform(trs);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    applyPhysicsTranslate
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_applyPhysicsTranslate
    (JNIEnv *env, jobject object, jlong bodyId, jobject translate) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }

        btVector3 vec = btVector3();
        jmeBulletUtil::convert(env, translate, &vec);
        body->translate(vec);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    applyPhysicsRotation
     * Signature: (JLcom/jme3/math/Quaternion;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_applyPhysicsRotation
    (JNIEnv *env, jobject object, jlong bodyId, jobject rotation) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }

        btQuaternion rot = btQuaternion();
        jmeBulletUtil::convert(env, rotation, &rot);
        body->rotate(rot);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    applyPhysicsScale
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_applyPhysicsScale
    (JNIEnv *env, jobject object, jlong bodyId, jobject scl) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }

        btVector3 vec = btVector3();
        jmeBulletUtil::convert(env, scl, &vec);
        body->scale(vec);
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
        // scale unavailable with btTransform
        btTransform trs = btTransform();
        jmeBulletUtil::convert(env, transform, &trs);
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
        // scale unavailable with btTransform
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
        vec -= getBoundingCenter(body);
        body->translate(vec);
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
        btVector3 vec = getBoundingCenter(body);
        jmeBulletUtil::convert(env, &vec, location);
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
     * Method:    generateBendingConstraints
     * Signature: (JIJ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_generateBendingConstraints
    (JNIEnv *env, jobject object, jlong bodyId, jint dist, jlong matId) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }

        btSoftBody::Material* mat = reinterpret_cast<btSoftBody::Material*> (matId);
        if (mat == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }

        body->generateBendingConstraints(dist, mat);
    }

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
     * Signature: (JII)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_generateClusters
    (JNIEnv *env, jobject object, jlong bodyId, jint k, jint maxIter) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        body->generateClusters(k, maxIter);
    }

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

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    getBoundingCenter
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_getBoundingCenter
    (JNIEnv *env, jobject object, jlong bodyId, jobject vec) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 result;
        result = getBoundingCenter(body);
        jmeBulletUtil::convert(env, &result, vec);
        return;
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsSoftBody
     * Method:    updateTriMesh
     * Signature: (JILjava/nio/IntBuffer;Ljava/nio/FloatBuffer;Ljava/nio/FloatBuffer;ZZ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsSoftBody_updateTriMesh
    (JNIEnv *env, jobject object, jlong bodyId, jint indexSize, jobject indexMap, jobject positionsBuffer, jobject normalsBuffer, jboolean meshInLocalSpace, jboolean doNormalUpdate) {
        btSoftBody* body = reinterpret_cast<btSoftBody*> (bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }

        jint* map = (jint*) env->GetDirectBufferAddress(indexMap);

        jfloat* positions = (jfloat*) env->GetDirectBufferAddress(positionsBuffer);

        btVector3 center = btVector3(0, 0, 0);
        if (meshInLocalSpace) {
            center = (body->m_bounds[0] + body->m_bounds[1]) / 2;
        }

        if (doNormalUpdate) {
            jfloat* normals = (jfloat*) env->GetDirectBufferAddress(normalsBuffer);

            for (int i = 0; i < indexSize; ++i) {
                const btSoftBody::Node& n = body->m_nodes[map[i]];
                positions[i * 3 + 0] = n.m_x.getX() - center.getX();
                positions[i * 3 + 1] = n.m_x.getY() - center.getY();
                positions[i * 3 + 2] = n.m_x.getZ() - center.getZ();
                //--- normals
                normals[i * 3 + 0] = n.m_n.getX();
                normals[i * 3 + 1] = n.m_n.getY();
                normals[i * 3 + 2] = n.m_n.getZ();
            }
        } else {
            for (int i = 0; i < indexSize; ++i) {
                const btSoftBody::Node& n = body->m_nodes[map[i]];
                positions[i * 3 + 0] = n.m_x.getX() - center.getX();
                positions[i * 3 + 1] = n.m_x.getY() - center.getY();
                positions[i * 3 + 2] = n.m_x.getZ() - center.getZ();
            }
        }
    }

#ifdef __cplusplus
}
#endif