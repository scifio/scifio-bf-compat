/*
 * #%L
 * SCIFIO Bio-Formats compatibility format.
 * %%
 * Copyright (C) 2013 - 2020 SCIFIO developers.
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
import io.scif.bf.wrapper.DataHandleAdapter;
import io.scif.config.SCIFIOConfig;
import io.scif.ome.services.OMEXMLService;
import io.scif.util.FormatTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imglib2.Interval;
import net.imglib2.display.ColorTable;
import net.imglib2.display.ColorTable16;
import net.imglib2.display.ColorTable8;

import org.scijava.Priority;
import org.scijava.io.handle.DataHandle;
import org.scijava.io.handle.DataHandleService;
import org.scijava.io.location.FileLocation;
import org.scijava.io.location.Location;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.LongArray;

import loci.formats.ClassList;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.meta.MetadataStore;
import loci.formats.ome.OMEXMLMetadataImpl;
import ome.units.quantity.Length;
import ome.xml.model.primitives.Color;

/**
 * Wraps an {@link ImageReader} in a SCIFIO {@link Format}, allowing proprietary
 * Bio-Formats readers to be used in SCIFIO-based applications.
 *
 * @author Mark Hiner hinerm at gmail.com
 */
@Plugin(type = Format.class, name = "Bio-Formats Compatibility Format",
	priority = Priority.VERY_HIGH)
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
	private ClassList<IFormatReader> readerClasses;

	private ImageReader cachedReader;

	private int cachedReaderHash;

	// -- Constructors --

	/**
	 * Constructs a new Format with the default list of reader classes from
	 * readers.txt.
	 */
	public BioFormatsFormat() {
		cacheReaderClasses();
	}

	// -- BioFormatsFormat API Methods --

	/** Creates a new Bio-Formats {@link ImageReader}. */
	public ImageReader createImageReader() {
		// Check for updated reader classes
		cacheReaderClasses();
		return new ImageReader(readerClasses);
	}

	/**
	 * Gets a cached {@link ImageReader}, suitable for using for format checking
	 * or suffix generation. Will only instantiate an {@code ImageReader} if
	 * necessary.
	 */
	public ImageReader getCachedImageReader() {
		if (cacheReaderClasses() || cachedReader == null) {
			cachedReader = createImageReader();
		}
		return cachedReader;
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
	public String[] getSuffixes() {
		// NB: the suffixes may change, so this array must always be re-generated.
		return getCachedImageReader().getSuffixes();
	}

	// -- Nested Classes --

	public static class Metadata extends AbstractMetadata implements
		HasColorTable
	{

		// -- Fields --

		@Parameter
		private OMEXMLService omexmlService;

		private IFormatReader reader;

		private MetadataStore metadataStore;

		private String formatName;

		private final Map<String, ColorTable> colorTables16 = new WeakHashMap<>();

		private final Map<String, ColorTable> colorTables8 = new WeakHashMap<>();

		private final Map<MetadataRetrieve, ColorTable> colorTableXML =
			new WeakHashMap<>();

		// -- BioFormatsFormatMetadata methods --

		// -- Getters and Setters --

		public IFormatReader getReader() {
			return reader;
		}

		public void setReader(final IFormatReader reader) {
			this.reader = reader;
			formatName = null;
			metadataStore = null;
		}

		// -- Metadata API Methods --

		public MetadataStore getMetadataStore() {
			// Cache the metadata store so that it can be used after the reader is
			// closed.
			if (Objects.isNull(metadataStore)) metadataStore = getReader()
				.getMetadataStore();
			return metadataStore;
		}

		@Override
		public void populateImageMetadata() {
			for (int s = 0; s < reader.getSeriesCount(); s++) {
				add(convertMetadata(reader, s));
			}
			formatName = super.getFormatName();
			formatName += " - Bio-Formats reader used: " + reader.getFormat();
			// Ensure the metadata store is initialized
			getMetadataStore();
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

			final String key = getKey(reader, imageIndex);

			// return early if we have the color table cached
			final ColorTable ct16 = colorTables16.get(key);
			final ColorTable ct8 = colorTables8.get(key);
			if (ct16 != null || ct8 != null) {
				return ct16 == null ? ct8 : ct16;
			}
			final int oldIndex = reader.getSeries();
			reader.setSeries(imageIndex);

			// See if the reader has a ColorTable attached already
			try {
				// try getting the 16 bit one
				final short[][] table16 = reader.get16BitLookupTable();
				if (table16 != null) {
					reader.setSeries(oldIndex);
					return colorTables16.put(key, new ColorTable16(table16));
				}
				// try getting 8bit color table
				final byte[][] table8 = reader.get8BitLookupTable();
				if (table8 != null) {
					reader.setSeries(oldIndex);
					return colorTables8.put(key, new ColorTable8(table8));
				}
			}
			catch (loci.formats.FormatException | IOException e) {
				log().error(e);
			}

			ColorTable ct = null;
			// Check the metadata to see if there is a Color entry in the XML
			final MetadataRetrieve retrieve = omexmlService.asRetrieve(
				getMetadataStore());
			if (retrieve != null) {
				ct = colorTableXML.get(retrieve);
				if (ct == null) {
					final long channelIndex = FormatTools.getNonPlanarAxisPosition(this,
						imageIndex, planeIndex, Axes.CHANNEL);
					if (channelIndex >= 0 && retrieve.getChannelCount(imageIndex) > 0 &&
						channelIndex < retrieve.getChannelCount(imageIndex))
					{
						final Color channelColor = retrieve.getChannelColor(imageIndex,
							(int) channelIndex);
						final boolean eightBit = reader
							.getPixelType() == FormatTools.UINT8 || reader
								.getPixelType() == FormatTools.INT8;
						ct = makeColorTable(channelColor, eightBit);
						colorTableXML.put(retrieve, ct);
					}
				}
			}

			return ct;
		}

		private String getKey(final IFormatReader r, final int imageIndex) {
			return "r" + r.hashCode() + "img" + imageIndex;
		}

		/**
		 * Turns a {@link Color} into a {@link ColorTable}. If {@code eightBit} is
		 * true, then a {@link ColorTable8} will be made - otherwise a
		 * {@link ColorTable16}.
		 */
		private ColorTable makeColorTable(final Color color,
			final boolean eightBit)
		{
			if (color == null) return null;
			final int red = color.getRed();
			final int green = color.getGreen();
			final int blue = color.getBlue();
			ColorTable lut = null;
			final int lutLength = 256;
			final int lutDivisor = lutLength - 1;
			if (eightBit) {
				// Make an 8-bit color table. The Color object is 8-bit, so this maps
				// perfectly
				final byte[] r = new byte[lutLength];
				final byte[] g = new byte[lutLength];
				final byte[] b = new byte[lutLength];
				for (int i = 0; i < lutLength; i++) {
					r[i] = (byte) (i * red / lutDivisor);
					g[i] = (byte) (i * green / lutDivisor);
					b[i] = (byte) (i * blue / lutDivisor);
				}
				lut = new ColorTable8(r, g, b);
			}
			else {
				// Make a 16-bit color table. Since the Color object is 8-bit, we
				// have to chunk it across the 16-bit lut.
				final short[] r = new short[65536];
				final short[] g = new short[65536];
				final short[] b = new short[65536];
				for (int i = 0; i < lutLength; i++) {
					final int index = i * (65536 / 256);
					r[index] = (short) ((i * red / lutDivisor) << 8);
					g[index] = (short) ((i * green / lutDivisor) << 8);
					b[index] = (short) ((i * blue / lutDivisor) << 8);
				}
				lut = new ColorTable16(r, g, b);
			}
			return lut;
		}
	}

	public static class Checker extends AbstractChecker {

		// -- Checker API Methods --

		@Parameter
		DataHandleService handles;

		@Parameter
		LogService log;

		@Override
		public boolean isFormat(final Location loc) {
			if (!realSource(loc)) return false;
			return getCachedImageReader(this).isThisType(loc.getName());
		}

		@Override
		public boolean isFormat(final Location loc, final SCIFIOConfig config) {
			try {
				DataHandle<Location> handle = handles.create(loc);
				if (handle == null || !handle.exists() || !realSource(loc)) {
					return false;
				}
				if (loc instanceof FileLocation) {
					// shortcut for FileLocations
					return getCachedImageReader(this).isThisType(((FileLocation) loc)
						.getFile().getAbsolutePath(), config.checkerIsOpen());
				}
				loci.common.Location.getIdMap().put(loc.getName(),
					new DataHandleAdapter(handle));
			}
			catch (final IOException exc) {
				log.error("Failed to create handle for location " + loc.toString(),
					exc);
				return false;
			}
			return getCachedImageReader(this).isThisType(loc.getName(), config
				.checkerIsOpen());
		}

		@Override
		public boolean isFormat(final DataHandle<Location> handle)
			throws IOException
		{
			if (!realSource(handle)) return false;
			loci.common.Location.getIdMap().put(handle.get().getName(), handle);
			return getCachedImageReader(this).isThisType(new DataHandleAdapter(
				handle));
		}

		@Override
		public boolean suffixSufficient() {
			return false;
		}

		/**
		 * @return true iff the given name corresponds to a non-virtual source
		 */
		private boolean realSource(final Location loc) { // FIXME this is wasteful!
			try (final DataHandle<Location> handle = handles.create(loc)) {
				return (handle != null && realSource(handle));
			}
			catch (final IOException e) {
				return false;
			}
		}

		/**
		 * @return true iff the given stream is non-virtual (can read at least one
		 *         position)
		 */
		private boolean realSource(final DataHandle<Location> handle)
			throws IOException
		{
			return FormatTools.validStream(handle, 1, handle.isLittleEndian());
		}

		@Override
		public boolean checkHeader(final byte[] block) {
			return getCachedImageReader(this).isThisType(block);
		}

	}

	public static class Parser extends AbstractParser<Metadata> {

		// -- Parser API Methods --

		@Override
		protected void typedParse(final DataHandle<Location> stream,
			final Metadata meta, final SCIFIOConfig config) throws IOException,
			FormatException
		{
			try {
				final ImageReader reader = createImageReader(this);
				meta.setReader(reader);

				final MetadataStore store = new OMEXMLMetadataImpl();
				reader.setMetadataStore(store);
				reader.setOriginalMetadataPopulated(config
					.parserIsSaveOriginalMetadata());
				reader.setMetadataFiltered(config.parserIsFiltered());
				reader.setGroupFiles(config.groupableIsGroupFiles());

				if (stream.get() instanceof FileLocation) {
					// short-cut for file-locations
					reader.setId(((FileLocation) stream.get()).getFile()
						.getAbsolutePath());
				}
				else {
					// fall-back: we try to map the datahandle directly
					final DataHandleAdapter value = new DataHandleAdapter(stream);
					loci.common.Location.getIdMap().put(stream.get().getName(), value);
					reader.setId(stream.get().getName());
				}

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
		public ByteArrayPlane openPlane(final int imageIndex, final long planeIndex,
			final ByteArrayPlane plane, final Interval bounds,
			final SCIFIOConfig config) throws FormatException, IOException
		{
			final IFormatReader reader = getMetadata().getReader();
			reader.setSeries(imageIndex);
			try {
				final Metadata meta = getMetadata();
				final int xIndex = meta.get(imageIndex).getAxisIndex(Axes.X);
				final int yIndex = meta.get(imageIndex).getAxisIndex(Axes.Y);
				final int x = (int) bounds.min(xIndex);
				final int y = (int) bounds.min(yIndex);
				final int w = (int) bounds.dimension(xIndex);
				final int h = (int) bounds.dimension(yIndex);
				reader.openBytes((int) planeIndex, plane.getBytes(), x, y, w, h);

				plane.setColorTable(getMetadata().getColorTable(imageIndex,
					planeIndex));
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
	 *
	 * @return true if the reader class list was re-generated.
	 */
	private boolean cacheReaderClasses() {
		final Class<? extends IFormatReader>[] defaultClasses = ImageReader
			.getDefaultReaderClasses().getClasses();
		final int currentHash = Arrays.hashCode(defaultClasses);

		// If our classList is uninitialized, or the Bio-Formats classList has
		// changed, compute the current reader classes.
		if (readerClasses == null || cachedReaderHash != currentHash) {
			final ClassList<IFormatReader> targetClasses = new ClassList<>(
				IFormatReader.class);

			// add reader classes to the list, excluding the blacklist
			for (final Class<? extends IFormatReader> c : defaultClasses) {
				if (convert(c)) targetClasses.addClass(c);
			}
			readerClasses = targetClasses;
			cachedReaderHash = currentHash;

			return true;
		}
		return false;
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
	 * As {@link #createImageReader(HasFormat)} but will use
	 * {@link #getCachedImageReader()} instead.
	 */
	private static ImageReader getCachedImageReader(final HasFormat thing) {
		return ((BioFormatsFormat) thing.getFormat()).getCachedImageReader();
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

		final OMEXMLMetadataImpl store = (OMEXMLMetadataImpl) reader
			.getMetadataStore();
		final ArrayList<CalibratedAxis> axes = new ArrayList<>();
		final LongArray axisLengths = new LongArray();
		imgMeta.setPlanarAxisCount(2);
		// parse interleaved channel dimensions
		parseChannelDimensions(reader, imgMeta, DesiredChannels.INTERLEAVED, axes,
			axisLengths);
		// parse standard dimensions in dimensional order
		final String dimOrder = reader.getDimensionOrder().toUpperCase();
		CalibratedAxis axis = null;
		// CTR HACK: Recover gracefully when StageLabel element is missing.
		// This avoids a problem with the OMEXMLMetadataImpl implementation,
		// which currently does not check for null on the StageLabel object.
		Length stageLabelX = null, stageLabelY = null, stageLabelZ = null;
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
					calibrate(store.getPixelsPhysicalSizeX(s), axis, stageLabelX);
					break;
				case 'Y':
					axis = FormatTools.createAxis(Axes.Y);
					axes.add(axis);
					axisLengths.add((long) reader.getSizeY());
					calibrate(store.getPixelsPhysicalSizeY(s), axis, stageLabelY);
					// Ensure non-interleaved RGB channels are parsed after the Y axis
					parseChannelDimensions(reader, imgMeta, DesiredChannels.PLANAR, axes,
						axisLengths);
					break;
				case 'Z':
					axis = FormatTools.createAxis(Axes.Z);
					if (reader.getSizeZ() > 1) {
						axes.add(axis);
						axisLengths.add((long) reader.getSizeZ());
						calibrate(store.getPixelsPhysicalSizeZ(s), axis, stageLabelZ);
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

		imgMeta.setThumbSizeX(reader.getThumbSizeX());
		imgMeta.setThumbSizeY(reader.getThumbSizeY());
		imgMeta.setPixelType(reader.getPixelType());

		final int bpp = reader.getBitsPerPixel();
		final int bitsPerPixel = bpp == 0 ? FormatTools.getBitsPerPixel(reader
			.getPixelType()) : bpp;
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
	 *
	 * @param stageLabel
	 */
	private static void calibrate(final Length pixelsPhysicalSize,
		final CalibratedAxis axis, final Length stageLabel)
	{
		if (pixelsPhysicalSize != null) {
			FormatTools.calibrate(axis, pixelsPhysicalSize.value().doubleValue(),
				stageLabel == null ? 0.0 : stageLabel.value().doubleValue());
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
		if (query == DesiredChannels.INTERLEAVED ||
			query == DesiredChannels.PLANAR)
		{
			if (reader.isInterleaved() != (query == DesiredChannels.INTERLEAVED))
				return;
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
