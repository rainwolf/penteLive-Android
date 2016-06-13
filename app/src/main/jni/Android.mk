LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := Ai
LOCAL_SRC_FILES := AiWrapper.cpp Ai.cpp

include $(BUILD_SHARED_LIBRARY)