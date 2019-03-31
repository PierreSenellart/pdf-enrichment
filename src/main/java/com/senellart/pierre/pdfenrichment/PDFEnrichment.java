package com.senellart.pierre.pdfenrichment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

public class PDFEnrichment extends PDFTextStripper {
	public static final PDColor CYAN = new PDColor(new float[] { 0, 1, 1 },
			PDDeviceRGB.INSTANCE);

	protected class Word {
		String string;
		PDRectangle rect;
		String fontName;

		Word(String string, PDRectangle rect, String fontName) {
			this.string = string;
			this.rect = rect;
			this.fontName = fontName;
		}

		public String getString() {
			return string;
		}

		public PDRectangle getRect() {
			return rect;
		}

		public String getFontName() {
			return fontName;
		}
	}

	protected List<Word> words;

	Pattern regex = Pattern
			.compile("^(ht|f)tps?://[a-zA-Z0-9][a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}(:[0-9]{1,5})?(/([a-zA-Z0-9.,;:\\?'+&%$#=~_()-]+)?)*$");

	public PDFEnrichment() throws IOException {
		super();
	}

	public static void main(String[] args) throws IOException {
		if(args.length!=2) {
			System.err.println("Usage: java " + PDFEnrichment.class.getName() + " input.pdf output.pdf");
			System.exit(1);
		}
		
		PDDocument document = null;
		try {
			document = PDDocument.load(new File(args[0]));
			PDFEnrichment stripper = new PDFEnrichment();
			stripper.setSortByPosition(true);

			stripper.writeText(document, new OutputStreamWriter(
				new ByteArrayOutputStream()));
			
			document.save(new File(args[1]));
		} finally {
			if (document != null) {
				document.close();
			}
		}
	}

	@Override
	public void processPage(PDPage page) throws IOException {
		words = new ArrayList<Word>();
		super.processPage(page);

		List<PDAnnotation> annotations = page.getAnnotations();

		for (int i = 0; i < words.size(); ++i) {
			String fontName = words.get(i).getFontName();

			int end;
			for (end = i + 1; end < words.size()
					&& words.get(end).getRect().getLowerLeftX() < words
							.get(end - 1).getRect().getLowerLeftX()
							+ words.get(end - 1).getRect().getWidth() / 2
					&& words.get(end).getFontName().equals(fontName); ++end) {
			}

			for (int j = end; j > i; --j) {
				String s = "";
				for (int k = i; k < j; ++k) {
					s = s + words.get(k).getString();
				}

				Matcher m = regex.matcher(s);
				if (m.matches()) {
					PDActionURI act = new PDActionURI();
					act.setURI(s);

					for (int k = i; k < j; ++k) {
						PDAnnotationLink ann = new PDAnnotationLink();
						ann.setRectangle(words.get(k).getRect());
						ann.setColor(CYAN);
						ann.setAction(act);
						annotations.add(ann);
					}

					if (s.contains("S00362ED"))
						return;
					break;
				}
			}
		}
	}

	private void writeStringInternal(String string,
			List<TextPosition> textPositions, String fontName)
			throws IOException {
		TextPosition initial = textPositions.get(0);
		float leftX = initial.getXDirAdj();
		float width = 0;
		float bottomY = initial.getYDirAdj() + initial.getHeightDir();
		float height = 0;

		for (TextPosition text : textPositions) {
			PDFont font = text.getFont();
			BoundingBox bbox = font.getBoundingBox();

			float yScaling = font.getFontMatrix().getScaleY()
					* text.getTextMatrix().getScaleY();

			width = width + text.getWidthDirAdj();
			bottomY = Math.min(bottomY,
					text.getYDirAdj() - bbox.getLowerLeftY() * yScaling);
			height = Math.max(height, bbox.getHeight() * yScaling);
		}

		words.add(new Word(string, new PDRectangle(leftX - 0.5f, initial
				.getPageHeight() - bottomY - 0.5f, width + 1, height + 1),
				fontName));
	}

	@Override
	protected void writeString(String string, List<TextPosition> textPositions)
			throws IOException {
		if (textPositions.isEmpty())
			return;

		// Split the string if different fonts are used
		String fontName = textPositions.get(0).getFont().getName();
		for (int i = 1; i < textPositions.size(); ++i) {
			String newFontName = textPositions.get(i).getFont().getName();
			if (!fontName.equals(newFontName)) {
				writeStringInternal(string.substring(0, i),
						textPositions.subList(0, i), fontName);
				string = string.substring(i);
				textPositions = textPositions.subList(i, textPositions.size());
				fontName = newFontName;
				i = 0;
			}
		}

		writeStringInternal(string, textPositions, fontName);
	}
}
