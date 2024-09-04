package com.example.javafxdemo.json;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

class JsonSyntacticAnalysis {

    private JsonSyntacticAnalysis() {
        // do nothing
    }

    private static final BuildStatusNode ROOT_NODE;

    static {
        ROOT_NODE = buildStatusMap();
    }

    public static JsonElement syntacticAnalysis(List<JsonToken> tokens, String json) throws JsonParseException {
        if (tokens == null || tokens.size() < 2) {
            throw new JsonParseException("json lack necessary structure.");
        }
        Stack<BuildStatusNode> pendingStack = new Stack<>();
        JsonBuilder jsonBuilder = new JsonBuilder();
        BuildStatusNode cursor = ROOT_NODE;
        for (JsonToken token : tokens) {
            if(token.getType() == JsonToken.Type.BLANK){ continue; }
            BuildStatusNode pre = cursor;
            cursor = cursor.move(token);
            if (cursor == null && pre.isTerminal() && !pendingStack.isEmpty()) {
                cursor = pendingStack.pop().move(token);
            }
            if (cursor == null) {
                JsonParseUtil.triggerException(token, json);
            }
            assert cursor != null;
            cursor.tokenHandler.handle(token, jsonBuilder);

            if (cursor.isNest()) {    // 如果是嵌套节点
                pendingStack.push(cursor);
                cursor = cursor.moveInNest(token);
                if (cursor == null) {
                    JsonParseUtil.triggerException(token, json);
                }
                assert cursor != null;
                cursor.tokenHandler.handle(token, jsonBuilder);
            }
        }
        if (!jsonBuilder.stack.isEmpty()) {
            throw new JsonParseException("json is not end right");
        }
        return jsonBuilder.root.build();
    }

    private static BuildStatusNode buildStatusMap() {
        // --- object ---
        BuildStatusNode objectBegin = new BuildStatusNode(JsonWord.OBJECT_BEGIN, JsonToken.Type.STRUCTURE,
                (token, builder) -> builder.newJsonObject());
        BuildStatusNode objectKey = new BuildStatusNode(BuildStatusNode.WILDCARD, JsonToken.Type.KEY,
                (token, builder) -> builder.newObjectKey(trimQuote(token.getLiteral())));
        BuildStatusNode objectColon = new BuildStatusNode(JsonWord.COLON, JsonToken.Type.STRUCTURE, (token, builder) -> { /* do nothing */ });
        BuildStatusNode objectComma = new BuildStatusNode(JsonWord.COMMA, JsonToken.Type.STRUCTURE, (token, builder) -> {/* do nothing */});
        BuildStatusNode objectEnd = new BuildStatusNode(JsonWord.OBJECT_END, JsonToken.Type.STRUCTURE,
                (token, builder) -> builder.closeElement());
        BuildStatusNode objectNest = new BuildStatusNode(null, null, (token, builder) -> { /* do nothing */ }, true);

        objectBegin.to(objectKey);
        objectBegin.to(objectEnd);

        objectKey.to(objectColon);

        objectColon.to(objectNest);

        objectNest.to(objectComma);
        objectNest.to(objectEnd);

        objectComma.to(objectKey);

        // --- array ---
        BuildStatusNode arrayBegin = new BuildStatusNode(JsonWord.ARRAY_BEGIN, JsonToken.Type.STRUCTURE,
                (token, builder) -> builder.newJsonArray());
        BuildStatusNode arrayEnd = new BuildStatusNode(JsonWord.ARRAY_END, JsonToken.Type.STRUCTURE,
                (token, builder) -> builder.closeElement());
        BuildStatusNode arrayComma = new BuildStatusNode(JsonWord.COMMA, JsonToken.Type.STRUCTURE, (token, builder) -> {/* do nothing */ });
        BuildStatusNode arrayNest = new BuildStatusNode(null, null, (token, builder) -> { /* do nothing */ }, true);

        arrayBegin.to(arrayEnd);
        arrayBegin.to(arrayNest);
        arrayNest.to(arrayComma);
        arrayNest.to(arrayEnd);
        arrayComma.to(arrayNest);

        // --- value ---
        BuildStatusNode stringValue = new BuildStatusNode(BuildStatusNode.WILDCARD, JsonToken.Type.STR_VALUE,
                (token, builder) -> builder.addArrayValue(new StringJsonValue(trimQuote(token.getLiteral()))));
        BuildStatusNode constValue = new BuildStatusNode(BuildStatusNode.WILDCARD, JsonToken.Type.BOOL,
                (token, builder) -> builder.addArrayValue(new ConstJsonValue(token.getLiteral())));
        BuildStatusNode nullValue = new BuildStatusNode(BuildStatusNode.WILDCARD, JsonToken.Type.NULL,
                (token, builder) -> builder.addArrayValue(new ConstJsonValue(token.getLiteral())));
        BuildStatusNode numberValue = new BuildStatusNode(BuildStatusNode.WILDCARD, JsonToken.Type.NUMBER,
                (token, builder) -> builder.addArrayValue(new NumberJsonValue(token.getLiteral())));
        arrayNest.nestInclude(arrayBegin);
        arrayNest.nestInclude(objectBegin);
        arrayNest.nestInclude(stringValue);
        arrayNest.nestInclude(constValue);
        arrayNest.nestInclude(nullValue);
        arrayNest.nestInclude(numberValue);

        objectNest.nestInclude(arrayBegin);
        objectNest.nestInclude(objectBegin);
        objectNest.nestInclude(stringValue);
        objectNest.nestInclude(constValue);
        objectNest.nestInclude(nullValue);
        objectNest.nestInclude(numberValue);

        BuildStatusNode rootNode = new BuildStatusNode(null, null, (token, builder) -> {/* do nothing */ });
        rootNode.to(objectBegin);
        rootNode.to(arrayBegin);
        return rootNode;
    }

    private static class JsonBuilder {

        private final Stack<JsonElementBuilder> stack = new Stack<>();

        private JsonElementBuilder root;

        public void newJsonObject() {
            stack.push(new JsonObjectBuilder());
            createCallback();
        }

        public void newJsonArray() {
            stack.push(new JsonArrayBuilder());
            createCallback();
        }

        private void createCallback() {
            if (root == null) {
                root = stack.peek();
            }
        }

        public void newObjectKey(String key) {
            stack.peek().setPendingKey(key);
        }

        public void addArrayValue(JsonValue<?> jsonValue) {
            stack.peek().setJsonValue(jsonValue);
        }

        public void closeElement() {
            JsonElementBuilder builder = stack.pop();
            JsonElementBuilder parent;
            if (stack.isEmpty()) {
                parent = root;
            } else {
                parent = stack.peek();
            }
            if (parent == builder) {
                return;
            }
            parent.setJsonElement(builder.build());
        }
    }

    private interface JsonElementBuilder {
        void setPendingKey(String pendingKey);

        void setJsonValue(JsonValue<?> jsonValue);

        void setJsonElement(JsonElement jsonObject);

        JsonElement build();
    }

    private static class JsonObjectBuilder implements JsonElementBuilder {
        private String pendingKey = null;

        private JsonObject jsonObject = new JsonObject();

        @Override
        public void setPendingKey(String pendingKey) {
            if (this.pendingKey == null) {
                this.pendingKey = pendingKey;
            } else {
                throw new IllegalStateException("repeat assign object key.");
            }
        }

        @Override
        public void setJsonValue(JsonValue<?> jsonValue) {
            if (pendingKey == null) {
                throw new IllegalStateException("key is not assign");
            } else {
                jsonObject.putJsonValue(pendingKey, jsonValue);
            }
            pendingKey = null;
        }

        @Override
        public void setJsonElement(JsonElement jsonElement) {
            if (pendingKey == null) {
                throw new IllegalStateException("key is not assign");
            } else {
                if (jsonElement instanceof JsonObject) {
                    jsonObject.putObject(pendingKey, (JsonObject) jsonElement);
                } else {
                    jsonObject.putArray(pendingKey, (JsonArray) jsonElement);
                }
                this.pendingKey = null;
            }
        }

        @Override
        public JsonElement build() {
            JsonElement result = this.jsonObject;
            this.jsonObject = null;
            return result;
        }
    }

    private static class JsonArrayBuilder implements JsonElementBuilder {

        private JsonArray jsonArray = new JsonArray();

        @Override
        public void setPendingKey(String pendingKey) {
            throw new UnsupportedOperationException("setPendingKey");
        }

        @Override
        public void setJsonValue(JsonValue<?> jsonValue) {
            jsonArray.addJsonValue(jsonValue);
        }

        @Override
        public void setJsonElement(JsonElement jsonElement) {
            if (jsonElement instanceof JsonObject) {
                jsonArray.addObject((JsonObject) jsonElement);
            } else {
                jsonArray.addArray((JsonArray) jsonElement);
            }
        }

        @Override
        public JsonElement build() {
            JsonElement result = this.jsonArray;
            this.jsonArray = null;
            return result;
        }
    }

    private interface ITokenHandler {
        void handle(JsonToken token, JsonBuilder builder);
    }

    private static class BuildStatusNode {

        public static final String WILDCARD = "*";

        private final JsonToken.Type tokenType;

        private final String literal;

        private final HashMap<JsonToken.Type, HashMap<String, BuildStatusNode>> moveMap = new HashMap<>();

        private final HashMap<JsonToken.Type, HashMap<String, BuildStatusNode>> moveNestMap = new HashMap<>(0);

        private BuildStatusNode defaultNode = null;

        private final ITokenHandler tokenHandler;

        // 该节点是否为嵌套节点
        private final boolean nest;

        public BuildStatusNode(String literal, JsonToken.Type tokenType, ITokenHandler tokenHandler) {
            this.tokenHandler = tokenHandler;
            this.tokenType = tokenType;
            this.literal = literal;
            this.nest = false;
        }

        public BuildStatusNode(String literal, JsonToken.Type tokenType, ITokenHandler tokenHandler, boolean nest) {
            this.tokenHandler = tokenHandler;
            this.tokenType = tokenType;
            this.literal = literal;
            this.nest = nest;
        }

        /**
         * 移动至下一个状态节点
         * 如果返回null, 说明移动失败
         */
        BuildStatusNode move(JsonToken token) {
            BuildStatusNode next = moveByMap(token, this.moveMap);
            if (next == null) {
                next = defaultNode;
            }
            return next;
        }

        void to(BuildStatusNode node) {
            if (node.tokenType == null) {
                if(this.defaultNode != null){
                    throw new IllegalStateException("default node has exists.");
                }
                this.defaultNode = node;
            } else {
                addToMap(node, this.moveMap);
            }
        }

        private BuildStatusNode moveByMap(JsonToken token, HashMap<JsonToken.Type, HashMap<String, BuildStatusNode>> map) {
            HashMap<String, BuildStatusNode> literalToNode = map.get(token.getType());
            BuildStatusNode next = null;
            if (literalToNode != null) {
                next = literalToNode.get(token.getLiteral());
                if (next == null && literalToNode.containsKey(WILDCARD)) {
                    next = literalToNode.get(WILDCARD);
                }
            }
            return next;
        }

        private void addToMap(BuildStatusNode node, HashMap<JsonToken.Type, HashMap<String, BuildStatusNode>> map) {
            HashMap<String, BuildStatusNode> literalToNode = map.computeIfAbsent(node.tokenType, k -> new HashMap<>());
            if (literalToNode.containsKey(node.literal)) {
                throw new IllegalArgumentException("assign map has exists, type : " + node.tokenType + ", literal : " + node.literal);
            }
            literalToNode.put(node.literal, node);
        }

        boolean isNest() {
            return nest;
        }

        void nestInclude(BuildStatusNode node) {
            addToMap(node, this.moveNestMap);
        }

        BuildStatusNode moveInNest(JsonToken token) {
            return moveByMap(token, this.moveNestMap);
        }

        boolean isTerminal(){
            return this.defaultNode == null && this.moveMap.isEmpty() && this.moveNestMap.isEmpty();
        }

    }

    private static String trimQuote(String str){
        return str.substring(1, str.length() - 1);
    }


}
