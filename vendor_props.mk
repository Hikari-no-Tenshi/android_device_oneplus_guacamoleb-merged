#
# Copyright (C) 2019 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Display
PRODUCT_PROPERTY_OVERRIDES += \
    ro.sf.lcd_density=420

PRODUCT_PROPERTY_OVERRIDES += \
    ro.vendor.build.release_type=release \
    ro.vendor.build.real.device=true \
    vendor.product.device=guacamoleb \
    vendor.product.manufacturer=oneplus

# SurfaceFlinger
PRODUCT_PROPERTY_OVERRIDES += \
    ro.surface_flinger.protected_contents=true \
    ro.surface_flinger.use_smart_90_for_video=true \
    ro.surface_flinger.set_display_power_timer_ms=10000 \
    ro.surface_flinger.set_touch_timer_ms=5000 \
    ro.surface_flinger.set_idle_timer_ms=9000
