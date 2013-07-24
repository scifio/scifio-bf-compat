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

  @Override
  public void setEncoding(String encoding) {
    unwrap().setEncoding(encoding);
  }

  @Override
  public void seek(long pos) throws IOException {
    unwrap().seek(pos);
  }

  @Override
  public long length() throws IOException {
    return unwrap().length();
  }

  @Override
  public void setLength(long newLength) throws IOException {
    unwrap().setLength(newLength);
  }

  @Override
  public long getFilePointer() throws IOException {
    return unwrap().getFilePointer();
  }

  @Override
  public void close() throws IOException {
    unwrap().close();
  }

  @Override
  public void order(boolean little) {
    unwrap().order(little);
  }

  @Override
  public boolean isLittleEndian() {
    return unwrap().isLittleEndian();
  }

  @Override
  public String readString(String lastChars) throws IOException {
    return unwrap().readString(lastChars);
  }

  @Override
  public String findString(String... terminators) throws IOException {
    return unwrap().findString(terminators);
  }

  @Override
  public String findString(boolean saveString, String... terminators)
      throws IOException
      {
    return unwrap().findString(saveString, terminators);
      }

  @Override
  public String findString(int blockSize, String... terminators)
      throws IOException
      {
    return unwrap().findString(blockSize, terminators);
      }

  @Override
  public String findString(boolean saveString, int blockSize,
      String... terminators) throws IOException
      {
    return unwrap().findString(saveString, blockSize, terminators);
      }

  // -- DataInput API methods --

  @Override
  public boolean readBoolean() throws IOException {
    return unwrap().readBoolean();
  }

  @Override
  public byte readByte() throws IOException {
    return unwrap().readByte();
  }

  @Override
  public char readChar() throws IOException {
    return unwrap().readChar();
  }

  @Override
  public double readDouble() throws IOException {
    return unwrap().readDouble();
  }

  @Override
  public float readFloat() throws IOException {
    return unwrap().readFloat();
  }

  @Override
  public int readInt() throws IOException {
    return unwrap().readInt();
  }

  @Override
  public String readLine() throws IOException {
    return unwrap().readLine();
  }

  @Override
  public String readCString() throws IOException {
    return unwrap().readCString();
  }

  @Override
  public String readString(int n) throws IOException {
    return unwrap().readString(n);
  }

  @Override
  public long readLong() throws IOException {
    return unwrap().readLong();
  }

  @Override
  public short readShort() throws IOException {
    return unwrap().readShort();
  }

  @Override
  public int readUnsignedByte() throws IOException {
    return unwrap().readUnsignedByte();
  }

  @Override
  public int readUnsignedShort() throws IOException {
    return unwrap().readUnsignedShort();
  }

  @Override
  public String readUTF() throws IOException {
    return unwrap().readUTF();
  }

  @Override
  public int skipBytes(int n) throws IOException {
    return unwrap().skipBytes(n);
  }

  @Override
  public int read(byte[] array) throws IOException {
    return unwrap().read(array);
  }

  @Override
  public int read(byte[] array, int offset, int n) throws IOException {
    return unwrap().read(array, offset, n);
  }

  @Override
  public int read(ByteBuffer buf) throws IOException {
    return unwrap().read(buf);
  }

  @Override
  public int read(ByteBuffer buf, int offset, int n) throws IOException {
    return unwrap().read(buf, offset, n);
  }

  @Override
  public void readFully(byte[] array) throws IOException {
    unwrap().readFully(array);
  }

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
