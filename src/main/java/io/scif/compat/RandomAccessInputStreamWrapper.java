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

import java.io.IOException;
import java.nio.ByteBuffer;

/** Wrapper class to convert ome.scifio.io.RAIS to loci.common.RAIS. */
class RandomAccessInputStreamWrapper
  extends loci.common.RandomAccessInputStream
{

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
  @Override
  public void setEncoding(String encoding) {
    unwrap().setEncoding(encoding);
  }

  /** Seeks to the given offset within the stream. */
  @Override
  public void seek(long pos) throws IOException {
    unwrap().seek(pos);
  }

  /** Gets the number of bytes in the file. */
  @Override
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
  @Override
  public void setLength(long newLength) throws IOException {
    unwrap().setLength(newLength);
  }

  /** Gets the current (absolute) file pointer. */
  @Override
  public long getFilePointer() throws IOException {
    return unwrap().getFilePointer();
  }

  /** Closes the streams. */
  @Override
  public void close() throws IOException {
    unwrap().close();
  }

  /** Sets the endianness of the stream. */
  @Override
  public void order(boolean little) {
    unwrap().order(little);
  }

  /** Gets the endianness of the stream. */
  @Override
  public boolean isLittleEndian() {
    return unwrap().isLittleEndian();
  }

  /**
   * Reads a string ending with one of the characters in the given string.
   *
   * @see #findString(String...)
   */
  @Override
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
  @Override
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
  @Override
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
  @Override
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
  @Override
  public String findString(boolean saveString, int blockSize,
      String... terminators) throws IOException
      {
    return unwrap().findString(saveString, blockSize, terminators);
      }

  // -- DataInput API methods --

  /** Read an input byte and return true if the byte is nonzero. */
  @Override
  public boolean readBoolean() throws IOException {
    return unwrap().readBoolean();
  }

  /** Read one byte and return it. */
  @Override
  public byte readByte() throws IOException {
    return unwrap().readByte();
  }

  /** Read an input char. */
  @Override
  public char readChar() throws IOException {
    return unwrap().readChar();
  }

  /** Read eight bytes and return a double value. */
  @Override
  public double readDouble() throws IOException {
    return unwrap().readDouble();
  }

  /** Read four bytes and return a float value. */
  @Override
  public float readFloat() throws IOException {
    return unwrap().readFloat();
  }

  /** Read four input bytes and return an int value. */
  @Override
  public int readInt() throws IOException {
    return unwrap().readInt();
  }

  /** Read the next line of text from the input stream. */
  @Override
  public String readLine() throws IOException {
    return unwrap().readLine();
  }

  /** Read a string of arbitrary length, terminated by a null char. */
  @Override
  public String readCString() throws IOException {
    return unwrap().readCString();
  }

  /** Read a string of up to length n. */
  @Override
  public String readString(int n) throws IOException {
    return unwrap().readString(n);
  }

  /** Read eight input bytes and return a long value. */
  @Override
  public long readLong() throws IOException {
    return unwrap().readLong();
  }

  /** Read two input bytes and return a short value. */
  @Override
  public short readShort() throws IOException {
    return unwrap().readShort();
  }

  /** Read an input byte and zero extend it appropriately. */
  @Override
  public int readUnsignedByte() throws IOException {
    return unwrap().readUnsignedByte();
  }

  /** Read two bytes and return an int in the range 0 through 65535. */
  @Override
  public int readUnsignedShort() throws IOException {
    return unwrap().readUnsignedShort();
  }

  /** Read a string that has been encoded using a modified UTF-8 format. */
  @Override
  public String readUTF() throws IOException {
    return unwrap().readUTF();
  }

  /** Skip n bytes within the stream. */
  @Override
  public int skipBytes(int n) throws IOException {
    return unwrap().skipBytes(n);
  }

  /** Read bytes from the stream into the given array. */
  @Override
  public int read(byte[] array) throws IOException {
    return unwrap().read(array);
  }

  /**
   * Read n bytes from the stream into the given array at the specified offset.
   */
  @Override
  public int read(byte[] array, int offset, int n) throws IOException {
    return unwrap().read(array, offset, n);
  }

  /** Read bytes from the stream into the given buffer. */
  @Override
  public int read(ByteBuffer buf) throws IOException {
    return unwrap().read(buf);
  }

  /**
   * Read n bytes from the stream into the given buffer at the specified offset.
   */
  @Override
  public int read(ByteBuffer buf, int offset, int n) throws IOException {
    return unwrap().read(buf, offset, n);
  }

  /** Read bytes from the stream into the given array. */
  @Override
  public void readFully(byte[] array) throws IOException {
    unwrap().readFully(array);
  }

  /**
   * Read n bytes from the stream into the given array at the specified offset.
   */
  @Override
  public void readFully(byte[] array, int offset, int n) throws IOException {
    unwrap().readFully(array, offset, n);
  }

  // -- InputStream API methods --

  @Override
  public int read() throws IOException {
    return unwrap().read();
  }

  @Override
  public int available() throws IOException {
    return unwrap().available();
  }

  @Override
  public void mark(int readLimit) {
    unwrap().mark(readLimit);
  }

  @Override
  public boolean markSupported() {
    return unwrap().markSupported();
  }

  @Override
  public void reset() throws IOException {
    unwrap().reset();
  }

}
