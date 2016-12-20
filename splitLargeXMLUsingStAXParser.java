import java.io.*;
import java.util.*;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import javax.xml.transform.stream.StreamSource;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.*;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import org.apache.commons.lang.StringEscapeUtils;


/*
* Author: Akshith
* Date: 12/20/2016
* Desc: This code can be used to split huge XML files in to smaller XML files in SoftwareAG webMethods Integration server.
*/


IDataCursor pipelineCursor = pipeline.getCursor();
		String	xmlFileAbsoluteName = IDataUtil.getString( pipelineCursor, "xmlFileAbsoluteName" );
		String	rootNode = IDataUtil.getString( pipelineCursor, "rootNode" );
		String	repeatingNode = IDataUtil.getString( pipelineCursor, "repeatingNode" );
		String	splitFilesTargetDirectory = IDataUtil.getString( pipelineCursor, "splitFilesTargetDirectory" );
		String	splitFilePrefixName = IDataUtil.getString( pipelineCursor, "splitFilePrefixName" );
		String	numOfRecordsPerFile = IDataUtil.getString( pipelineCursor, "numOfRecordsPerFile" );		
		pipelineCursor.destroy();		
				
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();	    
		outputFactory.setProperty("javax.xml.stream.isRepairingNamespaces", Boolean.TRUE);
		
		int count = 0;
		int nodeCount=0;
		File f = new File(xmlFileAbsoluteName);
		String sourceFilePath = f.getParent();
		String tempOutputFilePrefix=sourceFilePath+"\\"+splitFilePrefixName;
		QName name = new QName(repeatingNode);
		XMLEventReader reader = null;
		FileReader tempFileReader = null;
		try {
				tempFileReader=new FileReader(xmlFileAbsoluteName);
				reader = inputFactory.createXMLEventReader(tempFileReader);
								
				while (reader.hasNext()) {
					XMLEvent event = reader.nextEvent();
					
					if(event.getEventType() == XMLStreamConstants.START_ELEMENT){
						StartElement startElement = event.asStartElement();
						if(startElement.getName().equals(name)){
							if(count==Integer.parseInt(numOfRecordsPerFile)){
								nodeCount++;
								count=0;
							}
							if(count<Integer.parseInt(numOfRecordsPerFile)){
								writeToFile(reader, event, tempOutputFilePrefix+ (nodeCount) + ".xml_init");								
								count++;
							}
						}
					}
					if (event.isEndDocument())
						break;
				}				
			reader.close();
			tempFileReader.close();
			
			//Adding root tag to the xml files.
			addRootTag(sourceFilePath,splitFilesTargetDirectory, rootNode,splitFilePrefixName);
			
			/*Code to rename the processed file.
			 * File renameOrigianlFile=new File(xmlFileAbsoluteName+"_processed");
			 * f.renameTo(renameOrigianlFile);
			*/
			
			new File(xmlFileAbsoluteName).delete();
			
			IDataUtil.put(pipelineCursor, "splitStatus", "success");
			} catch (FileNotFoundException e) {
				IDataUtil.put(pipelineCursor, "splitStatus", e.toString());
				writeToApplicationLogFile("ERROR: "+e.toString(), logFileName, project);
			} catch (XMLStreamException e) {
				IDataUtil.put(pipelineCursor, "splitStatus", e.toString());
				writeToApplicationLogFile("ERROR: "+e.toString(), logFileName, project);
			} catch (IOException e) {
				IDataUtil.put(pipelineCursor, "splitStatus", e.toString());
				writeToApplicationLogFile("ERROR: "+e.toString(), logFileName, project);
			}			
			pipelineCursor.destroy();
	}
	
	// --- <<IS-BEGIN-SHARED-SOURCE-AREA>> ---
	
	static String logFileName="..\\default\\logs\\applicationLogs\\LargeFileHandling.log";
	static String project="WHATEVER";
	private static String NEWLINE = "\n";
		
	private static void writeToFile(XMLEventReader reader,XMLEvent startEvent,String filename) throws XMLStreamException, IOException {
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();	    
		outputFactory.setProperty("javax.xml.stream.isRepairingNamespaces", Boolean.TRUE);
		StartElement element = startEvent.asStartElement();
		QName name = element.getName();
		int stack = 1;
		XMLEventWriter writer=null;
		boolean appendFlag=false;
		File nodeFileName=new File(filename);
		if(nodeFileName.exists()){
			appendFlag=true;			
		}
		FileWriter nodeFileWriter=new FileWriter(nodeFileName,appendFlag);
		try {
			writer = outputFactory.createXMLEventWriter(nodeFileWriter);
			writer.add(element);
			while (reader.hasNext()) {
			XMLEvent event = reader.nextEvent();
			if (event.isStartElement()
			        && event.asStartElement().getName().equals(name))
			    stack++;
			if (event.isEndElement()) {
			    EndElement end = event.asEndElement();
			    if (end.getName().equals(name)) {
			        stack--;
			        if (stack == 0) {
			            writer.add(event);
			            break;
			        }
			    }
			}
			writer.add(event);
			}
		} catch (Exception e) {
			writeToApplicationLogFile("ERROR: "+e.toString(), logFileName, project);
		}
		finally{			
			writer.flush();
			writer.close();
			nodeFileWriter.close();
		}
	}
	
	private static void writeToApplicationLogFile(String logData,String logFileName, String project){
		IData input = IDataFactory.create();
		IDataCursor inputCursor = input.getCursor();
		IDataUtil.put( inputCursor, "data", logData);
		IDataUtil.put( inputCursor, "logFileName", logFileName );
		IDataUtil.put( inputCursor, "projectName", project);
		inputCursor.destroy();
		IData 	output = IDataFactory.create();
		try{
			output = Service.doInvoke( "servicefolderName", "serviceName", input );
		}catch( Exception e){}
	}
	
	private static void addRootTag(String processedFilesDirectoryName,String targetFilesDirectoryName, String rootName, String filPrefix) {
		if(targetFilesDirectoryName.isEmpty() || targetFilesDirectoryName==null){
			targetFilesDirectoryName=processedFilesDirectoryName;
		}
		File dir = new File(processedFilesDirectoryName);
		File[] files = dir.listFiles(new FilenameFilter() { 
			public boolean accept(File dir, String filename)
			     { 
			    	 return filename.endsWith(".xml_init"); 
			     }
			}
		);
		List<String> startContent=new ArrayList<String>();
		for (int i = 0; i < files.length; i++) {
			startContent.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			startContent.add("<"+rootName+">");
			try {
				FileReader fReader=new FileReader(files[i]);
				BufferedReader reader = new BufferedReader(fReader);
				String         line = null;
			    StringBuilder  stringBuilder = new StringBuilder();
			    while( ( line = reader.readLine() ) != null ) {
			        stringBuilder.append( line );
			        stringBuilder.append( "\n" );
			    }
			    startContent.add(stringBuilder.toString());
			    reader.close();
			    fReader.close();
			} catch (FileNotFoundException e1) {
				writeToApplicationLogFile("ERROR: addRoot FilenotFound read existing file."+e1.toString(), logFileName, project);
			} catch (IOException e) {
				writeToApplicationLogFile("ERROR: addRoot Read existing file"+e.toString(), logFileName, project);
			}
			startContent.add("</"+rootName+">");
		    
			//Create new file with the root info.			  
			try {
				File temp=new File(targetFilesDirectoryName+"\\"+filPrefix+"_"+i+".xml");
				if (!temp.exists()) {
					temp.createNewFile();
				}
				FileWriter writer = new FileWriter(temp); 
				for(String str: startContent) {
				  writer.write(str);
				}
				writer.close();
			} catch (UnsupportedEncodingException e) {
				writeToApplicationLogFile("ERROR: addRoot Unsupported"+e.toString(), logFileName, project);
			} catch (FileNotFoundException e) {
				writeToApplicationLogFile("ERROR: addRoot FileNotFound"+e.toString(), logFileName, project);
			} catch (IOException e) {
				writeToApplicationLogFile("ERROR: addRoot IOException"+e.toString(), logFileName, project);
			}
			startContent.clear();
			files[i].delete();
		}
	}
