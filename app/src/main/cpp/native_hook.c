#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <stdint.h>
#include <dlfcn.h>
#include <android/log.h>

#ifdef XHL_HAVE_DOBBY
#include "dobby.h"
#endif

#define TAG "XHookLab-Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

#define MAX_HOOKS 128

typedef struct {
    void *addr;
    void *orig;
    char  lib[128];
    char  sym[128];
    int   used;
} Slot;

static Slot g_hooks[MAX_HOOKS];

static int find_slot(void *addr) {
    for (int i = 0; i < MAX_HOOKS; i++)
        if (g_hooks[i].used && g_hooks[i].addr == addr) return i;
    return -1;
}

static int free_slot(void) {
    for (int i = 0; i < MAX_HOOKS; i++)
        if (!g_hooks[i].used) return i;
    return -1;
}

#ifdef XHL_HAVE_DOBBY
static void common_replace(void) {
    LOGI("hook hit (generic stub)");
}
#endif

JNIEXPORT jlong JNICALL
Java_com_xhooklab_NativeHook_resolve(JNIEnv *env, jclass clz, jstring jlib, jstring jsym) {
    const char *lib = (*env)->GetStringUTFChars(env, jlib, 0);
    const char *sym = (*env)->GetStringUTFChars(env, jsym, 0);
    void *handle = dlopen(lib, RTLD_NOW);
    void *p = NULL;
    if (!handle) {
        LOGI("dlopen fail: %s (%s)", lib, dlerror());
    } else {
        p = dlsym(handle, sym);
        LOGI("resolve %s!%s = %p", lib, sym, p);
    }
    (*env)->ReleaseStringUTFChars(env, jlib, lib);
    (*env)->ReleaseStringUTFChars(env, jsym, sym);
    return (jlong)(uintptr_t)p;
}

JNIEXPORT jlong JNICALL
Java_com_xhooklab_NativeHook_hookSymbol(JNIEnv *env, jclass clz, jstring jlib, jstring jsym, jlong target) {
    const char *lib = (*env)->GetStringUTFChars(env, jlib, 0);
    const char *sym = (*env)->GetStringUTFChars(env, jsym, 0);
    void *addr = (void *)(uintptr_t)target;
    jlong ret = 0;

#ifdef XHL_HAVE_DOBBY
    if (!addr) { LOGI("hookSymbol: null addr"); goto done; }

    int idx = find_slot(addr);
    if (idx >= 0) { LOGI("already hooked %p", addr); ret = (jlong)(uintptr_t)g_hooks[idx].orig; goto done; }

    idx = free_slot();
    if (idx < 0) { LOGI("slot table full"); goto done; }

    void *orig = NULL;
    int rc = DobbyHook(addr, (dobby_dummy_func_t)common_replace, (dobby_dummy_func_t *)&orig);
    if (rc != 0) { LOGI("DobbyHook rc=%d for %s!%s", rc, lib, sym); goto done; }

    g_hooks[idx].addr = addr;
    g_hooks[idx].orig = orig;
    g_hooks[idx].used = 1;
    snprintf(g_hooks[idx].lib, sizeof(g_hooks[idx].lib), "%s", lib);
    snprintf(g_hooks[idx].sym, sizeof(g_hooks[idx].sym), "%s", sym);
    LOGI("hooked %s!%s @%p -> orig %p", lib, sym, addr, orig);
    ret = (jlong)(uintptr_t)orig;
#else
    (void)clz; (void)addr;
    LOGI("hookSymbol called without Dobby: %s!%s (stub)", lib, sym);
    ret = 0;
#endif

done:
    (*env)->ReleaseStringUTFChars(env, jlib, lib);
    (*env)->ReleaseStringUTFChars(env, jsym, sym);
    return ret;
}

JNIEXPORT jboolean JNICALL
Java_com_xhooklab_NativeHook_unhook(JNIEnv *env, jclass clz, jlong addr) {
#ifndef XHL_HAVE_DOBBY
    (void)env; (void)clz; (void)addr;
    LOGI("unhook (no Dobby stub)");
    return JNI_FALSE;
#else
    int idx = find_slot((void *)(uintptr_t)addr);
    if (idx < 0) return JNI_FALSE;
    int rc = DobbyDestroy((void *)(uintptr_t)addr);
    LOGI("unhook %p rc=%d", (void *)(uintptr_t)addr, rc);
    g_hooks[idx].used = 0;
    return rc == 0 ? JNI_TRUE : JNI_FALSE;
#endif
}

JNIEXPORT jstring JNICALL
Java_com_xhooklab_NativeHook_info(JNIEnv *env, jclass clz) {
    char buf[4096];
    int off = 0;
#ifdef XHL_HAVE_DOBBY
    off += snprintf(buf + off, sizeof(buf) - off, "xhooklab native (Dobby ON), hooks:\n");
#else
    off += snprintf(buf + off, sizeof(buf) - off, "xhooklab native (Dobby OFF stub), hooks:\n");
#endif
    for (int i = 0; i < MAX_HOOKS && off < (int)sizeof(buf) - 200; i++) {
        if (!g_hooks[i].used) continue;
        off += snprintf(buf + off, sizeof(buf) - off, "  %s!%s @%p\n",
                        g_hooks[i].lib, g_hooks[i].sym, g_hooks[i].addr);
    }
    (void)clz;
    return (*env)->NewStringUTF(env, buf);
}
