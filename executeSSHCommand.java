import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/*
* Author: Akshith
* Date: 12/20/2016
* Desc: This code will accept a Unix command as input and execute it remotely using SSH on a Unix machine using 
* Jsch libraries on SoftwareAG webMethods Integration server. The results from the command will also be logged back
* to a folder on the Unix machine using SFTP connection.
*/

// pipeline

IDataCursor pipelineCursor = pipeline.getCursor();
		String	userName = IDataUtil.getString( pipelineCursor, "userName" );
		String	password = IDataUtil.getString( pipelineCursor, "password" );
		String	serverName = IDataUtil.getString( pipelineCursor, "serverName" );
		String	port = IDataUtil.getString( pipelineCursor, "port" );
		String	command = IDataUtil.getString( pipelineCursor, "command" );
		String	threadSleep = IDataUtil.getString( pipelineCursor, "threadSleep" );
		
		pipelineCursor.destroy();
		
		if(threadSleep==null){
			threadSleep="1000";
		}
		
		JSch jsch=new JSch();
		com.jcraft.jsch.Session session;
		try {
			session = jsch.getSession(userName, serverName, Integer.valueOf(port));
			session.setPassword(password);
			Properties config = new Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();
		
			ChannelExec channel=(ChannelExec) session.openChannel("exec");
			BufferedReader in=new BufferedReader(new InputStreamReader(channel.getInputStream()));
			channel.setCommand(command);
			
			channel.connect();
			
			
		
			String msg=null;
			String temp = "";
			List<String> test=new ArrayList<String>();
			//Make sure to wait till the channel is closed and get the right exit code.
			while(true){
				while((msg=in.readLine())!=null){
					System.out.println(msg);
					test.add(msg);
					temp=temp+"\n"+msg;
				}
				if(channel.isClosed()){
					break;
				}
				//Thread.sleep(250);
				Thread.sleep(Long.parseLong(threadSleep));				
			}
			
			//Optional logging to the Unix server.
			IData	logging = IDataUtil.getIData( pipelineCursor, "logging" );
			if ( logging != null)
			{
				IDataCursor loggingCursor = logging.getCursor();
				String	logDirectory = IDataUtil.getString( loggingCursor, "logDirectoryName" );
				String	logFileName = IDataUtil.getString( loggingCursor, "logFileName" );
				loggingCursor.destroy();
				logToUnix(session, logDirectory, logFileName,temp.trim());
			}
			
			channel.disconnect();
			session.disconnect();
			IDataUtil.put( pipelineCursor, "status", getParsedExitStatus(temp.trim()));			
			IDataUtil.put( pipelineCursor, "returnMessage", temp.trim());
		} catch (JSchException e) {
			IDataUtil.put( pipelineCursor, "returnMessage", e.toString());
		} catch (IOException e) {
			IDataUtil.put( pipelineCursor, "returnMessage", e.toString());
		} catch (InterruptedException e) {
			IDataUtil.put( pipelineCursor, "returnMessage", e.toString());
		} catch (SftpException e) {
			IDataUtil.put( pipelineCursor, "returnMessage", e.toString());
		}
		pipelineCursor.destroy();
    
    	// --- <<IS-BEGIN-SHARED-SOURCE-AREA>> ---
    
    private static void logToUnix(com.jcraft.jsch.Session session,String logDirectory,String logFileName,String logMessage) throws 
    JSchException, SftpException, IOException{
      Channel channel=session.openChannel("sftp");
      ChannelSftp sftpChannel = (ChannelSftp) channel;
      sftpChannel.connect();
      sftpChannel.cd(logDirectory);		
      InputStream inputStream = new ByteArrayInputStream(logMessage.getBytes());
      sftpChannel.put(inputStream, logDirectory+ logFileName,ChannelSftp.APPEND);
      sftpChannel.exit();
      inputStream.close();
	}
  
  	// --- <<IS-END-SHARED-SOURCE-AREA>> ---
