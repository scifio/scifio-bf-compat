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

import io.scif.MetadataLevel;
import io.scif.common.DateTools;
import io.scif.formats.DICOMFormat;
import io.scif.ome.xml.meta.OMEMetadata;
import io.scif.ome.xml.meta.OMEXMLMetadata;
import ome.xml.model.primitives.PositiveFloat;
import ome.xml.model.primitives.Timestamp;

import org.scijava.Priority;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

/**
 * Container class for translators between OME and DICOM formats.
 * 
 * @author Mark Hiner hinerm at gmail.com
 */
public class DICOMTranslator {

	/**
	 * Translator class from {@link io.scif.formats.DICOMFormat.Metadata} to
	 * {@link ome.xml.meta.OMEMetadata}
	 * <p>
	 * NB: Plugin priority is set to high to be selected over the base
	 * {@link io.scif.Metadata} translator.
	 * </p>
	 * 
	 * @author Mark Hiner
	 */
	@Plugin(type = ToOMETranslator.class, priority = Priority.HIGH_PRIORITY,
		attrs = {
			@Attr(name = DICOMOMETranslator.SOURCE,
				value = DICOMFormat.Metadata.CNAME),
			@Attr(name = DICOMOMETranslator.DEST, value = OMEMetadata.CNAME) })
	public static class DICOMOMETranslator extends
		ToOMETranslator<DICOMFormat.Metadata>
	{

		// -- Translator API Methods --

		/*
		 * @see OMETranslator#typedTranslate(io.scif.Metadata, io.scif.Metadata)
		 */
		@Override
		protected void typedTranslate(final DICOMFormat.Metadata source,
			final OMEMetadata dest)
		{
			// The metadata store we're working with.

			String stamp = null;

			final OMEXMLMetadata store = dest.getRoot();

			final String date = source.getDate();
			final String time = source.getTime();
			final String imageType = source.getImageType();
			final String pixelSizeX = source.getPixelSizeX();
			final String pixelSizeY = source.getPixelSizeY();
			final Double pixelSizeZ = source.getPixelSizeZ();

			if (date != null && time != null) {
				stamp = date + " " + time;
				stamp = DateTools.formatDate(stamp, "yyyy.MM.dd HH:mm:ss.SSSSSS");
			}

			if (stamp == null || stamp.trim().equals("")) stamp = null;

			for (int i = 0; i < source.getImageCount(); i++) {
				if (stamp != null) store.setImageAcquisitionDate(new Timestamp(stamp),
					i);
				store.setImageName("Series " + i, i);
			}

			if (source.getMetadataOptions().getMetadataLevel() != MetadataLevel.MINIMUM)
			{
				for (int i = 0; i < source.getImageCount(); i++) {
					store.setImageDescription(imageType, i);

					if (pixelSizeX != null) {
						final Double sizeX = new Double(pixelSizeX);
						if (sizeX > 0) {
							store.setPixelsPhysicalSizeX(new PositiveFloat(sizeX), i);
						}
						else {
							log().warn(
								"Expected positive value for PhysicalSizeX; got " + sizeX);
						}
					}
					if (pixelSizeY != null) {
						final Double sizeY = new Double(pixelSizeY);
						if (sizeY > 0) {
							store.setPixelsPhysicalSizeY(new PositiveFloat(sizeY), i);
						}
						else {
							log().warn(
								"Expected positive value for PhysicalSizeY; got " + sizeY);
						}
					}
					if (pixelSizeZ != null && pixelSizeZ > 0) {
						store.setPixelsPhysicalSizeZ(new PositiveFloat(pixelSizeZ), i);
					}
					else {
						log().warn(
							"Expected positive value for PhysicalSizeZ; got " + pixelSizeZ);
					}
				}
			}
		}
	}
}
