/*
 * #%L
 * SCIFIO Bio-Formats compatibility format.
 * %%
 * Copyright (C) 2013 - 2017 Board of Regents of the University of
 * Wisconsin-Madison.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package io.scif.bf.wrapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.scijava.io.handle.DataHandle;
import org.scijava.io.location.Location;

import loci.common.IRandomAccess;
import loci.common.RandomAccessInputStream;

/**
 * Adapter class to convert {@link DataHandle} to
 * {@link loci.common.RandomAccessInputStream}.
 */
public class DataHandleAdapter extends RandomAccessInputStream implements
	IRandomAccess
{

	// -- Fields --

	private final DataHandle<Location> handle;

	// -- Constructors --

	public DataHandleAdapter(final DataHandle<Location> handle)
		throws IOException
	{
		super(new byte[1]);
		this.handle = handle;
	}

	// -- RandomAccessInputStreamWrapper API Methods --

	public DataHandle<Location> unwrap() {
		return handle;
	}

	// -- RandomAccessInputStream API methods --

	@Override
	public void setEncoding(final String encoding) {
		handle.setEncoding(encoding);
	}

	@Override
	public void seek(final long pos) throws IOException {
		if (handle != null) {
			handle.seek(pos);
		}
	}

	@Override
	public long length() throws IOException {
		return handle.length();
	}

	@Override
	public void setLength(final long newLength) throws IOException {
		handle.setLength(newLength);
	}

	@Override
	public long getFilePointer() throws IOException {
		return handle.offset();
	}

	@Override
	public boolean exists() throws IOException {
		return handle.exists();
	}

	@Override
	public void close() throws IOException {
		handle.close(); // FIXME this could explode us
	}

	@Override
	public void order(final boolean little) {
		handle.setLittleEndian(little);
	}

	@Override
	public boolean isLittleEndian() {
		return handle.isLittleEndian();
	}

	@Override
	public String readString(final String lastChars) throws IOException {
		return handle.readString(lastChars);
	}

	@Override
	public String findString(final String... terminators) throws IOException {
		return handle.findString(terminators);
	}

	@Override
	public String findString(final boolean saveString,
		final String... terminators) throws IOException
	{
		return handle.findString(saveString, terminators);
	}

	@Override
	public String findString(final int blockSize, final String... terminators)
		throws IOException
	{
		return handle.findString(blockSize, terminators);
	}

	@Override
	public String findString(final boolean saveString, final int blockSize,
		final String... terminators) throws IOException
	{
		return handle.findString(saveString, blockSize, terminators);
	}

	// -- DataInput API methods --

	@Override
	public boolean readBoolean() throws IOException {
		return handle.readBoolean();
	}

	@Override
	public byte readByte() throws IOException {
		return handle.readByte();
	}

	@Override
	public char readChar() throws IOException {
		return handle.readChar();
	}

	@Override
	public double readDouble() throws IOException {
		return handle.readDouble();
	}

	@Override
	public float readFloat() throws IOException {
		return handle.readFloat();
	}

	@Override
	public int readInt() throws IOException {
		return handle.readInt();
	}

	@Override
	public String readLine() throws IOException {
		return handle.readLine();
	}

	@Override
	public String readCString() throws IOException {
		return handle.readCString();
	}

	@Override
	public String readString(final int n) throws IOException {
		return handle.readString(n);
	}

	@Override
	public long readLong() throws IOException {
		return handle.readLong();
	}

	@Override
	public short readShort() throws IOException {
		return handle.readShort();
	}

	@Override
	public int readUnsignedByte() throws IOException {
		return handle.readUnsignedByte();
	}

	@Override
	public int readUnsignedShort() throws IOException {
		return handle.readUnsignedShort();
	}

	@Override
	public String readUTF() throws IOException {
		return handle.readUTF();
	}

	@Override
	public int skipBytes(final int n) throws IOException {
		return handle.skipBytes(n);
	}

	@Override
	public int read(final byte[] array) throws IOException {
		return handle.read(array);
	}

	@Override
	public int read(final byte[] array, final int offset, final int n)
		throws IOException
	{
		return handle.read(array, offset, n);
	}

	@Override
	public int read(final ByteBuffer buf) throws IOException {
		return handle.read(buf.array());
	}

	@Override
	public int read(final ByteBuffer buf, final int offset, final int n)
		throws IOException
	{
		return handle.read(buf.array(), offset, n);
	}

	@Override
	public void readFully(final byte[] array) throws IOException {
		handle.readFully(array);
	}

	@Override
	public void readFully(final byte[] array, final int offset, final int n)
		throws IOException
	{
		handle.readFully(array, offset, n);
	}

	// -- InputStream API methods --

	@Override
	public int read() throws IOException {
		return handle.read();
	}

	@Override
	public int available() throws IOException {
		if (handle.offset() < handle.length()) {
			return (int) (handle.offset() - handle.length());
		}
		return 0;
	}

	@Override
	public void mark(final int readLimit) {
		// NO-Op
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public void reset() throws IOException {
		handle.seek(0);
	}

	@Override
	public void write(final int b) throws IOException {
		throw readOnly();
	}

	private IOException readOnly() {
		return new IOException("DataHandleAdapter is read-only!");
	}

	@Override
	public void write(final byte[] b) throws IOException {
		throw readOnly();
	}

	@Override
	public void write(final byte[] b, final int off, final int len)
		throws IOException
	{
		throw readOnly();
	}

	@Override
	public void writeBoolean(final boolean v) throws IOException {
		throw readOnly();
	}

	@Override
	public void writeByte(final int v) throws IOException {
		throw readOnly();
	}

	@Override
	public void writeShort(final int v) throws IOException {
		throw readOnly();
	}

	@Override
	public void writeChar(final int v) throws IOException {
		throw readOnly();
	}

	@Override
	public void writeInt(final int v) throws IOException {
		throw readOnly();
	}

	@Override
	public void writeLong(final long v) throws IOException {
		throw readOnly();
	}

	@Override
	public void writeFloat(final float v) throws IOException {
		throw readOnly();
	}

	@Override
	public void writeDouble(final double v) throws IOException {
		throw readOnly();
	}

	@Override
	public void writeBytes(final String s) throws IOException {
		throw readOnly();
	}

	@Override
	public void writeChars(final String s) throws IOException {
		throw readOnly();
	}

	@Override
	public void writeUTF(final String s) throws IOException {
		throw readOnly();
	}

	@Override
	public ByteOrder getOrder() {
		return handle.isLittleEndian() ? ByteOrder.LITTLE_ENDIAN
			: ByteOrder.BIG_ENDIAN;
	}

	@Override
	public void setOrder(final ByteOrder order) {
		handle.setLittleEndian(order == ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void write(final ByteBuffer buf) throws IOException {
		throw readOnly();
	}

	@Override
	public void write(final ByteBuffer buf, final int off, final int len)
		throws IOException
	{
		throw readOnly();
	}
}
