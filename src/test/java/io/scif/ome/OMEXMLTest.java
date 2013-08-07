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

package io.scif.ome;

import static org.junit.Assert.assertTrue;
import io.scif.FormatException;
import io.scif.Metadata;
import io.scif.SCIFIO;
import io.scif.ome.xml.meta.OMEXMLFormat;

import java.io.IOException;

import org.junit.Test;

/**
 * Test class to verify that metadata can be translated to OME-XML.
 * 
 * @author Mark Hiner
 */
public class OMEXMLTest {

	private final SCIFIO scifio = new SCIFIO();

	@Test
	public void extractOMEXML() throws FormatException, IOException {
		// Get Metadata describing a .fake image
		final String fakeId = "testImg&sizeX=512&sizeY=512.fake";
		final Metadata fakeMeta =
			scifio.initializer().initializeReader(fakeId).getMetadata();

		// Create omexmlformat metadata
		final Metadata omexmlMeta =
			scifio.format().getFormatFromClass(OMEXMLFormat.class).createMetadata();

		// Translate fake metadata to omexmlmetadata
		assertTrue(scifio.translator().translate(fakeMeta, omexmlMeta, false));

		// verify we have omexml
		final String omexml =
			((OMEXMLFormat.Metadata) omexmlMeta).getOMEMeta().getRoot().dumpXML();

		assertTrue(omexml.length() > 0);

		assertTrue(omexml
			.contains("<Pixels DimensionOrder=\"XYZCT\" ID=\"Pixels:0\" SizeC=\"1\" SizeT=\"1\" SizeX=\"512\" SizeY=\"512\" SizeZ=\"1\" Type=\"uint8\">"));
	}
}
