package com.zijie.treader.view.animation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Region;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.widget.Scroller;

/**
 * Created by Administrator on 2016/8/26 0026.
 * 仿真阅读模式
 */
public class SimulationAnimation extends AnimationProvider {
    /**
     * 拖拽点对应的页右下脚坐标  可能是屏幕宽
     */
    private int mCornerX = 1;
    private int mCornerY = 1;
    private Path mPath0;
    private Path mPath1;

    /**
     * 贝塞尔曲线起始点
     * PointF 里成员  x y为float类型  Point则是int类型
     */
    PointF mBezierStart1 = new PointF();
    /**
     * 贝塞尔曲线控制点
     */
    PointF mBezierControl1 = new PointF();
    /**
     * 贝塞尔曲线顶点
     */
    PointF mBeziervertex1 = new PointF();
    /**
     * 贝塞尔曲线结束点
     */
    PointF mBezierEnd1 = new PointF();

    // 另一条贝塞尔曲线
    PointF mBezierStart2 = new PointF();
    PointF mBezierControl2 = new PointF();
    PointF mBeziervertex2 = new PointF();
    PointF mBezierEnd2 = new PointF();

    /**
     * 触摸点  和  右下页脚点的中点x坐标
     */
    float mMiddleX;
    /**
     * 触摸点  和  右下页脚点的中点y坐标
     */
    float mMiddleY;
    float mDegrees;
    float mTouchToCornerDis;
    ColorMatrixColorFilter mColorMatrixFilter;
    Matrix mMatrix;
    float[] mMatrixArray = {0, 0, 0, 0, 0, 0, 0, 0, 1.0f};

    /**
     * 是否属于右上左下
     */
    boolean mIsRTandLB;
    private float mMaxLength;
    /**
     * 背面颜色组
     */
    int[] mBackShadowColors;
    /**
     * 前面颜色组
     */
    int[] mFrontShadowColors;

    // 有阴影的GradientDrawable
    GradientDrawable mBackShadowDrawableLR;
    GradientDrawable mBackShadowDrawableRL;
    GradientDrawable mFolderShadowDrawableLR;
    GradientDrawable mFolderShadowDrawableRL;

    GradientDrawable mFrontShadowDrawableHBT;
    GradientDrawable mFrontShadowDrawableHTB;
    GradientDrawable mFrontShadowDrawableVLR;
    GradientDrawable mFrontShadowDrawableVRL;

    Paint mPaint;

    /**
     * 仿真模式构造
     *
     * @param mCurrentBitmap 当前bitmap
     * @param mNextBitmap
     * @param width
     * @param height
     */
    public SimulationAnimation(Bitmap mCurrentBitmap, Bitmap mNextBitmap, int width, int height) {
        super(mCurrentBitmap, mNextBitmap, width, height);

        mPath0 = new Path();
        mPath1 = new Path();
        mMaxLength = (float) Math.hypot(mScreenWidth, mScreenHeight);
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);

        createDrawable();

        /**
         * 色彩矩阵 详见 https://blog.csdn.net/xiongkai520520/article/details/52472638
         * //设置颜色数组  色彩矩阵变换
         * // 对角线上的取值范围是0-1的   0会去掉某个颜色
         */
        ColorMatrix cm = new ColorMatrix();
        float array[] = {
                // R 0 0 0 0  操作红色
                0.55f, 0, 0, 0, 80.0f,
                //  0 G 0 0 0  操作绿色
                0, 0.55f, 0, 0, 80.0f,
                // 0 0 B 0 0  操作蓝色
                0, 0, 0.55f, 0, 80.0f,
                // 0 0 0 Alpha 0 操作透明度   更改此处 变化翻页背景页透明度
                0, 0, 0, 0.2f, 0};
//        float array[] = {1, 0, 0, 0, 0,
//                0, 1, 0, 0, 0,
//                0, 0, 1, 0, 0,
//                0, 0, 0, 1, 0};
        cm.set(array);
        mColorMatrixFilter = new ColorMatrixColorFilter(cm);
        mMatrix = new Matrix();

        mTouch.x = 0.01f; // 不让x,y为0,否则在点计算时会有问题
        mTouch.y = 0.01f;
    }

    @Override
    public void drawMove(Canvas canvas) {
        if (getDirection().equals(Direction.next)) {
            calcPoints();
            drawCurrentPageArea(canvas, mCurPageBitmap, mPath0);
            drawNextPageAreaAndShadow(canvas, mNextPageBitmap);
//            drawCurrentPageShadow(canvas);
//            drawCurrentBackArea(canvas, mCurPageBitmap);
        } else {
            calcPoints();
            drawCurrentPageArea(canvas, mNextPageBitmap, mPath0);
            drawNextPageAreaAndShadow(canvas, mCurPageBitmap);
            drawCurrentPageShadow(canvas);
            drawCurrentBackArea(canvas, mNextPageBitmap);
        }
    }

    @Override
    public void drawStatic(Canvas canvas) {
        if (getCancel()) {
            canvas.drawBitmap(mCurPageBitmap, 0, 0, null);
        } else {
            canvas.drawBitmap(mNextPageBitmap, 0, 0, null);
        }
    }

    @Override
    public void setCancel(boolean isCancel) {
        super.setCancel(isCancel);
    }

    @Override
    public void startAnimation(Scroller scroller) {
        int dx, dy;
        // dx 水平方向滑动的距离，负值会使滚动向左滚动
        // dy 垂直方向滑动的距离，负值会使滚动向上滚动
        if (getCancel()) {
            if (mCornerX > 0 && getDirection().equals(Direction.next)) {
                dx = (int) (mScreenWidth - mTouch.x);
            } else {
                dx = -(int) mTouch.x;
            }

            if (!getDirection().equals(Direction.next)) {
                dx = (int) -(mScreenWidth + mTouch.x);
            }

            if (mCornerY > 0) {
                dy = (int) (mScreenHeight - mTouch.y);
            } else {
                dy = -(int) mTouch.y; // 防止mTouch.y最终变为0
            }
        } else {
            if (mCornerX > 0 && getDirection().equals(Direction.next)) {
                dx = -(int) (mScreenWidth + mTouch.x);
            } else {
                dx = (int) (mScreenWidth - mTouch.x + mScreenWidth);
            }
            if (mCornerY > 0) {
                dy = (int) (mScreenHeight - mTouch.y);
            } else {
                dy = (int) (1 - mTouch.y); // 防止mTouch.y最终变为0
            }
        }
        scroller.startScroll((int) mTouch.x, (int) mTouch.y, dx, dy, 400);
    }


    @Override
    public void setDirection(Direction direction) {
        super.setDirection(direction);
        switch (direction) {
            case pre:
                //上一页滑动不出现对角
                if (myStartX > mScreenWidth / 2) {
                    calcCornerXY(myStartX, mScreenHeight);
                } else {
                    calcCornerXY(mScreenWidth - myStartX, mScreenHeight);
                }
                break;
            case next:
                if (mScreenWidth / 2 > myStartX) {
                    calcCornerXY(mScreenWidth - myStartX, myStartY);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void setStartPoint(float x, float y) {
        super.setStartPoint(x, y);
        calcCornerXY(x, y);
    }

    /**
     * 设置拖拽点point
     *
     * @param x
     * @param y
     */
    @Override
    public void setTouchPoint(float x, float y) {
        super.setTouchPoint(x, y);
        //触摸y中间位置吧y变成屏幕高度
        if ((myStartY > mScreenHeight / 3 && myStartY < mScreenHeight * 2 / 3) || getDirection().equals(Direction.pre)) {
            mTouch.y = mScreenHeight;
        }

        if (myStartY > mScreenHeight / 3 && myStartY < mScreenHeight / 2 && getDirection().equals(Direction.next)) {
            mTouch.y = 1;
        }
    }

    /**
     * 创建阴影的GradientDrawable
     */
    private void createDrawable() {
        int[] color = {0x333333, 0xb0333333};
        // 参数一  渐变色方向 从右向左  参数二 渐变色数组
        mFolderShadowDrawableRL = new GradientDrawable(
                GradientDrawable.Orientation.RIGHT_LEFT, color);
        mFolderShadowDrawableRL
                .setGradientType(GradientDrawable.LINEAR_GRADIENT);

        mFolderShadowDrawableLR = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, color);
        mFolderShadowDrawableLR
                .setGradientType(GradientDrawable.LINEAR_GRADIENT);

        mBackShadowColors = new int[]{0xff111111, 0x111111};
        mBackShadowDrawableRL = new GradientDrawable(
                GradientDrawable.Orientation.RIGHT_LEFT, mBackShadowColors);
        mBackShadowDrawableRL.setGradientType(GradientDrawable.LINEAR_GRADIENT);

        mBackShadowDrawableLR = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, mBackShadowColors);
        mBackShadowDrawableLR.setGradientType(GradientDrawable.LINEAR_GRADIENT);

        mFrontShadowColors = new int[]{0x80111111, 0x111111};
        mFrontShadowDrawableVLR = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, mFrontShadowColors);
        mFrontShadowDrawableVLR
                .setGradientType(GradientDrawable.LINEAR_GRADIENT);
        mFrontShadowDrawableVRL = new GradientDrawable(
                GradientDrawable.Orientation.RIGHT_LEFT, mFrontShadowColors);
        mFrontShadowDrawableVRL
                .setGradientType(GradientDrawable.LINEAR_GRADIENT);

        mFrontShadowDrawableHTB = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, mFrontShadowColors);
        mFrontShadowDrawableHTB
                .setGradientType(GradientDrawable.LINEAR_GRADIENT);

        mFrontShadowDrawableHBT = new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP, mFrontShadowColors);
        mFrontShadowDrawableHBT
                .setGradientType(GradientDrawable.LINEAR_GRADIENT);
    }

    /**
     * 是否能够拖动过去
     *
     * @return
     */
    public boolean canDragOver() {
        if (mTouchToCornerDis > mScreenWidth / 10)
            return true;
        return false;
    }

    public boolean right() {
        if (mCornerX > -4)
            return false;
        return true;
    }

    /**
     * 绘制翻起页背面
     *
     * @param canvas
     * @param bitmap
     */
    private void drawCurrentBackArea(Canvas canvas, Bitmap bitmap) {
        int i = (int) (mBezierStart1.x + mBezierControl1.x) / 2;
        float f1 = Math.abs(i - mBezierControl1.x);
        int i1 = (int) (mBezierStart2.y + mBezierControl2.y) / 2;
        float f2 = Math.abs(i1 - mBezierControl2.y);
        float f3 = Math.min(f1, f2);
        mPath1.reset();
        mPath1.moveTo(mBeziervertex2.x, mBeziervertex2.y);
        mPath1.lineTo(mBeziervertex1.x, mBeziervertex1.y);
        mPath1.lineTo(mBezierEnd1.x, mBezierEnd1.y);
        mPath1.lineTo(mTouch.x, mTouch.y);
        mPath1.lineTo(mBezierEnd2.x, mBezierEnd2.y);
        mPath1.close();
        GradientDrawable mFolderShadowDrawable;
        int left;
        int right;
        if (mIsRTandLB) {
            left = (int) (mBezierStart1.x - 1);
            right = (int) (mBezierStart1.x + f3 + 1);
            mFolderShadowDrawable = mFolderShadowDrawableLR;
        } else {
            left = (int) (mBezierStart1.x - f3 - 1);
            right = (int) (mBezierStart1.x + 1);
            mFolderShadowDrawable = mFolderShadowDrawableRL;
        }
        canvas.save();
        try {
            canvas.clipPath(mPath0);
            canvas.clipPath(mPath1, Region.Op.INTERSECT);
        } catch (Exception e) {
        }


        mPaint.setColorFilter(mColorMatrixFilter);

        float dis = (float) Math.hypot(mCornerX - mBezierControl1.x,
                mBezierControl2.y - mCornerY);
        float f8 = (mCornerX - mBezierControl1.x) / dis;
        float f9 = (mBezierControl2.y - mCornerY) / dis;
        mMatrixArray[0] = 1 - 2 * f9 * f9;
        mMatrixArray[1] = 2 * f8 * f9;
        mMatrixArray[3] = mMatrixArray[1];
        mMatrixArray[4] = 1 - 2 * f8 * f8;
        mMatrix.reset();
        mMatrix.setValues(mMatrixArray);
        mMatrix.preTranslate(-mBezierControl1.x, -mBezierControl1.y);
        mMatrix.postTranslate(mBezierControl1.x, mBezierControl1.y);
        canvas.drawBitmap(bitmap, mMatrix, mPaint);
        // canvas.drawBitmap(bitmap, mMatrix, null);
        mPaint.setColorFilter(null);

        canvas.rotate(mDegrees, mBezierStart1.x, mBezierStart1.y);
        mFolderShadowDrawable.setBounds(left, (int) mBezierStart1.y, right,
                (int) (mBezierStart1.y + mMaxLength));
        mFolderShadowDrawable.draw(canvas);
        canvas.restore();
    }

    /**
     * 绘制翻起页的阴影
     *
     * @param canvas
     */
    public void drawCurrentPageShadow(Canvas canvas) {
        double degree;
        if (mIsRTandLB) {
            degree = Math.PI
                    / 4
                    - Math.atan2(mBezierControl1.y - mTouch.y, mTouch.x
                    - mBezierControl1.x);
        } else {
            degree = Math.PI
                    / 4
                    - Math.atan2(mTouch.y - mBezierControl1.y, mTouch.x
                    - mBezierControl1.x);
        }
        // 翻起页阴影顶点与touch点的距离
        double d1 = (float) 25 * 1.414 * Math.cos(degree);
        double d2 = (float) 25 * 1.414 * Math.sin(degree);
        float x = (float) (mTouch.x + d1);
        float y;
        if (mIsRTandLB) {
            y = (float) (mTouch.y + d2);
        } else {
            y = (float) (mTouch.y - d2);
        }
        mPath1.reset();
        mPath1.moveTo(x, y);
        mPath1.lineTo(mTouch.x, mTouch.y);
        mPath1.lineTo(mBezierControl1.x, mBezierControl1.y);
        mPath1.lineTo(mBezierStart1.x, mBezierStart1.y);
        mPath1.close();
        float rotateDegrees;
        canvas.save();
        try {
            canvas.clipPath(mPath0, Region.Op.XOR);
            canvas.clipPath(mPath1, Region.Op.INTERSECT);
        } catch (Exception e) {
            // TODO: handle exception
        }

        int leftx;
        int rightx;
        GradientDrawable mCurrentPageShadow;
        if (mIsRTandLB) {
            leftx = (int) (mBezierControl1.x);
            rightx = (int) mBezierControl1.x + 25;
            mCurrentPageShadow = mFrontShadowDrawableVLR;
        } else {
            leftx = (int) (mBezierControl1.x - 25);
            rightx = (int) mBezierControl1.x + 1;
            mCurrentPageShadow = mFrontShadowDrawableVRL;
        }

        rotateDegrees = (float) Math.toDegrees(Math.atan2(mTouch.x
                - mBezierControl1.x, mBezierControl1.y - mTouch.y));
        canvas.rotate(rotateDegrees, mBezierControl1.x, mBezierControl1.y);
        mCurrentPageShadow.setBounds(leftx,
                (int) (mBezierControl1.y - mMaxLength), rightx,
                (int) (mBezierControl1.y));
        mCurrentPageShadow.draw(canvas);
        canvas.restore();

        mPath1.reset();
        mPath1.moveTo(x, y);
        mPath1.lineTo(mTouch.x, mTouch.y);
        mPath1.lineTo(mBezierControl2.x, mBezierControl2.y);
        mPath1.lineTo(mBezierStart2.x, mBezierStart2.y);
        mPath1.close();
        canvas.save();
        try {
            canvas.clipPath(mPath0, Region.Op.XOR);
            canvas.clipPath(mPath1, Region.Op.INTERSECT);
        } catch (Exception e) {
        }

        if (mIsRTandLB) {
            leftx = (int) (mBezierControl2.y);
            rightx = (int) (mBezierControl2.y + 25);
            mCurrentPageShadow = mFrontShadowDrawableHTB;
        } else {
            leftx = (int) (mBezierControl2.y - 25);
            rightx = (int) (mBezierControl2.y + 1);
            mCurrentPageShadow = mFrontShadowDrawableHBT;
        }
        rotateDegrees = (float) Math.toDegrees(Math.atan2(mBezierControl2.y
                - mTouch.y, mBezierControl2.x - mTouch.x));
        canvas.rotate(rotateDegrees, mBezierControl2.x, mBezierControl2.y);
        float temp;
        if (mBezierControl2.y < 0)
            temp = mBezierControl2.y - mScreenHeight;
        else
            temp = mBezierControl2.y;

        int hmg = (int) Math.hypot(mBezierControl2.x, temp);
        if (hmg > mMaxLength)
            mCurrentPageShadow
                    .setBounds((int) (mBezierControl2.x - 25) - hmg, leftx,
                            (int) (mBezierControl2.x + mMaxLength) - hmg,
                            rightx);
        else
            mCurrentPageShadow.setBounds(
                    (int) (mBezierControl2.x - mMaxLength), leftx,
                    (int) (mBezierControl2.x), rightx);

        // Log.i("hmg", "mBezierControl2.x   " + mBezierControl2.x
        // + "  mBezierControl2.y  " + mBezierControl2.y);
        mCurrentPageShadow.draw(canvas);
        canvas.restore();
    }

    private void drawNextPageAreaAndShadow(Canvas canvas, Bitmap bitmap) {
        mPath1.reset();
        mPath1.moveTo(mBezierStart1.x, mBezierStart1.y);
        mPath1.lineTo(mBeziervertex1.x, mBeziervertex1.y);
        mPath1.lineTo(mBeziervertex2.x, mBeziervertex2.y);
        mPath1.lineTo(mBezierStart2.x, mBezierStart2.y);
        mPath1.lineTo(mCornerX, mCornerY);
        mPath1.close();

        mDegrees = (float) Math.toDegrees(Math.atan2(mBezierControl1.x
                - mCornerX, mBezierControl2.y - mCornerY));
        int leftx;
        int rightx;
        GradientDrawable mBackShadowDrawable;
        if (mIsRTandLB) {  //左下及右上
            leftx = (int) (mBezierStart1.x);
            rightx = (int) (mBezierStart1.x + mTouchToCornerDis / 4);
            mBackShadowDrawable = mBackShadowDrawableLR;
        } else {
            leftx = (int) (mBezierStart1.x - mTouchToCornerDis / 4);
            rightx = (int) mBezierStart1.x;
            mBackShadowDrawable = mBackShadowDrawableRL;
        }
        canvas.save();
        try {
            canvas.clipPath(mPath0);
            canvas.clipPath(mPath1, Region.Op.INTERSECT);
        } catch (Exception e) {
        }
        canvas.drawBitmap(bitmap, 0, 0, null);
        canvas.rotate(mDegrees, mBezierStart1.x, mBezierStart1.y);
        //左上及右下角的xy坐标值,构成一个矩形
        mBackShadowDrawable.setBounds(leftx, (int) mBezierStart1.y, rightx, (int) (mMaxLength + mBezierStart1.y));
        mBackShadowDrawable.draw(canvas);
        canvas.restore();
    }

    /**
     * 绘制上方页     整个界面 -
     *
     * @param canvas
     * @param bitmap
     * @param path
     */
    private void drawCurrentPageArea(Canvas canvas, Bitmap bitmap, Path path) {
        // mPath0 从屏幕右边起第一条二阶贝塞尔曲线
        mPath0.reset();
        // 移到第一条二阶贝塞尔起点
        mPath0.moveTo(mBezierStart1.x, mBezierStart1.y);
        // 第一段弧线  传入控制点和终点
        mPath0.quadTo(mBezierControl1.x, mBezierControl1.y, mBezierEnd1.x,
                mBezierEnd1.y);
        // 第一条弧线到上方页脚 直线
        mPath0.lineTo(mTouch.x, mTouch.y);
        // 上方页脚 到第二条贝塞尔曲线终点【起点】
        mPath0.lineTo(mBezierEnd2.x, mBezierEnd2.y);
        // 第二个贝塞尔曲线
        mPath0.quadTo(mBezierControl2.x, mBezierControl2.y, mBezierStart2.x,
                mBezierStart2.y);
        // 链接到右下角
        mPath0.lineTo(mCornerX, mCornerY);
        // 右下角链接到起点 形成闭环  【闭环部分用来绘制上方页阴影和下方页右下角】
        mPath0.close();

        // 保存绘制前的状态
        canvas.save();
        // 先裁剪画布再绘制   【XOR就是全集的减去交集  剩余的部分  即可视的上一页文字】
        canvas.clipPath(path, Region.Op.XOR);
        // 裁剪完的画布 可绘制区域就是上方可视页
        canvas.drawBitmap(bitmap, 0, 0, null);
        try {
            // restore：用来恢复Canvas之前保存的状态。防止save后对Canvas执行的操作对后续的绘制有影响。
            canvas.restore();
        } catch (Exception e) {

        }

    }

    /**
     * 计算拖拽点对应的拖拽脚
     *
     * @param x
     * @param y
     */
    public void calcCornerXY(float x, float y) {
        //  Log.i("hck", "PageWidget x:" + x + "      y" + y);
        if (x <= mScreenWidth / 2) {
            mCornerX = 0;
        } else {
            mCornerX = mScreenWidth;
        }
        if (y <= mScreenHeight / 2) {
            mCornerY = 0;
        } else {
            mCornerY = mScreenHeight;
        }

        if ((mCornerX == 0 && mCornerY == mScreenHeight) || (mCornerX == mScreenWidth && mCornerY == 0)) {
            mIsRTandLB = true;
        } else {
            mIsRTandLB = false;
        }

    }

    /**
     * 计算各个点的坐标
     */
    private void calcPoints() {
        // 算出触摸点  和  右下页脚点中点坐标
        mMiddleX = (mTouch.x + mCornerX) / 2;
        mMiddleY = (mTouch.y + mCornerY) / 2;
        // 算出第一条贝塞尔曲线控制点 坐标
        /**
         *  中点mMiddle到屏幕底部的距离= mCornerY【屏幕高度】- mMiddleY【中点y坐标】
         *  由相似三角形对应边成比例 算出 控制点1 到 【中点到屏幕下方垂直点的距离 em】 即   em=gm*gm/mf;
         *  那么控制点e的横坐标为中点横坐标 - em的距离  即如下
         */
        mBezierControl1.x = mMiddleX - (mCornerY - mMiddleY) * (mCornerY - mMiddleY) / (mCornerX - mMiddleX);
        // 屏幕高 【这条控制点一直在屏幕最下边】
        mBezierControl1.y = mCornerY;


        // 第二条贝塞尔曲线控制点坐标 【控制点一直在屏幕最右边】
        mBezierControl2.x = mCornerX;
        //   mBezierControl2.y = mMiddleY - (mCornerX - mMiddleX)* (mCornerX - mMiddleX) / (mCornerY - mMiddleY);
        //  同理算出控制点2的坐标  【为了解决起始控制点 快与 右下角重合  分母不为0】
        float f4 = mCornerY - mMiddleY;
        if (f4 == 0) {
            mBezierControl2.y = mMiddleY - (mCornerX - mMiddleX) * (mCornerX - mMiddleX) / 0.1f;
            Log.d("PageWidget", "" + f4);
        } else {
            mBezierControl2.y = mMiddleY - (mCornerX - mMiddleX) * (mCornerX - mMiddleX) / (mCornerY - mMiddleY);
            Log.d("PageWidget", "没有进入if判断" + mBezierControl2.y + "");
        }

        // Log.i("hmg", "mTouchX  " + mTouch.x + "  mTouchY  " + mTouch.y);
        // Log.i("hmg", "mBezierControl1.x  " + mBezierControl1.x + "  mBezierControl1.y  " + mBezierControl1.y);
        // Log.i("hmg", "mBezierControl2.x  " + mBezierControl2.x + "  mBezierControl2.y  " + mBezierControl2.y);

        /**
         *  算出第一条贝塞尔起始点 【也是一直在屏幕底部】
         *  算法：设n为ag中点，同理，根据相似三角形fgh和fnj，且比例为2:3
         *  即fe/fc = 2/3 【fe 为 mCornerX - mBezierControl1.x    fc 为mCornerX - c横坐标  】
         *  整理后c横坐标如下
         */
        mBezierStart1.x = mBezierControl1.x - (mCornerX - mBezierControl1.x) / 2;
        mBezierStart1.y = mCornerY;

        /**
         * 当mBezierStart1.x < 0或者mBezierStart1.x > 屏幕宽时
         * 如果继续翻页，会出现BUG故在此限制
         * 限制贝塞尔起始点 不能超过屏幕左边距  【即不能为负数】  这时改变触摸点位置 重新计算mTouch
         * 由相似梯形  重新算出 mTouch 点的位置   从而重新算各个点的位置
         */
        if (mTouch.x > 0 && mTouch.x < mScreenWidth) {
            if (mBezierStart1.x < 0 || mBezierStart1.x > mScreenWidth) {
                // 贝塞尔1的起始点超出屏幕了 算出超出屏幕时到屏幕右下角的距离【图四中 cf的距离 如下】
                if (mBezierStart1.x < 0) {
                    mBezierStart1.x = mScreenWidth - mBezierStart1.x;
                }
                // 下面根据相似图形算出新的触摸点坐标 【pf的距离如下】
                float pf = Math.abs(mCornerX - mTouch.x);
                // 相似梯形 对应边成比例 【如图四p1f/pf = width/cf】
                float p1f = mScreenWidth * pf / mBezierStart1.x;
                // 算出 起始点放在临界值位置时  新的触摸点横坐标
                mTouch.x = Math.abs(mCornerX - p1f);


                /**
                 *  oldTouchHeight 为旧的触摸点到底部屏幕高度
                 *  oldTouchHeight/newTouchHeight = p1f/pf  而p1f = 屏幕宽度- 新触摸点的横坐标 【Math.abs(mCornerX - mTouch.x)】
                 *
                 * 算出新触摸点纵坐标 【newTouchHeight为 新触摸点到底部的距离】
                 */
                float oldTouchHeight = Math.abs(mCornerY - mTouch.y);
                float newTouchHeight = Math.abs(mCornerX - mTouch.x) * oldTouchHeight / pf;
                mTouch.y = Math.abs(mCornerY - newTouchHeight);


                // mCornerX mCornerY 是不变的   由新的触摸点和页脚点    重新计算出 绘制用的所有点
                mMiddleX = (mTouch.x + mCornerX) / 2;
                mMiddleY = (mTouch.y + mCornerY) / 2;

                // 重新计算1 的控制点
                mBezierControl1.x = mMiddleX - (mCornerY - mMiddleY) * (mCornerY - mMiddleY) / (mCornerX - mMiddleX);
                mBezierControl1.y = mCornerY;

                // 重新计算2的控制点
                mBezierControl2.x = mCornerX;
                //    mBezierControl2.y = mMiddleY - (mCornerX - mMiddleX)
                //  * (mCornerX - mMiddleX) / (mCornerY - mMiddleY);

                float f5 = mCornerY - mMiddleY;
                if (f5 == 0) {
                    mBezierControl2.y = mMiddleY - (mCornerX - mMiddleX) * (mCornerX - mMiddleX) / 0.1f;
                } else {
                    mBezierControl2.y = mMiddleY - (mCornerX - mMiddleX) * (mCornerX - mMiddleX) / (mCornerY - mMiddleY);
                    //    Log.d("PageWidget", mBezierControl2.y + "");
                }


                // Log.i("hmg", "mTouchX --> " + mTouch.x + "  mTouchY-->  "
                // + mTouch.y);
                // Log.i("hmg", "mBezierControl1.x--  " + mBezierControl1.x
                // + "  mBezierControl1.y -- " + mBezierControl1.y);
                // Log.i("hmg", "mBezierControl2.x -- " + mBezierControl2.x
                // + "  mBezierControl2.y -- " + mBezierControl2.y);
                // 重新计算1的起始点横坐标  纵坐标一直是屏高
                mBezierStart1.x = mBezierControl1.x - (mCornerX - mBezierControl1.x) / 2;
            }
        }
        mBezierStart2.x = mCornerX;
        /**
         *  如图3  根据相似三角形 fgh 和fnj   得出 fg/fn = 2/3 = fh/fj
         *  fh = mCornerY - mBezierControl2.y 【屏幕高度 - 贝塞尔2的控制点纵坐标】
         *  fj = mCornerY -  mBezierStart2.y  = 3(mCornerY - mBezierControl2.y)/2 整理得如下
         */
        mBezierStart2.y = mBezierControl2.y - (mCornerY - mBezierControl2.y) / 2;


        /**
         * 贝塞尔1  2 的终点 【即求交点坐标】
         */
        mBezierEnd1 = getCrossPoint(mTouch, mBezierControl1, mBezierStart1, mBezierStart2);
        mBezierEnd2 = getCrossPoint(mTouch, mBezierControl2, mBezierStart1, mBezierStart2);

        // Log.i("hmg", "mBezierEnd1.x  " + mBezierEnd1.x + "  mBezierEnd1.y  "+ mBezierEnd1.y);
        // Log.i("hmg", "mBezierEnd2.x  " + mBezierEnd2.x + "  mBezierEnd2.y  "+ mBezierEnd2.y);

        /**
         * Math.hypot（float a,float b） = a方 + b方 的开平方即 sqrt(x*x+y*y)
         *  返回 触摸点   在   屏幕下方 和右方的投影到右下角形成三角形的斜边
         */
        mTouchToCornerDis = (float) Math.hypot((mTouch.x - mCornerX), (mTouch.y - mCornerY));


        /**
         * 贝塞尔1  2 的顶点  如图三
         * p 为 cd的中点   d 为pe 中点   先求出p点的xy
         * p横坐标为（cx + bx）/2 即贝塞尔1的起点 + 终点 /2
         * p纵坐标为 （cy + by）/2 =
         * 有p点 和e点的坐标 求中点的坐标  ((mBezierStart1.x+mBezierEnd1.x)/2+mBezierControl1.x)/2 化简等价于(mBezierStart1.x+ 2*mBezierControl1.x+mBezierEnd1.x) / 4
         * mBeziervertex1.x 推导
         */
        mBeziervertex1.x = (mBezierStart1.x + 2 * mBezierControl1.x + mBezierEnd1.x) / 4;
        mBeziervertex1.y = (2 * mBezierControl1.y + mBezierStart1.y + mBezierEnd1.y) / 4;
        mBeziervertex2.x = (mBezierStart2.x + 2 * mBezierControl2.x + mBezierEnd2.x) / 4;
        mBeziervertex2.y = (2 * mBezierControl2.y + mBezierStart2.y + mBezierEnd2.y) / 4;
    }

    /**
     * 求解直线P1P2和直线P3P4的交点坐标
     *
     * @param P1
     * @param P2
     * @param P3
     * @param P4
     * @return
     */
    public PointF getCrossPoint(PointF P1, PointF P2, PointF P3, PointF P4) {
        PointF crossoverPoint = new PointF();
        // 二元函数通式： y=ax+b  用2点求ab 列2元一次方程 求解
        // 将2点P1  P2 带入方程    P1.y = P1.x * a +b;    P2.y = P2.x * a +b; 求解出  第一个方程的 a b
        float a1 = (P2.y - P1.y) / (P2.x - P1.x);
        float b1 = ((P1.x * P2.y) - (P2.x * P1.y)) / (P1.x - P2.x);

        // 同样将点带入方程整理 出第二个方程的a  b
        float a2 = (P4.y - P3.y) / (P4.x - P3.x);
        float b2 = ((P3.x * P4.y) - (P4.x * P3.y)) / (P3.x - P4.x);
        // 最后2条线求交点坐标  令交点坐标为xy  求解 方程组
        crossoverPoint.x = (b2 - b1) / (a1 - a2);
        crossoverPoint.y = a1 * crossoverPoint.x + b1;
        return crossoverPoint;
    }

}
