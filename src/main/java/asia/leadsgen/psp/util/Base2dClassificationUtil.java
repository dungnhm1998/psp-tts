package asia.leadsgen.psp.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Base2dClassificationUtil {

    public static List<String> shirts = new ArrayList<>();
    public static List<String> mugs = new ArrayList<>();
    public static List<String> poster = new ArrayList<>();
    public static List<String> phonecases = new ArrayList<>();
    public static List<String> canvas = new ArrayList<>();
    public static List<String> normal3d = new ArrayList<>();


    public static void init(){
        shirts.add("8d_QKhXD-86igKPP");//v-neck
        shirts.add("KfXL7aOF8wUOJZyH");//unisex-hoodies
        shirts.add("9BESdMiawozq3yyX");//womentank
        shirts.add("aneISamhDEoT39oy");//kid hoodie
        shirts.add("-cwc847TMWTaZmVg");//popular-tee
        shirts.add("DpyfFZQ04G-jN_pL");//tank-top
        shirts.add("fyO46EELynO72L0V");//unisex zip hoodie
        shirts.add("7wTKZtthhjDW71oA");//unisex crewneck sweatshirt
        shirts.add("tL9HoLtLPWpM1Mz4");//women t-shirt
        shirts.add("WS-CFAJxOx1edtrB");//kid tee
        shirts.add("---2BfBws3P6wKDO");//men's long sleeve
        shirts.add("Nhy-R_esvYxrhdCX");//premium-tee

        mugs.add("j1e2h3Wq4Gz5ksjs");//Ceramic Mugs - Normal
        mugs.add("LCV9OQEvNZqeuX3D");//Ceramic Mugs - Color changing

        poster.add("i8ja02GzNr57tTwZ");//36x24 Poster
        poster.add("WWQ7xQ2aYSa0XUEC");//17x11 Poster
        poster.add("EOCuz3dpn8DqSVPB");//24x16 Poster
        poster.add("NIUJTUQ0BwZ473QF");//11x17 Poster
        poster.add("wg1fgLbO77j2WKky");//16x24 Poster
        poster.add("hUeboM3JbQW9lXRK");//24x36 Poster

        String basePhoneCases = BasePhoneCaseUtil.getBasePhoneCaseIds();
        phonecases = new ArrayList<>(Arrays.asList(basePhoneCases.split(",")));

        canvas.add("1XzIeZzaWu65jf92");//Metal Square
        canvas.add("oxrtWFG9QF0FWVyE");//Metal Landscape
        canvas.add("lAXkEDpRhWu3_ulu");//Metal Portrait
        canvas.add("NsNoxYr6XBXcIYJQ");//Wood Landscape
        canvas.add("hnqJaRc12vOV5H-N");//Wood Portrait
        canvas.add("a-x80z12BQyuq1E3");//Cotton 3 Pieces Portrait
        canvas.add("0-BOpogayL1X3FCt");//Cotton 1 Piece
        canvas.add("2iOH2h0TgphfF48v");//Cotton 3 Pieces - Unframed
        canvas.add("ZV6YA9SorkvO1eof");//Cotton 1 Piece - Unframed
        canvas.add("AML2RNWTivebyev1");//Cotton 3 Pieces
        canvas.add("L0baqM1xW12XIaxj");//Cotton 1 Piece - Portrait
        canvas.add("Q32VnDgI8ghohFaY");//Cotton 4 Pieces
        canvas.add("zDqncXhoPPasNQPE");//Cotton 5 Pieces
        canvas.add("TYr3JbE7xrqWSI_8");//Cotton 5 Pieces - Unframed
        canvas.add("hOpvQ23725fpkzIP");//Cotton 4 Pieces - Unframed

        normal3d.add("KRfcLkcYItB4MQmG");
        normal3d.add("N9xCAMpNLanQnP2q");
        normal3d.add("GAFofttyq18A5l2u");
        normal3d.add("zUCKpLPrl7xnsQU5");
        normal3d.add("Bj0pbciRSov9PhE4");
        normal3d.add("xtcHqBfLVsVt6YT8");
        normal3d.add("mrnEth0Nvn3011nn");
        normal3d.add("7ojeKe0NVNlUR2kp");
        normal3d.add("xaLgxhHX9KFQfLNY");
        normal3d.add("hncITeOCNanKXcYp");
        normal3d.add("QOYmaDsgdE7nNy21");
        normal3d.add("aIEqbcaqYKa0u8mr");
        normal3d.add("lWoVznlz828x8wvd");

    }

    public static String classify(String baseId) {
        init();
        if (shirts.contains(baseId)) return "shirt";
        if (mugs.contains(baseId)) return "mugs";
        if (poster.contains(baseId)) return "poster";
        if (phonecases.contains(baseId)) return "phonecase";
        if (canvas.contains(baseId)) return "canvas";
        if (normal3d.contains(baseId)) return "normal3d";
        return null;
    }
}
