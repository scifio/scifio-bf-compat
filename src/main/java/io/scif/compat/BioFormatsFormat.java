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
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;

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
 *
 */
@Plugin(type = BioFormatsFormat.class, priority = Priority.VERY_HIGH_PRIORITY)
public class BioFormatsFormat extends AbstractFormat {

  // -- Constants --
  
  /**
   * List of classes already converted to SCIFIO
   */
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
    } catch (IOException e) {
      LOGGER.error("Error closing Bio-Formats ImageReader", e);
    } catch (FormatException e) {
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
      } catch (IOException e) {
        LOGGER.error("Error closing Bio-Formats ImageReader");
      } catch (FormatException e) {
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
      } catch (IOException e) {
        LOGGER.error("Error closing Bio-Formats ImageReader");
      } catch (FormatException e) {
        LOGGER.error("Error creating IFormatReader" ,e);
      }
      return isFormat;
    }

    public boolean isFormat(RandomAccessInputStream stream) throws IOException {
      boolean isFormat = false;

      try {
        ImageReader reader = getReader(getClass());
        isFormat =  reader.isThisType(new RandomAccessInputStreamWrapper(stream)); 
        reader.close();
      } catch (FormatException e) {
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
      } catch (IOException e) {
        LOGGER.error("Error closing Bio-Formats ImageReader");
      } catch (FormatException e) {
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
      } catch (loci.formats.FormatException e) {
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
      } catch (loci.formats.FormatException e) {
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
          if (!dontConvert(className)) {
            @SuppressWarnings("unchecked")
            Class<? extends IFormatReader> readerClass = (Class<? extends IFormatReader>) Class.forName(className);
            readerClasses.addClass(readerClass );
          }
        }
      }
    } catch (IOException e) {
      throw new FormatException(e);
    } catch (ClassNotFoundException e) {
      throw new FormatException(e);
    }
    
    ImageReader r = new ImageReader(readerClasses);
    return r;
  }
  
  // Returns true if this reader class already exists in SCIFIO
  private static boolean dontConvert(String className) {
    for (String s : DO_NOT_CONVERT) {
      if (s.equals(className)) return true;
    }
    return false;
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
  
  // -- Helper class --
  
  // Wrapper class to conver ome.scifio.io.RAIS to loci.common.RAIS
  private static class RandomAccessInputStreamWrapper extends loci.common.RandomAccessInputStream 
  implements DataInput {

    // -- Constants --

    // -- Fields --

    private io.scif.io.RandomAccessInputStream rais;

    // -- Constructors --

    /** Wrapper constructor. 
     * @throws IOException */
    public RandomAccessInputStreamWrapper(io.scif.io.RandomAccessInputStream rais) throws IOException {
      super((String)null);
      this.rais = rais;
    }

    // -- Wrapper API Methods --

    /* @see Wrapper#unwrap() */
    public io.scif.io.RandomAccessInputStream unwrap() {
      return rais;
    }

    // -- RandomAccessInputStream API methods --

    /**
     * Sets the native encoding of the stream.
     *
     * @see loci.common.Constants#ENCODING
     */
    public void setEncoding(String encoding) {
      unwrap().setEncoding(encoding);
    }

    /** Seeks to the given offset within the stream. */
    public void seek(long pos) throws IOException {
      unwrap().seek(pos);
    }

    /** Gets the number of bytes in the file. */
    public long length() throws IOException {
      return unwrap().length();
    }

    /**
     * Sets the length of the stream.
     * The new length must be less than the real length of the stream.
     * This allows us to work with a truncated view of a file, without modifying
     * the file itself.
     *
     * Passing in a negative value will reset the length to the stream's real length.
     */
    public void setLength(long newLength) throws IOException {
      unwrap().setLength(newLength);
    }

    /** Gets the current (absolute) file pointer. */
    public long getFilePointer() throws IOException {
      return unwrap().getFilePointer();
    }

    /** Closes the streams. */
    public void close() throws IOException {
      unwrap().close();
    }

    /** Sets the endianness of the stream. */
    public void order(boolean little) {
      unwrap().order(little);
    }

    /** Gets the endianness of the stream. */
    public boolean isLittleEndian() {
      return unwrap().isLittleEndian();
    }

    /**
     * Reads a string ending with one of the characters in the given string.
     *
     * @see #findString(String...)
     */
    public String readString(String lastChars) throws IOException {
      return unwrap().readString(lastChars);
    }

    /**
     * Reads a string ending with one of the given terminating substrings.
     *
     * @param terminators The strings for which to search.
     *
     * @return The string from the initial position through the end of the
     *   terminating sequence, or through the end of the stream if no
     *   terminating sequence is found.
     */
    public String findString(String... terminators) throws IOException {
      return unwrap().findString(terminators);
    }

    /**
     * Reads or skips a string ending with
     * one of the given terminating substrings.
     *
     * @param saveString Whether to collect the string from the current file
     *   pointer to the terminating bytes, and return it. If false, returns null.
     * @param terminators The strings for which to search.
     *
     * @throws IOException If saveString flag is set
     *   and the maximum search length (512 MB) is exceeded.
     *
     * @return The string from the initial position through the end of the
     *   terminating sequence, or through the end of the stream if no
     *   terminating sequence is found, or null if saveString flag is unset.
     */
    public String findString(boolean saveString, String... terminators)
        throws IOException
        {
      return unwrap().findString(saveString, terminators);
        }

    /**
     * Reads a string ending with one of the given terminating
     * substrings, using the specified block size for buffering.
     *
     * @param blockSize The block size to use when reading bytes in chunks.
     * @param terminators The strings for which to search.
     *
     * @return The string from the initial position through the end of the
     *   terminating sequence, or through the end of the stream if no
     *   terminating sequence is found.
     */
    public String findString(int blockSize, String... terminators)
        throws IOException
        {
      return unwrap().findString(blockSize, terminators);
        }

    /**
     * Reads or skips a string ending with one of the given terminating
     * substrings, using the specified block size for buffering.
     *
     * @param saveString Whether to collect the string from the current file
     *   pointer to the terminating bytes, and return it. If false, returns null.
     * @param blockSize The block size to use when reading bytes in chunks.
     * @param terminators The strings for which to search.
     *
     * @throws IOException If saveString flag is set
     *   and the maximum search length (512 MB) is exceeded.
     *
     * @return The string from the initial position through the end of the
     *   terminating sequence, or through the end of the stream if no
     *   terminating sequence is found, or null if saveString flag is unset.
     */
    public String findString(boolean saveString, int blockSize,
        String... terminators) throws IOException
        {
      return unwrap().findString(saveString, blockSize, terminators);
        }

    // -- DataInput API methods --

    /** Read an input byte and return true if the byte is nonzero. */
    public boolean readBoolean() throws IOException {
      return unwrap().readBoolean();
    }

    /** Read one byte and return it. */
    public byte readByte() throws IOException {
      return unwrap().readByte();
    }

    /** Read an input char. */
    public char readChar() throws IOException {
      return unwrap().readChar();
    }

    /** Read eight bytes and return a double value. */
    public double readDouble() throws IOException {
      return unwrap().readDouble();
    }

    /** Read four bytes and return a float value. */
    public float readFloat() throws IOException {
      return unwrap().readFloat();
    }

    /** Read four input bytes and return an int value. */
    public int readInt() throws IOException {
      return unwrap().readInt();
    }

    /** Read the next line of text from the input stream. */
    public String readLine() throws IOException {
      return unwrap().readLine();
    }

    /** Read a string of arbitrary length, terminated by a null char. */
    public String readCString() throws IOException {
      return unwrap().readCString();
    }

    /** Read a string of up to length n. */
    public String readString(int n) throws IOException {
      return unwrap().readString(n);
    }

    /** Read eight input bytes and return a long value. */
    public long readLong() throws IOException {
      return unwrap().readLong();
    }

    /** Read two input bytes and return a short value. */
    public short readShort() throws IOException {
      return unwrap().readShort();
    }

    /** Read an input byte and zero extend it appropriately. */
    public int readUnsignedByte() throws IOException {
      return unwrap().readUnsignedByte();
    }

    /** Read two bytes and return an int in the range 0 through 65535. */
    public int readUnsignedShort() throws IOException {
      return unwrap().readUnsignedShort();
    }

    /** Read a string that has been encoded using a modified UTF-8 format. */
    public String readUTF() throws IOException {
      return unwrap().readUTF();
    }

    /** Skip n bytes within the stream. */
    public int skipBytes(int n) throws IOException {
      return unwrap().skipBytes(n);
    }

    /** Read bytes from the stream into the given array. */
    public int read(byte[] array) throws IOException {
      return unwrap().read(array);
    }

    /**
     * Read n bytes from the stream into the given array at the specified offset.
     */
    public int read(byte[] array, int offset, int n) throws IOException {
      return unwrap().read(array, offset, n);
    }

    /** Read bytes from the stream into the given buffer. */
    public int read(ByteBuffer buf) throws IOException {
      return unwrap().read(buf);
    }

    /**
     * Read n bytes from the stream into the given buffer at the specified offset.
     */
    public int read(ByteBuffer buf, int offset, int n) throws IOException {
      return unwrap().read(buf, offset, n);
    }

    /** Read bytes from the stream into the given array. */
    public void readFully(byte[] array) throws IOException {
      unwrap().readFully(array);
    }

    /**
     * Read n bytes from the stream into the given array at the specified offset.
     */
    public void readFully(byte[] array, int offset, int n) throws IOException {
      unwrap().readFully(array, offset, n);
    }

    // -- InputStream API methods --

    public int read() throws IOException {
      return unwrap().read();
    }

    public int available() throws IOException {
      return unwrap().available();
    }

    public void mark(int readLimit) {
      unwrap().mark(readLimit);
    }

    public boolean markSupported() {
      return unwrap().markSupported();
    }

    public void reset() throws IOException {
      unwrap().reset();
    }
  }
}
