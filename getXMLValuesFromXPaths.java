import javax.xml.parsers.*;
import javax.xml.xpath.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.io.*;
import java.util.*;

/* Author: Akshith
* Date: 12/20/2016
* Desc: This code will return a string list of values from XML based on XPATH in SoftwareAG webMethods Integration Server.
*/

// pipeline
		IDataCursor pipelineCursor = pipeline.getCursor();
		String	inXMLString = IDataUtil.getString( pipelineCursor, "inXMLString" );
		String[]	xPaths = IDataUtil.getStringArray( pipelineCursor, "xPaths" );
		pipelineCursor.destroy();
		
		if(inXMLString.isEmpty()){
			IDataUtil.put(pipelineCursor, "error", "Input XML String is empty!!");
		}
		else{
			try {
				DocumentBuilderFactory builderfactory = DocumentBuilderFactory.newInstance();
				builderfactory.setNamespaceAware(true);
				DocumentBuilder builder=builderfactory.newDocumentBuilder();
				InputSource is=new InputSource(new StringReader(inXMLString));
				org.w3c.dom.Document xmlDocument = builder.parse(is);
				XPathFactory xPathFactory = javax.xml.xpath.XPathFactory.newInstance();
				XPath xPath = xPathFactory.newXPath();
				
				List<String> outputValues=new ArrayList<String>();
				for (int i=0;i<xPaths.length;i++){
					XPathExpression xPathExpression =xPath.compile(xPaths[i]);					
					outputValues.add(xPathExpression.evaluate(xmlDocument, XPathConstants.STRING).toString());
				}
				String[] returnValues=new String[outputValues.size()];
				IDataUtil.put(pipelineCursor, "returnValues", outputValues.toArray(returnValues));		
			}
			catch (ParserConfigurationException e) {
				IDataUtil.put(pipelineCursor, "error", e.toString());
			} catch (SAXException e) {
				IDataUtil.put(pipelineCursor, "error", e.toString());
			} catch (IOException e) {
				IDataUtil.put(pipelineCursor, "error", e.toString());
			} catch (XPathExpressionException e) {
				IDataUtil.put(pipelineCursor, "error", e.toString());
			}
		}
		pipelineCursor.destroy();	
