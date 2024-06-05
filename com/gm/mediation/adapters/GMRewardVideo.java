package com.gm.mediation.adapters;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.bytedance.sdk.openadsdk.TTRewardVideoAd;
import com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot;
import com.bytedance.sdk.openadsdk.mediation.manager.MediationFullScreenManager;
import com.bytedance.sdk.openadsdk.mediation.manager.MediationRewardManager;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;

import java.util.HashMap;
import java.util.Map;

public class GMRewardVideo extends TPRewardAdapter {

    private static final String TAG = "GMRewardVideo";
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private OnC2STokenListener onC2STokenListener;
    private String slotId,mName, appId;
    private String userId, customData;
    private boolean videoMute = true;
    private TTRewardVideoAd rewardVideoAd;
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;
    private boolean alwaysRewardUserAgain;

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

        initRequestParams(tpParams,localParams);

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
                        setNetworkObjectAd(rewardVideoAd);
                        mLoadAdapterListener.loadAdapterLoaded(null);
                    }
                    return;
                }

                Activity activity = GlobalTradPlus.getInstance().getActivity();
                if (activity == null) {
                    loadFailed(new TPError(ADAPTER_ACTIVITY_ERROR), "", "GMRewardAd need activity，but activity == null.");
                    return;
                }

                MediationAdSlot.Builder mediationAdSoltBuilder = new MediationAdSlot.Builder();
                mediationAdSoltBuilder.setMuted(videoMute);

                AdSlot.Builder adSlotBuilder = new AdSlot.Builder().setCodeId(slotId)
                        .setMediationAdSlot(mediationAdSoltBuilder.build());

                if (!TextUtils.isEmpty(userId)) {
                    adSlotBuilder.setUserID(userId);
                }

                if (!TextUtils.isEmpty(customData)) {
                    adSlotBuilder.setUserData(customData);
                }

                TTAdNative adNativeLoader = TTAdSdk.getAdManager().createAdNative(activity);

                adNativeLoader.loadRewardVideoAd(adSlotBuilder.build(), new TTAdNative.RewardVideoAdListener() {
                    @Override
                    public void onError(int i, String s) {
                        loadFailed(new TPError(NETWORK_NO_FILL), i + "", s);
                    }

                    @Override
                    public void onRewardVideoAdLoad(TTRewardVideoAd ttRewardVideoAd) {

                    }

                    @Override
                    public void onRewardVideoCached() {

                    }

                    @Override
                    public void onRewardVideoCached(TTRewardVideoAd ttRewardVideoAd) {
                        if (ttRewardVideoAd == null) {
                            loadFailed(new TPError(NETWORK_NO_FILL), "", "onRewardVideoCached,but ttRewardVideoAd == null");
                            return;
                        }

                        rewardVideoAd = ttRewardVideoAd;
                        Log.i(TAG, "onFullScreenVideoCached: ");
                        if (isC2SBidding) {
                            if (onC2STokenListener != null) {
                                double bestPrice = GMUtil.getBestPrice(rewardVideoAd);
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
                            setNetworkObjectAd(rewardVideoAd);
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


    private void initRequestParams(Map<String, String> tpParams,Map<String, Object> userParams) {
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

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey("user_id")) {
                Object user_id = userParams.get("user_id");
                if (user_id instanceof String) {
                    userId = (String)user_id;
                }
            }

            if (userParams.containsKey("custom_data")) {
                Object custom_data = userParams.get("custom_data");
                if (custom_data instanceof String) {
                    customData = (String)custom_data;
                }
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
        if (rewardVideoAd != null) {
            MediationRewardManager mediationManager = rewardVideoAd.getMediationManager();
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
        if (rewardVideoAd == null) {
            if (mShowListener != null) {
                tpErrorShow.setErrorMessage("showFailed: rewardVideoAd == null");
                mShowListener.onAdVideoError(tpErrorShow);
            }
            return;
        }

        rewardVideoAd.setRewardAdInteractionListener(new TTRewardVideoAd.RewardAdInteractionListener() {
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
                    if (hasGrantedReward || alwaysRewardUser) {
                        mShowListener.onReward();
                    }
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
            public void onVideoError() {
                if (mShowListener != null) {
                    mShowListener.onAdVideoError(new TPError(SHOW_FAILED));
                }
            }

            @Override
            public void onRewardVerify(boolean b, int i, String s, int i1, String s1) {

            }

            @Override
            public void onRewardArrived(boolean b, int i, Bundle bundle) {
                Log.i(TAG, "onRewardVerify 是否有效 :" + b);
                hasGrantedReward = b;
            }

            @Override
            public void onSkippedVideo() {
                alwaysRewardUser = false;
                if (mShowListener != null) {
                    mShowListener.onRewardSkip();
                }
            }
        });

        rewardVideoAd.setRewardPlayAgainInteractionListener(new TTRewardVideoAd.RewardAdInteractionListener() {
            @Override
            public void onAdShow() {
                Log.i(TAG, "PlayAgain onAdShow: ");
                if (mShowListener != null) {
                    mShowListener.onAdShown();
                    mShowListener.onAdAgainVideoStart();
                }
            }

            @Override
            public void onAdVideoBarClick() {
                Log.i(TAG, "PlayAgain onRewardClick: ");
                if (mShowListener != null) {
                    mShowListener.onAdAgainVideoClicked();
                }
            }

            @Override
            public void onAdClose() {
                Log.i(TAG, "PlayAgain onAdClose: ");
                if (mShowListener != null) {
                    if (hasGrantedReward || alwaysRewardUserAgain) {
                        mShowListener.onPlayAgainReward();
                    }
                    mShowListener.onAdClosed();
                }
            }

            @Override
            public void onVideoComplete() {
                Log.i(TAG, "PlayAgain onVideoComplete: ");
                if (mShowListener != null) {
                    mShowListener.onAdAgainVideoEnd();
                }
            }

            @Override
            public void onVideoError() {
                if (mShowListener != null) {
                    mShowListener.onAdVideoError(new TPError(SHOW_FAILED));
                }
            }

            @Override
            public void onRewardVerify(boolean b, int i, String s, int i1, String s1) {

            }

            @Override
            public void onRewardArrived(boolean b, int i, Bundle bundle) {
                Log.i(TAG, "PlayAgain onRewardVerify 是否有效 :" + b);
                hasGrantedReward = b;
            }

            @Override
            public void onSkippedVideo() {
                Log.i(TAG, "PlayAgain onSkippedVideo: ");
                alwaysRewardUserAgain = false;
                if (mShowListener != null) {
                    mShowListener.onRewardSkip();
                }

            }
        });
        rewardVideoAd.showRewardVideoAd(activity);
    }

    @Override
    public void clean() {
        if (rewardVideoAd != null) {
            MediationRewardManager mediationManager = rewardVideoAd.getMediationManager();
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
