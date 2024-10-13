package imageprocessing.filter;

import gui.OptionPane;
import imageprocessing.IImageProcessor;
import imageprocessing.ImageProcessing;
import main.Picsi;
import org.eclipse.swt.graphics.ImageData;

public class GaussFilter implements IImageProcessor {

    private float[][] gaussFilter;

    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_GRAY;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {

        int size = OptionPane.showIntegerDialog("Filter Size [Default: 5]", 5);
        int sigma = OptionPane.showIntegerDialog("Sigma [Default: 1]", 1);
        gaussFilter = prepareGaussFilter(size, sigma);
        ImageData out = new ImageData(inData.width, inData.height, 8, inData.palette);

        for (int x = 0; x < inData.width; x++) {
            for (int y = 0; y < inData.height; y++) {
                out.setPixel(x, y, applyGaussFilter(x, y, inData));
            }
        }

        return out;
    }

    private static float[][] prepareGaussFilter(int size, int sigma) {

        float[][] filter = new float[size][size];

        int center = size / 2;
        float sigmaSquared = sigma * sigma;
        double normalizationFactor = 1 / (2 * Math.PI * sigmaSquared);
        float sum = 0;

        // calculate filter values
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int x = i - center;
                int y = j - center;
                filter[i][j] = (float) (normalizationFactor * Math.exp(-(x * x + y * y) / sigmaSquared));
                sum += filter[i][j];
            }
        }

        // normalization
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                filter[i][j] /= sum;
            }
        }

        return filter;
    }

    private int applyGaussFilter(int x, int y, ImageData inData) {

        int filterSize = gaussFilter.length;
        int filterHalf = filterSize / 2;
        float value = 0;

        for (int filterX = 0; filterX < filterSize; filterX++) {
            for (int filterY = 0; filterY < filterSize; filterY++) {
                int imageX = x + filterX - filterHalf;
                int imageY = y + filterY - filterHalf;

                if (imageX >= 0 && imageX < inData.width && imageY >= 0 && imageY < inData.height) {
                    value += inData.getPixel(imageX, imageY) * gaussFilter[filterX][filterY];
                }
            }
        }

        return ImageProcessing.clamp8(value);
    }
}
