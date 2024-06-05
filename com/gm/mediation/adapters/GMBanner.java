package com.gm.mediation.adapters;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdDislike;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.base.common.TPError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GMBanner extends TPBannerAdapter {

    private static final String TAG = "GMBanner";
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private OnC2STokenListener onC2STokenListener;
    private String slotId, appId, mName;
    private TPBannerAdImpl mTpBannerAd;
    private String bannerSize;
    private TTNativeExpressAd mBannerAd;
    private int mAdHeight;
    private int mAdWidth;

    @Override
    public void getC2SBidding(Context context, Map<String, Object> localParams, Map<String, String> tpParams, OnC2STokenListener onC2STokenListener) {
        this.onC2STokenListener = onC2STokenListener;
        isC2SBidding = true;
        loadCustomAd(context, localParams, tpParams);
    }

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
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

        InitManager.getInstance().initSDK(context, userParams, tpParams, new InitManager.InitCallback() {
            @Override
            public void onSuccess() {
                if (isC2SBidding && isBiddingLoaded) {
                    if (mLoadAdapterListener != null) {
                        mLoadAdapterListener.loadAdapterLoaded(mTpBannerAd);
                    }
                    return;
                }

                Activity activity = GlobalTradPlus.getInstance().getActivity();
                if (activity == null) {
                    loadFailed(new TPError(ADAPTER_ACTIVITY_ERROR), "", "Gromore need activity，but activity == null.");
                    return;
                }

                calculateAdSize(activity, bannerSize ,userParams);

                TTAdNative adNativeLoader = TTAdSdk.getAdManager().createAdNative(activity);
                AdSlot.Builder adSlotBuilder = new AdSlot.Builder();
                adSlotBuilder.setCodeId(slotId);
                adSlotBuilder.setImageAcceptedSize(mAdWidth, mAdHeight);
                Log.i(TAG, "slotId: " + slotId + ", loadBannerExpressAd " + ", AdWidth: " + mAdWidth + ", mAdHeight: " + mAdHeight);
                adNativeLoader.loadBannerExpressAd(adSlotBuilder.build(), new TTAdNative.NativeExpressAdListener() {
                    @Override
                    public void onError(int i, String s) {
                        loadFailed(new TPError(NETWORK_NO_FILL), i + "", s);
                    }

                    @Override
                    public void onNativeExpressAdLoad(List<TTNativeExpressAd> list) {
                        boolean ready = list != null && list.size() > 0;
                        if (!ready) {
                            loadFailed(new TPError(NETWORK_NO_FILL), "", "onNativeExpressAdLoad,but list == null");
                            return;
                        }

                        mBannerAd = list.get(0);
                        if (mBannerAd == null) {
                            loadFailed(new TPError(NETWORK_NO_FILL), "", "onNativeExpressAdLoad,but BannerAd == null ");
                            return;
                        }

                        mBannerAd.setExpressInteractionListener(new TTNativeExpressAd.ExpressAdInteractionListener() {
                            @Override
                            public void onAdClicked(View view, int i) {
                                Log.i(TAG, "onAdClicked: ");
                                if (mTpBannerAd != null) {
                                    mTpBannerAd.adClicked();
                                }
                            }

                            @Override
                            public void onAdShow(View view, int i) {
                                bindDislikeCallbackListener(activity, mBannerAd);

                                if (mTpBannerAd != null) {
                                    Log.i(TAG, "onAdShow: ");
                                    mTpBannerAd.adShown();
                                }
                            }

                            @Override
                            public void onRenderFail(View view, String s, int i) {
                            }

                            @Override
                            public void onRenderSuccess(View view, float v, float v1) {

                            }
                        });

                        View bannerView = mBannerAd.getExpressAdView();
                        if (bannerView == null) {
                            Log.i(TAG, "onNativeExpressAdLoad,but bannerView == null");
                            loadFailed(new TPError(NETWORK_NO_FILL), "", "onNativeExpressAdLoad,but bannerView == null ");
                            return;
                        }

                        mTpBannerAd = new TPBannerAdImpl(null, bannerView);

                        if (isC2SBidding) {
                            if (onC2STokenListener != null) {
                                double bestPrice = GMUtil.getBestPrice(mBannerAd);
                                Log.i(TAG, "bid price: " + bestPrice);
                                if (bestPrice <= 0) {
                                    loadFailed(null, "", "onNativeExpressAdLoad,but bestPrice == null");
                                    return;
                                }

                                isBiddingLoaded = true;
                                Map<String, Object> hashMap = new HashMap<>();
                                hashMap.put("ecpm", bestPrice);
                                onC2STokenListener.onC2SBiddingResult(hashMap);
                            }
                            return;
                        }

                        Log.i(TAG, "onAdLoaded: ");
                        if (mLoadAdapterListener != null) {
                            mLoadAdapterListener.loadAdapterLoaded(mTpBannerAd);
                        }
                    }
                });


            }

            @Override
            public void onFailed(String code, String msg) {
                loadFailed(new TPError(INIT_FAILED), code, msg);
            }
        });

    }

    private void initRequestParams(Map<String, String> tpParams) {
        if (tpParams != null && tpParams.size() > 0) {
            if (tpParams.containsKey("placementId")) {
                slotId = tpParams.get("placementId");
            }

            if (tpParams.containsKey("bannerSize")) {
                bannerSize = tpParams.get("bannerSize");
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

    private void bindDislikeCallbackListener(Activity activity, TTNativeExpressAd bannerAd) {
        if (bannerAd == null || activity == null) return;

        Log.i(TAG, "bindDislikeCallbackListener: ");
        bannerAd.setDislikeCallback(activity, new TTAdDislike.DislikeInteractionCallback() {
            @Override
            public void onShow() {

            }

            @Override
            public void onSelected(int i, String s, boolean b) {
                if (mTpBannerAd != null) {
                    mTpBannerAd.adClosed();
                }
            }

            @Override
            public void onCancel() {

            }
        });
    }


    @Override
    public void clean() {
        if (mBannerAd != null) {
            mBannerAd.destroy();
        }
    }


    private void calculateAdSize(Activity activity, String adSize, Map<String, Object> localParams) {
        try {
            if (!TextUtils.isEmpty(adSize)) {
                switch (adSize) {
                    case "1":
                        mAdWidth = GMUtil.dip2px(activity, 320);
                        mAdHeight = GMUtil.dip2px(activity, 50);
                        break;
                    case "2":
                        mAdWidth = GMUtil.dip2px(activity, 320);
                        mAdHeight = GMUtil.dip2px(activity, 100);
                        break;
                    case "3":
                        mAdWidth = GMUtil.dip2px(activity, 300);
                        mAdHeight = GMUtil.dip2px(activity, 250);
                        break;
                    case "4":
                        mAdWidth = GMUtil.dip2px(activity, 468);
                        mAdHeight = GMUtil.dip2px(activity, 60);
                        break;
                    case "5":
                        mAdWidth = GMUtil.dip2px(activity, 728);
                        mAdHeight = GMUtil.dip2px(activity, 90);
                        break;
                }
            }

            if (localParams != null && localParams.size() > 0) {
                if (localParams.containsKey("width")) {
                    Object width = localParams.get("width");
                    if(width instanceof Integer) {
                        mAdWidth = GMUtil.dip2px(activity, (int)width);
                    }
                }

                if (localParams.containsKey("height")) {
                    Object height = localParams.get("height");
                    if(height instanceof Integer) {
                        mAdHeight = GMUtil.dip2px(activity, (int)height);
                    }
                }
            }

            if (mAdWidth <= 0 || mAdHeight <= 0) {
                mAdWidth = GMUtil.dip2px(activity, 320);
                mAdHeight = GMUtil.dip2px(activity, 50);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
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
