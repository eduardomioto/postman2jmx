package com.loadium.postman2jmx.parser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XLMSorting {

	public void sort(String jmxFilePath) throws Exception, TransformerFactoryConfigurationError {

		final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		final Document doc = docBuilder.parse(jmxFilePath);

		final Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		// initialize StreamResult with File object to save to file
		final StreamResult result = new StreamResult(new StringWriter());
		final DOMSource source = new DOMSource(doc);
		transformer.transform(source, result);

		final Node[] ret = sortNodes(doc.getElementsByTagName("HeaderManager"), "testname", true, String.class);
		final Node[] ret1 = sortNodes(doc.getElementsByTagName("HTTPSamplerProxy"), "testname", true, String.class);
		int i = 0;
		final StringBuilder lineXML = new StringBuilder();
		transformer.setOutputProperty("omit-xml-declaration", "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

		StringWriter writer;

		final Node rootNode = doc.getChildNodes().item(0);
		final String rootNodeLine = String.format("<%s %s %s %s>", rootNode.getNodeName(),
				rootNode.getAttributes().getNamedItem("version").toString(),
				rootNode.getAttributes().getNamedItem("properties").toString(),
				rootNode.getAttributes().getNamedItem("jmeter").toString());

		lineXML.append(rootNodeLine).append(System.getProperty("line.separator"));
		lineXML.append("<hashTree>").append(System.getProperty("line.separator"));

		final NodeList nodeTestPlan = doc.getElementsByTagName("TestPlan");
		writer = new StringWriter();
		transformer.transform(new DOMSource(nodeTestPlan.item(0)), new StreamResult(writer));
		lineXML.append(writer.toString());
		lineXML.append("<hashTree>").append(System.getProperty("line.separator"));

		final NodeList nodeThreadGroup = doc.getElementsByTagName("ThreadGroup");
		writer = new StringWriter();
		transformer.transform(new DOMSource(nodeThreadGroup.item(0)), new StreamResult(writer));
		lineXML.append(writer.toString());
		lineXML.append("<hashTree>").append(System.getProperty("line.separator"));

		for (final Node n : ret) {
			writer = new StringWriter();
			transformer.transform(new DOMSource(ret1[i]), new StreamResult(writer));
			lineXML.append(writer.toString());

			lineXML.append("<hashTree>").append(System.getProperty("line.separator"));

			writer = new StringWriter();
			transformer.transform(new DOMSource(ret[i]), new StreamResult(writer));
			lineXML.append(writer.toString());

			lineXML.append("<hashTree/>").append(System.getProperty("line.separator"));
			lineXML.append("</hashTree>").append(System.getProperty("line.separator"));
			i++;
		}

		lineXML.append("</hashTree>").append(System.getProperty("line.separator"));
		lineXML.append("</hashTree>").append(System.getProperty("line.separator"));
		lineXML.append("</hashTree>").append(System.getProperty("line.separator"));
		lineXML.append("</jmeterTestPlan>").append(System.getProperty("line.separator"));

		save(format(lineXML.toString(), true), jmxFilePath);

	}

	private static void save(String content, String pathName) throws Exception {
		final File file = new File(pathName);
		if (!file.exists()) {
			file.createNewFile();
		}
		try (FileOutputStream fop = new FileOutputStream(file)) {
			final byte[] contentInBytes = content.getBytes();
			fop.write(contentInBytes);
			fop.flush();
		}

	}

	private static String format(String xml, Boolean ommitXmlDeclaration)
			throws IOException, SAXException, ParserConfigurationException {
		final DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		final Document doc = db.parse(new InputSource(new StringReader(xml)));
		final OutputFormat format = new OutputFormat(doc);
		format.setIndenting(true);
		format.setIndent(2);
		format.setOmitXMLDeclaration(ommitXmlDeclaration);
		format.setLineWidth(Integer.MAX_VALUE);
		final Writer outxml = new StringWriter();
		final XMLSerializer serializer = new XMLSerializer(outxml, format);
		serializer.serialize(doc);
		return outxml.toString();
	}

	/**
	 * Method sorts any NodeList by provided attribute.
	 *
	 * @param nl            NodeList to sort
	 * @param attributeName attribute name to use
	 * @param asc           true - ascending, false - descending
	 * @param B             class must implement Comparable and have
	 *                      Constructor(String) - e.g. Integer.class ,
	 *                      BigDecimal.class etc
	 * @return Array of Nodes in required order
	 */
	private static Node[] sortNodes(NodeList nl, String attributeName, boolean asc, Class<? extends Comparable> B) {
		class NodeComparator<T> implements Comparator<T> {
			@Override
			public int compare(T a, T b) {
				int ret;
				Comparable bda = null, bdb = null;
				try {
					final Constructor bc = B.getDeclaredConstructor(String.class);
					bda = (Comparable) bc.newInstance(((Element) a).getAttribute(attributeName));
					bdb = (Comparable) bc.newInstance(((Element) b).getAttribute(attributeName));
				} catch (final Exception e) {
					return 0; // yes, ugly, i know :)
				}
				ret = bda.compareTo(bdb);
				return asc ? ret : -ret;
			}
		}

		final List<Node> x = new ArrayList<>();
		for (int i = 0; i < nl.getLength(); i++) {
			x.add(nl.item(i));
		}
		Node[] ret = new Node[x.size()];
		ret = x.toArray(ret);
		Arrays.sort(ret, new NodeComparator<Node>());
		return ret;
	}
}
