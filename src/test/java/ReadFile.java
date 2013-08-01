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

import io.scif.Format;
import io.scif.FormatException;
import io.scif.Metadata;
import io.scif.Parser;
import io.scif.SCIFIO;

import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;

import net.imglib2.meta.AxisType;

/**
 * A simple manual test for the SCIFIO Bio-Formats compatibility layer.
 * 
 * @author Curtis Rueden
 */
public class ReadFile {

	public static void main(final String[] args) throws FormatException,
		IOException
	{
		final SCIFIO scifio = new SCIFIO();

		final JFileChooser opener =
			new javax.swing.JFileChooser(System.getProperty("user.home"));
		final int result = opener.showOpenDialog(null);
		if (result != JFileChooser.APPROVE_OPTION) return;
		final File file = opener.getSelectedFile();

		final Format format = scifio.format().getFormat(file.getAbsolutePath());
		System.out.println("file = " + file);
		System.out.println("format = " + format);
		final Parser parser = format.createParser();
		final Metadata meta = parser.parse(file);
		for (int i = 0; i < meta.getImageCount(); i++) {
			System.out.println("image #" + i + " dimensions:");
			for (int a = 0; a < meta.getAxisCount(i); a++) {
				final AxisType axisType = meta.getAxisType(i, a);
				final int axisLength = meta.getAxisLength(i, a);
				System.out.println("\t" + axisLength + " : " + axisType);
			}
		}
	}

}
