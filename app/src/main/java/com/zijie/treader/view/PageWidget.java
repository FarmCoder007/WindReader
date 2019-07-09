package com.zijie.treader.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;

import com.zijie.treader.Config;
import com.zijie.treader.util.PageFactory;
import com.zijie.treader.view.animation.AnimationProvider;
import com.zijie.treader.view.animation.CoverAnimation;
import com.zijie.treader.view.animation.NoneAnimation;
import com.zijie.treader.view.animation.SimulationAnimation;
import com.zijie.treader.view.animation.SlideAnimation;

/**
 * 阅读驱动翻页view
 */
public class PageWidget extends View {
    private final static String TAG = "BookPageWidget";
    // 屏幕宽
    private int mScreenWidth = 0;
    // 屏幕高
    private int mScreenHeight = 0;
    private Context mContext;
    /**
     * 是否触发开始移动了
     */
    private Boolean isMove = false;
    //是否翻到下一页
    private Boolean isNext = false;
    //是否取消翻页
    private Boolean cancelPage = false;
    /**
     * 是否没下一页或者上一页
     */
    private Boolean noNext = false;
    private int downX = 0;
    private int downY = 0;

    private int moveX = 0;
    private int moveY = 0;
    //翻页动画是否在执行
    private Boolean isRuning = false;
    // 当前页
    Bitmap mCurPageBitmap = null;
    Bitmap mNextPageBitmap = null;
    /**
     * 控制翻页动画
     */
    private AnimationProvider mAnimationProvider;

    /**
     * Scroller只是个计算器，提供插值计算，让滚动过程具有动画属性
     */
    Scroller mScroller;
    /**
     * 阅读页背景色
     */
    private int mBgColor = 0xFFCEC29C;
    /**
     * 设置触摸事件
     */
    private TouchListener mTouchListener;

    /**
     * 三个构造函数
     *
     * @param context
     */
    public PageWidget(Context context) {
        this(context, null);
    }

    public PageWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        initPage();
        mScroller = new Scroller(getContext(), new LinearInterpolator());
        mAnimationProvider = new SimulationAnimation(mCurPageBitmap, mNextPageBitmap, mScreenWidth, mScreenHeight);
    }

    /**
     * 初始化 PageWidget 中用到的屏幕宽高 及 绘制 字的上下bitmap
     */
    private void initPage() {
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metric = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metric);
        mScreenWidth = metric.widthPixels;
        mScreenHeight = metric.heightPixels;
        //android:LargeHeap=true  use in  manifest application
        mCurPageBitmap = Bitmap.createBitmap(mScreenWidth, mScreenHeight, Bitmap.Config.RGB_565);
        mNextPageBitmap = Bitmap.createBitmap(mScreenWidth, mScreenHeight, Bitmap.Config.RGB_565);
    }

    /**
     * 设置翻页阅读模式
     *
     * @param pageMode
     */
    public void setPageMode(int pageMode) {
        switch (pageMode) {
            case Config.PAGE_MODE_SIMULATION:
                mAnimationProvider = new SimulationAnimation(mCurPageBitmap, mNextPageBitmap, mScreenWidth, mScreenHeight);
                break;
            case Config.PAGE_MODE_COVER:
                mAnimationProvider = new CoverAnimation(mCurPageBitmap, mNextPageBitmap, mScreenWidth, mScreenHeight);
                break;
            case Config.PAGE_MODE_SLIDE:
                mAnimationProvider = new SlideAnimation(mCurPageBitmap, mNextPageBitmap, mScreenWidth, mScreenHeight);
                break;
            case Config.PAGE_MODE_NONE:
                mAnimationProvider = new NoneAnimation(mCurPageBitmap, mNextPageBitmap, mScreenWidth, mScreenHeight);
                break;
            default:
                mAnimationProvider = new SimulationAnimation(mCurPageBitmap, mNextPageBitmap, mScreenWidth, mScreenHeight);
        }
    }

    /**
     * 获取当前页的bitmap
     *
     * @return
     */
    public Bitmap getCurPageBitmap() {
        return mCurPageBitmap;
    }

    /**
     * 获取下一页的bitmap
     *
     * @return
     */
    public Bitmap getNextPageBitmap() {
        return mNextPageBitmap;
    }

    /**
     * 设置书页背景色
     *
     * @param color
     */
    public void setBgColor(int color) {
        mBgColor = color;
    }

    /**
     * 执行view的绘制   走各自翻页模式的绘制方法
     *
     * @param canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        // 绘制背景
        canvas.drawColor(mBgColor);
        Log.e("onDraw", "isNext:" + isNext + "          isRuning:" + isRuning);
        if (isRuning) {
            // 绘制滑动状态时的界面
            mAnimationProvider.drawMove(canvas);
        } else {
            // 绘制滑动完成时的界面
            mAnimationProvider.drawStatic(canvas);
        }
    }

    /**
     * 监听bookView总体的触摸事件   给各自翻页模式分发
     *
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        // 打开书籍中...   过滤触摸事件
        if (PageFactory.getStatus() == PageFactory.Status.OPENING) {
            return true;
        }

        int x = (int) event.getX();
        int y = (int) event.getY();

        mAnimationProvider.setTouchPoint(x, y);
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downX = (int) event.getX();
            downY = (int) event.getY();
            moveX = 0;
            moveY = 0;
            isMove = false;
//            cancelPage = false;
            noNext = false;
            isNext = false;
            isRuning = false;
            // 设置阅读模式【动画  或者 拖拽】起始点
            mAnimationProvider.setStartPoint(downX, downY);
            // 如果正在移动或者翻页 终止动画滚动 直接到最后滚动坐标
            abortAnimation();
            Log.e(TAG, "ACTION_DOWN");
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            // 通过 系统提供的view配置类    获取 判断触发滑动的最小距离
            final int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
            //判断是否可触发移动
            if (!isMove) {
                // 横向或者竖向  满足最小滑动距离 触发移动
                isMove = Math.abs(downX - x) > slop || Math.abs(downY - y) > slop;
            }
            // 可移动中
            if (isMove) {
                isMove = true;
                // 刚开始移动时  判断滑动【翻页】的方向  【每次触摸调用一次】
                if (moveX == 0 && moveY == 0) {
                    Log.e(TAG, "isMove");
                    //判断翻得是上一页还是下一页
                    if (x - downX > 0) {
                        isNext = false;
                    } else {
                        isNext = true;
                    }
                    cancelPage = false;
                    // 向右执行翻页
                    if (isNext) {
                        Boolean isCanTurnNext = mTouchListener.nextPage();
//                        calcCornerXY(downX,mScreenHeight);
                        // 初始化此次触摸翻页 方向
                        mAnimationProvider.setDirection(AnimationProvider.Direction.next);
                        // 尝试翻下页  判断是否有下一页
                        if (!isCanTurnNext) {
                            noNext = true;
                            return true;
                        }
                    } else {
                        // 向左翻页
                        Boolean isCanTurnPre = mTouchListener.prePage();
                        mAnimationProvider.setDirection(AnimationProvider.Direction.pre);
                        if (!isCanTurnPre) {
                            noNext = true;
                            return true;
                        }
                    }
                    Log.e(TAG, "isNext:" + isNext);
                } else {
                    //判断是否  超过翻页范围   取消翻页
                    if (isNext) {
                        // 向右 超过 最大
                        if (x - moveX > 0) {
                            cancelPage = true;
                            mAnimationProvider.setCancel(true);
                        } else {
                            cancelPage = false;
                            mAnimationProvider.setCancel(false);
                        }
                    } else {
                        // 向左 超过 最大
                        if (x - moveX < 0) {
                            mAnimationProvider.setCancel(true);
                            cancelPage = true;
                        } else {
                            mAnimationProvider.setCancel(false);
                            cancelPage = false;
                        }
                    }
                    Log.e(TAG, "cancelPage:" + cancelPage);
                }

                moveX = x;
                moveY = y;
                isRuning = true;
                this.postInvalidate();
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            Log.e(TAG, "ACTION_UP");
            // 没有再移动 走手势放开
            if (!isMove) {
                cancelPage = false;
                // 判断手指拿开时   移动的方向  【中间  向右  向左】
                if (downX > mScreenWidth / 5 && downX < mScreenWidth * 4 / 5 && downY > mScreenHeight / 3 && downY < mScreenHeight * 2 / 3) {
                    if (mTouchListener != null) {
                        mTouchListener.center();
                    }
                    Log.e(TAG, "center");
//                    mCornerX = 1; // 拖拽点对应的页脚
//                    mCornerY = 1;
//                    mTouch.x = 0.1f;
//                    mTouch.y = 0.1f;
                    return true;
                } else if (x < mScreenWidth / 2) {
                    isNext = false;
                } else {
                    isNext = true;
                }

                // 向右 方向 翻页
                if (isNext) {
                    // 尝试向右翻页   里边结合业务判断 是否可翻下一页
                    Boolean isCanTurnNext = mTouchListener.nextPage();
                    mAnimationProvider.setDirection(AnimationProvider.Direction.next);
                    // 没有下一页  【消费掉事件】
                    if (!isCanTurnNext) {
                        return true;
                    }
                } else {
                    // 尝试向左翻页   里边结合业务判断 是否可翻上一页
                    Boolean isCanTurnPre = mTouchListener.prePage();
                    mAnimationProvider.setDirection(AnimationProvider.Direction.pre);
                    // 没有下一页  【消费掉事件】
                    if (!isCanTurnPre) {
                        return true;
                    }
                }
            }

            if (cancelPage && mTouchListener != null) {
                mTouchListener.cancel();
            }

            Log.e(TAG, "isNext:" + isNext);
            // 含有下一页 执行 滚动动画【也就是渐变的翻页】
            if (!noNext) {
                isRuning = true;
                mAnimationProvider.startAnimation(mScroller);
                this.postInvalidate();
            }
        }

        return true;
    }

    /**
     * computeScroll   在view  onDraw 的时候调用这个方法
     *  在这里可以判断 滚动【翻页是否结束】
     */
    @Override
    public void computeScroll() {
        // mScroller.computeScrollOffset()  返回true  表示动画还没有完成
        if (mScroller.computeScrollOffset()) {
            float x = mScroller.getCurrX();
            float y = mScroller.getCurrY();
            mAnimationProvider.setTouchPoint(x, y);
            // 通过不断获取  mScroller.getCurrX()  进行对比  来看滑动是否已经完成
            if (mScroller.getFinalX() == x && mScroller.getFinalY() == y) {
                isRuning = false;
            }
            postInvalidate();
        }
        super.computeScroll();
    }

    /**
     * 终止 动画
     */
    public void abortAnimation() {
        // 滚动 尚未完成
        if (!mScroller.isFinished()) {
            // 中止动画使滚动条移动到最后的X和Y
            mScroller.abortAnimation();
            mAnimationProvider.setTouchPoint(mScroller.getFinalX(), mScroller.getFinalY());
            postInvalidate();
        }
    }

    /**
     * 返回是否 在 滚动 【翻页】
     *
     * @return
     */
    public boolean isRunning() {
        return isRuning;
    }

    /**
     * 设置触摸事件
     *
     * @param mTouchListener
     */
    public void setTouchListener(TouchListener mTouchListener) {
        this.mTouchListener = mTouchListener;
    }

    /**
     * 设置触摸监听
     */
    public interface TouchListener {
        /**
         * 点击中心   显示工具栏
         */
        void center();

        /**
         * 尝试翻前一页    返回是否有前一页
         *
         * @return
         */
        Boolean prePage();

        /**
         * 尝试翻阅下一页   返回是否有后一页
         *
         * @return
         */
        Boolean nextPage();

        /**
         * 取消翻页
         */
        void cancel();
    }

}
