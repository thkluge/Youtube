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

import org.apache.log4j.Logger;

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
import com.google.common.io.Files;

/**
 * Hello world!
 *
 */
public class LiveCuepointInserter 
{
	private final static Logger log = Logger.getLogger(LiveCuepointInserter.class);
	
	private static String APPLICATION_NAME = "LiveCueInserter";
	private static List<String> scopes = Lists.newArrayList(YouTubePartnerScopes.YOUTUBEPARTNER, YouTubeScopes.YOUTUBE, YouTubeScopes.YOUTUBE_FORCE_SSL);
	
	public static String PROPERTY_DIR = "YTHelper";
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
    	log.info("Start LiveCuepointInserter.");
    	Properties prop = initProperties();
    	log.info("Load Credentials.");
        Credential credential = YTAuth.authorize(scopes, "insertMidroll");
        
        initYTAPI(credential);
        initYTPartnerAPI(credential);
        
        String videoID = getCurrentLiveStreamID(prop.getProperty(PROP_CHANNEL_ID), prop.getProperty(PROP_CONTENT_OWNER_ID));
        
        if(!Strings.isNullOrEmpty(videoID)){
        	if(Boolean.valueOf(prop.getProperty(PROP_INSERT_SLATE, "true"))){
        		log.info(String.format("Insert Slate for Video %s", videoID));
        		changeSlateStatus(videoID, prop.getProperty(PROP_CHANNEL_ID), prop.getProperty(PROP_CONTENT_OWNER_ID), true);
        	}
        	
        	insertMidroll(videoID, prop.getProperty(PROP_CHANNEL_ID), prop.getProperty(PROP_CONTENT_OWNER_ID));
        	
        	if(Boolean.valueOf(prop.getProperty(PROP_INSERT_SLATE, "true"))){
        		log.info("Wait for 30 seconds");
        		Thread.sleep(30000);	
        		log.info(String.format("Remove Slate for Video %s", videoID));
        		changeSlateStatus(videoID, prop.getProperty(PROP_CHANNEL_ID), prop.getProperty(PROP_CONTENT_OWNER_ID), false);
        	}
        }
    }
    
    private static Properties initProperties() throws IOException {
    	log.info("Start loading Properties");
		Properties prop = new Properties();
		
		String path = System.getProperty("user.home").concat(File.separator).concat(PROPERTY_DIR).concat(File.separator);
		log.info(String.format("Search for file %s in folder %s", PROPERTY_FILE, path));
		
		InputStream inStream;
		try {
			inStream = new FileInputStream(path.concat(PROPERTY_FILE));
			prop.load(inStream);
			
		} catch (FileNotFoundException e) {
			prop.setProperty(PROP_CONTENT_OWNER_ID, PROP_CONTENT_OWNER_ID_DEFAULT);
			prop.setProperty(PROP_CHANNEL_ID, PROP_CHANNEL_ID_DEFAULT);
			prop.setProperty(PROP_INSERT_SLATE, PROP_INSERT_SLATE_DEFAULT);
			
			new File(path).mkdirs();
			OutputStream out = new FileOutputStream(path.concat(PROPERTY_FILE));
			prop.store(out, null);
			log.error(String.format("Could not find property file. Generating Default Property file. Please open the file %s and insert information.", path.concat(PROPERTY_FILE)));
		}
		
		if(prop.getProperty(PROP_CHANNEL_ID).equals(PROP_CHANNEL_ID_DEFAULT)){
			log.error("Channel ID is still default value. Please insert the ID of your YT channel in the property file.");
		}
		if(prop.getProperty(PROP_CONTENT_OWNER_ID).equals(PROP_CONTENT_OWNER_ID_DEFAULT)){

			log.error("Content OWner ID is still default value. Please insert the ID of your YT Content Owner in the property file.");
		}
		
		return prop;
	}

	private static void initYTAPI(Credential credential){
		log.info("Init YouTube API");
    	youtube = new YouTube.Builder(YTAuth.HTTP_TRANSPORT, YTAuth.JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
    }
    
    private static void initYTPartnerAPI(Credential credential){
    	log.info("Init YouTube Partner API");
    	youTubePartner = new YouTubePartner.Builder(YTAuth.HTTP_TRANSPORT, YTAuth.JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
    }
    
    private static String getCurrentLiveStreamID(String channelID, String contentOwnerID) throws IOException{
    	log.info("Search current Live Stream");
    	YouTube.LiveBroadcasts.List liveBroadcastRequest = youtube.liveBroadcasts().
    																list("id").
    																setBroadcastStatus("active").
    																setBroadcastType("all").
    																setOnBehalfOfContentOwner(contentOwnerID).
    																setOnBehalfOfContentOwnerChannel(channelID);
    	LiveBroadcastListResponse returnedListResponse = liveBroadcastRequest.execute();
    	
    	if(returnedListResponse.getItems().size() == 1){
    		String videoID = returnedListResponse.getItems().get(0).getId();
    		log.info(String.format("Found one live stream with video id %s", videoID));
    		return videoID;
    	}else{
    		log.error(String.format("Found %s live streams. Not sure what do to. No Midrolls will be inserted", returnedListResponse.getItems().size()));
    		return null;
    	}
    }
    
    private static void changeSlateStatus(String videoID, String channelID, String contentOwnerID, boolean showSlide) throws IOException{
    	
    	YouTube.LiveBroadcasts.Control control = youtube.liveBroadcasts().
    														control(videoID, "id").
    														setDisplaySlate(showSlide).
    														setOnBehalfOfContentOwner(contentOwnerID).
															setOnBehalfOfContentOwnerChannel(channelID);;
    	control.execute();
    	
    }
    
    private static void insertMidroll(String videoID, String channelID, String contentOwnerID) throws IOException{
    	log.info(String.format("Start insert Midroll for video %s on channel %s for content owner %s", videoID, channelID, contentOwnerID));
    	CuepointSettings settings = new CuepointSettings();
    	settings.setCueType("ad");
    	settings.setDurationSecs(30l);
    	
    	LiveCuepoint content = new LiveCuepoint();
    	content.setBroadcastId(videoID);
		content.setSettings(settings);
    	
		Insert insertCuePoint = youTubePartner.liveCuepoints().insert(channelID, content).setOnBehalfOfContentOwner(contentOwnerID);
		LiveCuepoint result = insertCuePoint.execute();
		
		log.info(result.toPrettyString());
    	
    }
    
}
