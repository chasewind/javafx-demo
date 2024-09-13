package com.example.javafxdemo.components;

import com.example.javafxdemo.MessageEmitter;
import com.example.javafxdemo.util.ClipboardUtil;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.Paragraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonTextBox extends BorderPane {

    private final CustomCodeArea codeArea = new CustomCodeArea();

    private final SearchBox textSearchBox;

    private final SearchAction searchAction;

    public JsonTextBox(MessageEmitter messageEmitter) {
        initCodeArea();
        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
        super.setCenter(scrollPane);
        this.searchAction = new SearchAction(codeArea, messageEmitter);
        textSearchBox = new SearchBox(this, (text, searchOverviewFetcher) -> {
            messageEmitter.clear();
            this.searchAction.search(text, true, searchOverviewFetcher);
        }, (text, searchOverviewFetcher) -> {
            messageEmitter.clear();
            this.searchAction.search(text, false, searchOverviewFetcher);
        });
        this.setOnKeyPressed(keyEvent -> {
            //window支持
            if(keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.F){
                this.searchBegin();
                keyEvent.consume(); // 消耗事件，防止进一步处理
            }
            //mac支持
            if(keyEvent.isMetaDown() && keyEvent.getCode()== KeyCode.F){
                this.searchBegin();
                keyEvent.consume(); // 消耗事件，防止进一步处理
            }
        });
        textSearchBox.onClose(this.codeArea::requestFocus);
    }

    private void initCodeArea(){
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.prefHeightProperty().bind(this.heightProperty());
        codeArea.prefWidthProperty().bind(this.widthProperty());
        codeArea.textProperty().addListener((observableValue, s, t1) -> searchAction.reset());
        codeArea.setContextMenu(initContextMenu());
        // 换行后自动缩进
        final Pattern whiteSpace = Pattern.compile( "^\\s+" );
        codeArea.addEventHandler( KeyEvent.KEY_PRESSED, e ->
        {
            if(e.getCode() == KeyCode.ENTER){
                int caretPosition = codeArea.getCaretPosition();
                int currentParagraphIndex = codeArea.getCurrentParagraph();
                Paragraph<Collection<String>, String, Collection<String>> preParagraph = codeArea.getParagraph(currentParagraphIndex - 1);
                String tail = preParagraph.getText().trim();
                boolean preLineEndWithBracket = tail.endsWith("{") || tail.endsWith("[");

                Paragraph<Collection<String>, String, Collection<String>> currentParagraph = codeArea.getParagraph(currentParagraphIndex);
                String postText = currentParagraph.getText();
                boolean postLineStartWithBracket = postText.startsWith("}") || postText.startsWith("]");

                Matcher m0 = whiteSpace.matcher(preParagraph.getSegments().get(0));
                Platform.runLater(() -> {
                    codeArea.insertText(caretPosition, (m0.find() ? m0.group() : "") + (preLineEndWithBracket && !postLineStartWithBracket ? "    " : ""));
                });
            }
        });
        JsonAssist highlight = JsonAssist.getInstance();
        highlight.enableAssist(codeArea);
    }


    private ContextMenu initContextMenu(){
        ContextMenu contextMenu = new ContextMenu();
        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(event -> {
            String selectedText = codeArea.getSelectedText();
            if(selectedText != null && !"".equals(selectedText)){
                ClipboardUtil.putToClipboard(selectedText);
            }
        });
        MenuItem find = new MenuItem("Find");
        find.setOnAction(event -> {
            String selectedText = codeArea.getSelectedText();
            if(selectedText != null && !"".equals(selectedText)){
                this.searchBegin();
            }
        });
        MenuItem undo = new MenuItem("Undo");
        undo.setOnAction(event -> {
            codeArea.undo();
        });
        MenuItem redo = new MenuItem("Redo");
        redo.setOnAction(event -> {
            codeArea.redo();
        });
        ObservableList<MenuItem> items = contextMenu.getItems();
        items.add(copy);
        items.add(find);
        items.add(undo);
        items.add(redo);
        return contextMenu;
    }

    private void searchBegin(){
        this.setTop(textSearchBox);
        textSearchBox.focusSearch(codeArea.getSelectedText());
    }

    public String getContent() {
        return codeArea.getText();
    }

    public void setContent(String json) {
        codeArea.clear();
        codeArea.appendText(json);
    }

    private static class SearchAction {

        private final CodeArea codeArea;

        private final MessageEmitter messageEmitter;

        public SearchAction(CodeArea codeArea, MessageEmitter messageEmitter) {
            this.codeArea = codeArea;
            this.messageEmitter = messageEmitter;
        }

        private ArrayList<SearchResult> searchResults = null;

        private String searchText;

        private boolean reachBottom;

        private boolean reachTop;

        private int lastCaretPos = -1;

        public void search(String searchText, boolean backward/*true -- 向后搜索, false - 向前搜索*/, SearchBox.SearchOverviewFetcher overviewFetcher){
            if(!searchText.equals(this.searchText)){ // 新的关键字
                this.searchText = searchText;
                this.searchResults = null;
                this.reachBottom = this.reachTop = false;
                buildSearchResult();
            }

            int cursorIndex = -1;
            if(this.searchResults.isEmpty()){
                messageEmitter.emitWarn("no result found for : " + searchText);
            }else{
                int startIndex = backward ? searchResults.size() - 1 : 0;
                int start = codeArea.getCaretPosition();
                if(lastCaretPos != start){
                    lastCaretPos = start;
                    this.reachBottom = this.reachTop = false;
                }
                for(int i = 0, len = this.searchResults.size(); i < len; i++){
                    SearchResult result = searchResults.get(i);
                    int edge = backward ? result.start : (result.end + 1);
                    if(start < edge){
                        if(backward){
                            startIndex = i - 1;
                        }else{
                            startIndex = i;
                        }
                        break;
                    }
                }

                if(backward){
                    int nextIndex = startIndex + 1;
                    if(nextIndex == searchResults.size()){
                        if(!this.reachBottom){
                            cursorIndex = nextIndex - 1;
                            this.messageEmitter.emitWarn("search reach bottom");
                        }else{
                            cursorIndex = 0;
                        }
                        this.reachBottom = !this.reachBottom;
                    }else{
                        cursorIndex = nextIndex;
                    }
                }else{
                    int nextIndex = startIndex - 1;
                    if(nextIndex == -1){
                        if(!this.reachTop){
                            cursorIndex = 0;
                            this.messageEmitter.emitWarn("search reach top");
                        }else{
                            cursorIndex = searchResults.size() - 1;
                        }
                        this.reachTop = !this.reachTop;
                    }else{
                        cursorIndex = nextIndex;
                    }
                }

                if(cursorIndex >= 0){
                    focusFindResult(searchResults.get(cursorIndex));
                    overviewFetcher.setSearchOverview(new SearchBox.SearchOverview(searchResults.size() - 1, cursorIndex));
                }

            }
        }

        private void focusFindResult(SearchResult result){
            codeArea.selectRange(result.start, result.end);
            codeArea.requestFollowCaret();
        }

        private void buildSearchResult(){
            if(searchText == null || searchText.isEmpty()){
                searchResults = new ArrayList<>(0);
                return;
            }
            int len = searchText.length();
            String content = this.codeArea.getText();
            int index = content.indexOf(searchText);
            searchResults = new ArrayList<>();
            while(index >= 0){
                searchResults.add(new SearchResult(index, index + len));
                index = content.indexOf(searchText, index + len);
            }
        }

        public void reset(){
            this.searchText = null;
            this.searchResults = null;
            this.reachBottom = this.reachTop = false;
        }

    }

    private static class SearchResult {
        private final int start;
        private final int end;

        public SearchResult(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}
