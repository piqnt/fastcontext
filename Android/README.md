The native interface used by FastContext is defined in `FastContextJNI.java`.
If you change `FastContextJNI.java`, you'll also need to regenerate 
`FastContextJNI.h` and then recompile the native code.

To build changes [Android NDK](http://developer.android.com/tools/sdk/ndk/index.html) is required.

1. Compile FastContextJNI.java:

    javac -d . src/com/adobe/plugins/FastContextJNI.java

2. Create FastContextJNI.h:

    javah -jni com.adobe.plugins.FastContextJNI

3. Move it to the correct location:

    mv com_piqnt_fastcontext_FastContextJNI.h jni/FastContextJNI.h

4. Clean up

    rm -rf com

5. Build the JNI library:

    ndk-build

Should produce output similar to:

    Compile++ thumb  : FastContextJNI <= FastContextJNI.cpp
    Compile++ thumb  : FastContextJNI <= Canvas.cpp
    StaticLibrary    : libstdc++.a
    SharedLibrary    : libFastContextJNI.so
    Install          : libFastContextJNI.so => libs/armeabi/libFastContextJNI.so