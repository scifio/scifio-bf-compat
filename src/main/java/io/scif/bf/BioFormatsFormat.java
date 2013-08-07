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

package io.scif.bf;

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
import io.scif.HasFormat;
import io.scif.ImageMetadata;
import io.scif.MetaTable;
import io.scif.common.RandomAccessInputStreamWrapper;
import io.scif.io.RandomAccessInputStream;
import io.scif.util.FormatTools;

import java.io.IOException;
import java.util.ArrayList;

import loci.formats.ClassList;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import net.imglib2.display.ColorTable16;
import net.imglib2.display.ColorTable8;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;
import org.scijava.util.IntArray;

/**
 * Wraps an {@link ImageReader} in a SCIFIO {@link Format},
 * allowing proprietary Bio-Formats readers to be used in
 * SCIFIO-based applications.
 * 
 * @author Mark Hiner hinerm at gmail.com
 */
@Plugin(type = Format.class, priority = Priority.VERY_HIGH_PRIORITY)
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

  // -- Fields --

  /**
   * List of Bio-Formats reader classes, excluding the {@link #DO_NOT_CONVERT}
   * blacklist.
   */
  private final ClassList<IFormatReader> readerClasses;

  // -- Constructors --

  /**
   * Constructs a new Format with the default
   * list of reader classes from readers.txt.
   */
  public BioFormatsFormat() {
    readerClasses = compileReaderClasses();
    suffixes = createImageReader().getSuffixes();
  }

  // -- BioFormatsFormat API Methods --

  /** Creates a new Bio-Formats {@link ImageReader}. */
  public ImageReader createImageReader() {
    return new ImageReader(readerClasses);
  }

  // -- Format API Methods --

  public String getFormatName() {
    return "Bio-Formats Compatibility Format";
  }

  public String[] getSuffixes() {
    return suffixes;
  }

  // -- Nested Classes --

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
      for (int s = 0; s < reader.getSeriesCount(); s++) {
        add(convertMetadata(reader, s));
      }
    }

    @Override
    public void close(boolean fileOnly) throws IOException {
      super.close(fileOnly);
      if (reader != null) reader.close(fileOnly);
    }
  }

  public static class Checker extends AbstractChecker {

    // -- Checker API Methods --

    @Override
    public boolean isFormat(String name) {
      return createImageReader(this).isThisType(name);
    }

    @Override
    public boolean isFormat(String name, boolean open) {
      return createImageReader(this).isThisType(name, open);
    }

    @Override
    public boolean isFormat(RandomAccessInputStream stream) throws IOException {
      return createImageReader(this).isThisType(
        new RandomAccessInputStreamWrapper(stream));
    }

    @Override
    public boolean checkHeader(byte[] block) {
      return createImageReader(this).isThisType(block);
    }

  }

  public static class Parser extends AbstractParser<Metadata> {

    // -- Parser API Methods --

    @Override
    protected void typedParse(RandomAccessInputStream stream, Metadata meta)
      throws IOException, FormatException
    {
      try {
        ImageReader reader = createImageReader(this);
        meta.setReader(reader);

        reader.setId(stream.getFileName());
      }
      catch (loci.formats.FormatException e) {
        throw new FormatException(e);
      }
    }
  }

  public static class Reader extends ByteArrayReader<Metadata> {

    // -- Reader API Methods --

    public ByteArrayPlane openPlane(int imageIndex, int planeIndex,
      ByteArrayPlane plane, int x, int y, int w, int h)
      throws FormatException, IOException
    {
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

  /**
   * Compiles the list of Bio-Formats reader classes, excluding the
   * {@link #DO_NOT_CONVERT} blacklist.
   */
  private ClassList<IFormatReader> compileReaderClasses() {
    final ClassList<IFormatReader> targetClasses =
      new ClassList<IFormatReader>(IFormatReader.class);

    // add reader classes to the list, excluding the blacklist
    final ClassList<IFormatReader> defaultClasses =
      ImageReader.getDefaultReaderClasses();
    for (final Class<? extends IFormatReader> c : defaultClasses.getClasses()) {
      if (convert(c)) targetClasses.addClass(c);
    }

    return targetClasses;
  }

  /** Returns false if this reader class already exists in SCIFIO. */
  private boolean convert(Class<? extends IFormatReader> c) {
    for (String s : DO_NOT_CONVERT) {
      if (s.equals(c.getName())) return false;
    }
    return true;
  }

  /**
   * Creates a new Bio-Formats {@link ImageReader}. This static method takes a
   * {@link HasFormat} object as input, which is presumed to be one of the
   * static inner classes of the {@link BioFormatsFormat}, to work around typing
   * limitations in the SCIFIO component interface hierarchy.
   */
  private static ImageReader createImageReader(final HasFormat thing) {
    return ((BioFormatsFormat) thing.getFormat()).createImageReader();
  }

  /**
   * Constructs a SCIFIO {@link ImageMetadata} object from the {@code s}th
   * series of the given Bio-Formats {@link IFormatReader}.
   */
  private static ImageMetadata convertMetadata(IFormatReader reader, int s) {
    ImageMetadata imgMeta = new DefaultImageMetadata();
    reader.setSeries(s);

    final ArrayList<AxisType> axisTypes = new ArrayList<AxisType>();
    final IntArray axisLengths = new IntArray();

    // parse interleaved channel dimensions
    parseChannelDimensions(reader, true, axisTypes, axisLengths);

    // parse standard dimensions in dimensional order
    final String dimOrder = reader.getDimensionOrder().toUpperCase();
    for (int i = 0; i < dimOrder.length(); i++) {
      switch (dimOrder.charAt(i)) {
        case 'X':
          if (reader.getSizeX() <= 1) continue;
          axisTypes.add(Axes.X);
          axisLengths.add(reader.getSizeX());
          break;
        case 'Y':
          if (reader.getSizeY() <= 1) continue;
          axisTypes.add(Axes.Y);
          axisLengths.add(reader.getSizeY());
          break;
        case 'Z':
          if (reader.getSizeZ() <= 1) continue;
          axisTypes.add(Axes.Z);
          axisLengths.add(reader.getSizeZ());
          break;
        case 'C':
          // parse non-interleaved channel dimensions
          parseChannelDimensions(reader, false, axisTypes, axisLengths);
          break;
        case 'T':
          if (reader.getSizeT() <= 1) continue;
          axisTypes.add(Axes.TIME);
          axisLengths.add(reader.getSizeT());
          break;
      }
    }

    imgMeta.setAxisTypes(axisTypes.toArray(new AxisType[0]));
    imgMeta.setAxisLengths(axisLengths.copyArray());
    imgMeta.setRGB(reader.isRGB());

    imgMeta.setPlaneCount(reader.getImageCount());

    imgMeta.setThumbSizeX(reader.getThumbSizeX());
    imgMeta.setThumbSizeY(reader.getThumbSizeY());
    imgMeta.setPixelType(reader.getPixelType());

    int bpp = reader.getBitsPerPixel();
    int bitsPerPixel = bpp == 0 ?
      FormatTools.getBitsPerPixel(reader.getPixelType()) : bpp;
    imgMeta.setBitsPerPixel(bitsPerPixel);
    imgMeta.setOrderCertain(reader.isOrderCertain());
    imgMeta.setLittleEndian(reader.isLittleEndian());
    imgMeta.setInterleaved(reader.isInterleaved());
    imgMeta.setIndexed(reader.isIndexed());
    imgMeta.setFalseColor(reader.isFalseColor());
    imgMeta.setMetadataComplete(reader.isMetadataComplete());

    MetaTable table = new DefaultMetaTable(reader.getSeriesMetadata());

    imgMeta.setTable(table);
    imgMeta.setThumbnail(reader.isThumbnailSeries());

    return imgMeta;
  }

  private static void parseChannelDimensions(IFormatReader reader,
    boolean interleaved, ArrayList<AxisType> axisTypes, IntArray axisLengths)
  {
    final int[] cDimLengths = reader.getChannelDimLengths();
    final String[] cDimTypes = reader.getChannelDimTypes();
    for (int subC = 0; subC < cDimLengths.length; subC++) {
      if (cDimLengths[subC] <= 1) continue;
      if (interleaved != reader.isInterleaved(subC)) continue;
      axisTypes.add(Axes.get(cDimTypes[subC]));
      axisLengths.add(cDimLengths[subC]);
    }
  }

}
