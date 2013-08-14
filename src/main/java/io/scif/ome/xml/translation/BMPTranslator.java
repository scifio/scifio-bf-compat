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
import io.scif.formats.BMPFormat;
import io.scif.ome.xml.meta.OMEMetadata;
import ome.xml.model.primitives.PositiveFloat;

import org.scijava.Priority;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

/**
 * Container class for translators between OME and BMP formats.
 * 
 * @author Mark Hiner hinerm at gmail.com
 */
public class BMPTranslator {

	/**
	 * Translator class from {@link io.scif.formats.BMPFormat.Metadata} to
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
			@Attr(name = BMPOMETranslator.SOURCE, value = BMPFormat.Metadata.CNAME),
			@Attr(name = BMPOMETranslator.DEST, value = OMEMetadata.CNAME) })
	public static class BMPOMETranslator extends
		ToOMETranslator<BMPFormat.Metadata>
	{

		// -- Translator API Methods --

		/*
		 * @see OMETranslator#typedTranslate(io.scif.Metadata, io.scif.Metadata)
		 */
		@Override
		protected void typedTranslate(final BMPFormat.Metadata source,
			final OMEMetadata dest)
		{
			if (source.getMetadataOptions().getMetadataLevel() != MetadataLevel.MINIMUM)
			{
				// resolution is stored as pixels per meter; we want to convert to
				// microns per pixel

				final int pixelSizeX = (Integer) source.getTable().get("X resolution");

				final int pixelSizeY = (Integer) source.getTable().get("Y resolution");

				final double correctedX =
					pixelSizeX == 0 ? 0.0 : 1000000.0 / pixelSizeX;
				final double correctedY =
					pixelSizeY == 0 ? 0.0 : 1000000.0 / pixelSizeY;

				if (correctedX > 0) {
					dest.getRoot().setPixelsPhysicalSizeX(new PositiveFloat(correctedX),
						0);
				}
				else {
					log().warn(
						"Expected positive value for PhysicalSizeX; got " + correctedX);
				}
				if (correctedY > 0) {
					dest.getRoot().setPixelsPhysicalSizeY(new PositiveFloat(correctedY),
						0);
				}
				else {
					log().warn(
						"Expected positive value for PhysicalSizeY; got " + correctedY);
				}
			}
		}
	}
}
