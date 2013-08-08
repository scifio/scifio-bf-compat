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

import io.scif.formats.TIFFFormat;
import io.scif.ome.xml.meta.OMEMetadata;
import io.scif.ome.xml.meta.OMEXMLMetadata;
import ome.xml.model.primitives.PositiveFloat;
import ome.xml.model.primitives.Timestamp;

import org.scijava.Priority;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

/**
 * Container class for translators between OME and TIFF formats.
 * 
 * @author Mark Hiner hinerm at gmail.com
 */
public class TIFFTranslator {

	// -- Constants --

	public static final double PRIORITY = Priority.HIGH_PRIORITY;

	/**
	 * Translator class from {@link io.scif.formats.TIFFFormat.Metadata} to
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
		priority = TIFFTranslator.PRIORITY,
		attrs = {
			@Attr(name = TIFFOMETranslator.SOURCE, value = TIFFFormat.Metadata.CNAME),
			@Attr(name = TIFFOMETranslator.DEST, value = OMEMetadata.CNAME) })
	public static class TIFFOMETranslator extends
		ToOMETranslator<TIFFFormat.Metadata>
	{

		// -- Translator API Methods --

		/*
		 * @see OMETranslator#typedTranslate(io.scif.Metadata, io.scif.Metadata)
		 */
		@Override
		protected void typedTranslate(final TIFFFormat.Metadata source,
			final OMEMetadata dest)
		{
			super.typedTranslate(source, dest);

			final OMEXMLMetadata meta = dest.getRoot();

			final double physX = source.getPhysicalSizeX();
			final double physY = source.getPhysicalSizeY();
			final double physZ = source.getPhysicalSizeZ();

			meta
				.setPixelsPhysicalSizeX(new PositiveFloat(physX > 0 ? physX : 1.0), 0);
			meta
				.setPixelsPhysicalSizeY(new PositiveFloat(physY > 0 ? physY : 1.0), 0);
			meta
				.setPixelsPhysicalSizeZ(new PositiveFloat(physZ > 0 ? physZ : 1.0), 0);
			meta.setImageDescription(source.getDescription(), 0);
			meta.setExperimenterFirstName(source.getExperimenterFirstName(), 0);
			meta.setExperimenterLastName(source.getExperimenterLastName(), 0);
			meta.setExperimenterEmail(source.getExperimenterEmail(), 0);

			final String creationDate = source.getCreationDate();
			if (creationDate != null) meta.setImageAcquisitionDate(new Timestamp(
				creationDate), 0);
		}
	}

	/**
	 * Translator class from {@link io.scif.formats.TIFFFormat.Metadata} to
	 * {@link ome.xml.meta.OMEMetadata}.
	 * <p>
	 * NB: Plugin priority is set to high to be selected over the base
	 * {@link io.scif.Metadata} translator.
	 * </p>
	 * 
	 * @author Mark Hiner
	 */
	@Plugin(type = FromOMETranslator.class, priority = TIFFTranslator.PRIORITY,
		attrs = {
			@Attr(name = OMETIFFTranslator.SOURCE, value = OMEMetadata.CNAME),
			@Attr(name = OMETIFFTranslator.DEST, value = TIFFFormat.Metadata.CNAME) })
	public static class OMETIFFTranslator extends
		FromOMETranslator<TIFFFormat.Metadata>
	{

		/*
		 * @see OMETranslator#typedTranslate(io.scif.Metadata, io.scif.Metadata)
		 */
		@Override
		protected void typedTranslate(final OMEMetadata source,
			final TIFFFormat.Metadata dest)
		{
			super.typedTranslate(source, dest);

			final OMEXMLMetadata meta = source.getRoot();

			if (meta.getPixelsBinDataCount(0) > 0) {
				dest.setPhysicalSizeX(checkValue(meta.getPixelsPhysicalSizeX(0)));
				dest.setPhysicalSizeY(checkValue(meta.getPixelsPhysicalSizeY(0)));
				dest.setPhysicalSizeZ(checkValue(meta.getPixelsPhysicalSizeZ(0)));
			}

			if (meta.getImageCount() > 0) dest.setImageDescription(meta
				.getImageDescription(0));

			if (meta.getExperimentCount() > 0) {
				dest.setExperimenterEmail(meta.getExperimenterEmail(0));
				dest.setExperimenterFirstName(meta.getExperimenterFirstName(0));
				dest.setExperimenterLastName(meta.getExperimenterLastName(0));
				dest.setCreationDate(meta.getImageAcquisitionDate(0).getValue());
			}
		}

		private double checkValue(final PositiveFloat f) {
			if (f == null) return 1.0;
			return f.getValue();
		}
	}
}
