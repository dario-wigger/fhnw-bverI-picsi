package imageprocessing;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;

/**
 * Region of Interest (ROI)
 *
 * @author Christoph Stamm
 *
 */
public class ROI {
	public ImageData m_imageData;
	public Rectangle m_rect;

	/**
	 * Create new ROI
	 * @param imageData image data
	 * @param roi region of interest
	 */
	public ROI(ImageData imageData, Rectangle roi) {
		m_imageData = imageData;
		m_rect = roi;
	}

	/**
	 * @return width of ROI
	 */
	public int getWidth() {
		return m_rect.width;
	}

	/**
	 * @return height of ROI
	 */
	public int getHeight() {
		return m_rect.height;
	}

	/**
	 * Get pixel at position (x,y)
	 * @param x x-coordinate in ROI coordinate system
	 * @param y y-coordinate in ROI coordinate system
	 * @return
	 */
	public int getPixel(int x, int y) {
		x += m_rect.x;
		y += m_rect.y;

		if (x >= m_imageData.width || y >= m_imageData.height) {
			throw new IndexOutOfBoundsException();
		}

		return m_imageData.getPixel(x, y);
	}

	/**
	 * Set pixel at position (x,y)
	 * @param x x-coordinate in ROI coordinate system
	 * @param y y-coordinate in ROI coordinate system
	 * @param val
	 */
	public void setPixel(int x, int y, int val) {
		x += m_rect.x;
		y += m_rect.y;

		if (x >= m_imageData.width || y >= m_imageData.height) {
			throw new IndexOutOfBoundsException();
		}

		m_imageData.setPixel(x, y, val);
	}

	/**
	 * Returns true if this ROI overlaps with r
	 * @param other another ROI
	 * @return
	 */
	public boolean overlaps(ROI other) {
		int thisXMin = m_rect.x;
		int thisXMax = m_rect.x + m_rect.width;
		int thisYMin = m_rect.y;
		int thisYMax = m_rect.y + m_rect.height;

		int otherXMin = other.m_rect.x;
		int otherXMax = other.m_rect.x + other.m_rect.width;
		int otherYMin = other.m_rect.y;
		int otherYMax = other.m_rect.y + other.m_rect.height;

		boolean xOverlaps = (otherXMin >= thisXMin && otherXMin <= thisXMax)
				|| (otherXMax >= thisXMin && otherXMax <= thisXMax);

		boolean yOverlaps = (otherYMin >= thisYMin && otherYMin <= thisYMax)
				|| (otherYMax >= thisYMin && otherYMax <= thisYMax);

		return xOverlaps && yOverlaps;
	}

}
