package com.senellart.pierre.pdfenrichment;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

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
import org.grobid.core.data.BibDataSet;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.Date;
import org.grobid.core.data.Person;
import org.grobid.core.engines.Engine;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.utilities.GrobidProperties;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class PDFEnrichment extends PDFTextStripper {
  private Engine grobid;
  Map<String,String> refTitle2Url = new HashMap<String,String>();

  PDFEnrichment() throws IOException
  {
    super();

    GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList("lib/grobid-home"));
    GrobidProperties.getInstance(grobidHomeFinder);
    grobid = GrobidFactory.getInstance().createEngine();
  }

  public static final PDColor CYAN = new PDColor(new float[] { 0, 1, 1 },
      PDDeviceRGB.INSTANCE);

  public static final PDColor RED = new PDColor(new float[] { 1, 0, 0 },
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

  static String normalizeString(String s)
  {
    // Very rough implementation for now
    return s.replaceAll("[^a-zA-Z]", "");
  }

  protected List<Word> words;

  Pattern regex = Pattern
    .compile("^(ht|f)tps?://[a-zA-Z0-9][a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}(:[0-9]{1,5})?(/([a-zA-Z0-9.,;:\\?'+&%$#=~_()-]+)?)*$");

  public static void main(String[] args) throws IOException {
    if(args.length!=2) {
      System.err.println("Usage: java " + PDFEnrichment.class.getName() + " input.pdf output.pdf");
      System.exit(1);
    }

    File input = new File(args[0]);
    File output = new File(args[1]); 


    PDDocument document = null;
    try {
      document = PDDocument.load(input);
      PDFEnrichment stripper = new PDFEnrichment();

      stripper.extractReferences(input);

      stripper.setSortByPosition(true);

      stripper.writeText(document, new OutputStreamWriter(
            new ByteArrayOutputStream()));

      document.save(output);
    } finally {
      if (document != null) {
        document.close();
      }
    }
  }

  private void addLink(PDPage page, List<PDRectangle> rectangles, String url, PDColor color) throws IOException {
    List<PDAnnotation> annotations = page.getAnnotations();

    PDActionURI act = new PDActionURI();
    act.setURI(url);

    for(PDRectangle rect : rectangles) {
      PDAnnotationLink ann = new PDAnnotationLink();
      ann.setRectangle(rect);
      ann.setColor(color);
      ann.setAction(act);
      annotations.add(ann);
    }
  }

  @Override
  public void processPage(PDPage page) throws IOException {
    words = new ArrayList<Word>();
    super.processPage(page);

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
          ArrayList<PDRectangle> list = new ArrayList<PDRectangle>();

          for (int k = i; k < j; ++k)
            list.add(words.get(k).getRect());

          addLink(page, list, s, CYAN);
          break;
        }
      }

      for (int j = i+1; j < words.size() && j < i+100 && words.get(j).getFontName().equals(fontName); ++j)
      {
        String s = "";
        for (int k = i; k < j; ++k) {
          s = s + words.get(k).getString();
        }

        s = normalizeString(s);

        if(refTitle2Url.containsKey(normalizeString(s))) {
          List<PDRectangle> list = new ArrayList<PDRectangle>();

          for(int start = i ; start < j;) {
            float miny = words.get(start).getRect().getLowerLeftY();
            float maxy = words.get(start).getRect().getUpperRightY();
            int k;

            for (k = start + 1; k < j
                && words.get(k).getRect().getLowerLeftX() >= words.get(k - 1).getRect().getLowerLeftX()
                + words.get(k - 1).getRect().getWidth() / 2; ++k) {
              if(miny>words.get(k).getRect().getLowerLeftY())
                miny=words.get(k).getRect().getLowerLeftY();
              if(maxy<words.get(k).getRect().getUpperRightY())
                maxy=words.get(k).getRect().getUpperRightY();

                }

            list.add(new PDRectangle(
                  words.get(start).getRect().getLowerLeftX(),
                  miny,
                  words.get(k-1).getRect().getUpperRightX()-words.get(start).getRect().getLowerLeftX(),
                  maxy-miny)
                );

            start = k;
          }

          addLink(page, list, refTitle2Url.get(s), RED);

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

  private JSONObject disseminQuery(JSONObject query) throws IOException, ParseException
  {
    final String url = "https://dissem.in/api/query";

    URL obj = null;

    try {
      obj = new URL(url);
    } catch(MalformedURLException e) {
      // Cannot happen, constant URL
    }

    HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

    con.setRequestMethod("POST");			
    con.setRequestProperty("Content-Type", "application/json");

    // Send post request
    con.setDoOutput(true);
    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
    wr.write(query.toJSONString().getBytes("utf-8"));
    wr.flush();
    wr.close();

    if(con.getResponseCode()==404)
      return null;

    JSONParser parser = new JSONParser();
    return (JSONObject) parser.parse(new InputStreamReader(con.getInputStream()));
  }

  @SuppressWarnings({ "unchecked"})
    private void extractReferences(File pdfFile) {
      List<BibDataSet> citations = grobid.processReferences(pdfFile, 0);
      for (BibDataSet bib : citations) {
        BiblioItem b = bib.getResBib();
        if(b==null || b.getTitle()==null || b.getFullAuthors()==null || b.getNormalizedPublicationDate()==null) {
          continue;
        }

        JSONObject json = new JSONObject();

        json.put("title", b.getTitle());

        Date d = b.getNormalizedPublicationDate();

        String date = d.getYearString();
        if(d.getMonth()!=-1) {
          date+=d.getMonth()<10?"-0":"-";
          date+=d.getMonth();
        } else {
          date+="-01";
        }

        if(d.getDay()!=-1) {
          date+=d.getDay()<10?"-0":"-";
          date+=d.getDay();
        } else {
          date+="-01";
        }

        json.put("date", date);

        ArrayList<JSONObject> authors = new ArrayList<JSONObject>();

        for(Person author : b.getFullAuthors()) {       		
          JSONObject o = new JSONObject();
          if(author.getFirstName()!=null)
            o.put("first", author.getFirstName());
          else
            o.put("first","");
          if(author.getLastName()!=null)
            o.put("last", author.getLastName());
          else
            o.put("last", "");

          authors.add(o);
        }

        json.put("authors", authors);

        JSONObject result = null;

        try {
          result = disseminQuery(json);
        } catch(IOException e) {
          System.err.println(json);
          e.printStackTrace();
        } catch(ParseException e) {
          System.err.println(json);
          e.printStackTrace();
        }

        if(result == null || !result.containsKey("paper")) {
          System.err.println(b.getTitle()+": no result on Dissemin");
          continue;
        }

        JSONObject p = (JSONObject) result.get("paper");

        String url = null;

        if(p.containsKey("pdf_url")) {
          url = (String) p.get("pdf_url");
        } else if(p.containsKey("records")) {
          JSONArray records = (JSONArray) p.get("records");

          for(Object o : records) {
            JSONObject record = (JSONObject) o;
            if(record.containsKey("pdf_url")) {
              url= (String) record.get("splash_url");
              break;
            } else if(record.containsKey("splash_url")) {
              url= (String) record.get("splash_url");
              break;
            }
          }
        }

        if(url==null) {
          System.err.println("No URL on dissemin");
          continue;
        }

        System.err.println(b.getTitle()+" -> "+url);
        refTitle2Url.put(normalizeString(b.getTitle()), url);
      }
    }
}
