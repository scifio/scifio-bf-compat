/*
 * #%L
 * SCIFIO Bio-Formats compatibility format.
 * %%
 * Copyright (C) 2013 - 2022 SCIFIO developers.
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

package io.scif.bf.ome;

import io.scif.Metadata;
import io.scif.bf.BioFormatsFormat;
import io.scif.ome.OMEMetadata;
import io.scif.ome.translators.ToOMETranslator;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import loci.formats.meta.MetadataStore;
import loci.formats.ome.OMEXMLMetadata;

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
		protected void translateFormatMetadata(final BioFormatsFormat.Metadata source,
			final OMEMetadata dest)
		{
			// Just copy the root over
			MetadataStore store = source.getMetadataStore();
			if (store != null && store instanceof ome.xml.meta.OMEXMLMetadata) {
				dest.setRoot((OMEXMLMetadata) store);
			}
		}
	}
}
