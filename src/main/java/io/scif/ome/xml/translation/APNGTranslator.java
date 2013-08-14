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
import io.scif.formats.APNGFormat;
import io.scif.ome.xml.meta.OMEMetadata;
import io.scif.ome.xml.services.OMEXMLMetadataService;
import io.scif.util.FormatTools;

import org.scijava.Priority;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

/**
 * Container class for translators between OME and APNG formats.
 * 
 * @author Mark Hiner hinerm at gmail.com
 */
public class APNGTranslator {

	/**
	 * Translator class from {@link io.scif.formats.APNGFormat.Metadata} to
	 * {@link ome.xml.meta.OMEMetadata}.
	 * <p>
	 * NB: Plugin priority is set to high to be selected over the base
	 * {@link io.scif.Metadata} translator.
	 * </p>
	 * 
	 * @author Mark Hiner
	 */
	@Plugin(type = FromOMETranslator.class, priority = Priority.HIGH_PRIORITY,
		attrs = {
			@Attr(name = OMEAPNGTranslator.SOURCE, value = OMEMetadata.CNAME),
			@Attr(name = OMEAPNGTranslator.DEST, value = APNGFormat.Metadata.CNAME) })
	public static class OMEAPNGTranslator extends
		FromOMETranslator<APNGFormat.Metadata>
	{

		/*
		 * @see OMETranslator#typedTranslate(io.scif.Metadata, io.scif.Metadata)
		 */
		@Override
		protected void typedTranslate(final OMEMetadata source,
			final APNGFormat.Metadata dest)
		{
			super.typedTranslate(source, dest);

			if (dest.getFctl() != null && dest.getFctl().size() > 0) {
				final Double timeIncrement = source.getRoot().getPixelsTimeIncrement(0);

				final short tIncrement = 1;
				if (timeIncrement != null && !timeIncrement.isNaN() &&
					!timeIncrement.isInfinite()) Short.parseShort(Double
					.toString(timeIncrement));

				dest.getFctl().get(0).setDelayNum(tIncrement);
				dest.getFctl().get(0).setDelayDen((short) 1);
			}
		}

	}

	/**
	 * Translator class from {@link io.scif.formats.APNGFormat.Metadata} to
	 * {@link ome.xml.meta.OMEMetadata}
	 * <p>
	 * NB: Plugin priority is set to high to be selected over the base
	 * {@link io.scif.Metadata} translator.
	 * </p>
	 * 
	 * @author Mark Hiner
	 */
	@Plugin(
		type = ToOMETranslator.class,
		priority = Priority.HIGH_PRIORITY,
		attrs = {
			@Attr(name = APNGOMETranslator.SOURCE, value = APNGFormat.Metadata.CNAME),
			@Attr(name = APNGOMETranslator.DEST, value = OMEMetadata.CNAME) })
	public static class APNGOMETranslator extends
		ToOMETranslator<APNGFormat.Metadata>
	{

		// -- Translator API Methods --

		/*
		 * @see OMETranslator#typedTranslate(io.scif.Metadata, io.scif.Metadata)
		 */
		@Override
		protected void typedTranslate(final APNGFormat.Metadata source,
			final OMEMetadata dest)
		{
			int sizeC = 1;

			switch (source.getIhdr().getColourType()) {
				case 0x2:
					sizeC = 3;
					break;
				case 0x4:
					sizeC = 2;
					break;
				case 0x6:
					sizeC = 4;
					break;
				default:
					break;
			}

			final String dimOrder = "XYCTZ";
			final int sizeX = source.getIhdr().getWidth();
			final int sizeY = source.getIhdr().getHeight();
			final int sizeT =
				source.getActl() == null ? 1 : source.getActl().getNumFrames();
			final int sizeZ = 1;
			String pixelType = null;
			try {
				pixelType =
					FormatTools.getPixelTypeString(FormatTools.pixelTypeFromBytes(source
						.getIhdr().getBitDepth() / 8, false, false));
			}
			catch (final FormatException e) {
				log().debug("Failed to find pixel type from bytes: " +
					(source.getIhdr().getBitDepth() / 8), e);
			}
			final boolean littleEndian = false;
			final int series = 0;

			// TODO what should these be in APNG?
			final int samplesPerPixel = 1;
			// = sizeC / effectiveSizeC... just sizeC for APNG? #planes / Z * T
			final String imageName = "";

			getContext().getService(OMEXMLMetadataService.class).populateMetadata(
				dest.getRoot(), series, imageName, littleEndian, dimOrder, pixelType,
				sizeX, sizeY, sizeZ, sizeC, sizeT, 1.0, 1.0, 1.0, 1.0, 1.0,
				samplesPerPixel);

			if (source.getFctl() != null && source.getFctl().size() > 0) dest
				.getRoot().setPixelsTimeIncrement(
					(double) source.getFctl().get(0).getDelayNum() /
						source.getFctl().get(0).getDelayDen(), 0);
		}
	}
}
