/*
 * #%L
 * SCIFIO Bio-Formats compatibility format.
 * %%
 * Copyright (C) 2013 - 2014 Board of Regents of the University of
 * Wisconsin-Madison
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
import io.scif.HasColorTable;
import io.scif.HasFormat;
import io.scif.ImageMetadata;
import io.scif.MetaTable;
import io.scif.common.RandomAccessInputStreamWrapper;
import io.scif.config.SCIFIOConfig;
import io.scif.io.RandomAccessInputStream;
import io.scif.util.FormatTools;

import java.io.IOException;
import java.util.ArrayList;

import loci.formats.ClassList;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.meta.MetadataStore;
import loci.formats.ome.OMEXMLMetadataImpl;
import net.imglib2.display.ColorTable;
import net.imglib2.display.ColorTable16;
import net.imglib2.display.ColorTable8;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.meta.CalibratedAxis;
import ome.xml.model.primitives.PositiveFloat;

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

	// Channel mapping enum

	private static enum DesiredChannels {
		INTERLEAVED, PLANAR, NONPLANAR;
	}

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
		"loci.formats.in.SCIFIOReader", "loci.formats.in.SDTReader",
		"loci.formats.in.TextReader", "loci.formats.in.TiffDelegateReader",
		"loci.formats.in.TiffJAIReader", "loci.formats.in.TiffReader",
		"loci.formats.in.TileJPEGReader", "loci.formats.in.ZipReader" };

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
	}

	// -- BioFormatsFormat API Methods --

	/** Creates a new Bio-Formats {@link ImageReader}. */
	public ImageReader createImageReader() {
		return new ImageReader(readerClasses);
	}

	/** Adds the given reader class to this format's supported reader list. */
	public void addReader(final Class<IFormatReader> readerClass) {
		readerClasses.addClass(readerClass);
	}

	// -- Format API Methods --

	@Override
	protected String[] makeSuffixArray() {
		// NB: suffixes may change. see getSuffixes() override.
		return null;
	}

	@Override
	public String getFormatName() {
		return "Bio-Formats Compatibility Format";
	}

	@Override
	public String[] getSuffixes() {
		// NB: the suffixes may change, so this array must always be re-generated.
		return createImageReader().getSuffixes();
	}

	// -- Nested Classes --

	public static class Metadata extends AbstractMetadata implements
		HasColorTable
	{

		// -- Fields --

		private IFormatReader reader;

		private ColorTable colorTable;
		private boolean[] checkedColorTable;

		private String formatName;

		// -- BioFormatsFormatMetadata methods --

		// -- Getters and Setters --

		public IFormatReader getReader() {
			return reader;
		}

		public void setReader(final IFormatReader reader) {
			this.reader = reader;
			formatName = null;
		}

		// -- Metadata API Methods --

		@Override
		public void populateImageMetadata() {
			for (int s = 0; s < reader.getSeriesCount(); s++) {
				add(convertMetadata(reader, s));
			}
			formatName = super.getFormatName();
			formatName += " - Bio-Formats reader used: " + reader.getFormat();
			checkedColorTable = null;
		}

		@Override
		public void close(final boolean fileOnly) throws IOException {
			super.close(fileOnly);
			if (reader != null) reader.close(fileOnly);
		}

		// -- HasFormat methods --

		@Override
		public String getFormatName() {
			return formatName == null ? super.getFormatName() : formatName;
		}

		// -- HasColorTable methods --

		@Override
		public ColorTable getColorTable(int imageIndex, final long planeIndex) {
			if (imageIndex >= reader.getSeriesCount()) imageIndex = 0;

			if (!checkedColorTable(imageIndex)) {
				try {
					if (reader.get16BitLookupTable() != null) {
						colorTable = new ColorTable16(reader.get16BitLookupTable());
					}
					else if (reader.get8BitLookupTable() != null) {
						colorTable = new ColorTable8(reader.get8BitLookupTable());
					}
				}
				catch (loci.formats.FormatException e) {
					log().error(e);
				}
				catch (IOException e) {
					log().error(e);
				}
				checkedColorTable[imageIndex] = true;
			}

			return colorTable;
		}

		private boolean checkedColorTable(final int imageIndex) {
			if (checkedColorTable == null) checkedColorTable =
				new boolean[reader.getSeriesCount()];

			return checkedColorTable[imageIndex];
		}
	}

	public static class Checker extends AbstractChecker {

		// -- Checker API Methods --

		@Override
		public boolean isFormat(final String name) {
			if (!realSource(name)) return false;
			return createImageReader(this).isThisType(name);
		}

		@Override
		public boolean isFormat(final String name, final SCIFIOConfig config) {
			if (!realSource(name)) return false;
			return createImageReader(this).isThisType(name, config.checkerIsOpen());
		}

		@Override
		public boolean isFormat(final RandomAccessInputStream stream)
			throws IOException
		{
			if (!realSource(stream)) return false;
			return createImageReader(this).isThisType(
				new RandomAccessInputStreamWrapper(stream));
		}

		/**
		 * @return true iff the given name corresponds to a non-virtual source
		 */
		private boolean realSource(String name) {
			RandomAccessInputStream stream = null;
			try {
				stream = new RandomAccessInputStream(getContext(), name);
				boolean isRealSource = realSource(stream);
				stream.close();
				return isRealSource;
			}
			catch (IOException e) {
				return false;
			}
		}

		/**
		 * @return true iff the given stream is non-virtual (can read at least one
		 *         position)
		 */
		private boolean realSource(RandomAccessInputStream stream)
			throws IOException
		{
			return FormatTools.validStream(stream, 1, stream.isLittleEndian());
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
			final Metadata meta, final SCIFIOConfig config) throws IOException,
			FormatException
		{
			try {
				final ImageReader reader = createImageReader(this);
				meta.setReader(reader);

				MetadataStore store = new OMEXMLMetadataImpl();
				reader.setMetadataStore(store);
				reader.setOriginalMetadataPopulated(config.parserIsSaveOriginalMetadata());
				reader.setMetadataFiltered(config.parserIsFiltered());
				reader.setGroupFiles(config.groupableIsGroupFiles());
				reader.setId(stream.getFileName());

				meta.setTable(new DefaultMetaTable(reader.getGlobalMetadata()));
			}
			catch (final loci.formats.FormatException e) {
				throw new FormatException(e);
			}
		}
	}

	public static class Reader extends ByteArrayReader<Metadata> {

		// -- Reader API Methods --

		@Override
		public ByteArrayPlane openPlane(final int imageIndex,
			final long planeIndex, final ByteArrayPlane plane, final long[] offsets,
			final long[] lengths, final SCIFIOConfig config) throws FormatException,
			IOException
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

				plane.setColorTable(getMetadata().getColorTable(imageIndex, planeIndex));
			}
			catch (final loci.formats.FormatException e) {
				throw new FormatException(e);
			}

			return plane;
		}

		@Override
		protected String[] createDomainArray() {
			return new String[0];
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

		OMEXMLMetadataImpl store = (OMEXMLMetadataImpl) reader.getMetadataStore();
		final ArrayList<CalibratedAxis> axes = new ArrayList<CalibratedAxis>();
		final LongArray axisLengths = new LongArray();
		imgMeta.setPlanarAxisCount(2);
		// parse interleaved channel dimensions
		parseChannelDimensions(reader, imgMeta, DesiredChannels.INTERLEAVED, axes, axisLengths);
		// parse standard dimensions in dimensional order
		final String dimOrder = reader.getDimensionOrder().toUpperCase();
		CalibratedAxis axis = null;
		// CTR HACK: Recover gracefully when StageLabel element is missing.
		// This avoids a problem with the OMEXMLMetadataImpl implementation,
		// which currently does not check for null on the StageLabel object.
		Double stageLabelX = null, stageLabelY = null, stageLabelZ = null;
		try {
			stageLabelX = store.getStageLabelX(s);
			stageLabelY = store.getStageLabelY(s);
			stageLabelZ = store.getStageLabelZ(s);
		}
		catch (final NullPointerException exc) {
			// ignore
		}
		for (int i = 0; i < dimOrder.length(); i++) {
			switch (dimOrder.charAt(i)) {
				case 'X':
					axis = FormatTools.createAxis(Axes.X);
					axes.add(axis);
					axisLengths.add((long) reader.getSizeX());
					calibarte(store.getPixelsPhysicalSizeX(s), axis, stageLabelX);
					break;
				case 'Y':
					axis = FormatTools.createAxis(Axes.Y);
					axes.add(axis);
					axisLengths.add((long) reader.getSizeY());
					calibarte(store.getPixelsPhysicalSizeY(s), axis, stageLabelY);
					// Ensure non-interleaved RGB channels are parsed after the Y axis
					parseChannelDimensions(reader, imgMeta, DesiredChannels.PLANAR, axes,
						axisLengths);
					break;
				case 'Z':
					axis = FormatTools.createAxis(Axes.Z);
					if (reader.getSizeZ() > 1) {
						axes.add(axis);
						axisLengths.add((long) reader.getSizeZ());
						calibarte(store.getPixelsPhysicalSizeZ(s), axis, stageLabelZ);
					}
					break;
				case 'C':
					// parse non-planar channel dimensions
					parseChannelDimensions(reader, imgMeta, DesiredChannels.NONPLANAR,
						axes, axisLengths);
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

	/**
	 * Calibrates the given axis if the physical pixel size is non-null
	 * @param stageLabel 
	 */
	private static void calibarte(PositiveFloat pixelsPhysicalSize,
		CalibratedAxis axis, Double stageLabel)
	{
		if (pixelsPhysicalSize != null) {
			FormatTools.calibrate(axis, pixelsPhysicalSize.getValue(),
				stageLabel == null ? 0.0 : stageLabel);
		}
	}

	/**
	 * Bio-Formats groups RGB and non-RGB channels together. To map back to SCIFIO
	 * there are three classes of channels we want to extract:
	 * <ul>
	 * <li>RGB interleaved</li>
	 * <li>RGB non-interleaved</li>
	 * <li>Non-RGB, non-interleaved</li>
	 * </ul>
	 * The first two of these will be planar axes in SCIFIO (note they are
	 * mutually exclusive with each other). The third is a non-planar Channel
	 * axis.
	 */
	private static void parseChannelDimensions(final IFormatReader reader,
		final ImageMetadata meta, final DesiredChannels query,
		final ArrayList<CalibratedAxis> axisTypes, final LongArray axisLengths)
	{
		// FIXME: this logic discards any of the sub-channel labels known by
		// Bio-Formats.. we need to find a way to account for them and properly
		// transfer labels
		if (query == DesiredChannels.INTERLEAVED || query == DesiredChannels.PLANAR) {
			if (reader.isInterleaved() == (query == DesiredChannels.INTERLEAVED)) return;
			long length = 1;
			if (reader.getRGBChannelCount() > 1) {
				length = reader.getRGBChannelCount();
			}
			else if (reader.isIndexed() && reader.getEffectiveSizeC() == 1) {
				// If effectiveSizeC and RGBChannelCount are both 1, we stick the 
				// indexed Channel axis as a planar axis.
				meta.setIndexed(true);
			}
			else {
				// Reader isn't indexed and doesn't have a significant RGB channel count
				return;
			}

			// We use Axes.CHANNEL for RGB channels
			axisTypes.add(FormatTools.createAxis(Axes.CHANNEL));
			meta.setPlanarAxisCount(meta.getPlanarAxisCount() + 1);
			axisLengths.add(length);
		}
		else if (query == DesiredChannels.NONPLANAR) {
			if (reader.getEffectiveSizeC() > 1) {
				meta.setIndexed(reader.isIndexed());
				AxisType type = null;
				if (reader.getRGBChannelCount() > 1) {
					// Axes.CHANNEL was already used for RGB channels
					type = Axes.get("Channels-planar");
				}
				else {
					type = Axes.CHANNEL;
				}
				axisTypes.add(FormatTools.createAxis(type));
				axisLengths.add((long) reader.getEffectiveSizeC());
			}
		}
	}
}
