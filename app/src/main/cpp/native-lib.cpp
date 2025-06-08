#include <jni.h>
#include <vector>
#include <cmath>
#include <algorithm>
#include <stdint.h>

inline int getAlpha(jint color) { return (color >> 24) & 0xFF; }

inline int getRed(jint color) { return (color >> 16) & 0xFF; }

inline int getGreen(jint color) { return (color >> 8) & 0xFF; }

inline int getBlue(jint color) { return color & 0xFF; }

inline jint composeColor(int alpha, int red, int green, int blue) {
    return (alpha << 24) | (red << 16) | (green << 8) | blue;
}

inline int clamp(int val, int minVal = 0, int maxVal = 255) {
    return std::min(maxVal, std::max(minVal, val));
}

// Helper function to process image pixels with a given pixel processing lambda
template<typename Func>
jintArray processPixels(JNIEnv *env, jintArray pixels, jint width, jint height, Func pixelFunc) {
    jint length = width * height;
    jint *pixelArray = env->GetIntArrayElements(pixels, nullptr);
    std::vector<jint> result(length);

    for (int i = 0; i < length; ++i) {
        result[i] = pixelFunc(pixelArray[i]);
    }

    env->ReleaseIntArrayElements(pixels, pixelArray, 0);

    jintArray resultArray = env->NewIntArray(length);
    env->SetIntArrayRegion(resultArray, 0, length, result.data());
    return resultArray;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_example_nativeimageprocessing_activities_EditActivity_convertToGrayscale(JNIEnv *env,
                                                                                  jobject /* this */,
                                                                                  jintArray pixels,
                                                                                  jint width,
                                                                                  jint height) {
    return processPixels(env, pixels, width, height, [](jint color) {
        int alpha = getAlpha(color);
        int red = getRed(color);
        int green = getGreen(color);
        int blue = getBlue(color);

        int gray = (red + green + blue) / 3;
        return composeColor(alpha, gray, gray, gray);
    });
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_example_nativeimageprocessing_activities_EditActivity_invertColors(JNIEnv *env,
                                                                            jobject /* this */,
                                                                            jintArray pixels,
                                                                            jint width,
                                                                            jint height) {
    return processPixels(env, pixels, width, height, [](jint color) {
        int alpha = getAlpha(color);
        int red = 255 - getRed(color);
        int green = 255 - getGreen(color);
        int blue = 255 - getBlue(color);
        return composeColor(alpha, red, green, blue);
    });
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_example_nativeimageprocessing_activities_EditActivity_sepia(JNIEnv *env,
                                                                     jobject /* this */,
                                                                     jintArray pixels, jint width,
                                                                     jint height) {
    return processPixels(env, pixels, width, height, [](jint color) {
        int alpha = getAlpha(color);
        int red = getRed(color);
        int green = getGreen(color);
        int blue = getBlue(color);

        int tr = static_cast<int>(0.393 * red + 0.769 * green + 0.189 * blue);
        int tg = static_cast<int>(0.349 * red + 0.686 * green + 0.168 * blue);
        int tb = static_cast<int>(0.272 * red + 0.534 * green + 0.131 * blue);

        red = clamp(tr);
        green = clamp(tg);
        blue = clamp(tb);

        return composeColor(alpha, red, green, blue);
    });
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_example_nativeimageprocessing_activities_BrightnessActivity_brightness(JNIEnv *env,
                                                                                jobject /* this */,
                                                                                jintArray pixels,
                                                                                jint width,
                                                                                jint height,
                                                                                jint brightnessValue) {
    float brightnessFactor = brightnessValue / 100.0f;  // 0.0 to 2.0

    return processPixels(env, pixels, width, height, [brightnessFactor](jint color) {
        int alpha = getAlpha(color);
        int red = clamp(static_cast<int>(getRed(color) * brightnessFactor));
        int green = clamp(static_cast<int>(getGreen(color) * brightnessFactor));
        int blue = clamp(static_cast<int>(getBlue(color) * brightnessFactor));
        return composeColor(alpha, red, green, blue);
    });
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_example_nativeimageprocessing_activities_ContrastActivity_contrast(JNIEnv *env,
                                                                            jobject /* this */,
                                                                            jintArray pixels,
                                                                            jint width,
                                                                            jint height,
                                                                            jint contrastValue) {
    int contrast = std::clamp(contrastValue, -255, 255);
    float factor = (259.0f * (contrast + 255)) / (255.0f * (259 - contrast));

    return processPixels(env, pixels, width, height, [factor](jint color) {
        int alpha = getAlpha(color);

        int red = clamp(static_cast<int>(factor * (getRed(color) - 128) + 128));
        int green = clamp(static_cast<int>(factor * (getGreen(color) - 128) + 128));
        int blue = clamp(static_cast<int>(factor * (getBlue(color) - 128) + 128));

        return composeColor(alpha, red, green, blue);
    });
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_example_nativeimageprocessing_activities_BlurActivity_blur(JNIEnv *env,
                                                                    jobject /* this */,
                                                                    jintArray pixels,
                                                                    jint width,
                                                                    jint height,
                                                                    jint radius) {
    if (radius < 1) radius = 1;
    if (radius > 25) radius = 25; // Limit radius

    jint *src = env->GetIntArrayElements(pixels, nullptr);
    int size = width * height;
    std::vector<jint> result(size);

    int wm = width - 1;
    int hm = height - 1;

    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            int rsum = 0, gsum = 0, bsum = 0, count = 0;

            for (int ky = -radius; ky <= radius; ky++) {
                int py = std::min(hm, std::max(0, y + ky));
                for (int kx = -radius; kx <= radius; kx++) {
                    int px = std::min(wm, std::max(0, x + kx));
                    int idx = py * width + px;

                    int color = src[idx];
                    rsum += getRed(color);
                    gsum += getGreen(color);
                    bsum += getBlue(color);
                    count++;
                }
            }

            int idx = y * width + x;
            int alpha = getAlpha(src[idx]);
            int red = rsum / count;
            int green = gsum / count;
            int blue = bsum / count;

            result[idx] = composeColor(alpha, red, green, blue);
        }
    }

    env->ReleaseIntArrayElements(pixels, src, 0);

    jintArray resultArray = env->NewIntArray(size);
    env->SetIntArrayRegion(resultArray, 0, size, result.data());
    return resultArray;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_example_nativeimageprocessing_activities_EditActivity_edgeDetect(
        JNIEnv *env, jobject /* thiz */, jintArray pixels, jint width, jint height) {

    jint *inputPixels = env->GetIntArrayElements(pixels, nullptr);
    jintArray result = env->NewIntArray(width * height);
    jint *outputPixels = new jint[width * height];

    // Convert to grayscale (luma)
    std::vector<uint8_t> gray(width * height);
    for (int i = 0; i < width * height; i++) {
        gray[i] = static_cast<uint8_t>(0.3 * getRed(inputPixels[i]) +
                                       0.59 * getGreen(inputPixels[i]) +
                                       0.11 * getBlue(inputPixels[i]));
    }

    // Sobel kernels
    const int gx[3][3] = {
            {-1, 0, 1},
            {-2, 0, 2},
            {-1, 0, 1}
    };
    const int gy[3][3] = {
            {-1, -2, -1},
            {0,  0,  0},
            {1,  2,  1}
    };

    // Apply Sobel operator
    for (int y = 1; y < height - 1; y++) {
        for (int x = 1; x < width - 1; x++) {
            int sumX = 0;
            int sumY = 0;

            for (int ky = -1; ky <= 1; ky++) {
                for (int kx = -1; kx <= 1; kx++) {
                    int pixel = gray[(y + ky) * width + (x + kx)];
                    sumX += pixel * gx[ky + 1][kx + 1];
                    sumY += pixel * gy[ky + 1][kx + 1];
                }
            }

            int magnitude = clamp(static_cast<int>(std::sqrt(sumX * sumX + sumY * sumY)));
            int alpha = getAlpha(inputPixels[y * width + x]);
            outputPixels[y * width + x] = composeColor(alpha, magnitude, magnitude, magnitude);
        }
    }

    // Fill borders with black or copy
    for (int x = 0; x < width; x++) {
        outputPixels[x] = 0xFF000000;
        outputPixels[(height - 1) * width + x] = 0xFF000000;
    }
    for (int y = 0; y < height; y++) {
        outputPixels[y * width] = 0xFF000000;
        outputPixels[y * width + (width - 1)] = 0xFF000000;
    }

    env->ReleaseIntArrayElements(pixels, inputPixels, 0);
    env->SetIntArrayRegion(result, 0, width * height, outputPixels);
    delete[] outputPixels;

    return result;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_example_nativeimageprocessing_activities_EditActivity_rotate(
        JNIEnv *env, jobject /* this */, jintArray pixels, jint width, jint height,
        jint rotationDegrees) {
    jint *inputPixels = env->GetIntArrayElements(pixels, nullptr);
    jintArray result = env->NewIntArray(width * height);
    std::vector<jint> outputPixels(width * height);

    switch (rotationDegrees) {
        case 90:
            for (int y = 0; y < height; ++y)
                for (int x = 0; x < width; ++x)
                    outputPixels[x * height + (height - y - 1)] = inputPixels[y * width + x];
            break;

        case 180:
            for (int y = 0; y < height; ++y)
                for (int x = 0; x < width; ++x)
                    outputPixels[(height - y - 1) * width + (width - x - 1)] = inputPixels[
                            y * width + x];
            break;

        case 270:
            for (int y = 0; y < height; ++y)
                for (int x = 0; x < width; ++x)
                    outputPixels[(width - x - 1) * height + y] = inputPixels[y * width + x];
            break;

        default:
            // If invalid angle, just copy input to output without change
            for (int i = 0; i < width * height; ++i)
                outputPixels[i] = inputPixels[i];
            break;
    }

    env->ReleaseIntArrayElements(pixels, inputPixels, 0);
    env->SetIntArrayRegion(result, 0, width * height, outputPixels.data());

    return result;
}