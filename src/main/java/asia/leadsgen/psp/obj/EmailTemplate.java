package asia.leadsgen.psp.obj;

import java.util.Map;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;

public class EmailTemplate {

	private static final String MAIL_TEMPLATE = "mail_template_";
	private String subject;
	private String content;

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public EmailTemplate(String subject, String content) {
		super();
		this.subject = subject;
		this.content = content;
	}

	public static EmailTemplate fromMap(Map<String, Object> map) {
		String subject = ParamUtil.getString(map, AppParams.S_SUBJECT);
		String content = ParamUtil.getString(map, AppParams.C_CONTENT);
		return new EmailTemplate(subject, content);
	}

//	public static EmailTemplate init(String templateKey) throws SQLException {
//		EmailTemplate template = (EmailTemplate) RedisService.get(MAIL_TEMPLATE + templateKey);
//		if (template == null || template.getSubject() == null || template.getContent() == null) {
//			template = EmailTemplateService.get(templateKey);
//			RedisService.save6Hours(MAIL_TEMPLATE + templateKey, template);
//		}
//		return template;
//	}
}
