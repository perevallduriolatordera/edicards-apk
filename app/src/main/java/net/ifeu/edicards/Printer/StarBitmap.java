package net.ifeu.edicards.Printer;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.Color;

public class StarBitmap
{
	int[] pixels;
	int height;
	int width;
	boolean dithering;
	byte[] imageData;
	
	StarBitmap(Bitmap picture, boolean supportDithering, int maxWidth)
	{
		if(picture.getWidth() > maxWidth)
		{
			ScallImage(picture, maxWidth);
		}
		else
		{
			height = picture.getHeight();
			width = picture.getWidth();
			pixels = new int[height * width];
			for(int y=0;y < height; y++)
			{
				for(int x=0;x<width; x++)
				{
					pixels[PixelIndex(x,y)] = picture.getPixel(x, y);
				}
			}
			//picture.getPixels(pixels, 0, width, 0, 0, width, height);
		}
		
		dithering = supportDithering;
		imageData = null;
	}
	
	private int pixelBrightness(int red, int green, int blue)
	{
		return (red + green + blue) / 3;
	}
	
	private int PixelIndex(int x, int y)
	{
		return (y * width) + x;
	}
	
	public void ScallImage(Bitmap picture, int newWidth)
	{
		int w1 = picture.getWidth();
		int h1 = picture.getHeight();
		int newHeight = newWidth * h1;
		newHeight = newHeight / w1;
		Bitmap bm = Bitmap.createScaledBitmap(picture, newWidth, newHeight, false);	
		height = bm.getHeight();
		width = bm.getWidth();
		pixels = new int[height * width];
		for(int y=0;y < height; y++)
		{
			for(int x=0;x<width; x++)
			{
				pixels[PixelIndex(x,y)] = bm.getPixel(x, y);
			}
		}
	}
	
	private int GetGreyLevel(int pixel, float intensity)
	{
		float red = Color.red(pixel);
		float green = Color.green(pixel);
		float blue = Color.blue(pixel);
		float parcial = red + green + blue;
		parcial = (float)(parcial / 3.0);
		int gray = (int)(parcial * intensity);
		if(gray > 255)
		{
			gray = 255;
		}
		return gray;
	}
	
	private void ConvertToMonochromeSteinbertDithering(float intensity)
	{
		int[][] levelmap = new int[width][height];
		for(int y=0;y<height;y++)
		{
			if((y & 1)== 0)
			{
				for(int x = 0; x<width; x++)
				{
					int pixel = pixels[PixelIndex(x, y)];
					levelmap[x][y] += 255 - GetGreyLevel(pixel, intensity);
					if(levelmap[x][y] >= 255)
					{
						levelmap[x][y] -= 255;
						pixels[PixelIndex(x, y)] = Color.BLACK;
					}
					else
					{
							pixels[PixelIndex(x, y)] = Color.WHITE;
					}
					
					int sixteenthOfQuantError = levelmap[x][y] / 16;
					
					if(x < width - 1)
						levelmap[x+1][y] += sixteenthOfQuantError * 7;
					
					if(y < height - 1)
					{
						levelmap[x][y+1] += sixteenthOfQuantError * 5;
						
						if(x > 0)
							levelmap[x-1][y+1] += sixteenthOfQuantError * 3;
						if(x < width - 1)
							levelmap[x+1][y+1] += sixteenthOfQuantError;
					}
				}
			}
			else
			{
				for(int x = width - 1; x >= 0; x--)
				{
					int pixel = pixels[PixelIndex(x, y)];
					
					levelmap[x][y] += 255 - GetGreyLevel(pixel, intensity);
					
					if(levelmap[x][y] >= 255)
					{
						levelmap[x][y] -= 255;
						pixels[PixelIndex(x, y)] = Color.BLACK;
					}
					else
					{
						pixels[PixelIndex(x, y)] = Color.WHITE;
					}
					
					int sixteenthOfQuantError = levelmap[x][y] / 16;
					
					if(x > 0)
						levelmap[x - 1][y] += sixteenthOfQuantError * 7;
					
					if(y < height - 1)
					{
						levelmap[x][y+1] += sixteenthOfQuantError * 5;
						
						if(x < width - 1)
							levelmap[x+1][y+1] += sixteenthOfQuantError * 3;
						
						if(x > 0)
							levelmap[x-1][y+1] += sixteenthOfQuantError;
					}
				} 
			}
		}
	}

	public byte[] getImageEscPosDataForPrinting()
	{
		if(imageData != null)
		{
			return imageData;
		}
		
		if(dithering)
		{
			ConvertToMonochromeSteinbertDithering((float)1.5);
		}
		
		int w = width / 8;
		if((width % 8) != 0)
			w++;
		int mWidth = w * 8;
		
		int idx;
		int byteWidth = mWidth / 8;
		byte[] data;
		ArrayList<Byte> someData = new ArrayList<Byte>();
		
//		Byte[] beginingBytes = {0x1b, 0x40, 0x1b, 0x58, 0x32, 0x18};
		Byte[] beginingBytes = {0x1b, 0x40,    // ESC @
				                0x1b, 0x4c,    // ESC L (Start Page mode) 		/* for smooth printing by mobile printer */
				                0x1b, 0x57,    // ESC W xL xH yL yH dxL dxH dyL dyH (Setting of page mode printable area)
				                0x00, 0x00, 0x00, 0x00,
				                (byte) (mWidth % 256), (byte) (mWidth / 256),
				                (byte) ((height + 40) % 256), (byte) ((height + 40) / 256),
				                0x1b, 0x58, 0x32, 0x18};    // ESC X 2 n
		for(int count=0;count<beginingBytes.length; count++)
		{
			someData.add(beginingBytes[count]);
		}

		int totalRowCount = 0;
		
		for(int z=0; z< (height - (height % 24))/24; z++)
		{
			Byte[] imagestarting = { 0x1b, 0x58, 0x34, 0, 24 };
			imagestarting[3] = (byte)byteWidth;
			
			for(int count=0;count<imagestarting.length;count++)
			{
				someData.add(imagestarting[count]);
			}
			
			idx = 0;
			data = new byte[byteWidth * 24];
			
			for(int y=0; y<24; y++)
			{
				for(int x = 0; x<byteWidth; x++)
				{
					int bits = 8;
					if((x == (byteWidth -1)) && (width < mWidth))
					    bits = 8 - (mWidth - width);
					
					//int xoffset = x * 8;
					for(int xbit = 0; xbit<bits; xbit++)
					{
						
						int pixel = pixels[PixelIndex(x * 8 + xbit, totalRowCount)];
						if(pixelBrightness(Color.red(pixel), Color.green(pixel), Color.blue(pixel)) < 127) 
							data[idx] = (byte)(data[idx] | (byte)(0x01 << (7 - xbit)));
					}
					idx++;
				}
				totalRowCount++;
			}
			for(int count=0;count<data.length; count++)
			{
				someData.add(data[count]);
			}
			byte[] imageData2 = {0x1b, 0x58, 0x32, 0x18};
			for(int count=0;count<imageData2.length; count++)
			{
				someData.add(imageData2[count]);
			}
		}
			
		if(height % 24 > 0)
		{
			byte[] imageData3 = {0x1b, 0x58, 0x34, 0, 0};
			imageData3[3] = (byte)byteWidth;
			imageData3[4] = 24;
			for(int count=0;count<imageData3.length; count++)
			{
				someData.add(imageData3[count]);
			}
			int idx2 = 0;
			data = new byte[byteWidth * 24];
			for(int y = 0; y< 24; y++)
			{
				for(int x = 0; x<byteWidth; x++)
				{
					int bits = 8;
					if((x == (byteWidth - 1)) && (width < mWidth))
						bits = 8 - (mWidth - width);
					
					//int xoffset = x * 8;
					for(int xbit = 0; xbit<bits; xbit++)
					{
						int pixel;
						if(totalRowCount < height)
						{
							pixel = pixels[PixelIndex(xbit + x * 8, totalRowCount)];
						}
						else 
						{
							pixel = Color.WHITE;
						}

						if(pixelBrightness(Color.red(pixel), Color.green(pixel), Color.blue(pixel)) < 127)
							data[idx2] = (byte)(data[idx2] | (0x01 << (7 - xbit)));
					}
					idx2++;
				}
				totalRowCount++;
			}
			for(int count=0;count<data.length;count++)
			{
				someData.add(data[count]);
			}
			
			byte[] imageData4 = {0x1b, 0x58, 0x32, 0x18};
			for(int count=0;count<imageData4.length;count++)
			{
				someData.add(imageData4[count]);
			}
		}

		byte[] imageData5 = {0x0c,    // FF (printing of page mode and return printing of standard mode) /* for smooth printing by mobile printer */
				             0x1b, 0x4A, 0x28};
		for(int count=0;count<imageData5.length;count++)
		{
			someData.add(imageData5[count]);
		}
		
		imageData = new byte[someData.size()];
		for(int count=0;count<someData.size(); count++)
		{
			imageData[count] = someData.get(count);
		}

		return imageData;  
	}

}
