package com.ametro;

import com.ametro.resources.MapResource;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.view.SurfaceHolder;


public class MapRenderThread extends Thread
{

   /** �������, �� ������� ����� �������� */
    private SurfaceHolder surfaceHolder;
    //private MapResource map;
    private boolean isRunning;
    
    private Paint paint;
    
    private Rect field;

    /**
     * �����������
     * @param surfaceHolder ������� ���������
     * @param context �������� ����������
     */
    public MapRenderThread(SurfaceHolder surfaceHolder, Context context, MapResource map)
    {
        this.surfaceHolder = surfaceHolder;
        //this.map = map;
        isRunning = false;

        paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(2);
        paint.setStyle(Style.STROKE);

        field = new Rect(0,0,1050,1220);
    }

    /**
     * ������� ��������� ������
     * @param running
     */
    public void setRunning(boolean running)
    {
        isRunning = running;
        
    }

    @Override
    /** ��������, ����������� � ������ */
    public void run()
    {
        while (isRunning)
        {
            Canvas canvas = null;
            try
            {
                // ���������� Canvas-�
                canvas = surfaceHolder.lockCanvas();
                synchronized (surfaceHolder)
                {
                    // ���������� ���������
                    canvas.drawRect(field, paint);
                }
            }
            catch (Exception e) { }
            finally
            {
                if (canvas != null)
                {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }
}