/*
 * Copyright (C) 2019 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "FingerprintInscreenService"

#include "FingerprintInscreen.h"
#include <android-base/logging.h>
#include <hidl/HidlTransportSupport.h>
#include <fstream>
#include <vector>
#include <stdlib.h>

#define FINGERPRINT_ACQUIRED_VENDOR 6
#define FINGERPRINT_ERROR_VENDOR 8

#define OP_ENABLE_FP_LONGPRESS 3
#define OP_DISABLE_FP_LONGPRESS 4
#define OP_RESUME_FP_ENROLL 8
#define OP_FINISH_FP_ENROLL 10

#define OP_DISPLAY_AOD_MODE 8
#define OP_DISPLAY_NOTIFY_PRESS 9
#define OP_DISPLAY_SET_DIM 10

// This is not a typo by me. It's by OnePlus.
#define BRIGHTNESS_PATH "/sys/class/backlight/panel0-backlight/brightness"
#define HBM_ENABLE_PATH "/sys/class/drm/card0-DSI-1/op_friginer_print_hbm"
#define HBM_PATH "/sys/class/drm/card0-DSI-1/hbm"

namespace vendor {
namespace lineage {
namespace biometrics {
namespace fingerprint {
namespace inscreen {
namespace V1_0 {
namespace implementation {

const std::vector<std::vector<int>> BRIGHTNESS_ALPHA_ARRAY = {
    std::vector<int>{0, 255},
    std::vector<int>{1, 241},
    std::vector<int>{2, 240},
    std::vector<int>{4, 238},
    std::vector<int>{5, 236},
    std::vector<int>{6, 235},
    std::vector<int>{10, 231},
    std::vector<int>{20, 223},
    std::vector<int>{30, 216},
    std::vector<int>{45, 208},
    std::vector<int>{70, 197},
    std::vector<int>{100, 185},
    std::vector<int>{150, 175},
    std::vector<int>{227, 153},
    std::vector<int>{300, 136},
    std::vector<int>{400, 118},
    std::vector<int>{500, 102},
    std::vector<int>{600, 89},
    std::vector<int>{800, 66},
    std::vector<int>{1023, 42},
    std::vector<int>{2000, 131}
};

/*
 * Write value to path and close file.
 */
template <typename T>
static void set(const std::string& path, const T& value) {
    std::ofstream file(path);
    file << value;
}

template <typename T>
static T get(const std::string& path, const T& def) {
    std::ifstream file(path);
    T result;

    file >> result;
    return file.fail() ? def : result;
}

FingerprintInscreen::FingerprintInscreen() {
    this->mFodCircleVisible = false;
    this->mVendorFpService = IVendorFingerprintExtensions::getService();
    this->mVendorDisplayService = IOneplusDisplay::getService();
}

Return<void> FingerprintInscreen::onStartEnroll() {
    this->mVendorFpService->updateStatus(OP_DISABLE_FP_LONGPRESS);
    this->mVendorFpService->updateStatus(OP_RESUME_FP_ENROLL);

    return Void();
}

Return<void> FingerprintInscreen::onFinishEnroll() {
    this->mVendorFpService->updateStatus(OP_FINISH_FP_ENROLL);

    return Void();
}

Return<void> FingerprintInscreen::switchHbm(bool enabled) {
    if (enabled) {
        set(HBM_ENABLE_PATH, 1);
    } else {
        set(HBM_ENABLE_PATH, 0);
    }
    return Void();
}

Return<void> FingerprintInscreen::onPress() {
    this->mVendorDisplayService->setMode(OP_DISPLAY_NOTIFY_PRESS, 1);

    return Void();
}

Return<void> FingerprintInscreen::onRelease() {
    this->mVendorDisplayService->setMode(OP_DISPLAY_NOTIFY_PRESS, 0);

    return Void();
}

Return<void> FingerprintInscreen::onShowFODView() {
    this->mFodCircleVisible = true;
    this->mVendorDisplayService->setMode(OP_DISPLAY_AOD_MODE, 2);
    this->mVendorDisplayService->setMode(OP_DISPLAY_SET_DIM, 1);

    return Void();
}

Return<void> FingerprintInscreen::onHideFODView() {
    this->mFodCircleVisible = false;
    this->mVendorDisplayService->setMode(OP_DISPLAY_AOD_MODE, 0);
    this->mVendorDisplayService->setMode(OP_DISPLAY_SET_DIM, 0);
    set(HBM_ENABLE_PATH, 0);
    this->mVendorDisplayService->setMode(OP_DISPLAY_NOTIFY_PRESS, 0);

    return Void();
}

Return<bool> FingerprintInscreen::handleAcquired(int32_t acquiredInfo, int32_t vendorCode) {
    std::lock_guard<std::mutex> _lock(mCallbackLock);
    if (mCallback == nullptr) {
        return false;
    }

    if (acquiredInfo == FINGERPRINT_ACQUIRED_VENDOR) {
        if (mFodCircleVisible && vendorCode == 0) {
            Return<void> ret = mCallback->onFingerDown();
            if (!ret.isOk()) {
                LOG(ERROR) << "FingerDown() error: " << ret.description();
            }
            return true;
        }

        if (mFodCircleVisible && vendorCode == 1) {
            Return<void> ret = mCallback->onFingerUp();
            if (!ret.isOk()) {
                LOG(ERROR) << "FingerUp() error: " << ret.description();
            }
            return true;
        }
    }

    return false;
}

Return<bool> FingerprintInscreen::handleError(int32_t error, int32_t vendorCode) {
    return error == FINGERPRINT_ERROR_VENDOR && vendorCode == 6;
}

Return<void> FingerprintInscreen::setLongPressEnabled(bool enabled) {
    this->mVendorFpService->updateStatus(
            enabled ? OP_ENABLE_FP_LONGPRESS : OP_DISABLE_FP_LONGPRESS);

    return Void();
}

static int interpolate(int x, int xa, int xb, int ya, int yb) {
    int sub = 0;
    int bf = (((yb - ya) * 2) * (x - xa)) / (xb - xa);
    int factor = bf / 2;
    int plus = bf % 2;
    if (!(xa - xb == 0 || yb - ya == 0)) {
        sub = (((2 * (x - xa)) * (x - xb)) / (yb - ya)) / (xa - xb);
    }
    return ya + factor + plus + sub;
}

int getDimAlpha(int brightness) {
    int level = BRIGHTNESS_ALPHA_ARRAY.size();
    int i = 0;
    while (i < level && BRIGHTNESS_ALPHA_ARRAY[i][0] < brightness) {
        i++;
    }
    if (i == 0) {
        return BRIGHTNESS_ALPHA_ARRAY[0][1];
    }
    if (i == level) {
        return BRIGHTNESS_ALPHA_ARRAY[level - 1][1];
    }
    return interpolate(brightness,
            BRIGHTNESS_ALPHA_ARRAY[i-1][0],
            BRIGHTNESS_ALPHA_ARRAY[i][0],
            BRIGHTNESS_ALPHA_ARRAY[i-1][1],
            BRIGHTNESS_ALPHA_ARRAY[i][1]);
}

Return<int32_t> FingerprintInscreen::getDimAmount(int32_t) {
    int brightness = get(BRIGHTNESS_PATH, 0);
    int dimAmount = getDimAlpha(brightness);
    int hbmMode = get(HBM_PATH, 0);
    if (hbmMode == 5) {
        dimAmount = 42;
    }
    LOG(INFO) << "dimAmount = " << dimAmount;

    return dimAmount;
}

Return<bool> FingerprintInscreen::shouldBoostBrightness() {
    return false;
}

Return<void> FingerprintInscreen::setCallback(const sp<IFingerprintInscreenCallback>& callback) {
    {
        std::lock_guard<std::mutex> _lock(mCallbackLock);
        mCallback = callback;
    }

    return Void();
}

Return<int32_t> FingerprintInscreen::getPositionX() {
    return 437;
}

Return<int32_t> FingerprintInscreen::getPositionY() {
    return 1959;
}

Return<int32_t> FingerprintInscreen::getSize() {
    return 204;
}

}  // namespace implementation
}  // namespace V1_0
}  // namespace inscreen
}  // namespace fingerprint
}  // namespace biometrics
}  // namespace lineage
}  // namespace vendor
