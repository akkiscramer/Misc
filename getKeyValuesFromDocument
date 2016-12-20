import java.util.ArrayList;
import java.util.List;

/* Author: Akshith
* Date:12/20/2016
* Desc: This code will extract key value pairs from a webMethods IData document on SoftwareAG webMethods Integration Server.
*/

IDataCursor pipelineCursor = pipeline.getCursor();
		IData	inputDoc = IDataUtil.getIData( pipelineCursor, "inputDoc" );
		if ( inputDoc != null)
		{
			IData[] tempOutput=getKeyValues(inputDoc);
			IDataUtil.put( pipelineCursor, "outList", tempOutput);
		}
		pipelineCursor.destroy();	

// --- <<IS-BEGIN-SHARED-SOURCE-AREA>> ---
private static IData[] getKeyValues(IData inDoc){
		
		//Create a dynamic output array to store the key\value pairs from the document.
		ArrayList<IData> outputDocarr = new ArrayList<IData>();		
		IData	inputDoc = inDoc;
		if ( inputDoc != null)
		{
			IDataCursor idc=inputDoc.getCursor();
			IData output=null;
			IDataCursor idcOut=null;
			
			while(idc.next()){
				output= IDataFactory.create();
				idcOut= output.getCursor();
				
				// Add to the output doc array only if the cursor has a string or object key\value pair.				
				if ((idc.getValue() instanceof String) ||((idc.getValue() instanceof Object))){
					IDataUtil.put(idcOut, "key", idc.getKey());
					IDataUtil.put(idcOut, "value", idc.getValue());
					idcOut.destroy();
					outputDocarr.add(output);
				}
				else if(idc.getValue() instanceof IData){
					idc.next();
				}
			}
		}
		//Convert the dynamic array to IData Document list and return it. 
		return outputDocarr.toArray(new IData[outputDocarr.size()]);
	}
		// --- <<IS-END-SHARED-SOURCE-AREA>> ---
