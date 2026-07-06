# The canonical mmai engine (Ai.cpp, synced from react_mmai/MMAIWASM) pulls in
# <string>, <iostream> and <fstream> to load pente.tbl / pente.scs / opngbk.pen
# from the app files dir. ndk-build's default "system" STL does not provide the
# C++ standard library, so a full STL is required.
# Pin the native API level to the app's minSdk (build.gradle minSdkVersion 26).
# Without this, ndk-build defaults to the NDK's own minimum, so the toolchain
# could target a different platform than the Java/AGP side expects.
APP_PLATFORM := android-26
APP_STL      := c++_static
APP_CPPFLAGS += -std=c++11
