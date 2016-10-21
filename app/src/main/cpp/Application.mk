# Tell the compiler to use the newer 4.8 version, required for c++11 features
NDK_TOOLCHAIN_VERSION := 4.9
#NDK_TOOLCHAIN_VERSION := 4.8

# The 4.8 toolchain disables RTTI
APP_CPPFLAGS += -frtti
# Also, we want c++11 yay!
APP_CPPFLAGS += -std=c++11

#APP_STL := gnustl_shared

APP_ABI := armeabi-v7a

#APP_OPTIM=debug