package com.gm.mediation.adapters;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.CSJAdError;
import com.bytedance.sdk.openadsdk.CSJSplashAd;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot;
import com.bytedance.sdk.openadsdk.mediation.manager.MediationSplashManager;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.splash.TPSplashAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.util.DeviceUtils;

import java.util.HashMap;
import java.util.Map;

public class GMSplash extends TPSplashAdapter {

    private static final String TAG = "GMSplash";
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private OnC2STokenListener onC2STokenListener;
    private String slotId,mName, appId;
    //开屏广告加载超时时间,建议大于3000,这里为了冷启动第一次加载到广告并且展示,示例设置了3500ms
    private int timeout = 3500;
    private boolean videoMute = true;
    private int mHeightPx = 0;// 开发者本地传入
    private int mWidthPx = 0;
    private CSJSplashAd mCSJSplashAd;


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

        initRequestParams(context,userParams,tpParams);

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

        InitManager.getInstance().initSDK(context, userParams, tpParams,  new InitManager.InitCallback() {
            @Override
            public void onSuccess() {
                if (isC2SBidding && isBiddingLoaded) {
                    if (mLoadAdapterListener != null) {
                        setNetworkObjectAd(mCSJSplashAd);
                        mLoadAdapterListener.loadAdapterLoaded(null);
                    }
                    return;
                }

                Activity activity = GlobalTradPlus.getInstance().getActivity();
                if (activity == null) {
                    loadFailed(new TPError(ADAPTER_ACTIVITY_ERROR), "", "GMSplash need activity，but activity == null.");
                    return;
                }

                TTAdNative adNativeLoader = TTAdSdk.getAdManager().createAdNative(activity);

                AdSlot adSlot = new AdSlot.Builder()
                        .setCodeId(slotId)
                        .setMediationAdSlot(new MediationAdSlot.Builder()
                                .setMuted(videoMute)
                                .build())
                        .setImageAcceptedSize(mWidthPx, mHeightPx)//单位px
                        .build();
                adNativeLoader.loadSplashAd(adSlot, new TTAdNative.CSJSplashAdListener() {
                    @Override
                    public void onSplashLoadSuccess(CSJSplashAd csjSplashAd) {

                    }

                    @Override
                    public void onSplashLoadFail(CSJAdError csjAdError) {
                        if (csjAdError != null) {
                            loadFailed(new TPError(NETWORK_NO_FILL), csjAdError.getCode() + "", csjAdError.getMsg());
                        }else{
                            loadFailed(new TPError(NETWORK_NO_FILL), "", "onSplashLoadFail");
                        }

                    }

                    @Override
                    public void onSplashRenderSuccess(CSJSplashAd csjSplashAd) {
                        if (csjSplashAd == null) {
                            loadFailed(new TPError(NETWORK_NO_FILL), "", "onSplashRenderSuccess, but csjSplashAd == null");
                            return;
                        }

                        mCSJSplashAd = csjSplashAd;
                        if (isC2SBidding) {
                            if (onC2STokenListener != null) {
                                double bestPrice = GMUtil.getBestPrice(mCSJSplashAd);
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
                            setNetworkObjectAd(mCSJSplashAd);
                            mLoadAdapterListener.loadAdapterLoaded(null);
                        }

                    }

                    @Override
                    public void onSplashRenderFail(CSJSplashAd csjSplashAd, CSJAdError csjAdError) {

                    }
                }, timeout);
            }

            @Override
            public void onFailed(String s, String s1) {
                loadFailed(new TPError(INIT_FAILED), s, s);
            }
        });
    }

    private void initRequestParams(Context context,Map<String, Object> userParams, Map<String, String> tpParams) {
        if (tpParams != null && tpParams.size() > 0) {
            if (tpParams.containsKey("placementId")) {
                slotId = tpParams.get("placementId");
            }

            if (tpParams.containsKey("name")) {
                mName = tpParams.get("name");
            }

            if (tpParams.containsKey("videoMute")) {
                String videoMuteParms = tpParams.get("videoMute");
                if ("false".equals(videoMuteParms)) {
                    videoMute = false;
                }
            }

            if (tpParams.containsKey("tolerateTimeout")) {
                int timeoutParms = Integer.parseInt(tpParams.get("tolerateTimeout"));

                if (timeoutParms > 0) {
                    timeout = timeoutParms;
                    Log.i(TAG, "timeout: " + timeout);
                }
            }

            if (tpParams.containsKey("appId")) {
                appId = tpParams.get("appId");
            }
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey("splash_height")) {
                try {
                    int height = (int) userParams.get("splash_height");
                    // CSJ setImageAcceptedSize 要求PX,此时不做转换会导致半屏图片模糊
                    mHeightPx = DeviceUtils.dip2px(context, height);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }

            if (userParams.containsKey("splash_width")) {
                try {
                    int width = (int) userParams.get("splash_width");
                    mWidthPx = DeviceUtils.dip2px(context, width);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }

        if (mWidthPx <= 0) {
            mWidthPx = context.getResources().getDisplayMetrics().widthPixels;
        }

        if (mHeightPx <= 0) {
            mHeightPx =context.getResources().getDisplayMetrics().heightPixels;
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


    private int onAdShow = 0;

    @Override
    public void showAd() {
        if (mShowListener == null) return;

        if (mCSJSplashAd == null || mAdContainerView == null) {
            TPError tpError = new TPError(SHOW_FAILED);
            tpError.setErrorMessage("AdContainerView == null or GMSplashA == null");
            if (mShowListener != null) {
                mShowListener.onAdVideoError(tpError);
            }
            return;
        }


        View splashView = mCSJSplashAd.getSplashView();
        if (splashView == null) {
            TPError tpError = new TPError(SHOW_FAILED);
            tpError.setErrorMessage("splashView == null");
            if (mShowListener != null) {
                mShowListener.onAdVideoError(tpError);
            }
            return;
        }

        mCSJSplashAd.setSplashAdListener(new CSJSplashAd.SplashAdListener() {
            @Override
            public void onSplashAdShow(CSJSplashAd csjSplashAd) {
                if (mShowListener != null && onAdShow == 0) {
                    Log.i(TAG, "onAdShow: ");
                    onAdShow = 1;
                    mShowListener.onAdShown();
                }
            }

            @Override
            public void onSplashAdClick(CSJSplashAd csjSplashAd) {
                Log.i(TAG, "onAdClicked: ");
                if (mShowListener != null) {
                    mShowListener.onAdClicked();
                }
            }

            @Override
            public void onSplashAdClose(CSJSplashAd csjSplashAd, int i) {
                Log.i(TAG, "onSplashAdClose: ");
                if (mShowListener != null) {
                    mShowListener.onAdClosed();
                }
            }
        });


        ViewParent vp = splashView.getParent();
        if (vp instanceof ViewGroup) {
            ((ViewGroup) vp).removeView(splashView);
        }

        mAdContainerView.removeAllViews();
        mAdContainerView.addView(splashView);
    }

    @Override
    public void clean() {
        if (mCSJSplashAd != null) {
            MediationSplashManager mediationManager = mCSJSplashAd.getMediationManager();
            if (mediationManager != null) {
                mediationManager.destroy();
            }
        }

    }

    @Override
    public boolean isReady() {
        if (mCSJSplashAd != null) {
            MediationSplashManager mediationManager = mCSJSplashAd.getMediationManager();
            if (mediationManager != null) {
                return mediationManager.isReady();
            }
        }
        return false;
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
