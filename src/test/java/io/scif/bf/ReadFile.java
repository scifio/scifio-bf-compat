/*
 * #%L
 * SCIFIO Bio-Formats compatibility format.
 * %%
 * Copyright (C) 2013 - 2014 Open Microscopy Environment:
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

package io.scif.bf;

import io.scif.Format;
import io.scif.FormatException;
import io.scif.Metadata;
import io.scif.Parser;
import io.scif.SCIFIO;
import io.scif.config.SCIFIOConfig;

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

	public static void main(final String[] args) throws Exception {
		final JFileChooser opener =
			new JFileChooser(System.getProperty("user.home"));
		final int result = opener.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
			readFile(opener.getSelectedFile());
		}
		System.exit(0);
	}

	private static void readFile(final File file) throws FormatException,
		IOException
	{
		final SCIFIO scifio = new SCIFIO();
		final Format format =
			scifio.format().getFormat(file.getAbsolutePath(),
				new SCIFIOConfig().checkerSetOpen(true));
		System.out.println("file = " + file);
		System.out.println("format = " + format.getFormatName());
		final Parser parser = format.createParser();
		final Metadata meta = parser.parse(file);
		for (int i = 0; i < meta.getImageCount(); i++) {
			System.out.println("image #" + i + " dimensions:");
			for (int a = 0; a < meta.get(i).getAxes().size(); a++) {
				final AxisType axisType = meta.get(i).getAxis(a).type();
				final long axisLength = meta.get(i).getAxisLength(a);
				System.out.println("\t" + axisLength + " : " + axisType);
			}
		}
	}

}
