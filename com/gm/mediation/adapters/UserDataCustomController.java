package com.gm.mediation.adapters;


import com.bytedance.sdk.openadsdk.LocationProvider;
import com.bytedance.sdk.openadsdk.TTCustomController;
import com.bytedance.sdk.openadsdk.mediation.init.IMediationPrivacyConfig;
import com.bytedance.sdk.openadsdk.mediation.init.MediationPrivacyConfig;
import com.tradplus.ads.base.annotation.Nullable;

public class UserDataCustomController extends TTCustomController {

    private boolean userAgree; // 隐私控制
    private boolean openPersonalizedAd;// 个性化 TP内部定义默认开启true,关闭false　

    public UserDataCustomController(boolean userAgree, boolean openPersonalizedAd) {
        this.userAgree = userAgree;
        this.openPersonalizedAd = openPersonalizedAd;
    }

    // 重写相应的函数，设置需要设置的权限开关，不重写的将采用默认值
    // 例如，重写isCanUsePhoneState函数返回true，表示允许使用ReadPhoneState权限。
    @Override
    public boolean isCanUsePhoneState() {
        if (!userAgree) {
            return false;
        }
        return true;
    }

    //当isCanUseWifiState=false时，可传入Mac地址信息，穿山甲sdk使用您传入的Mac地址信息
    @Override
    public String getMacAddress() {
        return "";
    }

    public boolean isCanUseLocation() {
        return true;
    }

    @Nullable
    public LocationProvider getTTLocation() {
        return null;
    }

    public boolean alist() {
        if (!userAgree) {
            return false;
        }
        return true;
    }

    @Nullable
    public String getDevImei() {
        return null;
    }

    public boolean isCanUseWifiState() {
        if (!userAgree) {
            return false;
        }
        return true;
    }


    public boolean isCanUseWriteExternal() {
        if (!userAgree) {
            return false;
        }
        return true;
    }

    @Nullable
    public String getDevOaid() {
        return null;
    }

    public boolean isCanUseAndroidId() {
        if (!userAgree) {
            return false;
        }
        return true;
    }

    // 设置聚合隐私控制开关
    @Nullable
    public IMediationPrivacyConfig getMediationPrivacyConfig() {
        return new MediationPrivacyConfig() {

            // 个性化推荐接口 默认false 不限制个性化; true 限制个性化
            @Override
            public boolean isLimitPersonalAds() {
                return !openPersonalizedAd;
            }

            // 是否启用程序化广告推荐 true启用 false不启用
            @Override
            public boolean isProgrammaticRecommend() {
                return super.isProgrammaticRecommend();
            }
        };
    }

    @Nullable
    public String getAndroidId() {
        return null;
    }

    public boolean isCanUsePermissionRecordAudio() {
        if (!userAgree) {
            return false;
        }
        return true;
    }
}
