package com.tatsuyuki;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class TestCameraActivity extends Activity implements
		SurfaceHolder.Callback
{
	TextView testView;
	public Bitmap b;
	Camera camera;
	SurfaceView surfaceView;
	SurfaceHolder surfaceHolder;

	private final String tag = "VideoServer";

	Button start, stop, ca;
	EditText ip;
	ImageView iv;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.detectDiskReads().detectDiskWrites().detectNetwork() // or
																						// .detectAll()
																						// for all
																						// detectable
																						// problems
				.penaltyLog().build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
				.detectLeakedSqlLiteObjects().penaltyLog().penaltyDeath().build());
		ip = (EditText) findViewById(R.id.ed_ip);
		start = (Button) findViewById(R.id.btn_start);
		start.setOnClickListener(new Button.OnClickListener()
		{
			public void onClick(View arg0)
			{
				start_camera();
			}
		});
		stop = (Button) findViewById(R.id.btn_stop);
		stop.setOnClickListener(new Button.OnClickListener()
		{
			public void onClick(View arg0)
			{
				stop_camera();
			}
		});
		ca = (Button) findViewById(R.id.ca);
		ca.setOnClickListener(new Button.OnClickListener()
		{
			public void onClick(View arg0)
			{
				new Thread(new Runnable()
				{
					public void run()
					{
						cap();
						re();
					}
				}).start();
				// cap();
			}
		});
		surfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	@SuppressLint("HandlerLeak")
	Handler handler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			ImageView iv = (ImageView) findViewById(R.id.iv);
			Bitmap resize = Bitmap.createScaledBitmap(b,320,240,true);
			Matrix m = new Matrix();
			int width = resize.getWidth();
			int height = resize.getHeight();
			m.setRotate(-90);
			Bitmap bi = Bitmap.createBitmap(resize,0,0,width,height,m,true);
			iv.setImageBitmap(bi);
			super.handleMessage(msg);
		}
	};

	public void re()
	{

		new Thread(new Runnable()
		{

			public void run()
			{
				try
				{
					DatagramSocket socket = new DatagramSocket(8810);
					socket.setReceiveBufferSize(65535);
					byte[] buffer = new byte[65535];

					while(true)
					{
						buffer = new byte[65535];
						DatagramPacket packet = new DatagramPacket(buffer,
								buffer.length);
						socket.receive(packet);
						ByteArrayInputStream bais = new ByteArrayInputStream(buffer,
								0,packet.getLength());
						ObjectInputStream ois = new ObjectInputStream(bais);
						Object o = ois.readObject();
						campacket p = (campacket) o;
						b = BitmapFactory.decodeByteArray(p.getData(),0,
								p.getData().length);
						Log.e("-------------------------------",p.getData().length
								+ "");
						Message msg = handler.obtainMessage();
						msg.arg1 = 1;
						msg.sendToTarget();
					}
				} catch(SocketException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch(IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch(ClassNotFoundException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Log.e("-------------------------------","no!!!!!!!!!!");
			}
		}).start();

	}

	private void start_camera()
	{
		camera = openFrontFacingCameraGingerbread();
		Camera.Parameters param;
		param = camera.getParameters();
		// modify parameter
		param.setPreviewFrameRate(15);
		param.setPreviewSize(176,144);
		List<Camera.Size> sizes = param.getSupportedPreviewSizes();
		for(int i = 0;i < sizes.size();i++)
		{
			Log.i("size:","w=" + sizes.get(i).width + " H=" + sizes.get(i).height);
		}
		camera.setParameters(param);
		try
		{
			camera.setPreviewDisplay(surfaceHolder);
			camera.setDisplayOrientation(90);
			camera.startPreview();
		} catch(Exception e)
		{
			Log.e(tag,"init_camera: " + e);
			return;
		}
	}

	private void stop_camera()
	{
		camera.stopPreview();
		camera.release();
	}

	private Camera openFrontFacingCameraGingerbread()
	{
		int cameraCount = 0;
		Camera cam = null;
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		cameraCount = Camera.getNumberOfCameras();
		for(int camIdx = 0;camIdx < cameraCount;camIdx++)
		{
			Camera.getCameraInfo(camIdx,cameraInfo);
			if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
			{
				try
				{
					cam = Camera.open(camIdx);
				} catch(RuntimeException e)
				{
				}
			}
		}

		return cam;
	}

	public void surfaceChanged(SurfaceHolder arg0,int arg1,int arg2,int arg3)
	{
		// TODO Auto-generated method stub
	}

	public void surfaceCreated(SurfaceHolder holder)
	{
		// TODO Auto-generated method stub
	}

	public void surfaceDestroyed(SurfaceHolder holder)
	{
		// TODO Auto-generated method stub
	}

	PictureCallback jpegCallback = new PictureCallback()
	{
		public void onPictureTaken(byte[] data,Camera camera)
		{
			Bitmap bitmapPicture = BitmapFactory.decodeByteArray(data,0,
					data.length);
			iv.setImageBitmap(bitmapPicture);
			camera.startPreview();
		}
	};

	public void cap()
	{
		// camera.takePicture(null,null,jpegCallback);
		camera.setPreviewCallback(pcb);
	}

	PreviewCallback pcb = new PreviewCallback()
	{

		@Override
		public void onPreviewFrame(byte[] data,Camera arg1)
		{
			// TODO Auto-generated method stub
			// for(int i = 0;i < data.length;i++)
			Log.d(tag,"gg: " + data.length);

			if(data != null)
			{
				Camera.Parameters parameters = camera.getParameters();
				int imageFormat = parameters.getPreviewFormat();
				Log.i("map","Image Format: " + imageFormat);

				Log.i("CameraPreviewCallback","data length:" + data.length);
				if(imageFormat == ImageFormat.NV21)
				{
					// get full picture

					int w = parameters.getPreviewSize().width;
					int h = parameters.getPreviewSize().height;
					Rect rect = new Rect(0,0,w,h);
					YuvImage img = new YuvImage(data,ImageFormat.NV21,w,h,null);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();

					if(img.compressToJpeg(rect,100,baos))
					{

						try
						{
							DatagramSocket socket = new DatagramSocket();
							InetAddress iddr = InetAddress.getByName(ip.getText()
									.toString());
							ByteArrayOutputStream bao = new ByteArrayOutputStream();
							ObjectOutputStream oos = new ObjectOutputStream(bao);
							campacket p = new campacket(baos.toByteArray());
							oos.writeObject(p);
							byte[] buffer = bao.toByteArray();
							Log.i("buffer","" + buffer.length);
							DatagramPacket packet = new DatagramPacket(buffer,
									buffer.length,iddr,8810);
							socket.setSendBufferSize(131070);
							socket.send(packet);
							Log.d(tag,"send");
						} catch(IOException e)
						{
							e.printStackTrace();
						}
					}

				}
			}
		}
	};
}
