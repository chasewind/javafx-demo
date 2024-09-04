package com.example.javafxdemo.json;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class JsonLexicalAnalyzer {

    private JsonLexicalAnalyzer() {
        // do nothing
    }

    public static List<JsonToken> lexicalAnalysis(String json, DeserializerConfiguration configuration){
        boolean abortWhenIncorrect = configuration.isAbortWhenIncorrect();
        boolean escape = configuration.isEscape();
        LinkedList<JsonToken> tokens = new LinkedList<>();
        StringBuilder illegalStr = new StringBuilder();
        boolean waitValue = false;
        Stack<Integer> objOrArr = new Stack<>(); // 0 - obj, 1 - array
        String lastNormalLiteral = "";    // 上一个正确的非blank token
        JsonToken.Type preNormalType = null;
        boolean finish = false;
        int i = 0;
        for(int len = json.length(); i < len; i++){
            char ch = json.charAt(i);
            JsonToken t = null;
            switch (ch){
                case '{':
                    if((!finish && objOrArr.isEmpty()) || waitValue){
                        t = new JsonToken(i, "{", JsonToken.Type.STRUCTURE);
                    }else{
                        t = new JsonToken(i, "{", JsonToken.Type.STRUCTURE, true);
                    }
                    objOrArr.push(0);
                    break;
                case '}':
                    if(stackPeek(objOrArr) == 0 && !waitValue && preNormalType != JsonToken.Type.KEY && !",".equals(lastNormalLiteral)){
                        t = new JsonToken(i, "}", JsonToken.Type.STRUCTURE);
                        objOrArr.pop();
                    }else{
                        t = new JsonToken(i, "}", JsonToken.Type.STRUCTURE, true);
                    }
                    break;
                case ':':
                    if(stackPeek(objOrArr) == 0 && preNormalType == JsonToken.Type.KEY){
                        t = new JsonToken(i, ":", JsonToken.Type.STRUCTURE);
                    }else{
                        t = new JsonToken(i, ":", JsonToken.Type.STRUCTURE, true);
                    }
                    break;
                case '[':
                    if(waitValue || (!finish && objOrArr.isEmpty())){
                        t = new JsonToken(i, "[", JsonToken.Type.STRUCTURE);
                    }else{
                        t = new JsonToken(i, "[", JsonToken.Type.STRUCTURE, true);
                    }
                    objOrArr.push(1);
                    break;
                case ']':
                    if(stackPeek(objOrArr) == 1){
                        if(!",".equals(lastNormalLiteral)){
                            t = new JsonToken(i, "]", JsonToken.Type.STRUCTURE);
                        }
                        objOrArr.pop();
                    }
                    if(t == null){
                        t = new JsonToken(i, "]", JsonToken.Type.STRUCTURE, true);
                    }
                    break;
                case ',':
                    if(!waitValue){
                        t = new JsonToken(i, ",", JsonToken.Type.STRUCTURE);
                    }else{
                        t = new JsonToken(i, ",", JsonToken.Type.STRUCTURE, true);
                    }
                    break;
                case 'n':
                    String subStr = trySubString(json, i, 4);
                    if("null".equals(subStr)){
                        if(waitValue){
                            t = new JsonToken(i, "null", JsonToken.Type.NULL);
                        }else{
                            t = new JsonToken(i, "null", JsonToken.Type.NULL, true);
                        }
                        i += 3;
                    }
                    break;
                case 't':
                    subStr = trySubString(json, i, 4);
                    if("true".equals(subStr)){
                        if(waitValue){
                            t = new JsonToken(i, "true", JsonToken.Type.BOOL);
                        }else{
                            t = new JsonToken(i, "true", JsonToken.Type.BOOL, true);
                        }
                        i += 3;
                    }
                    break;
                case 'f':
                    subStr = trySubString(json, i, 5);
                    if("false".equals(subStr)){
                        if(waitValue){
                            t = new JsonToken(i, "false", JsonToken.Type.BOOL);
                        }else{
                            t = new JsonToken(i, "false", JsonToken.Type.BOOL, true);
                        }
                        i += 4;
                    }
                    break;
                case '"':
                    RefObj<Boolean> check = new RefObj<>(false);
                    subStr = matchString(json, i, check);
                    String valStr = subStr;
                    if(check.value){
                        valStr = valStr.substring(1, valStr.length() - 1);
                    }
                    if(escape){
                        valStr = "\"" + JsonEscapeUtils.escapeForDeserialize(valStr) + "\"";
                    }else{
                        valStr = subStr;
                    }
                    if(waitValue){
                        t = new JsonToken(i, valStr, JsonToken.Type.STR_VALUE, !check.value);
                    }else {
                        if("{".equals(lastNormalLiteral) || ",".equals(lastNormalLiteral)){
                            t = new JsonToken(i, valStr, JsonToken.Type.KEY, !check.value);
                        }else{
                            t = new JsonToken(i, valStr, JsonToken.Type.KEY, true);
                        }
                    }
                    i += subStr.length() - 1;
                    break;
                default:
                    if(isWhitespace(ch)){
                        String s = matchWhitespace(json, i);
                        t = new JsonToken(i, s, JsonToken.Type.BLANK);
                        i += s.length() - 1;
                    }else{
                        String number = matchNumber(json, i);
                        if(number != null){
                            if(waitValue){
                                t = new JsonToken(i, number, JsonToken.Type.NUMBER);
                            }else{
                                t = new JsonToken(i, number, JsonToken.Type.NUMBER, true);
                            }
                            i += number.length() - 1;
                        }
                    }
                    break;
            }
            if(ch == '[' || (ch == ':' && stackPeek(objOrArr) == 0) || (ch == ','&& stackPeek(objOrArr) == 1)){
                waitValue = true;
            }else if(!isWhitespace(ch) && t != null){
                waitValue = false;
            }
            if(t == null){
                illegalStr.append(ch);
            }else {
                if(illegalStr.length() > 0){
                    tokens.add(new JsonToken(i - illegalStr.length(), illegalStr.toString(), JsonToken.Type.UNKNOWN));
                    illegalStr = new StringBuilder();
                    if(abortWhenIncorrect){ break; }
                }
                if(!t.isError() && t.getType() != JsonToken.Type.BLANK){
                    lastNormalLiteral = t.getLiteral();
                    preNormalType = t.getType();
                    if(objOrArr.isEmpty()){
                        finish = true;
                    }
                }
                tokens.add(t);
                if(t.isError() && abortWhenIncorrect){
                    break;
                }
            }
        }
        if(illegalStr.length() > 0){
            tokens.add(new JsonToken(i - illegalStr.length(), illegalStr.toString(), JsonToken.Type.UNKNOWN));
        }
        if(!objOrArr.isEmpty()){
            JsonToken last = tokens.removeLast();
            tokens.add(new JsonToken(last.getStart(), last.getLiteral(), last.getType(), true));
        }
        return tokens;
    }

    private static int stackPeek(Stack<Integer> stack){
        return stack.isEmpty() ? -1 : stack.peek();
    }

    private static boolean isWhitespace(char ch) {
        return ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n';
    }

    private static String matchWhitespace(String str, int start){
        StringBuilder sb = new StringBuilder();
        for(int i = start, len = str.length(); i < len; i++){
            char ch = str.charAt(i);
            if(!isWhitespace(ch)){
                break;
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    private static String matchString(String str, int start, RefObj<Boolean> check){
        check.value = false;
        StringBuilder result = new StringBuilder();
        int i = start;
        int len = str.length();
        char ch = str.charAt(i);
        if (ch != '"') { return result.toString(); }
        i++;
        result.append('"');
        int escape = 0;
        while (i < len) {
            ch = str.charAt(i);
            if (ch == '"' && (escape & 1) == 0) {
                check.value = true;
                result.append('"');
                break;
            }else{
                result.append(ch);
                i++;
            }
            if(ch == '\\'){
                escape++;
            }else{
                escape = 0;
            }
        }
        return result.toString();
    }

    private static boolean isNotDigit(char ch) {
        return ch < '0' || ch > '9';
    }

    private static String matchNumber(String str, int start){
        StringBuilder number = new StringBuilder();
        int i = start;
        int len = str.length();
        char ch = str.charAt(i);
        if(ch == '-' && i + 1 < len){  // 匹配正负号
            number.append(ch);
            i++;
        }

        ch = str.charAt(i);
        if(isNotDigit(ch)){
            return null;
        }

        number.append(ch);
        i++;
        if(ch != '0'){  // 匹配整数部分
            for(; i < len; i++){
                ch = str.charAt(i);
                if(isNotDigit(ch)){ break; }
                number.append(ch);
            }
        }else if(i < len){
            ch = str.charAt(i);
        }

        if(ch == '.' && i + 1 < len){
            i++;
            ch = str.charAt(i); // 向后预看一位
            if(isNotDigit(ch)){
                return number.toString();
            }
            number.append('.');
            for(; i < len; i++){
                ch = str.charAt(i);
                if(isNotDigit(ch)){ break; }
                number.append(ch);
            }
        }

        if((ch == 'e' || ch == 'E') && i + 1 < len){
            i++;
            char p = str.charAt(i);
            if(isNotDigit(p)){
                if(i + 1 < len && (p == '+' || p == '-')){
                    i++;
                    char p2 = str.charAt(i);
                    if(!isNotDigit(p2)){
                        number.append(ch).append(p);
                    }else{
                        return number.toString();
                    }
                }else{
                    return number.toString();
                }
            }else{
                number.append(ch);
            }

            for(; i < len; i++){
                ch = str.charAt(i);
                if(isNotDigit(ch)){ break; }
                number.append(ch);
            }
        }

        return number.toString();
    }

    private static String trySubString(String str, int start, int len){
        if(str.length() >= start + len){
            return str.substring(start, start + len);
        }else{
            return str.substring(start);
        }
    }

    public static List<BracketPair> getBracketPair(List<JsonToken> tokens){
        ArrayList<BracketPair> pairs = new ArrayList<>();
        if(tokens == null || tokens.isEmpty()){ return pairs; }
        Stack<JsonToken> stack = new Stack<>();
        for (JsonToken token : tokens) {
            if(token.getType() == JsonToken.Type.STRUCTURE){
                String literal = token.getLiteral();
                switch (literal){
                    case "{":
                    case "[":
                        stack.push(token);
                        break;
                    case "}":
                        if(!stack.isEmpty() && "{".equals(stack.peek().getLiteral())){
                            pairs.add(new BracketPair(stack.pop(), token));
                        }
                        break;
                    case "]":
                        if(!stack.isEmpty() && "[".equals(stack.peek().getLiteral())){
                            pairs.add(new BracketPair(stack.pop(), token));
                        }
                        break;
                }
            }
        }
        return pairs;
    }

    /**
     * 括号对
     */
    public static class BracketPair{
        private final JsonToken start;
        private final JsonToken end;

        public BracketPair(JsonToken start, JsonToken end) {
            this.start = start;
            this.end = end;
        }

        public JsonToken getStart() {
            return start;
        }

        public JsonToken getEnd() {
            return end;
        }
    }
}
