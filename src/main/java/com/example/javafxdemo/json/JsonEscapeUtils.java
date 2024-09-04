package com.example.javafxdemo.json;

public class JsonEscapeUtils {

    private static final char[] unescapeChars = new char[128];
    private static final char[] escapeChars = new char[128];

    static {
        escapeChars['"'] = '\"';
        escapeChars['\\'] = '\\';
        escapeChars['/'] = '/';
        escapeChars['b'] = '\b';
        escapeChars['f'] = '\f';
        escapeChars['n'] = '\n';
        escapeChars['r'] = '\r';
        escapeChars['t'] = '\t';

        unescapeChars['\"'] = '"';
        unescapeChars['\\'] = '\\';
        unescapeChars['/'] = '/';
        unescapeChars['\b'] = 'b';
        unescapeChars['\f'] = 'f';
        unescapeChars['\n'] = 'n';
        unescapeChars['\r'] = 'r';
        unescapeChars['\t'] = 't';

    }
    /**
     * 序列化 转义字符串
     */
    public static String escapeForSerialize(String origin){
        if(origin == null){ return null; }
        StringBuilder sb = new StringBuilder();
        for(int i = 0, len = origin.length(); i < len; i++){
            char ch = origin.charAt(i);
            if(ch < unescapeChars.length && unescapeChars[ch] != 0){
                sb.append('\\').append(unescapeChars[ch]);
            }else{
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * 反序列化 转义
     */
    public static String escapeForDeserialize(String origin){
        if(origin == null){ return null; }
        StringBuilder sb = new StringBuilder();
        for(int i = 0, len = origin.length(); i < len; i++){
            char ch = origin.charAt(i);
            if(ch == '\\'  && i + 1 < len){
                char c = origin.charAt(i + 1);
                if(c < escapeChars.length && escapeChars[c] != 0){
                    sb.append(escapeChars[c]);
                    i++;
                    continue;
                }
            }
            sb.append(ch);
        }
        return sb.toString();
    }

}
