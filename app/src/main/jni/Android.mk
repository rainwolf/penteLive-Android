LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := Ai
# CPoint.cpp is a (near-empty) translation unit in the canonical engine; kept in
# the build so the sync matches react_mmai/MMAIWASM verbatim.
LOCAL_SRC_FILES := AiWrapper.cpp Ai.cpp CPoint.cpp

# The synced engine uses C++11 features (nullptr, default member initializers,
# std::string/std::fstream). Force the standard here as well as in Application.mk.
LOCAL_CPPFLAGS  += -std=c++11

# Align ELF load segments to 16 KB so the lib works on 16 KB page-size devices.
LOCAL_LDFLAGS   += -Wl,-z,max-page-size=16384

include $(BUILD_SHARED_LIBRARY)