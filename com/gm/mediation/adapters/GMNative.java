package com.gm.mediation.adapters;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

import android.app.Activity;
import android.content.Context;
import android.media.metrics.LogSessionId;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdDislike;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.bytedance.sdk.openadsdk.TTFeedAd;
import com.bytedance.sdk.openadsdk.mediation.MediationConstant;
import com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot;
import com.bytedance.sdk.openadsdk.mediation.ad.MediationExpressRenderListener;
import com.bytedance.sdk.openadsdk.mediation.manager.MediationNativeManager;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.common.util.DeviceUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GMNative extends TPNativeAdapter {

    private static final String TAG = "GMNative";
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private OnC2STokenListener onC2STokenListener;
    private GMNativeAd gmNativeAd;
    private TTFeedAd ttFeedAd;
    private View ttView;
    private String slotId,mName, appId;
    private int mAdWidth, mAdHeight;
    private boolean videoMute = true;
    public static final int NATIVE_DEFAULT_HEIGHT = 340;

    @Override
    public void getC2SBidding(Context context, Map<String, Object> localParams, Map<String, String> tpParams, OnC2STokenListener onC2STokenListener) {
        this.onC2STokenListener = onC2STokenListener;
        isC2SBidding = true;
        loadCustomAd(context, localParams, tpParams);
    }

    @Override
    public void loadCustomAd(Context context, Map<String, Object> localParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null && !isC2SBidding) {
            return;
        }

        initRequestParams(context,tpParams, localParams);

        if (TextUtils.isEmpty(slotId) || TextUtils.isEmpty(appId)) {
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("", ADAPTER_CONFIGURATION_ERROR);
                }
            } else {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }

        InitManager.getInstance().initSDK(context, localParams, tpParams, new InitManager.InitCallback() {
            @Override
            public void onSuccess() {
                if (isC2SBidding && isBiddingLoaded) {
                    if (mLoadAdapterListener != null) {
                        mLoadAdapterListener.loadAdapterLoaded(gmNativeAd);
                    }
                    return;
                }

                Activity activity = GlobalTradPlus.getInstance().getActivity();
                if (activity == null) {
                    loadFailed(new TPError(ADAPTER_ACTIVITY_ERROR), "", "Gromore need activity，but activity == null.");
                    return;
                }


                TTAdNative ttAdNative = TTAdSdk.getAdManager().createAdNative(activity);

                AdSlot adSlot = new AdSlot.Builder()
                        .setCodeId(slotId)
                        .setImageAcceptedSize(DeviceUtils.dip2px(activity, mAdWidth), DeviceUtils.dip2px(activity, mAdHeight)) // 自渲染设置宽高，单位px
                        .setExpressViewAcceptedSize(mAdWidth, mAdHeight) // 模板设置宽高，单位dp
                        .setAdCount(1)
                        .setMediationAdSlot(new MediationAdSlot.Builder()
                                .setExtraObject(MediationConstant.KEY_BAIDU_CACHE_VIDEO_ONLY_WIFI, true)
                                .setExtraObject(MediationConstant.KEY_GDT_MIN_VIDEO_DURATION, 1000)
                                .setExtraObject(MediationConstant.KEY_GDT_MAX_VIDEO_DURATION, 2000)
                                .setMuted(videoMute)
                                .build())
                        .build();
                ttAdNative.loadFeedAd(adSlot, new TTAdNative.FeedAdListener() {
                    @Override
                    public void onError(int i, String s) {
                        loadFailed(new TPError(NETWORK_NO_FILL), i+"", s);
                    }

                    @Override
                    public void onFeedAdLoad(List<TTFeedAd> list) {
                        if (list != null && list.size() > 0) {
                            ttFeedAd = list.get(0);
                            if (ttFeedAd == null) {
                                loadFailed(new TPError(NETWORK_NO_FILL), "", "onFeedAdLoad,but TTFeedAd == null");
                                return;
                            }

                            MediationNativeManager mediationManager = ttFeedAd.getMediationManager();
                            if (mediationManager == null) {
                                loadFailed(new TPError(NETWORK_NO_FILL), "", "onFeedAdLoad,but mediationManager == null");
                                return;
                            }

                            bindDislike(activity,ttFeedAd,mediationManager);

                            boolean express = mediationManager.isExpress();
                            if (express) {
                                Log.i(TAG, "loadExpressRender: ");
                                loadExpressRender(ttFeedAd);
                            }else {
                                Log.i(TAG, "loadNormalRander: ");
                                loadNormalRander(ttFeedAd,context);
                            }
                        }else {
                            loadFailed(new TPError(NETWORK_NO_FILL), "", "onFeedAdLoad: ad is null!");
                        }

                    }
                });
            }

            @Override
            public void onFailed(String s, String s1) {
                loadFailed(new TPError(INIT_FAILED), s, s);
            }
        });
    }

    private void bindDislike(Activity activity,TTFeedAd ttFeedAd, MediationNativeManager mediationManager) {
        if (activity == null || ttFeedAd == null || mediationManager == null) return;
        if (mediationManager.hasDislike()) {
            ttFeedAd.setDislikeCallback(activity, new TTAdDislike.DislikeInteractionCallback() {
                @Override
                public void onShow() {

                }

                @Override
                public void onSelected(int i, String s, boolean b) {
                    Log.i(TAG, "onSelected: ");
                    if (gmNativeAd != null) {
                        gmNativeAd.adClosed();
                    }
                }

                @Override
                public void onCancel() {

                }
            });
        }
    }

    private void loadNormalRander(TTFeedAd ttFeedAd, Context context) {
        if (isC2SBidding) {
            if (onC2STokenListener != null) {
                double bestPrice = GMUtil.getBestPrice(ttFeedAd);
                Log.i(TAG, "bid price: " + bestPrice);
                if (bestPrice <= 0) {
                    loadFailed(null, "", "onNativeExpressAdLoad,but bestPrice == null");
                    return;
                }

                gmNativeAd = new GMNativeAd(ttFeedAd, null);
                isBiddingLoaded = true;
                Map<String, Object> hashMap = new HashMap<>();
                hashMap.put("ecpm", bestPrice);
                onC2STokenListener.onC2SBiddingResult(hashMap);
            }
            return;
        }

        Log.i(TAG, "onAdLoaded: ");
        if (mLoadAdapterListener != null) {
            gmNativeAd = new GMNativeAd(ttFeedAd, null);
            mLoadAdapterListener.loadAdapterLoaded(gmNativeAd);
        }
    }

    private void loadExpressRender(TTFeedAd ttFeedAd) {
        ttFeedAd.setExpressRenderListener(new MediationExpressRenderListener() {
            @Override
            public void onRenderSuccess(View view, float v, float v1, boolean b) {
                if (isC2SBidding) {
                    if (onC2STokenListener != null) {
                        double bestPrice = GMUtil.getBestPrice(ttFeedAd);
                        Log.i(TAG, "bid price: " + bestPrice);
                        if (bestPrice <= 0) {
                            loadFailed(null, "", "onNativeExpressAdLoad,but bestPrice == null");
                            return;
                        }

                        Log.i(TAG, "onRenderSuccess: ");
                        gmNativeAd = new GMNativeAd(ttFeedAd, ttView);
                        isBiddingLoaded = true;
                        Map<String, Object> hashMap = new HashMap<>();
                        hashMap.put("ecpm", bestPrice);
                        onC2STokenListener.onC2SBiddingResult(hashMap);
                    }
                    return;
                }

                Log.i(TAG, "onRenderSuccess: ");
                if (mLoadAdapterListener != null) {
                    gmNativeAd = new GMNativeAd(ttFeedAd, ttView);
                    mLoadAdapterListener.loadAdapterLoaded(gmNativeAd);
                }
            }

            @Override
            public void onRenderFail(View view, String s, int i) {

            }

            @Override
            public void onAdClick() {
                if (gmNativeAd != null) {
                    Log.i(TAG, "onAdClick: ");
                    gmNativeAd.adClicked();
                }
            }

            @Override
            public void onAdShow() {
                if (gmNativeAd != null) {
                    Log.i(TAG, "onAdShow: ");
                    gmNativeAd.adShown();
                }
            }
        });

        ttView = ttFeedAd.getAdView();
        if (ttView == null) {
            loadFailed(new TPError(NETWORK_NO_FILL), "", "onFeedAdLoad,but view == null");
            return;
        }

        ttFeedAd.render();
    }

    private void initRequestParams(Context context,Map<String, String> tpParams, Map<String, Object> userParams) {
        if (tpParams != null && tpParams.size() > 0) {
            if (tpParams.containsKey("placementId")) {
                slotId = tpParams.get("placementId");
            }

            if (tpParams.containsKey("name")) {
                mName = tpParams.get("name");
            }

            if (tpParams.containsKey("appId")) {
                appId = tpParams.get("appId");
            }

            if (tpParams.containsKey("videoMute")) {
                String videoMuteParms = tpParams.get("videoMute");
                if ("false".equals(videoMuteParms)) {
                    videoMute = false;
                }
            }
        }


        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey("width")) {
                Object object = userParams.get("width");
                if(object instanceof Integer) {
                    mAdWidth = (int) object;
                }
            }

            if (userParams.containsKey("height")) {
                Object object = userParams.get("height");
                if(object instanceof Integer) {
                    mAdHeight = (int) object;
                }
            }
        }


        if (mAdWidth <= 0) {
            // 模版 默认自适应屏幕
            mAdWidth = context.getResources().getDisplayMetrics().widthPixels;
        }

        if (mAdHeight <= 0) {
            mAdHeight = NATIVE_DEFAULT_HEIGHT;
        }

        Log.i(TAG, "initRequestParams AdWidth : " + mAdWidth + ", AdHeight : "+ mAdHeight);
    }


    private void loadFailed(TPError tpError, String errorCode, String errorMsg) {
        Log.i(TAG, "loadFailed: errorCode :" + errorCode + ", errorMsg :" + errorMsg);
        if (isC2SBidding) {
            if (onC2STokenListener != null) {
                onC2STokenListener.onC2SBiddingFailed(errorCode, errorMsg);
            }
            return;
        }

        if (tpError != null && mLoadAdapterListener != null) {
            tpError.setErrorCode(errorCode);
            tpError.setErrorMessage(errorMsg);
            mLoadAdapterListener.loadAdapterLoadFailed(tpError);
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public String getNetworkName() {
        return mName;
    }

    @Override
    public String getNetworkVersion() {
        return TTAdSdk.getAdManager().getSDKVersion();
    }
}
