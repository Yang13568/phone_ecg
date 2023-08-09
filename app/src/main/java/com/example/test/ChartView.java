package com.example.test;

/*
 *  KY LAB program source for development
 *
 *	Ver: EM202.1.0
 *	Data : 2010/06/01
 *	Designer : Weiting Lin
 *
 *	function description:
 *	RAW Data chart draw.
 */
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class ChartView extends View
{
    // Debugging
    private static final String TAG = "ChartView";
    private static final boolean D = true;

    protected static int mTileSize;
    final float PROPORTION = 1f;

    private Bitmap  mBitmap;
    private Paint   mPaint = new Paint();
    private Paint	textPaint = new Paint();
    private Canvas  mCanvas = new Canvas();

    private Resources res = getResources();

    private int X_Axis;
    private int Y_Axis;
    private float   mLastX;
    private float	mNextX;
    private float	mLastY;
    private float	mNextY;
    private float   mSpeed;
    private int   mMaxX;

    private String mIHR ;

    private ShapeDrawable Mask = new ShapeDrawable(new RectShape());


    public ChartView(Context context)
    {	// TODO Auto-generated constructor stub
        super(context);
    }

    //從外部使用此class 則必需使用此初始函式
    public ChartView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        if (D) Log.d(TAG, "ChartView Initial");

        //get argument in value/dimens
        X_Axis = (int) res.getDimension(R.dimen.Max_X_Axis) ;	//Default 100px
        Y_Axis = (int) res.getDimension(R.dimen.Max_Y_Axis) ;	//Default 400px

        mLastX = 0;
        mNextX = 0;
        mLastY = 0;
        mSpeed = 5.0f;
        mMaxX = X_Axis;

        //Set mPaint 各項參數
        Mask.getPaint().setColor(Color.BLACK);
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle( Paint.Style.FILL_AND_STROKE);
        mPaint.setStrokeWidth(2);
        mPaint.setTextSize(50);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
//        mPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.GREEN);
        textPaint.setStyle( Paint.Style.STROKE );
        textPaint.setStrokeWidth(2);
        textPaint.setTextSize(5);

        //in pixel unit
        mBitmap = Bitmap.createBitmap(X_Axis, Y_Axis, Bitmap.Config.RGB_565);
        mCanvas.setBitmap(mBitmap);
        mCanvas.drawColor(Color.BLACK);
    }

    //將各變數初始化
    public void ClearChart()
    {
        if (mBitmap != null)
        {
            final Canvas canvas = mCanvas;

            Mask.setBounds(0,0 , X_Axis , Y_Axis);
            Mask.draw(canvas);
            invalidate();//invalidate()觸發 onDraw 事件
        }
        mLastX = 0;
        mLastY = 0;
    }

    public void setIHR(String IHR)
    {
        mIHR = IHR;
    }

    public void setX_Axis(int xValue)
    {
        X_Axis = xValue;

        //in pixel unit
        mBitmap = Bitmap.createBitmap(X_Axis, Y_Axis, Bitmap.Config.RGB_565);
        mCanvas.setBitmap(mBitmap);
        mCanvas.drawColor(Color.BLACK);

        mMaxX = X_Axis;
    }

    protected void onDraw(Canvas canvas)
    {
        synchronized (this)
        {
            if (mBitmap != null)
            {
                canvas.drawBitmap(mBitmap, 0, 0, null);
            }
        }//end of [synchronized (this)]
    }//end of [protected void onDraw(Canvas canvas)]

    public void Wave_Draw(byte[] Raw_Data)
    {
        synchronized (this)
        {
            if (mBitmap != null)
            {
                final Canvas canvas = mCanvas;
                final Paint paint = mPaint;
                int Mask_Start;
                int Mask_End;



                Mask_Start = (int)mLastX;
                Mask_End = (int)(mLastX + (Raw_Data.length * mSpeed));

                if ( Mask_End < mMaxX)
                {
                    Mask.setBounds(Mask_Start,0 , Mask_End , Y_Axis);
                    Mask.draw(canvas);
                }else
                {
                    Mask.setBounds(Mask_Start,0 , mMaxX , Y_Axis);
                    Mask.draw(canvas);
                    Mask_End = Mask_End - mMaxX ;
                    Mask.setBounds(0,0 ,Mask_End , Y_Axis);
                    Mask.draw(canvas);
                }

                //Moving mark line
                canvas.drawLine(Mask_End+1, 0,Mask_End+1 , Y_Axis, paint);

                for (byte raw_datum : Raw_Data) {
                    mNextX = mLastX + mSpeed;
                    if (mNextX == mMaxX) {
                        mNextX = 0;
                    }//跳過一個點，無損於ECG data 判讀
                    else {
                        //Java not support unsigned value, so byte's value is -128 ~ 127
                        mNextY = Y_Axis - ((raw_datum & 0xFF) * PROPORTION);
                        canvas.drawLine(mLastX, mLastY, mNextX, mNextY, paint);
                    }
                    mLastX = mNextX;
                    mLastY = mNextY;

                }

                invalidate();//invalidate()觸發 onDraw 事件
            }//end of [if (mBitmap != null) ]
        }//end of [synchronized (this) ]
    }//end of [public void Wave_Draw(String Data)]


}//end of [private class GraphView extends View]

/**********************************
 Notice Notes
 *********************************/
/*(A)
 * Synchronized使用時，需指定一個物件，系統會Lock此物件，當
 * 程式進入Synchrnoized區塊或Method時，該物件會被Lock，直到
 * 離開Synchronized時才會被釋放。在Lock期間，鎖定同一物件的
 * 其他Synchronized區塊，會因為無法取得物件的Lock而等待。
 * 待物件Release Lock後，其他的Synchronized區塊會有一個取得
 * 該物件的Lock而可以執行。
 * */
/*(B)
 * 一般在剛開始開發android時，會犯一個錯誤，即在View的構造函數
 * 中獲取getWidth()和getHeight()，當一個view對象創建時，android
 * 並不知道其大小，所以getWidth()和getHeight( )返回的結果是0，真
 * 正大小是在計算佈局時才會計算，所以會發現一個有趣的事，即在
 * onDraw( ) 卻能取得長寬的原因。
 */

