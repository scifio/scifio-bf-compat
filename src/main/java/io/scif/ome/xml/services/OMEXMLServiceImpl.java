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

package io.scif.ome.xml.services;

import io.scif.ome.xml.meta.OMEMetadata;
import io.scif.ome.xml.meta.OMEXMLMetadata;
import io.scif.ome.xml.meta.OMEXMLMetadataImpl;
import io.scif.services.ServiceException;
import io.scif.xml.XMLService;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataConverter;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.meta.MetadataStore;
import ome.xml.OMEXMLFactory;
import ome.xml.model.BinData;
import ome.xml.model.Channel;
import ome.xml.model.Image;
import ome.xml.model.MetadataOnly;
import ome.xml.model.OME;
import ome.xml.model.OMEModel;
import ome.xml.model.OMEModelImpl;
import ome.xml.model.OMEModelObject;
import ome.xml.model.Pixels;
import ome.xml.model.StructuredAnnotations;
import ome.xml.model.XMLAnnotation;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * <dl>
 * <dt><b>Source code:</b></dt>
 * <dd><a href=
 * "http://trac.openmicroscopy.org.uk/ome/browser/bioformats.git/components/bio-formats/src/loci/formats/services/OMEXMLServiceImpl.java"
 * >Trac</a>, <a href=
 * "http://git.openmicroscopy.org/?p=bioformats.git;a=blob;f=components/bio-formats/src/loci/formats/services/OMEXMLServiceImpl.java;hb=HEAD"
 * >Gitweb</a></dd>
 * </dl>
 * 
 * @author callan
 */
@Plugin(type = OMEXMLService.class)
public class OMEXMLServiceImpl extends AbstractService implements OMEXMLService
{
	
	public static String URL_BIO_FORMATS_LIBRARY = 
			"http://www.openmicroscopy.org/site/support/bio-formats4/developers/java-library.html";

	@Parameter
	OMEXMLMetadataService omexmlMetadataService;
	
	@Parameter
	XMLService xmlService;

	public static final String NO_OME_XML_MSG =
		"ome-xml.jar is required to read OME-TIFF files.  " +
			"Please download it from " + URL_BIO_FORMATS_LIBRARY;
				
	/** Logger for this class. */
	private static final Logger LOGGER = LoggerFactory
		.getLogger(OMEXMLService.class);

	// -- Stylesheet names --

	private static final String XSLT_PATH = "/io/scif/ome/xml/meta/";
	private static final String XSLT_REORDER = XSLT_PATH + "reorder-2008-09.xsl";
	private static final String XSLT_2003FC = XSLT_PATH +
		"2003-FC-to-2008-09.xsl";
	private static final String XSLT_2006LO = XSLT_PATH +
		"2006-LO-to-2008-09.xsl";
	private static final String XSLT_200706 = XSLT_PATH +
		"2007-06-to-2008-09.xsl";
	private static final String XSLT_200802 = XSLT_PATH +
		"2008-02-to-2008-09.xsl";
	private static final String XSLT_200809 = XSLT_PATH +
		"2008-09-to-2009-09.xsl";
	private static final String XSLT_200909 = XSLT_PATH +
		"2009-09-to-2010-04.xsl";
	private static final String XSLT_201004 = XSLT_PATH +
		"2010-04-to-2010-06.xsl";
	private static final String XSLT_201006 = XSLT_PATH +
		"2010-06-to-2011-06.xsl";
	private static final String XSLT_201106 = XSLT_PATH +
		"2011-06-to-2012-06.xsl";

	// -- Cached stylesheets --

	/** Reordering stylesheet. */
	private static Templates reorderXSLT;

	/** Stylesheets for updating from previous schema releases. */
	private static Templates update2003FC;
	private static Templates update2006LO;
	private static Templates update200706;
	private static Templates update200802;
	private static Templates update200809;
	private static Templates update200909;
	private static Templates update201004;
	private static Templates update201006;
	private static Templates update201106;

	private static final String SCHEMA_PATH =
		"http://www.openmicroscopy.org/Schemas/OME/";

	/** @see OMEXMLService#getLatestVersion() */
	public String getLatestVersion() {
		return OMEXMLFactory.LATEST_VERSION;
	}

	/** @see OMEXMLService#transformToLatestVersion(String) */
	public String transformToLatestVersion(String xml) {
		final String version = getOMEXMLVersion(xml);
		if (version.equals(getLatestVersion())) return xml;
		LOGGER.debug("Attempting to update XML with version: {}", version);
		LOGGER.trace("Initial dump: {}", xml);

		String transformed = null;
		try {
			if (version.equals("2003-FC")) {
				xml = verifyOMENamespace(xml);
				LOGGER.debug("Running UPDATE_2003FC stylesheet.");
				if (update2003FC == null) {
					update2003FC =
						xmlService.getStylesheet(XSLT_2003FC, OMEXMLServiceImpl.class);
				}
				transformed = xmlService.transformXML(xml, update2003FC);
			}
			else if (version.equals("2006-LO")) {
				xml = verifyOMENamespace(xml);
				LOGGER.debug("Running UPDATE_2006LO stylesheet.");
				if (update2006LO == null) {
					update2006LO =
						xmlService.getStylesheet(XSLT_2006LO, OMEXMLServiceImpl.class);
				}
				transformed = xmlService.transformXML(xml, update2006LO);
			}
			else if (version.equals("2007-06")) {
				xml = verifyOMENamespace(xml);
				LOGGER.debug("Running UPDATE_200706 stylesheet.");
				if (update200706 == null) {
					update200706 =
						xmlService.getStylesheet(XSLT_200706, OMEXMLServiceImpl.class);
				}
				transformed = xmlService.transformXML(xml, update200706);
			}
			else if (version.equals("2008-02")) {
				xml = verifyOMENamespace(xml);
				LOGGER.debug("Running UPDATE_200802 stylesheet.");
				if (update200802 == null) {
					update200802 =
						xmlService.getStylesheet(XSLT_200802, OMEXMLServiceImpl.class);
				}
				transformed = xmlService.transformXML(xml, update200802);
			}
			else transformed = xml;
			LOGGER.debug("XML updated to at least 2008-09");
			LOGGER.trace("At least 2008-09 dump: {}", transformed);

			if (!version.equals("2009-09") && !version.equals("2010-04") &&
				!version.equals("2010-06") && !version.equals("2011-06") &&
				!version.equals("2012-06"))
			{
				transformed = verifyOMENamespace(transformed);
				LOGGER.debug("Running UPDATE_200809 stylesheet.");
				if (update200809 == null) {
					update200809 =
						xmlService.getStylesheet(XSLT_200809, OMEXMLServiceImpl.class);
				}
				transformed = xmlService.transformXML(transformed, update200809);
			}
			LOGGER.debug("XML updated to at least 2009-09");
			LOGGER.trace("At least 2009-09 dump: {}", transformed);
			if (!version.equals("2010-04") && !version.equals("2010-06") &&
				!version.equals("2011-06") && !version.equals("2012-06"))
			{
				transformed = verifyOMENamespace(transformed);
				LOGGER.debug("Running UPDATE_200909 stylesheet.");
				if (update200909 == null) {
					update200909 =
						xmlService.getStylesheet(XSLT_200909, OMEXMLServiceImpl.class);
				}
				transformed = xmlService.transformXML(transformed, update200909);
			}
			else transformed = xml;
			LOGGER.debug("XML updated to at least 2010-04");
			LOGGER.trace("At least 2010-04 dump: {}", transformed);

			if (!version.equals("2010-06") && !version.equals("2011-06") &&
				!version.equals("2012-06"))
			{
				transformed = verifyOMENamespace(transformed);
				LOGGER.debug("Running UPDATE_201004 stylesheet.");
				if (update201004 == null) {
					update201004 =
						xmlService.getStylesheet(XSLT_201004, OMEXMLServiceImpl.class);
				}
				transformed = xmlService.transformXML(transformed, update201004);
			}
			else transformed = xml;
			LOGGER.debug("XML updated to at least 2010-06");

			if (!version.equals("2011-06") && !version.equals("2012-06")) {
				transformed = verifyOMENamespace(transformed);
				LOGGER.debug("Running UPDATE_201006 stylesheet.");
				if (update201006 == null) {
					update201006 =
						xmlService.getStylesheet(XSLT_201006, OMEXMLServiceImpl.class);
				}
				transformed = xmlService.transformXML(transformed, update201006);
			}
			else transformed = xml;
			LOGGER.debug("XML updated to at least 2011-06");

			if (!version.equals("2012-06")) {
				transformed = verifyOMENamespace(transformed);
				LOGGER.debug("Running UPDATE_201106 stylesheet.");
				if (update201106 == null) {
					update201106 =
						xmlService.getStylesheet(XSLT_201106, OMEXMLServiceImpl.class);
				}
				transformed = xmlService.transformXML(transformed, update201106);
			}
			else transformed = xml;
			LOGGER.debug("XML updated to at least 2012-06");

			// fix namespaces
			transformed = transformed.replaceAll("<ns.*?:", "<");
			transformed = transformed.replaceAll("xmlns:ns.*?=", "xmlns:OME=");
			transformed = transformed.replaceAll("</ns.*?:", "</");
			LOGGER.trace("Transformed XML dump: {}", transformed);
			return transformed;
		}
		catch (final IOException e) {
			LOGGER.warn("Could not transform version " + version + " OME-XML.");
		}
		return null;
	}

	/**
	 * @throws ServiceException
	 * @see OMEXMLService#createOMEXMLMetadata()
	 */
	public OMEXMLMetadata createOMEXMLMetadata() throws ServiceException {
		return createOMEXMLMetadata(null);
	}

	/**
	 * @throws ServiceException
	 * @see OMEXMLService#createOMEXMLMetadata(java.lang.String)
	 */
	public OMEXMLMetadata createOMEXMLMetadata(final String xml)
		throws ServiceException
	{
		return createOMEXMLMetadata(xml, null);
	}

	/**
	 * @throws ServiceException
	 * @see OMEXMLService#createOMEXMLMetadata(java.lang.String, java.lang.String)
	 */
	public OMEXMLMetadata createOMEXMLMetadata(String xml, final String version)
		throws ServiceException
	{
		if (xml != null) {
			xml = xmlService.sanitizeXML(xml);
		}
		final OMEModelObject ome =
			xml == null ? null : createRoot(transformToLatestVersion(xml));

		final OMEXMLMetadata meta = new OMEXMLMetadataImpl();
		if (ome != null) meta.setRoot(ome);
		return meta;
	}

	/**
	 * @throws ServiceException
	 * @see OMEXMLService#createOMEXMLRoot(java.lang.String)
	 */
	public Object createOMEXMLRoot(final String xml) throws ServiceException {
		return createRoot(transformToLatestVersion(xml));
	}

	/** @see OMEXMLService#isOMEXMLMetadata(java.lang.Object) */
	public boolean isOMEXMLMetadata(final Object o) {
		return o instanceof OMEXMLMetadata;
	}

	/** @see OMEXMLService#isOMEXMLRoot(java.lang.Object) */
	public boolean isOMEXMLRoot(final Object o) {
		return o instanceof OMEModelObject;
	}

	/**
	 * Constructs an OME root node. <b>NOTE:</b> This method is mostly here to
	 * ensure type safety of return values as instances of service dependency
	 * classes should not leak out of the interface.
	 * 
	 * @param xml String of XML to create the root node from.
	 * @return An ome.xml.model.OMEModelObject subclass root node.
	 * @throws ServiceException
	 * @throws IOException If there is an error reading from the string.
	 * @throws SAXException If there is an error parsing the XML.
	 * @throws ParserConfigurationException If there is an error preparing the
	 *           parsing infrastructure.
	 */
	private OMEModelObject createRoot(final String xml) throws ServiceException {
		final OMEModel model = new OMEModelImpl();
		OME ome = null;
		try {
			ome = new OME(xmlService.parseDOM(xml).getDocumentElement(), model);
		}
		catch (final Exception e) {
			throw new ServiceException(e);
		}
		model.resolveReferences();
		return ome;
	}

	/** @see OMEXMLService#getOMEXMLVersion(java.lang.Object) */
	public String getOMEXMLVersion(final Object o) {
		if (o == null) return null;
		if (o instanceof OMEXMLMetadata || o instanceof OMEModelObject) {
			return OMEXMLFactory.LATEST_VERSION;
		}
		else if (o instanceof String) {
			final String xml = (String) o;
			try {
				final Element e = xmlService.parseDOM(xml).getDocumentElement();
				String namespace = e.getAttribute("xmlns");
				if (namespace == null || namespace.equals("")) {
					namespace = e.getAttribute("xmlns:ome");
				}
				if (namespace == null || namespace.equals("")) {
					namespace = e.getAttribute("xmlns:OME");
				}

				return namespace.endsWith("ome.xsd") ? "2003-FC" : namespace
					.substring(namespace.lastIndexOf("/") + 1);
			}
			catch (final ParserConfigurationException pce) {}
			catch (final SAXException se) {}
			catch (final IOException ioe) {}
		}
		return null;
	}

	/**
	 * @throws ServiceException
	 * @see OMEXMLService#getOMEMetadata(loci.formats.meta.MetadataRetrieve)
	 */
	public OMEXMLMetadata getOMEMetadata(final MetadataRetrieve src)
		throws ServiceException
	{
		// check if the metadata is already an OME-XML metadata object
		if (src instanceof OMEXMLMetadata) return (OMEXMLMetadata) src;

		// populate a new OME-XML metadata object with metadata
		// converted from the non-OME-XML metadata object
		final OMEXMLMetadata omexmlMeta = createOMEXMLMetadata();
		convertMetadata(src, omexmlMeta);
		return omexmlMeta;
	}

	/**
	 * @throws ServiceException
	 * @see OMEXMLService#getOMEXML(loci.formats.meta.MetadataRetrieve)
	 */
	public String getOMEXML(final MetadataRetrieve src) throws ServiceException {
		final OMEXMLMetadata omexmlMeta = getOMEMetadata(src);
		String xml = omexmlMeta.dumpXML();

		// make sure that the namespace has been set correctly

		// convert XML string to DOM
		Document doc = null;
		Exception exception = null;
		try {
			doc = xmlService.parseDOM(xml);
		}
		catch (final ParserConfigurationException exc) {
			exception = exc;
		}
		catch (final SAXException exc) {
			exception = exc;
		}
		catch (final IOException exc) {
			exception = exc;
		}
		if (exception != null) {
			LOGGER.info("Malformed OME-XML", exception);
			return null;
		}

		final Element root = doc.getDocumentElement();
		root.setAttribute("xmlns", SCHEMA_PATH + getLatestVersion());

		// convert tweaked DOM back to XML string
		try {
			xml = xmlService.getXML(doc);
		}
		catch (final TransformerConfigurationException exc) {
			exception = exc;
		}
		catch (final TransformerException exc) {
			exception = exc;
		}
		if (exception != null) {
			LOGGER.info("Internal XML conversion error", exception);
			return null;
		}

		return xml;
	}

	/** @see OMEXMLService#validateOMEXML(java.lang.String) */
	public boolean validateOMEXML(final String xml) {
		return validateOMEXML(xml, false);
	}

	/** @see OMEXMLService#validateOMEXML(java.lang.String, boolean) */
	public boolean validateOMEXML(String xml, final boolean pixelsHack) {
		// HACK: Inject a TiffData element beneath any childless Pixels elements.
		if (pixelsHack) {
			// convert XML string to DOM
			Document doc = null;
			Exception exception = null;
			try {
				doc = xmlService.parseDOM(xml);
			}
			catch (final ParserConfigurationException exc) {
				exception = exc;
			}
			catch (final SAXException exc) {
				exception = exc;
			}
			catch (final IOException exc) {
				exception = exc;
			}
			if (exception != null) {
				LOGGER.info("Malformed OME-XML", exception);
				return false;
			}

			// inject TiffData elements as needed
			final NodeList list = doc.getElementsByTagName("Pixels");
			for (int i = 0; i < list.getLength(); i++) {
				final Node node = list.item(i);
				final NodeList children = node.getChildNodes();
				boolean needsTiffData = true;
				for (int j = 0; j < children.getLength(); j++) {
					final Node child = children.item(j);
					final String name = child.getLocalName();
					if ("TiffData".equals(name) || "BinData".equals(name)) {
						needsTiffData = false;
						break;
					}
				}
				if (needsTiffData) {
					// inject TiffData element
					final Node tiffData = doc.createElement("TiffData");
					node.insertBefore(tiffData, node.getFirstChild());
				}
			}

			// convert tweaked DOM back to XML string
			try {
				xml = xmlService.getXML(doc);
			}
			catch (final TransformerConfigurationException exc) {
				exception = exc;
			}
			catch (final TransformerException exc) {
				exception = exc;
			}
			if (exception != null) {
				LOGGER.info("Internal XML conversion error", exception);
				return false;
			}
		}
		return xmlService.validateXML(xml, "OME-XML");
	}

	/**
	 * @see OMEXMLService#populateOriginalMetadata(loci.formats.ome.OMEXMLMetadata,
	 *      Hashtable)
	 */
	public void populateOriginalMetadata(final OMEMetadata omeMeta,
		final Hashtable<String, Object> metadata)
	{
		final OMEXMLMetadata omexmlMeta = omeMeta.getRoot();
		((OMEXMLMetadataImpl) omexmlMeta).resolveReferences();
		final OME root = (OME) omexmlMeta.getRoot();
		StructuredAnnotations annotations = root.getStructuredAnnotations();
		if (annotations == null) annotations = new StructuredAnnotations();
		int annotationIndex = annotations.sizeOfXMLAnnotationList();

		for (final String key : metadata.keySet()) {
			final OriginalMetadataAnnotation annotation =
				new OriginalMetadataAnnotation();
			annotation.setID(omexmlMetadataService.createLSID("Annotation",
				annotationIndex));
			annotation.setKey(key);
			annotation.setValue(metadata.get(key).toString());
			annotations.addXMLAnnotation(annotation);
			annotationIndex++;
		}

		root.setStructuredAnnotations(annotations);
		omexmlMeta.setRoot(root);
	}

	/**
	 * @see OMEXMLService#populateOriginalMetadata(loci.formats.ome.OMEXMLMetadata,
	 *      java.lang.String, java.lang.String)
	 */
	public void populateOriginalMetadata(final OMEMetadata omeMeta,
		final String key, final String value)
	{
		final OMEXMLMetadata omexmlMeta = omeMeta.getRoot();
		((OMEXMLMetadataImpl) omexmlMeta).resolveReferences();
		final OME root = (OME) omexmlMeta.getRoot();
		StructuredAnnotations annotations = root.getStructuredAnnotations();
		if (annotations == null) annotations = new StructuredAnnotations();
		final int annotationIndex = annotations.sizeOfXMLAnnotationList();

		final OriginalMetadataAnnotation annotation =
			new OriginalMetadataAnnotation();
		annotation.setID(omexmlMetadataService.createLSID("Annotation",
			annotationIndex));
		annotation.setKey(key);
		annotation.setValue(value);
		annotations.addXMLAnnotation(annotation);

		root.setStructuredAnnotations(annotations);
		omexmlMeta.setRoot(root);
	}

	/*
	 * @see ome.xml.services.OMEXMLService#getOriginalMetadata(ome.xml.meta.OMEXMLMetadata)
	 */
	public Hashtable<String, Object> getOriginalMetadata(
		final OMEXMLMetadata omexmlMeta)
	{
		final OME root = (OME) omexmlMeta.getRoot();
		final StructuredAnnotations annotations = root.getStructuredAnnotations();
		if (annotations == null) {
			return null;
		}

		final Hashtable<String, Object> metadata = new Hashtable<String, Object>();

		for (int i = 0; i < annotations.sizeOfXMLAnnotationList(); i++) {
			final XMLAnnotation annotation = annotations.getXMLAnnotation(i);
			final String xml = annotation.getValue();

			try {
				final Document annotationRoot = xmlService.parseDOM(xml);
				NodeList metadataNodes =
					annotationRoot.getElementsByTagName("OriginalMetadata");

				for (int meta = 0; meta < metadataNodes.getLength(); meta++) {
					final Element metadataNode = (Element) metadataNodes.item(meta);
					final NodeList keys = metadataNode.getElementsByTagName("Key");
					final NodeList values = metadataNode.getElementsByTagName("Value");

					for (int q = 0; q < keys.getLength(); q++) {
						final Node key = keys.item(q);
						final Node value = values.item(q);

						metadata.put(key.getTextContent(), value.getTextContent());
					}
				}

				if (metadataNodes.getLength() == 0) {
					metadataNodes = annotationRoot.getDocumentElement().getChildNodes();

					for (int meta = 0; meta < metadataNodes.getLength(); meta++) {
						final Element node = (Element) metadataNodes.item(meta);
						final String name = node.getNodeName();

						final NamedNodeMap attrs = node.getAttributes();
						final Node value = attrs.getNamedItem("Value");
						if (value != null) {
							metadata.put(name, value.getNodeValue());
						}
					}
				}
			}
			catch (final ParserConfigurationException e) {
				LOGGER.debug("Failed to parse OriginalMetadata", e);
			}
			catch (final SAXException e) {
				LOGGER.debug("Failed to parse OriginalMetadata", e);
			}
			catch (final IOException e) {
				LOGGER.debug("Failed to parse OriginalMetadata", e);
			}
		}

		return metadata;
	}

	/**
	 * @throws ServiceException
	 * @see OMEXMLService#convertMetadata(java.lang.String,
	 *      loci.formats.meta.MetadataStore)
	 */
	public void convertMetadata(final String xml, final MetadataStore dest)
		throws ServiceException
	{
		final OMEModelObject ome = createRoot(transformToLatestVersion(xml));
		final String rootVersion = getOMEXMLVersion(ome);
		final String storeVersion = getOMEXMLVersion(dest);
		if (rootVersion.equals(storeVersion)) {
			// metadata store is already an OME-XML metadata object of the
			// correct schema version; populate OME-XML string directly
			if (!(dest instanceof OMEXMLMetadata)) {
				throw new IllegalArgumentException("Expecting OMEXMLMetadata instance.");
			}

			dest.setRoot(ome);
		}
		else {
			// metadata store is incompatible; create an OME-XML
			// metadata object and copy it into the destination
			final IMetadata src = createOMEXMLMetadata(xml);
			convertMetadata(src, dest);
		}
	}

	/**
	 * @see OMEXMLService#convertMetadata(loci.formats.meta.MetadataRetrieve,
	 *      loci.formats.meta.MetadataStore)
	 */
	public void convertMetadata(final MetadataRetrieve src,
		final MetadataStore dest)
	{
		MetadataConverter.convertMetadata(src, dest);
	}

	/** @see OMEXMLService#removeBinData(OMEXMLMetadata) */
	public void removeBinData(final OMEXMLMetadata omexmlMeta) {
		((OMEXMLMetadataImpl) omexmlMeta).resolveReferences();
		final OME root = (OME) omexmlMeta.getRoot();
		final List<Image> images = root.copyImageList();
		for (final Image img : images) {
			final Pixels pix = img.getPixels();
			final List<BinData> binData = pix.copyBinDataList();
			for (final BinData bin : binData) {
				pix.removeBinData(bin);
			}
		}
		omexmlMeta.setRoot(root);
	}

	/** @see OMEXMLService#removeChannels(OMEXMLMetadata, int, int) */
	public void removeChannels(final OMEXMLMetadata omexmlMeta, final int image,
		final int sizeC)
	{
		((OMEXMLMetadataImpl) omexmlMeta).resolveReferences();
		final OME root = (OME) omexmlMeta.getRoot();
		final Pixels img = root.getImage(image).getPixels();
		final List<Channel> channels = img.copyChannelList();

		for (int c = 0; c < channels.size(); c++) {
			final Channel channel = channels.get(c);
			if (channel.getID() == null || c >= sizeC) {
				img.removeChannel(channel);
			}
		}
		omexmlMeta.setRoot(root);
	}

	/** @see OMEXMLService#addMetadataOnly(OMEXMLMetadata, int) */
	public void addMetadataOnly(final OMEXMLMetadata omexmlMeta, final int image)
	{
		((OMEXMLMetadataImpl) omexmlMeta).resolveReferences();
		final MetadataOnly meta = new MetadataOnly();
		final OME root = (OME) omexmlMeta.getRoot();
		final Pixels pix = root.getImage(image).getPixels();
		pix.setMetadataOnly(meta);
		omexmlMeta.setRoot(root);
	}

	/** @see OMEXMLService#isEqual(OMEXMLMetadata, OMEXMLMetadata) */
	public boolean isEqual(final OMEXMLMetadata src1, final OMEXMLMetadata src2) {
		((OMEXMLMetadataImpl) src1).resolveReferences();
		((OMEXMLMetadataImpl) src2).resolveReferences();

		final OME omeRoot1 = (OME) src1.getRoot();
		final OME omeRoot2 = (OME) src2.getRoot();

		DocumentBuilder builder = null;
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		}
		catch (final ParserConfigurationException e) {
			return false;
		}

		final Document doc1 = builder.newDocument();
		final Document doc2 = builder.newDocument();

		final Element root1 = omeRoot1.asXMLElement(doc1);
		final Element root2 = omeRoot2.asXMLElement(doc2);

		return equals(root1, root2);
	}

	// -- Utility methods - casting --

	/** @see OMEXMLService#asStore(loci.formats.meta.MetadataRetrieve) */
	public MetadataStore asStore(final MetadataRetrieve meta) {
		return meta instanceof MetadataStore ? (MetadataStore) meta : null;
	}

	/** @see OMEXMLService#asRetrieve(loci.formats.meta.MetadataStore) */
	public MetadataRetrieve asRetrieve(final MetadataStore meta) {
		return meta instanceof MetadataRetrieve ? (MetadataRetrieve) meta : null;
	}

	// -- Helper methods --

	/** Ensures that an xmlns:ome element exists. */
	private String verifyOMENamespace(final String xml) {
		try {
			final Document doc = xmlService.parseDOM(xml);
			final Element e = doc.getDocumentElement();
			final String omeNamespace = e.getAttribute("xmlns:ome");
			if (omeNamespace == null || omeNamespace.equals("")) {
				e.setAttribute("xmlns:ome", e.getAttribute("xmlns"));
			}
			return xmlService.getXML(doc);
		}
		catch (final ParserConfigurationException pce) {}
		catch (final TransformerConfigurationException tce) {}
		catch (final TransformerException te) {}
		catch (final SAXException se) {}
		catch (final IOException ioe) {}
		return null;
	}

	/** Compares two Elements for equality. */
	public boolean equals(final Node e1, final Node e2) {
		final NodeList children1 = e1.getChildNodes();
		final NodeList children2 = e2.getChildNodes();

		String localName1 = e1.getLocalName();
		if (localName1 == null) {
			localName1 = "";
		}
		String localName2 = e2.getLocalName();
		if (localName2 == null) {
			localName2 = "";
		}
		if (!localName1.equals(localName2)) {
			return false;
		}

		if (localName1.equals("StructuredAnnotations")) {
			// we don't care about StructuredAnnotations at all
			return true;
		}

		final NamedNodeMap attributes1 = e1.getAttributes();
		final NamedNodeMap attributes2 = e2.getAttributes();

		if (attributes1 == null || attributes2 == null) {
			if ((attributes1 == null && attributes2 != null) ||
				(attributes1 != null && attributes2 == null))
			{
				return false;
			}
		}
		else if (attributes1.getLength() != attributes2.getLength()) {
			return false;
		}
		else {
			// make sure that all of the attributes are equal, except for IDs

			final int nAttributes = attributes1.getLength();

			for (int i = 0; i < nAttributes; i++) {
				final Node n1 = attributes1.item(i);
				final String localName = n1.getNodeName();

				if (localName != null && !localName.equals("ID")) {
					final Node n2 = attributes2.getNamedItem(localName);
					if (n2 == null) {
						return false;
					}

					if (!equals(n1, n2)) {
						return false;
					}
				}
				else if ("ID".equals(localName)) {
					if (localName1.endsWith("Settings")) {
						// this is a reference to a different node
						// the references are equal if the two referenced nodes are equal

						final Node n2 = attributes2.getNamedItem(localName);

						final Node realRoot1 = findRootNode(e1);
						final Node realRoot2 = findRootNode(e2);

						final String refName = localName1.replaceAll("Settings", "");

						final Node ref1 =
							findChildWithID(realRoot1, refName, n1.getNodeValue());
						final Node ref2 =
							findChildWithID(realRoot2, refName, n2.getNodeValue());

						if (ref1 == null && ref2 == null) {
							return true;
						}
						else if ((ref1 == null && ref2 != null) ||
							(ref1 != null && ref2 == null) || !equals(ref1, ref2))
						{
							return false;
						}
					}
				}
			}
		}

		if (children1.getLength() != children2.getLength()) {
			return false;
		}

		final Object node1 = e1.getNodeValue();
		final Object node2 = e2.getNodeValue();

		if (node1 == null && node2 != null) {
			return false;
		}
		if (node1 != null && !node1.equals(node2) && !localName1.equals("")) {
			return false;
		}

		for (int i = 0; i < children1.getLength(); i++) {
			if (!equals(children1.item(i), children2.item(i))) {
				return false;
			}
		}
		return true;
	}

	/** Return the absolute root node for the specified child node. */
	private Node findRootNode(final Node child) {
		if (child.getParentNode() != null) {
			return findRootNode(child.getParentNode());
		}
		return child;
	}

	/** Return the child node with specified value for the "ID" attribute. */
	private Node findChildWithID(final Node root, final String name,
		final String id)
	{
		final NamedNodeMap attributes = root.getAttributes();
		if (attributes != null) {
			final Node idNode = attributes.getNamedItem("ID");

			if (idNode != null && id.equals(idNode.getNodeValue()) &&
				name.equals(root.getNodeName()))
			{
				return root;
			}
		}

		final NodeList children = root.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {
			final Node result = findChildWithID(children.item(i), name, id);
			if (result != null) {
				return result;
			}
		}

		return null;
	}

	// -- Helper class --

	class OriginalMetadataAnnotation extends XMLAnnotation {

		private static final String ORIGINAL_METADATA_NS =
			"openmicroscopy.org/OriginalMetadata";

		private String key, value;

		// -- OriginalMetadataAnnotation methods --

		public void setKey(final String key) {
			this.key = key;
		}

		@Override
		public void setValue(final String value) {
			this.value = value;
		}

		// -- XMLAnnotation methods --

		/* @see ome.xml.model.XMLAnnotation#asXMLElement(Document, Element) */
		@Override
		protected Element asXMLElement(final Document document, Element element) {
			if (element == null) {
				element =
					document.createElementNS(XMLAnnotation.NAMESPACE, "XMLAnnotation");
			}

			final Element keyElement =
				document.createElementNS(ORIGINAL_METADATA_NS, "Key");
			final Element valueElement =
				document.createElementNS(ORIGINAL_METADATA_NS, "Value");
			keyElement.setTextContent(key);
			valueElement.setTextContent(value);

			final Element originalMetadata =
				document.createElementNS(ORIGINAL_METADATA_NS, "OriginalMetadata");
			originalMetadata.appendChild(keyElement);
			originalMetadata.appendChild(valueElement);

			final Element annotationValue =
				document.createElementNS(XMLAnnotation.NAMESPACE, "Value");
			annotationValue.appendChild(originalMetadata);

			element.appendChild(annotationValue);
			return super.asXMLElement(document, element);
		}

	}

}
