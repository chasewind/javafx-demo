package com.example.javafxdemo;

public class GlobalConstants {

    public static String DEFAULT_TEMPLATE= """
             { \s
              "query": { \s
                "bool": {
                "filter": [ \s
                   {
                      "exists" : {
                        "field" : "title_en",
                        "boost" : 1.0
                      }
                    },
                   {
                      "terms" : {
                        "goodSn" : [
                          "CJ38SAECRMO90NGRCV0",
                          "JXGJ143812SAE8DWQV0",
                          "LMTQQ29JT000UIA3TV0"
                        ],
                        "boost" : 1.0
                      }
                    },
                    { \s
                      "term": { \s
                        "category": "tech" \s
                      } \s
                    } \s
                  ],  \t
                  "must": [ \s
                    { \s
                      "term": { \s
                        "status": "active" \s
                      } \s
                    }, \s
                    { \s
                      "match": { \s
                        "title": "Elasticsearch" \s
                      } \s
                    } \s
                  ], \s
                  "must_not": [ \s
                    { \s
                      "term": { \s
                        "type": "temporary" \s
                      } \s
                    } \s
                  ], \s
                  "should": [ \s
                    { \s
                       "match_phrase": { \s
                              "description": "powerful search engine" \s
                       } \s
                    },
                    { \s
                      "match": { \s
                        "description": "search engine" \s
                      } \s
                    }, \s
                    { \s
                      "range": { \s
                        "age": { \s
                          "gte": 10, \s
                          "lte": 20 \s
                        } \s
                      } \s
                    } \s
                  ], \s
                  "minimum_should_match": 1, \s
                  "boost": 1.0 \s
                } \s
              } \s
            }""";
}
