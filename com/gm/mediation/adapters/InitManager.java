package com.gm.mediation.adapters;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.bytedance.sdk.openadsdk.TTAdConfig;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.bytedance.sdk.openadsdk.TTCustomController;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.TestDeviceUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class InitManager {

    private static final String TAG = "GromoreInit";
    private String appId;
    private static InitManager sInstance;
    public TTCustomController mGMPrivacyConfig;
    private boolean openPersonalizedAd = true;  // 设置个性化开关
    private boolean privacyUserAgree = true;
    private boolean mIsOpenDirectDownload;

    public synchronized static InitManager getInstance() {
        if (sInstance == null) {
            sInstance = new InitManager();
        }
        return sInstance;
    }

    public void initSDKwithMixCSJ(Context context, String appId) {
        if (TextUtils.isEmpty(appId)) {
            throw new IllegalArgumentException("appId isEmpty");
        }else {
            Map<String, String> serverParams = new HashMap<>();
            serverParams.put("appId", appId);
            initSDK(context, new HashMap<>(), serverParams, new InitCallback() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onFailed(String s, String s1) {
                }
            });
        }
    }

    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> serverParams, InitCallback initCallback) {
        if (serverParams != null && serverParams.size() > 0) {
            if (serverParams.containsKey("appId")) {
                appId = serverParams.get("appId");
            }
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey("privacy_useragree")) {
                Object privacy_useragree = userParams.get("privacy_useragree");
                if (privacy_useragree instanceof Boolean) {
                    privacyUserAgree = (boolean) privacy_useragree;
                }
            }

            if (userParams.containsKey("open_personalized")) {
                Object open_personalized = userParams.get("open_personalized");
                if (open_personalized instanceof Boolean) {
                    openPersonalizedAd = (boolean) open_personalized;
                }
            }
        }


        if (TTAdSdk.isSdkReady()) {
            TTAdConfig ttAdConfig = new TTAdConfig.Builder()
                    .customController(mGMPrivacyConfig == null ? new UserDataCustomController(privacyUserAgree,openPersonalizedAd) : getGMPrivacyConfig())
                    .data(getData())
                    .build();
            TTAdSdk.updateAdConfig(ttAdConfig);

            if (initCallback != null) {
                initCallback.onSuccess();
            }
            return;
        }

        String appName = context.getPackageManager()
                .getApplicationLabel(context.getApplicationInfo()).toString();

        final int[] download;
        if (mIsOpenDirectDownload) {
            download = new int[]{TTAdConstant.NETWORK_STATE_MOBILE, TTAdConstant.NETWORK_STATE_2G, TTAdConstant.NETWORK_STATE_3G, TTAdConstant.NETWORK_STATE_WIFI, TTAdConstant.NETWORK_STATE_4G};
        } else {
            download = new int[]{TTAdConstant.NETWORK_STATE_2G};
        }

        TTAdConfig build = new TTAdConfig.Builder()
                .appName(appName)
                .appId(appId)
                .useMediation(true) //使用聚合功能一定要打开此开关，否则不会请求聚合广告，默认这个值为false
                .directDownloadNetworkType(download)
                .customController(mGMPrivacyConfig == null ? new UserDataCustomController(privacyUserAgree, openPersonalizedAd) : getGMPrivacyConfig())
                .data(getData())
                .build();

        TTAdSdk.init(context, build);
        TTAdSdk.start(new TTAdSdk.Callback() {
            @Override
            public void success() {
                Log.i(TAG, "TTAdSdk init, start success: ");
               if (initCallback != null) {
                   initCallback.onSuccess();
               }
            }

            @Override
            public void fail(int i, String s) {
                if (initCallback != null) {
                    initCallback.onFailed(i+"",s);
                }
            }
        });
    }

    public void setOpenDirectDownload(boolean mIsOpenDirectDownload) {
        this.mIsOpenDirectDownload = mIsOpenDirectDownload;
    }

    private String getData() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", "personal_ads_type");
            jsonObject.put("value", openPersonalizedAd ? "1" : "0");
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(jsonObject);
            return jsonArray.toString();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return "";
    }

    public TTCustomController getGMPrivacyConfig() {
        return mGMPrivacyConfig;
    }

    public void setGmPrivacyConfig(TTCustomController gmPrivacyConfig) {
        mGMPrivacyConfig = gmPrivacyConfig;
    }


    public interface InitCallback {
        void onSuccess();

        void onFailed(String var1, String var2);
    }
}
