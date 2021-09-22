package asia.leadsgen.psp.server.handler.campaign_v2;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gson.Gson;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service.DesignService;
import asia.leadsgen.psp.service.ImageService;
import asia.leadsgen.psp.service.ProductDesignService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import asia.leadsgen.psp.util.StringPool;
import io.vertx.core.json.JsonObject;

public class DesignCreateV2 {
	
	public static Map insert(String campaignId, String productId, DesignModel designModel, Map baseInfo, String state, boolean isMainDesign, int imgPosition) throws SQLException {
		
		if (designModel == null) {
			throw new BadRequestException(SystemError.INVALID_IMAGE_ENCODED_DATA);
		}
		
		String designType = designModel.getType();
		if (designType == null || designType.isEmpty()
				|| (!designType.equalsIgnoreCase(AppConstants.DESIGN_TYPE_FRONT)
						&& !designType.equalsIgnoreCase(AppConstants.DESIGN_TYPE_BACK))) {
			throw new BadRequestException(SystemError.INVALID_FILE_TYPE);
		}
		
		LOGGER.info("designModel= " + designModel.toString());
		
		String imageUrl = designModel.getUrl();
		
		String thumbUrl = designModel.getThumb_url();
		
		String custom_texts = designModel.getCustom_texts();
		
		Map<String, Object> designInfoMap = new LinkedHashMap<>(); 
		
		if (imageUrl != null && imageUrl.isEmpty() == false) {
			
			String imageWidth = designModel.getWidth();
			
			String imageHeight = designModel.getHeight();
			
			String cropGeometry = designModel.getCrop_geometry();
			
			String printableTop = designModel.getPrintable_top();
			
			String printableLeft = designModel.getPrintable_left();
			
			String printableWidth = designModel.getPrintable_width();
			
			String printableHeight = designModel.getPrintable_height();
			
			String zIndex = designModel.getZIndex();
					
			Map<String, Object> imageInfoMap = createDesignImage(productId, StringPool.BLANK, imageUrl,
					imageWidth, imageHeight, printableTop, printableLeft, printableWidth, printableHeight,
					thumbUrl, "", 0, 0);		
			
			String imageId = ParamUtil.getString(imageInfoMap, AppParams.ID);
			
			Map createDesignMap = createDesign(designType, imageId, imgPosition, "");
			
			String designId = ParamUtil.getString(createDesignMap, AppParams.ID);
			
			if (state.equalsIgnoreCase(ResourceStates.SHOPIFY_APP)) {
				isMainDesign = true;
			}
			LOGGER.info("isMainDesign= " + isMainDesign);
			ProductDesignService.insert(productId, designId, isMainDesign, "0.00");
			
			ImageService.update(imageId, imageWidth, imageHeight, printableTop, printableLeft, printableWidth,
					printableHeight, "", cropGeometry, zIndex);
			
			if (custom_texts != null && custom_texts.isEmpty() == false) {
				
				Map customData = new LinkedHashMap<>();
				customData.put("base", baseInfo);
				
				String customTexts = "{\"custom_data\":" + custom_texts + "}";
				
				Map customTextsMap = new Gson().fromJson(customTexts, Map.class);
				LOGGER.info("customTextsMap= " + customTextsMap);
				customData.putAll(customTextsMap);
				
				String customDataString = new JsonObject(customData).encode();
				
				LOGGER.info("customData= " + customData);
				DesignService.addCustomTexts(designId, customDataString);
			}
			
			designInfoMap = DesignService.get(designId);
			
		} else {
			throw new BadRequestException(SystemError.INVALID_DESIGN);
		}
		
		return designInfoMap;	
	}

	private static Map<String, Object> createDesignImage(String productId, String mimeType, String imageUrl, String imageWidth,
			String imageHeight, String printableTop, String printableLeft, String printableWidth,
			String printableHeight, String thumbUrl, String colors, int totalColors, int dpi) throws SQLException {
		
		return ImageService.insert(mimeType, productId, "", imageUrl, imageWidth, imageHeight, printableTop,
				printableLeft, printableWidth, printableHeight, thumbUrl, colors, totalColors, dpi);
	}
	
	private static Map<String, Object> createDesign(String designType, String imageId, int imagePosition, String artId) throws SQLException {

		return DesignService.insert(designType, imageId, imagePosition, artId, 0, "", 0, "", "");
	}
	
	private static final Logger LOGGER = Logger.getLogger(DesignCreateV2.class.getName());
}
