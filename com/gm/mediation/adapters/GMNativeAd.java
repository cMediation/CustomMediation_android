package com.gm.mediation.adapters;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTFeedAd;
import com.bytedance.sdk.openadsdk.TTImage;
import com.bytedance.sdk.openadsdk.TTNativeAd;
import com.bytedance.sdk.openadsdk.mediation.ad.MediationViewBinder;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPError;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GMNativeAd extends TPBaseAd {

    private static final String TAG = "GMNative";
    private TTFeedAd mttFeedAd;
    private View expressView;
    private TPNativeAdView mTpNativeAdView;

    public GMNativeAd(TTFeedAd ttFeedAd, View view) {
        mttFeedAd = ttFeedAd;
        if (view != null) {
            expressView = view;
        } else {
            //自渲染
            mTpNativeAdView = new TPNativeAdView();

            String title = ttFeedAd.getTitle();
            if (!TextUtils.isEmpty(title)) {
                mTpNativeAdView.setTitle(title);
            }

            String body = ttFeedAd.getDescription();
            if (!TextUtils.isEmpty(body)) {
                mTpNativeAdView.setSubTitle(body);
            }

            int imageMode = mttFeedAd.getImageMode();
            Log.i(TAG, "ImageMode = " + imageMode);
            if (imageMode == TTAdConstant.IMAGE_MODE_SMALL_IMG || imageMode == TTAdConstant.IMAGE_MODE_LARGE_IMG || imageMode == TTAdConstant.IMAGE_MODE_VERTICAL_IMG) {
                //信息流自渲染广告渲染 ：小图广告 大图广告 竖图广告
                List<TTImage> mainImage = ttFeedAd.getImageList();
                if (mainImage != null && mainImage.size() > 0) {
                    TTImage ttImage = mainImage.get(0);
                    if (ttImage != null) {
                        mTpNativeAdView.setMainImageUrl(ttImage.getImageUrl());
                    }
                }
            }

            //信息流自渲染广告渲染 ：视频广告 竖版视频广告
            if (imageMode == TTAdConstant.IMAGE_MODE_VIDEO || imageMode == TTAdConstant.IMAGE_MODE_VIDEO_VERTICAL) {
                Context context = GlobalTradPlus.getInstance().getContext();
                if (context != null) {
                    FrameLayout ttMediaView = new FrameLayout(context);
                    ttMediaView.setTag("gromore_mediaview");
                    ttMediaView.setId(new Random().nextInt());
                    mTpNativeAdView.setMediaView(ttMediaView);
                }else{
                    Log.i(TAG, "自渲染视频类型，但是context为null: ");
                }
            }

            //信息流自渲染广告渲染 ：组图广告
            if (imageMode == TTAdConstant.IMAGE_MODE_GROUP_IMG) {
                List<TTImage> mainImage = ttFeedAd.getImageList();
                if (mainImage != null && mainImage.size() > 3) {
                    List<String> picUrls = new ArrayList<>();
                    List<TTImage> imageList = ttFeedAd.getImageList();
                    if (imageList != null && imageList.size() > 0) {
                        for (int i = 0; i < imageList.size(); i++) {
                            picUrls.add(imageList.get(i).getImageUrl());
                        }
                    }
                    mTpNativeAdView.setPicUrls(picUrls);
                }
            }

            TTImage tticon = ttFeedAd.getIcon();
            if (tticon != null) {
                String icon = tticon.getImageUrl();
                if (!TextUtils.isEmpty(icon)) {
                    mTpNativeAdView.setIconImageUrl(icon);
                }
            }

            Bitmap adLogo = ttFeedAd.getAdLogo();
            if (adLogo != null) {
                BitmapDrawable bitmapDrawable = new BitmapDrawable(adLogo);
                mTpNativeAdView.setAdChoiceImage(bitmapDrawable);
            }

            int appScore = ttFeedAd.getAppScore();
            if (appScore != 0) {
                mTpNativeAdView.setStarRating((double) appScore);
            }

            String callToAction = "";
            switch (ttFeedAd.getInteractionType()) {
                case TTAdConstant.INTERACTION_TYPE_DOWNLOAD:
                    callToAction = TextUtils.isEmpty(ttFeedAd.getButtonText()) ? "立即下载" : ttFeedAd.getButtonText();
                    break;
                case TTAdConstant.INTERACTION_TYPE_DIAL:
                    callToAction = "立即拨打";
                    break;
                case TTAdConstant.INTERACTION_TYPE_LANDING_PAGE:
                case TTAdConstant.INTERACTION_TYPE_BROWSER:
                    callToAction = TextUtils.isEmpty(ttFeedAd.getButtonText()) ? "查看详情" : ttFeedAd.getButtonText();
                    break;
                default:
                    break;
            }

            if (!TextUtils.isEmpty(callToAction)) {
                mTpNativeAdView.setCallToAction(callToAction);
            }

        }

    }

    @Override
    public Object getNetworkObj() {
        return mttFeedAd;
    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> arrayList) {
        if (mttFeedAd != null) {
            mttFeedAd.setVideoAdListener(new TTFeedAd.VideoAdListener() {
                @Override
                public void onVideoLoad(TTFeedAd ttFeedAd) {

                }

                @Override
                public void onVideoError(int i, int i1) {
                    Log.i(TAG, "onVideoError:  code :" + i + ", message :" + i1);
                    TPError tpError = new TPError(SHOW_FAILED);
                    tpError.setErrorCode(i + "");
                    tpError.setErrorMessage(i1 + "");
                    if (mShowListener != null) mShowListener.onAdVideoError(tpError);
                }

                @Override
                public void onVideoAdStartPlay(TTFeedAd ttFeedAd) {
                    Log.i(TAG, "onVideoStart: ");
                    if (mShowListener != null) {
                        mShowListener.onAdVideoStart();
                    }
                }

                @Override
                public void onVideoAdPaused(TTFeedAd ttFeedAd) {

                }

                @Override
                public void onVideoAdContinuePlay(TTFeedAd ttFeedAd) {

                }

                @Override
                public void onProgressUpdate(long l, long l1) {

                }

                @Override
                public void onVideoAdComplete(TTFeedAd ttFeedAd) {
                    Log.i(TAG, "onVideoComplete: ");
                    if (mShowListener != null) {
                        mShowListener.onAdVideoEnd();
                    }
                }
            });
        }
    }
    private MediationViewBinder.Builder builder;

    @Override
    public void registerClickAfterRender(ViewGroup viewGroup, ArrayList<View> clickViews) {
        if (mttFeedAd != null && expressView == null) {
            // 自渲染注册监听
            builder = new MediationViewBinder.Builder(viewGroup.getId());
            View titleView = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_TITLE);
            if (titleView != null) {
                builder.titleId(titleView.getId());
            }

            View subTitleView = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_SUBTITLE);
            if (subTitleView != null) {
                builder.descriptionTextId(subTitleView.getId());
            }

            View callToAction = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_CALLTOACTION);
            if (callToAction != null) {
                builder.callToActionId(callToAction.getId());
            }

            View iconView = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_ICON);
            if (iconView != null) {
                builder.iconImageId(iconView.getId());
            }

            View logoView = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_ADCHOICES_IMAGE);
            if (logoView != null) {
                builder.logoLayoutId(logoView.getId());
            }

            View gromore_mediaview = viewGroup.findViewWithTag("gromore_mediaview");
            if (gromore_mediaview != null) {
                builder.mediaViewIdId(gromore_mediaview.getId());
            }

            List<View> creativeViewList = new ArrayList<>();
            creativeViewList.add(callToAction);

            // 重要! 这个涉及到广告计费，必须正确调用。
            Activity activity = GlobalTradPlus.getInstance().getActivity();
            if (activity == null) {
                if (mShowListener != null) {
                    mShowListener.onAdVideoError(new TPError(ADAPTER_ACTIVITY_ERROR));
                }
                return;
            }

            mttFeedAd.registerViewForInteraction(activity, viewGroup, clickViews, creativeViewList, null, new TTNativeAd.AdInteractionListener() {
                @Override
                public void onAdClicked(View view, TTNativeAd ttNativeAd) {
                    adClicked();
                }

                @Override
                public void onAdCreativeClick(View view, TTNativeAd ttNativeAd) {

                }

                @Override
                public void onAdShow(TTNativeAd ttNativeAd) {
                    adShown();
                }
            }, builder.build());

        }
    }

    @Override
    public TPNativeAdView getTPNativeView() {
        return mTpNativeAdView;
    }

    @Override
    public int getNativeAdType() {
        if (expressView != null) {
            return AD_TYPE_NATIVE_EXPRESS;
        } else {
            return AD_TYPE_NORMAL_NATIVE;

        }
    }

    @Override
    public View getRenderView() {
        return expressView;
    }

    @Override
    public List<View> getMediaViews() {
        return null;
    }

    @Override
    public ViewGroup getCustomAdContainer() {
        return null;
    }

    @Override
    public void clean() {

    }

    public void adClicked() {
        if (mShowListener != null) {
            mShowListener.onAdClicked();
        }
    }

    public void adShown() {
        if (mShowListener != null) {
            mShowListener.onAdShown();
        }
    }

    public void adClosed() {
        if (mShowListener != null) {
            mShowListener.onAdClosed();
        }
    }
}
