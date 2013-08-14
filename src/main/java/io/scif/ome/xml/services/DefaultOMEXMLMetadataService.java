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

package io.scif.ome.xml.services;

import io.scif.FormatException;
import io.scif.ImageMetadata;
import io.scif.Metadata;
import io.scif.Reader;
import io.scif.common.DateTools;
import io.scif.io.Location;
import io.scif.ome.xml.meta.OMEXMLMetadata;
import io.scif.services.FormatService;
import io.scif.services.ServiceException;
import io.scif.util.FormatTools;
import io.scif.util.SCIFIOMetadataTools;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.meta.MetadataStore;
import net.imglib2.meta.Axes;
import ome.xml.model.BinData;
import ome.xml.model.OME;
import ome.xml.model.enums.Binning;
import ome.xml.model.enums.Correction;
import ome.xml.model.enums.DetectorType;
import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.enums.EnumerationException;
import ome.xml.model.enums.ExperimentType;
import ome.xml.model.enums.Immersion;
import ome.xml.model.enums.LaserMedium;
import ome.xml.model.enums.LaserType;
import ome.xml.model.enums.PixelType;
import ome.xml.model.enums.handlers.BinningEnumHandler;
import ome.xml.model.enums.handlers.CorrectionEnumHandler;
import ome.xml.model.enums.handlers.DetectorTypeEnumHandler;
import ome.xml.model.enums.handlers.ExperimentTypeEnumHandler;
import ome.xml.model.enums.handlers.ImmersionEnumHandler;
import ome.xml.model.enums.handlers.LaserMediumEnumHandler;
import ome.xml.model.enums.handlers.LaserTypeEnumHandler;
import ome.xml.model.primitives.NonNegativeInteger;
import ome.xml.model.primitives.NonNegativeLong;
import ome.xml.model.primitives.PositiveFloat;
import ome.xml.model.primitives.PositiveInteger;
import ome.xml.model.primitives.Timestamp;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link OMEXMLMetadataService}.
 */
@Plugin(type = Service.class)
public class DefaultOMEXMLMetadataService extends AbstractService implements
	OMEXMLMetadataService
{

	// -- Constants --

	private static final Logger LOGGER = LoggerFactory
		.getLogger(DefaultOMEXMLMetadataService.class);

	// -- Static fields --

	private boolean defaultDateEnabled = true;

	// -- Parameters --

	@Parameter
	private FormatService formatService;

	// -- Utility methods - OME-XML --

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#
	 * populatePixels(ome.xml.meta.MetadataStore, io.scif.Metadata)
	 */
	public void populatePixels(final MetadataStore store, final Metadata meta) {
		populatePixels(store, meta, false, true);
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#
	 * populatePixels(ome.xml.meta.MetadataStore, io.scif.Metadata, boolean)
	 */
	public void populatePixels(final MetadataStore store, final Metadata meta,
		final boolean doPlane)
	{
		populatePixels(store, meta, doPlane, true);
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#
	 * populatePixels(ome.xml.meta.MetadataStore, io.scif.Metadata, boolean, boolean)
	 */
	public void populatePixels(final MetadataStore store, final Metadata meta,
		final boolean doPlane, final boolean doImageName)
	{
		if (store == null || meta == null) return;
		for (int i = 0; i < meta.getImageCount(); i++) {

			String imageName = null;
			if (doImageName) {
				final Location f = new Location(getContext(), meta.getDatasetName());
				imageName = f.getName();
			}
			final String pixelType =
				FormatTools.getPixelTypeString(meta.getPixelType(i));
			final String order = FormatTools.findDimensionOrder(meta, i);

			final int xSize = meta.getAxisLength(i, Axes.X);
			final int ySize = meta.getAxisLength(i, Axes.Y);
			final int zSize = meta.getAxisLength(i, Axes.Z);
			final int cSize = meta.getAxisLength(i, Axes.CHANNEL);
			final int tSize = meta.getAxisLength(i, Axes.TIME);
			final double calX = meta.getAxis(i, Axes.X).calibration();
			final double calY = meta.getAxis(i, Axes.X).calibration();
			final double calZ = meta.getAxis(i, Axes.X).calibration();
			final double calC = meta.getAxis(i, Axes.X).calibration();
			final double calT = meta.getAxis(i, Axes.X).calibration();

			populateMetadata(store, meta.getDatasetName(), i, imageName, meta
				.isLittleEndian(i), order, pixelType, xSize, ySize, zSize, cSize,
				tSize, calX, calY, calZ, calC, calT, meta.getRGBChannelCount(i));

			final OMEXMLService service =
				formatService.getInstance(OMEXMLService.class);
			if (service.isOMEXMLRoot(store.getRoot())) {
				// TODO any way or reason to access a base store?
				if (service.isOMEXMLMetadata(store)) {
					OMEXMLMetadata omeMeta;
					try {
						omeMeta = service.getOMEMetadata(service.asRetrieve(store));
						omeMeta.resolveReferences();
					}
					catch (final ServiceException e) {
						LOGGER.warn("Failed to resolve references", e);
					}
				}

				final OME root = (OME) store.getRoot();
				final BinData bin = root.getImage(i).getPixels().getBinData(0);
				bin.setLength(new NonNegativeLong(0L));
				store.setRoot(root);
			}

			if (doPlane) {
				for (int q = 0; q < meta.getPlaneCount(i); q++) {
					final int[] coords = FormatTools.getZCTCoords(meta, i, q);
					store.setPlaneTheZ(new NonNegativeInteger(coords[0]), i, q);
					store.setPlaneTheC(new NonNegativeInteger(coords[1]), i, q);
					store.setPlaneTheT(new NonNegativeInteger(coords[2]), i, q);
				}
			}
		}
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#populateMetadata(
	 * ome.xml.meta.MetadataStore, int, java.lang.String, boolean, 
	 * java.lang.String, java.lang.String, int, int, int, int, int, int)
	 */
	public void populateMetadata(final MetadataStore store, final int imageIndex,
		final String imageName, final boolean littleEndian,
		final String dimensionOrder, final String pixelType, final int sizeX,
		final int sizeY, final int sizeZ, final int sizeC, final int sizeT,
		final double calX, final double calY, final double calZ, final double calC,
		final double calT, final int samplesPerPixel)
	{
		populateMetadata(store, null, imageIndex, imageName, littleEndian,
			dimensionOrder, pixelType, sizeX, sizeY, sizeZ, sizeC, sizeT, calX, calY,
			calZ, calC, calT, samplesPerPixel);
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#populateMetadata(
	 * ome.xml.meta.MetadataStore, int, java.lang.String, io.scif.Metadata)
	 */
	public void populateMetadata(final MetadataStore store, final int imageIndex,
		final String imageName, final Metadata meta)
	{

		final int sizeX = meta.getAxisLength(imageIndex, Axes.X);
		final int sizeY = meta.getAxisLength(imageIndex, Axes.Y);
		final int sizeZ = meta.getAxisLength(imageIndex, Axes.Z);
		final int sizeC = meta.getAxisLength(imageIndex, Axes.CHANNEL);
		final int sizeT = meta.getAxisLength(imageIndex, Axes.TIME);
		final double calX = meta.getAxis(imageIndex, Axes.X).calibration();
		final double calY = meta.getAxis(imageIndex, Axes.X).calibration();
		final double calZ = meta.getAxis(imageIndex, Axes.X).calibration();
		final double calC = meta.getAxis(imageIndex, Axes.X).calibration();
		final double calT = meta.getAxis(imageIndex, Axes.X).calibration();

		final String pixelType =
			FormatTools.getPixelTypeString(meta.getPixelType(imageIndex));
		final int effSizeC = meta.getPlaneCount(imageIndex) / sizeZ / sizeT;
		final int samplesPerPixel = sizeC / effSizeC;
		populateMetadata(store, null, imageIndex, imageName, meta
			.isLittleEndian(imageIndex), FormatTools.findDimensionOrder(meta,
			imageIndex), pixelType, sizeX, sizeY, sizeZ, sizeC, sizeT, calX, calY,
			calZ, calC, calT, samplesPerPixel);
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#
	 * populateMetadata(ome.xml.meta.MetadataStore,
	 * java.lang.String, int, java.lang.String, boolean,
	 * java.lang.String, java.lang.String, int, int, int, int, int, int)
	 */
	public void populateMetadata(final MetadataStore store, final String file,
		final int imageIndex, final String imageName, final boolean littleEndian,
		final String dimensionOrder, final String pixelType, final int sizeX,
		final int sizeY, final int sizeZ, final int sizeC, final int sizeT,
		final double calX, final double calY, final double calZ, final double calC,
		final double calT, final int samplesPerPixel)
	{
		store.setImageID(createLSID("Image", imageIndex), imageIndex);
		setDefaultCreationDate(store, file, imageIndex);
		if (imageName != null) store.setImageName(imageName, imageIndex);
		populatePixelsOnly(store, imageIndex, littleEndian, dimensionOrder,
			pixelType, sizeX, sizeY, sizeZ, sizeC, sizeT, calX, calY, calZ, calC,
			calT, samplesPerPixel);
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#populatePixelsOnly(
	 * ome.xml.meta.MetadataStore, io.scif.Reader)
	 */
	public void populatePixelsOnly(final MetadataStore store, final Reader r) {
		final Metadata dMeta = r.getMetadata();

		for (int imageIndex = 0; imageIndex < r.getImageCount(); imageIndex++) {
			final String pixelType =
				FormatTools.getPixelTypeString(dMeta.getPixelType(imageIndex));

			populatePixelsOnly(store, imageIndex, dMeta.isLittleEndian(imageIndex),
				FormatTools.findDimensionOrder(dMeta, imageIndex), pixelType, dMeta
					.getAxisLength(imageIndex, Axes.X), dMeta.getAxisLength(imageIndex,
					Axes.Y), dMeta.getAxisLength(imageIndex, Axes.Z), dMeta
					.getAxisLength(imageIndex, Axes.CHANNEL), dMeta.getAxisLength(
					imageIndex, Axes.TIME), dMeta.getAxis(imageIndex, Axes.X)
					.calibration(), dMeta.getAxis(imageIndex, Axes.Y).calibration(),
				dMeta.getAxis(imageIndex, Axes.Z).calibration(), dMeta.getAxis(
					imageIndex, Axes.CHANNEL).calibration(), dMeta.getAxis(imageIndex,
					Axes.TIME).calibration(), dMeta.getRGBChannelCount(imageIndex));
		}
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#populatePixelsOnly(
	 * ome.xml.meta.MetadataStore, int, boolean, java.lang.String, 
	 * java.lang.String, int, int, int, int, int, int)
	 */
	public void populatePixelsOnly(final MetadataStore store,
		final int imageIndex, final boolean littleEndian,
		final String dimensionOrder, final String pixelType, final int sizeX,
		final int sizeY, final int sizeZ, final int sizeC, final int sizeT,
		final double calX, final double calY, final double calZ, final double calC,
		final double calT, final int samplesPerPixel)
	{
		store.setPixelsID(createLSID("Pixels", imageIndex), imageIndex);
		store.setPixelsBinDataBigEndian(!littleEndian, imageIndex, 0);
		try {
			store.setPixelsDimensionOrder(DimensionOrder.fromString(dimensionOrder),
				imageIndex);
		}
		catch (final EnumerationException e) {
			LOGGER.warn("Invalid dimension order: " + dimensionOrder, e);
		}
		try {
			store.setPixelsType(PixelType.fromString(pixelType), imageIndex);
		}
		catch (final EnumerationException e) {
			LOGGER.warn("Invalid pixel type: " + pixelType, e);
		}
		store.setPixelsSizeX(new PositiveInteger(sizeX), imageIndex);
		store.setPixelsSizeY(new PositiveInteger(sizeY), imageIndex);
		store.setPixelsSizeZ(new PositiveInteger(sizeZ), imageIndex);
		store.setPixelsSizeC(new PositiveInteger(sizeC), imageIndex);
		store.setPixelsSizeT(new PositiveInteger(sizeT), imageIndex);
		final int effSizeC = sizeC / samplesPerPixel;
		for (int i = 0; i < effSizeC; i++) {
			store.setChannelID(createLSID("Channel", imageIndex, i), imageIndex, i);
			store.setChannelSamplesPerPixel(new PositiveInteger(samplesPerPixel),
				imageIndex, i);
		}
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#setDefaultDateEnabled(boolean)
	 */
	public void setDefaultDateEnabled(final boolean enabled) {
		defaultDateEnabled = enabled;
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#
	 * setDefaultCreationDate(ome.xml.meta.MetadataStore, java.lang.String, int)
	 */
	public void setDefaultCreationDate(final MetadataStore store,
		final String id, final int imageIndex)
	{
		if (!defaultDateEnabled) {
			return;
		}
		final Location file =
			id == null ? null : new Location(getContext(), id).getAbsoluteFile();
		long time = System.currentTimeMillis();
		if (file != null && file.exists()) time = file.lastModified();
		store.setImageAcquisitionDate(new Timestamp(DateTools.convertDate(time,
			DateTools.UNIX)), imageIndex);
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#
	 * verifyMinimumPopulated(ome.xml.meta.MetadataRetrieve)
	 */
	public void verifyMinimumPopulated(final MetadataRetrieve src)
		throws FormatException
	{
		verifyMinimumPopulated(src, 0);
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#
	 * verifyMinimumPopulated(ome.xml.meta.MetadataRetrieve, int)
	 */
	public void verifyMinimumPopulated(final MetadataRetrieve src, final int n)
		throws FormatException
	{
		if (src == null) {
			throw new FormatException("Metadata object is null; "
				+ "call IFormatWriter.setMetadataRetrieve() first");
		}
		if (src instanceof MetadataStore && ((MetadataStore) src).getRoot() == null)
		{
			throw new FormatException("Metadata object has null root; "
				+ "call IMetadata.createRoot() first");
		}
		if (src.getImageID(n) == null) {
			throw new FormatException("Image ID #" + n + " is null");
		}
		if (src.getPixelsID(n) == null) {
			throw new FormatException("Pixels ID #" + n + " is null");
		}
		for (int i = 0; i < src.getChannelCount(n); i++) {
			if (src.getChannelID(n, i) == null) {
				throw new FormatException("Channel ID #" + i + " in Image #" + n +
					" is null");
			}
		}
		if (src.getPixelsBinDataBigEndian(n, 0) == null) {
			throw new FormatException("BigEndian #" + n + " is null");
		}
		if (src.getPixelsDimensionOrder(n) == null) {
			throw new FormatException("DimensionOrder #" + n + " is null");
		}
		if (src.getPixelsType(n) == null) {
			throw new FormatException("PixelType #" + n + " is null");
		}
		if (src.getPixelsSizeC(n) == null) {
			throw new FormatException("SizeC #" + n + " is null");
		}
		if (src.getPixelsSizeT(n) == null) {
			throw new FormatException("SizeT #" + n + " is null");
		}
		if (src.getPixelsSizeX(n) == null) {
			throw new FormatException("SizeX #" + n + " is null");
		}
		if (src.getPixelsSizeY(n) == null) {
			throw new FormatException("SizeY #" + n + " is null");
		}
		if (src.getPixelsSizeZ(n) == null) {
			throw new FormatException("SizeZ #" + n + " is null");
		}
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#
	 * makeSaneDimensionOrder(java.lang.String)
	 */
	public String makeSaneDimensionOrder(final String dimensionOrder) {
		String order = dimensionOrder.toUpperCase();
		order = order.replaceAll("[^XYZCT]", "");
		final String[] axes = new String[] { "X", "Y", "C", "Z", "T" };
		for (final String axis : axes) {
			if (order.indexOf(axis) == -1) order += axis;
			while (order.indexOf(axis) != order.lastIndexOf(axis)) {
				order = order.replaceFirst(axis, "");
			}
		}
		return order;
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#
	 * createLSID(java.lang.String, int[])
	 */
	public String createLSID(final String type, final int... indices) {
		final StringBuffer lsid = new StringBuffer(type);
		for (final int index : indices) {
			lsid.append(":");
			lsid.append(index);
		}
		return lsid.toString();
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#
	 * getExperimentType(java.lang.String)
	 */
	public ExperimentType getExperimentType(final String value)
		throws FormatException
	{
		final ExperimentTypeEnumHandler handler = new ExperimentTypeEnumHandler();
		try {
			return (ExperimentType) handler.getEnumeration(value);
		}
		catch (final EnumerationException e) {
			throw new FormatException("ExperimentType creation failed", e);
		}
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#getLaserType(java.lang.String)
	 */
	public LaserType getLaserType(final String value) throws FormatException {
		final LaserTypeEnumHandler handler = new LaserTypeEnumHandler();
		try {
			return (LaserType) handler.getEnumeration(value);
		}
		catch (final EnumerationException e) {
			throw new FormatException("LaserType creation failed", e);
		}
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#getLaserMedium(java.lang.String)
	 */
	public LaserMedium getLaserMedium(final String value) throws FormatException {
		final LaserMediumEnumHandler handler = new LaserMediumEnumHandler();
		try {
			return (LaserMedium) handler.getEnumeration(value);
		}
		catch (final EnumerationException e) {
			throw new FormatException("LaserMedium creation failed", e);
		}
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#getImmersion(java.lang.String)
	 */
	public Immersion getImmersion(final String value) throws FormatException {
		final ImmersionEnumHandler handler = new ImmersionEnumHandler();
		try {
			return (Immersion) handler.getEnumeration(value);
		}
		catch (final EnumerationException e) {
			throw new FormatException("Immersion creation failed", e);
		}
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#getCorrection(java.lang.String)
	 */
	public Correction getCorrection(final String value) throws FormatException {
		final CorrectionEnumHandler handler = new CorrectionEnumHandler();
		try {
			return (Correction) handler.getEnumeration(value);
		}
		catch (final EnumerationException e) {
			throw new FormatException("Correction creation failed", e);
		}
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#getDetectorType(java.lang.String)
	 */
	public DetectorType getDetectorType(final String value)
		throws FormatException
	{
		final DetectorTypeEnumHandler handler = new DetectorTypeEnumHandler();
		try {
			return (DetectorType) handler.getEnumeration(value);
		}
		catch (final EnumerationException e) {
			throw new FormatException("DetectorType creation failed", e);
		}
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#getBinning(java.lang.String)
	 */
	public Binning getBinning(final String value) throws FormatException {
		final BinningEnumHandler handler = new BinningEnumHandler();
		try {
			return (Binning) handler.getEnumeration(value);
		}
		catch (final EnumerationException e) {
			throw new FormatException("Binning creation failed", e);
		}
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#
	 * populateMetadata(ome.xml.meta.MetadataRetrieve, io.scif.Metadata)
	 */
	public void populateMetadata(final MetadataRetrieve retrieve,
		final Metadata meta)
	{

		final int numImages = retrieve.getImageCount();

		if (numImages > 0) meta.setDatasetName(retrieve.getImageName(0));

		meta.createImageMetadata(numImages);

		for (int i = 0; i < numImages; i++) {
			populateImageMetadata(retrieve, i, meta.get(i));
		}
	}

	/*
	 * @see ome.xml.services.OMEXMLMetadataService#
	 * populateImageMetadata(ome.xml.meta.MetadataRetrieve, int, io.scif.ImageMetadata)
	 */
	public void populateImageMetadata(final MetadataRetrieve retrieve,
		final int imageIndex, final ImageMetadata iMeta)
	{
		final int sizeX = retrieve.getPixelsSizeX(imageIndex).getValue();
		final int sizeY = retrieve.getPixelsSizeY(imageIndex).getValue();
		final int sizeZ = retrieve.getPixelsSizeZ(imageIndex).getValue();
		final int sizeC = retrieve.getPixelsSizeC(imageIndex).getValue();
		final int sizeT = retrieve.getPixelsSizeT(imageIndex).getValue();

		final String dimensionOrder =
			retrieve.getPixelsDimensionOrder(imageIndex).getValue();
		final boolean little = !retrieve.getPixelsBinDataBigEndian(imageIndex, 0);
		final String pixelType = retrieve.getPixelsType(imageIndex).getValue();

		final PositiveInteger spp =
			retrieve.getChannelCount(imageIndex) <= 0 ? null : retrieve
				.getChannelSamplesPerPixel(imageIndex, 0);

		final int rgbCCount = spp == null ? 1 : spp.getValue();

		SCIFIOMetadataTools.populateDimensions(iMeta, dimensionOrder, sizeX, sizeY,
			sizeZ, sizeC, sizeT);

		iMeta.setLittleEndian(little);
		iMeta.setPixelType(FormatTools.pixelTypeFromString(pixelType));
		iMeta.setBitsPerPixel(FormatTools.getBitsPerPixel(iMeta.getPixelType()));
		if (rgbCCount > 1) iMeta.setRGB(true);

		iMeta.setPlaneCount(sizeZ * (sizeC / rgbCCount) * sizeT);

		final PositiveFloat physX = retrieve.getPixelsPhysicalSizeX(imageIndex);
		final PositiveFloat physY = retrieve.getPixelsPhysicalSizeY(imageIndex);
		final PositiveFloat physZ = retrieve.getPixelsPhysicalSizeZ(imageIndex);
		final Double physT = retrieve.getPixelsTimeIncrement(imageIndex);

		if (physX != null) iMeta.getAxis(Axes.X).setCalibration(physX.getValue());
		if (physY != null) iMeta.getAxis(Axes.Y).setCalibration(physY.getValue());
		if (physZ != null) iMeta.getAxis(Axes.Z).setCalibration(physZ.getValue());
		if (physT != null) iMeta.getAxis(Axes.TIME).setCalibration(physT);
	}
}
