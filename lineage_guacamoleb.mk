#
# Copyright (C) 2019 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit from guacamoleb device
$(call inherit-product, device/oneplus/guacamoleb/device.mk)

# Inherit some common Lineage stuff.
$(call inherit-product, vendor/lineage/config/common_full_phone.mk)

# Device identifier. This must come after all inclusions.
PRODUCT_NAME := lineage_guacamoleb
PRODUCT_DEVICE := guacamoleb
PRODUCT_BRAND := OnePlus
PRODUCT_MODEL := GM1905
PRODUCT_MANUFACTURER := OnePlus

PRODUCT_AAPT_CONFIG := xxxhdpi
PRODUCT_AAPT_PREF_CONFIG := xxxhdpi
PRODUCT_CHARACTERISTICS := nosdcard

# Boot animation
TARGET_SCREEN_HEIGHT := 2340
TARGET_SCREEN_WIDTH := 1080

# Extra FOD stuff
TARGET_HAS_FOD := true

# Build info
BUILD_FINGERPRINT := "OnePlus/OnePlus7/OnePlus7:10/QKQ1.190716.003/2010111713:user/release-keys"
PRODUCT_BUILD_PROP_OVERRIDES += \
    PRODUCT_DEVICE=OnePlus7 \
    PRODUCT_NAME=OnePlus7 \
    PRIVATE_BUILD_DESC="OnePlus7-user 10 QKQ1.190716.003 2010111713 release-keys"

PRODUCT_ODM_PROPERTY_BLACKLIST += \
    ro.product.odm.model

PRODUCT_SYSTEM_PROPERTY_BLACKLIST += \
    ro.product.system.model

PLATFORM_SECURITY_PATCH_OVERRIDE := 2020-09-01

PRODUCT_GMS_CLIENTID_BASE := android-oneplus
