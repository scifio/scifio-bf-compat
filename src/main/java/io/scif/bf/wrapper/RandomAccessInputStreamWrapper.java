/*
 * #%L
 * SCIFIO Bio-Formats compatibility format.
 * %%
 * Copyright (C) 2013 - 2016 Board of Regents of the University of
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

import loci.common.RandomAccessInputStream;

/**
 * Wrapper class to convert {@link io.scif.io.RandomAccessInputStream} to
 * {@link loci.common.RandomAccessInputStream}.
 */
public class RandomAccessInputStreamWrapper extends
	RandomAccessInputStream
{

	// -- Fields --

	private final io.scif.io.RandomAccessInputStream rais;

	// -- Constructors --

	public RandomAccessInputStreamWrapper(
		final io.scif.io.RandomAccessInputStream rais) throws IOException
	{
		super((String) null);
		this.rais = rais;
	}

	// -- RandomAccessInputStreamWrapper API Methods --

	public io.scif.io.RandomAccessInputStream unwrap() {
		return rais;
	}

	// -- RandomAccessInputStream API methods --

	@Override
	public void setEncoding(final String encoding) {
		unwrap().setEncoding(encoding);
	}

	@Override
	public void seek(final long pos) throws IOException {
		unwrap().seek(pos);
	}

	@Override
	public long length() throws IOException {
		return unwrap().length();
	}

	@Override
	public void setLength(final long newLength) throws IOException {
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
	public void order(final boolean little) {
		unwrap().order(little);
	}

	@Override
	public boolean isLittleEndian() {
		return unwrap().isLittleEndian();
	}

	@Override
	public String readString(final String lastChars) throws IOException {
		return unwrap().readString(lastChars);
	}

	@Override
	public String findString(final String... terminators) throws IOException {
		return unwrap().findString(terminators);
	}

	@Override
	public String findString(final boolean saveString,
		final String... terminators) throws IOException
	{
		return unwrap().findString(saveString, terminators);
	}

	@Override
	public String findString(final int blockSize, final String... terminators)
		throws IOException
	{
		return unwrap().findString(blockSize, terminators);
	}

	@Override
	public String findString(final boolean saveString, final int blockSize,
		final String... terminators) throws IOException
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
	public String readString(final int n) throws IOException {
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
	public int skipBytes(final int n) throws IOException {
		return unwrap().skipBytes(n);
	}

	@Override
	public int read(final byte[] array) throws IOException {
		return unwrap().read(array);
	}

	@Override
	public int read(final byte[] array, final int offset, final int n)
		throws IOException
	{
		return unwrap().read(array, offset, n);
	}

	@Override
	public int read(final ByteBuffer buf) throws IOException {
		return unwrap().read(buf);
	}

	@Override
	public int read(final ByteBuffer buf, final int offset, final int n)
		throws IOException
	{
		return unwrap().read(buf, offset, n);
	}

	@Override
	public void readFully(final byte[] array) throws IOException {
		unwrap().readFully(array);
	}

	@Override
	public void readFully(final byte[] array, final int offset, final int n)
		throws IOException
	{
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
	public void mark(final int readLimit) {
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
