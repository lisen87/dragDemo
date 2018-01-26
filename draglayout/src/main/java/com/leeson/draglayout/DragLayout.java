package com.leeson.draglayout;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;


/**
 * Created by lisen on 2017/12/12.
 * 内部布局可回弹
 * @author lisen < 4533548588@qq.com >
 */

public class DragLayout extends FrameLayout {
    private ViewDragHelper mDragger;
    private int dragViewIndex;
    private View dragView;
    private Point point = new Point();//记录可拖动的view的原来的XY位置
    private boolean isOnlayout = false;

    private View zoomView;//可放大的view
    private int zoomViewH;//可放大的view的原始高度

    private MotionEvent motionEvent;

    public DragLayout(Context context) {
        this(context,null);
    }

    public DragLayout(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public DragLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.DragLayout,defStyleAttr,0);
        dragViewIndex = typedArray.getInt(R.styleable.DragLayout_dragViewIndex,0);
        typedArray.recycle();
        DragCallBack callBack = new DragCallBack();
        mDragger = ViewDragHelper.create(this, 1f, callBack);

    }
    // 手指滑动距离与下拉头的滑动距离比，中间会随正切函数变化
    private float radio = 1f;
    public float touchTopY = 0;
    public float lastY = 0;

    private int height ;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (CommonUtils.isKeyboardShown(DragLayout.this)){
            CommonUtils.closeKeyBoard(getContext());
        }
        mDragger.processTouchEvent(event);

        return touchSlowDown(event);
    }
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        touchSlowDown(event);
        return mDragger.shouldInterceptTouchEvent(event);
    }


    private boolean touchSlowDown(MotionEvent event){
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if (motionEvent == null){
                    motionEvent = event;
                }
                lastY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:

                touchTopY = touchTopY + (event.getY() - lastY) / radio;
                lastY = event.getY();
                // 根据下拉距离改变比例
                radio = (float) (1.5 + 2 * Math.tan(Math.PI / 2 / height
                        * (touchTopY + Math.abs(touchTopY))));
                if (lastY >= getMeasuredHeight() || lastY <= 0
                        || event.getX() > getMeasuredWidth()-30 || event.getX()-30 <= 0){
                    if (mDragger.smoothSlideViewTo(dragView,0,point.y)) {
                        touchTopY = point.y;
                        ViewCompat.postInvalidateOnAnimation(this);
                        postInvalidate();
                    }
                    if (dragZoomListener != null){
                        recover();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                touchTopY = point.y;
                break;
        }
        return true;
    }
    @Override
    public void computeScroll() {
        if (mDragger.continueSettling(true)){
            invalidate();
        }
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (getChildCount() > 0 && !isOnlayout){
            dragView = getChildAt(dragViewIndex);
            point.x = dragView.getLeft();
            point.y = dragView.getTop();
            touchTopY = point.y;
            height = getMeasuredHeight();
            if (dragView instanceof BaseScrollView){

                //设置惯性滑动
                ((BaseScrollView)dragView).setOnScrollListener(new BaseScrollView.OnScrollListener() {
                    @Override
                    public void onScroll(int l, int t, int oldl, int oldt) {
                        if ( CommonUtils.isReachBottom(dragView) || CommonUtils.isReachTop(dragView)){
                            inertiaScroll(oldt - t);
                        }
                    }
                });
            }else if (dragView instanceof RecyclerView){
                ((RecyclerView) dragView).addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        if (CommonUtils.isReachBottom(dragView) || CommonUtils.isReachTop(dragView)){
                            inertiaScroll(-dy);
                        }
                    }
                });
            }
            if (dragZoomListener != null){
                zoomView = dragZoomListener.zoomView();
                if (zoomView != null){
                    zoomViewH = zoomView.getMeasuredHeight();
                }
            }
            isOnlayout = true;
        }
    }

    /**
     * 惯性滚动
     * @param scrollY 滚动的距离
     */
    private void inertiaScroll(int scrollY){
        if (mDragger.smoothSlideViewTo(dragView,point.x,scrollY)) {
            ViewCompat.postInvalidateOnAnimation(DragLayout.this);
            invalidate();
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDragger.smoothSlideViewTo(dragView,point.x,point.y);
                    ViewCompat.postInvalidateOnAnimation(DragLayout.this);
                    invalidate();
                }
            },100);
        }
    }

    private class DragCallBack extends ViewDragHelper.Callback {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == dragView;
        }
        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            if (listener != null){
                if (listener.isCanPullDown() && dy > 0 || listener.isCanPullUp() && dy < 0 ){
                    if (dragZoomListener != null){
                        startZoom(top);//startZoom(dy);
                    }
                    return (int) touchTopY;
                }else if(listener.isCanPullDown() && top > point.y || listener.isCanPullUp() && top < point.y){
                    //下拉后未松手又上滑或下拉后未松手又上拉（在没有设置zoomview时）
                    return (int) touchTopY;
                } else{
                    return point.y;
                }
            }

            if (dragZoomListener != null){
                startZoom(top);
                return top;
            }
            return (int) touchTopY;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return point.x;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            if (dragView == releasedChild){
                if (zoomView != null){
                    recover();
                }
                mDragger.settleCapturedViewAt(point.x, point.y);
                invalidate();
            }
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return 1;
        }
    }

    /**
     * 恢复原状
     */
    private void recover(){
        if (zoomView != null){

            ValueAnimator valueAnimator = ValueAnimator.ofInt(zoomView.getMeasuredHeight(),zoomViewH);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    zoomView.getLayoutParams().height = (Integer) animation.getAnimatedValue();
                    zoomView.requestLayout();
                }
            });
            valueAnimator.setDuration(300);
            valueAnimator.setInterpolator(new DecelerateInterpolator());
            valueAnimator.start();
        }
    }

    /**
     *开始放大指定view的高度
     * @param dy 变化的y轴值
     */
    private ViewGroup.LayoutParams params;
    private void startZoom(int dy){
        if (zoomView == null){
            return;
        }
        if (dy > 0){
            if (params == null){
                params = zoomView.getLayoutParams();
            }
            params.height = zoomView.getMeasuredHeight() + dy;
            zoomView.setLayoutParams(params);
//            zoomView.requestLayout()
        }
    }

    //判断当appBarLayout是否展开时多次调用
    private boolean isOne = true;

    /**
     * 当appBarLayout 展开时为了取得appBarLayout的触摸事件
     */
    public void dispatchEvent(){

        if (motionEvent != null && !isOne){
            motionEvent.setAction(MotionEvent.ACTION_CANCEL);
            dispatchTouchEvent(motionEvent);
            isOne = true;
        }
    }

    /**
     * 当appBarLayout 关闭时恢复限制
     */
    public void resetDispatchEvent(){
        isOne = false;
    }

    public interface DragListener {
        /**
         *
         * @return true:能下拉 false :不能
         */
        boolean isCanPullDown();

        /**
         *
         * @return true :能上拉 false :不能
         */
        boolean isCanPullUp();
    }
    private DragListener listener;

    public void setListener(DragListener listener) {
        this.listener = listener;
    }

    public interface DragZoomListener{
        View zoomView();
    }

    private DragZoomListener dragZoomListener;

    public void setDragZoomListener(DragZoomListener dragZoomListener) {
        this.dragZoomListener = dragZoomListener;
    }
}
