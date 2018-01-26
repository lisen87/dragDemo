package com.leeson.dragdemo;

import android.os.Bundle;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.leeson.draglayout.CommonUtils;
import com.leeson.draglayout.DragLayout;

public class MainActivity extends AppCompatActivity implements DragLayout.DragListener,DragLayout.DragZoomListener {

    private DragLayout dragLayout;
    private View topView;
    private NestedScrollView nestScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dragLayout = (DragLayout) findViewById(R.id.dragLayout);
        nestScrollView = (NestedScrollView) findViewById(R.id.nestScrollView);
        topView  = findViewById(R.id.topView);

        dragLayout.setListener(this);
        dragLayout.setDragZoomListener(this);
    }

    @Override
    public boolean isCanPullDown() {
        return CommonUtils.isReachTop(nestScrollView);
    }

    @Override
    public boolean isCanPullUp() {
        return CommonUtils.isReachBottom(nestScrollView);
    }

    @Override
    public View zoomView() {
        return topView;
    }
}
