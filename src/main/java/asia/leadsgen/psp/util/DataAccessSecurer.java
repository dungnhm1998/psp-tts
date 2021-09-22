package asia.leadsgen.psp.util;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.service.DomainService;
import asia.leadsgen.psp.service.EmailCampaignsService;
import asia.leadsgen.psp.service.MockupService;
import asia.leadsgen.psp.service.PromotionService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DataAccessSecurer {

	private static void secureSession(String userId) {
		if (StringUtils.isEmpty(userId)) {
			throw new LoginException(SystemError.LOGIN_REQUIRED);
		}
	}

	public static void secureCampaign(String userId, String campaignId) {
		secureSession(userId);
		if (StringUtils.isEmpty(campaignId)) {
			throw new BadRequestException(SystemError.INVALID_CAMPAIGN);
		}
		if (campaignId.startsWith(userId) == false) {
			throw new LoginException(SystemError.OPERATION_NOT_PERMITTED);
		}
	}

	public static void secureDropshipStore(String userId, String storeId) throws SQLException {
		secureSession(userId);
		if (StringUtils.isEmpty(storeId)) {
			throw new BadRequestException(SystemError.INVALID_DROPSHIP_STORE_ID);
		}
		Map dropshipStore = DropShipStoreService.lookUp(storeId);
		if (dropshipStore.isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_DROPSHIP_STORE_ID);
		}
		String userIdOfDropshipStore = ParamUtil.getString(dropshipStore, AppParams.USER_ID);
		if (userId.equals(userIdOfDropshipStore) == false) {
			throw new LoginException(SystemError.OPERATION_NOT_PERMITTED);
		}
	}

	public static void secureEmailCampaign(String userId, String emailCampaignId) throws SQLException {
		secureSession(userId);

		if (StringUtils.isEmpty(emailCampaignId)) {
			throw new BadRequestException(SystemError.INVALID_EMAIL_CAMP_ID);
		}

		Map emailCampaign = EmailCampaignsService.lookupEmailCampaign(userId, emailCampaignId);

		if (emailCampaign.isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_EMAIL_CAMP_ID);
		}

		String dbUserId = ParamUtil.getString(emailCampaign, AppParams.USER_ID);
		if (userId.equals(dbUserId) == false) {
			throw new LoginException(SystemError.OPERATION_NOT_PERMITTED);
		}

	}

	public static void secureMockup(String userId, String mockupId) throws SQLException {
		secureSession(userId);

		if (StringUtils.isEmpty(mockupId)) {
			throw new BadRequestException(SystemError.INVALID_MOCKUP_ID);
		}

		Map mockup = MockupService.get(mockupId);
		if (mockup.isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_MOCKUP_ID);
		}

		String campaignId = ParamUtil.getString(mockup, AppParams.CAMPAIGN_ID);

		if (StringUtils.isEmpty(campaignId) || !campaignId.startsWith(userId)) {
			throw new LoginException(SystemError.OPERATION_NOT_PERMITTED);
		}

	}

	public static void secureDomain(String userId, String domainId) throws SQLException {
		secureSession(userId);

		if (StringUtils.isEmpty(domainId)) {
			throw new BadRequestException(SystemError.INVALID_DOMAIN);
		}

		Map domainInfo = DomainService.lookup(domainId, userId);
		if (domainInfo.isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_DOMAIN);
		}

		String userIdOfDomain = ParamUtil.getString(domainInfo, AppParams.USER_ID);
		if (!userId.equals(userIdOfDomain)) {
			throw new LoginException(SystemError.OPERATION_NOT_PERMITTED);
		}

	}

	public static void securePromotion(String userId, String prId) throws SQLException {
		secureSession(userId);

		if (StringUtils.isEmpty(prId)) {
			throw new BadRequestException(SystemError.INVALID_PROMOTION);
		}

		Map prInfo = PromotionService.get(prId);
		if (prInfo.isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_PROMOTION);
		}

		String userIdOfprInfo = ParamUtil.getString(prInfo, AppParams.USER_ID);
		if (!userId.equals(userIdOfprInfo)) {
			throw new LoginException(SystemError.OPERATION_NOT_PERMITTED);
		}

	}
	
	public static void secureSubaccountAccessStore(RoutingContext routingContext , String storeId) {
		Boolean isOwner = routingContext.get(AppParams.OWNER);
		if (!isOwner) {
			List<String> listStore = ContextUtil.getListData(routingContext, AppParams.STORES);
			long count = listStore.stream().filter(e -> storeId.equalsIgnoreCase(e)).count();
			if (count <= 0) {
				throw new BadRequestException(SystemError.OPERATION_NOT_PERMITTED);
			}
		}
	}
	
	public static Map secureDropshipStoreV2(String userId, String storeId) throws SQLException {
		secureSession(userId);
		if (StringUtils.isEmpty(storeId)) {
			throw new BadRequestException(SystemError.INVALID_DROPSHIP_STORE_ID);
		}
		Map dropshipStore = DropShipStoreService.getStoreApprovedAndDisconnectedById(storeId);
		if (dropshipStore.isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_DROPSHIP_STORE_ID);
		}
		String userIdOfDropshipStore = ParamUtil.getString(dropshipStore, AppParams.USER_ID);
		if (userId.equals(userIdOfDropshipStore) == false) {
			throw new LoginException(SystemError.OPERATION_NOT_PERMITTED);
		}
		return dropshipStore;
	}
}
