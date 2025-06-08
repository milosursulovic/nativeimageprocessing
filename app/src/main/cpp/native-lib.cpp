#include <jni.h>
#include <vector>

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_example_nativeimageprocessing_activities_EditActivity_convertToGrayscale(JNIEnv *env,
                                                                                  jobject /* this */,
                                                                                  jintArray pixels,
                                                                                  jint width,
                                                                                  jint height) {
    jint *pixelArray = env->GetIntArrayElements(pixels, nullptr);
    jint length = width * height;

    std::vector<jint> result(length);

    for (int i = 0; i < length; ++i) {
        jint color = pixelArray[i];

        int alpha = (color >> 24) & 0xff;
        int red = (color >> 16) & 0xff;
        int green = (color >> 8) & 0xff;
        int blue = (color) & 0xff;

        int gray = (red + green + blue) / 3;

        result[i] = (alpha << 24) | (gray << 16) | (gray << 8) | gray;
    }

    env->ReleaseIntArrayElements(pixels, pixelArray, 0);

    jintArray resultArray = env->NewIntArray(length);
    env->SetIntArrayRegion(resultArray, 0, length, result.data());
    return resultArray;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_example_nativeimageprocessing_activities_EditActivity_invertColors(JNIEnv *env,
                                                                            jobject /* this */,
                                                                            jintArray pixels,
                                                                            jint width,
                                                                            jint height) {
    jint *pixelArray = env->GetIntArrayElements(pixels, nullptr);
    jint length = width * height;
    std::vector<jint> result(length);

    for (int i = 0; i < length; ++i) {
        jint color = pixelArray[i];

        int alpha = (color >> 24) & 0xff;
        int red = (color >> 16) & 0xff;
        int green = (color >> 8) & 0xff;
        int blue = (color) & 0xff;

        red = 255 - red;
        green = 255 - green;
        blue = 255 - blue;

        result[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    env->ReleaseIntArrayElements(pixels, pixelArray, 0);

    jintArray resultArray = env->NewIntArray(length);
    env->SetIntArrayRegion(resultArray, 0, length, result.data());
    return resultArray;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_example_nativeimageprocessing_activities_EditActivity_sepia(JNIEnv *env,
                                                                     jobject /* this */,
                                                                     jintArray pixels, jint width,
                                                                     jint height) {
    jint *pixelArray = env->GetIntArrayElements(pixels, nullptr);
    jint length = width * height;
    std::vector<jint> result(length);

    for (int i = 0; i < length; ++i) {
        jint color = pixelArray[i];

        int alpha = (color >> 24) & 0xff;
        int red = (color >> 16) & 0xff;
        int green = (color >> 8) & 0xff;
        int blue = (color) & 0xff;

        int tr = (int) (0.393 * red + 0.769 * green + 0.189 * blue);
        int tg = (int) (0.349 * red + 0.686 * green + 0.168 * blue);
        int tb = (int) (0.272 * red + 0.534 * green + 0.131 * blue);

        // Clamp values to [0,255]
        red = std::min(255, tr);
        green = std::min(255, tg);
        blue = std::min(255, tb);

        result[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    env->ReleaseIntArrayElements(pixels, pixelArray, 0);

    jintArray resultArray = env->NewIntArray(length);
    env->SetIntArrayRegion(resultArray, 0, length, result.data());
    return resultArray;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_example_nativeimageprocessing_activities_EditActivity_brightness(JNIEnv *env,
                                                                          jobject /* this */,
                                                                          jintArray pixels,
                                                                          jint width,
                                                                          jint height,
                                                                          jint brightnessValue) {
    jint *pixelArray = env->GetIntArrayElements(pixels, nullptr);
    jint length = width * height;
    std::vector<jint> result(length);

    float brightnessFactor = brightnessValue / 100.0f;  // from 0.0 to 2.0

    for (int i = 0; i < length; ++i) {
        jint color = pixelArray[i];

        int alpha = (color >> 24) & 0xff;
        int red = (color >> 16) & 0xff;
        int green = (color >> 8) & 0xff;
        int blue = (color) & 0xff;

        red = std::min(255, int(red * brightnessFactor));
        green = std::min(255, int(green * brightnessFactor));
        blue = std::min(255, int(blue * brightnessFactor));

        result[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    env->ReleaseIntArrayElements(pixels, pixelArray, 0);

    jintArray resultArray = env->NewIntArray(length);
    env->SetIntArrayRegion(resultArray, 0, length, result.data());
    return resultArray;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_example_nativeimageprocessing_activities_EditActivity_contrast(JNIEnv *env,
                                                                        jobject /* this */,
                                                                        jintArray pixels,
                                                                        jint width,
                                                                        jint height,
                                                                        jint contrastValue) {
    jint *pixelArray = env->GetIntArrayElements(pixels, nullptr);
    jint length = width * height;
    std::vector<jint> result(length);

    // Clamp contrastValue to [-255, 255] if needed
    int contrast = contrastValue;
    if (contrast < -255) contrast = -255;
    if (contrast > 255) contrast = 255;

    float factor = (259.0f * (contrast + 255)) / (255.0f * (259 - contrast));

    for (int i = 0; i < length; ++i) {
        jint color = pixelArray[i];

        int alpha = (color >> 24) & 0xff;
        int red = (color >> 16) & 0xff;
        int green = (color >> 8) & 0xff;
        int blue = (color) & 0xff;

        red = static_cast<int>(factor * (red - 128) + 128);
        green = static_cast<int>(factor * (green - 128) + 128);
        blue = static_cast<int>(factor * (blue - 128) + 128);

        // Clamp the values to [0, 255]
        red = std::min(255, std::max(0, red));
        green = std::min(255, std::max(0, green));
        blue = std::min(255, std::max(0, blue));

        result[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    env->ReleaseIntArrayElements(pixels, pixelArray, 0);

    jintArray resultArray = env->NewIntArray(length);
    env->SetIntArrayRegion(resultArray, 0, length, result.data());
    return resultArray;
}
