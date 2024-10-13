package imageprocessing.patternmatching;

import gui.OptionPane;
import gui.RectTracker;
import imageprocessing.IImageProcessor;
import imageprocessing.ImageProcessing;
import imageprocessing.ROI;

import java.util.ArrayList;

import main.Picsi;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import utils.BoundedPQ;
import utils.Parallel;

/**
 * Pattern matching based on correlation coefficient
 *
 * @author Christoph Stamm
 */
public class PatternMatching implements IImageProcessor {
    public static class PMResult implements Comparable<PMResult> {
        public ROI m_roi;
        public double m_cl;

        public PMResult(ROI roi, double cl) {
            m_roi = roi;
            m_cl = cl;
        }

        public int compareTo(PMResult pm) {
            if (m_cl < pm.m_cl) return -1;
            else if (m_cl > pm.m_cl) return 1;
            else return 0;
        }
    }

    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_GRAY;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        // let the user choose the operation
        Object[] operations = {"Pattern Matching", "PM with modified Pattern", "User defined Pattern"};
        int f = OptionPane.showOptionDialog("Pattern Matching Operation", SWT.ICON_INFORMATION, operations, 0);
        if (f < 0) return null;

        final int intensityOffset = -50;
        final int contrastFactor = 4;
        final boolean predefinedPattern = f < 2;

        // pattern region
        Rectangle pr = null;

        if (predefinedPattern) {
            pr = new Rectangle(200, 310, 70, 50);
        } else {
            // let the user choose the ROI using a tracker
            RectTracker rt = new RectTracker();
            // pr = rt.track(inData.width/2, inData.height/2, 70, 50);
            pr = rt.start(70, 50);
        }

        final int nResults = 10 * 10;    // search nResults best matches
        final ROI pattern = new ROI((f > 0) ? (ImageData) inData.clone() : inData, pr);
        final int pw = pattern.getWidth();
        final int ph = pattern.getHeight();

        // pre-processing
        if (f == 1) {
            // TODO: modify pattern contrast and intensity
            Parallel.For(0, ph, v -> {
                for (int u = 0; u < pw; u++) {
                    int val = pattern.getPixel(u, v);
                    val = ImageProcessing.clamp8(val * contrastFactor);
                    val = ImageProcessing.clamp8(val + intensityOffset);
                    pattern.setPixel(u, v, val);
                }
            });
        }

        // pattern matching
        BoundedPQ<PMResult> results = pm(inData, pattern, nResults);

        // create output
        ImageData outData = ImageProcessing.createImage(inData.width, inData.height, Picsi.IMAGE_TYPE_RGB);

        // copy inData to outData
        Parallel.For(0, inData.height, v -> {
            for (int u = 0; u < inData.width; u++) {
                RGB rgb = inData.palette.getRGB(inData.getPixel(u, v));
                outData.setPixel(u, v, outData.palette.getPixel(rgb));
            }
        });

        return createOutput(outData, results, nResults);
    }

    /**
     * Pattern matching based on correlation coefficient
     *
     * @param inData
     * @param pattern
     * @param nResults number of best results
     * @return results
     */
    public static BoundedPQ<PMResult> pm(ImageData inData, ROI pattern, int nResults) {
        final int patternWidth = pattern.getWidth();
        final int patternHeight = pattern.getHeight();
        final int size = patternHeight * patternWidth;
        BoundedPQ<PMResult> results = new BoundedPQ<>(nResults);

        // Pre-Calculations
        double meanPattern = 0;

        for (int j = 0; j < patternHeight; j++) {
            for (int i = 0; i < patternWidth; i++) {
                meanPattern += pattern.getPixel(i, j);
            }
        }
        meanPattern = meanPattern / size;

        double standardDeviationPattern = 0;

        for (int j = 0; j < patternHeight; j++) {
            for (int i = 0; i < patternWidth; i++) {
                standardDeviationPattern += Math.pow(pattern.getPixel(i, j) - meanPattern, 2);
            }
        }
        standardDeviationPattern = Math.sqrt(standardDeviationPattern / size);

        // calculation of correlation coefficient
        for (int xImage = 0; xImage < inData.width - patternWidth + 1; xImage++) {
            for (int yImage = 0; yImage < inData.height - patternHeight + 1; yImage++) {

                int intensityProduct = 0;
                int intensityImageSquared = 0;
                double intensityImageAverage = 0;

                for (int xPattern = 0; xPattern < patternWidth; xPattern++) {
                    for (int yPattern = 0; yPattern < patternHeight; yPattern++) {
                        int intensityImage = inData.getPixel(xImage + xPattern, yImage + yPattern);
                        int intensityPattern = pattern.getPixel(xPattern, yPattern);

                        intensityProduct += intensityImage * intensityPattern;
                        intensityImageAverage += intensityImage;
                        intensityImageSquared += Math.pow(intensityImage, 2);
                    }
                }

                intensityImageAverage = intensityImageAverage / size;
                double correlationCoefficient = (intensityProduct - size * intensityImageAverage * meanPattern) /
                        (Math.sqrt(intensityImageSquared - size * Math.pow(intensityImageAverage, 2)) * standardDeviationPattern * Math.sqrt(size));

                results.add(new PMResult(new ROI(inData, new Rectangle(xImage, yImage, patternWidth, patternHeight)), correlationCoefficient));
            }
        }

        return results;
    }

    /**
     * Show best matching results as rectangles in the input image
     *
     * @param outData  output image
     * @param pq
     * @param nResults
     * @return
     */
    private ImageData createOutput(ImageData outData, BoundedPQ<PMResult> pq, int nResults) {
        ArrayList<PMResult> results = new ArrayList<>();

        // create image and write text into image
        Display display = Picsi.s_shell.getDisplay();
        Image output = new Image(display, outData);
        GC gc = new GC(output);

        // set font
        gc.setForeground(new Color(display, 255, 0, 0)); // red
        gc.setBackground(new Color(display, 255, 255, 255)); // white
        gc.setFont(new Font(display, "Segoe UI", 8, 0));

        for (int i = 0; i < nResults; i++) {
            final PMResult pm = pq.removeMax();

            if (pm != null) {
                int j = 0;
                while (j < results.size() && !pm.m_roi.overlaps(results.get(j).m_roi)) j++;
                if (j == results.size()) {
                    final Rectangle r = pm.m_roi.m_rect;

                    results.add(pm);

                    gc.drawRectangle(r);
                    gc.drawText(String.format("%.2f", pm.m_cl), r.x, r.y + r.height, true);
                }
            }
        }

        gc.dispose();

        outData = output.getImageData();
        output.dispose();
        return outData;
    }

}
