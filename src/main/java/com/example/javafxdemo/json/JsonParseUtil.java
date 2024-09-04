package com.example.javafxdemo.json;

class JsonParseUtil {

    static void triggerException(JsonToken token, String json) throws JsonParseException {
        int start = token.getStart();
        int rowIndex = 0;
        int colIndex = 0;
        for(int i = 0; i <= start; i++){
            if(isLineFeed(json.charAt(i))){
                rowIndex++;
                colIndex = 0;
            }else{
                colIndex++;
            }
        }
        rowIndex++;
        colIndex++;
        throw new JsonParseException("json format is not right, row : " + rowIndex + ", col : " + colIndex + ", literal : " + token.getLiteral());
    }

    /**
     * 判断是否是换行符
     * @param ch 待判断的字符
     * @return true - 是; false - 否
     */
    private static boolean isLineFeed(char ch) {
        return ch == '\n';
    }

}
