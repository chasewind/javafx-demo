package com.example.javafxdemo.components;

import com.example.javafxdemo.json.DeserializerConfiguration;
import com.example.javafxdemo.json.JsonLexicalAnalyzer;
import com.example.javafxdemo.json.JsonToken;
import com.example.javafxdemo.util.GlobalApplicationLifecycleUtil;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JsonAssist {

    private final CodeHighLight codeHighLight = new CodeHighLight();

    private final BracketHighLight bracketHighLight = new BracketHighLight();

    private static JsonAssist instance = null;

    public static JsonAssist getInstance(){
        if(instance == null){
            instance = new JsonAssist();
            GlobalApplicationLifecycleUtil.addOnCloseListener(() -> {
                instance.codeHighLight.stop();
            });
        }
        return instance;
    }

    public void enableAssist(final CustomCodeArea codeArea) {
        codeArea.textProperty().addListener((observableValue, s, t1) -> {
            codeArea.getContext().put(AssistObject.class, null);
        });

        codeHighLight.enable(codeArea);
        bracketHighLight.enable(codeArea);
    }


    private class CodeHighLight {

        private final ExecutorService executor = Executors.newSingleThreadExecutor();

        public void enable(final CustomCodeArea codeArea) {
            // ---- 高亮 ----
            codeArea.multiPlainChanges()
                    .successionEnds(Duration.ofMillis(500))
                    .retainLatestUntilLater(executor)
                    .supplyTask(() ->
                            computeHighlightingAsync(codeArea)
                    )
                    .awaitLatest(codeArea.multiPlainChanges())
                    .filterMap(t -> {
                        if(t.isSuccess()) {
                            return Optional.of(t.get());
                        } else {
                            t.getFailure().printStackTrace();
                            return Optional.empty();
                        }
                    })
                    .subscribe((highlighting) -> {
                        applyHighlighting(highlighting, codeArea);
                    });
        }

        private Task<StyleSpans<Collection<String>>> computeHighlightingAsync(CustomCodeArea codeArea) {
            Task<StyleSpans<Collection<String>>> task = new Task<>() {
                @Override
                protected StyleSpans<Collection<String>> call() {
                    return computeHighlighting(codeArea);
                }
            };
            executor.execute(task);
            return task;
        }

        private void applyHighlighting(StyleSpans<Collection<String>> highlighting, CustomCodeArea codeArea) {
            codeArea.setStyleSpans(0, highlighting);
            bracketHighLight.beginHighLight(codeArea);
        }

        // 一段文本(即一行)中所允许包含的最大span数量
        private static final int MAX_SPAN_COUNT_IN_ONE_PARAGRAPH = 2000;

        private StyleSpans<Collection<String>> computeHighlighting(CustomCodeArea codeArea) {
            StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
            List<JsonToken> tokens = getAssistObject(codeArea).getTokens();
            if(tokens == null || tokens.isEmpty()){
                spansBuilder.add(Collections.emptyList(), codeArea.getText().length());
                return spansBuilder.create();
            }
            ArrayList<String> preStyles = null;
            ArrayList<String> cursorStyles = null;
            ArrayList<StyleBox> styleList = new ArrayList<>(tokens.size());
            for (JsonToken token : tokens) {
                JsonToken.Type type = token.getType();
                String style;
                switch (type){
                    case BOOL:
                        style = "boolean";
                        break;
                    case NULL:
                        style = "null";
                        break;
                    case NUMBER:
                        style = "number";
                        break;
                    case KEY:
                        style = "key";
                        break;
                    case STR_VALUE:
                        style = "string-value";
                        break;
                    case STRUCTURE:
                        String literal = token.getLiteral();
                        if("{".equals(literal) || "}".equals(literal)){ style = "brace"; }
                        else if ("[".equals(literal) || "]".equals(literal)){ style = "bracket"; }
                        else { style = "splitter"; }
                        break;
                    default:
                        style = "unknown";
                        break;
                }
                ArrayList<String> styles = new ArrayList<>();
                if(token.isError() || token.getType() == JsonToken.Type.UNKNOWN){
                    styles.add("underlined");
                }
                preStyles = cursorStyles;
                cursorStyles = styles;
                styles.add(style);
                styleList.add(new StyleBox(token.getLiteral(), styles));
            }
            if(preStyles != null){
                JsonToken t = tokens.get(tokens.size() - 1);
                if(t.getType() == JsonToken.Type.BLANK && t.isError()){
                    preStyles.add("underlined");
                }
            }

            if(styleList.size() > MAX_SPAN_COUNT_IN_ONE_PARAGRAPH){
                // 如果一行包含的spanCount过多, 则多出部分不高亮
                StyledDocument<Collection<String>, String, Collection<String>> doc = codeArea.getDocument();
                List<Paragraph<Collection<String>, String, Collection<String>>> paragraphs = doc.getParagraphs();
                int[] paragraphRange = new int[paragraphs.size() + 1];
                int i = 1;
                for (Paragraph<Collection<String>, String, Collection<String>> paragraph : paragraphs) {
                    String text = paragraph.getText();
                    paragraphRange[i] = paragraphRange[i - 1] + text.length();
                    i++;
                }
                int cursor = 1;
                int spanCount = 0;
                int textLen = 0;
                boolean plain;
                int plainTextLen = 0;
                for (StyleBox styleBox : styleList) {
                    textLen += styleBox.token.length();
                    spanCount++;
                    plain = spanCount > MAX_SPAN_COUNT_IN_ONE_PARAGRAPH;
                    if(!plain){
                        spansBuilder.add(styleBox.styles, styleBox.token.length());
                    }else{
                        plainTextLen += styleBox.token.length();
                    }
                    if(textLen >= paragraphRange[cursor]){
                        if(plain){
                            spansBuilder.add(Collections.singletonList("unknown"), plainTextLen);
                        }
                        textLen = 0;
                        spanCount = 0;
                        cursor++;
                        plainTextLen = 0;
                    }
                }
            }else{
                for (StyleBox styleBox : styleList) {
                    spansBuilder.add(styleBox.styles, styleBox.token.length());
                }
            }
            return spansBuilder.create();
        }

        public void stop() {
            executor.shutdown();
        }
    }

    private class BracketHighLight {

        private final String MATCH_STYLE = "match-pair";

        private static final String BRACKET_PAIRS = "{}[]";

        private void match(CodeArea codeArea, Pair pair){
            styleBracket(codeArea, pair.start, MATCH_STYLE);
            styleBracket(codeArea, pair.end, MATCH_STYLE);
        }

        private void clearMatch(CodeArea codeArea, Pair pair){
            removeStyle(codeArea, pair.start, MATCH_STYLE);
            removeStyle(codeArea, pair.end, MATCH_STYLE);
        }

        private void removeStyle(CodeArea codeArea, int pos, String style){
            if (pos < codeArea.getLength()) {
                String text = codeArea.getText(pos, pos + 1);
                if (BRACKET_PAIRS.contains(text)) {
                    StyleSpans<Collection<String>> styleSpans = codeArea.getStyleSpans(pos, pos + 1);
                    HashSet<String> newStyles = new HashSet<>();
                    if(styleSpans != null && styleSpans.length() > 0){
                        for (StyleSpan<Collection<String>> styleSpan : styleSpans) {
                            for (String s : styleSpan.getStyle()) {
                                if(!style.equals(s)){
                                    newStyles.add(s);
                                }
                            }
                        }
                    }
                    codeArea.setStyle(pos, pos + 1, newStyles);
                }
            }
        }

        private void styleBracket(CodeArea codeArea, int pos, String style) {
            if (pos < codeArea.getLength()) {
                String text = codeArea.getText(pos, pos + 1);
                if (BRACKET_PAIRS.contains(text)) {
                    StyleSpans<Collection<String>> styleSpans = codeArea.getStyleSpans(pos, pos + 1);
                    HashSet<String> newStyles = new HashSet<>();
                    if(styleSpans != null && styleSpans.length() > 0){
                        for (StyleSpan<Collection<String>> styleSpan : styleSpans) {
                            newStyles.addAll(styleSpan.getStyle());
                        }
                    }
                    newStyles.add(style);
                    codeArea.setStyle(pos, pos + 1, newStyles);
                }
            }
        }

        private void highlightBracket(CustomCodeArea codeArea, int newVal) {
            AssistObject assistObject = getAssistObject(codeArea);
            if(assistObject.bracketHighlight){
                this.clearBracket(codeArea);

                String prevChar = (newVal > 0 && newVal <= codeArea.getLength()) ? codeArea.getText(newVal - 1, newVal) : "";
                if (BRACKET_PAIRS.contains(prevChar)) newVal--;

                int other = getMatchingBracket(codeArea, newVal);

                if (other < 0) { return; }
                Pair pair = new Pair(newVal, other);
                match(codeArea, pair);
                assistObject.matchPairs.add(pair);
            }
        }

        private int getMatchingBracket(CustomCodeArea codeArea, int index) {
            if (index < 0 || index >= codeArea.getLength()) return -1;

            AssistObject assistObject = getAssistObject(codeArea);
            List<JsonLexicalAnalyzer.BracketPair> bracketPairs = assistObject.getBracketPairs();
            for (JsonLexicalAnalyzer.BracketPair bracketPair : bracketPairs) {
                if(bracketPair.getStart().getStart() == index){
                    return bracketPair.getEnd().getStart();
                }else if(bracketPair.getEnd().getStart() == index){
                    return bracketPair.getStart().getStart();
                }
            }
            return -1;
        }

        public void clearBracket(CustomCodeArea codeArea) {
            AssistObject assistObject = getAssistObject(codeArea);
            Iterator<Pair> iterator = assistObject.matchPairs.iterator();
            while (iterator.hasNext()) {
                Pair pair = iterator.next();
                clearMatch(codeArea, pair);
                iterator.remove();
            }
        }

        public void enable(CustomCodeArea codeArea) {
            codeArea.caretPositionProperty().addListener((obs, oldVal, newVal) -> Platform.runLater(() -> highlightBracket(codeArea, newVal)));
        }

        public void beginHighLight(CustomCodeArea codeArea) {
            AssistObject assistObject = getAssistObject(codeArea);
            assistObject.bracketHighlight = true;
            highlightBracket(codeArea, codeArea.getCaretPosition());
        }
    }

    public AssistObject getAssistObject(CustomCodeArea codeArea){
        CustomCodeArea.CustomCodeAreaContext context = codeArea.getContext();
        AssistObject assistObject = context.getVariable(AssistObject.class);
        if(assistObject == null){
            assistObject = new AssistObject(codeArea);
            context.put(AssistObject.class, assistObject);
        }
        return assistObject;
    }

    private static class Pair{
        final int start;
        final int end;

        public Pair(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    private static final DeserializerConfiguration DESERIALIZER_CONFIGURATION = new DeserializerConfiguration();
    static {
        DESERIALIZER_CONFIGURATION.setEscape(false);
        DESERIALIZER_CONFIGURATION.setAbortWhenIncorrect(false);
    }

    private static class AssistObject{
        private final CustomCodeArea codeArea;

        public AssistObject(CustomCodeArea codeArea){
            this.codeArea = codeArea;
        }

        private boolean bracketHighlight = false;

        private final ArrayList<Pair> matchPairs = new ArrayList<>();
        private List<JsonToken> tokens;
        private List<JsonLexicalAnalyzer.BracketPair> bracketPairs;

        public List<JsonToken> getTokens(){
            if(tokens == null){
                String text = codeArea.getText();
                if(StringUtils.isNotBlank(text)){
                    tokens = JsonLexicalAnalyzer.lexicalAnalysis(text, DESERIALIZER_CONFIGURATION);
                }
            }
            return tokens;
        }

        public List<JsonLexicalAnalyzer.BracketPair> getBracketPairs(){
            if(bracketPairs == null && tokens != null){
                bracketPairs = JsonLexicalAnalyzer.getBracketPair(tokens);
            }
            return bracketPairs;
        }
    }

    private static class StyleBox{
        private final String token;
        private final List<String> styles;
        public StyleBox(String token, List<String> styles) {
            this.token = token;
            this.styles = styles;
        }
    }
}
