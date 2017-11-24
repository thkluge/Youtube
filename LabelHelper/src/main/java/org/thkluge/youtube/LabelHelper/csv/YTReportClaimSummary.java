package org.thkluge.youtube.LabelHelper.csv;

import com.opencsv.bean.CsvBindByName;

public class YTReportClaimSummary {

	@CsvBindByName(column = "Video ID")
	private String video_id;
	@CsvBindByName(column = "Channel ID")
	private String channel_id;
	@CsvBindByName(column = "Asset ID")
	private String asset_id;
	@CsvBindByName(column = "Asset Channel ID")
	private String asset_channel_id;
	@CsvBindByName(column = "Custom ID")
	private String custom_id;
	//@CsvBindByName(column = "TMS")
	//private final String tms = "";
	@CsvBindByName(column = "Content Type")
	private String content_type;
	@CsvBindByName(column = "Policy")
	private String policy;
	@CsvBindByName(column = "Claim Type")
	private String claim_type;
	@CsvBindByName(column = "Claim Origin")
	private String claim_origin;
	@CsvBindByName(column = "Multiple Claims?")
	private String muliple_claims;
	
	public String getVideo_id() {
		return video_id;
	}
	public void setVideo_id(String video_id) {
		this.video_id = video_id;
	}
	public String getChannel_id() {
		return channel_id;
	}
	public void setChannel_id(String channel_id) {
		this.channel_id = channel_id;
	}
	public String getAsset_id() {
		return asset_id;
	}
	public void setAsset_id(String asset_id) {
		this.asset_id = asset_id;
	}
	public String getAsset_channel_id() {
		return asset_channel_id;
	}
	public void setAsset_channel_id(String asset_channel_id) {
		this.asset_channel_id = asset_channel_id;
	}
	public String getCustom_id() {
		return custom_id;
	}
	public void setCustom_id(String custom_id) {
		this.custom_id = custom_id;
	}
//	public String getTms() {
//		return tms;
//	}
	public String getContent_type() {
		return content_type;
	}
	public void setContent_type(String content_type) {
		this.content_type = content_type;
	}
	public String getPolicy() {
		return policy;
	}
	public void setPolicy(String policy) {
		this.policy = policy;
	}
	public String getClaim_type() {
		return claim_type;
	}
	public void setClaim_type(String claim_type) {
		this.claim_type = claim_type;
	}
	public String getClaim_origin() {
		return claim_origin;
	}
	public void setClaim_origin(String claim_origin) {
		this.claim_origin = claim_origin;
	}
	public String getMuliple_claims() {
		return muliple_claims;
	}
	public void setMuliple_claims(String muliple_claims) {
		this.muliple_claims = muliple_claims;
	}
}
