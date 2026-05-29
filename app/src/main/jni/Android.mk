LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := Ai
LOCAL_SRC_FILES := AiWrapper.cpp Ai.cpp

# Align ELF load segments to 16 KB so the lib works on 16 KB page-size devices.
LOCAL_LDFLAGS   += -Wl,-z,max-page-size=16384

include $(BUILD_SHARED_LIBRARY)