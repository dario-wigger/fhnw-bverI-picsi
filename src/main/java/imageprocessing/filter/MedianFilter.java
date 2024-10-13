package imageprocessing.filter;

import imageprocessing.IImageProcessor;
import main.Picsi;
import org.eclipse.swt.graphics.ImageData;

import java.util.Arrays;

public class MedianFilter implements IImageProcessor {

    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_GRAY;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        return applyMedianFilter(inData);
    }

    public static ImageData applyMedianFilter(ImageData inData) {
        final int size = 3*3;
        ImageData outData = (ImageData) inData.clone();
        int[] pixels = new int[size];

        for(int v = 0; v < inData.height; v++) {
            for(int u = 0; u < inData.width; u++) {
                int n = 0;

                for(int i = 0; i < size; i++) {
                    int p = getPixel(inData, u, v, i);
                    if(p >= 0) {
                        pixels[n++] = p;
                    }
                }

                Arrays.sort(pixels, 0, n);
                final int n2 = n/2;
                final int p = ((n & 1) == 1) ? pixels[n2] : (pixels[n2 - 1] + pixels[n2]);
                outData.setPixel(u, v, p);
            }
        }

        return outData;
    }

    private static int[] dx = {0,1,1,0,-1,-1,-1,0,1};
    private static int[] dy = {0,0,-1,-1,-1,0,1,1,1};
    private static int getPixel(ImageData inData, int u, int v, int i) {

        u += dx[i];
        if(u < 0 || u >= inData.width) {
            return -1;
        }

        v += dy[i];
        if(v < 0 || v >= inData.height) {
            return -1;
        }

        return inData.getPixel(u, v);
    }
}
