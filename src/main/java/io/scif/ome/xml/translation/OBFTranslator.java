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

import io.scif.ImageMetadata;
import io.scif.formats.OBFFormat;
import io.scif.ome.xml.meta.OMEMetadata;

import java.util.List;

import net.imglib2.meta.Axes;
import ome.xml.model.primitives.PositiveFloat;

import org.scijava.Priority;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

/**
 * Container class for translators between OME and OBF formats.
 * 
 * @author Mark Hiner hinerm at gmail.com
 */
public class OBFTranslator {

	/**
	 * Translator class from {@link io.scif.formats.OBFFormat.Metadata} to
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
			@Attr(name = OBFOMETranslator.SOURCE, value = OBFFormat.Metadata.CNAME),
			@Attr(name = OBFOMETranslator.DEST, value = OMEMetadata.CNAME) })
	public static class OBFOMETranslator extends
		ToOMETranslator<OBFFormat.Metadata>
	{

		// -- Translator API methods --

		@Override
		protected void typedTranslate(final OBFFormat.Metadata source,
			final OMEMetadata dest)
		{
			for (int image = 0; image != source.getImageCount(); ++image) {
				final ImageMetadata obf = source.get(image);

				final String name = obf.getTable().get("Name").toString();
				dest.getRoot().setImageName(name, image);

				@SuppressWarnings("unchecked")
				final List<Double> lengths =
					(List<Double>) obf.getTable().get("Lengths");

				final double lengthX = Math.abs(lengths.get(0));
				if (lengthX > 0) {
					final PositiveFloat physicalSizeX =
						new PositiveFloat(lengthX / obf.getAxisLength(Axes.X));
					dest.getRoot().setPixelsPhysicalSizeX(physicalSizeX, image);
				}
				final double lengthY = Math.abs(lengths.get(1));
				if (lengthY > 0) {
					final PositiveFloat physicalSizeY =
						new PositiveFloat(lengthY / obf.getAxisLength(Axes.Y));
					dest.getRoot().setPixelsPhysicalSizeY(physicalSizeY, image);
				}
				final double lengthZ = Math.abs(lengths.get(2));
				if (lengthZ > 0) {
					final PositiveFloat physicalSizeZ =
						new PositiveFloat(lengthZ / obf.getAxisLength(Axes.Z));
					dest.getRoot().setPixelsPhysicalSizeZ(physicalSizeZ, image);
				}
			}
		}
	}
}
