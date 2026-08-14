package com.hexagonal.portfolio.domain.service;

import java.util.Locale;
import java.util.Map;

/**
 * Formatting rules carried over verbatim from the legacy {@code MyMFBoxUtils}.
 * Only the members reachable from the investor-portfolio use case are retained.
 */
public class MyMFBoxUtils
{

    public static String formatInRupees(long amount) {
        String num = String.valueOf(amount);
        int len = num.length();

        if (len <= 3) return num;

        String last3 = num.substring(len - 3);
        String remaining = num.substring(0, len - 3);

        StringBuilder sb = new StringBuilder();

        while (remaining.length() > 2) {
            sb.insert(0, "," + remaining.substring(remaining.length() - 2));
            remaining = remaining.substring(0, remaining.length() - 2);
        }

        sb.insert(0, remaining);

        return sb + "," + last3;
    }

    public static String checkParem(String param)
    {
        if (param == null || param.trim().equalsIgnoreCase("null") || param.trim().equalsIgnoreCase("undefined"))
        {
            return "";
        }

        return param.trim();
    }

    public static String getLogoByAmcNameOrSchemeName(String amc_or_scheme_name)
    {
        amc_or_scheme_name = checkParem(amc_or_scheme_name);
        // Was org.hibernate.internal.util.StringHelper.isEmpty, which is exactly this test.
        // Inlined to keep the domain free of ORM types; see HexagonalArchitectureTest.
        if (amc_or_scheme_name == null || amc_or_scheme_name.isEmpty()) return "empty.png";

        String firstWord = amc_or_scheme_name.trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
        Map<String, String> LOGO_MAP = Map.ofEntries(
                Map.entry("axis", "axis.png"),
                Map.entry("bandhan", "bandhan.png"),
                Map.entry("baroda", "bnp.png"),
                Map.entry("aditya", "birla.png"),
                Map.entry("absl", "birla.png"),
                Map.entry("bnp", "bnp.png"),
                Map.entry("boi", "boi.png"),
                Map.entry("bank", "boi.png"),
                Map.entry("canara", "canara.png"),
                Map.entry("dsp", "dsp.png"),
                Map.entry("franklin", "franklin.png"),
                Map.entry("templeton", "franklin.png"),
                Map.entry("hdfc", "hdfc.png"),
                Map.entry("icici", "icici.png"),
                Map.entry("idbi", "idbi.png"),
                Map.entry("jm", "jm.png"),
                Map.entry("kotak", "kotak.png"),
                Map.entry("lic", "lic.png"),
                Map.entry("principal", "principal.png"),
                Map.entry("nippon", "nippon.png"),
                Map.entry("cpse", "nippon.png"),
                Map.entry("sbi", "sbi.png"),
                Map.entry("sundaram", "sundaram.png"),
                Map.entry("tata", "tata.png"),
                Map.entry("uti", "uti.png"),
                Map.entry("pgim", "pgim.png"),
                Map.entry("edelweiss", "edelweiss.png"),
                Map.entry("bharat", "edelweiss.png"),
                Map.entry("hsbc", "hsbc.png"),
                Map.entry("invesco", "invesco.png"),
                Map.entry("l&t", "lt.png"),
                Map.entry("lt", "lt.png"),
                Map.entry("mahindra", "mahindra.png"),
                Map.entry("mirae", "mirae.png"),
                Map.entry("motilal", "motilal.png"),
                Map.entry("essel", "essel.png"),
                Map.entry("navi", "navi.png"),
                Map.entry("quantum", "quantum.png"),
                Map.entry("quant", "quant.png"),
                Map.entry("taurus", "taurus.png"),
                Map.entry("union", "union.png"),
                Map.entry("360", "360_one.png"),
                Map.entry("ppfas", "ppfas.png"),
                Map.entry("parag", "ppfas.png"),
                Map.entry("shriram", "shriram.png"),
                Map.entry("yes", "yes.png"),
                Map.entry("iti", "iti.png"),
                Map.entry("nj", "nj.png"),
                Map.entry("whiteoak", "whiteoak.png"),
                Map.entry("woc", "whiteoak.png"),
                Map.entry("samco", "samco.png"),
                Map.entry("helios", "helios.png"),
                Map.entry("angel", "angelone.png"),
                Map.entry("old", "old-bridge.png"),
                Map.entry("bajaj", "bajaj.png"),
                Map.entry("groww", "groww.png"),
                Map.entry("zerodha", "zerodha.png"),
                Map.entry("unifi", "unifi.png"),
                Map.entry("trust", "trust.png"),
                Map.entry("altiva", "altiva.png"),
                Map.entry("magnum", "magnum.png"),
                Map.entry("the", "wealth.png"),
                Map.entry("abakkus","abacus.png")
        );

        return LOGO_MAP.getOrDefault(firstWord, "empty.png");
    }
}
