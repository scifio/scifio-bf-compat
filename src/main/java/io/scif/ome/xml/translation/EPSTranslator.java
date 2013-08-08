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

import io.scif.formats.EPSFormat;
import io.scif.ome.xml.meta.OMEMetadata;
import io.scif.ome.xml.meta.OMEXMLMetadata;
import net.imglib2.meta.Axes;

import org.scijava.Priority;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

/**
 * Container class for translators between OME and EPS formats.
 * 
 * @author Mark Hiner hinerm at gmail.com
 */
public class EPSTranslator {

	/**
	 * Translator class from {@link io.scif.formats.EPSFormat.Metadata} to
	 * {@link ome.xml.meta.OMEMetadata}.
	 * <p>
	 * NB: Plugin priority is set to high to be selected over the base
	 * {@link io.scif.Metadata} translator.
	 * </p>
	 * 
	 * @author Mark Hiner
	 */
	@Plugin(type = FromOMETranslator.class, priority = Priority.HIGH_PRIORITY,
		attrs = { @Attr(name = OMEEPSTranslator.SOURCE, value = OMEMetadata.CNAME),
			@Attr(name = OMEEPSTranslator.DEST, value = EPSFormat.Metadata.CNAME) })
	public static class OMEEPSTranslator extends
		FromOMETranslator<EPSFormat.Metadata>
	{

		/*
		 * @see OMETranslator#typedTranslate(io.scif.Metadata, io.scif.Metadata)
		 */
		@Override
		protected void typedTranslate(final OMEMetadata source,
			final EPSFormat.Metadata dest)
		{
			super.typedTranslate(source, dest);

			final OMEXMLMetadata meta = source.getRoot();

			final int sizeX = meta.getPixelsSizeX(0).getValue().intValue();
			final int sizeY = meta.getPixelsSizeY(0).getValue().intValue();
			final int sizeC =
				meta.getChannelSamplesPerPixel(0, 0).getValue().intValue();

			dest.setAxisLength(0, Axes.X, sizeX);
			dest.setAxisLength(0, Axes.Y, sizeY);
			dest.setAxisLength(0, Axes.CHANNEL, sizeC);
		}
	}
}
