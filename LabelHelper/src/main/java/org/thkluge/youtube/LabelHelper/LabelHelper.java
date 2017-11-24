package org.thkluge.youtube.LabelHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.thkluge.youtube.LabelHelper.csv.AssetUpdateTemplate;
import org.thkluge.youtube.LabelHelper.csv.YTReportClaimSummary;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.common.collect.Lists;
import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;


public class LabelHelper 
{
	private final static Logger log = Logger.getLogger(LabelHelper.class);

	private static final String PROPERTY_FILE = "labelHelper.properties";
	private static final String PROP_LABEL_CHANNEL = "LABEL_CHANNEL";
	private static final String PROP_LABEL_VIDEO_LENGHT = "LABEL_VIDEO_LENGHT";
	private static final String PROP_LABEL_VIDEO_PUB = "LABEL_VIDEO_PUB";

	private static final String APPLICATION_NAME = "LabelHelper";
	public static  final String PROPERTY_DIR = "YTHelper";
	public static final String CLAIM_REPORT_FILE = "claim_summary.csv";
	private static final String ASSET_UPDATE_FILE = "asset_update_labels.csv";
	public static String PATH = System.getProperty("user.home").concat(File.separator).concat(LabelHelper.PROPERTY_DIR).concat(File.separator);

	private static List<String> scopes = Lists.newArrayList(YouTubeScopes.YOUTUBE, 
															YouTubeScopes.YOUTUBE_FORCE_SSL);
	private static YouTube youtube;
	
    public static void main( String[] args ) throws IOException, InterruptedException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException
    {
    	log.info("Start LabelHelper.");
    	Properties prop = initProperties();

    	if(!(Boolean.valueOf(prop.getProperty(PROP_LABEL_CHANNEL, "true")) &&
    			Boolean.valueOf(prop.getProperty(PROP_LABEL_VIDEO_LENGHT, "true")) &&
    					Boolean.valueOf(prop.getProperty(PROP_LABEL_VIDEO_PUB, "true")))){
    		log.warn("Exit LabelHelper since there is nothing to do here (check property file)");
    		return;
    	}
    	
    	log.info("Load Credentials.");
        Credential credential = YTAuth.authorize(scopes, "labelHelper");
        initYTAPI(credential);
        
        List<YTReportClaimSummary> claimList = readClaimReport();
        
        log.info("Filter Claim List for Parter-provided");
        claimList = claimList.stream().filter(claim -> 
        			claim.getContent_type().equals("Partner-provided") && claim.getAsset_channel_id() != null).
        			collect(Collectors.toList());
        
        claimList = removeDuplicates(claimList);
        
        List<List<YTReportClaimSummary>> batches = Lists.partition(claimList, 50);
        
        log.info("Start gathering video information...");
        Map<String, Video> videoMap = new HashMap<String, Video>();
        VideoListResponse listVideoResponse;
		for (List<YTReportClaimSummary> list : batches) {
        	listVideoResponse = listVideosFromClaims(list);
        	addVideosToMap(videoMap,listVideoResponse.getItems());
		}
		
		List<AssetUpdateTemplate> assetUpdateList = new ArrayList<>();
    	
		Calendar cal = Calendar.getInstance();
		
		log.info("Generating Asset Update csv...");
    	for (YTReportClaimSummary claim : claimList) {
			AssetUpdateTemplate assetUpdateTemplate = new AssetUpdateTemplate();
			assetUpdateTemplate.setAsset_id(claim.getAsset_id());
			
			if(Boolean.valueOf(prop.getProperty(PROP_LABEL_CHANNEL, "true"))){
				assetUpdateTemplate.addLabel(claim.getChannel_id());
			}
			
			Video video = videoMap.get(claim.getVideo_id());		
			if(Boolean.valueOf(prop.getProperty(PROP_LABEL_VIDEO_LENGHT, "true"))){
				long duration = Duration.parse(video.getContentDetails().getDuration()).getSeconds();
				if(duration <= 120){
					assetUpdateTemplate.addLabel("Video Length Short");
				}else if (duration <= 600){
					assetUpdateTemplate.addLabel("Video Length Medium");
				}else{
					assetUpdateTemplate.addLabel("Video Length Long");
				}
			}
			
			if(Boolean.valueOf(prop.getProperty(PROP_LABEL_VIDEO_PUB, "true"))){
				Timestamp timestamp = new Timestamp(video.getSnippet().getPublishedAt().getValue());
				cal.setTimeInMillis(timestamp.getTime());
				assetUpdateTemplate.addLabel(String.valueOf(cal.get(Calendar.YEAR)));
			}
			
			assetUpdateList.add(assetUpdateTemplate);
		}
    	
    	log.info(String.format("Write asset update csv to %s", PATH.concat(ASSET_UPDATE_FILE)));
    	Writer writer = new FileWriter(PATH.concat(ASSET_UPDATE_FILE));
        StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder(writer).withQuotechar(CSVWriter.NO_QUOTE_CHARACTER).build();
        beanToCsv.write(assetUpdateList);
        writer.close();

    }

	private static List<YTReportClaimSummary> readClaimReport() throws IOException {
		log.info(String.format("Start Read Claim Report %s from %s", 
        		CLAIM_REPORT_FILE,
        		PATH));
		try{
	       return new CsvToBeanBuilder<YTReportClaimSummary>(
	        		new FileReader(PATH.concat(CLAIM_REPORT_FILE)))
	        		.withType(YTReportClaimSummary.class)
	        		.build().parse();
        }catch (IOException e) {
			log.error(String.format("Can not read file %s", PATH.concat(CLAIM_REPORT_FILE)));
			throw new IOException(e);
		}
	}
    
    private static void addVideosToMap(Map<String, Video> finalVideoMap, List<Video> items) {
		for (Video video : items) {
			if(finalVideoMap.containsKey(video.getId())){
				log.info(String.format("Dublicated video ID %s", video.getId()));
			}
			finalVideoMap.put(video.getId(), video);
		}
	}
    
    private static VideoListResponse listVideosFromClaims(List<YTReportClaimSummary> claims) throws IOException{
    	String videoIds = new String();
		for (YTReportClaimSummary claim : claims) {
			videoIds = videoIds.concat(claim.getVideo_id()).concat(",");
		}
		videoIds = videoIds.substring(0,videoIds.length()-1);
		YouTube.Videos.List listVideoRequest = youtube.videos().list("snippet,contentDetails").setId(videoIds);
    	VideoListResponse result = listVideoRequest.execute();
		return result;
    }
    
    private static void initYTAPI(Credential credential){
		log.info("Init YouTube API");
    	youtube = new YouTube.Builder(YTAuth.HTTP_TRANSPORT, YTAuth.JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
    }
    
    /**
     * Have to remove double claim entries from report.
     * @param claims
     * @return
     */
    public static List<YTReportClaimSummary> removeDuplicates(List<YTReportClaimSummary> claims){
    	Set<YTReportClaimSummary> s = new TreeSet<YTReportClaimSummary>(new Comparator<YTReportClaimSummary>() {

            @Override
            public int compare(YTReportClaimSummary o1, YTReportClaimSummary o2) {
                if(o1.getVideo_id().equals(o2.getVideo_id()) &&
                		o1.getAsset_id().equals(o2.getAsset_id())){
                	return 0;
                }
                return 1;
            }
        });
        s.addAll(claims);
        List<YTReportClaimSummary> result = new ArrayList<YTReportClaimSummary>();
        result.addAll(s);
        return result; 
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
			prop.setProperty(PROP_LABEL_CHANNEL, "true");
			prop.setProperty(PROP_LABEL_VIDEO_LENGHT, "true");
			prop.setProperty(PROP_LABEL_VIDEO_PUB, "true");
			
			new File(path).mkdirs();
			OutputStream out = new FileOutputStream(path.concat(PROPERTY_FILE));
			prop.store(out, null);
			log.error(String.format("Could not find property file. Generating Default Property file. Please open the file %s and insert information.", path.concat(PROPERTY_FILE)));
		}
		
		return prop;
	}
}
