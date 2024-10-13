package files;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.BitSet;
import java.util.PriorityQueue;

import javax.swing.JTextArea;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import imageprocessing.ImageProcessing;

/**
 * Huffman file format and codec
 * @author Christoph Stamm
 *
 */
public class Huffman implements IImageFile {
	static class Node implements Comparable<Node>, Serializable {
		static final long serialVersionUID = 1;
		protected transient double m_p; 	// probability (not needed during decoding)
		protected transient long m_code;	// binary code; maximum 64 bits  (not needed during decoding)
		protected transient byte m_codeLen;	// binary code length (not needed during decoding)
		protected Node m_left, m_right;		// children

		public Node(double p) {
			m_p = p;
		}

		public Node(Node left, Node right) {
			m_left = left;
			m_right = right;
			m_p = left.m_p + right.m_p;
		}

		public double getProbability() {
			return m_p;
		}

		public long getCode() {
			return m_code;
		}

		public byte getCodeLen() {
			return m_codeLen;
		}

		public boolean isInnerNode() {
			return m_left != null;
		}

		public int compareTo(Node v) {
			if (v != null) {
				if (m_p < v.m_p) {
					return -1;
				} else if (m_p == v.m_p) {
					return 0;
				} else {
					return 1;
				}
			} else {
				return -1;
			}
		}

		public void setCode(long code, int codeLen) {
			m_code = code;
			m_codeLen = (byte)codeLen;

			if (m_left != null) {
				// 0-Bit
				m_left.setCode(code << 1, codeLen + 1);
			}
			if (m_right != null) {
				// 1-Bit
				m_right.setCode((code << 1) + 1, codeLen + 1);
			}
		}

		public Node decodeBit(boolean bit) {
			return (bit) ? m_right : m_left;
		}
	}

	static class Leaf extends Node {
		static final long serialVersionUID = 1;
		private byte m_intensity;	// pixel intensity used during decoding

		public Leaf(double p, byte intensity) {
			super(p);
			m_intensity = intensity;
		}

		public byte getIntensity() {
			return m_intensity;
		}
	}

	@Override
	public ImageData read(String fileName) throws Exception {
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName));

		try {
			// read Header
			int width = in.readInt();
			int stride = ((width + 3)/4)*4;
			int height = in.readInt();

			// read code tree
			Node root = (Node)in.readObject();

			// read compressed data
			BitSet data = (BitSet)in.readObject();

			// close file
			in.close();

			// create palette
			RGB[] palette = new RGB[256];
			for (int i=0; i < palette.length; i++) {
				palette[i] = new RGB(i, i ,i);
			}

			// create image
			byte[] raw = new byte[stride*height];
			ImageData inData = new ImageData(width, height, 8, new PaletteData(palette), 4, raw); // stride is a multiple of 4 bytes

			// fill in image data
			Node node;
			int index = 0;

			for(int v = 0; v < inData.height; v++) {
				for(int u = 0; u < inData.width; u++) {
					node = root;
					while(node.isInnerNode()) {
						node = node.decodeBit(data.get(index++));
					}
					inData.setPixel(u, v, ((Leaf)node).getIntensity());
				}
			}
			return inData;
		} finally {
			in.close();
		}
}

	@Override
	public void save(String fileName, int fileType, ImageData imageData, int imageType) throws Exception {
		final int w = imageData.width;
		final int h = imageData.height;
		final int size = w*h;
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName));

		try {
			// huffman encoding
			int[] hist = ImageProcessing.histogram(imageData, 256);
			Leaf[] codes = new Leaf[hist.length];
			Node root = createHuffmanTree(hist, codes, size);
			BitSet data = encodeImage(imageData, codes);


			// TODO Header abspeichern
			out.writeInt(w);
			out.writeInt(h);

			// TODO Codebaum abspeichern
			out.writeObject(root);

			// TODO komprimierte Bilddaten abspeichern
			out.writeObject(data);

		} finally {
			out.close();
		}
	}

	/**
	 * Build code tree
	 * @param hist histogram of input image
	 * @param codes code table
	 * @param size number of pixels
	 * @return root node of code tree
	 */
	private Node createHuffmanTree(int[] hist, Leaf[] codes, int size) {
		PriorityQueue<Node> pq = new PriorityQueue<Node>(hist.length);

		// TODO Wahrscheinlichkeiten und Entropie berechnen und neue Blattknoten erzeugen (die Blattknoten sowohl in die Code-Tabelle als auch in die PQ einfügen)
		for(int i = 0; i < codes.length; i ++) {
			codes[i] = new Leaf((double) hist[i] / size, (byte) i);
			pq.add(codes[i]);
		}

		// TODO Mittlere Codelänge und Datei-Speicherbedarf mithilfe der Entropie abschätzen (Unter- und Obergrenze) und auf der Konsole ausgeben

		// TODO Codebaum aufbauen: Verwenden Sie die pq, um die zwei jeweils kleinsten Nodes zu holen
		Node root = null;

		while(pq.size() > 1) {
			Node leaf1 = pq.poll();
			Node leaf2 = pq.poll();
			root = new Node(leaf1, leaf2);
			pq.add(root);
		}

		// TODO Wurzelknoten holen und mittels Funktionsaufruf alle Codes rekursiv erzeugen
		if(root == null) {
			return null;
		}

		root.setCode(0, 0);

		// TODO Mittlere Codelänge und Datei-Speicherbedarf berechnen und auf der Konsole ausgeben

		return root;
	}

	/**
	 * Encode image data
	 * @param outData image data
	 * @param codes code table
	 * @return encoded data
	 */
	private BitSet encodeImage(ImageData outData, Leaf[] codes) {
		final int w = outData.width;
		final int h = outData.height;
		BitSet bs = new BitSet();

		// TODO alle Pixel der Reihe nach codieren und im bs abspeichern
		int index = 0;
		for(int y = 0; y < h; y++) {
			for(int x = 0; x < w; x ++) {
				int intensity = outData.getPixel(x, y);
				long code = codes[intensity].getCode();
				int length = codes[intensity].getCodeLen();

				for (int i = 1; i <= length; i++) {
					bs.set(index++, (code & (1 << (length - i))) > 0);
				}
			}
		}

		return bs;
	}

	@Override
	public void displayTextOfBinaryImage(ImageData imageData, JTextArea text) {
		text.append("P2");
		text.append("\n" + imageData.width + " " + imageData.height);
		text.append("\n255\n");
		PNM.writePGM(imageData, text, 255);
	}

	@Override
	public boolean isBinaryFormat() {
		return true;
	}

}
