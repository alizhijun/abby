package com.hyphenate.helpdesk.easeui.glide;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.hyphenate.helpdesk.R;

/**
 * This is a short description.
 *
 * @author 李志军 2022-08-19 16:58
 */
public class GlideEngineForScanCode implements ImageEngines {
    /**
     * 加载图片
     *
     * @param context   上下文
     * @param url       资源url
     * @param imageView 图片承载控件
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void loadImage(@NonNull Context context, @NonNull String url, @NonNull ImageView imageView) {
        if (!assertValidRequest(context)) {
            return;
        }
        Glide.with(context).load(url).into(imageView);
    }

    /**
     * 加载指定url并返回bitmap
     *
     * @param context 上下文
     * @param url     资源url
     * @param call    回调接口
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void loadImageBitmap(@NonNull Context context, @NonNull String url, int maxWidth, int maxHeight,
                                OnCallbackDataListener<Bitmap> call) {
        if (!assertValidRequest(context)) {
            return;
        }
        Glide.with(context).asBitmap().override(maxWidth, maxHeight).load(url).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                if (call != null) {
                    call.onCall(resource);
                }
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                if (call != null) {
                    call.onCall(null);
                }
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {

            }
        });
    }

    /**
     * 加载相册目录封面
     *
     * @param context   上下文
     * @param url       图片路径
     * @param imageView 承载图片ImageView
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void loadAlbumCover(@NonNull Context context, @NonNull String url, @NonNull ImageView imageView) {
        if (!assertValidRequest(context)) {
            return;
        }
        Glide.with(context)
                .asBitmap()
                .load(url)
                .override(180, 180)
                .centerCrop()
                .sizeMultiplier(0.5f)
                .placeholder(com.luck.picture.lib.R.drawable.ps_image_placeholder)
                .into(new BitmapImageViewTarget(imageView) {
                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.
                                create(context.getResources(), resource);
                        circularBitmapDrawable.setCornerRadius(8);
                        imageView.setImageDrawable(circularBitmapDrawable);
                    }
                });
    }

    /**
     * 加载图片列表图片
     *
     * @param context   上下文
     * @param url       图片路径
     * @param imageView 承载图片ImageView
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void loadGridImage(@NonNull Context context, @NonNull String url, @NonNull ImageView imageView) {
        if (!assertValidRequest(context)) {
            return;
        }
        Glide.with(context)
                .load(url)
                .override(200, 200)
                .centerCrop()
                .placeholder(com.luck.picture.lib.R.drawable.ps_image_placeholder)
                .into(imageView);
    }

    @Override
    public void pauseRequests(Context context) {
        Glide.with(context).pauseRequests();
    }

    @Override
    public void resumeRequests(Context context) {
        Glide.with(context).resumeRequests();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static boolean assertValidRequest(Context context) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            return !isDestroy(activity);
        } else if (context instanceof ContextWrapper) {
            ContextWrapper contextWrapper = (ContextWrapper) context;
            if (contextWrapper.getBaseContext() instanceof Activity) {
                Activity activity = (Activity) contextWrapper.getBaseContext();
                return !isDestroy(activity);
            }
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static boolean isDestroy(Activity activity) {
        if (activity == null) {
            return true;
        }
        return activity.isFinishing() || activity.isDestroyed();
    }

    private GlideEngineForScanCode() {
    }

    private static GlideEngineForScanCode instance;

    public static GlideEngineForScanCode createGlideEngine() {
        if (null == instance) {
            synchronized (GlideEngineForScanCode.class) {
                if (null == instance) {
                    instance = new GlideEngineForScanCode();
                }
            }
        }
        return instance;
    }
}