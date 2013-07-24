/*
 * #%L
 * SCIFIO Bio-Formats compatibility format.
 * %%
 * Copyright (C) 2013 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package io.scif.compat;

import io.scif.AbstractChecker;
import io.scif.AbstractFormat;
import io.scif.AbstractMetadata;
import io.scif.AbstractParser;
import io.scif.ByteArrayPlane;
import io.scif.ByteArrayReader;
import io.scif.DefaultImageMetadata;
import io.scif.DefaultMetaTable;
import io.scif.Format;
import io.scif.FormatException;
import io.scif.ImageMetadata;
import io.scif.MetaTable;
import io.scif.io.RandomAccessInputStream;
import io.scif.util.FormatTools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import loci.formats.ClassList;
import loci.formats.CoreMetadata;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import net.imglib2.display.ColorTable16;
import net.imglib2.display.ColorTable8;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;

/**
 * Wraps an {@link ImageReader} in a SCIFIO {@link Format},
 * allowing proprietary Bio-Formats readers to be used in
 * SCIFIO-based applications.
 * 
 * @author Mark Hiner hinerm at gmail.com
 */
@Plugin(type = BioFormatsFormat.class, priority = Priority.VERY_HIGH_PRIORITY)
public class BioFormatsFormat extends AbstractFormat {

  // -- Constants --

  /** List of classes already converted to SCIFIO. */
  public static final String[] DO_NOT_CONVERT = new String[]{
    "loci.formats.in.APNGReader",
    "loci.formats.in.AVIReader",
    "loci.formats.in.BMPReader",
    "loci.formats.in.DicomReader",
    "loci.formats.in.FakeReader",
    "loci.formats.in.FitsReader",
    "loci.formats.in.GIFReader",
    "loci.formats.in.ICSReader",
    "loci.formats.in.JPEG2000Reader",
    "loci.formats.in.MicromanagerReader",
    "loci.formats.in.MinimalTiffReader",
    "loci.formats.in.MNGReader",
    "loci.formats.in.NRRDReader",
    "loci.formats.in.OBFReader",
    "loci.formats.in.OMETiffReader",
    "loci.formats.in.OMEXMLReader",
    "loci.formats.in.PCXReader",
    "loci.formats.in.PGMReader",
    "loci.formats.in.PictReader",
    "loci.formats.in.QTReader",
    "loci.formats.in.SCIFIOReader",
    "loci.formats.in.TextReader",
    "loci.formats.in.TiffDelegateReader",
    "loci.formats.in.ZipReader"
  };

  // -- Constructors --

  /**
   * Constructs a new Format with the default
   * list of reader classes from readers.txt.
   */
  public BioFormatsFormat() {

    try {
      IFormatReader reader = getReader(getClass());
      suffixes = reader.getSuffixes();
      reader.close();
    }
    catch (IOException e) {
      LOGGER.error("Error closing Bio-Formats ImageReader", e);
    }
    catch (FormatException e) {
      LOGGER.error("Error creating IFormatReader" ,e);
    }
  }

  // -- Format API Methods --

  public String getFormatName() {
    return "Bio-Formats Compatibility Format";
  }

  public String[] getSuffixes() {
    return suffixes;
  }

  // -- Nested Classes --

  /**
   * @author Mark Hiner hinerm at gmail.com
   *
   */
  public static class Metadata extends AbstractMetadata {

    // -- Fields --

    private IFormatReader reader;

    // -- BioFormatsFormatMetadata methods --

    // -- Getters and Setters --

    public IFormatReader getReader() {
      return reader;
    }

    public void setReader(IFormatReader reader) {
      this.reader = reader;
    }

    // -- Metadata API Methods --

    public void populateImageMetadata() {
      for (CoreMetadata coreMeta : reader.getCoreMetadata()) {
        add(convertCoreMetadata(coreMeta));
      }
    }

    @Override
    public void close(boolean fileOnly) throws IOException {
      super.close(fileOnly);
      if (reader != null) reader.close(fileOnly);
    }
  }

  /**
   * @author Mark Hiner hinerm at gmail.com
   *
   */
  public static class Checker extends AbstractChecker {

    // -- Checker API Methods --

    public boolean isFormat(String name) {
      boolean isFormat = false;

      try {
        ImageReader reader = getReader(getClass());
        isFormat = reader.isThisType(name);
        reader.close();
      }
      catch (IOException e) {
        LOGGER.error("Error closing Bio-Formats ImageReader");
      }
      catch (FormatException e) {
        LOGGER.error("Error creating IFormatReader" ,e);
      }
      return isFormat;
    }

    public boolean isFormat(String name, boolean open) {
      boolean isFormat = false;

      try {
        ImageReader reader = getReader(getClass());
        isFormat = reader.isThisType(name, open);
        reader.close();
      }
      catch (IOException e) {
        LOGGER.error("Error closing Bio-Formats ImageReader");
      }
      catch (FormatException e) {
        LOGGER.error("Error creating IFormatReader" ,e);
      }
      return isFormat;
    }

    public boolean isFormat(RandomAccessInputStream stream) throws IOException {
      boolean isFormat = false;

      try {
        ImageReader reader = getReader(getClass());
        isFormat = reader.isThisType(new RandomAccessInputStreamWrapper(stream));
        reader.close();
      }
      catch (FormatException e) {
        LOGGER.error("Error creating IFormatReader" ,e);
      }

      return isFormat;
    }

    public boolean checkHeader(byte[] block) {
      boolean isFormat = false;
      try {
        ImageReader reader = getReader(getClass());
        isFormat = reader.isThisType(block);
        reader.close();
      }
      catch (IOException e) {
        LOGGER.error("Error closing Bio-Formats ImageReader");
      }
      catch (FormatException e) {
        LOGGER.error("Error creating IFormatReader" ,e);
      }
      return isFormat;
    }

  }

  /**
   * @author Mark Hiner hinerm at gmail.com
   *
   */
  public static class Parser extends AbstractParser<Metadata> {

    // -- Parser API Methods --

    @Override
    protected void typedParse(RandomAccessInputStream stream, Metadata meta)
        throws IOException, FormatException {
      try {
        ImageReader reader = getReader(getClass());
        meta.setReader(reader);

        reader.setId(stream.getFileName());
      }
      catch (loci.formats.FormatException e) {
        throw new FormatException(e);
      }
    }
  }

  /**
   * @author Mark Hiner hinerm at gmail.com
   *
   */
  public static class Reader extends ByteArrayReader<Metadata> {

    // -- Reader API Methods --

    public ByteArrayPlane openPlane(int imageIndex, int planeIndex,
        ByteArrayPlane plane, int x, int y, int w, int h)
            throws FormatException, IOException {
      IFormatReader reader = getMetadata().getReader();
      reader.setSeries(imageIndex);
      try {
        reader.openBytes(planeIndex, plane.getBytes(), x, y, w, h);

        if (reader.get8BitLookupTable() != null) {
          plane.setColorTable(new ColorTable8(reader.get8BitLookupTable()));
        }
        else if (reader.get16BitLookupTable() != null) {
          plane.setColorTable(new ColorTable16(reader.get16BitLookupTable()));
        }
      }
      catch (loci.formats.FormatException e) {
        throw new FormatException(e);
      }

      return plane;
    }

  }

  // -- Helper methods --

  // Creates an IFormatReader
  private static ImageReader getReader(Class<?> klass) throws FormatException {
    ClassList<IFormatReader> readerClasses = new ClassList<IFormatReader>(IFormatReader.class);

    InputStream is = klass.getResourceAsStream("/loci/formats/readers.txt");
    BufferedReader bis = new BufferedReader(new InputStreamReader(is));

    String line = null;
    try {
      while((line = bis.readLine()) != null) {
        if (line.length() > 0 && line.charAt(0) != '#') {
          String className = line.split("#")[0].trim();
          if (convert(className)) {
            @SuppressWarnings("unchecked")
            Class<? extends IFormatReader> readerClass = (Class<? extends IFormatReader>) Class.forName(className);
            readerClasses.addClass(readerClass );
          }
        }
      }
    }
    catch (IOException e) {
      throw new FormatException(e);
    }
    catch (ClassNotFoundException e) {
      throw new FormatException(e);
    }

    ImageReader r = new ImageReader(readerClasses);
    return r;
  }

  /** Returns false if this reader class already exists in SCIFIO. */
  private static boolean convert(String className) {
    for (String s : DO_NOT_CONVERT) {
      if (s.equals(className)) return false;
    }
    return true;
  }

  private static ImageMetadata convertCoreMetadata(CoreMetadata core) {
    ImageMetadata imgMeta = new DefaultImageMetadata();

    int[] axisLengths = new int[5];
    AxisType[] axisTypes = new AxisType[5];

    int planeCount = 1;

    for(int i = 0; i < core.dimensionOrder.length(); i++) {
      switch(core.dimensionOrder.toUpperCase().charAt(i)) {
        case 'X':
          axisLengths[i] = core.sizeX;
          axisTypes[i] = Axes.X;
          break;
        case 'Y':
          axisLengths[i] = core.sizeY;
          axisTypes[i] = Axes.Y;
          break;
        case 'Z':
          axisLengths[i] = core.sizeZ;
          axisTypes[i] = Axes.Z;
          planeCount *= core.sizeZ;
          break;
        case 'C':
          axisLengths[i] = core.sizeC;
          axisTypes[i] = Axes.CHANNEL;
          planeCount *= core.sizeC;
          break;
        case 'T':
          axisLengths[i] = core.sizeT;
          axisTypes[i] = Axes.TIME;
          planeCount *= core.sizeT;
          break;
      }
    }

    imgMeta.setAxisTypes(axisTypes);
    imgMeta.setAxisLengths(axisLengths);
    imgMeta.setRGB(core.rgb);

    if (core.rgb) planeCount /= 3;

    imgMeta.setPlaneCount(planeCount);

    imgMeta.setThumbSizeX(core.thumbSizeX);
    imgMeta.setThumbSizeY(core.thumbSizeY);
    imgMeta.setPixelType(core.pixelType);

    int bitsPerPixel = core.bitsPerPixel == 0 ?
      FormatTools.getBitsPerPixel(core.pixelType) : core.bitsPerPixel;
    imgMeta.setBitsPerPixel(bitsPerPixel);
    imgMeta.setChannelLengths(core.cLengths);
    imgMeta.setChannelTypes(core.cTypes);
    imgMeta.setOrderCertain(core.orderCertain);
    imgMeta.setLittleEndian(core.littleEndian);
    imgMeta.setInterleaved(core.interleaved);
    imgMeta.setIndexed(core.indexed);
    imgMeta.setFalseColor(core.falseColor);
    imgMeta.setMetadataComplete(core.metadataComplete);

    MetaTable table = new DefaultMetaTable(core.seriesMetadata);

    imgMeta.setTable(table);
    imgMeta.setThumbnail(core.thumbnail);

    return imgMeta;
  }

}
