package imageprocessing;

import gui.OptionPane;
import imageprocessing.colors.ColorSpaces;
import imageprocessing.colors.GrayscaleImage;
import main.Picsi;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

public class EdgeDetection implements IImageProcessor {

    public float[][] hx = {
            {-3 / 32f, 0 / 32f, 3 / 32f},
            {-10 / 32f, 0 / 32f, 10 / 32f},
            {-3 / 32f, 0 / 32f, 3 / 32f},
    };

    public float[][] hy = {
            {-3 / 32f, -10 / 32f, -3 / 32f},
            {0 / 32f, 0 / 32f, 0 / 32f},
            {3 / 32f, 10 / 32f, 3 / 32f},
    };

    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_GRAY || imageType == Picsi.IMAGE_TYPE_RGB;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {

        Object[] operations = {"Partielle Ableitungen", "Kantenstärke", "Colour by Angle"};
        int mode = OptionPane.showOptionDialog("Offsetting", SWT.ICON_INFORMATION, operations, 1);

        if (imageType == Picsi.IMAGE_TYPE_RGB) {
            inData = GrayscaleImage.grayscale(inData);
        }

        ImageData out = mode == 2
                ? ImageProcessing.createImage(inData.width, inData.height, Picsi.IMAGE_TYPE_RGB)
                : ImageProcessing.createImage(inData.width, inData.height, Picsi.IMAGE_TYPE_GRAY);

        for (int x = 0; x < inData.width; x++) {
            for (int y = 0; y < inData.height; y++) {
                out.setPixel(x, y, applySobelFilter(x, y, inData, mode, out.palette));
            }
        }

        return out;
    }

    private int applySobelFilter(int x, int y, ImageData inData, int mode, PaletteData palette) {
        int gx = 0;
        int gy = 0;

        // Apply Sobel filter in the x and y directions
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (x > 0 && x < inData.width - 1 && y > 0 && y < inData.height - 1) {
                    int pixelValue = inData.getPixel(x + i, y + j); // Get pixel intensity (grayscale)

                    gx += pixelValue * hx[i + 1][j + 1]; // Convolution with hx
                    gy += pixelValue * hy[i + 1][j + 1]; // Convolution with hy
                }
            }
        }

        // Calculate the magnitude of the gradient
        int gradientMagnitude = mode == 0 ? gx + gy + 128 : (int) Math.sqrt(gx * gx + gy * gy);

        if (mode == 2) {
            double angle = Math.atan((float) gx / gy);
            double[] rgb = new double[3];
            double[] hsv = {angle / Math.PI * 180, 1, gradientMagnitude};
            ColorSpaces.hsv2rgb(hsv, rgb);

            return palette.getPixel(new RGB(
                    ImageProcessing.clamp8(rgb[0]),
                    ImageProcessing.clamp8(rgb[1]),
                    ImageProcessing.clamp8(rgb[2])
            ));
        }

        // Clamp the result to a valid grayscale value (0-255)
        return ImageProcessing.clamp8(gradientMagnitude);
    }
}
