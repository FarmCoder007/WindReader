package com.zijie.treader.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.zijie.treader.Config;
import com.zijie.treader.R;
import com.zijie.treader.db.BookCatalogue;
import com.zijie.treader.db.BookInfo;
import com.zijie.treader.view.PageWidget;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/7/20 0020.
 */
public class PageFactory {
    private static final String TAG = "PageFactory";
    private static PageFactory pageFactory;

    private Context mContext;
    private Config config;
    //当前的书本
//    private File book_file = null;
    // 默认背景颜色
    private int m_backColor = 0xffff9e85;
    /**
     * 屏幕宽度
     */
    private int mScreenWidth;
    /**
     * 屏幕高度
     */
    private int mScreenHeight;
    //文字字体大小
    private float m_fontSize;
    //时间格式
    private SimpleDateFormat sdf;
    //时间
    private String date;
    //进度格式
    private DecimalFormat df;
    //电池边界宽度
    private float mBorderWidth;
    // 上下与边缘的距离
    private float marginHeight;
    /**
     * 绘制文字时的起始位置x
     */
    private float drawWordsStart_x;
    /**
     * 绘制时   控制 左右与边缘的距离
     */
    private float marginWidth;
    //状态栏距离底部高度
    private float statusMarginBottom;
    /**
     * 行间距
     */
    private float lineSpace;
    //段间距
    private float paragraphSpace;
    //字高度
    private float fontHeight;
    //字体
    private Typeface typeface;
    //文字画笔
    private Paint mPaint;
    //加载画笔
    private Paint waitPaint;
    //文字颜色
    private int m_textColor = Color.rgb(50, 65, 78);
    /**
     * 实际 绘制文字的总高度 = 屏幕高度 - 上下边距
     */
    private float realCanDrawHeight;
    /**
     * 实际 绘制文字的总宽度 = 屏幕宽度 - 左右边距
     */
    private float realCanDrawWidth;
    /**
     * 每页可以显示的行数
     */
    private int mLineCount;
    //电池画笔
    private Paint mBatterryPaint;
    //电池字体大小
    private float mBatterryFontSize;
    //背景图片
    private Bitmap m_book_bg = null;
    //当前显示的文字
//    private StringBuilder word = new StringBuilder();
    //当前总共的行
//    private Vector<String> m_lines = new Vector<>();
//    // 当前页起始位置
//    private long m_mbBufBegin = 0;
//    // 当前页终点位置
//    private long m_mbBufEnd = 0;
//    // 之前页起始位置
//    private long m_preBegin = 0;
//    // 之前页终点位置
//    private long m_preEnd = 0;
    // 图书总长度
//    private long m_mbBufLen = 0;
    private Intent batteryInfoIntent;
    //电池电量百分比
    private float mBatteryPercentage;
    //电池外边框
    private RectF rect1 = new RectF();
    //电池内边框
    private RectF rect2 = new RectF();
    //文件编码
//    private String m_strCharsetName = "GBK";
    //当前是否为第一页
    private boolean m_isfirstPage;
    //当前是否为最后一页
    private boolean m_islastPage;
    //书本widget
    private PageWidget mBookPageWidget;
    //    //书本所有段
//    List<String> allParagraph;
//    //书本所有行
//    List<String> allLines = new ArrayList<>();
    //现在的进度
    private float currentProgress;
    //目录
//    private List<BookCatalogue> directoryList = new ArrayList<>();
    //书本路径
    private String bookPath = "";
    //书本名字
    private String bookName = "";
    private BookInfo bookInfo;
    //书本章节
    private int currentCharter = 0;
    //当前电量
    private int level = 0;
    private BookUtil mBookUtil;
    /**
     * 阅读进度改变监听
     */
    private PageProgressChangeListener pageProgressChangeListener;
    private TRPage currentPage;
    private TRPage prePage;
    private TRPage cancelPage;
    private BookTask bookTask;
    ContentValues values = new ContentValues();

    private static Status mStatus = Status.OPENING;

    /**
     * 打开书籍状态
     */
    public enum Status {
        OPENING,
        FINISH,
        FAIL,
    }

    /**
     * 获取页面工厂的实例
     *
     * @return
     */
    public static synchronized PageFactory getInstance() {
        return pageFactory;
    }

    public static synchronized PageFactory createPageFactory(Context context) {
        if (pageFactory == null) {
            pageFactory = new PageFactory(context);
        }
        return pageFactory;
    }

    /**
     * 私有构造
     *
     * @param context
     */
    private PageFactory(Context context) {
        mBookUtil = new BookUtil();
        mContext = context.getApplicationContext();
        config = Config.getInstance();
        //获取屏幕宽高
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metric = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metric);
        mScreenWidth = metric.widthPixels;
        mScreenHeight = metric.heightPixels;

        sdf = new SimpleDateFormat("HH:mm");//HH:mm为24小时制,hh:mm为12小时制
        date = sdf.format(new java.util.Date());
        df = new DecimalFormat("#0.0");

        marginWidth = mContext.getResources().getDimension(R.dimen.readingMarginWidth);
        marginHeight = mContext.getResources().getDimension(R.dimen.readingMarginHeight);
        statusMarginBottom = mContext.getResources().getDimension(R.dimen.reading_status_margin_bottom);
        lineSpace = context.getResources().getDimension(R.dimen.reading_line_spacing);
        paragraphSpace = context.getResources().getDimension(R.dimen.reading_paragraph_spacing);
        realCanDrawWidth = mScreenWidth - marginWidth * 2;
        realCanDrawHeight = mScreenHeight - marginHeight * 2;

        typeface = config.getTypeface();
        // 初始化字体大小
        m_fontSize = config.getFontSize();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);// 画笔
        mPaint.setTextAlign(Paint.Align.LEFT);// 左对齐
        mPaint.setTextSize(m_fontSize);// 字体大小
        mPaint.setColor(m_textColor);// 字体颜色
        mPaint.setTypeface(typeface);
        mPaint.setSubpixelText(true);// 设置该项为true，将有助于文本在LCD屏幕上的显示效果

        waitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);// 画笔
        waitPaint.setTextAlign(Paint.Align.LEFT);// 左对齐
        waitPaint.setTextSize(mContext.getResources().getDimension(R.dimen.reading_max_text_size));// 字体大小
        waitPaint.setColor(m_textColor);// 字体颜色
        waitPaint.setTypeface(typeface);
        waitPaint.setSubpixelText(true);// 设置该项为true，将有助于文本在LCD屏幕上的显示效果
        calculateLineCount();

        mBorderWidth = mContext.getResources().getDimension(R.dimen.reading_board_battery_border_width);
        mBatterryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBatterryFontSize = CommonUtil.sp2px(context, 12);
        mBatterryPaint.setTextSize(mBatterryFontSize);
        mBatterryPaint.setTypeface(typeface);
        mBatterryPaint.setTextAlign(Paint.Align.LEFT);
        mBatterryPaint.setColor(m_textColor);
        batteryInfoIntent = context.getApplicationContext().registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));//注册广播,随时获取到电池电量信息

        initBg(config.getDayOrNight());
        measureWordsStart_x();
    }

    /**
     * 测量 绘制文字时的起点
     */
    private void measureWordsStart_x() {
        // 一个空字符的实际占用的宽度
        float wordWidth = mPaint.measureText("\u3000");
        float width = realCanDrawWidth % wordWidth;
        drawWordsStart_x = marginWidth + width / 2;

//        Rect rect = new Rect();
//        mPaint.getTextBounds("好", 0, 1, rect);
//        float wordHeight = rect.height();
//        float wordW = rect.width();
//        Paint.FontMetrics fm = mPaint.getFontMetrics();
//        float wrodH = (float) (Math.ceil(fm.top + fm.bottom + fm.leading));
//        String a = "";

    }

    /**
     * 初始化背景
     */
    private void initBg(Boolean isNight) {
        if (isNight) {
            //设置背景
//            setBgBitmap(BitmapUtil.decodeSampledBitmapFromResource(
//                    mContext.getResources(), R.drawable.main_bg, mScreenWidth, mScreenHeight));
            Bitmap bitmap = Bitmap.createBitmap(mScreenWidth, mScreenHeight, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.BLACK);
            setBgBitmap(bitmap);
            //设置字体颜色
            setTextColor(Color.rgb(128, 128, 128));
            setBookPageWidgetBg(Color.BLACK);
        } else {
            //设置背景
            creatBookPageBgBitmap(config.getBookBgType());
        }
    }

    /**
     * 计算 可显示的行数  【实际可绘制高度 - 字体大小- 行间距】
     */
    private void calculateLineCount() {
        mLineCount = (int) (realCanDrawHeight / (m_fontSize + lineSpace));
    }

    private void drawStatus(Bitmap bitmap) {
        String status = "";
        switch (mStatus) {
            case OPENING:
                status = "正在打开书本...";
                break;
            case FAIL:
                status = "打开书本失败！";
                break;
        }

        // 将替换完成的背景  绘制给指定页的bitmap
        Canvas c = new Canvas(bitmap);
        c.drawBitmap(getBgBitmap(), 0, 0, null);
        waitPaint.setColor(getTextColor());
        waitPaint.setTextAlign(Paint.Align.CENTER);

        Rect targetRect = new Rect(0, 0, mScreenWidth, mScreenHeight);
//        c.drawRect(targetRect, waitPaint);
        Paint.FontMetricsInt fontMetrics = waitPaint.getFontMetricsInt();
        // 转载请注明出处：http://blog.csdn.net/hursing
        int baseline = (targetRect.bottom + targetRect.top - fontMetrics.bottom - fontMetrics.top) / 2;
        // 下面这行是实现水平居中，drawText对应改为传入targetRect.centerX()
        waitPaint.setTextAlign(Paint.Align.CENTER);
        c.drawText(status, targetRect.centerX(), baseline, waitPaint);
//        c.drawText("正在打开书本...", mScreenHeight / 2, 0, waitPaint);
        mBookPageWidget.postInvalidate();
    }

    /**
     * 执行绘制界面的方法  绘制背景 绘制文字等
     *
     * @param bitmap        要绘制的载体bitmap
     * @param m_lines
     * @param updateCharter
     */
    public void onDraw(Bitmap bitmap, List<String> m_lines, Boolean updateCharter) {
        if (getDirectoryList().size() > 0 && updateCharter) {
            currentCharter = getCurrentCharter();
        }
        //更新数据库进度
        if (currentPage != null && bookInfo != null) {
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    values.put("begin", currentPage.getBegin());
                    DataSupport.update(BookInfo.class, values, bookInfo.getId());
                }
            }.start();
        }

        // 将替换完成的背景  绘制给指定页的bitmap
        Canvas c = new Canvas(bitmap);
        c.drawBitmap(getBgBitmap(), 0, 0, null);
//        word.setLength(0);
        // 设置文字画笔颜色
        mPaint.setTextSize(getFontSize());
        mPaint.setColor(getTextColor());
        mBatterryPaint.setColor(getTextColor());
        if (m_lines.size() == 0) {
            return;
        }

        // 执行绘制行数
        if (m_lines.size() > 0) {
            float y = marginHeight;
            for (String strLine : m_lines) {
                y += m_fontSize + lineSpace;
                c.drawText(strLine, drawWordsStart_x, y, mPaint);
//                word.append(strLine);
            }
        }

        //画进度及时间
        int dateWith = (int) (mBatterryPaint.measureText(date) + mBorderWidth);//时间宽度
        float fPercent = (float) (currentPage.getBegin() * 1.0 / mBookUtil.getBookLen());//进度
        currentProgress = fPercent;
        if (pageProgressChangeListener != null) {
            pageProgressChangeListener.changeProgress(fPercent);
        }
        String strPercent = df.format(fPercent * 100) + "%";//进度文字
        int nPercentWidth = (int) mBatterryPaint.measureText("999.9%") + 1;  //Paint.measureText直接返回參數字串所佔用的寬度
        c.drawText(strPercent, mScreenWidth - nPercentWidth, mScreenHeight - statusMarginBottom, mBatterryPaint);//x y为坐标值
        c.drawText(date, marginWidth, mScreenHeight - statusMarginBottom, mBatterryPaint);
        // 画电池
        level = batteryInfoIntent.getIntExtra("level", 0);
        int scale = batteryInfoIntent.getIntExtra("scale", 100);
        mBatteryPercentage = (float) level / scale;
        float rect1Left = marginWidth + dateWith + statusMarginBottom;//电池外框left位置
        //画电池外框
        float width = CommonUtil.convertDpToPixel(mContext, 20) - mBorderWidth;
        float height = CommonUtil.convertDpToPixel(mContext, 10);
        rect1.set(rect1Left, mScreenHeight - height - statusMarginBottom, rect1Left + width, mScreenHeight - statusMarginBottom);
        rect2.set(rect1Left + mBorderWidth, mScreenHeight - height + mBorderWidth - statusMarginBottom, rect1Left + width - mBorderWidth, mScreenHeight - mBorderWidth - statusMarginBottom);
        c.save(Canvas.CLIP_SAVE_FLAG);
        c.clipRect(rect2, Region.Op.DIFFERENCE);
        c.drawRect(rect1, mBatterryPaint);
        c.restore();
        //画电量部分
        rect2.left += mBorderWidth;
        rect2.right -= mBorderWidth;
        rect2.right = rect2.left + rect2.width() * mBatteryPercentage;
        rect2.top += mBorderWidth;
        rect2.bottom -= mBorderWidth;
        c.drawRect(rect2, mBatterryPaint);
        //画电池头
        int poleHeight = (int) CommonUtil.convertDpToPixel(mContext, 10) / 2;
        rect2.left = rect1.right;
        rect2.top = rect2.top + poleHeight / 4;
        rect2.right = rect1.right + mBorderWidth;
        rect2.bottom = rect2.bottom - poleHeight / 4;
        c.drawRect(rect2, mBatterryPaint);
        //画书名
        c.drawText(CommonUtil.subString(bookName, 12), marginWidth, statusMarginBottom + mBatterryFontSize, mBatterryPaint);
        //画章
        if (getDirectoryList().size() > 0) {
            String charterName = CommonUtil.subString(getDirectoryList().get(currentCharter).getBookCatalogue(), 12);
            int nChaterWidth = (int) mBatterryPaint.measureText(charterName) + 1;
            c.drawText(charterName, mScreenWidth - marginWidth - nChaterWidth, statusMarginBottom + mBatterryFontSize, mBatterryPaint);
        }

        mBookPageWidget.postInvalidate();
    }

    //向前翻页
    public void prePage() {
        if (currentPage.getBegin() <= 0) {
            Log.e(TAG, "当前是第一页");
            if (!m_isfirstPage) {
                Toast.makeText(mContext, "当前是第一页", Toast.LENGTH_SHORT).show();
            }
            m_isfirstPage = true;
            return;
        } else {
            m_isfirstPage = false;
        }

        cancelPage = currentPage;
        onDraw(mBookPageWidget.getCurPageBitmap(), currentPage.getLines(), true);
        currentPage = getPrePage();
        onDraw(mBookPageWidget.getNextPageBitmap(), currentPage.getLines(), true);
    }

    /**
     * 向后翻页
     */
    public void nextPage() {
        if (currentPage.getEnd() >= mBookUtil.getBookLen()) {
            Log.e(TAG, "已经是最后一页了");
            if (!m_islastPage) {
                Toast.makeText(mContext, "已经是最后一页了", Toast.LENGTH_SHORT).show();
            }
            m_islastPage = true;
            return;
        } else {
            m_islastPage = false;
        }

        cancelPage = currentPage;
        onDraw(mBookPageWidget.getCurPageBitmap(), currentPage.getLines(), true);
        prePage = currentPage;
        currentPage = getNextPage();
        onDraw(mBookPageWidget.getNextPageBitmap(), currentPage.getLines(), true);
        Log.e("nextPage", "nextPagenext");
    }

    /**
     * 取消翻页
     */
    public void cancelPage() {
        currentPage = cancelPage;
    }

    /**
     * 打开书本
     *
     * @throws IOException
     */
    public void openBook(BookInfo bookInfo) throws IOException {
        //清空数据
        currentCharter = 0;
//        m_mbBufLen = 0;
        initBg(config.getDayOrNight());

        this.bookInfo = bookInfo;
        bookPath = bookInfo.getBookpath();
        bookName = FileUtils.getFileName(bookPath);

        mStatus = Status.OPENING;
        drawStatus(mBookPageWidget.getCurPageBitmap());
        drawStatus(mBookPageWidget.getNextPageBitmap());
        if (bookTask != null && bookTask.getStatus() != AsyncTask.Status.FINISHED) {
            bookTask.cancel(true);
        }
        bookTask = new BookTask();
        bookTask.execute(bookInfo.getBegin());
    }

    private class BookTask extends AsyncTask<Long, Void, Boolean> {
        private long begin = 0;

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            Log.e("onPostExecute", isCancelled() + "");
            if (isCancelled()) {
                return;
            }
            if (result) {
                PageFactory.mStatus = PageFactory.Status.FINISH;
//                m_mbBufLen = mBookUtil.getBookLen();
                currentPage = getPageForBegin(begin);
                if (mBookPageWidget != null) {
                    drawCurrentPage(true);
                }
            } else {
                PageFactory.mStatus = PageFactory.Status.FAIL;
                drawStatus(mBookPageWidget.getCurPageBitmap());
                drawStatus(mBookPageWidget.getNextPageBitmap());
                Toast.makeText(mContext, "打开书本失败！", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Boolean doInBackground(Long... params) {
            begin = params[0];
            try {
                mBookUtil.openBook(bookInfo);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

    }

    public TRPage getNextPage() {
        mBookUtil.setPostition(currentPage.getEnd());

        TRPage trPage = new TRPage();
        trPage.setBegin(currentPage.getEnd() + 1);
        Log.e("begin", currentPage.getEnd() + 1 + "");
        trPage.setLines(getNextLines());
        Log.e("end", mBookUtil.getPosition() + "");
        trPage.setEnd(mBookUtil.getPosition());
        return trPage;
    }

    public TRPage getPrePage() {
        mBookUtil.setPostition(currentPage.getBegin());

        TRPage trPage = new TRPage();
        trPage.setEnd(mBookUtil.getPosition() - 1);
        Log.e("end", mBookUtil.getPosition() - 1 + "");
        trPage.setLines(getPreLines());
        Log.e("begin", mBookUtil.getPosition() + "");
        trPage.setBegin(mBookUtil.getPosition());
        return trPage;
    }

    public TRPage getPageForBegin(long begin) {
        TRPage trPage = new TRPage();
        trPage.setBegin(begin);

        mBookUtil.setPostition(begin - 1);
        trPage.setLines(getNextLines());
        trPage.setEnd(mBookUtil.getPosition());
        return trPage;
    }

    public List<String> getNextLines() {
        List<String> lines = new ArrayList<>();
        float width = 0;
        float height = 0;
        String line = "";
        while (mBookUtil.next(true) != -1) {
            char word = (char) mBookUtil.next(false);
            //判断是否换行
            if ((word + "").equals("\r") && (((char) mBookUtil.next(true)) + "").equals("\n")) {
                mBookUtil.next(false);
                if (!line.isEmpty()) {
                    lines.add(line);
                    line = "";
                    width = 0;
//                    height +=  paragraphSpace;
                    if (lines.size() == mLineCount) {
                        break;
                    }
                }
            } else {
                float widthChar = mPaint.measureText(word + "");
                width += widthChar;
                if (width > realCanDrawWidth) {
                    width = widthChar;
                    lines.add(line);
                    line = word + "";
                } else {
                    line += word;
                }
            }

            if (lines.size() == mLineCount) {
                if (!line.isEmpty()) {
                    mBookUtil.setPostition(mBookUtil.getPosition() - 1);
                }
                break;
            }
        }

        if (!line.isEmpty() && lines.size() < mLineCount) {
            lines.add(line);
        }
        for (String str : lines) {
            Log.e(TAG, str + "   ");
        }
        return lines;
    }

    /**
     * 获取前一页的行信息集合
     *
     * @return
     */
    public List<String> getPreLines() {
        List<String> lines = new ArrayList<>();
        float width = 0;
        // 每行信息
        String line = "";
        // 前一页  字符数组断行
        char[] par = mBookUtil.preLineWordsArray();
        while (par != null) {
            List<String> preLines = new ArrayList<>();
            for (int i = 0; i < par.length; i++) {
                char word = par[i];
                float widthChar = mPaint.measureText(word + "");
                width += widthChar;
                if (width > realCanDrawWidth) {
                    width = widthChar;
                    preLines.add(line);
                    line = word + "";
                } else {
                    line += word;
                }
            }
            if (!line.isEmpty()) {
                preLines.add(line);
            }

            lines.addAll(0, preLines);

            if (lines.size() >= mLineCount) {
                break;
            }
            width = 0;
            line = "";
            par = mBookUtil.preLineWordsArray();
        }

        List<String> reLines = new ArrayList<>();
        int num = 0;
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (reLines.size() < mLineCount) {
                reLines.add(0, lines.get(i));
            } else {
                num = num + lines.get(i).length();
            }
            Log.e(TAG, lines.get(i) + "   ");
        }

        if (num > 0) {
            if (mBookUtil.getPosition() > 0) {
                mBookUtil.setPostition(mBookUtil.getPosition() + num + 2);
            } else {
                mBookUtil.setPostition(mBookUtil.getPosition() + num);
            }
        }

        return reLines;
    }

    //上一章
    public void preChapter() {
        if (mBookUtil.getDirectoryList().size() > 0) {
            int num = currentCharter;
            if (num == 0) {
                num = getCurrentCharter();
            }
            num--;
            if (num >= 0) {
                long begin = mBookUtil.getDirectoryList().get(num).getBookCatalogueStartPos();
                currentPage = getPageForBegin(begin);
                drawCurrentPage(true);
                currentCharter = num;
            }
        }
    }

    /**
     * 下一章
     */
    public void nextChapter() {
        int num = currentCharter;
        if (num == 0) {
            num = getCurrentCharter();
        }
        num++;
        if (num < getDirectoryList().size()) {
            long begin = getDirectoryList().get(num).getBookCatalogueStartPos();
            currentPage = getPageForBegin(begin);
            drawCurrentPage(true);
            currentCharter = num;
        }
    }

    /**
     * 获取现在的章
     */
    public int getCurrentCharter() {
        int num = 0;
        for (int i = 0; getDirectoryList().size() > i; i++) {
            BookCatalogue bookCatalogue = getDirectoryList().get(i);
            if (currentPage.getEnd() >= bookCatalogue.getBookCatalogueStartPos()) {
                num = i;
            } else {
                break;
            }
        }
        return num;
    }

    /**
     * 绘制当前页面
     * updateChapter
     */
    private void drawCurrentPage(Boolean updateChapter) {
        onDraw(mBookPageWidget.getCurPageBitmap(), currentPage.getLines(), updateChapter);
        onDraw(mBookPageWidget.getNextPageBitmap(), currentPage.getLines(), updateChapter);
    }

    //更新电量
    public void updateBattery(int mLevel) {
        if (currentPage != null && mBookPageWidget != null && !mBookPageWidget.isRunning()) {
            if (level != mLevel) {
                level = mLevel;
                drawCurrentPage(false);
            }
        }
    }

    public void updateTime() {
        if (currentPage != null && mBookPageWidget != null && !mBookPageWidget.isRunning()) {
            String mDate = sdf.format(new java.util.Date());
            if (date != mDate) {
                date = mDate;
                drawCurrentPage(false);
            }
        }
    }

    //改变进度
    public void changeProgress(float progress) {
        long begin = (long) (mBookUtil.getBookLen() * progress);
        currentPage = getPageForBegin(begin);
        drawCurrentPage(true);
    }

    //改变进度
    public void changeChapter(long begin) {
        currentPage = getPageForBegin(begin);
        drawCurrentPage(true);
    }

    //改变字体大小
    public void changeFontSize(int fontSize) {
        this.m_fontSize = fontSize;
        mPaint.setTextSize(m_fontSize);
        calculateLineCount();
        measureWordsStart_x();
        currentPage = getPageForBegin(currentPage.getBegin());
        drawCurrentPage(true);
    }

    //改变字体
    public void changeTypeface(Typeface typeface) {
        this.typeface = typeface;
        mPaint.setTypeface(typeface);
        mBatterryPaint.setTypeface(typeface);
        calculateLineCount();
        measureWordsStart_x();
        currentPage = getPageForBegin(currentPage.getBegin());
        drawCurrentPage(true);
    }

    /**
     * 改变背景
     */
    public void changeBookBg(int type) {
        creatBookPageBgBitmap(type);
        drawCurrentPage(false);
    }

    /**
     * 设置 中  更改页面的背景颜色 和字体颜色  【生成背景bitmap】
     */
    private void creatBookPageBgBitmap(int type) {
        Bitmap bitmap = Bitmap.createBitmap(mScreenWidth, mScreenHeight, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        int color = 0;
        switch (type) {
            case Config.BOOK_BG_DEFAULT:
                canvas = null;
                bitmap.recycle();
                if (getBgBitmap() != null) {
                    getBgBitmap().recycle();
                }
                bitmap = BitmapUtil.decodeSampledBitmapFromResource(
                        mContext.getResources(), R.drawable.paper, mScreenWidth, mScreenHeight);
                color = mContext.getResources().getColor(R.color.read_font_default);
                setBookPageWidgetBg(mContext.getResources().getColor(R.color.read_bg_default));
                break;
            case Config.BOOK_BG_1:
                canvas.drawColor(mContext.getResources().getColor(R.color.read_bg_1));
                color = mContext.getResources().getColor(R.color.read_font_1);
                setBookPageWidgetBg(mContext.getResources().getColor(R.color.read_bg_1));
                break;
            case Config.BOOK_BG_2:
                canvas.drawColor(mContext.getResources().getColor(R.color.read_bg_2));
                color = mContext.getResources().getColor(R.color.read_font_2);
                setBookPageWidgetBg(mContext.getResources().getColor(R.color.read_bg_2));
                break;
            case Config.BOOK_BG_3:
                canvas.drawColor(mContext.getResources().getColor(R.color.read_bg_3));
                color = mContext.getResources().getColor(R.color.read_font_3);
                if (mBookPageWidget != null) {
                    mBookPageWidget.setBgColor(mContext.getResources().getColor(R.color.read_bg_3));
                }
                break;
            case Config.BOOK_BG_4:
                canvas.drawColor(mContext.getResources().getColor(R.color.read_bg_4));
                color = mContext.getResources().getColor(R.color.read_font_4);
                setBookPageWidgetBg(mContext.getResources().getColor(R.color.read_bg_4));
                break;
            default:
                break;
        }

        setBgBitmap(bitmap);
        //设置字体颜色
        setTextColor(color);
    }

    /**
     * 给控件 设置书页背景颜色
     *
     * @param color
     */
    private void setBookPageWidgetBg(int color) {
        if (mBookPageWidget != null) {
            mBookPageWidget.setBgColor(color);
        }
    }

    /**
     * 设置日间或者夜间模式
     */
    public void setDayOrNight(Boolean isNgiht) {
        initBg(isNgiht);
        drawCurrentPage(false);
    }

    /**
     * 释放资源
     */
    public void clear() {
        currentCharter = 0;
        bookPath = "";
        bookName = "";
        bookInfo = null;
        mBookPageWidget = null;
        pageProgressChangeListener = null;
        cancelPage = null;
        prePage = null;
        currentPage = null;
    }

    /**
     * 获取打开书籍状态
     *
     * @return
     */
    public static Status getStatus() {
        return mStatus;
    }

    /**
     * 获取书籍长度 存书签用
     *
     * @return
     */
    public long getBookLen() {
        return mBookUtil.getBookLen();
    }

    /**
     * 获取当前页 信息
     *
     * @return
     */
    public TRPage getCurrentPage() {
        return currentPage;
    }

    /**
     * 获取书本的章节列表
     */
    public List<BookCatalogue> getDirectoryList() {
        return mBookUtil.getDirectoryList();
    }

    /**
     * 获取书籍路径
     *
     * @return
     */
    public String getBookPath() {
        return bookPath;
    }

    /**
     * 是否是第一页
     */
    public boolean isfirstPage() {
        return m_isfirstPage;
    }

    /**
     * 是否是最后一页
     */
    public boolean islastPage() {
        return m_islastPage;
    }

    /**
     * 设置页面背景
     */
    private void setBgBitmap(Bitmap BG) {
        m_book_bg = BG;
    }

    /**
     * 获取页面背景
     */
    private Bitmap getBgBitmap() {
        return m_book_bg;
    }

    /**
     * 设置文字颜色
     */
    private void setTextColor(int textColor) {
        this.m_textColor = textColor;
    }

    /**
     * 获取文字颜色
     */
    public int getTextColor() {
        return this.m_textColor;
    }

    /**
     * 获取文字大小
     */
    private float getFontSize() {
        return this.m_fontSize;
    }

    /**
     * 设置阅读页  控件
     *
     * @param mBookPageWidget
     */
    public void setPageWidget(PageWidget mBookPageWidget) {
        this.mBookPageWidget = mBookPageWidget;
    }

    /**
     * 设置阅读进度改变监听
     *
     * @param progressChangeListener
     */
    public void setPageProgressChangeListener(PageProgressChangeListener progressChangeListener) {
        this.pageProgressChangeListener = progressChangeListener;
    }

    /**
     * 阅读进度监听
     */
    public interface PageProgressChangeListener {
        void changeProgress(float progress);
    }

}
