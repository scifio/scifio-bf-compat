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

import static org.junit.Assert.assertEquals;
import io.scif.FormatException;
import io.scif.Metadata;
import io.scif.SCIFIO;
import io.scif.ome.xml.meta.OMEMetadata;

import java.io.IOException;

import net.imglib2.meta.Axes;

import org.junit.Test;

/**
 * Unit tests to ensure calibration values are preserved properly when
 * translating to/from the OME classes.
 * 
 * @author Mark Hiner
 */
public class CalibrationTest {

	private final SCIFIO scifio = new SCIFIO();
	private String id = "testImg&sizeX=512&sizeY=512.fake";

	// Try setting calibration values, translate to OMEMetadata and
	// verify they persisted
	@Test
	public void testTranslateToOME() throws IOException, FormatException {
		// Parse source metadata
		Metadata meta = scifio.initializer().parseMetadata(id);

		// Adjust calibration
		meta.getAxis(0, Axes.X).setCalibration(5.0);
		meta.getAxis(0, Axes.Y).setCalibration(6.0);
		meta.getAxis(0, Axes.Z).setCalibration(7.0);
		meta.getAxis(0, Axes.TIME).setCalibration(8.0);

		OMEMetadata omeMeta = new OMEMetadata(scifio.getContext());

		// Translate to OMEMetadata
		scifio.translator().translate(meta, omeMeta, false);

		// Verify results
		assertEquals(omeMeta.getRoot().getPixelsPhysicalSizeX(0).getValue(),
			new Double(5.0));
		assertEquals(omeMeta.getRoot().getPixelsPhysicalSizeY(0).getValue(),
			new Double(6.0));
		assertEquals(omeMeta.getRoot().getPixelsPhysicalSizeZ(0).getValue(),
			new Double(7.0));
		assertEquals(omeMeta.getRoot().getPixelsTimeIncrement(0),
			new Double(8.0));
	}
	
}
