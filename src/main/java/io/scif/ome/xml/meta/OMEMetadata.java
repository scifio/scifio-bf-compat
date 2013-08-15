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

package io.scif.ome.xml.meta;

import io.scif.AbstractMetadata;
import io.scif.ome.xml.services.OMEXMLMetadataService;
import io.scif.ome.xml.services.OMEXMLService;
import io.scif.services.ServiceException;

import org.scijava.Context;

/**
 * io.scif.Metadata class wrapping an OME-XML root.
 * 
 * @see ome.xml.meta.OMEXMLMetadata
 * @see io.scif.Metadata
 * @author Mark Hiner
 */
public class OMEMetadata extends AbstractMetadata {

	// -- Constants --

	public static final String FORMAT_NAME = "OME-XML";
	public static final String CNAME = "io.scif.ome.xml.meta.OMEMetadata";

	// -- Fields --

	/** OME core */
	protected OMEXMLMetadata root;

	// -- Constructor --

	public OMEMetadata(final Context context) {
		this(context, null);
	}

	public OMEMetadata(final Context context, final OMEXMLMetadata root) {
		setContext(context);
		setRoot(root);
	}

	// -- Metadata API Methods --

	/*
	 * @see io.scif.AbstractMetadata#getFormatName()
	 */
	public String getFormatName() {
		return FORMAT_NAME;
	}

	/*
	 * @see io.scif.AbstractMetadata#populateImageMetadata()
	 */
	public void populateImageMetadata() {
		getContext().getService(OMEXMLMetadataService.class).populateMetadata(
			getRoot(), this);
	}

	// -- Helper Methods --

	/**
	 * Sets the root for this Metadata
	 */
	public void setRoot(final OMEXMLMetadata root) {
		this.root = root;
	}

	/**
	 * Returns the root of this Metadata
	 */
	public OMEXMLMetadata getRoot() {
		if (root == null) {
			final OMEXMLService service =
				scifio().format().getInstance(OMEXMLService.class);
			try {
				root = service.createOMEXMLMetadata();
			}
			catch (final ServiceException e) {
				log().debug("Failed to get OME-XML Service", e);
			}
		}
		return root;
	}
}
