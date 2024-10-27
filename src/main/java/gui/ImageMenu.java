package gui;

import imageprocessing.EdgeDetection;
import imageprocessing.bayerpattern.Debayering;
import imageprocessing.filter.GaussFilter;
import imageprocessing.filter.MedianFilter;
import imageprocessing.patternmatching.PatternMatching;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MenuItem;

import imageprocessing.Cropping;
import imageprocessing.colors.ChannelRGBA;
import imageprocessing.colors.Inverter;

/**
 * Image processing menu
 * @author Christoph Stamm
 *
 */
public class ImageMenu extends UserMenu {
	/**
	 * Registration of image operations
	 * @param item menu item
	 * @param views twin view
	 * @param mru MRU
	 */
	public ImageMenu(MenuItem item, TwinView views, MRU mru) {
		super(item, views, mru);

		// add(menuText, shortcut, instanceOfIImageProcessor)
		add("C&ropping\tCtrl+R", 								SWT.CTRL | 'R', new Cropping());
		add("&Invert\tF1", 										SWT.F1, 		new Inverter());

		UserMenu channels = addMenu("Channel");
		channels.add("R\tCtrl+1", 								SWT.CTRL | '1', new ChannelRGBA(0));
		channels.add("G\tCtrl+2", 								SWT.CTRL | '2', new ChannelRGBA(1));
		channels.add("B\tCtrl+3", 								SWT.CTRL | '3', new ChannelRGBA(2));
		channels.add("A\tCtrl+4", 								SWT.CTRL | '4', new ChannelRGBA(3));

		add("Debayering\tCtrl+D", 								SWT.CTRL | 'D', new Debayering());
		add("&Pattern Matching", 								SWT.CTRL | 'M', new PatternMatching());
		add("&Gauss Filter", 									SWT.CTRL | 'G', new GaussFilter());
		add("&Median Filter", 									SWT.CTRL | 'G', new MedianFilter());
		add("&Edge Detection", 								SWT.CTRL | 'E', new EdgeDetection());
		// TODO add here further image processing entries (they are inserted into the Image menu)
	}
}
