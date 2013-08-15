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

import io.scif.AbstractSCIFIOComponent;
import io.scif.common.Constants;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import ome.xml.DOMUtil;
import ome.xml.model.OME;
import ome.xml.model.OMEModelObject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A utility class for constructing and manipulating OME-XML DOMs. It is the
 * superclass for all versions of OME-XML. It requires the ome.xml package to
 * compile (part of ome-xml.jar).
 * <dl>
 * <dt><b>Source code:</b></dt>
 * <dd><a href=
 * "http://trac.openmicroscopy.org.uk/ome/browser/bioformats.git/components/bio-formats/src/loci/formats/ome/AbstractOMEXMLMetadata.java"
 * >Trac</a>, <a href=
 * "http://git.openmicroscopy.org/?p=bioformats.git;a=blob;f=components/bio-formats/src/loci/formats/ome/AbstractOMEXMLMetadata.java;hb=HEAD"
 * >Gitweb</a></dd>
 * </dl>
 * 
 * @author Curtis Rueden ctrueden at wisc.edu
 * @author Melissa Linkert melissa at glencoesoftware.com
 */
public abstract class AbstractOMEXMLMetadata extends AbstractSCIFIOComponent
	implements OMEXMLMetadata
{

	// -- Constants --

	/** XSI namespace. */
	public static final String XSI_NS =
		"http://www.w3.org/2001/XMLSchema-instance";

	/** OME-XML schema location. */
	public static final String SCHEMA =
	// "http://www.openmicroscopy.org/Schemas/OME/2012-06/ome.xsd";
		"https://raw.github.com/openmicroscopy/openmicroscopy/schema-2012-06/components/specification/InProgress/ome.xsd";

	// -- Fields --

	/** The root element of OME-XML. */
	protected OMEModelObject root;

	/** DOM element that backs the first Image's CustomAttributes node. */
	private Element imageCA;

	private DocumentBuilder builder;

	// -- Constructors --

	/** Creates a new OME-XML metadata object. */
	public AbstractOMEXMLMetadata() {}

	// -- OMEXMLMetadata API methods --

	/**
	 * Dumps the given OME-XML DOM tree to a string.
	 * 
	 * @return OME-XML as a string.
	 */
	public String dumpXML() {
		if (root == null) {
			root = (OMEModelObject) getRoot();
			if (root == null) return null;
		}
		try {
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			final Document doc = createNewDocument();
			final Element r = root.asXMLElement(doc);
			r.setAttribute("xmlns:xsi", XSI_NS);
			r.setAttribute("xsi:schemaLocation", OME.NAMESPACE + " " + SCHEMA);
			doc.appendChild(r);
			DOMUtil.writeXML(os, doc);
			return os.toString(Constants.ENCODING);
		}
		catch (final TransformerException exc) {
			log().warn("Failed to create OME-XML", exc);
		}
		catch (final UnsupportedEncodingException exc) {
			log().warn("Failed to create OME-XML", exc);
		}
		return null;
	}

	// -- MetadataRetrieve API methods --

	/* @see loci.formats.meta.MetadataRetrieve#getUUID() */
	public String getUUID() {
		final Element ome = getRootElement();
		return DOMUtil.getAttribute("UUID", ome);
	}

	// -- MetadataStore API methods --

	/* @see loci.formats.meta.MetadataStore#setRoot(Object) */
	public void setRoot(final Object root) {}

	/* @see loci.formats.meta.MetadataStore#getRoot() */
	public Object getRoot() {
		return root;
	}

	/* @see loci.formats.meta.MetadataRetrieve#setUUID(String) */
	public void setUUID(final String uuid) {
		final Element ome = getRootElement();
		DOMUtil.setAttribute("UUID", uuid, ome);
	}

	// -- Type conversion methods --

	/**
	 * Converts Boolean value to Integer. Used to convert from 2003-FC Laser
	 * FrequencyDoubled Boolean value to Laser FrequencyMultiplication Integer
	 * value.
	 */
	protected Integer booleanToInteger(final Boolean value) {
		return value == null ? null : new Integer(value.booleanValue() ? 2 : 1);
	}

	/**
	 * Converts Integer value to Boolean. Used to convert from Laser
	 * FrequencyMultiplication Integer value to 2003-FC Laser FrequencyDoubled
	 * Boolean value.
	 */
	protected Boolean integerToBoolean(final Integer value) {
		return value == null ? null : new Boolean(value.intValue() == 2);
	}

	/**
	 * Converts Double value to Integer. Used to convert from 2008-02
	 * LogicalChannel PinholeSize Integer value to LogicalChannel PinholeSize
	 * Double value.
	 */
	protected Integer doubleToInteger(final Double value) {
		return value == null ? null : new Integer(value.intValue());
	}

	/**
	 * Converts Integer value to Double. Used to convert from LogicalChannel
	 * PinholeSize Double value to 2008-02 LogicalChannel PinholeSize Integer
	 * value.
	 */
	protected Double integerToDouble(final Integer value) {
		return value == null ? null : new Double(value.doubleValue());
	}

	// -- Helper methods --

	private Document createNewDocument() {
		if (builder == null) {
			try {
				builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			}
			catch (final ParserConfigurationException e) {}
		}
		return builder.newDocument();
	}

	private Element getRootElement() {
		return root.asXMLElement(createNewDocument());
	}

}
