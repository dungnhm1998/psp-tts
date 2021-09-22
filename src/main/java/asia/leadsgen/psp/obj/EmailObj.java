/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package asia.leadsgen.psp.obj;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.Common;
import asia.leadsgen.psp.util.ParamUtil;
import io.vertx.core.json.JsonObject;

/**
 *
 * @author hungdt
 */
public class EmailObj {

	private String id;
	private String type;
	private String receiver;
	private String subject;
	private String content;
	private String state;
	private Date create;
	private Date update;
	private Date sent;
	private String sender;
	private String domain;
	private String failEvent;
	private String failReason;
	private String contentType;
	private String messageId;

	public EmailObj() {
	}

	public EmailObj(String type, String receiver, String subject, String content, String state, String domain,
			String contenttype) {
		this.type = type;
		this.receiver = receiver;
		this.subject = subject;
		this.content = content;
		this.state = state;
		this.domain = domain;
		this.contentType = contenttype;
		this.create = new Date();
	}

	public EmailObj(String type, String receiver, String subject, String content, String state, String domain,
			String contenttype, int sendAfter) {
		this.type = type;
		this.receiver = receiver;
		this.subject = subject;
		this.content = content;
		this.state = state;
		this.domain = domain;
		this.contentType = contenttype;
		if (sendAfter > 0) {
			Date dt = new Date();
			Calendar c = Calendar.getInstance();
			c.setTime(dt);
			c.add(Calendar.DATE, sendAfter);
			this.setCreate(c.getTime());
		}

	}

	public EmailObj(Map email) {
		
		if (StringUtils.isNotEmpty(ParamUtil.getString(email, AppParams.ID))) {
			this.id = ParamUtil.getString(email, AppParams.ID);
		}

		this.type = ParamUtil.getString(email, AppParams.TYPE);
		this.receiver = ParamUtil.getString(email, AppParams.RECEIVER);
		this.subject = ParamUtil.getString(email, AppParams.SUBJECT);
		this.content = ParamUtil.getString(email, AppParams.CONTENT);
		this.state = ParamUtil.getString(email, AppParams.STATE);
		this.domain = ParamUtil.getString(email, AppParams.DOMAIN);
		this.contentType = ParamUtil.getString(email, AppParams.CONTENT_TYPE);
		this.create = StringUtils.isEmpty(ParamUtil.getString(email, AppParams.CREATE_TIME))
				? Common.getUtilDate(ParamUtil.getString(email, AppParams.CREATE_TIME),
						AppConstants.DEFAULT_DATE_TIME_FORMAT_VALUE)
				: new Date();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getReceiver() {
		return receiver;
	}

	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}

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

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public Date getCreate() {
		return create;
	}

	public void setCreate(Date create) {
		this.create = create;
	}

	public Date getUpdate() {
		return update;
	}

	public void setUpdate(Date update) {
		this.update = update;
	}

	public Date getSent() {
		return sent;
	}

	public void setSent(Date sent) {
		this.sent = sent;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getFailEvent() {
		return failEvent;
	}

	public void setFailEvent(String failEvent) {
		this.failEvent = failEvent;
	}

	public String getFailReason() {
		return failReason;
	}

	public void setFailReason(String failReason) {
		this.failReason = failReason;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	@Override
	public String toString() {
		return "EmailObj [id=" + id + ", type=" + type + ", receiver=" + receiver + ", subject=" + subject
				+ ", content=" + content + ", state=" + state + ", create=" + create + ", update=" + update + ", sent="
				+ sent + ", sender=" + sender + ", domain=" + domain + ", failEvent=" + failEvent + ", failReason="
				+ failReason + ", contentType=" + contentType + ", messageId=" + messageId + "]";
	}

}
