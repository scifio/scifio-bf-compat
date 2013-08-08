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

import io.scif.AbstractTranslator;
import io.scif.Metadata;
import io.scif.Translator;

/**
 * Abstract base class for all {@link io.scif.Translator} implementations that
 * operate on {@link ome.xml.meta.OMEMetadata}.
 * 
 * @see io.scif.Translator
 * @see ome.xml.meta.OMEMetadata
 * @author Mark Hiner
 */
public abstract class OMETranslator<M extends Metadata, N extends Metadata>
	extends AbstractTranslator<M, N>
{

	/*
	 * Before invoking the OME-specific translation, perform the base
	 * Metadata-level translation.
	 */
	@Override
	protected void typedTranslate(final M source, final N dest) {
		final Translator t =
			scifio().translator().findTranslator(Metadata.class, dest.getClass(),
				true);

		t.translate(source, dest);
	}
}
