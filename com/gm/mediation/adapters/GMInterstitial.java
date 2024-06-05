package com.gm.mediation.adapters;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.bytedance.sdk.openadsdk.TTFullScreenVideoAd;
import com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot;
import com.bytedance.sdk.openadsdk.mediation.manager.MediationFullScreenManager;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;

import java.util.HashMap;
import java.util.Map;

public class GMInterstitial extends TPInterstitialAdapter {

    private static final String TAG = "GMInterstitial";
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private TTFullScreenVideoAd ttFullScreenAd;
    private OnC2STokenListener onC2STokenListener;
    private boolean videoMute = true;
    private String slotId,mName, appId;

    @Override
    public void getC2SBidding(final Context context, final Map<String, Object> localParams, final Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        this.onC2STokenListener = onC2STokenListener;
        isC2SBidding = true;
        loadCustomAd(context, localParams, tpParams);
    }

    @Override
    public void loadCustomAd(Context context, Map<String, Object> localParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null && !isC2SBidding) {
            return;
        }

        initRequestParams(tpParams);

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

        InitManager.getInstance().initSDK(context, localParams, tpParams,  new InitManager.InitCallback() {
            @Override
            public void onSuccess() {
                if (isC2SBidding && isBiddingLoaded) {
                    if (mLoadAdapterListener != null) {
                        setNetworkObjectAd(ttFullScreenAd);
                        mLoadAdapterListener.loadAdapterLoaded(null);
                    }
                    return;
                }

                Activity activity = GlobalTradPlus.getInstance().getActivity();
                if (activity == null) {
                    loadFailed(new TPError(ADAPTER_ACTIVITY_ERROR), "", "GMInterstitial need activity，but activity == null.");
                    return;
                }

                TTAdNative adNativeLoader = TTAdSdk.getAdManager().createAdNative(activity);

                AdSlot adslot = new AdSlot.Builder().setCodeId(slotId).setMediationAdSlot(new MediationAdSlot.Builder().setMuted(videoMute).build()).build();

                adNativeLoader.loadFullScreenVideoAd(adslot, new TTAdNative.FullScreenVideoAdListener() {
                    @Override
                    public void onError(int i, String s) {
                        Log.i(TAG, "onError: ");
                        loadFailed(new TPError(NETWORK_NO_FILL), i + "", s);
                    }

                    @Override
                    public void onFullScreenVideoAdLoad(TTFullScreenVideoAd ttFullScreenVideoAd) {

                    }

                    @Override
                    public void onFullScreenVideoCached() {

                    }

                    @Override
                    public void onFullScreenVideoCached(TTFullScreenVideoAd ttFullScreenVideoAd) {
                        if (ttFullScreenVideoAd == null) {
                            loadFailed(new TPError(NETWORK_NO_FILL), "", "onFullScreenVideoCached,but ttFullScreenVideoAd == null");
                            return;
                        }

                        ttFullScreenAd = ttFullScreenVideoAd;
                        Log.i(TAG, "onFullScreenVideoCached: ");
                        if (isC2SBidding) {
                            if (onC2STokenListener != null) {
                                double bestPrice = GMUtil.getBestPrice(ttFullScreenAd);
                                Log.i(TAG, "bid price: " + bestPrice);
                                if (bestPrice <= 0) {
                                    loadFailed(null, "", "onFullScreenVideoCached,but bestPrice == null");
                                    return;
                                }

                                isBiddingLoaded = true;
                                Map<String, Object> hashMap = new HashMap<>();
                                hashMap.put("ecpm", bestPrice);
                                onC2STokenListener.onC2SBiddingResult(hashMap);
                            }
                            return;
                        }

                        if (mLoadAdapterListener != null) {
                            setNetworkObjectAd(ttFullScreenAd);
                            mLoadAdapterListener.loadAdapterLoaded(null);
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

    private void initRequestParams(Map<String, String> tpParams) {
        if (tpParams != null && tpParams.size() > 0) {
            if (tpParams.containsKey("placementId")) {
                slotId = tpParams.get("placementId");
            }

            if (tpParams.containsKey("videoMute")) {
                String videoMuteParms = tpParams.get("videoMute");
                if ("false".equals(videoMuteParms)) {
                    videoMute = false;
                }
            }

            if (tpParams.containsKey("name")) {
                mName = tpParams.get("name");
            }

            if (tpParams.containsKey("appId")) {
                appId = tpParams.get("appId");
            }
        }
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
    public boolean isReady() {
        if (ttFullScreenAd != null) {
            MediationFullScreenManager mediationManager = ttFullScreenAd.getMediationManager();
            if (mediationManager != null) {
                return mediationManager.isReady();
            }
        }
        return false;
    }


    @Override
    public void showAd() {
        if (mShowListener == null) return;

        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mShowListener != null) {
                TPError tpError = new TPError(ADAPTER_ACTIVITY_ERROR);
                tpError.setErrorMessage("GMInterstital need activity，but activity == null.");
                mShowListener.onAdVideoError(tpError);
            }
            return;
        }

        TPError tpErrorShow = new TPError(UNSPECIFIED);
        if (ttFullScreenAd == null) {
            if (mShowListener != null) {
                tpErrorShow.setErrorMessage("showFailed: GMFullVideoAd == null");
                mShowListener.onAdVideoError(tpErrorShow);
            }
            return;
        }

        ttFullScreenAd.setFullScreenVideoAdInteractionListener(new TTFullScreenVideoAd.FullScreenVideoAdInteractionListener() {
            @Override
            public void onAdShow() {
                Log.i(TAG, "onAdShow: ");
                if (mShowListener != null) {
                    mShowListener.onAdShown();
                    mShowListener.onAdVideoStart();
                }
            }

            @Override
            public void onAdVideoBarClick() {
                Log.i(TAG, "onAdVideoBarClick: ");
                if (mShowListener != null) {
                    mShowListener.onAdVideoClicked();
                }
            }

            @Override
            public void onAdClose() {
                Log.i(TAG, "onAdClose: ");
                if (mShowListener != null) {
                    mShowListener.onAdClosed();
                }
            }

            @Override
            public void onVideoComplete() {
                Log.i(TAG, "onVideoComplete: ");
                if (mShowListener != null) {
                    mShowListener.onAdVideoEnd();
                }
            }

            @Override
            public void onSkippedVideo() {

            }
        });
        ttFullScreenAd.showFullScreenVideoAd(activity);
    }


    @Override
    public void clean() {
        if (ttFullScreenAd != null) {
            MediationFullScreenManager mediationManager = ttFullScreenAd.getMediationManager();
            if (mediationManager != null) {
                mediationManager.destroy();
            }
        }
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
