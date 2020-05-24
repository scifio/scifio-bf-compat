/*
 * #%L
 * SCIFIO Bio-Formats compatibility format.
 * %%
 * Copyright (C) 2013 - 2020 SCIFIO developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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

import net.imagej.axis.AxisType;

import org.scijava.io.location.FileLocation;

/**
 * A simple manual test for the SCIFIO Bio-Formats compatibility layer.
 *
 * @author Curtis Rueden
 */
public class ReadFile {

	public static void main(final String[] args) throws Exception {
		final JFileChooser opener = new JFileChooser(System.getProperty(
			"user.home"));
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
		final FileLocation id = new FileLocation(file);
		final Format format = scifio.format().getFormat(id, new SCIFIOConfig()
			.checkerSetOpen(true));
		System.out.println("file = " + file);
		System.out.println("format = " + format.getFormatName());
		final Parser parser = format.createParser();
		final Metadata meta = parser.parse(id);
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
