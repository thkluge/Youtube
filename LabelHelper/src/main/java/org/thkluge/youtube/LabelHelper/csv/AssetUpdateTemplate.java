package org.thkluge.youtube.LabelHelper.csv;

import com.google.common.base.Strings;

public class AssetUpdateTemplate {

	private String asset_id;
	private String add_asset_label = "";
	
	public AssetUpdateTemplate(){
	}
	
	public String getAsset_id() {
		return asset_id;
	}
	public void setAsset_id(String asset_id) {
		this.asset_id = asset_id;
	}
	public String getAdd_asset_label() {
		return add_asset_label;
	}
	public void setAdd_asset_label(String add_asset_label) {
		this.add_asset_label = add_asset_label;
	}
	
	public void addLabel(String label){
		if(!Strings.isNullOrEmpty(this.add_asset_label)){
			this.add_asset_label = this.add_asset_label.concat("|");
		}
		if(!Strings.isNullOrEmpty(label)){
			this.add_asset_label = this.add_asset_label.concat(label);
		}
	}
}
