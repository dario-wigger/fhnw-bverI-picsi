package imageprocessing.bayerpattern;

import main.Picsi;
import utils.Parallel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;

import gui.OptionPane;
import imageprocessing.IImageProcessor;
import imageprocessing.ImageProcessing;

/**
 * Debayering
 * @author Christoph Stamm
 *
 */
public class Debayering implements IImageProcessor {
	static final int Bypp = 3;

	private final static int[][] BAYER_PATTERN = new int[][] {
			{2, 1},
			{1, 0}
	};

	@Override
	public boolean isEnabled(int imageType) {
		return imageType == Picsi.IMAGE_TYPE_GRAY;
	}

	@Override
	public ImageData run(ImageData inData, int imageType) {
		Object[] outputTypes = { "Simple", "Good" };
		int ch = OptionPane.showOptionDialog("Debayering algorithms", SWT.ICON_QUESTION, outputTypes, 0);
		if (ch < 0) return null;

		// Debayering of raw input image
		if (ch == 0) return debayering1(inData);
		else return debayering2(inData);
	}

	/**
	 * @param inData raw data
	 * @return RGB image
	 */
	private ImageData debayering1(ImageData inData) {
		ImageData outData = ImageProcessing.createImage(inData.width, inData.height, Picsi.IMAGE_TYPE_RGB);

		Parallel.For(0, outData.height, y -> {

			for (int x=0; x < outData.width; x++) {

				int red = 0;
				int green = 0;
				int blue = 0;

				// do interpolation
				int pixelValue = inData.getPixel(x, y);

				// row % 2 == 0 && col % 2 == 0 --> Blue
				// row % 2 == 0 && col % 2 == 1 --> Green
				// row % 2 == 1 && col % 2 == 0 --> Green
				// row % 2 == 1 && col % 2 == 1 --> Red

				if(y % 2 == 0) {								// even row
					if(x % 2 == 0) {							// even column --> blue pixel
						red = interpolateSquare(inData, x, y);
						green = interpolateCross(inData, x, y);
						blue = pixelValue;
					} else {									// odd column --> green pixel
						red = interpolateTopAndBottom(inData, x, y);
						green = pixelValue;
						blue = interpolateLeftAndRight(inData, x, y);
					}
				} else {										// odd row
					if(x % 2 == 0) {							// even column --> green pixel
						red = interpolateLeftAndRight(inData, x, y);
						green = pixelValue;
						blue = interpolateTopAndBottom(inData, x, y);
					} else {									// odd column --> red pixel
						red = pixelValue;
						green = interpolateCross(inData, x, y);
						blue = interpolateSquare(inData, x, y);
					}
				}

				outData.setPixel(x, y, outData.palette.getPixel(new RGB(red, green, blue)));
			}
		});
		return outData;
	}

	private int interpolateLeftAndRight(ImageData inData, int x, int y) {

		int sum = 0;
		int count = 0;

		if(x > 0) {									// left
			sum += inData.getPixel(x - 1, y);
			count ++;
		}

		if(x < inData.width - 1) {					// right
			sum += inData.getPixel(x + 1, y);
			count ++;
		}

		return count > 0 ? sum / count : 0;
	}

	private int interpolateTopAndBottom(ImageData inData, int x, int y) {

		int sum = 0;
		int count = 0;

		if (y > 0) {								// top
			sum += inData.getPixel(x, y - 1);
			count ++;
		}

		if (y < inData.height - 1) {				// bottom
			sum += inData.getPixel(x, y + 1);
			count ++;
		}

		return count > 0 ? sum / count : 0;
	}

	private int interpolateCross(ImageData inData, int x, int y) {

		int sum = 0;
		int count = 0;

		if (y > 0) {								// top
			sum += inData.getPixel(x, y - 1);
			count ++;
		}

		if(y < inData.height - 1) {					// bottom
			sum += inData.getPixel(x, y + 1);
			count ++;
		}

		if(x > 0) {									// left
			sum += inData.getPixel(x - 1, y);
			count ++;
		}

		if (x < inData.width - 1) {					// right
			sum = inData.getPixel(x + 1, y);
			count ++;
		}

		return count > 0 ? sum / count : 0;
	}

	private int interpolateSquare(ImageData inData, int x, int y) {

		int sum = 0;
		int count = 0;

		if(x > 0 && y > 0) {									// top left
			sum += inData.getPixel(x - 1, y - 1);
			count ++;
		}

		if (x < inData.width - 1 && y > 0) {					// top right
			sum += inData.getPixel(x + 1, y - 1);
			count ++;
		}

		if (x > 0 && y < inData.height - 1) {					// bottom left
			sum += inData.getPixel(x - 1, y + 1);
			count ++;
		}

		if (x < inData.width - 1 && y < inData.height - 1) {	// bottom right
			sum += inData.getPixel(x + 1, y + 1);
			count ++;
		}

		return count > 0 ? sum / count : 0;
	}

	/**
	 * Advanced Debayering
	 * @param inData raw data
	 * @return RGB image
	 */
	private ImageData debayering2(ImageData inData) {
		ImageData outData = ImageProcessing.createImage(inData.width, inData.height, Picsi.IMAGE_TYPE_RGB);

		// interpolation of green channel
		Parallel.For(0, outData.height, v -> {
			RGB rgb = new RGB(0, 0, 0);

			for (int u=0; u < outData.width; u++) {
				outData.setPixel(u, v, outData.palette.getPixel(rgb));
			}
		});

		// interpolation of blue and red channels
		Parallel.For(0, outData.height, v -> {
			for (int u=0; u < outData.width; u++) {
				// outData.setPixel(u, v, outData.palette.getPixel(rgb));
			}
		});
		return outData;
	}

}
