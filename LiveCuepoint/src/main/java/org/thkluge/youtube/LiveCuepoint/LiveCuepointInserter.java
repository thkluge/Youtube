package org.thkluge.youtube.LiveCuepoint;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.LiveBroadcastListResponse;
import com.google.api.services.youtubePartner.YouTubePartner;
import com.google.api.services.youtubePartner.YouTubePartner.LiveCuepoints.Insert;
import com.google.api.services.youtubePartner.YouTubePartnerScopes;
import com.google.api.services.youtubePartner.model.CuepointSettings;
import com.google.api.services.youtubePartner.model.LiveCuepoint;
import com.google.common.collect.Lists;

/**
 * Hello world!
 *
 */
public class LiveCuepointInserter 
{
	private static String APPLICATION_NAME = "LiveCueInserter";
	private static List<String> scopes = Lists.newArrayList(YouTubePartnerScopes.YOUTUBEPARTNER, YouTubeScopes.YOUTUBE, YouTubeScopes.YOUTUBE_FORCE_SSL);
	
	private static String PROPERTY_DIR = "YTHelper";
	private static String PROPERTY_FILE = "ythelper.properties";
	private static String PROP_CONTENT_OWNER_ID = "CONTENT_OWNER_ID";
	private static String PROP_CONTENT_OWNER_ID_DEFAULT = "Please insert your external Content Owner ID here.";
	private static String PROP_CHANNEL_ID = "CHANNEL_ID";
	private static String PROP_CHANNEL_ID_DEFAULT = "Please insert your channel ID here";
	private static String PROP_INSERT_SLATE = "INSERT_SLATE";
	private static String PROP_INSERT_SLATE_DEFAULT = "true";
	
	private static YouTube youtube;
	private static YouTubePartner youTubePartner;
	
    public static void main( String[] args ) throws IOException, InterruptedException
    {
    	Properties prop = initProperties();
        Credential credential = YTAuth.authorize(scopes, "insertMidroll");
        
        initYTAPI(credential);
        initYTPartnerAPI(credential);
        
        String videoID = getCurrentLiveStreamID();
        
        if(!Strings.isNullOrEmpty(videoID)){
        	if(Boolean.valueOf(prop.getProperty(PROP_INSERT_SLATE, "true"))){
        		changeSlateStatus(videoID, true);
        	}
        	
        	insertMidroll(videoID, prop.getProperty(PROP_CHANNEL_ID), prop.getProperty(PROP_CONTENT_OWNER_ID));
        	
        	if(Boolean.valueOf(prop.getProperty(PROP_INSERT_SLATE, "true"))){
        		Thread.sleep(30000);	
        		changeSlateStatus(videoID, false);
        	}
        }
    }
    
    private static Properties initProperties() throws IOException {
		Properties prop = new Properties();
		
		String path = System.getProperty("user.dir").concat(File.separator).concat(PROPERTY_DIR).concat(File.separator);
		
		InputStream inStream;
		try {
			inStream = new FileInputStream(path.concat(PROPERTY_FILE));
			prop.load(inStream);
			
			if(prop.getProperty(PROP_CHANNEL_ID).equals(PROP_CHANNEL_ID_DEFAULT)){
				
			}
			if(prop.getProperty(PROP_CONTENT_OWNER_ID).equals(PROP_CONTENT_OWNER_ID_DEFAULT)){
				
			}
			
		} catch (FileNotFoundException e) {
			prop.setProperty(PROP_CONTENT_OWNER_ID, PROP_CONTENT_OWNER_ID_DEFAULT);
			prop.setProperty(PROP_CHANNEL_ID, PROP_CHANNEL_ID_DEFAULT);
			prop.setProperty(PROP_INSERT_SLATE, PROP_INSERT_SLATE_DEFAULT);
			
			OutputStream out = new FileOutputStream(path.concat(PROPERTY_FILE));
			prop.store(out, null);
		}
		return prop;
	}

	private static void initYTAPI(Credential credential){
    	youtube = new YouTube.Builder(YTAuth.HTTP_TRANSPORT, YTAuth.JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
    }
    
    private static void initYTPartnerAPI(Credential credential){
    	youTubePartner = new YouTubePartner.Builder(YTAuth.HTTP_TRANSPORT, YTAuth.JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
    }
    
    private static String getCurrentLiveStreamID() throws IOException{
    	YouTube.LiveBroadcasts.List liveBroadcastRequest = youtube.liveBroadcasts().list("id").setBroadcastStatus("active").setBroadcastType("all");
    	LiveBroadcastListResponse returnedListResponse = liveBroadcastRequest.execute();
    	
    	if(returnedListResponse.getItems().size() == 1){
    		return returnedListResponse.getItems().get(0).getId();
    	}else{
    		return null;
    	}
    }
    
    private static void changeSlateStatus(String videoID, boolean showSlide) throws IOException{
    	
    	YouTube.LiveBroadcasts.Control control = youtube.liveBroadcasts().control(videoID, "id").setDisplaySlate(showSlide);
    	control.execute();
    	
    }
    
    private static void insertMidroll(String videoID, String channelID, String contentOwnerID) throws IOException{
    	

    	CuepointSettings settings = new CuepointSettings();
    	settings.setCueType("ad");
    	settings.setDurationSecs(30l);
    	
    	LiveCuepoint content = new LiveCuepoint();
    	content.setId(videoID);
		content.setSettings(settings);
    	
		Insert insertCuePoint = youTubePartner.liveCuepoints().insert(channelID, content).setOnBehalfOfContentOwner(contentOwnerID);
		insertCuePoint.execute();
    	
    }
    
}
