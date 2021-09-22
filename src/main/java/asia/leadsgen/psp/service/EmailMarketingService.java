package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import asia.leadsgen.psp.obj.EmailObj;

/**
 * Created by hungdx on 4/1/17.
 */
public class EmailMarketingService {

	private static MongoTemplate mongoTemplate;

	private final static String EMAIL_MARKETING = "emailMarketing";

	public static void setMongoTemplate(MongoTemplate mongoTemplate) {
		EmailMarketingService.mongoTemplate = mongoTemplate;
	}

	public static EmailObj insert(EmailObj emailObj) {
//		LOGGER.log(Level.INFO, "insert email: {0}|{1}|{2}|{3}|{4}", new Object[] { emailObj.getType(),
//				emailObj.getReceiver(), emailObj.getSubject(), emailObj.getContent(), emailObj.getState() });
		mongoTemplate.save(emailObj, EMAIL_MARKETING);
		return emailObj;
	}

	public static void updateSentFailResons(String messageId, String event, String reason) throws SQLException {
		EmailObj obj = findByMessageId(messageId);
		if (obj != null) {
			obj.setFailEvent(event);
			obj.setFailReason(reason);
			obj.setUpdate(new Date());
			mongoTemplate.save(obj, EMAIL_MARKETING);
		}
	}

	public static EmailObj findByMessageId(String messageId) {
		Query query = new Query();
		query.addCriteria(Criteria.where("messageId").is(messageId));
		List<EmailObj> emails = mongoTemplate.find(query, EmailObj.class, EMAIL_MARKETING);
		EmailObj emailObj = null;
		if (CollectionUtils.isNotEmpty(emails)) {
			emailObj = emails.get(0);
		}
		return emailObj;
	}

	private static final Logger LOGGER = Logger.getLogger(EmailMarketingService.class.getName());
}
