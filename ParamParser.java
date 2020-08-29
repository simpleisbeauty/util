package org.simple.util;

import java.util.regex.Pattern;

/*
        String[] cmdFormats = {"get -n|--name regex:/.*?/.* [-f|--file-path] regex:.*?/.*",
                "put -n|--name regex:/.*?/.* -v|--value value [-o|--overwrite] regex:true|false|",
                 "put -n|--name regex:/.*?/.* -f|--file_path regex:.*?/.* [-o|--overwrite] regex:true|false|"};

        String err;
        try {
            ParamParser pas= new ParamParser(args);
            err = pas.validate(cmdFormats);
            if (err == null) {
                String name = pas.getParam("-n");

                String path = pas.getParam("-f");
                String val = pas.getParam("-v");

                boolean overwrite = pas.getParam("-o") == null ? false : true;
* */

public class ParamParser {

    public ParamParser(String[] args) {
        params = parse(args);
    }

    public String getParam(String name) {
        return params.get(name);
    }

    private List parse(String[] args) {

        List ret = new List(args.length);
        ret.add(OP, args[0]);
        int idx = 1;
        while (idx < args.length) {
            boolean optional = false;
            if (args[idx].startsWith("[") && args[idx].endsWith("]")) { // optional param
                optional = true;
                args[idx] = args[idx].substring(1, args[idx].length() - 1); // remove bracket
            }
            if (args[idx].startsWith("-")) {
                String n = args[idx];
                int i = n.indexOf("|");
                if (i != -1) { // param has alias
                    String p2 = n.substring(i + 1);
                    n = n.substring(0, i); // use first one
                    ret.add(ALIAS + p2, n);
                }
                idx++;
                if (ret.get(n) != null) {
                    throw new IllegalArgumentException("Duplicated param: " + n);
                }
                if (idx >= args.length || args[idx].startsWith("-")) {// no idx forward
                    ret.add(n, optional ? OPT : "");
                } else {
                    ret.add(n, optional ? OPT + args[idx] : args[idx]);
                    idx++; // next arg
                }
            } else {
                ret.add(args[idx++], "");// unknown param
            }
        }

        return ret;

    }

    public String validate(String[] cmdFormats) {

        String op = getParam(OP);
        if (op == null) {
            return "Error: first param must be operation";
        }
        StringBuilder unknown = new StringBuilder(256);
        for (String cmdFmt : cmdFormats) {

            String[] fmtArgs = cmdFmt.split("\\s+"); // 1 or more space
            List fmtList = parse(fmtArgs);

            if (!op.equals(fmtList.get(OP))) { // not this op
                continue; // next loop
            }

            boolean fmtMatch = true;
            // args must all in fmtList
            for (int i = 0; i < params.size; i++) {
                String argKey = params.objs[i].key;
                String argVal = params.objs[i].val;
                String fmtVal = fmtList.get(argKey);
                if (fmtVal != null) {
                    regexMatch(argVal, fmtVal);
                    fmtList.del(argKey); // mark found
                } else {
                    String alias = fmtList.get(ALIAS + argKey);
                    if (alias == null) {
                        fmtMatch = false;
                        unknown.append(argKey).append(':').append(argVal).append(';');
                        break;
                    } else { // find param alias in format
                        if (params.get(alias) != null) {
                            return "Error: duplicated argument: " + argKey;
                        }
                        regexMatch(argVal, fmtList.get(alias));
                        // always use one name, Map changed - ConcurrentModificationException
                        params.objs[i].key = alias; // rename
                        fmtList.del(alias); //mark found
                    }
                }

            }


            if (fmtMatch) {// validate this format
                //fmtList should all in params except optional param
                StringBuilder sb = new StringBuilder(256);
                for (int i = 0; i < fmtList.size; i++) {
                    if (fmtList.objs[i] != null) {
                        String key = fmtList.objs[i].key;
                        if (key != null && (!key.startsWith(ALIAS) &&
                                !fmtList.objs[i].val.startsWith(OPT))) {
                            sb.append(key).append(',');
                        }
                    }
                }
                if (sb.length() == 0) {
                    return null; // validated
                } else {
                    return "Error: missing argument: " + sb.toString();
                }
            }

        }
        return "Error: unknown argument: " + op + " " + unknown.toString();
    }

    private void regexMatch(String argVal, String fmt) {
        int idx = fmt.indexOf("regex:");
        if (idx != -1) {
            String pattern = fmt.substring(idx + 6);
            if (!Pattern.matches(pattern, argVal)) {
                throw new IllegalArgumentException(argVal + " doesn't match pattern :" + pattern);
            }
        }
    }

    private static class KeyVal {

        KeyVal(String k, String v) {
            key = k;
            val = v;
        }

        String key, val;
    }

    // avoid Create Generic Array error, and Map ConcurrentModificationException
    private static class List {
        List(int capacity) {
            objs = new KeyVal[capacity];
        }

        void del(String key) {
            for (int i = 0; i < size; i++) {
                if (objs[i] != null && objs[i].key.equals(key)) {
                    objs[i] = null;
                    break;
                }
            }
        }

        String get(String key) {
            for (int i = 0; i < size; i++) {
                if (objs[i] != null && objs[i].key.equals(key)) {
                    return objs[i].val;
                }
            }
            return null;
        }

        void add(String key, String val) {
            objs[size++] = new KeyVal(key, val);
        }

        private KeyVal[] objs;
        private int size = 0;
    }

    private List params;

    private final static String OP = "OP";
    private final static String ALIAS = "ALIAS_";
    private final static String OPT = "optional ";

}
