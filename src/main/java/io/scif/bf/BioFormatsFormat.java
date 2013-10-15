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
import net.imglib2.meta.CalibratedAxis;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;
import org.scijava.util.LongArray;

/**
 * Wraps an {@link ImageReader} in a SCIFIO {@link Format}, allowing proprietary
 * Bio-Formats readers to be used in SCIFIO-based applications.
 * 
 * @author Mark Hiner hinerm at gmail.com
 */
@Plugin(type = Format.class, priority = Priority.VERY_HIGH_PRIORITY)
public class BioFormatsFormat extends AbstractFormat {

	// -- Constants --

	/** List of classes already converted to SCIFIO. */
	public static final String[] DO_NOT_CONVERT = new String[] {
		"loci.formats.in.APNGReader", "loci.formats.in.AVIReader",
		"loci.formats.in.BMPReader", "loci.formats.in.DicomReader",
		"loci.formats.in.EPSReader", "loci.formats.in.FakeReader",
		"loci.formats.in.FitsReader", "loci.formats.in.GIFReader",
		"loci.formats.in.ICSReader", "loci.formats.in.JPEGReader",
		"loci.formats.in.JPEG2000Reader", "loci.formats.in.MicromanagerReader",
		"loci.formats.in.MinimalTiffReader", "loci.formats.in.MNGReader",
		"loci.formats.in.NRRDReader", "loci.formats.in.OBFReader",
		"loci.formats.in.OMETiffReader", "loci.formats.in.OMEXMLReader",
		"loci.formats.in.PCXReader", "loci.formats.in.PGMReader",
		"loci.formats.in.PictReader", "loci.formats.in.QTReader",
		"loci.formats.in.SCIFIOReader", "loci.formats.in.TextReader",
		"loci.formats.in.TiffDelegateReader", "loci.formats.in.TileJPEGReader",
		"loci.formats.in.ZipReader" };

	// -- Fields --

	/**
	 * List of Bio-Formats reader classes, excluding the {@link #DO_NOT_CONVERT}
	 * blacklist.
	 */
	private final ClassList<IFormatReader> readerClasses;

	// -- Constructors --

	/**
	 * Constructs a new Format with the default list of reader classes from
	 * readers.txt.
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

	/** Adds the given reader class to this format's supported reader list. */
	public void addReader(final Class<IFormatReader> readerClass) {
		readerClasses.addClass(readerClass);
		suffixes = createImageReader().getSuffixes();
	}

	// -- Format API Methods --

	@Override
	public String getFormatName() {
		return "Bio-Formats Compatibility Format";
	}

	@Override
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

		public void setReader(final IFormatReader reader) {
			this.reader = reader;
		}

		// -- Metadata API Methods --

		@Override
		public void populateImageMetadata() {
			for (int s = 0; s < reader.getSeriesCount(); s++) {
				add(convertMetadata(reader, s));
			}
		}

		@Override
		public void close(final boolean fileOnly) throws IOException {
			super.close(fileOnly);
			if (reader != null) reader.close(fileOnly);
		}
	}

	public static class Checker extends AbstractChecker {

		// -- Checker API Methods --

		@Override
		public boolean isFormat(final String name) {
			return createImageReader(this).isThisType(name);
		}

		@Override
		public boolean isFormat(final String name, final boolean open) {
			return createImageReader(this).isThisType(name, open);
		}

		@Override
		public boolean isFormat(final RandomAccessInputStream stream)
			throws IOException
		{
			return createImageReader(this).isThisType(
				new RandomAccessInputStreamWrapper(stream));
		}

		@Override
		public boolean checkHeader(final byte[] block) {
			return createImageReader(this).isThisType(block);
		}

	}

	public static class Parser extends AbstractParser<Metadata> {

		// -- Parser API Methods --

		@Override
		protected void typedParse(final RandomAccessInputStream stream,
			final Metadata meta) throws IOException, FormatException
		{
			try {
				final ImageReader reader = createImageReader(this);
				meta.setReader(reader);

				reader.setId(stream.getFileName());
			}
			catch (final loci.formats.FormatException e) {
				throw new FormatException(e);
			}
		}
	}

	public static class Reader extends ByteArrayReader<Metadata> {

		// -- Reader API Methods --

		@Override
		public ByteArrayPlane openPlane(final int imageIndex, final long planeIndex,
			final ByteArrayPlane plane, final long[] offsets, final long[] lengths)
			throws FormatException, IOException
		{
			final IFormatReader reader = getMetadata().getReader();
			reader.setSeries(imageIndex);
			try {
				Metadata meta = getMetadata();
				final int xIndex = meta.get(imageIndex).getAxisIndex(Axes.X), yIndex =
					meta.get(imageIndex).getAxisIndex(Axes.Y);
				final int x = (int) offsets[xIndex], y = (int) offsets[yIndex], w =
					(int) lengths[xIndex], h = (int) lengths[yIndex];
				reader.openBytes((int)planeIndex, plane.getBytes(), x, y, w, h);

				if (reader.get8BitLookupTable() != null) {
					plane.setColorTable(new ColorTable8(reader.get8BitLookupTable()));
				}
				else if (reader.get16BitLookupTable() != null) {
					plane.setColorTable(new ColorTable16(reader.get16BitLookupTable()));
				}
			}
			catch (final loci.formats.FormatException e) {
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
	private boolean convert(final Class<? extends IFormatReader> c) {
		for (final String s : DO_NOT_CONVERT) {
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
	private static ImageMetadata convertMetadata(final IFormatReader reader,
		final int s)
	{
		final ImageMetadata imgMeta = new DefaultImageMetadata();
		reader.setSeries(s);

		final ArrayList<CalibratedAxis> axes = new ArrayList<CalibratedAxis>();
		final LongArray axisLengths = new LongArray();
		imgMeta.setPlanarAxisCount(2);
		// parse interleaved channel dimensions
		parseChannelDimensions(reader, imgMeta, true, axes, axisLengths);

		// parse standard dimensions in dimensional order
		final String dimOrder = reader.getDimensionOrder().toUpperCase();
		for (int i = 0; i < dimOrder.length(); i++) {
			switch (dimOrder.charAt(i)) {
				case 'X':
					axes.add(FormatTools.createAxis(Axes.X));
					axisLengths.add((long) reader.getSizeX());
					break;
				case 'Y':
					axes.add(FormatTools.createAxis(Axes.Y));
					axisLengths.add((long) reader.getSizeY());
					break;
				case 'Z':
					if (reader.getSizeZ() > 1) {
						axes.add(FormatTools.createAxis(Axes.Z));
						axisLengths.add((long) reader.getSizeZ());
					}
					break;
				case 'C':
					if (reader.getSizeC() > 1) {
						// parse non-interleaved channel dimensions
						parseChannelDimensions(reader, imgMeta, false, axes, axisLengths);
					}
					else if (reader.isIndexed()) {
						// Indexed images have a length 1 channel axis
						imgMeta.setIndexed(true);
						axisLengths.add(1l);
						axes.add(FormatTools.createAxis(Axes.CHANNEL));
					}
					break;
				case 'T':
					if (reader.getSizeT() > 1) {
						axes.add(FormatTools.createAxis(Axes.TIME));
						axisLengths.add((long) reader.getSizeT());
					}
					break;
			}
		}

		imgMeta.setAxes(axes.toArray(new CalibratedAxis[axes.size()]));
		imgMeta.setAxisLengths(axisLengths.copyArray());
		if (reader.isRGB() && imgMeta.getAxisIndex(Axes.CHANNEL) >= 0) {
			imgMeta.setPlanarAxisCount(imgMeta.getAxisIndex(Axes.CHANNEL) + 1);
		}

		imgMeta.setThumbSizeX(reader.getThumbSizeX());
		imgMeta.setThumbSizeY(reader.getThumbSizeY());
		imgMeta.setPixelType(reader.getPixelType());

		final int bpp = reader.getBitsPerPixel();
		final int bitsPerPixel =
			bpp == 0 ? FormatTools.getBitsPerPixel(reader.getPixelType()) : bpp;
		imgMeta.setBitsPerPixel(bitsPerPixel);
		imgMeta.setOrderCertain(reader.isOrderCertain());
		imgMeta.setLittleEndian(reader.isLittleEndian());
		imgMeta.setFalseColor(reader.isFalseColor());
		imgMeta.setMetadataComplete(reader.isMetadataComplete());

		final MetaTable table = new DefaultMetaTable(reader.getSeriesMetadata());

		imgMeta.setTable(table);
		imgMeta.setThumbnail(reader.isThumbnailSeries());

		return imgMeta;
	}

	private static void parseChannelDimensions(final IFormatReader reader,
		final ImageMetadata meta, final boolean interleaved,
		final ArrayList<CalibratedAxis> axisTypes, final LongArray axisLengths)
	{
		final int[] cDimLengths = reader.getChannelDimLengths();
		final String[] cDimTypes = reader.getChannelDimTypes();
		for (int subC = 0; subC < cDimLengths.length; subC++) {
			if (interleaved != reader.isInterleaved(subC)) continue;
			if (interleaved) meta.setPlanarAxisCount(meta.getPlanarAxisCount() + 1);
			axisTypes.add(FormatTools.createAxis(Axes.get(cDimTypes[subC])));
			axisLengths.add((long) cDimLengths[subC]);
		}
	}

}
