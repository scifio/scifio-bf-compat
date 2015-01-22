/*
 * #%L
 * SCIFIO Bio-Formats compatibility format.
 * %%
 * Copyright (C) 2013 - 2015 Board of Regents of the University of
 * Wisconsin-Madison
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

package io.scif.bf.ome;

import loci.formats.ome.OMEXMLMetadata;
import io.scif.Metadata;
import io.scif.bf.BioFormatsFormat;
import io.scif.ome.OMEMetadata;
import io.scif.ome.translators.ToOMETranslator;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;

/**
 * Container class for translators between BioFormats and OME formats.
 * 
 * @author Mark Hiner hinerm at gmail.com
 */
public class BioFormatsTranslator {

	/**
	 * Translator class from {@link io.scif.bf.BioFormatsFormat.Metadata} to
	 * {@link OMEMetadata}
	 * <p>
	 * NB: Plugin priority is set to "high + 1" to be selected over the
	 * SCIFIO-OME-XML translators.
	 * </p>
	 * 
	 * @author Mark Hiner
	 */
	@Plugin(type = ToOMETranslator.class, priority = Priority.HIGH_PRIORITY + 1)
	public static class BioFormatsOMETranslator extends
		ToOMETranslator<BioFormatsFormat.Metadata>
	{

		// -- Translator API Methods --

		@Override
		public Class<? extends Metadata> source() {
			return BioFormatsFormat.Metadata.class;
		}

		@Override
		public Class<? extends Metadata> dest() {
			return OMEMetadata.class;
		}

		@Override
		protected void translateOMEXML(final BioFormatsFormat.Metadata source,
			final OMEMetadata dest)
		{
			// Just copy the root over
			Object root = source.getReader().getMetadataStore();
			if (root != null && root instanceof ome.xml.meta.OMEXMLMetadata) {
				dest.setRoot((OMEXMLMetadata) root);
			}
		}
	}
}
