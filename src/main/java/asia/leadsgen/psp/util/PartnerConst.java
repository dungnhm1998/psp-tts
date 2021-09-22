package asia.leadsgen.psp.util;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.service_fulfill.BaseSKUService;
import asia.leadsgen.psp.service_fulfill.RedisService;

public class PartnerConst {

	public static final String CHAMPY = "qKWWXsDnKbBJgaIe";
	public static final String TSC = "feeiFVNPgjmnzB8I";
	public static final String CUSTOM_CAT = "dxh1J4bF7Z7zPoE7";
	public static final String JOY = "4TyUq7HsUdxSiqnt";
	public static final String SCALABLE_PRESS = "hXm5JGzWm0kG83iP";
	public static final String GEARMENT = "UvpbyhyuFEa1nFh5";
	public static final String NO_ONE = "_qpdUbRt-zbDAk2v";
	public static final String PRINTWAY = "GAUuVlTWY92ncMUB";

	public static final String CANVAS_CHAMBO = "G8t4oU7VnYDHIjRU";
	public static final String CANVAS_CHAMBO_EMAIL = "1831062475@qq.com";
	public static final String CANVAS_CHAMBO_NAME = "Canvas Chambo";

	public static final String CANVAS_CHAMP = "dlBC0fE5y9uM89UE";
	public static final String CANVAS_CHAMP_EMAIL = "cs@canvaschamp.com";
	public static final String CANVAS_CHAMP_NAME = "Canvas Champ";

	public static final String LEE_COW_LEATHER = "bFa4kEWa0OfdRphH";

	private static String champyBases;

	private static String tscBases;

	private static String canvasChamboBases;

	private static String canvasChampBases;

	private static String joyBases;

	private static String scalablepressMugBases;

	private static String scalablepressPosterBases;
	
	private static String printwayBases;

	private static String leecowleatherBases;
	
	private static String scalablepressBases;

	public static void initPartnerConst() throws SQLException {
		PartnerConst.setCanvasChamboBases();
		PartnerConst.setCanvasChampBases();
		PartnerConst.setChampyBases();
		PartnerConst.setJoyBases();
		PartnerConst.setLeecowleatherBases();
		PartnerConst.setPrintwayBases();
		PartnerConst.setScalablepressMugBases();
		PartnerConst.setScalablepressPosterBases();
		PartnerConst.setTscBases();
	}
	
	public static void setScalablepressBases() throws SQLException {
		String temp = "";
		String key = "list_base_by_partner_id_" + PartnerConst.SCALABLE_PRESS;
		if (RedisService.get(key) != null) {
			if (RedisService.get(key).get("list_base") != null)
				temp = RedisService.get(key).get("list_base").toString();
		} else {
			Map listBase = new HashedMap<>();
			List<Map> listBaseByPartnerId = BaseSKUService.getListBaseByPartnerId(PartnerConst.SCALABLE_PRESS);

			for (Map baseId : listBaseByPartnerId) {
				if (baseId.get("S_BASE_ID") != null && !temp.contains(baseId.get("S_BASE_ID").toString()))
					temp += baseId.get("S_BASE_ID").toString() + "|";
			}
			listBase.put("list_base", temp);
			RedisService.save(key, listBase);
		}
		PartnerConst.scalablepressBases = temp;
	}
	
	public static String getScalablepressBases() {
		if (StringUtils.isEmpty(scalablepressBases)) {
			try {
				PartnerConst.setScalablepressBases();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return scalablepressBases;
	}

	public static void setChampyBases() throws SQLException {
		String temp = "";
		String key = "list_base_by_partner_id_" + PartnerConst.CHAMPY;
		if (RedisService.get(key) != null) {
			if (RedisService.get(key).get("list_base") != null)
				temp = RedisService.get(key).get("list_base").toString();
		} else {
			Map listBase = new HashedMap<>();
			List<Map> listBaseByPartnerId = BaseSKUService.getListBaseByPartnerId(PartnerConst.CHAMPY);

			for (Map baseId : listBaseByPartnerId) {
				if (baseId.get("S_BASE_ID") != null && !temp.contains(baseId.get("S_BASE_ID").toString()))
					temp += baseId.get("S_BASE_ID").toString() + "|";
			}
			listBase.put("list_base", temp);
			RedisService.save(key, listBase);
		}
		PartnerConst.champyBases = temp;
	}

	public static void setTscBases() throws SQLException {
		String temp = "";
		String key = "list_base_by_partner_id_" + PartnerConst.TSC;
		if (RedisService.get(key) != null) {
			if (RedisService.get(key).get("list_base") != null)
				temp = RedisService.get(key).get("list_base").toString();
		} else {
			Map listBase = new HashedMap<>();
			List<Map> listBaseByPartnerId = BaseSKUService.getListBaseByPartnerId(PartnerConst.TSC);
			for (Map baseId : listBaseByPartnerId) {
				if (baseId.get("S_BASE_ID") != null && !temp.contains(baseId.get("S_BASE_ID").toString()))
					temp += baseId.get("S_BASE_ID").toString() + "|";
			}
			listBase.put("list_base", temp);
			RedisService.save(key, listBase);
		}
		PartnerConst.tscBases = temp;
	}

	public static void setCanvasChamboBases() throws SQLException {
		String temp = "";
		String key = "list_base_by_partner_id_" + PartnerConst.CANVAS_CHAMBO;
		if (RedisService.get(key) != null) {
			if (RedisService.get(key).get("list_base") != null)
				temp = RedisService.get(key).get("list_base").toString();
		} else {
			Map listBase = new HashedMap<>();
			List<Map> listBaseByPartnerId = BaseSKUService.getListBaseByPartnerId(PartnerConst.CANVAS_CHAMBO);
			for (Map baseId : listBaseByPartnerId) {
				if (baseId.get("S_BASE_ID") != null && !temp.contains(baseId.get("S_BASE_ID").toString()))
					temp += baseId.get("S_BASE_ID").toString() + "|";
			}
			listBase.put("list_base", temp);
			RedisService.save(key, listBase);
		}
		PartnerConst.canvasChamboBases = temp;
	}

	public static void setCanvasChampBases() throws SQLException {
		String temp = "";
		String key = "list_base_by_partner_id_" + PartnerConst.CANVAS_CHAMP;
		if (RedisService.get(key) != null) {
			if (RedisService.get(key).get("list_base") != null)
				temp = RedisService.get(key).get("list_base").toString();
		} else {
			Map listBase = new HashedMap<>();
			List<Map> listBaseByPartnerId = BaseSKUService.getListBaseByPartnerId(PartnerConst.CANVAS_CHAMP);
			for (Map baseId : listBaseByPartnerId) {
				if (baseId.get("S_BASE_ID") != null && !temp.contains(baseId.get("S_BASE_ID").toString()))
					temp += baseId.get("S_BASE_ID").toString() + "|";
			}
			listBase.put("list_base", temp);
			RedisService.save(key, listBase);
		}
		PartnerConst.canvasChampBases = temp;
	}

	public static void setJoyBases() throws SQLException {
		String temp = "";
		String key = "list_base_by_partner_id_" + PartnerConst.JOY;
		if (RedisService.get(key) != null) {
			if (RedisService.get(key).get("list_base") != null)
				temp = RedisService.get(key).get("list_base").toString();
		} else {
			Map listBase = new HashedMap<>();
			List<Map> listBaseByPartnerId = BaseSKUService.getListBaseByPartnerId(PartnerConst.JOY);
			for (Map baseId : listBaseByPartnerId) {
				if (baseId.get("S_BASE_ID") != null && !temp.contains(baseId.get("S_BASE_ID").toString()))
					temp += baseId.get("S_BASE_ID").toString() + "|";
			}
			listBase.put("list_base", temp);
			RedisService.save(key, listBase);
		}
		PartnerConst.joyBases = temp;
	}

	public static void setScalablepressMugBases() throws SQLException {
		String temp = "";
		String key = "list_base_by_partner_id_" + PartnerConst.SCALABLE_PRESS + "_mugs";
		if (RedisService.get(key) != null) {
			if (RedisService.get(key).get("list_base") != null)
				temp = RedisService.get(key).get("list_base").toString();
		} else {
			Map listBase = new HashedMap<>();
			List<Map> listBaseByPartnerId = BaseSKUService.getListBaseByPartnerId(PartnerConst.SCALABLE_PRESS);
			for (Map baseId : listBaseByPartnerId) {
				if (baseId.get("S_BASE_NAME").toString().toLowerCase().contains("mug")) {
					if (baseId.get("S_BASE_ID") != null && !temp.contains(baseId.get("S_BASE_ID").toString()))
						temp += baseId.get("S_BASE_ID").toString() + "|";
				}
			}
			listBase.put("list_base", temp);
			RedisService.save(key, listBase);
		}
		PartnerConst.scalablepressMugBases = temp;
	}

	public static void setScalablepressPosterBases() throws SQLException {
		String temp = "";
		String key = "list_base_by_partner_id_" + PartnerConst.SCALABLE_PRESS + "_poster";
		if (RedisService.get(key) != null) {
			if (RedisService.get(key).get("list_base") != null)
				temp = RedisService.get(key).get("list_base").toString();
		} else {
			Map listBase = new HashedMap<>();
			List<Map> listBaseByPartnerId = BaseSKUService.getListBaseByPartnerId(PartnerConst.SCALABLE_PRESS);
			for (Map baseId : listBaseByPartnerId) {
				if (baseId.get("S_BASE_NAME").toString().toLowerCase().contains("poster")) {
					if (baseId.get("S_BASE_ID") != null && !temp.contains(baseId.get("S_BASE_ID").toString()))
						temp += baseId.get("S_BASE_ID").toString() + "|";
				}
			}
			listBase.put("list_base", temp);
			RedisService.save(key, listBase);
		}
		PartnerConst.scalablepressPosterBases = temp;
	}

	public static void setPrintwayBases() throws SQLException {
		String temp = "";
		String key = "list_base_by_partner_id_" + PartnerConst.PRINTWAY;
		if (RedisService.get(key) != null) {
			if (RedisService.get(key).get("list_base") != null)
				temp = RedisService.get(key).get("list_base").toString();
		} else {
			Map listBase = new HashedMap<>();
			List<Map> listBaseByPartnerId = BaseSKUService.getListBaseByPartnerId(PartnerConst.PRINTWAY);
			for (Map baseId : listBaseByPartnerId) {
				if (baseId.get("S_BASE_ID") != null && !temp.contains(baseId.get("S_BASE_ID").toString()))
					temp += baseId.get("S_BASE_ID").toString() + "|";
			}
			listBase.put("list_base", temp);
			RedisService.save(key, listBase);
		}
		PartnerConst.printwayBases = temp;
	}

	public static void setLeecowleatherBases() throws SQLException {
		String temp = "";
		String key = "list_base_by_partner_id_" + PartnerConst.LEE_COW_LEATHER;
		if (RedisService.get(key) != null) {
			if (RedisService.get(key).get("list_base") != null)
				temp = RedisService.get(key).get("list_base").toString();
		} else {
			Map listBase = new HashedMap<>();
			List<Map> listBaseByPartnerId = BaseSKUService.getListBaseByPartnerId(PartnerConst.LEE_COW_LEATHER);
			for (Map baseId : listBaseByPartnerId) {
				if (baseId.get("S_BASE_ID") != null && !temp.contains(baseId.get("S_BASE_ID").toString()))
					temp += baseId.get("S_BASE_ID").toString() + "|";
			}
			listBase.put("list_base", temp);
			RedisService.save(key, listBase);
		}
		PartnerConst.leecowleatherBases = temp;
	}

	public static String getChampyBases() {
		if (StringUtils.isEmpty(champyBases)) {
			try {
				PartnerConst.setChampyBases();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return champyBases;
	}

	public static String getTscBases() {
		if (StringUtils.isEmpty(tscBases)) {
			try {
				PartnerConst.setTscBases();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return tscBases;
	}

	public static String getCanvasChamboBases() {
		if (StringUtils.isEmpty(canvasChamboBases)) {
			try {
				PartnerConst.setCanvasChamboBases();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return canvasChamboBases;
	}

	public static String getCanvasChampBases() {
		if (StringUtils.isEmpty(canvasChampBases)) {
			try {
				PartnerConst.setCanvasChampBases();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return canvasChampBases;
	}

	public static String getJoyBases() {
		if (StringUtils.isEmpty(joyBases)) {
			try {
				PartnerConst.setJoyBases();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return joyBases;
	}

	public static String getScalablepressMugBases() {
		if (StringUtils.isEmpty(scalablepressMugBases)) {
			try {
				PartnerConst.setScalablepressMugBases();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return scalablepressMugBases;
	}

	public static String getScalablepressPosterBases() {
		if (StringUtils.isEmpty(scalablepressPosterBases)) {
			try {
				PartnerConst.setScalablepressPosterBases();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return scalablepressPosterBases;
	}

	public static String getPrintwayBases() {
		if (StringUtils.isEmpty(printwayBases)) {
			try {
				PartnerConst.setPrintwayBases();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return printwayBases;
	}

	public static String getLeecowleatherBases() {
		if (StringUtils.isEmpty(leecowleatherBases)) {
			try {
				PartnerConst.setLeecowleatherBases();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return leecowleatherBases;
	}

}
