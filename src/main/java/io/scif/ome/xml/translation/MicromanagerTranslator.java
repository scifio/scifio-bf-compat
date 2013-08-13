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

package io.scif.ome.xml.translation;

import io.scif.FormatException;
import io.scif.MetadataLevel;
import io.scif.common.DateTools;
import io.scif.formats.MicromanagerFormat;
import io.scif.formats.MicromanagerFormat.Metadata;
import io.scif.formats.MicromanagerFormat.Position;
import io.scif.io.Location;
import io.scif.ome.xml.meta.OMEMetadata;
import io.scif.ome.xml.meta.OMEXMLMetadata;
import io.scif.ome.xml.services.OMEXMLMetadataService;

import java.io.IOException;
import java.util.Vector;

import ome.xml.model.primitives.PositiveFloat;
import ome.xml.model.primitives.Timestamp;

import org.scijava.Priority;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

/**
 * Container class for translators between OME and Micromanager formats.
 * 
 * @author Mark Hiner hinerm at gmail.com
 */
public class MicromanagerTranslator {

	/**
	 * Translator class from {@link ome.xml.meta.OMEMetadata} to
	 * {@link io.scif.formats.MicromanagerFormat.Metadata}.
	 * <p>
	 * NB: Plugin priority is set to high to be selected over the base
	 * {@link io.scif.Metadata} translator.
	 * </p>
	 * 
	 * @author Mark Hiner
	 */
	@Plugin(type = FromOMETranslator.class, priority = Priority.HIGH_PRIORITY,
		attrs = {
			@Attr(name = MicromanagerOMETranslator.SOURCE,
				value = MicromanagerFormat.Metadata.CNAME),
			@Attr(name = MicromanagerOMETranslator.DEST, value = OMEMetadata.CNAME) })
	public static class MicromanagerOMETranslator extends
		ToOMETranslator<MicromanagerFormat.Metadata>
	{

		/*
		 * @see OMETranslator#typedTranslate(io.scif.Metadata, io.scif.Metadata)
		 */
		@Override
		protected void typedTranslate(final MicromanagerFormat.Metadata source,
			final OMEMetadata dest)
		{
			super.typedTranslate(source, dest);

			try {
				populateMetadata(source, dest.getRoot());
			}
			catch (final FormatException e) {
				log().error(
					"Error populating Metadata store with Micromanager metadata", e);
			}
			catch (final IOException e) {
				log().error(
					"Error populating Metadata store with Micromanager metadata", e);
			}
		}

		private void populateMetadata(final Metadata meta,
			final OMEXMLMetadata store) throws FormatException, IOException
		{
			final OMEXMLMetadataService service =
				scifio().get(OMEXMLMetadataService.class);
			final String instrumentID = service.createLSID("Instrument", 0);
			store.setInstrumentID(instrumentID, 0);
			final Vector<Position> positions = meta.getPositions();

			for (int i = 0; i < positions.size(); i++) {
				final Position p = positions.get(i);
				if (p.time != null) {
					final String date =
						DateTools.formatDate(p.time, MicromanagerFormat.Parser.DATE_FORMAT);
					if (date != null) {
						store.setImageAcquisitionDate(new Timestamp(date), i);
					}
				}

				if (positions.size() > 1) {
					final Location parent =
						new Location(getContext(), p.metadataFile).getParentFile();
					store.setImageName(parent.getName(), i);
				}

				if (meta.getMetadataOptions().getMetadataLevel() != MetadataLevel.MINIMUM)
				{
					store.setImageDescription(p.comment, i);

					// link Instrument and Image
					store.setImageInstrumentRef(instrumentID, i);

					for (int c = 0; c < p.channels.length; c++) {
						store.setChannelName(p.channels[c], i, c);
					}

					if (p.pixelSize != null && p.pixelSize > 0) {
						store.setPixelsPhysicalSizeX(new PositiveFloat(p.pixelSize), i);
						store.setPixelsPhysicalSizeY(new PositiveFloat(p.pixelSize), i);
					}
					else {
						log().warn("Expected positive value for PhysicalSizeX; got " +
							p.pixelSize);
					}
					if (p.sliceThickness != null && p.sliceThickness > 0) {
						store
							.setPixelsPhysicalSizeZ(new PositiveFloat(p.sliceThickness), i);
					}
					else {
						log().warn("Expected positive value for PhysicalSizeZ; got " +
							p.sliceThickness);
					}

					int nextStamp = 0;
					for (int q = 0; q < meta.getPlaneCount(i); q++) {
						store.setPlaneExposureTime(p.exposureTime, i, q);
						final String tiff = positions.get(i).getFile(meta, i, q);
						if (tiff != null && new Location(getContext(), tiff).exists() &&
							nextStamp < p.timestamps.length)
						{
							store.setPlaneDeltaT(p.timestamps[nextStamp++], i, q);
						}
					}

					final String serialNumber = p.detectorID;
					p.detectorID = service.createLSID("Detector", 0, i);

					for (int c = 0; c < p.channels.length; c++) {
						store.setDetectorSettingsBinning(service.getBinning(p.binning), i,
							c);
						store.setDetectorSettingsGain(new Double(p.gain), i, c);
						if (c < p.voltage.size()) {
							store.setDetectorSettingsVoltage(p.voltage.get(c), i, c);
						}
						store.setDetectorSettingsID(p.detectorID, i, c);
					}

					store.setDetectorID(p.detectorID, 0, i);
					if (p.detectorModel != null) {
						store.setDetectorModel(p.detectorModel, 0, i);
					}

					if (serialNumber != null) {
						store.setDetectorSerialNumber(serialNumber, 0, i);
					}

					if (p.detectorManufacturer != null) {
						store.setDetectorManufacturer(p.detectorManufacturer, 0, i);
					}

					if (p.cameraMode == null) p.cameraMode = "Other";
					store.setDetectorType(service.getDetectorType(p.cameraMode), 0, i);
					store.setImagingEnvironmentTemperature(p.temperature, i);
				}
			}
		}

	}
}
