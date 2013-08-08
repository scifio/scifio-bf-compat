
package io.scif.ome.xml.translation;

import io.scif.Metadata;
import io.scif.ome.xml.meta.OMEMetadata;
import io.scif.ome.xml.services.OMEXMLMetadataService;

import org.scijava.Priority;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

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

/**
 * Basic translator for OME Metadata. Uses ImageMetadata to populate a
 * MetadataStore.
 * 
 * @author Mark Hiner hinerm at gmail.com
 */
@Plugin(type = ToOMETranslator.class, priority = Priority.NORMAL_PRIORITY,
	attrs = { @Attr(name = DefaultOMETranslator.SOURCE, value = Metadata.CNAME),
		@Attr(name = DefaultOMETranslator.DEST, value = OMEMetadata.CNAME) })
public class DefaultOMETranslator extends ToOMETranslator<Metadata> {

	// -- Translator API Methods --

	/*
	 * @see OMETranslator#typedTranslate(io.scif.Metadata, io.scif.Metadata)
	 */
	@Override
	protected void typedTranslate(final Metadata source, final OMEMetadata dest) {
		for (int i = 0; i < source.getImageCount(); i++) {
			scifio().get(OMEXMLMetadataService.class).populateMetadata(
				dest.getRoot(), 0, source.getDatasetName(), source);
		}
	}
}
