package net.ifeu.library.Signature;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import net.ifeu.edicards.Constants.ConstantsFolders;
import net.ifeu.library.Imaging.BitmapConvertor;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class SignatureView extends View {

	private Bitmap mBitmap;
	private Canvas mCanvas;
	private Path mPath;
	private Paint mBitmapPaint;
	private Paint mPaint;

	// 3 Constructors, covers instantiating manually or from a layout file
	public SignatureView(Context context) {
		super(context);
		initSignatureView();
	}

	public SignatureView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initSignatureView();
	}

	public SignatureView(Context context, AttributeSet attrs, int defaultStyle) {
		super(context, attrs, defaultStyle);
		initSignatureView();
	}

	protected void initSignatureView() {
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setColor(Color.BLACK);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(12);
	}

	@Override
	public void onDraw(Canvas canvas) {

		if (mBitmap == null) {
			mBitmap = Bitmap.createBitmap(getMeasuredWidth(),
					getMeasuredHeight(), Bitmap.Config.ARGB_8888);
			mBitmap.eraseColor(Color.WHITE);
			mCanvas = new Canvas(mBitmap);// offscreen canvas
			mPath = new Path();
			mBitmapPaint = new Paint(Paint.DITHER_FLAG);
		}
		canvas.drawColor(Color.WHITE);
		canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint); // draw offscreen
														// changes
		canvas.drawPath(mPath, mPaint); // draw current path
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX();
		float y = event.getY();

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			touch_start(x, y);
			invalidate();
			break;
		case MotionEvent.ACTION_MOVE:
			touch_move(x, y);
			invalidate();
			break;
		case MotionEvent.ACTION_UP:
			touch_up();
			invalidate();
			break;
		}
		return true;
	}

	private float mX, mY;
	private static final float TOUCH_TOLERANCE = 4;

	private void touch_start(float x, float y) {
		mPath.reset();
		mPath.moveTo(x, y);
		mX = x;
		mY = y;
	}

	private void touch_move(float x, float y) {
		float dx = Math.abs(x - mX);
		float dy = Math.abs(y - mY);
		if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
			mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
			mX = x;
			mY = y;
		}
	}

	private void touch_up() {
		mPath.lineTo(mX, mY);
		mCanvas.drawPath(mPath, mPaint);// commit the path to our offscreen
		mPath.reset();// kill this so we don't double draw
	}

	public void save(int tipo, String name) {

		String path;

		try {
			path = Environment.getExternalStorageDirectory().toString() + "/"
					+ ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_FIRMAS
					+ "/"; // this is the sd card

			OutputStream fOut;

			File file;

			if (tipo == 1) {
				file = new File(path, "C_" + name + ".png");
			} else {
				file = new File(path, "V_" + name + ".png");
			}

			fOut = new FileOutputStream(file);
			mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);

			fOut.flush();
			fOut.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void saveBitmap1Color(int tipo, String name) {

		String path;

		try {
			path = Environment.getExternalStorageDirectory().toString() + "/"
					+ ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_FIRMAS
					+ "/"; // this is the sd card

			String file;

			if (tipo == 1) {
				file = path + "C1_" + name + ".bmp";
			} else {
				file = path + "V1_" + name + ".bmp";
			}

			BitmapConvertor converter = new BitmapConvertor();
			String result = converter.convertBitmap(mBitmap, file, mBitmap.getWidth() / 4, mBitmap.getHeight() / 4);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

}