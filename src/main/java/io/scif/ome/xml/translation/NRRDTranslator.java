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
import io.scif.formats.NRRDFormat;
import io.scif.ome.xml.meta.OMEMetadata;
import ome.xml.model.primitives.PositiveFloat;

import org.scijava.Priority;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

/**
 * Container class for translators between OME and NRRD formats.
 * 
 * @author Mark Hiner hinerm at gmail.com
 */
public class NRRDTranslator {

	/**
	 * Translator class from {@link io.scif.formats.NRRDFormat.Metadata} to
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
			@Attr(name = NRRDOMETranslator.SOURCE, value = NRRDFormat.Metadata.CNAME),
			@Attr(name = NRRDOMETranslator.DEST, value = OMEMetadata.CNAME) })
	public static class NRRDOMETranslator extends
		ToOMETranslator<NRRDFormat.Metadata>
	{

		// -- Translator API Methods --

		@Override
		protected void typedTranslate(final NRRDFormat.Metadata source,
			final OMEMetadata dest)
		{

			if (source.getMetadataOptions().getMetadataLevel() != MetadataLevel.MINIMUM)
			{

				final String[] pixelSizes = source.getPixelSizes();

				if (pixelSizes != null) {
					for (int i = 0; i < pixelSizes.length; i++) {
						if (pixelSizes[i] == null) continue;
						try {
							final Double d = new Double(pixelSizes[i].trim());
							if (d > 0) {
								if (i == 0) {
									dest.getRoot()
										.setPixelsPhysicalSizeX(new PositiveFloat(d), 0);
								}
								else if (i == 1) {
									dest.getRoot()
										.setPixelsPhysicalSizeY(new PositiveFloat(d), 0);
								}
								else if (i == 2) {
									dest.getRoot()
										.setPixelsPhysicalSizeZ(new PositiveFloat(d), 0);
								}
							}
							else {
								log().warn("Expected positive value for PhysicalSize; got " +
									d);
							}
						}
						catch (final NumberFormatException e) {}
					}
				}
			}
		}
	}
}
