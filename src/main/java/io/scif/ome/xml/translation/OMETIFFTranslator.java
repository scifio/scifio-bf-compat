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

import io.scif.ome.xml.meta.OMEMetadata;
import io.scif.ome.xml.meta.OMETIFFFormat;

import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

/**
 * Container class for translators between OME and OMETIFF formats.
 * 
 * @author Mark Hiner hinerm at gmail.com
 */
public class OMETIFFTranslator {

	/**
	 * Translator class from {@link io.scif.formats.OMETIFFFormat.Metadata} to
	 * {@link ome.xml.meta.OMEMetadata}
	 * <p>
	 * NB: Plugin priority is set to high to be selected over the base
	 * {@link io.scif.Metadata} translator.
	 * </p>
	 * 
	 * @author Mark Hiner
	 */
	@Plugin(type = ToOMETranslator.class, priority = TIFFTranslator.PRIORITY + 1,
		attrs = {
			@Attr(name = OMEtoOMETIFFTranslator.SOURCE,
				value = OMETIFFFormat.Metadata.CNAME),
			@Attr(name = OMEtoOMETIFFTranslator.DEST, value = OMEMetadata.CNAME) })
	public static class OMEtoOMETIFFTranslator extends
		ToOMETranslator<OMETIFFFormat.Metadata>
	{

		// -- Translator API Methods --

		/*
		 * @see OMETranslator#typedTranslate(io.scif.Metadata, io.scif.Metadata)
		 */
		@Override
		protected void typedTranslate(final OMETIFFFormat.Metadata source,
			final OMEMetadata dest)
		{
			dest.setRoot(source.getOmeMeta().getRoot());
			super.typedTranslate(source, dest);
		}
	}

	/**
	 * Translator class from {@link io.scif.formats.OMETIFFFormat.Metadata} to
	 * {@link ome.xml.meta.OMEMetadata}.
	 * <p>
	 * NB: Plugin priority is set to high to be selected over the base
	 * {@link io.scif.Metadata} translator.
	 * </p>
	 * 
	 * @author Mark Hiner
	 */
	@Plugin(type = FromOMETranslator.class,
		priority = TIFFTranslator.PRIORITY + 1, attrs = {
			@Attr(name = OMETIFFtoOMETranslator.SOURCE, value = OMEMetadata.CNAME),
			@Attr(name = OMETIFFtoOMETranslator.DEST,
				value = OMETIFFFormat.Metadata.CNAME) })
	public static class OMETIFFtoOMETranslator extends
		FromOMETranslator<OMETIFFFormat.Metadata>
	{

		/*
		 * @see OMETranslator#typedTranslate(io.scif.Metadata, io.scif.Metadata)
		 */
		@Override
		protected void typedTranslate(final OMEMetadata source,
			final OMETIFFFormat.Metadata dest)
		{
			OMEMetadata omeMeta = dest.getOmeMeta();

			if (omeMeta == null) {
				omeMeta = new OMEMetadata(getContext(), source.getRoot());
				dest.setOmeMeta(omeMeta);
			}
			else omeMeta.setRoot(source.getRoot());

			super.typedTranslate(source, dest);
		}
	}
}
