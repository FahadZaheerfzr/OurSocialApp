

package com.nust.socialapp.main.imageDetail;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.nust.socialapp.main.base.BasePresenter;



class ImageDetailPresenter extends BasePresenter<ImageDetailView> {

    ImageDetailPresenter(Context context) {
        super(context);
    }

    public int calcMaxImageSide() {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displaymetrics);

        int width = displaymetrics.widthPixels;
        int height = displaymetrics.heightPixels;

        return width > height ? width : height;
    }
}
