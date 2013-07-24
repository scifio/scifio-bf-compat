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
import io.scif.SCIFIO;

import java.io.File;

import javax.swing.JFileChooser;

/**
 * A simple manual test for the SCIFIO Bio-Formats compatibility layer.
 * 
 * @author Curtis Rueden
 */
public class ReadFile {

  public static void main(String[] args) throws FormatException {
    final SCIFIO scifio = new SCIFIO();

    final JFileChooser opener = new javax.swing.JFileChooser(System.getProperty("user.home"));
    int result = opener.showOpenDialog(null);
    if (result != JFileChooser.APPROVE_OPTION) return;
    final File file = opener.getSelectedFile();

    final Format format = scifio.format().getFormat(file.getAbsolutePath());
    System.out.println("file = " + file);
    System.out.println("format = " + format);
  }

}
