APP_STL := gnustl_static
APP_CPPFLAGS := -frtti -DCOCOS2D_DEBUG=1
APP_CPPFLAGS += -Wno-error=format-security
APP_CPPFLAGS += -fexceptions
APP_SHORT_COMMANDS := true
APP_OPTIM := debug
APP_ABI := armeabi-v7a arm64-v8a 
NDK_MODULE_PATH :=  F:\\cocos2d-x-2.1.4\\cocos2dx\\platform\\third_party\\android\\prebuilt;F:\\cocos2d-x-2.1.4\\cocos2dx