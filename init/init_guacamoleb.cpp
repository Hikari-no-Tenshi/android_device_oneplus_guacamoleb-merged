/*
   Copyright (c) 2015, The Linux Foundation. All rights reserved.
   Copyright (C) 2016 The CyanogenMod Project.
   Copyright (C) 2019 The LineageOS Project.
   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are
   met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.
   THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
   WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
   ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
   BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
   CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
   SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
   BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
   WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
   OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
   IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <fstream>
#include <unistd.h>

#include <android-base/properties.h>
#define _REALLY_INCLUDE_SYS__SYSTEM_PROPERTIES_H_
#include <sys/sysinfo.h>
#include <sys/_system_properties.h>

#include "property_service.h"
#include "vendor_init.h"

using android::base::GetProperty;
using android::base::SetProperty;

void property_override(char const prop[], char const value[])
{
    prop_info *pi;
    pi = (prop_info*) __system_property_find(prop);
    if (pi)
        __system_property_update(pi, value, strlen(value));
    else
        __system_property_add(prop, strlen(prop), value, strlen(value));
}
void property_override_dual(char const system_prop[],
    char const vendor_prop[], char const value[])
{
    property_override(system_prop, value);
    property_override(vendor_prop, value);
}

void property_override_multi(char const system_prop[],
    char const vendor_prop[], char const bootimage_prop[], char const value[])
{
    property_override(system_prop, value);
    property_override(vendor_prop, value);
    property_override(bootimage_prop, value);
}

void load_dalvikvm_properties()
{
    struct sysinfo sys;
    sysinfo(&sys);
    if (sys.totalram > 8192ull * 1024 * 1024) {
        // from - phone-xhdpi-12288-dalvik-heap.mk
        property_override("dalvik.vm.heapstartsize", "24m");
        property_override("dalvik.vm.heapgrowthlimit", "384m");
        property_override("dalvik.vm.heaptargetutilization", "0.42");
        property_override("dalvik.vm.heapmaxfree", "56m");
    } else if (sys.totalram > 6144ull * 1024 * 1024) {
        // from - phone-xhdpi-8192-dalvik-heap.mk
        property_override("dalvik.vm.heapstartsize", "24m");
        property_override("dalvik.vm.heapgrowthlimit", "256m");
        property_override("dalvik.vm.heaptargetutilization", "0.46");
        property_override("dalvik.vm.heapmaxfree", "48m");
    } else {
        // from - phone-xhdpi-6144-dalvik-heap.mk
        property_override("dalvik.vm.heapstartsize", "16m");
        property_override("dalvik.vm.heapgrowthlimit", "256m");
        property_override("dalvik.vm.heaptargetutilization", "0.5");
        property_override("dalvik.vm.heapmaxfree", "32m");
    }
    property_override("dalvik.vm.heapsize", "512m");
    property_override("dalvik.vm.heapminfree", "8m");
}

void vendor_load_properties()
{
    std::string build_type;
    build_type = GetProperty("ro.build.type", "");
    if (build_type == "userdebug") {
        property_override_dual("ro.build.type", "ro.vendor.build.type", "user");
        property_override_dual("ro.odm.build.type", "ro.product.build.type", "user");
        property_override_dual("ro.system.build.type", "ro.system_ext.build.type", "user");
        property_override_dual("ro.build.tags", "ro.vendor.build.tags", "release-keys");
        property_override_dual("ro.odm.build.tags", "ro.product.build.tags", "release-keys");
        property_override_dual("ro.system.build.tags", "ro.system_ext.build.tags", "release-keys");
    }

    property_override_dual("ro.boot.vbmeta.device_state", "vendor.boot.vbmeta.device_state", "locked");
    property_override_dual("ro.boot.verifiedbootstate", "vendor.boot.verifiedbootstate", "green");
    property_override("ro.build.description", "OnePlus7-user 11   release-keys");
    property_override("ro.build.keys", "release-keys");
    property_override("ro.is_ever_orange", "0");

    int prj_version = stoi(android::base::GetProperty("ro.boot.prj_version", ""));
    int rf_version = stoi(android::base::GetProperty("ro.boot.rf_version", ""));
    switch (rf_version){
      case 1:
        /* China*/
        property_override_dual("ro.product.model", "ro.product.vendor.model", "GM1900");
        property_override_dual("ro.product.product.model", "ro.product.system_ext.model", "GM1900");
        property_override_dual("ro.product.system.model", "ro.product.odm.model", "GM1900");
        property_override("ro.rf_version", "TDD_FDD_Ch_All");
        property_override("ro.build.display.id", "GM1900_14_220617");
        property_override("ro.build.id.hardware", "GM1900_14_");
        break;
      case 3:
        /* India*/
        property_override_dual("ro.product.model", "ro.product.vendor.model", "GM1901");
        property_override_dual("ro.product.product.model", "ro.product.system_ext.model", "GM1901");
        property_override_dual("ro.product.system.model", "ro.product.odm.model", "GM1901");
        property_override("ro.rf_version", "TDD_FDD_In_All");
        property_override("ro.build.display.id", "GM1901_14_220617");
        property_override("ro.build.id.hardware", "GM1901_14_");
        break;
      case 4:
        /* Europe */
        property_override_dual("ro.product.model", "ro.product.vendor.model", "GM1903");
        property_override_dual("ro.product.product.model", "ro.product.system_ext.model", "GM1903");
        property_override_dual("ro.product.system.model", "ro.product.odm.model", "GM1903");
        property_override("ro.rf_version", "TDD_FDD_Eu_All");
        property_override("ro.build.display.id", "GM1903_14_220617");
        property_override("ro.build.id.hardware", "GM1903_14_");
        break;
      case 5:
        /* Global / US Unlocked */
        property_override_dual("ro.product.model", "ro.product.vendor.model", "GM1905");
        property_override_dual("ro.product.product.model", "ro.product.system_ext.model", "GM1905");
        property_override_dual("ro.product.system.model", "ro.product.odm.model", "GM1905");
        property_override("ro.rf_version", "TDD_FDD_Am_All");
        property_override("ro.build.display.id", "GM1905_14_220617");
        property_override("ro.build.id.hardware", "GM1905_14_");
        break;
      default:
        /* Generic */
        property_override_dual("ro.product.model", "ro.product.vendor.model", "GM1905");
        property_override_dual("ro.product.product.model", "ro.product.system_ext.model", "GM1905");
        property_override_dual("ro.product.system.model", "ro.product.odm.model", "GM1905");
        property_override("ro.rf_version", "TDD_FDD_Am_All");
        property_override("ro.build.display.id", "GM1905_14_220617");
        property_override("ro.build.id.hardware", "GM1905_14_");
        break;
    }

    property_override("vendor.boot.prj_version", std::to_string(prj_version).c_str());
    property_override_dual("vendor.rf.version", "vendor.boot.rf_version", std::to_string(rf_version).c_str());

    std::string serialno = android::base::GetProperty("ro.serialno", "");
    property_override("ro.vendor.serialno", serialno.c_str());

    // From oem_build.prop
    property_override("ro.build.real_device", "true");
    property_override("ro.build.release_type", "release");
    property_override("ro.display.series", "OnePlus 7");

    // dalvikvm props
    load_dalvikvm_properties();
}
