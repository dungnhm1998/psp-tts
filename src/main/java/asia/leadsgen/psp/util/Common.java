package asia.leadsgen.psp.util;

//import com.app.http.Youtube;
//import com.app.services.DetectHandset;
//import com.app.services.LoggerInterface;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Common {

    public Common() {
    }

    public static final Pattern VALID_EMAIL_ADDRESS_REGEX =  Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    public static boolean validateEmail(String emailStr) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX .matcher(emailStr);
        return matcher.find();
}
    public static String hashPassword(String password) {
        String hashword = null;
        try {
//            MessageDigest md5 = MessageDigest.getInstance("MD5");
//            md5.update(password.getBytes());
//            BigInteger hash = new BigInteger(1, md5.digest());
//            hashword = hash.toString(32);
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(password.getBytes());
            byte messageDigest[] = m.digest();

            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xFF & messageDigest[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }
            hashword = hexString.toString();
        } catch (NoSuchAlgorithmException nsae) {
        }
        return hashword;
    }

    public static Date getSqlDate(java.util.Date date) {
        if (date == null) {
            return null;
        } else {
            return new Date(date.getTime());
        }
    }

    public static Timestamp getSqlTimestamp(java.util.Date date) {
        if (date == null) {
            return null;
        } else {
            return new Timestamp(date.getTime());
        }
    }

    public static java.util.Date getUtilDate(String sDate, String pattern) {
        SimpleDateFormat converttodt;
        java.util.Date date;
        try {
            if (sDate == null || sDate.equalsIgnoreCase("")) {
                return new java.util.Date();
            }
            converttodt = new SimpleDateFormat(pattern);
            date = (java.util.Date) converttodt.parse(sDate.trim());
        } catch (Exception e) {
            date = null;
        }

        return date;
    }

    public static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static long parseLong(String value, long defaultValue) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static double parseDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static java.util.Date addDaysToDate(java.util.Date date, int daysToAdd) {
        try {
            if (date == null) {
                date = new java.util.Date();
            }
            DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Calendar now = Calendar.getInstance();
            now.setTime(date);
            now.add(Calendar.DATE, daysToAdd);
            return sdf.parse(sdf.format(now.getTime()));
        } catch (Exception e) {
            return null;
        }
    }

    public static java.util.Date addMinuteToDate(java.util.Date date, int minute) {
        try {
            if (date == null) {
                date = new java.util.Date();
            }
            DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Calendar now = Calendar.getInstance();
            now.setTime(date);
            now.add(Calendar.MINUTE, minute);
            return sdf.parse(sdf.format(now.getTime()));
        } catch (Exception e) {
            return null;
        }
    }

    public static String getRandomString(int lenght) {
        String ret;
        Random r = new Random();
        String token = Long.toHexString(Math.abs(r.nextLong()));
        ret = token.substring(0, lenght);
        return ret;
    }
    
    public static String generateRandomString(Random random, int length){
        return random.ints(48,122)
                .filter(i-> (i<57 || i>65) && (i <90 || i>97))
                .mapToObj(i -> (char) i)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    public static String SubtractMonth(java.util.Date d, int month) {
        String ret;
        Calendar now = Calendar.getInstance();
        DateFormat sdf = new SimpleDateFormat("yyyyMM");
        now.setTime(d);
        now.add(2, -month);
        ret = sdf.format(now.getTime());
        return ret;
    }

    public static boolean isNummber(String strnum) {
        boolean ret = false;
        try {
            int tmp;
            tmp = parseInt(strnum, 0);
            if (tmp > 0) {
                ret = true;
            }
        } catch (Exception ex) {
            ret = false;
        }
        return ret;
    }

    public static String dateToString(java.util.Date date, Format formater, String defaultValue) {
        try {
            return formater.format(date);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static String integerToString(Integer value) {
        if (value != null) {
            return String.valueOf(value);
        } else {
            return null;
        }
    }

    public static String dateToString(java.util.Date date, String pattern, String defaultValue) {
        try {
            Format formater = new SimpleDateFormat(pattern);
            return formater.format(date);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static int countOfPage(int sum, int pageSize) {
        int ret;
        double temp;
        if (sum <= 0) {
            ret = 0;
        } else {
            temp = (double) sum / ((double) pageSize * 1.0D);
            ret = (int) Math.ceil(temp);
        }
        return ret;
    }

    public static String replaceUnuseSpace(String input) {
        if (input == null || input.equals("")) {
            return input;
        }
        String temp[] = input.trim().split("\\s");
        input = "";
        for (String temp1 : temp) {
            if (temp1 == null || temp1.equals("")) {
                continue;
            }
            if (!input.equals("")) {
                input = (new StringBuilder()).append(input).append(" ").toString();
            }
            input = (new StringBuilder()).append(input).append(temp1).toString();
        }

        return input;
    }

    public static int getRandomInt(int start, int end) {
        int ret;
        Random r = new Random();
        if ((ret = r.nextInt(end + 1)) < start);
        return ret;
    }

    public synchronized String unicodeEscape(String s) {
        if (s == null || s.equalsIgnoreCase("")) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >> 7 > 0) {
                sb.append("\\u");
                sb.append(hexChar[c >> 12 & 0xf]);
                sb.append(hexChar[c >> 8 & 0xf]);
                sb.append(hexChar[c >> 4 & 0xf]);
                sb.append(hexChar[c & 0xf]);
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    public static String getValue(String key) {
        Properties properties = loadConfig();
        return properties.getProperty(key);
    }

    private static Properties loadConfig() {
        Properties properties = new Properties();
        try {
            try (InputStream urlStream = Common.class.getClassLoader().getResourceAsStream("config.properties")) {
                properties.load(urlStream);
            }
        } catch (Exception e) {
        }
        return properties;
    }

    public static String replaceFolder(String folder) {
        if (folder == null || folder.length() == 0) {
            return folder;
        }
        String temp = "";
        for (int i = 0; i < folder.length(); i++) {
            if (folder.charAt(i) == '/') {
                temp += File.separator;
            } else {
                temp += folder.charAt(i);
            }
        }
        folder = temp.replaceAll("%5C", "/");
        folder = folder.replaceAll("%20", "");
        return folder;
    }

    public static String getMD5(String fileName) throws Exception {
        String original = fileName;
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(original.getBytes());
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(Integer.toHexString((int) (b & 0xff)));
        }

        return sb.toString();
    }

    public static String getTime(String dateTime) {
        if (dateTime == null) {
            return "";
        }
        try {
            boolean addSubFix = true;
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            StringBuilder sb = new StringBuilder();
            java.util.Date current = Calendar.getInstance().getTime();
            java.util.Date date = df.parse(dateTime);
            long diffInSeconds = (date.getTime() - current.getTime()) / 1000L;
            long sec = diffInSeconds < 60L ? diffInSeconds : diffInSeconds % 60L;
            long min = diffInSeconds / 60L < 60L ? diffInSeconds / 60L : diffInSeconds % 60L;
            long hrs = diffInSeconds / 3600L < 24L ? diffInSeconds / 3600L : (diffInSeconds / 3600L) % 24L;
            long days = diffInSeconds / 0x15180L < 30L ? diffInSeconds / 0x15180L : (diffInSeconds / 0x15180L) % 30L;
            long months = diffInSeconds / 0x278d00L < 12L ? diffInSeconds / 0x278d00L : (diffInSeconds / 0x278d00L) % 30L;
            long years = months / 12L;
            if (years > 0L) {
                if (months == 1L) {
                    sb.append(" 1 n\u0103m ");
                } else {
                    sb.append((new StringBuilder()).append(years).append(" n\u0103m ").toString());
                }
                if (months > 0L) {
                    if (months == 1L) {
                        sb.append(" 1 th\341ng ");
                    } else {
                        sb.append((new StringBuilder()).append(months).append(" th\341ng ").toString());
                    }
                    if (days > 0L) {
                        if (days == 1L) {
                            sb.append(" 1 ng\340y ");
                        } else {
                            sb.append((new StringBuilder()).append(days).append(" ng\340y ").toString());
                        }
                        if (hrs > 0L) {
                            if (hrs == 1L) {
                                sb.append(" 1 gi\uFFFD? ");
                                if (min > 0L) {
                                    if (min == 1L) {
                                        sb.append(" 1 ph\372t ");
                                    } else {
                                        sb.append((new StringBuilder()).append(min).append(" ph\372t ").toString());
                                    }
                                }
                            } else {
                                sb.append((new StringBuilder()).append(hrs).append(" gi\uFFFD? ").toString());
                                if (min > 0L) {
                                    if (min == 1L) {
                                        sb.append(" 1 ph\372t ");
                                    } else {
                                        sb.append((new StringBuilder()).append(min).append(" ph\372t ").toString());
                                    }
                                }
                            }
                        } else if (min > 0L) {
                            if (min == 1L) {
                                sb.append(" 1 ph\372t ");
                            } else {
                                sb.append((new StringBuilder()).append(min).append(" ph\372t ").toString());
                            }
                        }
                    }
                }
            } else if (months > 0L) {
                if (months == 1L) {
                    sb.append(" 1 th\341ng ");
                } else {
                    sb.append((new StringBuilder()).append(months).append(" th\341ng ").toString());
                }
                if (days > 0L) {
                    if (days == 1L) {
                        sb.append(" 1 ng\340y ");
                    } else {
                        sb.append((new StringBuilder()).append(days).append(" ng\340y ").toString());
                    }
                    if (hrs > 0L) {
                        if (hrs == 1L) {
                            sb.append(" 1 gi\uFFFD? ");
                            if (min > 0L) {
                                if (min == 1L) {
                                    sb.append(" 1 ph\372t ");
                                } else {
                                    sb.append((new StringBuilder()).append(min).append(" ph\372t ").toString());
                                }
                            }
                        } else {
                            sb.append((new StringBuilder()).append(hrs).append(" gi\uFFFD? ").toString());
                            if (min > 0L) {
                                if (min == 1L) {
                                    sb.append(" 1 ph\372t ");
                                } else {
                                    sb.append((new StringBuilder()).append(min).append(" ph\372t ").toString());
                                }
                            }
                        }
                    } else if (min > 0L) {
                        if (min == 1L) {
                            sb.append(" 1 ph\372t ");
                        } else {
                            sb.append((new StringBuilder()).append(min).append(" ph\372t ").toString());
                        }
                    }
                }
            } else if (days > 0L) {
                if (days == 1L) {
                    sb.append(" 1 ng\340y ");
                } else {
                    sb.append((new StringBuilder()).append(days).append(" ng\340y ").toString());
                }
                if (hrs > 0L) {
                    if (hrs == 1L) {
                        sb.append(" 1 gi\uFFFD? ");
                        if (min > 0L) {
                            if (min == 1L) {
                                sb.append(" 1 ph\372t ");
                            } else {
                                sb.append((new StringBuilder()).append(min).append(" ph\372t ").toString());
                            }
                        }
                    } else {
                        sb.append((new StringBuilder()).append(hrs).append(" gi\uFFFD? ").toString());
                        if (min > 0L) {
                            if (min == 1L) {
                                sb.append(" 1 ph\372t ");
                            } else {
                                sb.append((new StringBuilder()).append(min).append(" ph\372t ").toString());
                            }
                        }
                    }
                } else if (min > 0L) {
                    if (min == 1L) {
                        sb.append(" 1 ph\372t ");
                    } else {
                        sb.append((new StringBuilder()).append(min).append(" ph\372t ").toString());
                    }
                }
            } else if (hrs > 0L) {
                if (hrs == 1L) {
                    sb.append(" 1 gi\uFFFD? ");
                    if (min > 0L) {
                        if (min == 1L) {
                            sb.append(" 1 ph\372t ");
                        } else {
                            sb.append((new StringBuilder()).append(min).append(" ph\372t ").toString());
                        }
                    }
                } else {
                    sb.append((new StringBuilder()).append(hrs).append(" gi\uFFFD? ").toString());
                    if (min > 0L) {
                        if (min == 1L) {
                            sb.append(" 1 ph\372t ");
                        } else {
                            sb.append((new StringBuilder()).append(min).append(" ph\372t ").toString());
                        }
                    }
                }
            } else if (min > 0L) {
                if (min == 1L) {
                    sb.append(" 1 ph\372t ");
                } else {
                    sb.append((new StringBuilder()).append(min).append(" ph\372t ").toString());
                }
            } else if (sec <= 1L) {
                sb.append(" 1 gi\342y ");
            } else {
                sb.append((new StringBuilder()).append(sec).append(" gi\342y ").toString());
            }
            return (new StringBuilder()).append("C\362n ").append(sb.toString()).toString();
        } catch (Exception e) {
        }
        return "";
    }

    public ArrayList readText(File file) {
        ArrayList ret = new ArrayList();
        try {
            FileInputStream fstream = new FileInputStream(file);
            try (DataInputStream in = new DataInputStream(fstream)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String strLine;
                while ((strLine = br.readLine()) != null) {
                    ret.add(strLine);
                }
            }
        } catch (Exception e) {
            System.err.println((new StringBuilder()).append("Error: ").append(e.getMessage()).toString());
        }
        return ret;
    }

    public static String getDuration(String src) {
        String ret = src;
        int minute, second;
        try {
//            ret = src;
            if (src.indexOf(" ") > 0) {
                ret = src.substring(0, src.indexOf(" ")).trim();
                if (ret.length() == 1) {
                    ret = "0" + ret + ":00";
                } else {
                    ret += ":00";
                }
            } else {
                if (src.indexOf(":") <= 0) {
                    minute = Common.parseInt(src, 0) / 60;
                    second = Common.parseInt(src, 0) % 60;
                    if (minute < 10) {
                        ret = "0" + minute;
                    } else {
                        ret = "" + minute;
                    }
                    if (second < 10) {
                        ret += ":0" + second;
                    } else {
                        ret += ":" + second;
                    }
                }
            }
        } catch (Exception e) {
        }
        return ret;
    }

    public static String getKey(String source) {
        String ret = "";
        try {
            if (source == null) {
                source = "";
            }
            source = source.toLowerCase();
            source = source.replaceAll(" ", "").replaceAll("[*|.|?|/|,|+|(|)|'|%|=|<|>|;]", "");
            ret = source;
        } catch (Exception e) {
//            logger.error("", e);
        }
        return ret;
    }

    public static String processInfo(String info) {
        String ret = "";
        int t;
        for (int i = 0; i < info.length(); i++) {
            t = (int) info.charAt(i);
            if (((t >= 48) && (t <= 57)) || ((t >= 65) && (t <= 90)) || ((t >= 97) && (t <= 122)) || (t == 45) || (t == 32)) {
                ret += info.charAt(i);
            } else if ((t == 10) || (t == 13)) {
                break;
            }
        }
        return ret;
    }

    public static String getDurationString(int seconds) {

        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;
        if (hours <= 0) {
            return twoDigitString(minutes) + " : " + twoDigitString(seconds);
        } else {
            return twoDigitString(hours) + " : " + twoDigitString(minutes) + " : " + twoDigitString(seconds);
        }

    }

    private static String twoDigitString(int number) {

        if (number == 0) {
            return "00";
        }

        if (number / 10 == 0) {
            return "0" + number;
        }

        return String.valueOf(number);
    }

    public static String processInfoTag(String info) {
        String ret = "";
        int t;
        for (int i = 0; i < info.length(); i++) {
            t = (int) info.charAt(i);
            if (((t >= 48) && (t <= 57)) || ((t >= 65) && (t <= 90)) || ((t >= 97) && (t <= 122)) || (t == 45) || (t == 32) || (t == 93) || (t == 47)) {
                if (t == 93) {
                    ret += " ";
                } else {
                    ret += info.charAt(i);
                }
            } else if ((t == 10) || (t == 13)) {
                break;
            }
        }
//        ret = ret.trim();
        return ret;
    }
    
    public static String formatTimezone(String timezone) {
    	
    	if (timezone.endsWith(".25")) {
			timezone = timezone.replace(".25", ".5");
		}
		
		if (timezone.endsWith(".5")) {
			if (timezone.contains("+")) {
				if (timezone.substring((timezone.lastIndexOf("+") + 1), timezone.lastIndexOf(".")).length() < 2) {
					timezone = (timezone.replace("+", "+0")).replace(".5", ":30");
				} else {
					timezone = timezone.replace(".5", ":30");
				}
				
			} else if (timezone.contains("-")) {
				if (timezone.substring((timezone.lastIndexOf("-") + 1), timezone.lastIndexOf(".")).length() < 2) {
					timezone = (timezone.replace("-", "-0")).replace(".5", ":30");
				} else {
					timezone = timezone.replace(".5", ":30");
				}
			}
			
		} else if (timezone.endsWith(".75")) {
			if (timezone.contains("+")) {
				if (timezone.substring((timezone.lastIndexOf("+") + 1), timezone.lastIndexOf(".")).length() < 2) {
					timezone = (timezone.replace("+", "+0")).replace(".75", ":45");
				} else {
					timezone = timezone.replace(".75", ":45");
				}
				
			} else if (timezone.contains("-")) {
				if (timezone.substring((timezone.lastIndexOf("-") + 1), timezone.lastIndexOf(".")).length() < 2) {
					timezone = (timezone.replace("-", "-0")).replace(".75", ":45");
				} else {
					timezone = timezone.replace(".75", ":45");
				}
			}
		} else {
			if (timezone.contains("+")) {
				if (timezone.substring((timezone.lastIndexOf("+") + 1)).length() < 2) {
					timezone = (timezone.replace("+", "+0")).concat(":00");
				} else {
					timezone = timezone.concat(":00");
				}
				
			} else if (timezone.contains("-")) {
				if (timezone.substring((timezone.lastIndexOf("-") + 1)).length() < 2) {
					timezone = (timezone.replace("-", "-0")).concat(":00");
				} else {
					timezone = timezone.concat(":00");
				}
			}
		}
		
		return timezone;
	}

    public static void main(String a[]) throws NoSuchAlgorithmException {
//        System.out.println((new Common()).unicodeEscape("H\u1EBFt m\u1EA1ng r\u1ED3i, b\u1EA1n mu\u1ED1n th\352m m\u1EA1ng \u0111\u1EC3 ch\u01A1i ti\u1EBFp kh\364ng ???"));
//        System.out.println(hashPassword("ngat123456"));
//        System.out.println(getKey("http://gdata.youtube.com/feeds/api/videos?q=thonvl&format=5&max-results=18&v=2&alt=jsonc&filters=video&orderby=relevance&start-index=1"));
//        int r;
//        Random ran = new Random();
//        for(int i = 0; i <100; i++){
//            r = ran.nextInt(100);
//            System.out.println(r);
//        }
//        String s = initFolderName("Mozilla/5.0 (SymbianOS/9.3; Series60/3.2 NokiaE72-1/021.010; Profile/MIDP-2.1 Configuration/CLDC-1.1 ) AppleWebKit/525 (KHTML, like Gecko) Version/3.0 BrowserNG/7.1.14371");
//        System.out.println(s);
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTimeInMillis(1486829729168l);
//        System.out.println("date = " + calendar.getTime());
//        java.util.Date dd = getUtilDate("2017-01-18 06:50:48", "yyyy-MM-dd HH:mm:ss");
//        long timeExpire = dd.getTime() - new java.util.Date().getTime();
//        int date = (int) Math.ceil(timeExpire / (24 * 60 * 60 * 1000f));
//        System.out.println("dd = " + date);
        System.out.println("pass=" + hashPassword("hungdt122323adadadadad_adadada_adada_"));
//        MessageDigest m = MessageDigest.getInstance("MD5");
//        m.update("Hello".getBytes());
//        byte messageDigest[] = m.digest();
//
//        StringBuffer hexString = new StringBuffer();
//        for (int i = 0; i < messageDigest.length; i++) {
//            String hex = Integer.toHexString(0xFF & messageDigest[i]);
//            if (hex.length() == 1) {
//                hexString.append('0');
//            }
//
//            hexString.append(hex);
//        }
//
//        System.out.println("md5 = " + hexString.toString());

    }
    private static final char hexChar[] = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F'
    };
}
