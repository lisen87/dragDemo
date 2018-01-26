package com.leeson.draglayout;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ScrollView;

/**
 * Created by lisen on 2018/1/26.
 *
 * @author lisen < 4533548588@qq.com >
 */

public class CommonUtils {
    /**
     * 判断view是否滚动到达顶部
     * @param view
     * @return true 到达顶部
     */
    public static boolean isReachTop(View view){
        if (view instanceof ScrollView || view instanceof NestedScrollView){
            return view.getScrollY() ==0 ; //scrollView判断到达顶部
        }else if (view instanceof RecyclerView){
            return  !view.canScrollVertically(-1);//表示是否能向下滚动，false表示已经滚动到顶部
        }
        return true;
    }

    /**
     * 判断view是否滚动到达底部
     * @param view
     * @return true 到达底部
     */
    public static boolean isReachBottom(View view){
        if (view instanceof ScrollView || view instanceof NestedScrollView){
            return view.getMeasuredHeight()+view.getScrollY() >= ((ViewGroup)view).getChildAt(0).getMeasuredHeight();
        }else if (view instanceof RecyclerView ){
            return  !view.canScrollVertically(1);//表示是否能向上滚动，false表示已经滚动到底部
        }
        return true;
    }
    /**
     * 关闭输入法
     * @param context
     */
    public static void closeKeyBoard(Context context){
        InputMethodManager inputManager =
                (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow(((Activity)context).getCurrentFocus().getWindowToken(), 0);
    }

    /**
     * 键盘是否显示
     * @param view
     * @return
     */
    public static boolean isKeyboardShown(View view) {
        View rootView = view.getRootView();
        final int softKeyboardHeight = 50;
        Rect r = new Rect();
        rootView.getWindowVisibleDisplayFrame(r);
        DisplayMetrics dm = rootView.getResources().getDisplayMetrics();
        int heightDiff = rootView.getBottom() - r.bottom;
        return heightDiff > softKeyboardHeight * dm.density;
    }

}
